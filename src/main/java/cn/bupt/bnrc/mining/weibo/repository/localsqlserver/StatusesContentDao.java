package cn.bupt.bnrc.mining.weibo.repository.localsqlserver;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

/**
 * 
 * @author hsgui
 *
 */
@Repository
public interface StatusesContentDao {
	
	public void createTempTable(Map<String, Object> params);

	public List<Map<String, Object>> getTopNStatusContent(Map<String, Object> params);
	
	public void deleteTopNStatusContent(Map<String, Object> params);
	
	public void dropTempTable(Map<String, Object> params);
}
