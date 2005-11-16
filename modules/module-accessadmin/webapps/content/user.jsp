<%@ page import="org.mycore.user.MCRUserMgr,
	java.util.ArrayList,
	java.util.Collections"%>

<%
	ArrayList userids = MCRUserMgr.instance().getAllUserIDs();
	Collections.sort(userids);

%>

<h4>Vorhandene Benutzer</h4>

<form method=post action="./admin/user_validate.jsp" id="overview">

<p>
<%
	for (int i=65; i<90; i++){
		String b = new Character((char)i).toString();
		out.println("<a href=\"#"+b+"\">" + b + "</a> | ");
	}
	out.print("<a href=\"#z\">Z</a>");

%>
</p>
<table class="access" cellspacing="1" cellpadding="0" >
	<tr>
		<td width="200px">
			&nbsp;
		</td>
		<td>
			<input type="image" title="Neuen Benutzer anlegen" name="new" src="./admin/images/install.png">
		</td>
	</tr>
	<%
		String header = "";
		for (int i=0; i<userids.size(); i++){
			if (! header.equals(((String)userids.get(i)).substring(0,1).toUpperCase())){

				out.println("<tr><th colspan=\"2\"><A name=\"" + ((String)userids.get(i)).substring(0,1).toUpperCase() + "\">" + ((String)userids.get(i)).substring(0,1).toUpperCase() + "</a></th></tr>");
				header = ((String)userids.get(i)).substring(0,1).toUpperCase();
			}
			
			out.println("<tr>");
			out.println("<td class=\"rule\">" + (String)userids.get(i) + "</td>");
			out.println("<td class=\"rule\"><input type=\"image\" title=\"Benutzer bearbeiten\" name=\"e"+ (String)userids.get(i) +"\" src=\"./admin/images/edit.png\"> <input type=\"image\" title=\"Benutzer löschen\" name=\"d"+ (String)userids.get(i) +"\" src=\"./admin/images/delete.png\" onClick=\"return questionDel()\"></td>");
			out.println("</tr>");
		}
	%>
</table>
<input type="hidden" name="operation" value="detail">

</form>