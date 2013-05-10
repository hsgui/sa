<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="cn.bupt.bnrc.mining.weibo.tagging.TaggingStatusesService" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>

<html>
<head>
	<title>话题情感倾向统计</title>
	<script src="${ctx}/static/highcharts/highcharts.js" type="text/javascript"></script>
	<script src="${ctx}/static/highcharts/modules/exporting.js" type="text/javascript"></script>
	<%
		String recentlyTopics = (String)request.getAttribute("recentlyTopics");
		String defaultTopicInfo = (String)request.getAttribute("defaultTopic");
		String totalTrends = (String)request.getAttribute("totalTrends");
	%>
	<script type="text/javascript">
	$(document).ready(function() {
		var recentlyTopics = <%= recentlyTopics %>;
		var defaultTopicInfo = <%= defaultTopicInfo %>;
		var totalTrends = <%= totalTrends %>;

		Topics.showTopics(recentlyTopics);
		Topics.recentlyTopics = recentlyTopics;
		Topics.currentTopic = defaultTopicInfo;

		TotalNumsChart.showChart(defaultTopicInfo, defaultTopicInfo);
		TotalTrendsChart.showChart(defaultTopicInfo, totalTrends);

		StatusLoader.loadPositiveStatuses(defaultTopicInfo, defaultTopicInfo.total_positive_count);
		StatusLoader.loadNegativeStatuses(defaultTopicInfo, defaultTopicInfo.total_negative_count);
	});

	</script>
</head>

<body>	
	<div class="container">
		<div class="row">
			<div id="topics" class="span3 bs-docs-sidebar">
				<ul id="topics-list" class="nav nav-list bs-docs-sidenav submenu"></ul>
			</div>
			<div class="span9">
				<section id="total-percent-section">
					<div class="page-header">
						<h4 id="total-percent-page-header">话题情感倾向统计</h4></div>
					<div id="percentContainer" class="span9" style="margin: 0 auto"></div>
				</section>
						
				<section id="num-trend-section">
					<div class="page-header">
						<h4 id="num-trend-page-header">主观微博的数量变化图</h4></div>
					<div id="numTrendContainer" class="span9" sstyle="margin: 0 auto"></div>
				</section>
				
				<section id="statuses">
					<div class="page-header">
						<h4 id="statuses-page-header">微博流</h4></div>
					<div class="row-fluid">
						<div id="positive-statuses" class="span6">
							<div id="positive-statuses-content"></div>
							<div id="positive-statuses-pagination" class="pagination"></div>
						</div>
						<div id="negative-statuses" class="span6">
							<div id="negative-statuses-content"></div>
							<div id="negative-statuses-pagination" class="pagination"></div>
						</div>
					</div>
				</section>
			</div>			
		</div>
	</div>

	<script src="${ctx}/static/app-js/charts.js" type="text/javascript"></script>
	<script src="${ctx}/static/app-js/topics.js" type="text/javascript"></script>
	<script src="${ctx}/static/app-js/bootstrap-pagination.js" type="text/javascript"></script>
	<script src="${ctx}/static/app-js/statuses-loader.js" type="text/javascript"></script>
</body>