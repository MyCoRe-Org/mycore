<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.user.MCRUser,
	org.mycore.user.MCRUserContact,
	org.mycore.user.MCRUserMgr,
	org.mycore.common.MCRSession,
	org.mycore.frontend.servlets.MCRServlet,
	java.text.SimpleDateFormat,
	java.text.DateFormat,
	java.util.ArrayList,
	java.util.Date,
	java.util.Collections,
	java.lang.Exception"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%
	

    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	MCRUser user = null;
	MCRUserContact contact = null;
	ArrayList grouplist = MCRUserMgr.instance().getAllGroupIDs();
	Collections.sort(grouplist);

	if (request.getParameter("id")==null){
		user = new MCRUser(0,null, MCRServlet.getSession(request).getCurrentUserID(), null,null, true, true, null, null, null, null, null, null, null, null, null, null, null, null, null,null,null,null,null,null,null,null);

		contact = user.getUserContact();
	}else{
		user = MCRUserMgr.instance().retrieveUser(request.getParameter("id"));
		contact = user.getUserContact();
	}

%>

<SCRIPT TYPE="text/javascript">
	function validateOnSubmit() {
		var elem;
	    var errs=0;
        if (!validatePresent(document.forms.details.uid)) errs += 1;
		if (!validatePresent(document.forms.details.upass)) errs += 1;
		if (!validatePresent(document.forms.details.ufirstname)) errs += 1;
		if (!validatePresent(document.forms.details.uname)) errs += 1;
		if (!validatePresent(document.forms.details.uprimgroup)) errs += 1;

		if (errs > 0){
			alert("Bitte kontrollieren Sie die markierten Felder.");
			return false;
		}else{
			return true;
		}
	}
</SCRIPT>

<h4>Benutzer bearbeiten</h4>

<p><a href="<%= WebApplicationBaseURL %>admin?path=user">zur Übersicht</a></p>

