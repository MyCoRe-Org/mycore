<%@ page import="org.mycore.common.MCRSession,
	org.mycore.common.MCRSessionMgr,
	org.mycore.access.mcrimpl.MCRAccessStore,
	org.mycore.access.mcrimpl.MCRRuleMapping,
	java.util.List,
	java.util.Date"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%!
	List pool = null;
	MCRRuleMapping mapping = null;
	MCRSession mcrSession = null;
%>
<%
	pool = MCRAccessStore.getPools();
	mcrSession = MCRSessionMgr.getCurrentSession();
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	String operation = request.getParameter("operation");

	if(operation.equals("save")){
		String ids = request.getParameter("ids");
		String[] id = ids.split(" ");

		for(int i=0; i<id.length; i++){
			for (int j=0; j<pool.size(); j++){
				mapping = new MCRRuleMapping();
				mapping.setObjId(id[i]);
				mapping.setPool((String) pool.get(j));
				if(id.length==1){
					mapping.setRuleId(request.getParameter(id[i] + "_" + (String) pool.get(j)));
				}else{
					mapping.setRuleId(request.getParameter((String) pool.get(j)));
				}
				mapping.setCreationdate(new Date());
				mapping.setCreator(mcrSession.getCurrentUserID());
				if (mapping.getRuleId().equals("") || mapping.getRuleId()==null){
					MCRAccessStore.getInstance().deleteAccessDefinition(mapping);					
				}else{
					MCRAccessStore.getInstance().updateAccessDefinition(mapping);
				}
				mapping=null;
			}
		}
		response.sendRedirect(WebApplicationBaseURL +"admin?path=access");
	}else if(operation.equals("detail")){
		String ids = request.getParameter("ids");
		mcrSession.deleteObject("access_ids");
		mcrSession.put("access_ids", ids);
    
		response.sendRedirect(WebApplicationBaseURL +"admin?path=access_edit");
	}

	
%>

