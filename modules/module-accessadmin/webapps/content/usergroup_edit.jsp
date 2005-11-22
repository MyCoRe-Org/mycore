<%@ page import="org.mycore.common.MCRSession,
	org.mycore.frontend.servlets.MCRServlet,
	org.mycore.user.MCRGroup,
	org.mycore.user.MCRUserMgr,
	org.mycore.user.MCRPrivilege,
	java.text.SimpleDateFormat,
	java.text.DateFormat,
	java.util.Date,
	java.util.ArrayList,
	java.util.Collections"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%
	
	MCRSession mcrSession = MCRServlet.getSession(request);
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
    String operation = request.getParameter("operation");
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	ArrayList userlist = MCRUserMgr.instance().getAllUserIDs();
	ArrayList grouplist = MCRUserMgr.instance().getAllGroupIDs();
	ArrayList privilegelist = MCRUserMgr.instance().getAllPrivileges();

	Collections.sort(userlist);
	Collections.sort(grouplist);


	MCRGroup group = null;
	ArrayList l = null;
	if (operation == null)
		operation = "detail";

	if(request.getParameter("id") == null){
		//new
		group =  new MCRGroup("-", "root", null, null, "",l,l,l,l,l,l);
		operation = "new";
	}else{
		//edit
		if (mcrSession.get("group") == null){
			group = MCRUserMgr.instance().retrieveGroup((String)request.getParameter("id"));
		}else{
			group = (MCRGroup) mcrSession.get("group");
		}
	}

%>
<SCRIPT TYPE="text/javascript">
	function validateOnSubmit() {
		var elem;
	    var errs=0;
        if (!validatePresent(document.forms.details.gid)) errs += 1;

		if (errs > 0){
			alert("Bitte kontrollieren Sie die markierten Felder.");
			return false;
		}else{
			return true;
		}
	}
</SCRIPT>

<h4>Benutzergruppe bearbeiten</h4>

<p><a href="./admin?path=usergroup">zur Übersicht</a></p>


