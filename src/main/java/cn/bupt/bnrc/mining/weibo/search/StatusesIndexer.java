package cn.bupt.bnrc.mining.weibo.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;

public class StatusesIndexer extends ContentIndexer{

	private List<StringFilter> filters = new ArrayList<StringFilter>();
	
	public StatusesIndexer(){
		super(IndexSearchConstants.STATUS_INDEX);
		filters.add(StringFilterFactory.LENGTHFILTER);
		filters.add(StringFilterFactory.REMOVEILLEGALCHARFILTER);
	}
	public void indexString(IndexWriter writer, Map<String, Object> data) {
		try {
			String content = (String)data.get("content");
			for (StringFilter filter : filters){
				content = filter.filterAndHandle(content);
			}
			
			if (content != null) {
				Document doc = new Document();
				
				Field contentField = new TextField(IndexSearchConstants.CONTENT_FIELD,
						content, Field.Store.YES);
				doc.add(contentField);
				writer.addDocument(doc);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
