<%@ page contentType="text/html; charset=utf8"   %>

<%
  // ausloggen 
  if (request.getParameter("logoff") != null) {
    session.invalidate();
    response.sendRedirect("index.jsp");
    return;
  }
%>

<html>
<head>
<title>Benutzerkennung auswerten</title>
</head>
<body bgcolor="white">

Sie sind als Benutzer <b>«<%= request.getRemoteUser() %>»</b>
<%
  // verwendetes Passwort abfragen
  String[] roles={""};
  if (request.getUserPrincipal() != null) {
        java.security.Principal p = request.getUserPrincipal();
        if ( org.apache.catalina.realm.GenericPrincipal.class.getName().equals( p.getClass().getName()  )  ) {
            String credential = (String)  p.getClass().getMethod("getPassword", null).invoke(p, null);        
            out.println( "mit dem Passwort <b>«" + credential + "»</b>" );
            roles=(String[]) p.getClass().getMethod("getRoles",null).invoke(p, null);
        } 
  }
%>
eingeloggt. <p>

<%
  // Rolle überprüfen
  String role = request.getParameter("role");
  if (role == null)
      role = "";
  if (role.length() > 0) {
      if (request.isUserInRole(role)) {
          out.println( "Sie sind Mitglied in der Gruppe <b>«" + role + "»</b>");
      } else {
          out.println( "Sie sind <i>nicht</i> Mitglied in der Gruppe <b>«" + role + "»</b>");
      }
  }
%><p>
<%
	if (roles.length>0){
	    out.println("Sie haben folgende Rollen:<ul>");
	    for (String cRole:roles){
	        out.println("<li>"+cRole+"</li>");
	    }
	    out.println("</ul>");
	}
%>

<form method="GET" action='<%= response.encodeURL("index.jsp") %>'>
Hiermit kann überprüft werden, ob man Mitglied einer bestimmten Gruppe ist:
<input type="text" name="role" value="<%= role %>">
</form>

<p>
<a href='<%= response.encodeURL("index.jsp?logoff=true") %>'>Ausloggen</a>.


</body>
</html>
