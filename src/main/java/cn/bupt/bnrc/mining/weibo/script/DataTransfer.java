package cn.bupt.bnrc.mining.weibo.script;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.joda.time.DateTime;
import org.springframework.jdbc.core.JdbcTemplate;

import cn.bupt.bnrc.mining.weibo.util.Utils;

public class DataTransfer {

	private JdbcTemplate remoteJdbcTemplate;
	private JdbcTemplate localJdbcTemplate;

	public DataTransfer(){
		BasicDataSource localDS = DataSourceFactory.getDataSource(DataSourceFactory.DATASOURCEType.localHostMysql);		
		BasicDataSource remoteDS = DataSourceFactory.getDataSource(DataSourceFactory.DATASOURCEType.remoteSqlserver);

		this.setLocalJdbcTemplate(localDS);
		this.setRemoteJdbcTemplate(remoteDS);
	}
	public void setLocalJdbcTemplate(DataSource localDataSource){
		this.localJdbcTemplate = new JdbcTemplate(localDataSource);
	}
	public void setRemoteJdbcTemplate(DataSource remoteDataSource){
		this.remoteJdbcTemplate = new JdbcTemplate(remoteDataSource);
	}
	
	public void transferStatuses(){
		String selectStartTimeSql = "select top 1 created_at as start_time from statuses where created_at > '1900-01-01 00:00:00.0' order by created_at asc";
		Map<String, Object> startTimeMap = remoteJdbcTemplate.queryForMap(selectStartTimeSql);
		DateTime startTime = DataTransfer.timestamp2DataTime((Timestamp)startTimeMap.get("start_time"));
		
		String selectEndTimeSql = "select max(created_at) as end_time from statuses";
		Map<String, Object> endTimeMap = remoteJdbcTemplate.queryForMap(selectEndTimeSql);
		DateTime endTime = DataTransfer.timestamp2DataTime((Timestamp)endTimeMap.get("end_time"));
		
		String selectStatuses = "select status_id, created_at, content from statuses " +
				"where created_at between ? and ? " +
				"order by created_at asc";
		
		String insertSql = "insert ignore into statuses(status_id, content, created_at) values(?,?,?)";
		
		startTime = new DateTime(2011,12,27,20,57,22);
		int intervalDay = 2;
		for (DateTime start = startTime, next = startTime.plusDays(intervalDay); start.isBefore(endTime); 
				start = next, next = next.plusDays(intervalDay)){
			String startTimeString = start.toString("yyyy-MM-dd HH:mm:ss");
			String nextTimeString = next.toString("yyyy-MM-dd HH:mm:ss");
			
			Date s = new Date();
			
			List<Map<String, Object>> statuses = remoteJdbcTemplate.queryForList(selectStatuses, 
					new Object[]{new Timestamp(start.getMillis()), new Timestamp(next.getMillis())}, 
					new int[]{java.sql.Types.TIMESTAMP, java.sql.Types.TIMESTAMP});
			
			Date e = new Date();
			System.out.printf("get statuses: fetched-size=%d, start=%s, end=%s, cost-time=%d ms\n", 
					statuses.size(), startTimeString, nextTimeString, e.getTime() - s.getTime());
			
			int count = 0;
			for (Map<String, Object> status : statuses){
				String content = (String)status.get("content");
				if (Utils.isContainAnyEmoticons(content)){
					localJdbcTemplate.update(insertSql, 
							new Object[]{(Long)status.get("status_id"), content, (Timestamp)status.get("created_at")}, 
							new int[]{java.sql.Types.BIGINT, java.sql.Types.VARCHAR, java.sql.Types.TIMESTAMP});
					count++;
				}
			}
			s = e;
			e = new Date();
			System.out.printf("get statues: added-size=%d, start=%s, end=%s, cost-time=%d ms\n", 
					count, startTimeString, nextTimeString, e.getTime() - s.getTime());
		}
	}
	
	public static DateTime timestamp2DataTime(Timestamp stamp){
		DateTime time = new DateTime(stamp.getTime());
		return time;
	}
	
	public static void main(String[] args){
		DataTransfer transfer = new DataTransfer();
		transfer.transferStatuses();
	}
}