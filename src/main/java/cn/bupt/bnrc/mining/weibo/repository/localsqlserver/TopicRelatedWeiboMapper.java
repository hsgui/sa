package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;

/*
 * 	   [weibo_mid]
      ,[post_user]
      ,[post_time]
      ,[weight]
      ,[capture_time]
      ,[related_topic]
      ,[content]
      ,[is_trending]
  FROM [SinaWeiboTopicData].[dbo].[TopicRelatedWeibo]
 */
public interface TopicRelatedWeiboMapper {

	public List<Map<String, Object>> getTopicRelatedWeibo(Map<String, Object> params);
	
	@Select("select * from TopicRelatedWeibo " +
			" where post_time between #{start_time} and #{end_time}")
	public List<Map<String, Object>> getRelatedWeiboWithTime(Map<String, Object> params);
}
