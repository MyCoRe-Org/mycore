<%@ page import="org.mycore.user.MCRUserMgr,
	java.util.Date,
	java.util.ArrayList,
	org.jdom.Element"%>
<%@ page import="org.mycore.frontend.servlets.MCRServlet" %>    
<%
	String WebApplicationBaseURL = MCRServlet.getBaseURL();
	ArrayList userIds = MCRUserMgr.instance().getAllUserIDs();
	ArrayList groupIds = MCRUserMgr.instance().getAllGroupIDs();

	/*user selection dialog*/
	String usersel="\"<table border=\\\"0\\\"><tr><td valign=\\\"top\\\">User:</td><td><select name=\\\"users\\\" id=\\\"users\\\" 	size=\\\"4\\\" ONCLICK=setValue(this)>";
		for (int i=0; i< userIds.size(); i++){
			usersel +="<option value=\\\"" + (String) userIds.get(i) + "\\\">" + (String) userIds.get(i) + "</option>";
		}
	usersel +="</tr></table>\"";

	/*group selection dialog*/
	String groupsel="\"<table border=\\\"0\\\"><tr><td valign=\\\"top\\\">Groups:</td><td><select name=\\\"users\\\" id=\\\"users\\\" 	size=\\\"4\\\" ONCLICK=setValue(this)>";
		for (int i=0; i< groupIds.size(); i++){
			groupsel +="<option value=\\\"" + (String) groupIds.get(i) + "\\\">" + (String) groupIds.get(i) + "</option>";
		}
	groupsel +="</tr></table>\"";

	/*date selection dialog*/
	String datesel="\"<table border=\\\"0\\\"><tr><td rowspan=\\\"2\\\" valign=\\\"top\\\">Operator:</td><td rowspan=\\\"2\\\"><select name=\\\"ruleop\\\" id=\\\"ruleop\\\" size=\\\"4\\\" ONCLICK=setValue(this) ><option value=\\\"<= \\\">&lt=</option><option value=\\\">= \\\">&gt=</option><option value=\\\"< \\\">&lt</option><option value=\\\"> \\\">&gt</option></select></td><td valign=\\\"top\\\">Datum:</td><td><input type=\\\"text\\\" name=\\\"date\\\" id=\\\"datevalue\\\" size=\\\"10\\\" maxlength=\\\"10\\\"><button name=\\\"dateselect\\\" type=\\\"button\\\" ONCLICK=setValue(this)>OK</button></td></tr><tr><td align=\\\"right\\\" colspan=\\\"2\\\"><small>Format: dd.mm.yyyy</small></td></tr></table>\"";

	/*ip selection dialog*/
	String ipsel="\"<table border=\\\"0\\\"><tr><td valign=\\\"top\\\">IP:</td><td><input type=\\\"text\\\" name=\\\"ipvalue\\\" id=\\\"ipvalue\\\" size=\\\"15\\\"><button name=\\\"ipselect\\\" type=\\\"button\\\" ONCLICK=setValue(this)>OK</button></td></tr><tr><td align=\\\"right\\\" colspan=\\\"2\\\"><small>Format: xxx.xxx.xxx.xxx</small></td></tr></table>\"";

%>

