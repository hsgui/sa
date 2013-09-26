package cn.bupt.bnrc.mining.weibo.script;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bupt.bnrc.mining.weibo.classify.StatusesClassifier;
import cn.bupt.bnrc.mining.weibo.util.Constants;

public class TagStatusPolarityForLiu {

	private JdbcTemplate remoteJdbcTemplate;
	
	private StatusesClassifier classifier = new StatusesClassifier();
	
	public TagStatusPolarityForLiu(){
		BasicDataSource remoteDS = DataSourceFactory.getDataSource(DataSourceFactory.DATASOURCEType.remoteSqlserver);

		this.setRemoteJdbcTemplate(remoteDS);
	}
	
	public void setRemoteJdbcTemplate(DataSource remoteDataSource){
		this.remoteJdbcTemplate = new JdbcTemplate(remoteDataSource);
	}
	
	public void tagging(List<Map<String, Object>> statuses){
		System.out.println("get statuses complete -- size=" + statuses.size());
		
		String updateStr = "update weibo_quality set is_objective=?, is_positive=? where weibo_id=?";
		
		int updateCount = 0;
		for (Map<String, Object> status : statuses){
			String weiboMid = (String)status.get("weibo_mid");
			String content = (String)status.get("content");
			if (content == null || content.trim().isEmpty()) continue;
			
			int polarity = classifier.classify(content);
			if (polarity == Constants.OBJECTIVE){
				this.remoteJdbcTemplate.update(updateStr, new Object[]{true, false, weiboMid}, 
						new int[]{java.sql.Types.BOOLEAN, java.sql.Types.BOOLEAN, java.sql.Types.VARCHAR});
				updateCount++;
			}else{
				if (polarity == Constants.NEGATIVE_FLAG){
					this.remoteJdbcTemplate.update(updateStr, new Object[]{false, false, weiboMid}, 
							new int[]{java.sql.Types.BOOLEAN, java.sql.Types.BOOLEAN, java.sql.Types.VARCHAR});
					updateCount++;
				}else if (polarity == Constants.POSITIVE_FLAG){
					this.remoteJdbcTemplate.update(updateStr, new Object[]{false, true, weiboMid}, 
							new int[]{java.sql.Types.BOOLEAN, java.sql.Types.BOOLEAN, java.sql.Types.VARCHAR});
					updateCount++;
				}
			}
		}
		
		System.out.println("update statuses num=" + updateCount);
	}
	
	public void tagging(){
		String selectCount = "select count(weibo_mid) from topicrelatedweibo";
		int count = this.remoteJdbcTemplate.queryForInt(selectCount);
		System.out.println(count);
		
		int pageSize = 800000;
		
		String selectStr = "select top 800000 weibo_mid, content from topicrelatedweibo order by weibo_mid";
		List<Map<String, Object>> statuses;
		
		statuses = this.remoteJdbcTemplate.queryForList(selectStr);
		this.tagging(statuses);
		
		selectStr = "select top %d weibo_mid, content from topicrelatedweibo " +
				" where (weibo_mid > " +
				"			(select max(weibo_mid) from " +
				"				(select top %d weibo_mid from topicrelatedweibo order by weibo_mid) as T))";
		for (int pageNo = 1; pageNo <= 3; pageNo++){
			int offset = pageNo * pageSize;
			statuses = this.remoteJdbcTemplate.queryForList(String.format(selectStr, pageSize, offset));
			this.tagging(statuses);
		}
		
	}
	
	public static void main(String[] args){
		TagStatusPolarityForLiu tagger = new TagStatusPolarityForLiu();
		tagger.tagging();
	}

}
