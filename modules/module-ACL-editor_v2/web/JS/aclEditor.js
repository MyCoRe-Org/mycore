function $(id){return document.getElementById(id)}

function initAclEditor(){
	var editor = $('ACL-Editor');
	
	var newPerm = $('createNewPerm');
	newPerm.addEventListener("mosedown", changeVisibility($("createNewPermForm")), false);
}

function changeVisibility(node){
	if (node.style.visibility=="hidden")
		node.style.visibility="visible";
	else
		node.style.visibility="hidden";
}