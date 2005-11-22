<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<% String WebApplicationBaseURL = MCRServlet.getBaseURL(); %>
<table width="100%">
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>
			<h3>Das MyCoRe Administration Interface bietet folgende Funktionen:</h3>
		
			<ul>
				<li><a href="<%= WebApplicationBaseURL %>admin?path=rules">Regeleditor</a> zur Erstellung der Access Regeln</li>
				<li><a href="<%= WebApplicationBaseURL %>admin?path=access">Regelzuweisung</a>, um MyCoRe-Objekten Regeln zuweisen zu können</li>
				<li><a href="<%= WebApplicationBaseURL %>admin?path=usergroup">Benutzergruppenverwaltung</a> zur Erstellung der MyCoRe 	Benutzergruppen</li>
				<li><a href="<%= WebApplicationBaseURL %>admin?path=user">Benutzerverwaltung</a> zur Verwaltung der MyCoRe Benutzer</li>
			</ul>
		</td>
	</tr>
</table>
