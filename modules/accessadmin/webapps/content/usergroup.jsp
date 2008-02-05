<%@ page import="org.mycore.user.MCRUserMgr,
	org.mycore.user.MCRGroup,
	java.util.ArrayList"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%
	ArrayList groupids = MCRUserMgr.instance().getAllGroupIDs();
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
%>

<h4>Vorhandene Benutzergruppen</h4>

<form method=post action="<%= WebApplicationBaseURL %>admin/usergroup_validate.jsp" id="overview">
<table class="access" cellspacing="1" cellpadding="0" >
	<tr>
		<th width="200px">
			Benutzergruppen
		</th>
		<th>
			Beschreibung
		</th>
		<th>
			<input type="image" title="Neue Gruppe anlegen" name="new" src="./admin/images/install.png">
		</th>
	</tr>
	<%
		for (int i=0; i<groupids.size(); i++){
			MCRGroup grp = MCRUserMgr.instance().retrieveGroup((String)groupids.get(i));
			ArrayList members = grp.getMemberGroupIDs();

			out.println("<tr>");
			out.println("<td class=\"rule\">" + (String)groupids.get(i) + "</td>");
			out.println("<td class=\"rule\">" + grp.getDescription() + "</td>");
			out.println("<td class=\"rule\"><input type=\"image\" title=\"Gruppe bearbeiten\" name=\"e"+ (String)groupids.get(i) +"\" src=\"" + WebApplicationBaseURL +"admin/images/edit.png\"> <input type=\"image\" title=\"Gruppe löschen\" name=\"d"+ (String)groupids.get(i) +"\" src=\"" + WebApplicationBaseURL + "admin/images/delete.png\" onClick=\"return questionDel()\"></td>");
			out.println("</tr>");
		}
	%>
</table>
<input type="hidden" name="operation" value="detail">

</form>