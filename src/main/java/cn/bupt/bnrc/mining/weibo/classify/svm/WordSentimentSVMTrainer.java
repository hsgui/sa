package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.io.IOException;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.classify.Lexicon;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;

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
	private Map<String, Integer> wordsCount = new HashMap<String, Integer>();
	private int totalCount = 0;
	
	private ContentSearcher searcher = null;
	
	private Lexicon lexicon = null;
	
	private String prefix = Constants.RESOURCES_PREFIX +"/words/";
	private String trainingFilePath = prefix + "training-set.txt";
	
	public static void main(String argv[]) throws IOException
	{
		WordSentimentSVMTrainer t = new WordSentimentSVMTrainer();
		
		t.run(argv);
	}
	
	public void init(){
		this.initEmoticonWordsCount(emoticonWords);
		totalCount = this.countMapSum(wordsCount);
	}
	
	public Map<String, Integer> initEmoticonWordsCount(String[] positiveWords, String[] negativeWords){		
		for (String word : positiveWords){
			wordsCount.put(word, searcher.searchWord(word, 100).totalHits + 1);
		}
		for (String word : negativeWords){
			wordsCount.put(word, searcher.searchWord(word, 100).totalHits + 1);
		}
		
		return wordsCount;
	}
	
	public Map<String, Integer> initEmoticonWordsCount(String[] emoticonWords){
		for (String word : emoticonWords){
			int count = searcher.searchWord(word, 100).totalHits + 1;
			wordsCount.put(word, count);
		}
		return wordsCount;
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
		
		SentimentWordAttributeExtractor extractor = new SentimentWordAttributeExtractor(){
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
		SentimentWordAttributeExtractor extractor1 = new SentimentWordAttributeExtractor(){
			public double[] extractorSentimentWordAttribute(String word) {
				double[] attributes = new double[attributeCount];
				int count = 0;
				double sum = 0;
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
