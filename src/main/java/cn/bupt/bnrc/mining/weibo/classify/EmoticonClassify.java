package cn.bupt.bnrc.mining.weibo.classify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;
import cn.bupt.bnrc.mining.weibo.util.Utils;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class EmoticonClassify {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static Map<String, Double> polarEmoticons = new HashMap<String, Double>();
	
	private static HashMap<String, Double> allPolarEmoticons = new HashMap<String, Double>();
	
	private Map<String, Double> emoticonsInCopora = new HashMap<String, Double>();
	
	private EmoticonSearcher searcher;
	
	private double[][] matrix;
	private HashMap<String, Integer> word2Index;
	private HashMap<Integer, String> index2Word;
	
	private HashMap<String, Double> emoticonsCount = null;
	
	private double smoothingFactor = 0.01;
	
	private Random random = new Random();

	public static void main(String[] args){
		EmoticonClassify classify = new EmoticonClassify();
		classify.runTest();
	}
	
	public EmoticonClassify(){
		searcher = EmoticonSearcher.getInstance(null);
		EmoticonClassify.getPolarEmoticons();
		emoticonsInCopora = Emoticons.getEmoticonsInCopora();
		
		this.initMatrix(emoticonsInCopora);
		this.initEmoticonsCount(polarEmoticons);
	}
	
	public void initEmoticonsCount(Map<String, Double> emoticons){
		emoticonsCount = new HashMap<String, Double>();
		
		for (Iterator<Entry<String, Double>> it = emoticons.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			emoticonsCount.put(entry.getKey(), (double)searcher.getEmoticonCount(entry.getKey()));
		}
	}
	
	public void initMatrix(Map<String, Double> emoticons){
		word2Index = new HashMap<String, Integer>(emoticons.size());
		index2Word = new HashMap<Integer, String>(emoticons.size());
		int count = 0;
		for (Iterator<Entry<String, Double>> it = emoticons.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			word2Index.put(entry.getKey(), count);
			index2Word.put(count, entry.getKey());
			count++;
		}
		
		matrix = new double[emoticons.size()][emoticons.size()];
		for (int i = 0; i < emoticons.size(); i++){
			String e = index2Word.get(i);
			matrix[i][i] = searcher.getEmoticonCount(e);
			
			for (int j = i+1; j < emoticons.size(); j++){
				String other = index2Word.get(j);
				int occur = searcher.getEmoticonAndEmoticon(e, other);
				matrix[i][j] = occur;
				matrix[j][i] = occur;
			}
		}
	}
	
	public void classify(){
		Map<String, Double> polarEmoticons = EmoticonClassify.getPolarEmoticons();
		Map<String, Double> emoticonsInCopora = Emoticons.getEmoticonsInCopora();
		
		List<String> positiveWords = new ArrayList<String>();
		List<String> negativeWords = new ArrayList<String>();
		
		for (Iterator<Entry<String, Double>> it = polarEmoticons.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (!emoticonsInCopora.containsKey(entry.getKey())){
				logger.error("{} not exists in copora...", entry.getKey());
			}
			if (entry.getValue() == 2) {
				positiveWords.add(entry.getKey());
				polarEmoticons.put(entry.getKey(), 1.0);
			}
			else {
				negativeWords.add(entry.getKey());
				polarEmoticons.put(entry.getKey(), -1.0);
			}
		}
		logger.debug("tagged positive words num: {}", positiveWords.size());
		logger.debug("tagged negative words num: {}", negativeWords.size());
		
		List<String> selectedPWords = positiveWords;
		List<String> selectedNWords = this.randomSelectWords(negativeWords, selectedPWords.size());
		
		HashMap<String, Double> result = this.classifywithSeedsVector(polarEmoticons, selectedNWords, selectedPWords);
		double correctness = EmoticonClassify.computeTheCorrectness(result, polarEmoticons);
		logger.info("name=cosine, correctness="+correctness);
		
		result = this.classifywithSeedsVector(emoticonsInCopora, selectedNWords, selectedPWords);
		Utils.hashMapToDisk(result, "classifiedEmoticons");
	}
	
	public static HashMap<String, Double> getAllPolarEmoticons(){
		if (allPolarEmoticons != null && allPolarEmoticons.size() != 0) return allPolarEmoticons;
		
		String fileName = Constants.RESOURCES_PREFIX+"/data/" + "classifiedEmoticons";
		
		try {
			allPolarEmoticons = Files.readLines(new File(fileName), Constants.defaultCharset, new LineProcessor<HashMap<String, Double>>(){
				HashMap<String, Double> emoticons = new HashMap<String, Double>();
				public boolean processLine(String line) throws IOException {
					String[] pair = line.split(",");
					if (pair.length == 2){
						emoticons.put(pair[0], new Double(pair[1]));
					}
					return true;
				}
				public HashMap<String, Double> getResult() {
					return emoticons;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return allPolarEmoticons;
	}
	
	public void runTest(){
		Map<String, Double> polarEmoticons = EmoticonClassify.getPolarEmoticons();
		Map<String, Double> emoticonsInCopora = Emoticons.getEmoticonsInCopora();
		
		List<String> positiveWords = new ArrayList<String>();
		List<String> negativeWords = new ArrayList<String>();
		
		for (Iterator<Entry<String, Double>> it = polarEmoticons.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (!emoticonsInCopora.containsKey(entry.getKey())){
				logger.error("{} not exists in copora...", entry.getKey());
			}
			if (entry.getValue() == 2) {
				positiveWords.add(entry.getKey());
				polarEmoticons.put(entry.getKey(), 1.0);
			}
			else {
				negativeWords.add(entry.getKey());
				polarEmoticons.put(entry.getKey(), -1.0);
			}
		}
		logger.debug("tagged positive words num: {}", positiveWords.size());
		logger.debug("tagged negative words num: {}", negativeWords.size());
		
		for (int k = 5; k < 60; k += 5){
			int testNum = 100;
			double correctForPmi = 0.0;
			double correctForCos = 0.0;

			double correctness;
			for (int p = 0; p < testNum; p++){
				List<String> selectedPWords = this.randomSelectWords(positiveWords, k);
				List<String> selectedNWords = this.randomSelectWords(negativeWords, k);
				
				//HashMap<String, Double> result = this.classifyWithSeeds(polarEmoticons, selectedNWords, selectedPWords);			
				//correctness = EmoticonClassify.computeTheCorrectness(result, polarEmoticons);
				//correctForPmi += correctness;
				
				//logger.info(String.format("name=pmi,k=%d,p=%d,correctness=%.10f",k,p,correctness));
				
				HashMap<String, Double> result = this.classifywithSeedsVector(polarEmoticons, selectedNWords, selectedPWords);
				correctness = EmoticonClassify.computeTheCorrectness(result, polarEmoticons);
				correctForCos += correctness;
				
				logger.info(String.format("name=cos,k=%d,p=%d,correctness=%.10f",k,p,correctness));
			}
			
			//correctForPmi /= testNum;
			correctForCos /= testNum;
			
			logger.info(String.format("name=average,k=%d,pmi=%.10f,cos=%.10f", k, correctForPmi, correctForCos));
		}
	}
	
	public static double computeTheCorrectness(Map<String, Double> predictValues, Map<String, Double> trueValues){
		assert predictValues.size() == trueValues.size();
		
		int correct = 0;
		int incorrect = 0;
		for (Iterator<Entry<String, Double>> it = predictValues.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (entry.getValue() * trueValues.get(entry.getKey()) > 0){	//the same sign.
				correct += 1;
			}else{
				incorrect += 1;
			}
		}
		assert correct + incorrect == trueValues.size();

		return (double)correct/(predictValues.size());
	}
	
	public HashMap<String, Double> classifywithSeedsVector(Map<String, Double> words, List<String> negative, List<String> positive){
		HashMap<String, Double> result = new HashMap<String, Double>(words.size());
		
		for (Iterator<Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			String word = entry.getKey();
			
			result.put(word, this.classifyOneWordByVector2(word, negative, positive));
		}
		
		return result;
	}
	
	public double classifyOneWordByVector2(String word, List<String> negative, List<String> positive){
		double average = 0.0;
		
		int wordIndex = word2Index.get(word);
		double[] wv = matrix[wordIndex];
		for (int pi = 0; pi < positive.size(); pi++){
			average += cosine(wv, matrix[word2Index.get(positive.get(pi))]);
		}
		
		for (int ni = 0; ni < negative.size(); ni++){
			average -= cosine(wv, matrix[word2Index.get(negative.get(ni))]);
		}
		
		average = Math.signum(average);
		logger.debug("word={}, polar={}", word, average);
		
		return average;
	}
	
	public double cosine(double[] v1, double[] v2){
		double a = 0.0;
		double lengtha = 0.0;
		double lengthb = 0.0;
		for (int i = 0; i < v1.length; i++) a += v1[i]*v2[i];
		for (int i = 0; i < v1.length; i++) lengtha += v1[i]*v1[i];
		for (int i = 0; i < v2.length; i++) lengthb += v2[i]*v2[i];
		
		return a/(Math.sqrt(lengtha)*Math.sqrt(lengthb));
	}
	
	public List<String> randomSelectWords(List<String> words, int k){
		if (words.size() < k){
			logger.error(String.format("wordsSize={}, selectedNum={}", words.size(), k));
			System.exit(-1);
		}
		int num = words.size();
		List<String> selectedWords = new ArrayList<String>(k);
		
		for (int i = 0; i < num; i++){
			if ((Math.abs(random.nextInt()) % (num - i)) < k){
				selectedWords.add(words.get(i));
				k--;
			}
		}
		
		return selectedWords;
	}
	
	public HashMap<String, Double> classifyWithSeeds(Map<String, Double> words, List<String> negative, List<String> positive){
		if (negative.size() != positive.size()){
			logger.error("negativeSize={}, positiveSize={}", negative.size(), positive.size());
			System.exit(-1);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>(words.size());
		double factor = 1.0;
		for (int i =0; i < negative.size(); i++){
			double nCount = emoticonsCount.get(negative.get(i));
			double pCount = emoticonsCount.get(positive.get(i));
			
			factor *= nCount/pCount;
		}
		logger.info("factor={}", factor);
		
		for (Iterator<Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			String word = entry.getKey();
			
			result.put(word, this.classifyOneWord(word, negative, positive, factor));
		}
		
		return result;
	}
	
	public double classifyOneWord(String word, List<String> negative, List<String> positive, double factor){
		double f = 1.0;
		
		int wordIndex = word2Index.get(word);
		for(int i = 0; i < negative.size(); i++){
			String pWord = positive.get(i);
			String nWord = negative.get(i);
			double pCount = matrix[word2Index.get(pWord)][wordIndex] + this.smoothingFactor;
			double nCount = matrix[word2Index.get(nWord)][wordIndex] + this.smoothingFactor;
			f *= pCount/nCount;
			
			logger.trace(String.format("word: %s, pWord: %s, pCount: %f", word, pWord, pCount));
			logger.trace(String.format("word: %s, nWord: %s, nCount: %f", word, nWord, nCount));
		}
		double pmi = Math.log(f*factor)/Math.log(2);
		
		logger.debug(String.format("word=%s, pmi=%.10f", word, pmi));
		
		return pmi;
	}
	
	public static Map<String, Double> getPolarEmoticons(){
		if (polarEmoticons != null && polarEmoticons.size() != 0){
			return polarEmoticons;
		}
		
		String fileName = Constants.RESOURCES_PREFIX +"/data/polar-emoticons.txt";
		try {
			polarEmoticons = Files.readLines(new File(fileName), Constants.defaultCharset, new LineProcessor<Map<String, Double>>(){
				Map<String, Double> emoticons = new HashMap<String, Double>();
				public boolean processLine(String line) throws IOException {
					String[] pair = line.split(",");
					if (pair.length == 2){
						emoticons.put(pair[0], new Double(pair[1]));
					}
					return true;
				}
				public Map<String, Double> getResult() {
					return emoticons;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return polarEmoticons;
	}
	
	@Deprecated
	public double classifyOneWordByVector(String word, List<String> negative, List<String> positive){
		double average = 0.0;
		int vLength = negative.size() + positive.size();
		double[] wv = new double[vLength];
		for (int i= 0; i < wv.length; i++){
			if (i < negative.size()) wv[i] = matrix[word2Index.get(word)][word2Index.get(negative.get(i))];
			else wv[i] = matrix[word2Index.get(word)][word2Index.get(positive.get(i - negative.size()))];
		}
		for (int pi = 0; pi < positive.size(); pi++){
			double[] sv = new double[vLength];
			for (int i= 0; i < sv.length; i++){
				if (i < negative.size()) sv[i] = matrix[word2Index.get(positive.get(pi))][word2Index.get(negative.get(i))];
				else sv[i] = matrix[word2Index.get(positive.get(pi))][word2Index.get(positive.get(i - negative.size()))];
			}
			average += cosine(wv, sv);
		}
		
		for (int pi = 0; pi < negative.size(); pi++){
			double[] sv = new double[vLength];
			for (int i= 0; i < sv.length; i++){
				if (i < negative.size()) sv[i] = matrix[word2Index.get(negative.get(pi))][word2Index.get(negative.get(i))];
				else sv[i] = matrix[word2Index.get(negative.get(pi))][word2Index.get(positive.get(i - negative.size()))];
			}
			average -= cosine(wv, sv);
		}
		
		average = Math.tanh(average);
		logger.debug("word={}, polar={}", word, average);
		
		return average;
	}
}
