package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/*
 * create table positive_status(
	id bigint(11) not null,
	topic_id int not null,
	generate_time datetime not null,
	flag tinyint(4) default 1
)
 */
public interface PositiveStatusesMapper {

	@Insert("insert into positive_status(id, topic_id, generate_time) " +
			"values(#{id}, #{topic_id}, #{generate_time})")
	public void addPositiveStatus(Map<String, Object> params);
	
	@Select("select * from positive_status " +
			" where topic_id=#{topic_id} " +
			" and generate_time between #{start_time} and #{end_time}")
	public List<Map<String, Object>> getPositiveStatusesByTopicWithTime(Map<String, Object> params);
	
	@Select("select count(id) as positive_count from positive_status " +
			" where topic_id=#{topic_id} " +
			" and generate_time between #{start_time} and #{end_time}")
	public int getPositiveStatusesCountByTopicwithTime(Map<String, Object> params);
	
	@Select("select count(id) as total_positive_count from positive_status " +
			" where topic_id = #{topic_id}")
	public int getPositiveStatusesCountByTopic(Map<String, Object> params);
	
	/**
	 * params: {topic_id:1016, offset:1000, size:20}
	 * @param params
	 * @return
	 */
	@Select("select back_up_statuses.id as id, post_user, content from (select id from positive_status where id <= " +
			" (select id from positive_status where topic_id=#{topic_id} order by id desc limit #{offset},1) " +
			" and topic_id=#{topic_id} order by id desc) as temp, back_up_statuses " +
			" where temp.id = back_up_statuses.id limit #{size}")
	public List<Map<String, Object>> getPagedTopicPositiveStatuses(Map<String, Object> params);
}
