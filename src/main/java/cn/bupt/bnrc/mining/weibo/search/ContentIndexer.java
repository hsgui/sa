package cn.bupt.bnrc.mining.weibo.search;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public abstract class ContentIndexer {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static boolean create = false;

	public static Directory dir = null;
	public static Analyzer analyzer = null;
	public static IndexWriterConfig iwc = null;
	public static IndexWriter indexWriter = null;
	
	private StatusesContentDao dao = null;
	
	public ContentIndexer(String indexDir) {
		try{
			dir = FSDirectory.open(new File(indexDir));
			analyzer = new StandardAnalyzer(Version.LUCENE_40);
			iwc = new IndexWriterConfig(Version.LUCENE_40, analyzer);
			if (create) {
				// Create a new index in the directory, removing any previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}
			indexWriter = new IndexWriter(dir, iwc);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public ContentIndexer(String indexDir, StatusesContentDao dao){
		this(indexDir);
		if (null == dao){
			logger.error("statuses dao is null!!!");
		}
		this.dao = dao;
	}
	
	public void setStatusesContentDao(StatusesContentDao dao){
		this.dao = dao;
	}
	
	//index the whole table in the dao.
	public void indexStatusDriver(){
		this.indexStatusDrvier(dao);
	}

	public void indexStatusDrvier(StatusesContentDao dao) {
		Date lastTime = new Date();
		int pageSize = 100000;
		String tableName = "hsgui_temp_table";
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("tempTableName", tableName);
		dao.createTempTable(params);
		Date currentTime = new Date();
		logger.info("create temp table: tableName={}, costTime={}", 
				new Object[]{tableName, currentTime.getTime() - lastTime.getTime()});
		
		List<Map<String, Object>> topNStatuses = null;
		Map<String, Object> topNParams = new HashMap<String, Object>();
		topNParams.put("tempTableName", tableName);
		topNParams.put("pageSize", pageSize);
		do{
			lastTime = currentTime;
			topNStatuses = dao.getTopNStatusContent(topNParams);
			currentTime = new Date();
			logger.info("get topN statuses: tableName={}, pageSize={}, costTime={}",
					new Object[]{tableName, pageSize, currentTime.getTime() - lastTime.getTime()});
			
			lastTime = currentTime;
			this.indexStringList(topNStatuses);
			currentTime = new Date();
			logger.info("index topN statuses: num={}, costTime={}", 
					new Object[]{topNStatuses.size(), currentTime.getTime() - lastTime.getTime()});
			
			lastTime = currentTime;
			dao.deleteTopNStatusContent(topNParams);
			currentTime = new Date();
			logger.info("delete topN statuses: num={}, costTime={}",
					new Object[]{topNStatuses.size(), currentTime.getTime() - lastTime.getTime()});
		}while (topNStatuses.size() == pageSize);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		lastTime = currentTime;
		dao.dropTempTable(params);
		currentTime = new Date();
		logger.info("drop table: tableName={}, costTime={}",
				tableName, currentTime.getTime() - lastTime.getTime());
	}
	
	public void indexStringList(List<Map<String, Object>> statuses){
		this.indexStringList(indexWriter, statuses);
		try {
			indexWriter.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void indexStringList(IndexWriter writer, List<Map<String, Object>> statuses){
		if (statuses == null){
			logger.info("indexStringList: statuses is empty!");
			return;
		}
		logger.info("start processing to index statuses -- size: {}", statuses.size());
		for (Map<String, Object> status : statuses){
			this.indexString(writer, status);
		}
	}
	
	public void indexString(Map<String, Object> content){
		this.indexString(indexWriter, content);
	}

	public abstract void indexString(IndexWriter writer, Map<String, Object> content);
}
