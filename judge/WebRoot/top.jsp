<%@ taglib prefix="s" uri="/struts-tags"%>
<table width="100%" border="0" class="banner" style="margin-bottom:30px;"><tr><td><a href="toIndex.action">Home</a></td><td><a href="problem/toListProblem.action">Problems</a></td><td><a href="problem/status.action">Status</a></td><td><a href="contest/toListContest.action">Contest</a></td><td><s:if test="#session.visitor != null">[<a id="my_account" uid="<s:property value='#session.visitor.id' />" href="user/toUpdate.action?uid=<s:property value="#session.visitor.id" />"><s:property value="#session.visitor.username" /></a>]</s:if><s:else><a class="register" href="javascript:void(0)">Register</a></s:else></td><td><s:if test="#session.visitor != null"><a id="logout" href="javascript:void(0)">Logout</a></s:if><s:else><a class="login" href="javascript:void(0)">Login</a></s:else></td></tr></table>
<div id="dialog-form-login" style="display: none" title="Login">
	<p class="validateTips"></p><form id="login_form" action="javascript:void(0)"><fieldset><label for="username">Username *</label><input type="text" id="username" name="username" class="text ui-widget-content ui-corner-all" style="ime-mode:disabled" /><label for="password">Password *</label><input type="password" id="password" name="password" class="text ui-widget-content ui-corner-all" /></fieldset></form>
</div>
<s:if test="#session.visitor == null"><div id="dialog-form-register" style="display: none" title="Register">
	<p class="validateTips"></p><fieldset><div style="width:200px;float:left"><label for="username1">Username *</label><input type="text" id="username1" class="text ui-widget-content ui-corner-all" style="ime-mode:disabled" /><label for="password1">Password *</label><input type="password" id="password1" class="text ui-widget-content ui-corner-all" /><label for="repassword">Repeat *</label><input type="password" id="repassword" class="text ui-widget-content ui-corner-all" /><label for="nickname">Nickname</label><input type="text" id="nickname" class="text ui-widget-content ui-corner-all" /><label for="school">School</label><input type="text" id="school" class="text ui-widget-content ui-corner-all" /><label for="qq">QQ</label><input type="text" id="qq" class="text ui-widget-content ui-corner-all" /><label for="email">Email</label><input type="text" id="email" class="text ui-widget-content ui-corner-all" /><label for="share">Share code by default</label><br /><s:radio id="share" name="share" list="#{'0':'No', '1':'Yes'}" value="1" theme="simple" /></div><div style="width:200px;margin-left:20px;float:left"><label for="blog">Blog & Introduction</label><s:textarea id="blog" rows="25" cols="35" cssClass="text ui-widget-content ui-corner-all" /></div></fieldset>
</div></s:if>
