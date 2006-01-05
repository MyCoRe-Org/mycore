/* rulefunctions */

	function questionDel(){
		if (confirm("Soll dieser Eintrag wirklich geloescht werden?")){
			return true;
		}else{
			return false;
		}
	}

	var editor;
	function openRuleEditor (obj, id) {
		//editor = window.open("?path=rules_editor&id=<%=(String)request.getParameter("id")%>", "Editor", "width=800,height=440,scrollbars");
		editor = window.open("?path=rules_editor&id="+id, "Editor", "width=800,height=440,scrollbars");
		editor.focus();
	}

	function getEditorValue(value){
		if (value != ""){
			document.getElementById("rule").value = value;
			document.getElementById("rule_show").innerHTML = value;
		}
		editor = null;
	}

	function getEditRule(){
		if (document.getElementById("rule").value==""){
			return "";
		}else{
			return document.getElementById("rule").value;
		}
	}

	function selectall(value){
		var sel = document.getElementById(value);
		for(var i=0; i< sel.length; i++){
			sel[i].selected = true;
		}
	}
	
	function deselectall(value){
		var sel = document.getElementById(value);
		for(var i=0; i< sel.length; i++){
			sel[i].selected = false;
		}
	}



	function validatePresent(obj){
		if(obj.value=="" || obj.value.substring(0,1)=="("){
			obj.style.backgroundColor = '#FFE7E7';
			return false;
		}else{
			obj.style.backgroundColor = '#FFFFFF';
			return true;
		}
	}
