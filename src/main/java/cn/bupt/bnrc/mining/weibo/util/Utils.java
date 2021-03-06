package cn.bupt.bnrc.mining.weibo.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.bupt.bnrc.mining.weibo.weka.Constants;

import com.google.common.io.Files;

public class Utils {

	private static Logger logger = LoggerFactory.getLogger(Utils.class);
	
	private static Random random = new Random();
	
	public static Charset defaultCharset = Charset.forName("utf-8");
	
	public static String emoticonRegexStr = "\\[([a-zA-Z\u4e00-\u9FA5]+?)\\]";
	public static final Pattern emoticonsPattern = Pattern.compile(emoticonRegexStr);
	
	public static String anyEmoticonRegex = "\\[[a-zA-Z\u4e00-\u9FA5]+?\\]";
	public static final Pattern anyEmoticonPattern = Pattern.compile(anyEmoticonRegex);
	
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String[] args){
		Set<String> emoticons = new HashSet<String>();
		emoticons.add("嘻嘻");
		System.out.println(Utils.isContainAnyEmoticons("生活更精彩[嘻嘻]"));
		System.out.println(Utils.isContainEmoticons("生活更精彩[嘻嘻]", emoticons));
		
		Set<String> emoticons1 = Utils.emoticonsInContent("生活更精彩[嘻嘻],[精彩]");
		for (String e : emoticons1){
			System.out.println(e);
		}
	}
	
	public static Date getDateBefore(Date d,int day){
		Calendar now =Calendar.getInstance();
		now.setTime(d);
		now.set(Calendar.DATE,now.get(Calendar.DATE)-day);
		return now.getTime();
	}
	
	//this method has some bug..
	//eg. "[给力 对名字很纠结的兔子h 威武]", can't match [..], 当然不能匹配，因为不能有其他非中文的字符.
	public static boolean isContainEmoticons(String string, Set<String> emoticons){
		Matcher m = emoticonsPattern.matcher(string);
		while (m.find()){
			if(emoticons.contains(m.group(1))) return true;
		}
		return false;
	}
	
	public static boolean isContainAnyEmoticons(String string){
		return anyEmoticonPattern.matcher(string).find();
	}
	
	public static Set<String> emoticonsInContent(String content){
		Matcher m = emoticonsPattern.matcher(content);
		Set<String> emoticons = new HashSet<String>();
		while (m.find()){
			emoticons.add(m.group(1));
		}
		return emoticons;
	}
	
	public static List<String> randomSelectWords(List<String> words, int k){
		if (words.size() < k){
			System.exit(-1);
		}
		if (words.size() == k) return new ArrayList<String>(words);
		
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
	
	public double maxValue(HashMap<String, Double> map){
		double max = -1;
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			if (max < entry.getValue()) max = entry.getValue();
		}
		return max;
	}
	
	public static <T extends Number> double sumMap(HashMap<String, ? extends T> map){
		double sum = 0;
		for (Iterator<? extends Entry<String, ? extends T>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, ? extends T> entry = it.next();
			sum += entry.getValue().doubleValue();
		}
		return sum;
	}
	
	public static void normalization(HashMap<String, Double> map){
		double sum = sumMap(map);
		for (Iterator<Entry<String, Double>> it = map.entrySet().iterator(); it.hasNext();){
			Entry<String, Double> entry = it.next();
			entry.setValue(entry.getValue()/sum);
		}
	}
	
	public static List<String> extractWordNeigbors(String content, String word){
		List<String> contexts = new ArrayList<String>();
		String regex = Utils.wordToRegex(word);
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()){
			contexts.add(matcher.group(1) + " " + matcher.group(2));
		}
		return contexts;
	}
	
	public static String contextToRegex(String context){
		return context.charAt(0) + "([a-zA-Z\u4e00-\u9FA5&&[^"+context.charAt(0)+"]]{1,4}?)" + context.charAt(2);
	}
	
	public static String wordToRegex(String word){
		return "([\u4e00-\u9FA5])" + word + "([\u4e00-\u9FA5])";
	}
	
	public static void updateHashMapValue(HashMap<String, Integer> map, String key, Integer delta){
		Integer previous = map.get(key);
		previous = (previous == null)?delta:(previous + delta);
		map.put(key, previous);
	}
	
	public static <T extends Number> void arrayToDisk(ArrayList<Entry<String, T>> array, File file){
		if (file.exists()){
			try {
				Files.move(file, new File(file.getName()+new Date().toString()));
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("arrayToDisk -- write to file: {} -- start", file.getName());
		try{
			for (Entry<String, T> entry : array){
				Files.append(entryToString(entry), file, Utils.defaultCharset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("arrayToDisk -- write to file: {}, size: {} -- complete", file.getName(), array.size());
	}
	
	/**
	 * write to file, maybe serialization.
	 * I use the Generics, so I can write all the Number classes.
	 * @param hashMap
	 * @param file
	 */
	public static <T extends Number> void hashMapToDisk(HashMap<String, T> hashMap, File file){
		logger.info("hashMapToDisk -- write to file: {} -- start", file.getName());
		ArrayList<Entry<String, T>> array = sortHashMap(hashMap);
		try{
			for (Entry<String, T> entry : array){
				Files.append(entryToString(entry), file, Utils.defaultCharset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("hashMapToDisk -- write to file: {} -- complete", file.getName());
	}
	
	public static <T extends Number> void hashMapToDisk(HashMap<String, T> hashMap, String fileName){
		logger.info("hashMapToDisk -- write to file: {} -- start", fileName);
		ArrayList<Entry<String, T>> array = sortHashMap(hashMap);
		fileName = fileName+"-"+(new Date().getTime());
		File file = new File(fileName);
		try{
			file.createNewFile();
			for (Entry<String, T> entry : array){
				Files.append(entryToString(entry), file, Utils.defaultCharset);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		logger.info("hashMapToDisk -- write to file: {} -- complete", file.getName());
	}
	
	/**
	 * when read data from the file, all the number will be regarded as Integer default.
	 * so, don't use it if you don't known about it.
	 * @param file
	 * @return
	 */
	public static HashMap<String, Integer> diskToHashMap(File file){
		logger.info("diskToHashMap -- read file: {} -- start", file.getName());
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		try {
			List<String> lines = Files.readLines(file, defaultCharset);
			for (String entry : lines){
				String[] keyValuePair = entry.split(",");
				map.put(keyValuePair[0], new Integer(keyValuePair[1]));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("diskToHashMap -- read file: {}, map size: {} -- finish", file.getName(), map.size());
		
		return map;
	}
	
	public static <T extends Number> ArrayList<Entry<String, T>> sortHashMap(HashMap<String, T> hashMap){
		ArrayList<Entry<String, T>> array = new ArrayList<Entry<String, T>>(hashMap.entrySet());
		Collections.sort(array, new Comparator<Entry<String, ? extends Number>>(){
				public int compare(Entry<String, ? extends Number> o1, Entry<String, ? extends Number> o2){
					if (o2.getValue().doubleValue() == o1.getValue().doubleValue()) return 0;
					return (o2.getValue().doubleValue() > o1.getValue().doubleValue())?1:-1;
				}
		});
		return array;
	}
	
	public static <T extends Number> ArrayList<Entry<String, T>> sortHashMapInverse(HashMap<String, T> hashMap){
		ArrayList<Entry<String, T>> array = new ArrayList<Entry<String, T>>(hashMap.entrySet());
		Collections.sort(array, new Comparator<Entry<String, ? extends Number>>(){
				public int compare(Entry<String, ? extends Number> o1, Entry<String, ? extends Number> o2){
					if (o2.getValue().doubleValue() == o1.getValue().doubleValue()) return 0;
					return (o2.getValue().doubleValue() < o1.getValue().doubleValue())?1:-1;
				}
		});
		return array;
	}
	
	public static HashMap<String, Double> selectTopN(HashMap<String, Double> map, int n){
		if (n >= map.size()) return map;
		HashMap<String, Double> topN = new HashMap<String, Double>(n);
		ArrayList<Entry<String, Double>> sortedTopN = sortHashMap(map);
		for (int i = 0; i < n; i++){
			Entry<String, Double> entry = sortedTopN.get(i);
			topN.put(entry.getKey(), entry.getValue());
		}
		return topN;
	}
	
	public static void merge(HashMap<String, Integer> total, HashMap<String, Integer> part){
		Set<Entry<String, Integer>> entrySet = part.entrySet();
		for (Iterator<Entry<String, Integer>> it = entrySet.iterator();it.hasNext();){
			Entry<String, Integer> entry = it.next();
			String key = entry.getKey();
			Integer value = entry.getValue();
			Integer previous = total.get(key);
			previous = previous==null?value:(previous+value);
			total.put(key, previous);
		}
	}
	
	public static <T extends Number> String entryToString(Entry<String, T> entry){
		return entry.getKey()+","+entry.getValue()+Constants.lineSeparator;
	}
}
