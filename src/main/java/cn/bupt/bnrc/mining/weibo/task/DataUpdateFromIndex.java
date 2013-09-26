package cn.bupt.bnrc.mining.weibo.task;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.repository.localmysql.TopicsMapper;
import cn.bupt.bnrc.mining.weibo.repository.localsqlserver.TrendingTopicsMapper;

@Service
public class DataUpdateFromIndex {
	private static List<Map<String, Object>> emptyListMaps = new ArrayList<Map<String, Object>>();
	private static DateTimeFormatter dateTimeToDirFormatter = DateTimeFormat.forPattern("yyyy-MM-dd");
	private static ObjectMapper objectMapper = new ObjectMapper();
	
	private static Logger logger = LoggerFactory.getLogger(DataUpdateFromIndex.class);
	
	public int updateTopicStatusesSentiment(String dir){
		int modifiedCount = 0;
		
		return modifiedCount;
	}
	
	public List<Map<String, Object>> getTheDayHotTopics(String dir){
		DateTime theDay = null;
		try{
			theDay = dateTimeToDirFormatter.parseDateTime(dir);
			
		}catch (Exception e) {
			logger.error(e.getMessage());
			return emptyListMaps;
		}
		
		Map<String, Object> params = new HashMap<String, Object>();
		DateTime startTime = theDay.minusDays(1);
		DateTime endTime = theDay.minusSeconds(1);
		
		params.put("start_time", startTime.toString("yyyy-MM-dd HH:mm:ss"));
		params.put("end_time", endTime.toString("yyyy-MM-dd HH:mm:ss"));
		List<Map<String, Object>> hotTopicsList = trendingTopicsMapper.getHotTopicsWithTime(params);
		
		for (Map<String, Object> hotTopicMap : hotTopicsList){
			if (hotTopicMap.get("topic_name") == null) continue;
			String topicName = (String)hotTopicMap.get("topic_name");
			if (topicName.trim().length() <= 0) continue;
			
			params.put("topic_name", topicName);
			Map<String, Object> topicInfo = topicsMapper.getTopicWithName(params);
			if (topicInfo == null){
				topicsMapper.addTopic(params);
				logger.info("get new topic from remote: topic_name: {}, updated_time: {}", 
						params.get("topic_name"), params.get("end_time"));
			}else{
				params.put("topic_id", topicInfo.get("topic_id"));
				topicsMapper.updateTopicLastUpdatedTime(params);
				logger.info("topic [{}] already exists, just update the end_time from {} to {}",
						new Object[]{topicName, topicInfo.get("end_time"), params.get("end_time")});
			}
			
			List<Map<String, Object>> simpleStatuses = getTopicRelatedStatuses(topicName, dir);
			for (Map<String, Object> simpleStatus : simpleStatuses){
				String createdAt = (String)simpleStatus.get("createdAt");
				String content = (String)simpleStatus.get("content");
			}
		}
		
		
		return emptyListMaps;
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getTopicRelatedStatuses(String topicName, String dir){
		DefaultHttpClient client = new DefaultHttpClient();
		try{
			String uriString = String.format("http://127.0.0.1:8080/index/api/simplestatuses?topics=%s&separate=,&dir=%s", topicName, dir);
			URI uri = new URI(uriString);
			
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        logger.error("get error: statusCode=" + status);
                        return null;
                    }
                }
            };
            
			HttpGet get = new HttpGet(uri);
			String responseBody = client.execute(get, responseHandler);
			if (responseBody != null){
				List<Map<String, Object>> statuses = objectMapper.readValue(responseBody, List.class);
				return statuses;
			}else{
				return emptyListMaps;
			}
		}catch(Exception e){
			logger.error(e.getMessage());
		}
		
		return emptyListMaps;
	}
	
	@Autowired private TrendingTopicsMapper trendingTopicsMapper;
	@Autowired private TopicsMapper topicsMapper;
}
