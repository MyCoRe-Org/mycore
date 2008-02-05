<%@ page import="java.util.Enumeration,
		org.mycore.user.MCRUserMgr, 
		org.mycore.user.MCRUser, 
		java.util.ArrayList,
		java.sql.Timestamp,
		java.text.SimpleDateFormat,
		java.text.DateFormat,
		java.util.Date" %>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>        
<%
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
	String operation = request.getParameter("operation");
	String paramName = "";

	DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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
			// edit userdata
			response.sendRedirect(WebApplicationBaseURL + "admin?path=user_edit&id=" + val.substring(1));
		}else if (op.equals("d")){
			MCRUserMgr.instance().deleteUser(val.substring(1));
			response.sendRedirect(WebApplicationBaseURL + "admin?path=user");
		}else if (op.equals("n")){
			// new user
			response.sendRedirect(WebApplicationBaseURL +"admin?path=user_edit");
		}


	}else if (operation.equals("edit")){
		// save
		boolean idEnabled=false;
		boolean updateAllowed=false;
		ArrayList l = new ArrayList();
		String[] values = request.getParameterValues("ugroups");
		
		if (values!=null && values[0].substring(0,1)!="("){
			for(int i=0; i< values.length; i++){
				l.add(values[i]);
			}
		}

		if(request.getParameter("uenabled")!=null)
			idEnabled=true;
		if(request.getParameter("uupdate")!=null)
			updateAllowed=true;

		MCRUser user = null;
		int id = 0;
		String creationdate = "";

		if (request.getParameter("uid_orig").equals("")){
			id = MCRUserMgr.instance().getMaxUserNumID()+1;
			creationdate  = request.getParameter("creationtime");
		}else{
			MCRUser db_user = MCRUserMgr.instance().retrieveUser(request.getParameter("uid_orig"));
			id = db_user.getNumID();
			creationdate = df.format(db_user.getCreationDate());
		}

		user = new MCRUser(
			id,
			request.getParameter("uid"),
			request.getParameter("creator"),
			Timestamp.valueOf(creationdate),
			Timestamp.valueOf(request.getParameter("creationtime")),
			idEnabled,
			updateAllowed,			
			request.getParameter("udescr"),
			request.getParameter("upass"),
			request.getParameter("uprimgroup"),
			l,
			request.getParameter("usalutation"),
			request.getParameter("ufirstname"),
			request.getParameter("uname"),
			request.getParameter("uaddress"),
			request.getParameter("ucity"),
			request.getParameter("upostal"),
			request.getParameter("ucountry"),
			request.getParameter("ucountry"),
			request.getParameter("uinstitution"),
			request.getParameter("ufaculty"),
			request.getParameter("udept"),
			request.getParameter("uinstitute"),
			request.getParameter("utel"),
			request.getParameter("ufax"),
			request.getParameter("uemail"),
			request.getParameter("umobile"));

		MCRUserMgr manager = MCRUserMgr.instance();

		if (request.getParameter("uid_orig").equals("")){
			// create new user
			manager.createUser(user);
		}else{
			// update user
			manager.updateUser(user);
		}
		response.sendRedirect(WebApplicationBaseURL + "admin?path=user");
	}
%>