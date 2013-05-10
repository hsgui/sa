package cn.bupt.bnrc.mining.weibo.search;

import org.apache.lucene.index.IndexWriter;

public interface Indexable {

	public void indexString(IndexWriter writer, String content);
}
