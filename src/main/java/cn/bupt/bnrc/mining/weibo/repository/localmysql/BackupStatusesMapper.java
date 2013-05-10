package cn.bupt.bnrc.mining.weibo.repository.localmysql;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

/*
 * create table back_up_statuses(
	id bigint(11) not null auto_increment,
	weibo_mid varchar(255) not null,
	post_user varchar(255) not null,
	post_time datetime not null,
	related_topic_id int not null,
	content varchar(255) not null,
	primary key(id)
)
 */
public interface BackupStatusesMapper {

	@Insert("insert into back_up_statuses(weibo_mid, post_user, post_time, related_topic_id, content) " +
			" values(#{weibo_mid}, #{post_user}, #{post_time}, #{related_topic_id}, #{content})")
	@Options(useGeneratedKeys=true, keyProperty="status_id")
	public void addStatus(Map<String, Object> params);
	
	@Select("select exists (select weibo_mid from back_up_statuses where weibo_mid=#{weibo_mid})")
	public int isExisted(Map<String, Object> params);
	
	//for experiment purpose.
	@Select("select content from back_up_statuses")
	public List<Map<String, Object>> getAllStatuses();
}
