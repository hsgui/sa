package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.classify.Lexicon;
import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;

public class ExperimentsForWordsClassify {

	private EmoticonSearcher emoticonSearcher = EmoticonSearcher.getInstance(null);
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private double smoothingFactor = 0.01;
	
	public static void main(String[] args){
		ExperimentsForWordsClassify experiments = new ExperimentsForWordsClassify();
		experiments.testPMI();
	}
	
	public void testPMI(){
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
			for (Iterator<Entry<String, Double>> it = words.entrySet().iterator(); it.hasNext();){
				Entry<String, Double> entry = it.next();
				int count = emoticonSearcher.getWordCount(entry.getKey());
				if (10 <= count){
					usingWords.put(entry.getKey(), entry.getValue());
				}
				
				logger.info(String.format("word=%s, count=%d", entry.getKey(), count));
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
		
		String[] negative = {"可怜", "委屈","鄙视", "失望", "伤心", "讨厌", "阴险"};
		String[] positive = {"开心", "幸福","快乐", "精彩", "美丽", "漂亮", "美好"};
		
		HashMap<String, Double> result = this.classifyWithPMI(usingWords, Arrays.asList(negative), Arrays.asList(positive));
		double correctness = this.computeTheCorrectness(result, usingWords);
		logger.info("name=pmi, correctness=" + correctness);
	}
	
	public double computeTheCorrectness(Map<String, Double> predictValues, Map<String, Double> trueValues){
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
		
		logger.info("correct={}, total={}", correct, trueValues.size());

		return (double)correct/(predictValues.size());
	}
	
	public HashMap<String, Double> classifyWithPMI(Map<String, Double> words, List<String> negative, List<String> positive){
		if (negative.size() != positive.size()){
			logger.error("negativeSize={}, positiveSize={}", negative.size(), positive.size());
			System.exit(-1);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>(words.size());
		double factor = 1.0;
		for (int i =0; i < negative.size(); i++){
			double nCount = emoticonSearcher.getWordCount(negative.get(i));
			double pCount = emoticonSearcher.getWordCount(positive.get(i));
			
			logger.trace(String.format("word=%s, nCount=%f", negative.get(i), nCount));
			logger.trace(String.format("word=%s, pCount=%f", positive.get(i), pCount));
			
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
		
		for(int i = 0; i < negative.size(); i++){
			String pWord = positive.get(i);
			String nWord = negative.get(i);
			double pCount = emoticonSearcher.getWordAndWordCount(pWord, word) + this.smoothingFactor;
			double nCount = emoticonSearcher.getWordAndWordCount(nWord, word) + this.smoothingFactor;
			f *= pCount/nCount;
			
			logger.trace(String.format("word: %s, pWord: %s, pCount: %f", word, pWord, pCount));
			logger.trace(String.format("word: %s, nWord: %s, nCount: %f", word, nWord, nCount));
		}
		double pmi = Math.log(f*factor)/Math.log(2);
		
		logger.debug(String.format("word=%s, pmi=%.10f", word, pmi));
		
		return pmi;
	}
}
