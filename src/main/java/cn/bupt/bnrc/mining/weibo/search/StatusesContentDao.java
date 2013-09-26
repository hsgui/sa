package cn.bupt.bnrc.mining.weibo.search;

import java.util.List;
import java.util.Map;

public interface StatusesContentDao {

	/**
	 * params have following fields:
	 * 1. tempTableName: hsgui_temp_table
	 */
	public void createTempTable(Map<String, Object> params);

	/**
	 * 
	 * @param params
	 * params have following fields:
	 * tempTableName:
	 * pagesize:
	 * @return
	 */
	public List<Map<String, Object>> getTopNStatusContent(Map<String, Object> params);
	
	/**
	 * 
	 * @param params
	 * params have following fields:
	 * tempTableName:
	 * pagesize:
	 * @return
	 */
	public void deleteTopNStatusContent(Map<String, Object> params);
	
	/**
	 * params have following fields:
	 * 1. tempTableName: hsgui_temp_table
	 */
	public void dropTempTable(Map<String, Object> params);
}