<form name="details" method="post" action="<%= WebApplicationBaseURL %>admin/usergroup_validate.jsp" onSubmit="return validateOnSubmit()">
	<table class="access">
		<tr>
			<td>Gruppen-ID <sup class="required">*</sup>:</td>
			<td><input type="text" name="gid" value="<% if(! operation.equals("new")){ out.print(group.getID());}%>" maxlength="20" size="20" onchange="validatePresent(this);"><input type="hidden" name="gid_orig" value="<%=group.getID()%>"></td>
		</tr>
		<tr>
			<td valign="top">Beschreibung:</td>
			<td><textarea name="description" cols="50" rows="4"><%=group.getDescription()%></textarea></td>
		</tr>
		<tr>
			<td colspan="2">
				&nbsp;
			</td>
		</tr>
		<tr>
			<td valign="top">Admins:</td>
			<td>
				<table class="access">
					<tr>
						<th>Gruppen</th>
						<th>Benutzer</th>
					</tr>
					<tr>
						<td width="200px">
							<select multiple size="5" name="admingroup" id="admingroup" style="width:200px">
							<%
								if (!operation.equals("new"))
									l = group.getAdminGroupIDs();
								for(int i=0; i<grouplist.size(); i++){

									if (l!=null && l.contains((String) grouplist.get(i)))
										out.println("<option selected value=\""+(String) grouplist.get(i)+"\">"+(String) grouplist.get(i)+"</option>");
									else
										out.println("<option value=\""+(String) grouplist.get(i)+"\">"+(String) grouplist.get(i)+"</option>");
								}
							%>
							</select>
							<small>
								<a href="#" onclick="deselectall('admingroup')">Auswahl aufheben</a> |
								<a href="#" onclick="selectall('admingroup')">alle wählen</a>
							</small>
						</td>
						<td width="200px">
							<select multiple size="5" name="adminuser"  id="adminuser" style="width:200px">
							<%
								if (!operation.equals("new"))
									l = group.getAdminUserIDs();
								for(int i=0; i<userlist.size(); i++){

									if (l!=null && l.contains((String) userlist.get(i)))
										out.println("<option selected value=\""+(String) userlist.get(i)+"\">"+(String) userlist.get(i)+"</option>");
									else
										out.println("<option value=\""+(String) userlist.get(i)+"\">"+(String) userlist.get(i)+"</option>");
								}
							%>
							</select>
							<small>
								<a href="#" onclick="deselectall('adminuser')">Auswahl aufheben</a> |
								<a href="#" onclick="selectall('adminuser')">alle wählen</a>
							</small>
						</td>
					</tr>
				</table>
			</td>
			<tr>
			<td valign="top">Mitglieder:</td>
			<td>
				<table class="access">
					<tr>
						<th>Gruppen</th>
						<th>Benutzer</th>
					</tr>
					<tr>
						<td width="200px">
							<select multiple size="5" name="membergroup" id="membergroup" style="width:200px">
							<%
								if (!operation.equals("new"))
									l = group.getMemberGroupIDs();
								for(int i=0; i<grouplist.size(); i++){

									if (l!=null && l.contains((String) grouplist.get(i)))
										out.println("<option selected value=\""+(String) grouplist.get(i)+"\">"+(String) grouplist.get(i)+"</option>");
									else
										out.println("<option value=\""+(String) grouplist.get(i)+"\">"+(String) grouplist.get(i)+"</option>");
								}
							%>
							</select>
							<small>
								<a href="#" onclick="deselectall('membergroup')">Auswahl aufheben</a> |
								<a href="#" onclick="selectall('membergroup')">alle wählen</a>
							</small>
						</td>
						<td width="200px">
							<select multiple size="5" name="memberuser" id="memberuser" style="width:200px">
							<%
								if (!operation.equals("new"))
									l = group.getMemberUserIDs();
								for(int i=0; i<userlist.size(); i++){

									if (l!=null && l.contains((String) userlist.get(i)))
										out.println("<option selected value=\""+(String) userlist.get(i)+"\">"+(String) userlist.get(i)+"</option>");
									else
										out.println("<option value=\""+(String) userlist.get(i)+"\">"+(String) userlist.get(i)+"</option>");
								}
							%>
							</select>
							<small>
								<a href="#" onclick="deselectall('memberuser')">Auswahl aufheben</a> |
								<a href="#" onclick="selectall('memberuser')">alle wählen</a>
							</small>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td valign="top">Privilegien</td>
			<td>
				<table class="access">
					<tr>
						<td width="200px">
							<select multiple size="5" name="privs" id="privs" style="width:200px">
							<%
								if (!operation.equals("new"))
									l = group.getPrivileges();
								for(int i=0; i<privilegelist.size(); i++){
									String priv = ((MCRPrivilege) privilegelist.get(i)).getName();
									if (l!=null && l.contains(priv))
										out.println("<option selected value=\""+priv+"\">"+priv+"</option>");
									else
										out.println("<option value=\""+priv+"\">"+priv+"</option>");
								}
							%>
							</select>
							<small>
								<a href="#" onclick="deselectall('privs')">Auswahl aufheben</a> |
								<a href="#" onclick="selectall('privs')">alle wählen</a>
							</small>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><sup class="required">* Pflichtfeld</sup></td>
		</tr>
		<tr>
			<td>&nbsp;
				<input type="hidden" name="creator" value="<%=group.getCreator()%>">
				<input type="hidden" name="creationtime" value="<%=df.format(group.getCreationDate())%>">
				<input type="hidden" name="operation" value="<%=operation%>">
			</td>
			<td>
				<small>
					<%=group.getCreator()%>,&nbsp;
					<%=df.format(group.getCreationDate())%>
				</small>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><input type="reset">&nbsp;<input type="submit" value="Speichern"></td>
		</tr>
	</table>
</form>

