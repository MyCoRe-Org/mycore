function $(id){return document.getElementById(id)}
function N(name){return document.getElementsByName(name)[0]}

function initAclEditor(){
	var editor = $('ACL-Editor');
}

function initPermEditor(){
	var editor = $('ACL-Perm-Editor');
	
	if (editor.status != "initialized"){
	
		mappingLines = getChildrenById(editor, "tr", "mapping_line");
	
		for (var i = 0; i < mappingLines.length; i++){
			mappingLines[i].addEventListener("mouseover", markup, false);
			mappingLines[i].addEventListener("mouseout", unmark, false);
		}
		
		editor.status = "initialized";
	}
}

function changeVisibility(node){
	if (node.style.display=="none")
		node.style.display="block";
	else
		node.style.display="none";
}

function setChanged(e){
	changed = "changed$";
	node = e.currentTarget;
	
	if (!node.name.match("changed") && e.which != 0){
		node.name = changed + node.name;
	}
}

function setDeleted(e){
	deleted = "deleted$";
	node = e.currentTarget;
	
	if (!node.name.match("deleted") && e.which != 0){
		node.name = changed + node.name;
	}
}

function markup(e){
	node = e.currentTarget;
	node.style.backgroundColor="red";
}

function unmark(e){
	node = e.currentTarget;
	node.style.backgroundColor="";
}

function getChildrenById(parent, childTagName, id){
	children = parent.getElementsByTagName(childTagName);
	var array = [];
	
	for (var i = 0; i < children.length; i++){
		if (children[i].id == id)
			array = array.concat(children[i]);
	}
	
	return array;
}