package cn.bupt.bnrc.mining.weibo.search;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import cn.bupt.bnrc.mining.weibo.util.Utils;

public class EmoticonSearcher{

	//related to the number of record in the database.
	public static int MAX_SIZE = 1000000;
	
	private DirectoryReader reader;
	private IndexSearcher searcher;
	private Analyzer analyzer;
	private QueryParser parser;
	
	private static final TopDocs EmptyDocs = new TopDocs(0, null, 0);

	private static EmoticonSearcher emoticonSearcher = null;
	
	public static EmoticonSearcher getInstance(String indexDir){
		if (emoticonSearcher == null){
			if (null == indexDir)
				indexDir = IndexSearchConstants.EMOTICON_INDEX;
			emoticonSearcher = new EmoticonSearcher(indexDir);
		}
		
		return emoticonSearcher;
	}
	
	private EmoticonSearcher(){
		this(IndexSearchConstants.EMOTICON_INDEX);
	}
	
	private EmoticonSearcher(String indexDir) {
		try {
			reader = DirectoryReader.open(FSDirectory.open(new File(indexDir)));
			searcher = new IndexSearcher(reader);
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			parser = new QueryParser(Version.LUCENE_40, IndexSearchConstants.CONTENT_FIELD, analyzer);
			
			parser.setDefaultOperator(QueryParser.Operator.AND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public TopDocs searchWord(String word, int n){		
		try {
			String qWordString = String.format("%s:\"%s\"", IndexSearchConstants.CONTENT_FIELD, word);
			Query qWord = parser.parse(qWordString);
			return searcher.search(qWord, n);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return EmptyDocs;
	}
	
	public TopDocs searchEmoticon(String emoticon, int n){
		try {
			TermQuery q = new TermQuery(new Term(IndexSearchConstants.EMOTICON_FIELD, emoticon));
			
			return searcher.search(q, n);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return EmptyDocs;
	}
	
	public TopDocs searchWordAndEmoticon(String word, String emoticon, int n){
		try {
			String qWordString = String.format("%s:\"%s\"", IndexSearchConstants.CONTENT_FIELD, word);

			TermQuery qEmoticon = new TermQuery(new Term(IndexSearchConstants.EMOTICON_FIELD, emoticon));
			Query qWord = parser.parse(qWordString);
			
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(qEmoticon, BooleanClause.Occur.MUST);
			booleanQuery.add(qWord, BooleanClause.Occur.MUST);
			
			return searcher.search(booleanQuery, n);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return EmptyDocs;
	}
	
	public TopDocs searchWordAndWord(String word1, String word2, int n){
		if (word1.equals(word2)) return this.searchWord(word1, n);
		
		try{
			String qWordString1 = String.format("%s:\"%s\"", IndexSearchConstants.CONTENT_FIELD, word1);
			String qWordString2 = String.format("%s:\"%s\"", IndexSearchConstants.CONTENT_FIELD, word2);
			
			Query q1 = parser.parse(qWordString1);
			Query q2 = parser.parse(qWordString2);
			
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(q1, BooleanClause.Occur.MUST);
			booleanQuery.add(q2, BooleanClause.Occur.MUST);
			
			return searcher.search(booleanQuery, n);
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return EmptyDocs;
	}
	
	public TopDocs searchEmoticonAndEmoticon(String emoticon1, String emoticon2, int n){
		try {
			TermQuery qEmoticon1 = new TermQuery(new Term(IndexSearchConstants.EMOTICON_FIELD, emoticon1));
			TermQuery qEmoticon2 = new TermQuery(new Term(IndexSearchConstants.EMOTICON_FIELD, emoticon2));
			
			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.add(qEmoticon1, BooleanClause.Occur.MUST);
			booleanQuery.add(qEmoticon2, BooleanClause.Occur.MUST);
			
			return searcher.search(booleanQuery, n);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return EmptyDocs;
	}
	
	public HashMap<String, Integer> getEmoticonsOccurWithWord(String word){
		int max = 1000000;
		HashMap<String, Integer> emoticons = new HashMap<String, Integer>();
		TopDocs docs = this.searchWord(word, max);
		int num = Math.min(max, docs.totalHits);
		try{
			for (int i = 0; i <  num; i++){
				Document d = this.searcher.doc(docs.scoreDocs[i].doc);
				String[] values = d.getValues(IndexSearchConstants.EMOTICON_FIELD);
				for (String v : values){
					int newV = (emoticons.get(v) == null ? 0: emoticons.get(v)) + 1;
					emoticons.put(v, newV);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return emoticons;		
	}
	
	public int getCountWithOnlyThisEmoticon(String emoticon){
		int max = 600000;
		TopDocs docs = this.searchEmoticon(emoticon, max);
		int num = Math.min(max, docs.totalHits);
		int count = 0;
		try{
			for (int i = 0; i < num; i++){
				Document d = this.searcher.doc(docs.scoreDocs[i].doc);
				String[] values = d.getValues(IndexSearchConstants.EMOTICON_FIELD);
				count += values.length == 1? 1:0;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return count;
	}
	
	public HashMap<String, Integer> getEmoticonsOccurWithEmoticon(String emoticon){
		int max = 600000;
		TopDocs docs = this.searchEmoticon(emoticon, max);
		int num = Math.min(max, docs.totalHits);
		HashMap<String, Integer> emoticons = new HashMap<String, Integer>();
		
		try{
			for (int i = 0; i < num; i++){
				Document d = this.searcher.doc(docs.scoreDocs[i].doc);
				String[] values = d.getValues(IndexSearchConstants.EMOTICON_FIELD);
				for (String v : values){
					int newV = (emoticons.get(v) == null ? 0: emoticons.get(v)) + 1;
					emoticons.put(v, newV);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return emoticons;
	}
	
	public int getWordAndWordCount(String word1, String word2){
		if (word1.equals(word2)){
			return this.getWordCount(word1);
		}else{
			return this.searchWordAndWord(word1, word2, 1).totalHits;
		}
	}
	
	public int getWordAndEmoticonCount(String word, String emoticon){
		return this.searchWordAndEmoticon(word, emoticon, 1).totalHits;
	}
	
	public int getEmoticonCount(String emoticon){
		return this.searchEmoticon(emoticon, 1).totalHits;
	}
	
	public int getWordCount(String word){
		return this.searchWord(word, 1).totalHits;
	}
	
	public int getEmoticonAndEmoticon(String e1, String e2){
		return this.searchEmoticonAndEmoticon(e1, e2, 1).totalHits;
	}
	
	public int getDocumentCount(){
		return reader.numDocs();
	}
	
	public Set<String> getAllEmoticonsInCorpus(){
		Set<String> emoticons = new HashSet<String>();
		try {
			Terms terms = MultiFields.getTerms(reader, IndexSearchConstants.EMOTICON_FIELD);
			TermsEnum termsEnum = terms.iterator(null);
			
		    BytesRef text;
		    while((text = termsEnum.next()) != null) {
		    	emoticons.add(text.utf8ToString());
		    }
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return emoticons;
	}
	
	public void printTopDocs(TopDocs docs){
		int num = 100;
		
		for (int i = 0; i< Math.min(num, docs.totalHits); i++){
			int docId = docs.scoreDocs[i].doc;
			String content;
			try {
				content = this.searcher.doc(docId).get("content");
				System.out.println(content);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void testGetEmoticonsOccurWithWord(){
		this.getEmoticonsOccurWithWord("扭曲");
	}
	
	public void test(){
		Set<String> es = getAllEmoticonsInCorpus();
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		
		for (String e : es){
			TopDocs docs = this.searchEmoticon(e, 10);
			System.out.printf("emoticon=%s, count=%d\n", e, docs.totalHits);
			counts.put(e, docs.totalHits);
		}
		Utils.arrayToDisk(Utils.sortHashMap(counts), new File("emoticons-count"));
	}
	
	public void testSearchEmoticon(){
		int num = 100;
		TopDocs docs = this.searchEmoticon("钓鱼", 1000);
		System.out.println(docs.totalHits);
		
		for (int i = 0; i< Math.min(num, docs.totalHits); i++){
			int docId = docs.scoreDocs[i].doc;
			String content;
			try {
				content = this.searcher.doc(docId).get("content");
				System.out.println(content);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void testSearchWordAndEmoticon(){
		int num = 100;
		TopDocs docs = this.searchWordAndEmoticon("扭曲", "哈哈", 1000);
		System.out.println(docs.totalHits);
		
		for (int i = 0; i< Math.min(num, docs.totalHits); i++){
			int docId = docs.scoreDocs[i].doc;
			String content;
			try {
				content = this.searcher.doc(docId).get("content");
				System.out.println(content);
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public static void main(String[] args){
		EmoticonSearcher searcher = new EmoticonSearcher(IndexSearchConstants.EMOTICON_INDEX);

//		System.out.println(searcher.getCountWithOnlyThisEmoticon("心"));
		TopDocs docs = searcher.searchWord("鲤鱼跳龙门", 10);
		
		System.out.println(docs.totalHits);
		searcher.printTopDocs(docs);
		
//		HashMap<String, Integer> occur = searcher.getEmoticonsOccurWithEmoticon("嘻嘻");
//		Utils.hashMapToDisk(occur, "xixi.txt");
	}
}
