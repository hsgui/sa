package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

@Repository
public interface TaggingStatusMapper {

	/**
	 * params: {content:xxxx, tagging_flag:1}
	 * @param params
	 */
	@Insert("insert into tagging_statuses(content, tagging_flag) "+
			"values(#{content}, #{tagging_flag})")
	public void addUntaggedStatus(Map<String, Object> params);
	
	/**
	 * params: {page_size:1, tagging_flag:0}
	 * @param params
	 * @return
	 */
	@Select("select top ${page_size} * from tagging_statuses"+
		" where tagging_flag = #{tagging_flag}"+
		" order by id;")
	public List<Map<String, Object>> getFirstPageStatuses(Map<String, Object> params);
	
	/**
	 * params: {page_size:1, page_offset:100, tagging_flag:0}
	 * @param params
	 * @return
	 */
	@Select("select top ${page_size} *"+
		" from tagging_statuses " +
		" where tagging_flag=#{tagging_flag} and "+
		" <![CDATA[ "+
		"	(id >" +
		"		(select max(id) from "+
		"			(select top ${page_offset} id "+
		"				from tagging_statuses order by id) as T))]]>")
	public List<Map<String, Object>> getNextPageStatuses(Map<String, Object> params);
	
	/**
	 * params: {id:1, tagging_flag=1}
	 * @param params
	 */
	@Update("update tagging_statuses "+
		" set tagging_flag=#{tagging_flag}"+
		" where id=#{id}")
	public void updateStatusFlag(Map<String, Object> params);
	
	@Select("select count(id) from tagging_statuses "+
		" where tagging_flag = #{tagging_flag}")
	public int getTotalCount(Map<String, Object> params);
}
