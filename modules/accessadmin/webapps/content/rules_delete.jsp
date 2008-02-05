<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>

<%
	MCRRuleStore.getInstance().deleteRule(request.getParameter("id"));
%>

<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-strict.dtd">
<html>
<head>
<meta http-equiv="refresh" content="0; URL=<%= MCRServlet.getBaseURL() %>min?path=rules"/>
</head>
</html>