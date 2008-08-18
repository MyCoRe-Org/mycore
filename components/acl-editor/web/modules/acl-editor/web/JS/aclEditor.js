function $(id){return document.getElementById(id)}
function N(name){return document.getElementsByName(name)}

function getElementsByName_iefix(tag, name) {
     var elem = document.getElementsByTagName(tag);
     
     var arr = new Array();
     for(i = 0,iarr = 0; i < elem.length; i++) {
          att = elem[i].getAttribute("name");
          if(att == name) {
               arr[iarr] = elem[i];
               iarr++;
          }
     }
     return arr;
}

function isIE() {
	var browser = navigator.userAgent;
	if (browser.indexOf("MSIE") != -1) {
		return true;
	} else {
		return false;
	}
}

var IE = isIE();

function initAclEditor(){
	var editor = $('ACL-Editor');
}

function initPermEditor(){
	var editor = $('ACL-Perm-Editor');
	var mappingTable = getChildrenById(editor, "table", "mapping_table");
	
	if (editor.status != "initialized"){
	
		var mappingLines = getChildrenById(editor, "tr", "mapping_line");
	
		for (var i = 0; i < mappingLines.length; i++){
			delCheckBox = getChildrenByName(mappingLines[i], "input", "delete_mapping")[0];
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
	
		var ruleLines = document.getElementsByName("rule_line"); //getChildrenById(editor, "tr", "rule_line");
	
		for (var i = 0; i < ruleLines.length; i++){
			delCheckBox = getChildrenByName(ruleLines[i], "input", "delete_rule")[0];
			delCheckBox.addEventListener("change", setDeleted, false);
		}
		
		editor.status = "initialized";
	}
	
	var delAll = document.getElementById("delAll");
	if (delAll != null)
		delAll.addEventListener("click", deleteAllR, false);
	
}

function deleteAllFromDB(url,msg){
	var chk = window.confirm(msg);
	
	//alert(redir)
	
	if (chk == true) {
		self.location.href=url;
	}
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

function deleteAllR(e){
	var node = e.currentTarget;
	var checkBoxes = document.getElementsByName("delete_rule");

	for (var i = 0; i < checkBoxes.length; i++){
		if ((checkBoxes[i].checked == false) && (node.checked == true)){
			checkBoxes[i].click();
		}
	}
	
	node.removeEventListener("click", deleteAllR, false);
	node.addEventListener("click", deleteNothingR, false);
}

function deleteNothingR(e){
	var node = e.currentTarget;
	var checkBoxes = document.getElementsByName("delete_rule");
	
	for (var i = 0; i < checkBoxes.length; i++){
		if ((checkBoxes[i].checked == true) && (node.checked == false)){
			checkBoxes[i].click();
		}
	}
	
	node.removeEventListener("click", deleteNothingR, false);
	node.addEventListener("click", deleteAllR, false);
}

function changeVisibility(nodeName,button){
	var node = document.getElementById(nodeName);
	var status = null;
	
	if (IE){
		status = node.currentStyle.display;
		
		if (status == "none"){
			node.style.display = "block";
			button.firstChild.nodeValue="-";
		} else{
			node.style.display = "none";
			button.firstChild.nodeValue="+";
		}
	}
	else {
		node.style.display="table-row";
		status = document.defaultView.getComputedStyle(node, null).visibility;
		
		if (status == "collapse"){
			node.style.visibility = "visible";
			button.firstChild.nodeValue="-";
		} else{
			node.style.visibility="collapse";
			button.firstChild.nodeValue="+";
		}
	}
}

function initOpenAll(button,expandLabel, collapsLabel){
	button.expandLabel = expandLabel;
	button.collapsLabel = collapsLabel;
	
	if (button.init != true){
		try {
			button.addEventListener("click", openAll, false);
		} catch(e) {
			button.onclick=openAll;
			button.onmouseout=unsetStyleIE;
		}
		button.init = true;
	}
	//button.onmouseover = null;
	
	if (IE){
		button.inactColor=button.currentStyle.backgroundColor;
		button.inactCursor=button.currentStyle.cursor;
		button.style.backgroundColor="#14516E";
		button.style.cursor="pointer";
		
	}
}

// for IE only, it has no :hover
function unsetStyleIE(e){
	button=this;
	button.style.backgroundColor=button.inactColor;
	button.currentStyle.cursor=button.inactCursor;
}

function openAll(e){
	var expButtons = document.getElementsByName("visButton");
	var ruleLines = document.getElementsByName("rule_line");
	
	if (IE){
		expButtons = getElementsByName_iefix("div","visButton");
		ruleLines = getElementsByName_iefix("tr","rule_line");
	}
	
	// there is no currentTarget in IE
	try{
		button = e.currentTarget;
	} catch(error){
		button=this;
	}
	
	var status = null;
	
	for (var i = 0; i < ruleLines.length; i++){
		currentNodeId = ruleLines[i].id.replace("Rule","RuleField");
		currentNode = $(currentNodeId);
		
		try{
			status = currentNode.currentStyle.display;
		} catch(error) {
			status = document.defaultView.getComputedStyle(currentNode, null).visibility;
		}
		
		if (status == "collapse" || status == "none") {
			changeVisibility(currentNodeId,expButtons[i]);
		}
	}
	
	button.firstChild.replaceData(0,button.firstChild.nodeValue.length,button.collapsLabel);
	
	
	try {
		button.removeEventListener("click", openAll, false);
		button.addEventListener("click",collapsAll,false);
	} catch(error){
		button.onclick=collapsAll;
	}
}

function collapsAll(e){
	var expButtons = document.getElementsByName("visButton");
	var ruleLines = document.getElementsByName("rule_line");
	
	if (IE){
		expButtons = getElementsByName_iefix("div","visButton");
		ruleLines = getElementsByName_iefix("tr","rule_line");
	}
	
	try{
		button = e.currentTarget;
	} catch(error){
		button=this;
	}
	
	var status = null;
	
	for (var i = 0; i < ruleLines.length; i++){
		currentNodeId = ruleLines[i].id.replace("Rule","RuleField");
		currentNode = $(currentNodeId);
		
		try{
			status = currentNode.currentStyle.display;
		} catch(error) {
			status = document.defaultView.getComputedStyle(currentNode, null).visibility;
		}
		
		if (status == "visible" || status == "block") {
			changeVisibility(currentNodeId,expButtons[i]);
		}
	}
	
	button.firstChild.replaceData(0,button.firstChild.nodeValue.length,button.expandLabel);
	
	try {
		button.removeEventListener("click", collapsAll, false);
		button.addEventListener("click",openAll,false);
	} catch(e){
		button.onclick=openAll;
	}
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
	//node.style.backgroundColor="red";
	node.style.backgroundColor="#14516E";
	node.style.color="#EFF4F6";
}

function unmark(e){
	var node = e.currentTarget;
	node.style.backgroundColor="";
	node.style.color="";
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