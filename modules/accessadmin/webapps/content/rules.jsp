<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.access.mcrimpl.MCRAccessRule,
	org.mycore.common.MCRSession,
	org.mycore.common.MCRSessionMgr,
	java.util.ArrayList"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>    

<%
	MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
    
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	ArrayList ruleIds = MCRRuleStore.getInstance().retrieveAllIDs();
	mcrSession.deleteObject("rule");
%>

<h4>Vorhandene Regeln</h4>

<form method=post action="<%= WebApplicationBaseURL %>admin/rules_validate.jsp" id="overview">
<table class="access" cellspacing="1" cellpadding="0" >
	<tr>
		<th colspan="2">
			Accessregeln
		</th>
		<th>
			<input type="image" title="Neue Regel anlegen" name="new" src="<%= WebApplicationBaseURL %>admin/images/install.png">
		</th>
	</tr>
	<%
		for (int i=0; i<ruleIds.size(); i++){
			MCRAccessRule rule = MCRRuleStore.getInstance().getRule((String) ruleIds.get(i));
            %>
               <tr>
                  <td class="rule"><%= rule.getId() %></td>
                  <td class="rule"><%= rule.getDescription() %><br /><br /><%= rule.getRuleString() %></td>
                  <td class="rule">
                     <input type="image" title="Regel bearbeiten" name="e<%= rule.getId() %>" src="<%= WebApplicationBaseURL %>admin/images/edit.png"> 
                     <input type="image" title="Regel löschen" name="d<%= rule.getId() %>" src="<%= WebApplicationBaseURL %>admin/images/delete.png" onClick="return questionDel()">
                  </td>
               </tr>
            <%
    	}
	%>
</table>
<input type="hidden" name="operation" value="detail">

</form>
