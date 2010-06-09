<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%@ taglib prefix="s" uri="/struts-tags"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
    	<base href="<%=basePath%>" />
	    <title>Virtual Judge</title>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
		<link rel="stylesheet" type="text/css" href="css/global.css" />
	</head>

	<body>
		<br /><br /><br /><hr>
		<center>
			<p>
				All Copyright Reserved ©2010 <a href="http://acm.hust.edu.cn">HUST ACM/ICPC</a> TEAM
				<s:if test="#session.visitor.sup == 1">
					<a href="stat/listOL.action"><img style="text-decoration: none;" height="15px" src="images/statistics.gif" /></a>
				</s:if>
				<br>Anything about the OJ, Please Contact Author:<a href="mailto:is.un@qq.com">Isun</a><br>
				<a href="graph/draw.jsp">ofc</a>
			</p>
		</center>
	</body>
	<script type="text/javascript">
		var _gaq = _gaq || [];
		_gaq.push(['_setAccount', 'UA-13231844-2']);
		_gaq.push(['_trackPageview']);
		
		(function() {
		  var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
		  ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
		  var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
		})();
	</script>
</html>