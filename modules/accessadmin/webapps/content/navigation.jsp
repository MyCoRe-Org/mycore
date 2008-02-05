<%@ page import="java.util.StringTokenizer"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	val = (String)request.getParameter("path");    
	if (val == null){
		val = "";
	}else{
		StringTokenizer st = new StringTokenizer(val,"_");
		while (st.hasMoreTokens()) {
	         val = st.nextToken();
			 break;
	    }
	}
%>


<span>
	<ul class="nav">
		<li class="<%=get("")%>"><a href="<%= WebApplicationBaseURL %>admin">Home</a></li>
		<li class="<%=get("rules")%>"><a href="<%= WebApplicationBaseURL %>admin?path=rules">Regeleditor</a></li> 
		<li class="<%=get("access")%>"><a href="<%= WebApplicationBaseURL %>admin?path=access">Regelzuweisung</a></li>
	</ul>
	<ul class="nav">
		<li class="<%=get("usergroup")%>"><a href="<%= WebApplicationBaseURL %>admin?path=usergroup">Benutzergruppen</a></li> 
		<li class="<%=get("user")%>"><a href="<%= WebApplicationBaseURL %>admin?path=user">Benutzerverwaltung</a></li>
	</ul>
</span>


<%!
	String val;
	public String get(String linkpath){
		if (val.equals(linkpath)){
			return "navact";
		}else{
			return "nav";
		}
	}

%>

