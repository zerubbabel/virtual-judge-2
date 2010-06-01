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
	    <title>Virtual Judge -- Source code</title>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
		<script type="text/javascript" src="javascript/common.js"></script>
	</head>

	<body>
		<s:include value="/top.jsp" />
		
		<center><font color=#333399 size=5 >Source Code</font></center><br />
		<div class="plm" style="text-align:left">
			<table align="center" style="font-size:10pt">
				<tr>
					<td>
						<b>Problem: </b>
						<a href="problem/viewProblem.action?id=${submission.problemId}">${submission.problemId}</a>
					</td>
					<td width=10px></td>
					<td>
						<b>User: </b>
						<a href="user/profile.action?uid=${uid}">${un}</a>
					</td>
				</tr>
				<tr>
					<td>
						<b>Memory: </b>${submission.memory} KB
					</td>
					<td width=10px></td>
					<td>
						<b>Time: </b>${submission.time} MS
					</td>
				</tr>
				<tr>
					<td>
						<b>Language: </b>${submission.language}
					</td>
					<td width=10px></td>
					<td>
						<b>Result: </b>
						<font color=blue>${submission.status}</font>
					</td>
				</tr>
				<s:if test="#session.visitor.id == uid || #session.visitor.sup != 0">
					<tr>
						<td>
							<b>Public: </b>
						</td>
						<td colspan="2">
							<s:radio name="" list="#{'0':'No', '1':'Yes'}" value="%{submission.isOpen}" theme="simple" onchange="toggleOpen('%{submission.id}', 0);"></s:radio>
						</td>
					</tr>
				</s:if>
			</table>
		</div>	
		<p style="text-align:center;font-size:15pt;color:green;">
			<s:if test="submission.isOpen == 1">
				This source is shared by <b>${un}</b>
			</s:if>
			<s:else>
				&nbsp;
			</s:else>
		</p>
		<pre style="font-family:Courier New,Courier,monospace">${submission.source}</pre>
	</body>
	<s:include value="/bottom.jsp" />
</html>
