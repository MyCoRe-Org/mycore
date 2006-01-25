<%@ page import="org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.access.mcrimpl.MCRAccessRule,
	java.util.Date,
	java.text.SimpleDateFormat,
	java.text.DateFormat,
	org.mycore.common.MCRSession,
	org.mycore.frontend.servlets.MCRServlet,
	java.util.Enumeration"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>    
<%
	MCRSession mcrSession = MCRServlet.getSession(request);
    
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	String operation = request.getParameter("operation");
	String paramName = "";

	if(operation.equals("detail")){
		Enumeration paramNames = request.getParameterNames();
		while(paramNames.hasMoreElements()) {
	      paramName = (String)paramNames.nextElement();
			if(paramName.indexOf(".x")!=-1){
				 break;
			}
		}
		String val = paramName.substring(0,paramName.indexOf(".x"));
		String op = val.substring(0,1);

		if (op.equals("e")){
			// edit
			response.sendRedirect(WebApplicationBaseURL + "admin?path=rules_edit&id=" + val.substring(1));
		}else if (op.equals("d")){
			// delete
			MCRRuleStore.getInstance().deleteRule(val.substring(1));
			response.sendRedirect(WebApplicationBaseURL + "admin?path=rules");
		}else{
			response.sendRedirect(WebApplicationBaseURL + "admin?path=rules_edit");
		}
	}else{
		String id = request.getParameter("rid");
		String id_orig = request.getParameter("rid_orig");
		String ruleString = request.getParameter("rule");
		String description = request.getParameter("description");
		String creator = request.getParameter("creator");
		String dateString =request.getParameter("creationtime");
	
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date creationtime = new Date();
		creationtime = df.parse(dateString);

		String resultpage = WebApplicationBaseURL + "admin?path=rules";

	
		//validation
		MCRAccessRule rule = new MCRAccessRule(id, creator, creationtime, ruleString, description);

		if (request.getParameter("operation").equals("edit")){
			// update rule
			if (! id_orig.equals(id) && MCRRuleStore.getInstance().existsRule(id)){
				// error 
				rule.setId(id_orig);
				mcrSession.put("rule", rule);
				mcrSession.put("err_msg", "Duplicate key");
				resultpage = WebApplicationBaseURL + "admin?path=rules_edit&id=" + id;
			}else{
				if (! id_orig.equals(id)){
					MCRRuleStore.getInstance().deleteRule(id_orig);
				}
				MCRRuleStore.getInstance().updateRule(rule);
				mcrSession.deleteObject("rule");
			}
		}else if (request.getParameter("operation").equals("new")){
			//create new rule
			MCRRuleStore.getInstance().createRule(rule);
		}

		response.sendRedirect(resultpage);

	}

	
%>
