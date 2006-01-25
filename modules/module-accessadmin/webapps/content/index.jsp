<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.user.MCRUserMgr,
	org.mycore.user.MCRUser,
	org.mycore.user.MCRGroup,
	org.mycore.access.mcrimpl.MCRAccessRule,
	org.mycore.common.MCRSession,
	org.mycore.frontend.servlets.MCRServlet"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page import="org.mycore.user.MCRGroup" %>

<%
	MCRSession mcrSession = MCRServlet.getSession(request);
	MCRUser user = MCRUserMgr.instance().retrieveUser(mcrSession.getCurrentUserID());
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	String pageurl = (String) request.getAttribute("page");

	if(! user.isMemberOf(new MCRGroup("admingroup"))){
		pageurl="error.jsp";
	}

%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
		<title>MyCoRe Administration Interface</title>
		<script src="<%=WebApplicationBaseURL%>/admin/admin.js" type=text/javascript></script>
		<link rel="stylesheet" href="<%=WebApplicationBaseURL%>/admin/css/admin.css" />
	</head>
	<body>

<%
	if (((String) request.getAttribute("page")).equals("rules_editor.jsp")){	
%>
		<jsp:include page="<%=pageurl%>"/>

<%	// normal page
	}else{
%>

		<table cellpadding="0" cellspacing="0" id="mytable">
			<tr>
				<td id="mainLeftColumn" rowspan="2">
					<!--  <img src="../docportal/templates/master/template_mycoresample-1/IMAGES/logo.gif" alt="MyCoRe-Logo" /> -->
					<br />
		    		<jsp:include page='navigation.jsp'/>
				</td>

				<td valign="top">
					<table cellpadding="0" cellspacing="0" id="maintable">
						<tr>
							<td valign="top">
								<table width="100%" height="100%" cellpadding="0" cellspacing="0">
									<tr height="20px">
										<td valign="bottom" align="right" id="mainTopColumn" style="padding:10px">
											<h1>MyCoRe Administration Interface</h1>
										</td>
									</tr>
									<tr>
										<td valign="top" style="padding:5px">
					    					<jsp:include page="<%=pageurl%>"/>
										</td>
									</tr>
								</table>
							</td>
						</tr>
					</table>
				</td>
			</tr>
			<tr>
				<td valign="bottom"><div id="footer"><small>User: <%=mcrSession.getCurrentUserID()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;ver. 0.2</small></div></td>
			</tr>
		</table>
<%
	}
%>
	</body>
</html>

