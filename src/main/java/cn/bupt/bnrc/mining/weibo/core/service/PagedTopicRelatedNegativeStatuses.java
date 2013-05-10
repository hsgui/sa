package cn.bupt.bnrc.mining.weibo.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import cn.bupt.bnrc.mining.weibo.repository.localmysql.NegativeStatusesMapper;

@Service
public class PagedTopicRelatedNegativeStatuses {

	public List<Map<String, Object>> getPagedTopicRelatedNegativeStatuses(int topicId, int pageNum){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		params.put("offset", pageNum * pageSize);
		params.put("size", pageSize);
		
		List<Map<String, Object>> statuses = negativeStatusesMapper.getPagedTopicNegativeStatuses(params);
		
		return statuses;
	}
	
	public int getTopicRelatedNegativeStatusesPageCount(int topicId){
		int count = this.getTopicRelatedNegativeStatusesCount(topicId);
		if (count <= 0){
			return 0;
		}
		int page = (count-1)/pageSize + 1;
		
		return page;
	}
	
	public int getTopicRelatedNegativeStatusesCount(int topicId){
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("topic_id", topicId);
		int totalCount = negativeStatusesMapper.getNegativeStatusesCountByTopic(params);
		return totalCount;
	}
	
	private int pageSize = 10;
	@Autowired private NegativeStatusesMapper negativeStatusesMapper;
}