<form name="details" method="post" onSubmit="return validateOnSubmit()" action="<%= WebApplicationBaseURL %>admin/user_validate.jsp">
	
	<table class="access">
		<tr>
			<td>Benutzerkennung <sup class="required">*</sup>:</td>
			<td>
				<input type="text" name="uid" id="uid" value="<%=user.getID()%>" maxlength="20" size="30" style="width:209px" onchange="validatePresent(this);">
				<input type="hidden" name="uid_orig" value="<%=user.getID()%>">
			</td>
		</tr>
		<tr>
			<td>Passwort <sup class="required">*</sup>:</td>
			<td>
				<input type="password" name="upass" id="upass" value="<%=user.getPassword()%>" maxlength="200" size="30" style="width:209px" onchange="validatePresent(this);">
			</td>
		</tr>
		<tr>
			<td>Beschreibung:</td>
			<td>
				<input type="text" name="udescr" value="<%=user.getDescription()%>" maxlength="200" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input type="checkbox" name="uenabled" value="true"
				<% if (user.isEnabled()) out.print("checked"); %>
				> Benutzerkennung aktiv
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input type="checkbox" name="uupdate" value="true"
					<% if (user.isUpdateAllowed()) out.print("checked"); %>
				> Accountdaten änderbar
			</td>
		</tr>
		<tr>
			<td>Primäre Gruppe <sup class="required">*</sup>:</td>
			<td>
				<select style="width:215px" name="uprimgroup" onchange="validatePresent(this);">
					<option>(bitte auswählen)</option>
				<%
					for (int i=0; i<grouplist.size(); i++){
						if (user.getPrimaryGroupID().equals((String)grouplist.get(i))){
							out.print("<option value=\"" + (String)grouplist.get(i) + "\" selected>" + (String)grouplist.get(i) + "</option>");
						}else{
							out.print("<option value=\"" + (String)grouplist.get(i) + "\">" + (String)grouplist.get(i) + "</option>");
						}
					}
				%>		
				</select>
			</td>
		</tr>
		<tr>
			<td valign="top">Weitere Gruppen:</td>
			<td>
				<select multiple size="5" style="width:215px" name="ugroups">
					<option>(Mehrfachauswahl)</option>
				<%
				
					ArrayList gl = new ArrayList();
					if(user.getGroupCount()>0){
						gl = user.getGroupIDs();
					}

					for (int i=0; i<grouplist.size(); i++){
						if(gl.contains((String)grouplist.get(i))){
							out.print("<option value=\"" + (String)grouplist.get(i) + "\" selected>" + (String)grouplist.get(i) + "</option>");
						}else{
							out.print("<option value=\"" + (String)grouplist.get(i) + "\">" + (String)grouplist.get(i) + "</option>");
						}
					}
				%>		
				</select>
			</td>
		</tr>
		<tr>
			<td colspan="2" align="center" height="20px"><b>Kontaktinformationen</b></td>
		</tr>
		<tr>
			<td>Anrede / Titel:</td>
			<td>
				<input type="text" name="usalutation" value="<%=contact.getSalutation()%>" maxlength="24" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Vorname <sup class="required">*</sup>:</td>
			<td>
				<input type="text" name="ufirstname" id="ufirstname" value="<%=contact.getFirstName()%>" maxlength="64" size="30" style="width:209px" ONCHANGE="validatePresent(this);">
			</td>
		</tr>
		<tr>
			<td>Nachname <sup class="required">*</sup>:</td>
			<td>
				<input type="text" name="uname" id="uname" value="<%=contact.getLastName()%>" maxlength="32" size="30" style="width:209px" ONCHANGE="validatePresent(this);">
			</td>
		</tr>
		<tr>
			<td>Adresse:</td>
			<td><input type="text" name="uaddress" value="<%=contact.getStreet()%>" maxlength="64" size="30" style="width:209px"></td>
		</tr>
		<tr>
			<td>PLZ:</td>
			<td>
				<input type="text" name="upostal" value="<%=contact.getPostalCode()%>" maxlength="32" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Stadt:</td>
			<td>
				<input type="text" name="ucity" value="<%=contact.getCity()%>" maxlength="32" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Land:</td>
			<td>
				<input type="text" name="ucountry" value="<%=contact.getCountry()%>" maxlength="32" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Institution:</td>
			<td>
				<input type="text" name="uinstitution" value="<%=contact.getInstitution()%>" maxlength="64" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Fakultät/Fachbereich:</td>
			<td>
				<input type="text" name="ufaculty" value="<%=contact.getFaculty()%>" maxlength="64" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Abteilung:</td>
			<td>
				<input type="text" name="udept" value="<%=contact.getDepartment()%>" maxlength="64" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Institut/Einrichtung:</td>
			<td>
				<input type="text" name="uinstitute" value="<%=contact.getInstitute()%>" maxlength="64" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Email:</td>
			<td>
				<input type="text" name="uemail" value="<%=contact.getEmail()%>" maxlength="64" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Telefon:</td>
			<td><input type="text" name="utel" value="<%=contact.getTelephone()%>" maxlength="32" size="30" style="width:209px"></td>
		</tr>
		<tr>
			<td>Fax-Nummer:</td>
			<td>
				<input type="text" name="ufax" value="<%=contact.getFax()%>" maxlength="32" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>Mobiles-Telefon:</td>
			<td>
				<input type="text" name="umobile" value="<%=contact.getCellphone()%>" maxlength="32" size="30" style="width:209px">
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td><sup class="required">* Pflichtfeld</sup></td>
		</tr>
		<tr>
			<td>&nbsp;
				<input type="hidden" name="creator" value="<%=user.getCreator()%>">
				<input type="hidden" name="creationtime" value="<%=df.format(user.getCreationDate())%>">
				<input type="hidden" name="operation" value="edit">
			</td>
			<td>
				<small>
					<%=user.getCreator()%>,&nbsp;
					<%=df.format(user.getCreationDate())%>					
				</small>
			</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
			<td>
				<input type="reset">
				&nbsp;
				<input type="submit" onclick=return validateOnSubmit()  value="Speichern">
			</td>
		</tr>
	</table>

</form>