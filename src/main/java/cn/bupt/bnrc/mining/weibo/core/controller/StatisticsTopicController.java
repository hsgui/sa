package cn.bupt.bnrc.mining.weibo.core.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import cn.bupt.bnrc.mining.weibo.core.service.PagedTopicRelatedNegativeStatuses;
import cn.bupt.bnrc.mining.weibo.core.service.PagedTopicRelatedPositiveStatuses;
import cn.bupt.bnrc.mining.weibo.core.service.StatisticsTopic;
import cn.bupt.bnrc.mining.weibo.util.StringBuilderUtil;
import cn.bupt.bnrc.mining.weibo.util.Utils;

@Controller
@RequestMapping(value="/topic")
public class StatisticsTopicController {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@RequestMapping(value="main")
	public ModelAndView statisticTopicSentiment(){
		String page = "core/main";
		Map<String, Object> model = new HashMap<String, Object>();
		
		String startTime = Utils.simpleDateFormat.format(Utils.getDateBefore(new Date(), day));
		List<Map<String, Object>> recentlyTopics = statisticsTopic.getRecentlyTopics(startTime);
		if (recentlyTopics.size() == 0){
			logger.error("no topics!"); return null;
		}
		
		int topicId = (Integer)recentlyTopics.get(0).get("id");
		
		model.put("defaultTopic", StringBuilderUtil.buildJsonString(recentlyTopics.get(0)));
		model.put("recentlyTopics", StringBuilderUtil.buildJsonString(recentlyTopics));
		model.put("totalTrends", StringBuilderUtil.buildJsonString(statisticsTopic.statisticTotalPosNegNumTrend(topicId)));
		
		logger.info("forward to page: {}, totalTrends is null={}, topicId={}",
				new Object[]{page, model.get("totalTrends"), topicId});
		
		return new ModelAndView(page, model);
	}
	
	@RequestMapping(value="total-statistics", params={"topicId"})
	public void getStatisticsAboutTotalNums(@RequestParam("topicId") int topicId,
			HttpServletResponse response) throws IOException{
		Map<String, Object> statistic = statisticsTopic.statisticTotalPosNegNum(topicId);
		
		response.setContentType("text/text; charset=utf-8");
		response.getWriter().print(StringBuilderUtil.buildJsonString(statistic));
		
		logger.info("ajax request for total nums: topicId={}, null={}",topicId, statistic==null);
	}
	
	@RequestMapping(value="total-trends", params={"topicId"})
	public void getStatisticsAboutTotalTrends(@RequestParam("topicId") int topicId,
			HttpServletResponse response) throws IOException{
		List<Map<String, Object>> totalTrends = statisticsTopic.statisticTotalPosNegNumTrend(topicId);
		
		response.setContentType("text/text; charset=utf-8");
		response.getWriter().print(StringBuilderUtil.buildJsonString(totalTrends));
		
		logger.info("ajax request for total trends: topicId={}, size={}", topicId, totalTrends.size());
	}
	
	@RequestMapping(value="recently-topics")
	public void getRecentlyTopics(HttpServletResponse response) throws IOException{
		String startTime = Utils.simpleDateFormat.format(Utils.getDateBefore(new Date(), day));
		List<Map<String, Object>> recentlyTopics = statisticsTopic.getRecentlyTopics(startTime);
		
		response.setContentType("text/text; charset=utf-8");
		response.getWriter().print(StringBuilderUtil.buildJsonString(recentlyTopics));
		
		logger.info("ajax request for recently topics: size={}", recentlyTopics.size());
	}
	
	@RequestMapping(value="positive-statuses", params={"topicId", "page"})
	public void getTopicRelatedPositveStatuses(@RequestParam("topicId") int topicId, 
			@RequestParam("page") int page, HttpServletResponse response) throws IOException{
		List<Map<String, Object>> positiveStatuses = pagedTopicRelatedPositiveStatuses.getPagedTopicRelatedPostiveStatuses(topicId, page);
		
		response.setContentType("text/text; charset=utf-8");
		response.getWriter().print(StringBuilderUtil.buildJsonString(positiveStatuses));
		
		logger.info(String.format("ajax request for positive statuses: topicId=%d, page=%d, size=%d", 
				topicId, page, positiveStatuses.size()));
	}
	
	@RequestMapping(value="negative-statuses", params={"topicId", "page"})
	public void getTopicRelatedNegativeStatuses(@RequestParam("topicId") int topicId, 
			@RequestParam("page") int page, HttpServletResponse response) throws IOException{
		List<Map<String, Object>> negativeStatuses = pagedTopicRelatedNegativeStatuses.getPagedTopicRelatedNegativeStatuses(topicId, page);
		
		response.setContentType("text/text; charset=utf-8");
		response.getWriter().print(StringBuilderUtil.buildJsonString(negativeStatuses));
		
		logger.info(String.format("ajax request for negative statuses: topicId=%d, page=%d, size=%d", 
				topicId, page, negativeStatuses.size()));
	}
	
	private int day = 14;
	@Autowired private StatisticsTopic statisticsTopic;
	@Autowired private PagedTopicRelatedPositiveStatuses pagedTopicRelatedPositiveStatuses;
	@Autowired private PagedTopicRelatedNegativeStatuses pagedTopicRelatedNegativeStatuses;
}
