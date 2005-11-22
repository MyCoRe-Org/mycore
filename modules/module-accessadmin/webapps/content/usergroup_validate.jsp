<%@ page import="java.util.Enumeration,
			org.mycore.user.MCRGroup,
			org.mycore.user.MCRUserMgr,
			org.mycore.user.MCRPrivilege,
			java.util.ArrayList,
			java.lang.Exception"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	String operation = request.getParameter("operation");
	String paramName = "";

	String groupid = "";
	if (request.getParameter("gid_orig")!=null)
		groupid = request.getParameter("gid_orig");
	MCRGroup group = null;

	

try{
	if (groupid.equals("-")){
		// create new group
		group =  new MCRGroup(request.getParameter("gid"), request.getParameter("creator"), null, null, request.getParameter("description"), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList(), new ArrayList());
		MCRUserMgr.instance().createGroup(group);
		operation="detail";
		groupid=request.getParameter("gid");
	}

	if(operation.equals("detail")){
		Enumeration paramNames = request.getParameterNames();
		while(paramNames.hasMoreElements()) {
			paramName = (String)paramNames.nextElement();
			if(paramName.indexOf(".x")!=-1){
				break;
			}
		}

		String val = "";
		String op = "";
		if (paramName!="" && paramName.indexOf(".x")!=-1){
			val = paramName.substring(0,paramName.indexOf(".x"));
			op = val.substring(0,1);
			if (groupid==null && val.length()>0)
				groupid = val.substring(1);
		}

		if (op.equals("e")){
			// edit
			response.sendRedirect(WebApplicationBaseURL + "admin?path=usergroup_edit&id=" + val.substring(1));
		}else if (op.equals("d")){
			// delete
			MCRUserMgr.instance().deleteGroup(val.substring(1));
			response.sendRedirect(WebApplicationBaseURL + "admin?path=usergroup");
		}else if(op.equals("n")){
			// new
			response.sendRedirect(WebApplicationBaseURL + "admin?path=usergroup_edit&operation=new");
		}else{

			group = MCRUserMgr.instance().retrieveGroup(groupid);
			String[] values = null;
			ArrayList templ = null;

			// admingroup
			out.println(" save admingroup");
			templ = group.getAdminGroupIDs();
			for (int i=0; i< templ.size(); i++){
				group.removeAdminGroupID((String) templ.get(i));
			}
			if ( request.getParameterValues("admingroup")!=null){
				values = request.getParameterValues("admingroup");
				for(int i=0; i< values.length; i++){
					group.addAdminGroupID(values[i]);
				}
			}

			// adminuser
			out.println(" save adminuser");
			templ = group.getAdminUserIDs();
			for (int i=0; i< templ.size(); i++){
				group.removeAdminUserID((String) templ.get(i));
			}
			if(request.getParameterValues("adminuser")!=null){
				values = request.getParameterValues("adminuser");
				for(int i=0; i< values.length; i++){
					group.addAdminUserID(values[i]);
				}
			}

			// membergroup
			out.println(" save membergroup");
			templ = group.getMemberGroupIDs();
			for (int i=0; i< templ.size(); i++){
				group.removeMemberGroupID((String) templ.get(i));
			}
			if(request.getParameterValues("membergroup")!=null){
				values = request.getParameterValues("membergroup");
				for(int i=0; i< values.length; i++){
					group.addMemberGroupID(values[i]);
				}
			}

			// memberuser
			out.println(" save memberuser");
			templ = group.getMemberUserIDs();
			for (int i=0; i< templ.size(); i++){
				group.removeMemberUserID((String) templ.get(i));
			}
			if (request.getParameterValues("memberuser")!=null){
				values = request.getParameterValues("memberuser");
				for(int i=0; i< values.length; i++){
					group.addMemberUserID(values[i]);
				}
			}
			
			// privs
			out.println(" save privs");
			templ = group.getAllPrivileges();
			for (int i=0; i< templ.size(); i++){
				group.removePrivilege((String) templ.get(i));
			}
			if (request.getParameterValues("privs")!=null){

				values = request.getParameterValues("privs");
				for(int i=0; i< values.length; i++){
					group.addPrivilege(values[i]);
				}
			}

			//TODO: add changes of name and description


			MCRUserMgr.instance().updateGroup(group);
			response.sendRedirect(WebApplicationBaseURL + "admin?path=usergroup");
		}
	}
}catch(Exception e){
	out.print(e);
}
%>