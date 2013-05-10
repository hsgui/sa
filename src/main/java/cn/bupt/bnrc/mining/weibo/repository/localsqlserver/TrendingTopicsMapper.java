package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;

/*
 * 	   [topic_name]
      ,[capture_time]
      ,[rank]
      ,[hotness]
      ,[short_descp]
      ,[original_time]
      ,[first_user]
  FROM [SinaWeiboTopicData].[dbo].[TrendingTopics]
 */
public interface TrendingTopicsMapper {

	/*
	 * 这个时间可以有更大的跨度。比如天，不计较其小时:分钟:秒。
	 */
	@Select("select distinct(topic_name) from TrendingTopics " +
			" where original_time between #{start_time} and #{end_time}")
	public List<Map<String, Object>> getHotTopicsWithTime(Map<String, Object> params);
}
