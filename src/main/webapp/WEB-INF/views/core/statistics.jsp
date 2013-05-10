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
</head>

<body>
	<c:if test="${not empty message}">
		<div id="message" class="alert alert-success"><button data-dismiss="alert" class="close">×</button>${message}</div>
	</c:if>
	
	<div id="percentContainer" style="min-width: 400px; height: 400px; margin: 0 auto"></div>
	<div id="numTrendContainer" style="min-width: 400px; height: 400px; margin: 0 auto"></div>

	<script src="/WEB-INF/js/charts.js" type="text/javascript" ></script>
</body>