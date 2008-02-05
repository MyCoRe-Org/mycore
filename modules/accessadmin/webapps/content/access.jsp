<%@ page import="org.mycore.access.mcrimpl.MCRAccessStore,
		org.mycore.access.mcrimpl.MCRAccessDefinition,
		org.mycore.common.MCRConfiguration,
		java.util.ArrayList,
		java.util.Hashtable,
		java.util.List,
		java.util.Set,
		java.util.Iterator,
		org.mycore.datamodel.metadata.MCRXMLTableManager"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>
<%!
	List pool=null;
%>
<%
	pool = MCRAccessStore.getPools();
    String WebApplicationBaseURL = MCRServlet.getBaseURL();
%>

<script>
	function setValue(obj){
		if (obj.checked==true){
			document.getElementById("ids").value += obj.name + " ";
		}else{
			document.getElementById("ids").value = document.getElementById("ids").value.replace(obj.name + " ","");
		}
	}

	function setID(val){
		if (countCheck()==0){
			document.getElementById("ids").value="";
		}
		document.getElementById("ids").value = document.getElementById("ids").value.replace(val + " ","");
		document.getElementById("ids").value += val + " ";
	}

	function selectAll(val){
		var theForm = document.getElementById("overview"), z=0;
		while (theForm[z].type =="checkbox") {
			if(theForm[z].name.indexOf("_")!=-1){
				theForm[z].checked = val;
				setValue(theForm[z]);
			}
			z++;
		}
    }

	function selectAllType(obj){
		var theForm = document.getElementById("overview"), z=0;
		while (theForm[z].type =="checkbox") {
			if(theForm[z].name.indexOf("_")!=-1 && theForm[z].name.indexOf(obj.name)!=-1){
			theForm[z].checked = !(theForm[z].checked);
			setValue(theForm[z]);
			}
			z++;
		}
		obj.checked = false;
    }

	function countCheck(){
		var theForm = document.getElementById("overview"), z=0, anz=0;
		while (theForm[z].type =="checkbox") {
			if(theForm[z].checked == true){
				anz++;
			}
			z++;
		}
		return anz;
	}
	
</script>


<h4>Regelzuweisung</h4>
<form method=post action="<%= WebApplicationBaseURL %>/admin/access_validate.jsp" id="overview">
<table border="0">
<tr>
<td>

<table class="access" cellspacing="1" cellpadding="0">
	<tr>
		<td rowspan=2">
			Vorhandene MyCoRe Objekte
		</td>

		<td class="pool" colspan="<%=pool.size()%>">
			Pools
		</td>
		<td rowspan="2">
			&nbsp;
		</td>
	</tr>
	<tr>
		<%
			for(int i=0; i< pool.size(); i++){
				out.println("<td class=\"pool\">"+(String) pool.get(i)+"</td>");
			}
		%>

	</tr>
<%

	List table = MCRXMLTableManager.instance().getAllAllowedMCRObjectIDTypes();
	Iterator it = table.iterator();
        
        while (it.hasNext()){
			String key= (String) it.next();
			List l1 = MCRAccessStore.getInstance().getDefinition(key);
			out.print("<tr><th colspan=\""+(pool.size()+1)+"\">" + key + "</th><th align=\"left\"><input type=\"checkbox\" name=\""+key+"\" onclick=\"selectAllType(this)\"></th></tr>");
			for(int i=0; i< l1.size(); i++){
				MCRAccessDefinition element = (MCRAccessDefinition) l1.get(i);
				out.print("<tr><td>" + element.getObjID()+"</td>");

				for (int j=0; j<pool.size(); j++){
					String val = (String)element.getPool().get((String) pool.get(j));
					if (val.equals(" ")){
						val="&nbsp;";
					}
					out.print("<td class=\"pool\">"+val+ "</td>");
				}
				out.print("<td><input type=\"checkbox\" name=\""+element.getObjID()+"\" onclick=\"setValue(this)\">&nbsp;&nbsp;<input type=\"image\" src=\"./admin/images/edit.png\" onclick=\"setID('"+element.getObjID()+"')\"></td>");
			}
			out.print("</tr>");
        }

%>

</table>
<input type="hidden" value="detail" name="operation">
<input type="hidden" id="ids" name="ids" style="width:500px">
</td>
</tr>
<tr>
<td align="right">
<a href="#" onclick="selectAll(true)">Alle auswählen</a> | <a href="#" onclick="selectAll(false)">Auswahl aufheben</a>
</td>
</tr>
</table>
</form>
