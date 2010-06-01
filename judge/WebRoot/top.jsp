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

		<table width="100%" border="0" class="banner">
			<tr>
				<td>
					<a href="toIndex.action">Home</a>
				</td>
				<td>
					<a href="problem/listProblem.action">Problems</a>
				</td>
				<td>
					<a href="problem/status.action">Status</a>
				</td>
				<td>
					<a href="contest/listContest.action">Contest</a>
				</td>
				<td>
					<s:if test="#session.visitor != null">
						[<a href="user/toUpdate.action?uid=<s:property value="#session.visitor.id" />"><s:property value="#session.visitor.username" /></a>]
					</s:if>	
					<s:else>
						<a href="user/toRegister.action">Register</a>
					</s:else> 
				</td>
				<td>
					<s:if test="#session.visitor != null">
						<a href="user/logout.action">Logout</a>
					</s:if>	
					<s:else>
						<a href="user/toLogin.action">Login</a>
					</s:else> 
				</td>
			</tr>
		</table>
		<br /><br />

	</body>
</html>
