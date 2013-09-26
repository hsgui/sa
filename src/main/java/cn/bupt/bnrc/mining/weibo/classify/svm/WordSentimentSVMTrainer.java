package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import libsvm.LibSVMUtil;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.ejml.ops.MatrixIO;
import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.classify.EmoticonClassify;
import cn.bupt.bnrc.mining.weibo.classify.Lexicon;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;
import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;
import cn.bupt.bnrc.mining.weibo.util.Utils;

import com.google.common.io.Files;

public class WordSentimentSVMTrainer {

	private svm_parameter param;		// set by parse_command_line
	private svm_problem prob;		// set by read_problem
	private svm_model model;
	
	private String model_file_name;		// set by parse_command_line
	private String error_msg;
	private int cross_validation;
	private int nr_fold;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private String[] emoticonWords = {
			"哈哈", "心", "偷笑", "嘻嘻", "爱你", "鼓掌", "花心", "good", "赞", "酷", "给力", "威武", 
			"泪", "抓狂", "怒", "衰", "可怜", "汗", "晕", "囧", "困", "生病", "委屈", "淚"};	
	private int attributeCount = emoticonWords.length;
	
	private Map<String, Integer> emoticonsCount = new HashMap<String, Integer>();
	private int totalCount = 0;
	
	private ContentSearcher searcher = null;
	
	private Lexicon lexicon = null;
	
	private String prefix = Constants.RESOURCES_PREFIX +"/words/";
	private String trainingFilePath = prefix + "training-set.txt";
	
	private HashMap<String, Double> allPolarEmoticons = null;
	
	private EmoticonSearcher emoticonSearcher = null;
	
	private SentimentWordAttributeExtractor extractor1 = new SentimentWordAttributeExtractor(){
		public double[] extractorSentimentWordAttribute(String word) {
			double[] attributes = new double[attributeCount];
			for (int i = 0; i < attributeCount; i++){
				attributes[i] = emoticonSearcher.getWordAndEmoticonCount(word, emoticonWords[i]) * allPolarEmoticons.get(emoticonWords[i])
						/ emoticonSearcher.getWordCount(word);
			}
			return attributes;
		}		
	};
	
	private SentimentWordAttributeExtractor exractor3 = new SentimentWordAttributeExtractor(){
		double[] attributes = new double[2];
		public double[] extractorSentimentWordAttribute(String word) {
			for (int i = 0; i < emoticonWords.length; i++){
				if (allPolarEmoticons.get(emoticonWords[i]) < 0){
					attributes[1] += emoticonSearcher.getWordAndEmoticonCount(word, emoticonWords[i]);
				}else if (allPolarEmoticons.get(emoticonWords[i]) > 0){
					attributes[0] += emoticonSearcher.getWordAndEmoticonCount(word, emoticonWords[i]);
				}
			}
			int wordCount = emoticonSearcher.getWordCount(word);
			attributes[1] = (0 - attributes[1])/wordCount;
			attributes[0] = attributes[0]/wordCount;
			return attributes;
		}
		
	};
	
	SentimentWordAttributeExtractor extractor2 = new SentimentWordAttributeExtractor(){
		public double[] extractorSentimentWordAttribute(String word) {
			double[] attributes = new double[attributeCount];
			int count = 0;
			double sum = 0;
			for (String emoticonWord : emoticonWords){
				attributes[count++] = 
						(double)(searcher.search2Words(emoticonWord, word, 1000).totalHits + 1);
			}
			for (count = 0; count < attributeCount; count++){
				sum += attributes[count];
			}
			for (count = 0; count < attributeCount; count++){
				attributes[count] = (attributes[count] + totalCount/attributeCount)/(sum + totalCount);
			}
			return attributes;
		}
		
	};
	
	public static void main(String argv[]) throws IOException
	{
		WordSentimentSVMTrainer t = new WordSentimentSVMTrainer();
		
		t.test();
	}
	
	public SimpleMatrix buildFeaturedMatrix2(String[] words){
		double[][] data = new double[words.length][2];
		for (int i = 0; i < words.length; i++){
			data[i] = exractor3.extractorSentimentWordAttribute(words[i]);
			logger.debug(String.format("word=%s, 0=%f, 1=%f", words[i], data[i][0], data[i][1]));
		}
		
		SimpleMatrix a = new SimpleMatrix(data);
		
		return a;
	}
	
