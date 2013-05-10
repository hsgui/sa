package cn.bupt.bnrc.mining.weibo.classify;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import cn.bupt.bnrc.mining.weibo.search.ContentSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;

import com.google.common.io.Files;

public class WordSentimentClassifier2 {
	
	private Logger logger = LoggerFactory.getLogger(getClass());

	public static String[] positiveWords = Arrays.copyOf(Emoticons.positiveEmoticonWords, 10);
	public static String[] negativeWords = Arrays.copyOf(Emoticons.negativeEmoticonWords, 10);
	
	public static Map<String, Integer> positiveWordsHits = new HashMap<String, Integer>();
	public static Map<String, Integer> negativeWordsHits = new HashMap<String, Integer>();
	
	private double factor = 1;
	private double smoothingFactor = 0.01;
	
	public static int POSITIVE = 10;
	public static int NEGATIVE = 11;
	public static int NEUTRAL = 12;
	public static int UNKOWN = 13;
	
	public static int attributeCount = positiveWords.length + negativeWords.length;
	
	private ContentSearcher searcher = null;
	
	private Lexicon lexicon = null;
	
	private String trainingFilePath = WordSentimentClassifier2.class.getClassLoader().getResource("").getPath() + 
			"/words/training-set.txt".substring(1);
	
	private Instances data = null;
	
	public static void main(String[] args){
		WordSentimentClassifier2 classifier = new WordSentimentClassifier2();
		classifier.run();
	}
	
	
	public WordSentimentClassifier2(){
		searcher = new ContentSearcher(Constants.EMOTICON_INDEX);
		lexicon = Lexicon.getInstance();
		lexicon.readUntaggedSentimentWords();
	}
	
	public Map<String, Integer> readTrainingSet(String filePath){
		Map<String, Integer> map = new HashMap<String, Integer>();
		
		File file = new File(filePath);
		if (!file.exists()){
			logger.info(String.format("file not exists -- %s", filePath));
			return null;
		}
		try {
			List<String> lines = Files.readLines(file, Constants.defaultCharset);
			for (String entry : lines){
				String[] keyValuePair = entry.split(",");
				map.put(keyValuePair[0], new Integer(keyValuePair[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return map;
	}
	
	public Classifier initModel(Map<String, Integer> trainingSet){
		FastVector atts;
		atts = new FastVector();
		for (int i = 0; i < attributeCount; i++){
			atts.addElement(new Attribute("p"+i));
		}
		
		FastVector classLabel = new FastVector(2); 
		classLabel.addElement("1"); 
		classLabel.addElement("2");
		Attribute polarity = new Attribute("polarity", classLabel);
		atts.addElement(polarity);
		
		data = new Instances("sentimentWords", atts, trainingSet.size());
		data.setClassIndex(data.numAttributes() - 1);
		
		for (Iterator<Entry<String, Integer>> it = trainingSet.entrySet().iterator(); it.hasNext();){
			Entry<String, Integer> entry = it.next();
			String sentimentWord = entry.getKey();
			int label = entry.getValue();
			double[] attributes = this.extractorSentimentWordAttribute(sentimentWord);
			
			Instance instance = new Instance(1.0, attributes);
			instance.setValue(polarity, label+"");
			
			data.add(instance);
		}
		
		Classifier cModel = (Classifier)new NaiveBayes();
		try {
			cModel.buildClassifier(data);
			Evaluation eTest = new Evaluation(data);
			eTest.evaluateModel(cModel, data);
			String strSummary = eTest.toSummaryString();
			System.out.println(strSummary);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return cModel;
	}
	
	public double[] extractorSentimentWordAttribute(String sentimentWord){
		double[] attributes = new double[attributeCount + 1];
		int count = 0;
		
		for (String word : positiveWords){
			attributes[count++] = searcher.search2Words(sentimentWord, word, 1000).totalHits + 1;
		}
		for (String word : negativeWords){
			attributes[count++] = searcher.search2Words(sentimentWord, word, 1000).totalHits + 1;
		}
		
		return attributes;
	}
	
	public void run(){
		Map<String, Integer> trainingSet = this.readTrainingSet(trainingFilePath);
		Classifier model = this.initModel(trainingSet);
		
		List<String> words = lexicon.getSentimentWords(2000);
		
		for (String word : words){
			if (!trainingSet.containsKey(word)){
				double[] attributes = this.extractorSentimentWordAttribute(word);
				Instance instance = new Instance(1.0, attributes);
				instance.setDataset(data);
				
				double[] fDistribution;
				try {
					fDistribution = model.distributionForInstance(instance);
					
					System.out.print("\nword: "+ word+", distribution: ");
					for (int i = 0; i < fDistribution.length; i++){
						System.out.print(fDistribution[i]+", ");
					}
					
					double label = model.classifyInstance(instance);
					System.out.print("label: "+label);
					instance.setClassValue(label);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
		}

	}
	
	public double classifierWord(String word){
		String[] positiveWords = this.getTaggedPositiveWords();
		String[] negativeWords = this.getTaggedNegativeWords();
		double f = 1.0;
		for(int i = 0; i < positiveWords.length; i++){
			String pWord = positiveWords[i];
			String nWord = negativeWords[i];
			double pCount = searcher.search2Words(pWord, word, 1000).totalHits + this.smoothingFactor;
			double nCount = searcher.search2Words(nWord, word, 1000).totalHits + this.smoothingFactor;
			f *= pCount/nCount;
			
			logger.info(String.format("word: %s, pWord: %s, pCount: %f", word, pWord, pCount));
			logger.info(String.format("word: %s, nWord: %s, nCount: %f", word, nWord, nCount));
		}
		double pmi = Math.log(f*factor)/Math.log(2);
		
		logger.info(String.format("word: %s, pmi: %.10f", word, pmi));
		
		return pmi;
	}
	
	public String[] getTaggedPositiveWords(){
		return positiveWords;
	}
	
	public String[] getTaggedNegativeWords(){
		return negativeWords;
	}
	
	public void initTaggedWordsHits(){
		String[] positiveWords = this.getTaggedPositiveWords();
		String[] negativeWords = this.getTaggedNegativeWords();
		if (positiveWords.length != negativeWords.length){
			logger.error("length(positiveWords) != length(negativeWords)");
			System.exit(-1);
		}
		
		for(int i = 0; i < positiveWords.length; i++){
			String pWord = positiveWords[i];
			String nWord = negativeWords[i];
			int pCount = searcher.searchWord(pWord, 1000).totalHits;
			int nCount = searcher.searchWord(nWord, 1000).totalHits;
			
			positiveWordsHits.put(pWord, pCount);
			negativeWordsHits.put(nWord, nCount);
			
			factor *= (double)nCount/pCount;
			
			String log = String.format("i: %d, pWord: %s, pCount: %d -- nWord: %s, nCount: %d -- factor: %.10f",
					i, pWord, pCount, nWord, nCount, factor);
			logger.info(log);
		}
		logger.info("nwords/pwords: {}", factor);
	}
	
	public Lexicon getLexicon(){
		return lexicon;		
	}
	
	public void setLexicon(Lexicon lexicon){
		this.lexicon = lexicon;
	}
}