<html>
<head>
  <META http-equiv="content-type" content="text/html; charset=UTF-8">
  <title>MyCoRe Administration Interface - Regeleditor</title>
  
	<link rel="stylesheet" href="<%= WebApplicationBaseURL %>admin/css/rules_editor.css">
	<script>
		var i = 0;
		var value = "";
		var selected = false;
		var checkselect = 0;
		var spancount = 1;
		var spanid = 0;

		var userSelection = <%=usersel%>;
		var groupSelection = <%=groupsel%>;
		var dateSelection = <%=datesel%>;
		var ipSelection = <%=ipsel%>;

		function ReplaceTags(xStr){
			xStr = xStr.replace(/[\r\n\t]/gi,"");
			xStr = xStr.replace(/<\/?[^>]+>/gi,"");
			xStr = xStr.replace(/&lt;/gi,"<");
			xStr = xStr.replace(/&gt;/gi,">");
			value = xStr;
			return xStr;
	    }

		function pick(obj) {
			if (selected==false){
				i++;
				if (i==1){
					obj.style.border = "solid red 1px";
					obj.style.cursor ="pointer";
					obj.style.cursor ="hand";
					obj.style.margin="0px";
					spanid=obj.id;
				}
			}	
		}

		function unpick(obj) {
			if (selected==false){
				obj.style.border = "solid white 0px";
				i=0;
				obj.style.margin="1px";
				spanid = 0;
			}
		}

		function selectpart(obj){
			if (checkselect == 0)
				checkselect = obj.id;
		
			if (checkselect==obj.id){
				if (selected==true)
					selected=false;
				else
					selected=true;
				document.getElementById("rule_part").innerHTML = ReplaceTags(document.getElementById(obj.id).innerHTML);
			}
			if (obj.id==1)
				checkselect=0;
		}

		function setValue(obj){
			if (obj.name=="ruleop"){
				
				if (document.getElementById("rule_part").innerHTML=="date "){
					document.getElementById("rule_part").innerHTML += document.getElementById(obj.name).value;
				}
			}else if(obj.name=="rulearg" ){
				if (document.getElementById("rulearg").value=="user "){
					document.getElementById("argspan").innerHTML = userSelection;
				}else if (document.getElementById("rulearg").value=="group "){
					document.getElementById("argspan").innerHTML = groupSelection;
				}else if(document.getElementById("rulearg").value=="date "){
					document.getElementById("argspan").innerHTML = dateSelection;
				}else if(document.getElementById("rulearg").value=="ip "){
					document.getElementById("argspan").innerHTML = ipSelection;
				}
				document.getElementById("rule_part").innerHTML = document.getElementById(obj.name).value;
				document.getElementById("ruleop").disabled = false;
			}else if(obj.name=="users" || obj.name=="groups"){
				document.getElementById("rule_part").innerHTML += document.getElementById(obj.name).value;
				document.getElementById("argspan").innerHTML="&nbsp;";
			}else if(obj.name=="dateselect"){
				if (document.getElementById("datevalue").value.length==10){
					document.getElementById("rule_part").innerHTML += document.getElementById("datevalue").value;
					document.getElementById("argspan").innerHTML = "&nbsp;";
				}
			}else if(obj.name=="ipselect"){
				if (document.getElementById("ipvalue").value!=""){
					document.getElementById("rule_part").innerHTML += document.getElementById("ipvalue").value;
					document.getElementById("argspan").innerHTML = "&nbsp;";
				}

			}else if(obj.name=="ruletype"){
				rulestr = document.getElementById(obj.name).value;
				if (rulestr.indexOf("[op1]")>0 && document.getElementById("value1").value!=""){
					position = rulestr.indexOf("[op1]");
					rulestr = rulestr.substring(0,position) + document.getElementById("value1").value + rulestr.substring(position+5);
				}
				
				if (rulestr.indexOf("[op2]")>0 && document.getElementById("value2").value!=""){
					position = rulestr.indexOf("[op2]");
					rulestr = rulestr.substring(0,position) + document.getElementById("value2").value + rulestr.substring(position+5);
				}

				document.getElementById("rule_part").innerHTML = rulestr;
			}
		}

		function setPart(){
			var value = document.getElementById("rule_part").innerHTML;
			if (value.substring(0,1) != "("){
				value= "( " + value + " )";
			}
			value = value.replace("&lt;","<");
			value = value.replace("&gt;",">");
			option = new Option(value, value, false, false);
			document.getElementById("parts").options[document.getElementById("parts").length] = option;
			document.getElementById("rule_part").innerHTML ="";
			document.getElementById("argspan").innerHTML ="&nbsp;";
		}

		function changePart(){
			if (selected==true){
				var value= document.getElementById("rule_part").innerHTML;
				if (value !=""){

  					if (value.substring(0,1) != "("){
						value= "( " + value + " )";
					}
					document.getElementById(spanid).innerHTML = replaceBracket(value);
				}
			}
		}

		function clearParts(){
			document.getElementById("parts").length=null;
		}
		
		function setOperator(){

			if (document.getElementById("op").checked){
				document.getElementById("value1").value= document.getElementById("parts").value;
			}else{
				document.getElementById("value2").value= document.getElementById("parts").value;
			}
		}

		function replaceBracket(value){
			value = value.replace(/[)]/gi,")</span>");
			value = value.replace(/[(]/gi,"<span class=\"rule\" id=\"!\" ONMOUSEOVER=\"pick(this)\" ONMOUSEOUT=\"unpick(this)\" ONCLICK=\"selectpart(this)\">(");
			position = 0;
			while( value.indexOf("!")>0){
				position = value.indexOf("!");
				value = value.substring(0,position) + spancount + value.substring(position+1);
				spancount++;
			}
			return value;
		}

		function setMainRule(){
			var value = "( true )";
			if (opener.getEditRule()!=""){
				value=opener.getEditRule();
			}
			if(value.indexOf("(")!=-1){
				document.getElementById("MainRule").innerHTML = replaceBracket(value);
			}
		}

		function setReturn(){
			opener.getEditorValue( ReplaceTags(document.getElementById("MainRule").innerHTML));
			self.close();
		}

		function getSelectedValue(){
			document.getElementById("rule_part").innerHTML = document.getElementById("parts").value;
		}
	
	
	
	</script>