	public SimpleMatrix buildFeaturedMatrix(String[] words){
		String akFilePath = Constants.RESOURCES_PREFIX +"/data/ak";
		String aFilePath = Constants.RESOURCES_PREFIX + "/data/a";
		
		SimpleMatrix Ak = null;
		if (new File(akFilePath).exists()){
			try {
				Ak = SimpleMatrix.loadCSV(akFilePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("read ak from csv file");
		}else{
			SimpleMatrix A = this.createWordsMatrix(words);
			try{
				PrintStream print = new PrintStream(aFilePath);
				MatrixIO.print(print, A.getMatrix(), "%.10f");
				//A.saveToFileCSV(aFilePath); -- the precision is too low.
			}catch(Exception e){
				e.printStackTrace();
			}
			
			Ak = this.reductionMatrix(A, 0.99);
			
			try {
				PrintStream out = new PrintStream(akFilePath);
				MatrixIO.print(out, Ak.getMatrix(), "%.10f");
				//Ak.saveToFileCSV(akFilePath); -- the precision is too low.
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			logger.info("reduction the matrix complete! new matrix: rowNum={}, colNum={}", Ak.numRows(), Ak.numCols());
		}
		
		return Ak;
	}
	
	public HashMap<String, Double> prepareSentimentWords(){
		String usingWordsFilePath = Constants.RESOURCES_PREFIX +"/data/usingWords";
		
		HashMap<String, Double> usingWords = null;
		if (new File(usingWordsFilePath).exists()){
			ObjectInputStream in;
			try {
				in = new ObjectInputStream(new FileInputStream(usingWordsFilePath));
				usingWords = (HashMap<String, Double>)in.readObject();
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			logger.info("read usingWords from Serializable file");
		}else{
			usingWords = new HashMap<String, Double>();
			
			HashMap<String, Double> words = Lexicon.readDLLGWords();
			
			HashMap<String, Double> pWordCount = new HashMap<String, Double>();
			HashMap<String, Double> nWordCount = new HashMap<String, Double>();
			
			for (Iterator<Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext();){
				Entry<String, Double> entry = it.next();
				int count = emoticonSearcher.getWordCount(entry.getKey());
				if (10 <= count){
					if (entry.getValue() > 0){
						pWordCount.put(entry.getKey(), count+0.0);
					} else if (entry.getValue() < 0){
						nWordCount.put(entry.getKey(), count+0.0);
					}
				}
				
				logger.info(String.format("word=%s, count=%d", entry.getKey(), count));
			}
			
			logger.info("pWordNum={}, nWordNum={}", pWordCount.size(), nWordCount.size());
			
			pWordCount = Utils.selectTopN(pWordCount, 700);
			nWordCount = Utils.selectTopN(nWordCount, 700);
			for (Iterator<Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext();){
				Entry<String, Double> entry = it.next();
				if (pWordCount.containsKey(entry.getKey()) || nWordCount.containsKey(entry.getKey())){
					usingWords.put(entry.getKey(), entry.getValue());
				}
			}
			
			ObjectOutputStream out;
			try {
				out = new ObjectOutputStream(new FileOutputStream(usingWordsFilePath));
				out.writeObject(usingWords);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			logger.info("compute the usingWords completes.");
		}
		
		return usingWords;
	}
	
	@SuppressWarnings("unchecked")
	public void test(){
		String wordsArrayFilePath = Constants.RESOURCES_PREFIX +"/data/wordsArray";
		
		init();
		
		HashMap<String, Double> usingWords = this.prepareSentimentWords();
		
		String[] wordsArray = null;
		if (new File(wordsArrayFilePath).exists()){
			ObjectInputStream in;
			try {
				in = new ObjectInputStream(new FileInputStream(wordsArrayFilePath));
				wordsArray = (String[])in.readObject();
				in.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			logger.info("read wordsArray from Serializable file");
		}else{
			wordsArray = usingWords.keySet().toArray(new String[0]);
			ObjectOutputStream out;
			try {
				out = new ObjectOutputStream(new FileOutputStream(wordsArrayFilePath));
				out.writeObject(wordsArray);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			logger.info("compute the wordsArray completes.");
		}
				
		logger.info("words size=" + wordsArray.length);
		
		SimpleMatrix Ak = this.buildFeaturedMatrix2(wordsArray);
		
		attributeCount = Ak.numCols();
		
		svm_problem problem = new svm_problem();
		problem.l = wordsArray.length;
		problem.x = new svm_node[problem.l][];
		problem.y = new double[problem.l];		
		for (int sampleIndex = 0; sampleIndex < problem.l; sampleIndex++){			
			svm_node[] attributesNode = new svm_node[attributeCount];
			for (int i = 0;i < attributeCount; i++){
				attributesNode[i] = new svm_node();
				attributesNode[i].index = i;
				attributesNode[i].value = Ak.get(sampleIndex, i);
			}
			problem.x[sampleIndex] = attributesNode;
			problem.y[sampleIndex] = usingWords.get(wordsArray[sampleIndex]);
		}
		
		param = svm_parameter.parse_command_line(new String[0]);
		if(param.gamma == 0 && attributeCount > 0)
			param.gamma = 1.0/attributeCount;

		if(param.kernel_type == svm_parameter.PRECOMPUTED){
			for(int i=0;i<problem.l;i++){
				if (problem.x[i][0].index != 0){
					System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
					System.exit(1);
				}
				if ((int)problem.x[i][0].value <= 0 || (int)problem.x[i][0].value > attributeCount){
					System.err.print("Wrong input format: sample_serial_number out of range\n");
					System.exit(1);
				}
			}
		}
		param.C = 32768;
		param.gamma = 8;
		param.probability = 0;
		
		cross_validation = 1;
		nr_fold = 10;
		
		LibSVMUtil.findBestCGParameter(problem, param);
		logger.info("find best c, g parameters complete. C={}, gamma={}", param.C, param.gamma);
		
		error_msg = svm.svm_check_parameter(problem,param);

		if(error_msg != null)
		{
			System.err.print("ERROR: "+error_msg+"\n");
			System.exit(1);
		}

		if(cross_validation != 0)
		{
			do_cross_validation(problem, param, nr_fold);
		}
		logger.info("cross validation completes");
		
		logger.info("start training the data set");
		svm_model model = svm.svm_train(problem, param);
		
		int predict_probability = 0;
		int svm_type=svm.svm_get_svm_type(model);
		int nr_class=svm.svm_get_nr_class(model);
		double[] prob_estimates=null;

		if(predict_probability == 1)
		{
			if(svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR){
			}
			else{
				int[] labels=new int[nr_class];
				svm.svm_get_labels(model,labels);
				prob_estimates = new double[nr_class];
				System.out.print("labels");
				for(int j=0;j<nr_class;j++)
					System.out.print(" "+labels[j]);
				System.out.print("\n");
			}
		}
		
		int correctness = 0;
		for (int i = 0; i < problem.l; i++){
			svm_node[] node = problem.x[i];
			double v;
			if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC)){
				v = svm.svm_predict_probability(model, node, prob_estimates);
				logger.debug(wordsArray[i]+ "--"+v+"/"+problem.y[i]);
				if (v * problem.y[i] > 0) correctness++;
			}else{
				v = svm.svm_predict(model, node);
				logger.debug(wordsArray[i]+ "--"+v+"/"+problem.y[i]);
				if (v * problem.y[i] > 0) correctness++;
			}
		}
		logger.info(String.format("correctness=%d, total=%d, rate=%f", correctness, problem.l, (double)correctness/problem.l));
	}
	
	public void init(){
		emoticonSearcher = EmoticonSearcher.getInstance(null);
		
		allPolarEmoticons = EmoticonClassify.getAllPolarEmoticons();
		
		List<String> emoticons = new ArrayList<String>(allPolarEmoticons.size());
		for (Iterator<Entry<String, Double>> it = allPolarEmoticons.entrySet().iterator(); it.hasNext();){
			emoticons.add(it.next().getKey());
		}
		
		emoticonWords = emoticons.toArray(new String[0]);
		attributeCount = emoticonWords.length;
		
		this.initEmoticonWordsCount(emoticonWords);
		//totalCount = this.countMapSum(wordsCount);
	}
	
	public SimpleMatrix createWordsMatrix(String[] words){
		double[][] data = new double[words.length][attributeCount];
		for (int i = 0; i < words.length; i++){
			data[i] = extractor1.extractorSentimentWordAttribute(words[i]);
		}
		
		/*scale*/
//		double lower = -1;
//		double upper = 1;
//		for (int i = 0; i < attributeCount; i++){
//			double max = -Double.MAX_VALUE;
//			double min = Double.MAX_VALUE;
//			for (int j = 0; j < words.length; j++){
//				max = Math.max(max, data[j][i]);
//				min = Math.min(min, data[j][i]);
//			}
//			
//			if (max == min) continue;
//			
//			for (int j = 0 ; j < words.length; j++){
//				data[j][i] = lower + (upper-lower) * (data[j][i]-min)/(max - min);
//			}
//		}
		
		SimpleMatrix matrix = new SimpleMatrix(data);
		
		return matrix;
	}
	
	public SimpleMatrix reductionMatrix(SimpleMatrix A, double gamma){
		SimpleSVD svd = A.svd();
		SimpleMatrix V = svd.getV();
		SimpleMatrix S = svd.getW();
		
		int n = Math.min(S.numCols(), S.numRows());
		double sum = 0;
		for (int i = 0; i < n; i++){
			sum += S.get(i, i);
			logger.debug("i={}, singleV={}", i, S.get(i, i));
		}
		
		double currentSum = 0;
		double threshold = sum * gamma;
		int k = 0;
		while (currentSum < threshold){
			currentSum += S.get(k, k);
			k++;
		}
		
		logger.info("k="+k);
		
		SimpleMatrix vk = V.extractMatrix(0, V.numRows(), 0, k);
		SimpleMatrix reductedMatrix = A.mult(vk);
		
		return reductedMatrix;
	}
	
	public Map<String, Integer> initEmoticonWordsCount(String[] emoticonWords){
		for (String emoticon : emoticonWords){
			int count = emoticonSearcher.getEmoticonCount(emoticon);
			emoticonsCount.put(emoticon, count);
		}
		return emoticonsCount;
	}
	public int countMapSum(Map<String, Integer> map){
		int sum = 0;
		for (Iterator<Entry<String, Integer>> it=map.entrySet().iterator(); it.hasNext();){
			sum += it.next().getValue();
		}
		return sum;
	}
	
	public List<String> readAllTrainingSet(String filePath){
		File file = new File(filePath);
		try{
			List<String> lines = Files.readLines(file, Constants.defaultCharset);
			logger.info("read training set -- num={}, file={}", lines.size(), file.getName());
			return lines;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return new ArrayList<String>();
	}
	
	public List<SampleEntry> parseTrainingSet(List<String> rawSet){
		List<SampleEntry> set = new ArrayList<SampleEntry>(rawSet.size());
		
		for (String line : rawSet){
			String[] pair = line.split(",");
			SampleEntry sample = new SampleEntry();
			sample.word = pair[0];
			if (pair.length > 1){
				sample.label = new Integer(pair[1]);
			}
			set.add(sample);
		}
		return set;
	}
	
	public svm_problem extractProblem(List<SampleEntry> set, SentimentWordAttributeExtractor extractor){
		svm_problem problem = new svm_problem();
		problem.l = set.size();
		problem.x = new svm_node[problem.l][];
		problem.y = new double[problem.l];
		
		for (int sampleIndex = 0; sampleIndex < problem.l; sampleIndex++){
			SampleEntry entry = set.get(sampleIndex);
			
			double[] attributes = extractor.extractorSentimentWordAttribute(entry.word);
			svm_node[] attributesNode = new svm_node[attributeCount];
			for (int i = 0;i < attributeCount; i++){
				attributesNode[i] = new svm_node();
				attributesNode[i].index = i;
				attributesNode[i].value = attributes[i];
			}
			problem.x[sampleIndex] = attributesNode;
			problem.y[sampleIndex] = entry.label;
			entry.index = sampleIndex;
		}
		
		if(param.gamma == 0 && attributeCount > 0)
			param.gamma = 1.0/attributeCount;

		if(param.kernel_type == svm_parameter.PRECOMPUTED){
			for(int i=0;i<prob.l;i++){
				if (prob.x[i][0].index != 0){
					System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
					System.exit(1);
				}
				if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > attributeCount){
					System.err.print("Wrong input format: sample_serial_number out of range\n");
					System.exit(1);
				}
			}
		}
		
		return problem;
	}
	
	public void run(String argv[]) throws IOException
	{
		searcher = new ContentSearcher(Constants.EMOTICON_INDEX);
		lexicon = Lexicon.getInstance();
		lexicon.readUntaggedSentimentWords();
		
		this.init();
		
		param = svm_parameter.parse_command_line(argv);
		param.C = 32768;
		param.gamma = 8;
		param.probability = 1;
		
		cross_validation = 0;
		nr_fold = 10;

		model_file_name = prefix + "svm-model.model";
		
		List<SampleEntry> set = this.parseTrainingSet(this.readAllTrainingSet(trainingFilePath));
		
		SentimentWordAttributeExtractor extractor1 = new SentimentWordAttributeExtractor(){
			public double[] extractorSentimentWordAttribute(String word) {
				double[] attributes = new double[attributeCount];
				int count = 0;
				for (String emoticonWord : emoticonWords){
					attributes[count++] = 
							(double)(searcher.search2Words(emoticonWord, word, 1000).totalHits + 1);
				}
				return attributes;
			}			
		};
		
		prob = this.extractProblem(set, extractor1);
		LibSVMUtil.writeProblemToFile(prob, prefix +"/problem.txt");
		
		System.out.println("find best c and g parameter!");
		LibSVMUtil.findBestCGParameter(prob, param);
		//LibSVMUtil.writeProblemToFile(prob, problem);
		
		error_msg = svm.svm_check_parameter(prob,param);

		if(error_msg != null)
		{
			System.err.print("ERROR: "+error_msg+"\n");
			System.exit(1);
		}

		if(cross_validation != 0)
		{
			do_cross_validation(prob, param, nr_fold);
		}
		else
		{
			model = svm.svm_train(prob,param);
			int predict_probability = 1;
			int svm_type=svm.svm_get_svm_type(model);
			int nr_class=svm.svm_get_nr_class(model);
			double[] prob_estimates=null;

			if(predict_probability == 1)
			{
				if(svm_type == svm_parameter.EPSILON_SVR || svm_type == svm_parameter.NU_SVR){
				}
				else{
					int[] labels=new int[nr_class];
					svm.svm_get_labels(model,labels);
					prob_estimates = new double[nr_class];
					System.out.print("labels");
					for(int j=0;j<nr_class;j++)
						System.out.print(" "+labels[j]);
					System.out.print("\n");
				}
			}
			
			for (int i = 0; i < prob.l; i++){
				svm_node[] node = prob.x[i];
				double v;
				if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC)){
					v = svm.svm_predict_probability(model, node, prob_estimates);
					System.out.print(set.get(i).word+ "--"+v+"/"+prob.y[i]);
					for(int j=0;j<nr_class;j++)
						System.out.print(prob_estimates[j]+" ");
					System.out.print("\n");
				}else{
					v = svm.svm_predict(model, node);
					System.out.print(v+"\n");
				}
			}
			
			List<String> words = lexicon.getSentimentWords(2000);
			for (String word : words){
				double[] attributes = extractor1.extractorSentimentWordAttribute(word);
				svm_node[] node = new svm_node[attributeCount];
				for (int i = 0; i<attributeCount;i++){
					node[i] = new svm_node();
					node[i].index = i;
					node[i].value = attributes[i];
				}
				double v;
				if (predict_probability==1 && (svm_type==svm_parameter.C_SVC || svm_type==svm_parameter.NU_SVC))
				{
					v = svm.svm_predict_probability(model, node, prob_estimates);
					System.out.print(word+ "--"+v+" ");
					for(int j=0;j<nr_class;j++)
						System.out.print(prob_estimates[j]+" ");
					System.out.print("\n");
				}else{
					v = svm.svm_predict(model, node);
					System.out.print(v+"\n");
				}
			}
			svm.svm_save_model(model_file_name, model);
		}
	}
	
	public void do_cross_validation(svm_problem prob, svm_parameter param, int nr_fold)
	{
		int i;
		int total_correct = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double[] target = new double[prob.l];

		svm.svm_cross_validation(prob,param,nr_fold,target);
		if(param.svm_type == svm_parameter.EPSILON_SVR ||
		   param.svm_type == svm_parameter.NU_SVR)
		{
			for(i=0;i<prob.l;i++)
			{
				double y = prob.y[i];
				double v = target[i];
				total_error += (v-y)*(v-y);
				sumv += v;
				sumy += y;
				sumvv += v*v;
				sumyy += y*y;
				sumvy += v*y;
			}
			System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
			System.out.print("Cross Validation Squared correlation coefficient = "+
				((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
				((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
				);
		}
		else
		{
			for(i=0;i<prob.l;i++)
				if(Math.abs(target[i] - prob.y[i]) < 0.1)
					++total_correct;
			System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
		}
	}
}
