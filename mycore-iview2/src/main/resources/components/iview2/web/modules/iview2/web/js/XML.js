/*
@description Generates from a URL the XML Object loads it's contents and returns it afterwards
@param file string which holds the URL to the XML File which shall be loaded
@return the XML Object
*/
function loadXML(file) {
	var xmlDoc;
	if (typeof(arguments[1]) !="undefined") {
		file += "?XSL.Style=" + arguments[1];
	}
	//file=file+"?XSL.Style=" + ((typeof(arguments[1]) !="undefined")? arguments[1]:"xml");
	// code for IE 
	if (window.ActiveXObject) {
		xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
		xmlDoc.async = false;
		xmlDoc.load(file);
	}//code for Mozilla, Firefox, Opera, Safari, etc.
	else if (window.XMLHttpRequest) {
		xmlDoc = new XMLHttpRequest();
		xmlDoc.open("GET",file, false);
		xmlDoc.send(null);
	}
	try {//ANTI Memory Leak
		if (window.ActiveXObject) {
			return xmlDoc;
		} else {
			return xmlDoc.responseXML;
		}
	} finally {
		xmlDoc = null;
	}
}
/*Gains the requested information from a given XML File.
*@param xmlfile  XML file from which the data will be extruded.
*@param nodeName the Node of the XML File which shall be gained, can be a group of nodes.
*@param getNode  return the X Nodes values
*@param absolute depending on this Value the getNode will be interpreted as absolute or relative value to the current curpage
*@return  Associated(By Nodename) array with connected values
*/
function nodeProps(xmlfile, nodeName, getNode, absolute) {
	var child_nodes = xmlfile.getElementsByTagName(nodeName);
	var Node = (absolute == true)? parseInt(getNode) : (pagenumber-1)+parseInt(getNode);
	var values = new Array();
	var child = null;
	var nodes = null;
	try {
		child = document.importNode( child_nodes.item(Node), true);
		nodes = child.childNodes;
	} catch (e) {
		child = child_nodes;
		nodes = child.item(Node).childNodes;
	}
	for (var i = 0; i < nodes.length; i++) {
		if (nodes.item(i).tagName) {
			values[nodes.item(i).tagName] = nodes.item(i).childNodes.item(0).nodeValue;
		}
	}
	try {//ANTI Memory Leak
		return values;
	} finally {
		values = null;
	}
}

/*Gains the requested information from a given XML File.
*@param xmlfile  XML file from which the data will be extruded.
*@param nodeName the Node of the XML File which shall be gained, can be a group of nodes.
*@param getNode  return the X Nodes values
*@param absolute depending on this Value the getNode will be interpreted as absolute or relative value to the current curpage
*@return  List of all Nodes with the given NodeName
*/
function getNodes(xmlfile, nodeName/*, getNode, absolute*/) {
	if (isBrowser(["Opera", "Firefox/2", "Safari"]) && nodeName.indexOf(":") != -1) {
		nodeName = nodeName.substring(nodeName.indexOf(":")+1);
	}

	var nodes = xmlfile.getElementsByTagName(nodeName);
	var nodeList = new Array();
	for ( i = 0; i < nodes.length; i++) {//TODO gegenstück für IE basteln
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
	var nodeName = "mets:file";
	if (isBrowser(["Opera", "Firefox/2", "Safari", "Mozilla/5"]) && nodeName.indexOf(":") != -1) {
		nodeName = "file";
	}
	return res.getElementsByTagName(nodeName).length;
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