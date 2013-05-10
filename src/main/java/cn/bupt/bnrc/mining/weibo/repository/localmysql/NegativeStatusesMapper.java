package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/*
 * create table negative_status(
	id bigint(11) not null,
	topic_id int not null,
	generate_time datetime not null,
	flag tinyint(4) default 2
)
 */
public interface NegativeStatusesMapper {

	@Insert("insert into negative_status(id, topic_id, generate_time) " +
			"values(#{id}, #{topic_id}, #{generate_time})")
	public void addNegativeStatus(Map<String, Object> params);
	
	@Select("select * from negative_status " +
			" where topic_id=#{topic_id} " +
			" and generate_time between #{start_time} and #{end_time}")
	public List<Map<String, Object>> getNegativeStatusesByTopicWithTime(Map<String, Object> params);
	
	@Select("select count(id) as negative_count from negative_status " +
			" where topic_id=#{topic_id} " +
			" and generate_time between #{start_time} and #{end_time}")
	public int getNegativeStatusesCountByTopicwithTime(Map<String, Object> params);
	
	@Select("select count(id) as total_negative_count from positive_status " +
			" where topic_id = #{topic_id}")
	public int getNegativeStatusesCountByTopic(Map<String, Object> params);
	
	/**
	 * params: {offset:1000, size:20}
	 * @param params
	 * @return
	 */
	@Select("select back_up_statuses.id as id, post_user, content from (select id from negative_status where id <= " +
			" (select id from negative_status where topic_id=#{topic_id} order by id desc limit #{offset},1) " +
			" and topic_id=#{topic_id} order by id desc) as temp, back_up_statuses " +
			" where temp.id = back_up_statuses.id limit #{size}")
	public List<Map<String, Object>> getPagedTopicNegativeStatuses(Map<String, Object> params);
}
