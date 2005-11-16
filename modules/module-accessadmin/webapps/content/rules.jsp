<%@ page import="org.mycore.access.MCRRuleStore,
	org.mycore.access.MCRAccessRule,
	org.mycore.common.MCRSession,
	org.mycore.common.MCRSessionMgr,
	java.util.ArrayList"%>

<%
	MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
	ArrayList ruleIds = MCRRuleStore.getInstance().retrieveAllIDs();
	mcrSession.deleteObject("rule");
%>

<h4>Vorhandene Regeln</h4>

<form method=post action="./admin/rules_validate.jsp" id="overview">
<table class="access" cellspacing="1" cellpadding="0" >
	<tr>
		<th colspan="2">
			Accessregeln
		</th>
		<th>
			<input type="image" title="Neue Regel anlegen" name="new" src="./admin/images/install.png">
		</th>
	</tr>
	<%
		for (int i=0; i<ruleIds.size(); i++){
			MCRAccessRule rule = MCRRuleStore.getInstance().getRule((String) ruleIds.get(i));
			out.println("<tr>");
			out.println("<td class=\"rule\">" + rule.getId() + "</td>");
			out.println("<td class=\"rule\">" + rule.getDescription() + "<br /><br />" + rule.getRuleString() + "</td>");
			out.println("<td class=\"rule\"><input type=\"image\" title=\"Regel bearbeiten\" name=\"e"+ rule.getId() +"\" src=\"./admin/images/edit.png\"> <input type=\"image\" title=\"Regel löschen\" name=\"d"+ rule.getId() +"\" src=\"./admin/images/delete.png\" onClick=\"return questionDel()\"></td>");
			out.println("</tr>");
		}
	%>
</table>
<input type="hidden" name="operation" value="detail">

</form>
