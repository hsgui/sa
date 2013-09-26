package cn.bupt.bnrc.mining.weibo.classify;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.ejml.simple.SimpleMatrix;

import cn.bupt.bnrc.mining.weibo.search.EmoticonSearcher;
import cn.bupt.bnrc.mining.weibo.util.Utils;

public class EmoticonAnalyzer {

	private EmoticonSearcher searcher;
	private int[][] matrix;
	private HashMap<String, Integer> word2Index;
	private HashMap<Integer, String> index2Word;
	
	private String[] positiveEmoticons = {"嘻嘻", "爱你", "good", "花心", "鼓掌", "赞", "给力", "酷"};
	private String[] negativeEmoticons = {"泪", "抓狂", "可怜", "怒", "汗", "衰", "晕", "生病"};
	
	private String[] emoticons = {"嘻嘻", "爱你", "good", "花心", "鼓掌", "赞", "给力", "酷", "泪", "抓狂", "可怜", "怒", "汗", "衰", "晕", "生病"};
	
	public static void main(String[] args){
		EmoticonAnalyzer a = new EmoticonAnalyzer();
		a.solveSOA();
	}
	
	public EmoticonAnalyzer(EmoticonSearcher searcher){
		this.searcher = searcher;
	}
	
	public EmoticonAnalyzer(){
		this.searcher = EmoticonSearcher.getInstance(null);
	}
	
	public void solveSOA(){
		double[][] m1 = new double[positiveEmoticons.length+negativeEmoticons.length][positiveEmoticons.length+negativeEmoticons.length];
		double[][] m2 = new double[positiveEmoticons.length+negativeEmoticons.length][positiveEmoticons.length+negativeEmoticons.length];
		
		int N = this.searcher.getDocumentCount();
		
		for (int i = 0; i < positiveEmoticons.length+negativeEmoticons.length; i++){
			for (int j = i; j < positiveEmoticons.length+negativeEmoticons.length; j++){
				if (j == i){
					m1[i][j] = this.searcher.getEmoticonCount(emoticons[i]);
				}else{
					m1[i][j] = this.searcher.getEmoticonAndEmoticon(emoticons[i], emoticons[j]);
					m1[j][i] = m1[i][j];
				}
			}
		}
		
		for (int i = 0; i < emoticons.length; i++){
			for (int j = 0; j < emoticons.length; j++){
				if (j == i){
					m2[i][j] = 0;
				}else{
					m2[i][j] = Math.log((double)N*m1[i][j]/((double)m1[i][i]*m1[j][j]))/Math.log(2);
					m2[j][i] = m2[i][j];
				}
			}
		}
		
		SimpleMatrix c = new SimpleMatrix(m1);
		c.print();		
		
		SimpleMatrix a = new SimpleMatrix(m2);
		SimpleMatrix b = new SimpleMatrix(emoticons.length, 1);
		b.set(0);
		
		a.print();
		
		try{
			SimpleMatrix x = a.solve(b);
			x.print();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void buildMatrix(){
		Set<String> emoticons = this.searcher.getAllEmoticonsInCorpus();
		word2Index = new HashMap<String, Integer>(emoticons.size());
		index2Word = new HashMap<Integer, String>(emoticons.size());
		int count = 0;
		for (String e : emoticons){
			word2Index.put(e, count);
			index2Word.put(count, e);
			count++;
		}
		
		matrix = new int[emoticons.size()][emoticons.size()];
		for (int i = 0; i < emoticons.size(); i++){
			String e = index2Word.get(i);
			int onlyCount = searcher.getCountWithOnlyThisEmoticon(e);
			HashMap<String, Integer> otherCount = searcher.getEmoticonsOccurWithEmoticon(e);
			matrix[i][i] = onlyCount;
			
			for (Iterator<Entry<String, Integer>> it = otherCount.entrySet().iterator(); it.hasNext();){
				Entry<String, Integer> entry = it.next();
				if (!entry.getKey().equals(e)){
					matrix[i][word2Index.get(entry.getKey())] = entry.getValue();
				}
			}
		}
		
		Formatter formatter = new Formatter(new StringBuilder());
		BufferedWriter save = null;
		try{
			save = new BufferedWriter(new FileWriter("matrix.txt"));
		}catch(Exception e){
			e.printStackTrace();
		}
		
		for (int i = 0; i < matrix.length; i++){
			for (int j = 0; j < matrix[i].length; j++){
				if (j == matrix[i].length - 1)
					formatter.format("%d\n", matrix[i][j]);
				else{
					formatter.format("%d,", matrix[i][j]);
				}
			}			
		}
		try {
			save.write(formatter.toString());
			save.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Utils.hashMapToDisk(word2Index, "word2Index.txt");
		
		return;
	}
	
}
