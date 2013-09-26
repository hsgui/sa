package cn.bupt.bnrc.mining.weibo.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bupt.bnrc.mining.weibo.classify.Emoticons;
import cn.bupt.bnrc.mining.weibo.script.DataSourceFactory;
import cn.bupt.bnrc.mining.weibo.util.Utils;

public class EmoticonsIndexer extends ContentIndexer{

	private List<StringFilter> filters = new ArrayList<StringFilter>();
	
	public EmoticonsIndexer(){
		super(IndexSearchConstants.EMOTICON_INDEX);
		
		filters.add(StringFilterFactory.EMOTICONFILTER);
		filters.add(StringFilterFactory.LENGTHFILTER);		
	}
	
	@Override
	public void indexString(IndexWriter writer, Map<String, Object> data) {
		try {
			String content = (String)data.get("content");
			for (StringFilter filter : filters){
				content = filter.filterAndHandle(content);
			}
			
			boolean isAddEmoticon = false;			
			if (content != null) {
				Document doc = new Document();
				
				Set<String> emoticons = Utils.emoticonsInContent(content);
				for (String e : emoticons){
					if (Emoticons.getEmoticonsInWeibo().contains(e)){
						Field emoticon = new StringField(IndexSearchConstants.EMOTICON_FIELD,
								e, Field.Store.YES);						
						doc.add(emoticon);
						
						isAddEmoticon = true;
					}
				}
				
				if (isAddEmoticon){
					Field contentField = new TextField(IndexSearchConstants.CONTENT_FIELD,
							content, Field.Store.YES);
					doc.add(contentField);
					
					writer.addDocument(doc);
				}				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static class EmoticonStatusesContentImp implements StatusesContentDao{

		private DataSource ds = null;
		private JdbcTemplate template = null;
		public EmoticonStatusesContentImp(DataSource source){
			this.ds = source;
			this.template = new JdbcTemplate(ds);
		}
		
		public void createTempTable(Map<String, Object> params) {
			String sql = String.format("create table %s select * from statuses", (String)params.get("tempTableName"));
			
			this.template.execute(sql);
		}

		public List<Map<String, Object>> getTopNStatusContent(Map<String, Object> params) {
			String tableName = (String)params.get("tempTableName");
			int pageSize = (Integer)params.get("pageSize");
			String sql = String.format("select * from %s order by status_id limit %d", tableName, pageSize);
			
			return this.template.queryForList(sql);
		}

		public void deleteTopNStatusContent(Map<String, Object> params) {
			String tableName = (String)params.get("tempTableName");
			int pageSize = (Integer)params.get("pageSize");
			String sql = String.format("delete from %s order by status_id limit %d", tableName, pageSize);
			
			this.template.execute(sql);
		}

		public void dropTempTable(Map<String, Object> params) {
			String sql = String.format("drop table %s", (String)params.get("tempTableName"));
			
			this.template.execute(sql);
		}		
	}
	
	public static void main(String[] args){
		ContentIndexer indexer = new EmoticonsIndexer();
		indexer.indexStatusDrvier(new EmoticonStatusesContentImp(DataSourceFactory.getDataSource(
				DataSourceFactory.DATASOURCEType.localHostMysql)));
	}
}
