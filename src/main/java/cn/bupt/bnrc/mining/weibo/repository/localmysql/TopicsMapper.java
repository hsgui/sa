package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/*
 * create table topics(
id int not null auto_increment,
name varchar(255) not null,
start_time datetime not null,
end_time datetime not null,
total_positive_count int default 0,
total_negative_count int default 0,
primary key(id),
unique key(name)
)
 */
public interface TopicsMapper {
	
	@Insert("insert ignore into topics(name, start_time, end_time) " +
			" values(#{topic_name}, #{start_time}, #{end_time})")
	@Options(useGeneratedKeys=true, keyProperty="topic_id")
	public void addTopic(Map<String, Object> params);
	
	@Select("select * from topics " +
			" where end_time > #{end_time} and total_positive_count > 0 and total_negative_count > 0 " +
			" order by end_time desc " +
			" limit #{count}")
	public List<Map<String, Object>> getRecentlyHotTopics(Map<String, Object> params);
	
	@Select("select * from topics " +
			" where id = #{topic_id}")
	public Map<String, Object> getTopicInfo(Map<String, Object> params);
	
	@Select("select * from topics where name=#{topic_name}")
	public Map<String, Object> getTopicWithName(Map<String, Object> params);
	
	@Update("update topics set total_positive_count=#{total_positive_count}, total_negative_count=#{total_negative_count}" +
			" where id = #{topic_id}")
	public void updateTopicPosNegCount(Map<String, Object> params);
	
	@Update("update topics set end_time=#{end_time} " +
			" where id = #{topic_id}")
	public void updateTopicLastUpdatedTime(Map<String, Object> params);
	
	@Update("update topics set total_positive_count=#{total_positive_count}, total_negative_count=#{total_negative_count}," +
			" end_time = #{end_time} " +
			" where id = #{topic_id}")
	public void updateTopicInformation(Map<String, Object> params);
}