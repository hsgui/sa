package cn.bupt.bnrc.mining.weibo.task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.core.service.DataCollector;

@Service
public class DataUpdateFromCrawler {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public void run(){
		Set<Integer> updatedTopics = new HashSet<Integer>();
		
		long startTime = this.lastEndTimeInMillis;
		Calendar calendar = Calendar.getInstance();
		Date now = this.getCurrentTimeWithHour();
		long nowInMillis = now.getTime();
		calendar.setTimeInMillis(nowInMillis);
		
		logger.info("update for data collection: {}", now);
		System.out.println("util time: " + simpleDateFormat.format(calendar.getTime()));
		
		List<Map<String, Object>> topics = dataCollector.getRecentlyTopics(new Date(startTime), now);
		System.out.println("update topics size: " + topics.size());
		
		for (long currentEndMillis = startTime + interval; currentEndMillis <= nowInMillis; currentEndMillis+=interval){
			calendar.setTimeInMillis(startTime);
			Date currentStart = calendar.getTime();
			System.out.println("start time: " + simpleDateFormat.format(currentStart));
			
			calendar.setTimeInMillis(currentEndMillis);
			Date currentEnd = calendar.getTime();
			System.out.println("end time: " + simpleDateFormat.format(currentEnd));
			
			Set<Integer> topicsId = dataCollector.statisticTopicsSentiment(currentStart, currentEnd);
			updatedTopics.addAll(topicsId);
			
			startTime = startTime + interval;
		}
		
		for (Integer topicId : updatedTopics){
			dataCollector.updateTopicInformation(topicId, now);
		}
		
		calendar.setTimeInMillis(startTime);
		System.out.println("current start time: "+simpleDateFormat.format(calendar.getTime()));
		
		lastEndTimeInMillis = startTime;
	}
	
	public Date getCurrentTimeWithHour(){
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}
	
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private long lastEndTimeInMillis = 1364140800286L;
	private final long interval = 7200000; //2 hours.
	
	@Autowired private DataCollector dataCollector;
}
