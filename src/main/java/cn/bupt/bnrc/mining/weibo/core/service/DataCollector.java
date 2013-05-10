package cn.bupt.bnrc.mining.weibo.core.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.classify.StatusesClassifier;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.BackupStatusesMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.NegativeStatusesMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.PositiveStatusesMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.TopicStatisticsMapper;
import cn.bupt.bnrc.mining.weibo.repository.localmysql.TopicsMapper;
import cn.bupt.bnrc.mining.weibo.repository.localsqlserver.TopicRelatedWeiboMapper;
import cn.bupt.bnrc.mining.weibo.repository.localsqlserver.TrendingTopicsMapper;
import cn.bupt.bnrc.mining.weibo.util.Constants;

@Service
public class DataCollector {

	/*
	 * get topics from liu-yue-jie's sql server.
	 * I can get the name of topics, the time is not very important,
	 * because i will update the important field:end_time later.
	 * So, if there is an existing topic already, just ignore it with the same topic name.
	 */
	public List<Map<String, Object>> getRecentlyTopics(Date start, Date end){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start_time", simpleDateFormat.format(start));
		params.put("end_time", simpleDateFormat.format(end));
		List<Map<String, Object>> topics = trendingTopicsMapper.getHotTopicsWithTime(params);
		for (Map<String, Object> topic : topics){
			if (topic.get("topic_name") == null) continue;
			String topicName = (String)topic.get("topic_name");
			if (topicName.trim().length() <= 0) continue;
			
			params.put("topic_name", topicName);
			params.put("start_time", simpleDateFormat.format(end));	//Attention, this is not the real time, but closely.
			params.put("end_time", simpleDateFormat.format(end));
			logger.info("getTopicsFromRemote: topic_name: {}, start_time: {}", 
					params.get("topic_name"), params.get("start_time"));
			topicsMapper.addTopic(params);
		}
		
		return topics;
	}
	
	/*
	 * get statuses between start and end,
	 * and classify the statuses.
	 * note: these statuses contain topics!
	 * and topics must exists before these statuses!
	 */
	public Set<Integer> statisticTopicsSentiment(Date start, Date end){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("start_time", simpleDateFormat.format(start));
		params.put("end_time", simpleDateFormat.format(end));
		List<Map<String, Object>> statuses = topicRelatedWeiboMapper.getRelatedWeiboWithTime(params);//get from liu-yue-jie
		Set<Integer> topicsId = new HashSet<Integer>();
		
		for (Map<String, Object> status : statuses){
			params.put("topic_name", status.get("related_topic"));
			Map<String, Object> topic = topicsMapper.getTopicWithName(params);
			if (topic == null){
				logger.debug("status with relatedTopic: {} not exists, so I ignore this status", params.get("topic_name"));
				continue;
			} 		//if status does not contain any topics, then this status will not be stored!
			
			if (status.get("content") == null) continue;
			String content = ((String)status.get("content")).trim();
			if (content.length() <= 0) continue;
			
			String weiboMid = ((String)status.get("weibo_mid")).trim();
			params.put("weibo_mid", weiboMid);
			if (backupStatusesMapper.isExisted(params) == 1){
				logger.debug("already exists this status! -- {}", weiboMid);
				continue;
			}
			
			params.put("post_user", status.get("post_user"));
			params.put("post_time", status.get("post_time"));
			params.put("related_topic_id", topic.get("id"));			
			
			params.put("content", content);
			backupStatusesMapper.addStatus(params);	//backup this status for later use.
			
			int flag = classifier.classify(content);
			Map<String, Object> polarityStatus = new HashMap<String, Object>();
			polarityStatus.put("flag", flag);
			
			polarityStatus.put("topic_id", topic.get("id"));
			polarityStatus.put("generate_time", status.get("post_time"));
			polarityStatus.put("id", params.get("status_id"));
			if (flag == Constants.POSITIVE_FLAG){
				positiveStatusesMapper.addPositiveStatus(polarityStatus);
			}else if (flag == Constants.NEGATIVE_FLAG){
				negativeStatusesMapper.addNegativeStatus(polarityStatus);
				
			}
			logger.info("statisticsSentiment -- topicId={}, weibo_mid={}, flag={}",
					new Object[]{topic.get("id"), status.get("weibo_mid"), flag});
			topicsId.add((Integer)topic.get("id"));
		}
		
		for (Integer topicId : topicsId){
			this.updateTopicStatistics(topicId, start, end);
		}
		
		return topicsId;
	}
	
	/*
	 * update the topic statistics.
	 * just update the table: topic_statistics
	 */
	public void updateTopicStatistics(int topicId, Date start, Date end){
		Map<String, Object> params = new HashMap<String, Object>();
		Map<String, Object> statistics = new HashMap<String, Object>();
		params.put("start_time", simpleDateFormat.format(start));
		params.put("end_time", simpleDateFormat.format(end));		
		params.put("topic_id", topicId);
		
		int negativeCount = negativeStatusesMapper.getNegativeStatusesCountByTopicwithTime(params);
		int positiveCount = positiveStatusesMapper.getPositiveStatusesCountByTopicwithTime(params);
		
		statistics.put("topic_id", topicId);
		statistics.put("negative_count", negativeCount);
		statistics.put("positive_count", positiveCount);
		statistics.put("start_time", simpleDateFormat.format(start));
		statistics.put("end_time", simpleDateFormat.format(end));
		topicStatisticsMapper.addTopicStatistics(statistics);
		
		logger.info(String.format("updateTopicsStatistics: topicId=%d, start=%s, end=%s", 
				topicId, simpleDateFormat.format(start), simpleDateFormat.format(end)));
	}
	
	public void updateTopicInformation(int topicId, Date now){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		Map<String, Object> totalCount = topicStatisticsMapper.getTopicTotalStatistics(params);
		params.put("total_positive_count", totalCount.get("total_positive_count"));
		params.put("total_negative_count", totalCount.get("total_negative_count"));
		params.put("end_time", dayDateFormat.format(now));
		
		topicsMapper.updateTopicInformation(params);
		logger.info("updateTopicInformation: topicId={}", topicId);
	}
	
	@Autowired private TopicStatisticsMapper topicStatisticsMapper;
	@Autowired private TopicRelatedWeiboMapper topicRelatedWeiboMapper;
	@Autowired private TrendingTopicsMapper trendingTopicsMapper;
	@Autowired private TopicsMapper topicsMapper;
	@Autowired private BackupStatusesMapper backupStatusesMapper;
	@Autowired private NegativeStatusesMapper negativeStatusesMapper;
	@Autowired private PositiveStatusesMapper positiveStatusesMapper;
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private SimpleDateFormat dayDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	private StatusesClassifier classifier = new StatusesClassifier();
}
