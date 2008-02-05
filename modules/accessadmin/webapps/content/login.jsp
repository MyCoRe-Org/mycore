<%@ page import="org.mycore.common.MCRSession,
                 org.mycore.common.MCRSessionMgr,
                 org.mycore.common.MCRConfiguration,
                 org.mycore.frontend.servlets.MCRServlet"%>
<% { %>
<%
    MCRSession mcrSession = MCRServlet.getSession(request);
    String username = mcrSession.getCurrentUserID();
    if(username == null)
        username = MCRConfiguration.instance().getString("MCR.users_guestuser_username");

%>

<table class="frame">
<tr>
<td>

    <table cellspacing="0" cellpadding="0" class="frameintern">
    <tr style="height:20px;">
    <td class="resultcmd">Sie sind derzeit angemeldet als:
    &nbsp;&nbsp;[&nbsp;<span class="username"><%=username%></span>&nbsp;]
    </td>
    </tr>
    </table>

    <table style="height:20px;" class="frameintern">
    <tr>
    <td colspan="2">&nbsp;</td>
    </tr>
    </table>

    <%
    String error = (String)request.getAttribute("error");
    if(error!=null) {
    %>

    <table cellspacing="0" cellpadding="0" class="frameintern">
    <tr style="height:20px;">
    <td class="error">
    <font color="red">Die Anmeldung ist fehlgeschlagen! <br/><%=error%>
    </font>
    </td>
    </tr>
    </table>

    <%
    }
    %>

    <form method="post" action="" class="login">

    <input name="url" value="" type="hidden">

    <table style="height:50px;" class="frameintern">
    <tr>
	<td class="inputcaption">Benutzerkennung:</td>
	<td class="inputfield"><input maxlength="30" class="text" type="text" name="uid"></td>
    </tr>
    <tr>
	<td class="inputcaption">Passwort:</td>
	<td class="inputfield"><input maxlength="30" class="text" type="password" name="pwd"></td>
    </tr>
    <tr>
	<td colspan="2">&nbsp;</td>
    </tr>
    <tr>
	<td class="logincmd">&nbsp;</td>
	<td style="white-space: nowrap;vertical-align:top;" class="resultcmd">
	&nbsp;
	<input name="LoginSubmit" value="Anmelden" class="submitbutton" type="submit">
	&nbsp;
	<input name="LoginReset" value="Abbrechen" class="submitbutton" type="reset"></td>
    </tr>
    </table>

    </form>
    </td>
    </tr>
</table>
<% } %>