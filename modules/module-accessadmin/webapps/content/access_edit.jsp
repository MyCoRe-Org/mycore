<%@ page import="org.mycore.common.MCRSession,
	org.mycore.access.mcrimpl.MCRAccessStore,
	org.mycore.common.MCRSessionMgr,
	org.mycore.access.mcrimpl.MCRRuleStore,
	org.mycore.access.mcrimpl.MCRAccessDefinition,
	java.util.ArrayList,
	java.util.List"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%!
	List pool = null;
	String ids = "";
	String[] id = null;
%>
<%
pool = MCRAccessStore.getPools();
MCRSession mcrSession = MCRSessionMgr.getCurrentSession();
String WebApplicationBaseURL = MCRServlet.getBaseURL();
ids = (String) mcrSession.get("access_ids");
mcrSession.deleteObject("access_ids");

id = ids.split(" ");

if(id.length==1){
	// single selection
	MCRAccessStore accStore = MCRAccessStore.getInstance();
		
%>
	<h4>Einzelzuweisung</h4>
	<p><a href="<%= WebApplicationBaseURL %>admin?path=access">zur Übersicht</a></p>

	<form method=post action="<%= WebApplicationBaseURL %>admin/access_validate.jsp">
	<table  class="access" cellspacing="1" cellpadding="0">
		<tr>
			<td rowspan="2">&nbsp;</td>
			<td colspan="<%=pool.size()%>" class="pool">Pools</td>
		</tr>
		<tr>
			<%
				for(int i=0; i<pool.size(); i++){
					out.println("<td class=\"pool\">"+(String)pool.get(i)+"</td>");
				}
	
			%>
		</tr>
		<tr>
			<td><%=id[0]%>&nbsp;&nbsp;&nbsp;</td>
			<%
				List l = accStore.getRules(id[0]);
				for(int i=0; i<pool.size(); i++){
					for(int j=0; j<l.size(); j++){
						MCRAccessDefinition element = (MCRAccessDefinition) l.get(j);
						out.println("<td class=\"pool\">"+getRuleCombo(id[0]+"_"+(String)pool.get(i),(String) element.getPool().get((String)pool.get(i)))+"</td>");
					}

				}


				
 			%>
		</tr>
	</table><br />
		<input type="hidden" value="<%=ids%>" name="ids">
		<input type="hidden" value="save" name="operation">
		<input type="reset">&nbsp;<input type="submit" value="Speichern">
	<form>
<%

	}else{
		// multi selection
%>

	<h4>Mehrfachzuweisung</h4>
	<p><a href="<%= WebApplicationBaseURL %>/admin?path=access">zur Übersicht</a></p>

	<form method=post action="<%= WebApplicationBaseURL %>/admin/access_validate.jsp">
	<table  class="access" cellspacing="1" cellpadding="0">
		<tr>
			<td rowspan="2">&nbsp;</td>
			<td colspan="<%=pool.size()%>" class="pool">Pools</td>
		</tr>
		<tr>
			<%
				for(int i=0; i<pool.size(); i++){
					out.println("<td class=\"pool\">"+(String)pool.get(i)+"</td>");
				}
	
			%>
		</tr>
		<tr>
			<td>
			<%
				for(int i=0; i<id.length; i++){
					out.println(id[i]+"&nbsp;&nbsp;&nbsp;<br>");
				}
			%>			
			</td>
			<%
				//List l = accStore.getRules(id[0]);
				for(int i=0; i<pool.size(); i++){
					out.println("<td class=\"pool\">"+getRuleCombo((String)pool.get(i),"")+"</td>");
				}


				
 			%>
		</tr>
	</table><br />
		<input type="hidden" value="<%=ids%>" name="ids">
		<input type="hidden" value="save" name="operation">
		<input type="reset">&nbsp;<input type="submit" value="Speichern">
	<form>
<%


	}
%>

<%!

	MCRRuleStore rstore = MCRRuleStore.getInstance();

	public String getRuleCombo(String name, String val){
		ArrayList rules = rstore.retrieveAllIDs();

		String ret ="<select name=\""+name+"\">";
		
		ret+="<option value=\"\">-keine-</option>";

		for (int i=0; i< rules.size();i++){
			if (val.equals((String) rules.get(i))){
				ret+="<option selected value=\""+(String) rules.get(i)+"\">"+(String) rules.get(i)+"</option>";
			}else{
				ret+="<option value=\""+(String) rules.get(i)+"\">"+(String) rules.get(i)+"</option>";
			}
			
		}
		
		ret +="</select>";
		return ret;
	}


%>