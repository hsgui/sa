package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.classify.EmoticonClassify;
import cn.bupt.bnrc.mining.weibo.classify.Lexicon;
import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;
import cn.bupt.bnrc.mining.weibo.util.Utils;

public class ExperementsForWordsClassifyWithEmoticons {

private EmoticonSearcher emoticonSearcher = EmoticonSearcher.getInstance(null);
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private double smoothingFactor = 0.01;
	
	private HashMap<String, Double> positiveCount = new HashMap<String, Double>();
	private HashMap<String, Double> negativeCount = new HashMap<String, Double>();
	
	public static void main(String[] args){
		ExperementsForWordsClassifyWithEmoticons experiments = new ExperementsForWordsClassifyWithEmoticons();
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
			
			logger.info("read usingWords from Serializable file, size={}", usingWords.size());
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
		
		int pc = 0;
		int nc = 0;
		for (Iterator<Entry<String, Double>> it = usingWords.entrySet().iterator();it.hasNext();){
			Entry<String, Double> entry = it.next();
			logger.info("word={}, polar={}", entry.getKey(), entry.getValue());
			if (entry.getValue() < 0) nc++;
			else if (entry.getValue() > 0) pc++;
		}
		logger.info("pc={}, nc={}", pc, nc);
		
		List<String> positiveWords = new ArrayList<String>();
		List<String> negativeWords = new ArrayList<String>();
		
		HashMap<String, Double> polarEmoticons = EmoticonClassify.getAllPolarEmoticons();
		for (Iterator<Entry<String, Double>> it = polarEmoticons.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (entry.getValue() > 0) {
				positiveWords.add(entry.getKey());
			}
			else if (entry.getValue() < 0){
				negativeWords.add(entry.getKey());
			}
		}
		
		logger.info("positive words size={}, negative words size={}", positiveWords.size(), negativeWords.size());

		for (String emoticon : positiveWords){
			positiveCount.put(emoticon, emoticonSearcher.getEmoticonCount(emoticon)+0.0);
		}
		for (String emoticon : negativeWords){
			negativeCount.put(emoticon, emoticonSearcher.getEmoticonCount(emoticon)+0.0);
		}
		
		for (int k= 7; k < 50; k+=7){
			positiveCount = Utils.selectTopN(positiveCount, k);
			negativeCount = Utils.selectTopN(negativeCount, k);
			
			positiveWords = new ArrayList<String>();
			negativeWords = new ArrayList<String>();
			for (Iterator<Entry<String, Double>> it = positiveCount.entrySet().iterator(); it.hasNext();){
				positiveWords.add(it.next().getKey());
			}
			for (Iterator<Entry<String, Double>> it = negativeCount.entrySet().iterator(); it.hasNext();){
				negativeWords.add(it.next().getKey());
			}
			
			HashMap<String, Double> result = this.classifyWithPMIByEmoticons(usingWords, negativeWords, positiveWords);
			double correctness = this.computeTheCorrectness(result, usingWords);
			logger.info("name=pmi, correctness=" + correctness);
		}
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
	
	public HashMap<String, Double> classifyWithPMIByEmoticons(Map<String, Double> words, List<String> negative, List<String> positive){
		if (negative.size() != positive.size()){
			logger.error("negativeSize={}, positiveSize={}", negative.size(), positive.size());
			System.exit(-1);
		}
		
		HashMap<String, Double> result = new HashMap<String, Double>(words.size());
		double factor = 1.0;
		for (int i =0; i < negative.size(); i++){
			double nCount = negativeCount.get(negative.get(i));
			double pCount = positiveCount.get(positive.get(i));
			
			logger.trace(String.format("emoticon=%s, nCount=%f", negative.get(i), nCount));
			logger.trace(String.format("emoticon=%s, pCount=%f", positive.get(i), pCount));
			
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
			String pEmoticon = positive.get(i);
			String nEmoticon = negative.get(i);
			double pCount = emoticonSearcher.getWordAndEmoticonCount(word, pEmoticon) + this.smoothingFactor;
			double nCount = emoticonSearcher.getWordAndEmoticonCount(word, nEmoticon) + this.smoothingFactor;
			f *= pCount/nCount;
			
			logger.trace(String.format("word: %s, pEmoticon: %s, pCount: %f", word, pEmoticon, pCount));
			logger.trace(String.format("word: %s, nEmoticon: %s, nCount: %f", word, nEmoticon, nCount));
		}
		double pmi = Math.log(f*factor)/Math.log(2);
		
		logger.debug(String.format("word=%s, pmi=%.10f", word, pmi));
		
		return pmi;
	}
}
