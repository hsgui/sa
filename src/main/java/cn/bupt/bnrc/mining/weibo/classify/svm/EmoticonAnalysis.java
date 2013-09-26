package cn.bupt.bnrc.mining.weibo.classify.svm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.bupt.bnrc.mining.weibo.classify.Lexicon;
import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Constants;
import cn.bupt.bnrc.mining.weibo.util.Utils;

import com.google.common.io.Files;
import com.google.common.io.LineProcessor;

public class EmoticonAnalysis {

	private EmoticonSearcher searcher = null;
	private Lexicon lexicon = null;
	
	private Map<String, Double> sentimentWords = null;
	
	public EmoticonAnalysis(){
		searcher = EmoticonSearcher.getInstance(null);
		lexicon = Lexicon.getInstance();
		sentimentWords = lexicon.readUntaggedSentimentWords();
	}
	
	public HashMap<String, Integer> getAllEmoticonsCount(){
		String emoticonsPath = Constants.RESOURCES_PREFIX +"/statistics/emoticons-count.txt";
		File emoticonsCountFile = new File(emoticonsPath);
		if (emoticonsCountFile.exists()){
			return Utils.diskToHashMap(emoticonsCountFile);
		}
		
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		
		Set<String> emoticons = searcher.getAllEmoticonsInCorpus();
		for (String e : emoticons){
			int value = searcher.getEmoticonCount(e);
			count.put(e, value);
		}

		if (!emoticonsCountFile.exists()){
			Utils.hashMapToDisk(count, emoticonsPath);
		}
		
		return count;
	}
	
	public void computeEmoticons(){
		String emoticon = "哈哈";
		System.out.println(searcher.getCountWithOnlyThisEmoticon(emoticon));
		//HashMap<String, Integer> count = searcher.getEmoticonsOccurWithEmoticon(emoticon);
		//Utils.hashMapToDisk(count, new File("ssss"));
	}
	
	public void computePercentage(){
		HashMap<String, Integer> count = this.getAllEmoticonsCount();
		double totalCount = Utils.sumMap(count);
		double currentCount = 0;
		ArrayList<Entry<String, Integer>> sortedList = Utils.sortHashMap(count);
		for (Iterator<Entry<String, Integer>> it = sortedList.iterator(); it.hasNext();){
			Entry<String, Integer> entry = it.next();
			currentCount += entry.getValue();
			System.out.println(currentCount/totalCount);
		}
	}
	
	public <T extends Number> void computeMapInformation(HashMap<String, T> map){
		ArrayList<Entry<String, T>> sortedList = Utils.sortHashMap(map);
		double totalCount = Utils.sumMap(map);
		List<Double> informations = new ArrayList<Double>(sortedList.size());
		
		double average = 0;
		
		for (Iterator<Entry<String, T>> it = sortedList.iterator(); it.hasNext();){
			Entry<String, T> entry = it.next();
			double p = entry.getValue().doubleValue() / totalCount;
			double information = -Math.log(p);
			informations.add(information);
			average += p*information;
		}
		
		System.out.println("average information="+average);
		for (int i = 0; i < sortedList.size(); i++){
			if (informations.get(i) < average){
				System.out.println(sortedList.get(i).getKey() +"=" + informations.get(i));
			}
		}
	}
	
	public void computeEmoticonsInformation(){
		HashMap<String, Integer> count = this.getAllEmoticonsCount();
		this.computeMapInformation(count);
	}
	
	public void computeWordsInformation(){
		String trainingSetFilePath = Constants.RESOURCES_PREFIX +"/words/training-set.txt";
		try {
			HashMap<String, Integer> words = Files.readLines(new File(trainingSetFilePath), Constants.defaultCharset, 
					new LineProcessor<HashMap<String, Integer>>(){
				private HashMap<String, Integer> results = new HashMap<String, Integer>();
				public boolean processLine(String line) throws IOException {
					String[] word = line.split(",");
					results.put(word[0], new Integer(word[1]));
					return true;
				}
				public HashMap<String, Integer> getResult() {
					return results;
				}				
			});
			for (Iterator<Entry<String, Integer>> it = words.entrySet().iterator(); it.hasNext();){
				Entry<String, Integer> entry = it.next();
				HashMap<String, Integer> count = searcher.getEmoticonsOccurWithWord(entry.getKey());
				
				String filePath = "";
				if (entry.getValue() == 1){
					filePath = "positive/" + entry.getKey();
				}else{
					filePath = "negative/" + entry.getKey();
				}
				
				Utils.hashMapToDisk(count, filePath);
				
				System.out.println("\nword="+entry.getKey());
				this.computeMapInformation(count);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void testSentimentWords(){
		String word = "可怕";
		HashMap<String, Integer> count = searcher.getEmoticonsOccurWithWord(word);
		Utils.hashMapToDisk(count, new File("example"));
	}
	
	public static void main(String[] args){
		EmoticonAnalysis analysis = new EmoticonAnalysis();
		analysis.testSentimentWords();
	}
}
