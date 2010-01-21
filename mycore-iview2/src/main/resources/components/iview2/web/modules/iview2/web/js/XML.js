/*
@description checks if the browser supports Namespaces within getElementsByTagName, if not the namespace will be parsed out
@param name the Namespace name which needs to be checked
@return the name, depending on the browser cleaned from Namespace information
 */
function namespaceCheck(name) {
	if (isBrowser(["Opera", "Firefox/2", "Safari", "Mozilla/5"]) && name.indexOf(":") != -1) {
		return name.substring(name.indexOf(":")+1,name.length);
	}
	return name;
}

//TODO renaming would be good, as the name of the function doesn't reflect the real intention of the function
/*
@description Gains the requested information from a given XML File.
@param xmlfile  XML file from which the data will be extruded.
@param nodeName the Node of the XML File which shall be gained, can be a group of nodes.
@param getNode  return the X Nodes values
@param absolute depending on this Value the getNode will be interpreted as absolute or relative value to the current curpage
@return  Associated(By Nodename) array with connected values
*/
function nodeProps(xmlfile, nodeName, getNode, absolute) {
	nodeName = namespaceCheck(nodeName); 
	var child_nodes = xmlfile.getElementsByTagName(nodeName);
	var Node = (absolute == true)? parseInt(getNode) : (pagenumber-1)+parseInt(getNode);
	var values = new Array();
	var child = null;
	var nodes = null;
	var count = 0;
	try {
		child = document.importNode( child_nodes.item(Node), true);
		nodes = child.childNodes;
	} catch (e) {
		child = child_nodes;
		nodes = child.item(Node).childNodes;
	}
	for (var i = 0; i < nodes.length; i++) {
		if (nodes.item(i).tagName) {
			values[count++] = nodeAttributes(nodes.item(i));
			//values[nodes.item(i).tagName] = nodes.item(i).childNodes.item(0).nodeValue;
		}
	}
	try {//ANTI Memory Leak
		return values;
	} finally {
		values = null;
	}
}

/*
@description collects into an array all attributes of a supplied Node, where the name of the object attribute is the attributes name,
 cleaned from any namespace things like regex: .*:
@param node the node to collect all properties from
@return an array with all Informations the node contained as attributes
 */
function nodeAttributes(node) {
	var attributes = new Object();
	for (var i = 0; i < node.attributes.length; i++) {
		//Remove the Namespace, as this is hindering access laterly
		attributes[node.attributes.item(i).nodeName.replace(/^.*:/,"")] = node.attributes.item(i).value;
	}
	return attributes;
}

/*
@description Gains the requested information from a given XML File.
@param xmlfile  XML file from which the data will be extruded.
@param nodeName the Node of the XML File which shall be gained, can be a group of nodes.
@param getNode  return the X Nodes values
@param absolute depending on this Value the getNode will be interpreted as absolute or relative value to the current curpage
@return  List of all Nodes with the given NodeName
*/
function getNodes(xmlfile, nodeName/*, getNode, absolute*/) {
	var nodes = xmlfile.getElementsByTagName(namespaceCheck(nodeName));
	var nodeList = new Array();
	for ( i = 0; i < nodes.length; i++) {
		try {
			nodeList[i] = document.importNode(nodes.item(i), true);
		} catch (e) {
			nodeList[i] = nodes.item(i);
		}
	}
	try {
		return nodeList;
	} finally {
		nodeList = null;
	}
}

function getPageCount(res) {
	return res.getElementsByTagName(namespaceCheck("mets:file")).length;
}

function loadVars(filename) {
	var file = loadXML(filename);
	var nodeList = getNodes(file, "var");
	for (var i = 0; i < nodeList.length; i++) {
		var name = nodeList[i].attributes.getNamedItem("name").value;
		window.eval(nodeList[i].attributes.getNamedItem("name").value + " = " + checkType(nodeList[i])+"");
	}
}

/*
 @description searches for the first Occurence of the given Tagname within the Element List and returns it
 @param nodeList Array which contains the Elements which shall be searched
 @param tagName String which contains the TagName, we're looking for
 @return Object first Element with the given tagname, if there's one else it returns null
 */
function getFirstNodeByTagName(nodeList, tagName) {
	for (i = 0; i < nodeList.length; i++) {
		if (nodeList[i].tagName == tagName) {
			return nodeList[i];
		}
	}
	return null;
}

function checkType(node) {
	if (node.firstChild) {
		switch (node.attributes.getNamedItem("type").value) {
			case "array": return "[]";
			case "bool": return (node.firstChild.nodeValue =="true")? true:false;
			case "float": return toFloat(node.firstChild.nodeValue);
			case "int": return toInt(node.firstChild.nodeValue);
			case "parse": return node.firstChild.nodeValue;
			case "string": return "'"+ node.firstChild.nodeValue + "'";
			default: return "'" + node.firstChild.nodeValue + "'";
		}
	}
}