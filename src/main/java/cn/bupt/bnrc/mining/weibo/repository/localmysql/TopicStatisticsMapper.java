package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/*
 * create table topic_statistics(
	id bigint not null auto_increment,
	topic_id int not null,
	start_time datetime not null,
	end_time datetime not null,
	positive_count int not null default 0,
	negative_count int not null default 0,
	primary key(id)
)
 */
public interface TopicStatisticsMapper {

	@Insert("insert into topic_statistics(topic_id, start_time, end_time, positive_count, negative_count) " +
			" values(#{topic_id}, #{start_time}, #{end_time}, #{positive_count}, #{negative_count})")
	public void addTopicStatistics(Map<String, Object> params);
	
	@Select("select * from topic_statistics where" +
			" topic_id=#{topic_id} and " +
			" end_time <= #{end_time} " +
			" order by end_time desc")
	public List<Map<String, Object>> getTopicStatisticsRecently(Map<String, Object> params);
	
	@Select("select COALESCE(sum(positive_count), 0) as total_positive_count, " +
			" COALESCE(sum(negative_count), 0) as total_negative_count " +
			" from topic_statistics " +
			" where topic_id =#{topic_id} " +
			" and end_time <= #{end_time}")
	public Map<String, Object> getTopicTotalStatisticsBetweenTime(Map<String, Object> params);
	
	@Select("select COALESCE(sum(positive_count), 0) as total_positive_count, " +
			" COALESCE(sum(negative_count), 0) as total_negative_count " +
			" from topic_statistics " +
			" where topic_id =#{topic_id} ")
	public Map<String, Object> getTopicTotalStatistics(Map<String, Object> params);
}
