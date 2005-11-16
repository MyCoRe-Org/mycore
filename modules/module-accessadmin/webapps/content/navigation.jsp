<%@ page import="java.util.StringTokenizer"%>
<%
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
		<li class="<%=get("")%>"><a href="admin">Home</a></li>
		<li class="<%=get("rules")%>"><a href="?path=rules">Regeleditor</a></li> 
		<li class="<%=get("access")%>"><a href="?path=access">Regelzuweisung</a></li>
	</ul>
	<ul class="nav">
		<li class="<%=get("usergroup")%>"><a href="?path=usergroup">Benutzergruppen</a></li> 
		<li class="<%=get("user")%>"><a href="?path=user">Benutzerverwaltung</a></li>
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