</head>
<body onload="setMainRule()">
<h1>Regeleditor</h1>

<p>Regel: <span id="MainRule">...</span></p>


<form name="form_input" method="post" >
	
	<table border="0" width="100%">
		<tr>
			<td>
				<br>Regel bearbeiten:
			</td>
			<td colspan="2" width="300px">&nbsp;</td>
			<td align="right">
				<button type="button" ONCLICK="changePart()" title="Regel übernehmen">Übernehmen</button>
			</td>
		</tr>
		<tr>
			<td colspan="4">
				<table border=0 width="100%" cellpadding="0" cellspacing="0">
					<tr>
						<td>
							<div class="field" id="rule_part"></div>
						</td>
						<td align="right">
							<button  align="right" type="button" ONCLICK="setPart()" title="Regel in Teileliste übertragen">in Teileliste</button>
						</td>
					</tr>
				</table>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<table>
					<tr>
						<td valign="top">
							Argument:
						</td>
						<td valign="top">
							<select name="rulearg" id="rulearg" size="4" ONCLICK="setValue(this)">
								<option value="user ">user __</option>
								<option value="group ">group __</option>
								<option value="date ">date __</option>
								<option value="ip ">ip __</option>
							</select>
						</td>
					</tr>
				</table>
			</td>
			<td valign="top" colspan="2">
				<span id="argspan">&nbsp;
					<!-- dummy span - do not delete-->
				</span>
			</td>
			<td rowspan="2" valign="top">
			Teileliste:<br>
				<select name="parts" id="parts" size="10" style="width:100%" onclick=setOperator() ondoubleclick="" >
				</select>
				<br>
				<button ONCLICK=clearParts() type="button" title="Löscht alle Einträge aus der Tileliste">alle l&ouml;schen</button>
				<button ONCLICK=getSelectedValue() type="button" title="Kopiert selektierten Eintrag in den Bearbeitungsbereich">Selektion bearbeiten</button>
			</td>
		</tr>
		<tr>
			<td valign="top">
				<table>
					<tr>
						<td valign="top">
							Regelart:
						</td>
						<td>
							<select name="ruletype" id="ruletype" size="4" ONCLICK=setValue(this)>
								<option value="( [op1] AND [op2] )">( __ AND __ )</option>
								<option value="( [op1] OR [op2] )">( __ OR __ )</option>
								<option value="NOT [op1] ">NOT ( __ )</option>
							</select>
						</td>
					</tr>
				</table>
			</td>
			<td valign="top" colspan="2">
				<INPUT type="radio" name="op" value="op1" id="op" checked> Wert 1 [op1]: <input type="text" name="value1" id="value1"></input>
				<br>
				<INPUT type="radio" name="op" value="op2" id="op"> Wert 2 [op2]: <input type="text" name="value2" id="value2"></input>
			</td>
		</tr>
		<tr>
			<td colspan="4" align="right">
				&nbsp;</br>&nbsp;</br>
				<button type="button" ONCLICK="setReturn()"> OK </button>&nbsp;&nbsp;&nbsp;
				<button type="button" ONCLICK="window.close()">  Abbrechen  </button>
			</td>
		</tr>
	</table>
</form>

</body>
</html>
