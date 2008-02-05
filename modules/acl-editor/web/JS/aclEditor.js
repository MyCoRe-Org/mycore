function $(id){return document.getElementById(id)}
function N(name){return document.getElementsByName(name)}

function initAclEditor(){
	var editor = $('ACL-Editor');
}

function initPermEditor(){
	var editor = $('ACL-Perm-Editor');
	var mappingTable = getChildrenById(editor, "table", "mapping_table");
	
	if (editor.status != "initialized"){
	
		var mappingLines = getChildrenById(editor, "tr", "mapping_line");
	
		for (var i = 0; i < mappingLines.length; i++){
			mappingLines[i].addEventListener("mouseover", markup, false);
			mappingLines[i].addEventListener("mouseout", unmark, false);
			delCheckBox = getChildrenByName(mappingLines[i], "input", "delete_mapping")[0];
			//delCheckBox.selBox = mappingLines[i].getElementsByTagName("select")[0];
			delCheckBox.addEventListener("change", setDeleted, false);
		}
		
		editor.status = "initialized";
	}
	
	var delAll = getChildrenById(editor, "input", "delAll")[0];
	if (delAll != null)
		delAll.addEventListener("click", deleteAll, false);
	
}

function initRuleEditor(){
	var editor = $('ACL-Rule-Editor');
	var ruleTable = getChildrenById(editor, "table", "rule_table");
	
	if (editor.status != "initialized"){
	
		var ruleLines = getChildrenById(editor, "tr", "rule_line");
	
		for (var i = 0; i < ruleLines.length; i++){
			ruleLines[i].addEventListener("mouseover", markup, false);
			ruleLines[i].addEventListener("mouseout", unmark, false);
			delCheckBox = getChildrenByName(ruleLines[i], "input", "delete_rule")[0];
			//delCheckBox.selBox = mappingLines[i].getElementsByTagName("select")[0];
			delCheckBox.addEventListener("change", setDeleted, false);
		}
		
		editor.status = "initialized";
	}
	
	var delAll = getChildrenById(editor, "input", "delAll")[0];
	if (delAll != null)
		delAll.addEventListener("click", deleteAll, false);
	
}

function deleteAll(e){
	var node = e.currentTarget;
	var editor = $('ACL-Perm-Editor');
	var mappingTable = getChildrenById(editor, "table", "mapping_table");
	var checkBoxes = getChildrenByName(mappingTable[0], "input", "delete_mapping");
	
	for (var i = 0; i < checkBoxes.length; i++){
		if (checkBoxes[i].checked == false){
			//checkBoxes[i].checked = true;
			checkBoxes[i].click();
		}
	}
	
	node.value = "nichts loeschen";
	node.removeEventListener("click", deleteAll, false);
	node.addEventListener("click", deleteNothing, false);
}

function deleteNothing(e){
	var node = e.currentTarget;
	var editor = $('ACL-Perm-Editor');
	var mappingTable = getChildrenById(editor, "table", "mapping_table");
	var checkBoxes = getChildrenByName(mappingTable[0], "input", "delete_mapping");
	
	for (var i = 0; i < checkBoxes.length; i++){
		if (checkBoxes[i].checked == true){
			//checkBoxes[i].checked = false;
			checkBoxes[i].click();
		}
	}
	
	node.value = "alles loeschen";
	node.removeEventListener("click", deleteNothing, false);
	node.addEventListener("click", deleteAll, false);
}

function changeVisibility(node){
	if (node.style.display=="none")
		node.style.display="block";
	else
		node.style.display="none";
}

function setChanged(e){
	var changed = "changed$";
	var node = e.currentTarget;
	
	if (!node.name.match("changed") && e.which != 0){
		node.name = changed + node.name;
	}
}

function setDeleted(e){
	var deleted = "deleted$";
	var node = e.currentTarget;
	//var objid = getChildrenById(node.parentNode.parentNode, "td", "OBJID");
	//var acpool = getChildrenById(node.parentNode.parentNode, "td", "ACPOOL");
	
	if (node.type.toLowerCase() == "checkbox"){
		if (node.checked == true){
			var newInput = document.createElement("input");
			newInput.name = deleted + node.value;
			newInput.id = deleted + node.value;
	
			node.appendChild(newInput);
			//objid[0].style.textDecoration = "line-through";
			//acpool[0].style.textDecoration = "line-through";
			node.parentNode.parentNode.className = "deleted";
		} else if (node.checked == false){
			node.removeChild($(deleted + node.value));
			//objid[0].style.textDecoration = "";
			//acpool[0].style.textDecoration = "";
			node.parentNode.parentNode.className = "";
		}
	}
}

function markup(e){
	var node = e.currentTarget;
	node.style.backgroundColor="red";
}

function unmark(e){
	var node = e.currentTarget;
	node.style.backgroundColor="";
}

function getChildrenById(parent, childTagName, id){
	var children = parent.getElementsByTagName(childTagName);
	var array = [];
	
	for (var i = 0; i < children.length; i++){
		if (children[i].id == id)
			array = array.concat(children[i]);
	}
	
	return array;
}

function getChildrenByName(parent, childTagName, name){
	var children = parent.getElementsByTagName(childTagName);
	var array = [];
	
	for (var i = 0; i < children.length; i++){
		if (children[i].name == name)
			array = array.concat(children[i]);
	}
	
	return array;
}

function startsWith(string, pattern){
	var begin = string.substr(0,pattern.length);
	
	alert(string);
	
	if (begin == pattern)
		return true;
	else
		return false;
}