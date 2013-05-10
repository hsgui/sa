package cn.bupt.bnrc.mining.weibo.core.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.repository.localmysql.NegativeStatusesMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.PositiveStatusesMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.TopicStatisticsMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.TopicsMapper;

@Service
public class StatisticsTopic {
	
	/**
	 * get recently hot topics, which end_time must be after the endTime.
	 * @param endTime
	 * @return
	 */
	public List<Map<String, Object>> getRecentlyTopics(String endTime){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("end_time", endTime);
		params.put("count", 24);
		List<Map<String, Object>> topics = topicsMapper.getRecentlyHotTopics(params);
		
		return topics;
	}

	public Map<String, Object> statisticTotalPosNegNum(int topicId){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		Map<String, Object> topicInfo = topicsMapper.getTopicInfo(params);
		if (topicInfo == null) return null;
		
		String startTime = timeToString(topicInfo.get("start_time"));
		String endTime = timeToString(topicInfo.get("end_time"));
		
		Map<String, Object> statistic = this.statisticTotalPosNegNumBetween(topicId, startTime, endTime);
		if (statistic == null) return null;
		
		statistic.put("start_time", startTime);
		statistic.put("end_time", endTime);
		
		return statistic;
	}
	
	public String timeToString(Object time){
		if (time instanceof String) return (String)time;
		else if (time instanceof Date) return simpleDateFormat.format(time);
		else{
			logger.error("time is neither string nor date.., this can't be happened!");
			return null;
		}
	}
	
	public Map<String, Object> statisticTotalPosNegNumBetween(int topicId, Date start, Date end){		
		return this.statisticTotalPosNegNumBetween(topicId, 
				simpleDateFormat.format(start), simpleDateFormat.format(end));
	}
	
	public Map<String, Object> statisticTotalPosNegNumBetween(int topicId, String startTime, String endTime){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		params.put("start_time", startTime);
		params.put("end_time", endTime);
		
		Map<String, Object> totalStatisticsNum = topicStatisticsMapper.getTopicTotalStatisticsBetweenTime(params);
		
		return totalStatisticsNum;
	}
	
	public List<Map<String, Object>> statisticTotalPosNegNumTrend(int topicId){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		Map<String, Object> topicInfo = topicsMapper.getTopicInfo(params);
		if (topicInfo == null) {
			logger.info("topicId={} is null.", topicId);
			return null;
		}
		
		//the start time of topic is not useful!
		String startTime = timeToString(topicInfo.get("start_time"));
		String endTime = timeToString(topicInfo.get("end_time"));
		
		return this.statisticTotalPosNegNumTrend(topicId, startTime, endTime);
	}
	
	/**
	 * get topic statistics
	 * @param topic
	 * @param start
	 * @param end
	 * @return
	 */
	public List<Map<String, Object>> statisticTotalPosNegNumTrend(int topicId, Date start, Date end){
		return this.statisticTotalPosNegNumTrend(topicId, 
				simpleDateFormat.format(start), simpleDateFormat.format(end));
	}
	
	public List<Map<String, Object>> statisticTotalPosNegNumTrend(int topicId, String startTime, String endTime){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		params.put("start_time", startTime);
		params.put("end_time", endTime);
		List<Map<String, Object>> numsTrend = topicStatisticsMapper.getTopicStatisticsRecently(params);
		
		return numsTrend;
	}
	
	public List<Map<String, Object>> getPositiveStatusesByTopicwithTime(int topicId, Date start, Date end){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start_time", dayDateFormat.format(start));
		params.put("end_time", dayDateFormat.format(end));
		params.put("topic_id", topicId);
		
		List<Map<String, Object>> positiveStatuses = positiveStatusesMapper.getPositiveStatusesByTopicWithTime(params);
		
		return positiveStatuses;
	}
	
	public List<Map<String, Object>> getNegativeStatusesByTopicWithTime(int topicId, Date start, Date end){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start_time", dayDateFormat.format(start));
		params.put("end_time", dayDateFormat.format(end));
		params.put("topic_id", topicId);
		
		List<Map<String, Object>> negativeStatuses = negativeStatusesMapper.getNegativeStatusesByTopicWithTime(params);
		
		return negativeStatuses;
	}
	
	@Autowired private TopicStatisticsMapper topicStatisticsMapper;
	@Autowired private TopicsMapper topicsMapper;
	@Autowired private NegativeStatusesMapper negativeStatusesMapper;
	@Autowired private PositiveStatusesMapper positiveStatusesMapper;
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleDateFormat dayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private Logger logger = LoggerFactory.getLogger(getClass());
}
