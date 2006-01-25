<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.access.mcrimpl.MCRAccessRule,
	org.mycore.common.MCRSession,
	org.mycore.frontend.servlets.MCRServlet,
	java.text.SimpleDateFormat,
	java.text.DateFormat,
	java.util.Date,
	java.util.Collections"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>    
<%
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
%>    
<h4>Regel bearbeiten</h4>

<p><a href="<%= WebApplicationBaseURL %>/admin?path=rules">zur Übersicht</a></p>

<%
	MCRSession mcrSession = MCRServlet.getSession(request);

	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String operation = request.getParameter("operation");
	MCRAccessRule rule = null;

	String errmsg = (String) mcrSession.get("err_msg");
	if ( mcrSession.get("err_msg") != null)
		out.println("<p class=\"error\">" + errmsg + "</p>");
	mcrSession.put("err_msg", null);


	if (operation == null)
		operation = "edit";

	if(request.getParameter("id") == null){
		//new
		rule = new MCRAccessRule("-new-", mcrSession.getCurrentUserID(), new Date(), null, "");
		operation = "new";
	}else{
		//edit
		if (mcrSession.get("rule") == null){
			rule = MCRRuleStore.getInstance().getRule((String)request.getParameter("id"));
		}else{
			rule = (MCRAccessRule) mcrSession.get("rule");
		}
	}


%>

<form method="post" action="<%= WebApplicationBaseURL %>/admin/rules_validate.jsp">
	<table class="access">
		<tr>
			<td>Regel-ID:</td>
			<td><input type="text" name="rid" value="<%=rule.getId()%>"><input type="hidden" name="rid_orig" value="<%=rule.getId()%>"></td>
		</tr>
		<tr>
			<td>Regel:</td>
			<td><div class="field" id="rule_show"><%=rule.getRuleString()%></div>
				<button type="button" ONCLICK="openRuleEditor(window,'<%=(String)request.getParameter("id")%>')">...</button><br>
				<input type="hidden" name="rule" id="rule" value="<%=rule.getRuleString()%>">
			</td>
		</tr>
		<tr>
			<td valign="top">Beschreibung:</td>
			<td><textarea name="description" cols="50" rows="3"><%=rule.getDescription()%></textarea></td>
		</tr>
		<tr>
			<td>&nbsp;
				<input type="hidden" name="creator" value="<%=rule.getCreator()%>">
				<input type="hidden" name="creationtime" value="<%=df.format(rule.getCreationTime())%>">
				<input type="hidden" name="operation" value="<%=operation%>">
			</td>
			<td>
				<small>
					<%=rule.getCreator()%>,&nbsp;
					<%=df.format(rule.getCreationTime())%>
				</small>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><input type="reset">&nbsp;<input type="submit" value="Speichern"></td>
		</tr>

	</table>
</form>