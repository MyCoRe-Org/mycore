/*****************************************************************************
 *
 * Sarissa XML library version 0.9
 * Copyright (c) 2003 Manos Batsis, mailto: mbatsis@netsmart.gr
 * This software is distributed under the Kupu License. See
 * LICENSE.txt for license text. See the Sarissa homepage at
 * http://sarissa.sourceforge.net for more information.
 *
 *****************************************************************************/

// $Id: sarissa.js 6772 2004-09-28 11:55:11Z guido $

// some basic browser detection TODO: change this 
var _SARISSA_IS_IE = (navigator.userAgent.toLowerCase().indexOf("msie") > -1)?true:false;
var _SARISSA_IS_MOZ = (document.implementation && document.implementation.createDocument)?true:false;
var _sarissa_iNsCounter = 0;
var _SARISSA_IEPREFIX4XSLPARAM = "";
if (_SARISSA_IS_MOZ)
{
	/**
	* <p>Factory method to obtain a new DOM Document object.</p>
	* @argument sUri the namespace of the root node (if any)
	* @argument sUri the local name of the root node (if any)
	* @returns a new DOM Document
	*/
	Sarissa.getDomDocument = function(sUri, sName)
	{
		var oDoc = document.implementation.createDocument(sUri, sName, null);
		oDoc.addEventListener("load", _sarissa__XMLDocument_onload, false);
		return oDoc;
	};
	/**
	* <p>Factory method to obtain a new XMLHTTP Request object</p>
	* @returns a new XMLHTTP Request object
	*/
	Sarissa.getXmlHttpRequest = function()
	{
		return new XMLHttpRequest();
	};
	/**
	* <p>Attached by an event handler to the load event. Internal use.</p>
	* @private
	*/
	function _sarissa__XMLDocument_onload()
	{
		_sarissa_loadHandler(this);
	};
	/** 
	* <p>Ensures the document was loaded correctly, otherwise sets the parseError to -1
	* to indicate something went wrong. Internal use.</p>
	* @private
	*/
	function _sarissa_loadHandler(oDoc)
	{
		if (!oDoc.documentElement || oDoc.documentElement.tagName == "parsererror")
			oDoc.parseError = -1;
		_sarissa_setReadyState(oDoc, 4);
	};
	/**
	* <p>Sets the readyState property of the given DOM Document object. Internal use.</p>
	* @private
	* @argument oDoc the DOM Document object to fire the readystatechange event
	* @argument iReadyState the number to change the readystate property to 
	*/
	function _sarissa_setReadyState(oDoc, iReadyState) 
	{
		oDoc.readyState = iReadyState;
		if (oDoc.onreadystatechange != null && typeof oDoc.onreadystatechange == "function")
			oDoc.onreadystatechange();
	};
	/**
	* Deletes all child Nodes of the Document. Internal use.
	* @private
	*/
	XMLDocument.prototype._sarissa_clearDOM = function()
	{
		while(this.hasChildNodes())
			this.removeChild(this.firstChild);
	}
	/** 
	* Replaces the childNodes of the Document object with the childNodes of 
	* the object given as the parameter
	* @private
	* @argument oDoc the Document to copy the childNodes from
	*/
	XMLDocument.prototype._sarissa_copyDOM = function(oDoc)
	{
		this._sarissa_clearDOM();
		// importNode is not yet needed in Moz due to a bug but it will be fixed at some point so...
		if(oDoc.nodeType == Node.DOCUMENT_NODE || oDoc.nodeType == Node.DOCUMENT_FRAGMENT_NODE)
		{
			var oNodes = oDoc.childNodes;
			for(i=0;i<oNodes.length;i++)
				this.appendChild(this.importNode(oNodes[i], true));
		}
		else if(oDoc.nodeType == Node.ELEMENT_NODE)
			this.appendChild(this.importNode(oDoc, true));
	};
	// used to normalise text nodes (for IE's innerText emulation)
	// i'd appreciate any ideas, regexp is not my strong point
	var _SARISSA_WSMULT = new RegExp("^\\s*|\\s*$", "g");
	var _SARISSA_WSENDS = new RegExp("\\s\\s+", "g");
	/**
	* <p>Used to "normalize" text (trim white space mostly). Internal use.</p>
	* @private
	*/
	function _sarissa_normalizeText(sIn)
	{
		return sIn.replace(_SARISSA_WSENDS, " ").replace(_SARISSA_WSMULT, " ");
	}
	/**
	* <p>Parses the String given as parameter to build the document content
	* for the object, exactly like IE's loadXML().</p>
	* @argument strXML The XML String to load as the Document's childNodes
	* @returns the old Document structure serialized as an XML String
	*/
	XMLDocument.prototype.loadXML = function(strXML) 
	{
		_sarissa_setReadyState(this, 1);
		var sOldXML = this.xml;
		var oDoc = (new DOMParser()).parseFromString(strXML, "text/xml");
		_sarissa_setReadyState(this, 2);
		this._sarissa_copyDOM(oDoc);
		_sarissa_setReadyState(this, 3);
		_sarissa_loadHandler(this);
		return sOldXML;
	};
	/**
	* <p>Emulates IE's xml property, giving read-only access to the XML tree in it's serialized form (in other words, an XML string).</p>
	* @uses Mozilla's XMLSerializer class.
	*/
	XMLDocument.prototype.__defineGetter__("xml", function ()
	{
		return (new XMLSerializer()).serializeToString(this);
	});
	/**
	* <p>Emulates IE's xml property, giving read-only access to the XML tree in it's serialized form (in other words, an XML string).</p>
	* @uses Mozilla's XMLSerializer class.
	*/
	Node.prototype.__defineGetter__("xml", function ()
	{
		return (new XMLSerializer()).serializeToString(this);
	});
	/**
	* <p>Ensures and informs the xml property is read only.</p>
	* @throws an &quot;Invalid assignment on read-only property&quot; error.
	*/
	XMLDocument.prototype.__defineSetter__("xml", function ()
	{
		throw "Invalid assignment on read-only property 'xml'. Hint: Use the 'loadXML(String xml)' method instead. (original exception: "+e+")";
	});
	/**
	* <p>Emulates IE's innerText (read/write). Note that this removes all childNodes of
	* an HTML Element and just replaces it with a textNode</p>
	*/
	HTMLElement.prototype.innerText;
	HTMLElement.prototype.__defineSetter__("innerText", function (sText)
	{
		var s = "" + sText;
		this.innerHTML = s.replace(/\&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
	});
	HTMLElement.prototype.__defineGetter__("innerText", function ()
	{
		return _sarissa_normalizeText(this.innerHTML.replace(/<[^>]+>/g,""));
	});
	/** 
	* <p>Emulate IE's onreadystatechange attribute</p>
	*/ 
	Document.prototype.onreadystatechange = null;
	/** 
	* <p>Emulate IE's parseError attribute</p>
	*/
	Document.prototype.parseError = 0;
	/**
	* <p>Emulates IE's readyState property, which always gives an integer from 0 to 4:</p>
	* <ul><li>1 == LOADING, </li>
	* <li>2 == LOADED, </li>
	* <li>3 == INTERACTIVE,</li> 
	* <li>4 == COMPLETED</li></ul>
	*/
	XMLDocument.prototype.readyState = 0;
	// NOTE: setting async to false will only work with documents 
	// called over HTTP (meaning a server), not the local file system,
	// unless you are using Moz 1.4+.
	// BTW the try>catch block is for 1.4; I haven't found a way to check if the property is implemented without 
	// causing an error and I dont want to use user agent stuff for that...
    var _SARISSA_SYNC_NON_IMPLEMENTED = false; 
	try{
        	/**
        	* <p>Emulates IE's async property for Moz versions prior to 1.4. It controls whether loading of
        	* remote XML files works synchronously or asynchronously.</p>
        	*/
			XMLDocument.prototype.async = true;
        	_SARISSA_SYNC_NON_IMPLEMENTED = true;
	}catch(e){/*trap*/}
	/** 
	* <p>Keeps a handle to the original load() method. Internal use and only if Mozilla version is lower than 1.4. </p>
	* @private
	*/ 
	XMLDocument.prototype._sarissa_load = XMLDocument.prototype.load;
	/** 
	* <p>Overrides the original load method to provide synchronous loading
	* for Mozilla versions prior to 1.4, using an XMLHttpRequest object (if async is set to false)</p>
	* @returns the DOM Object as it was before the load() call (may be empty)
	*/
	XMLDocument.prototype.load = function(sURI)
	{
		var oDoc = document.implementation.createDocument("", "", null);
		oDoc._sarissa_copyDOM(this);
		this.parseError = 0;
		_sarissa_setReadyState(this, 1);
		try
		{
			if(this.async == false && _SARISSA_SYNC_NON_IMPLEMENTED)
			{
				var tmp = new XMLHttpRequest();
				tmp.open("GET", sURI, false);
				//tmp.overrideMimeType("text/xml");
				tmp.send(null);
				_sarissa_setReadyState(this, 2);
				this._sarissa_copyDOM(tmp.responseXML);
				_sarissa_setReadyState(this, 3);
			}
			else
				this._sarissa_load(sURI);
		}
		catch (objException)
		{
			this.parseError = -1;
		}
		finally
		{
			_sarissa_loadHandler(this);
		}
		return oDoc;
	}; 
	/** 
	* <p>Extends the Element class to emulate IE's transformNodeToObject. 
	* <b>Note</b>: The transformation result <i>must</i> be well formed, 
	* otherwise an error will be thrown.</p>
	* @uses Mozilla's XSLTProcessor
	* @argument xslDoc The stylesheet to use (a DOM Document instance)
	* @argument oResult The Document to store the transformation result
	*/
	Element.prototype.transformNodeToObject = function(xslDoc, oResult)
	{
		var oDoc = document.implementation.createDocument("", "", null);
		oDoc._sarissa_copyDOM(this);
		oDoc.transformNodeToObject(xslDoc, oResult);
	};
	/** 
	* <p>Extends the Document class to emulate IE's transformNodeToObject</p>
	* @uses Mozilla's XSLTProcessor
	* @argument xslDoc The stylesheet to use (a DOM Document instance)
	* @argument oResult The Document to store the transformation result
	* @throws Errors that try to be informative ;-)
	*/
	Document.prototype.transformNodeToObject = function(xslDoc, oResult)
	{
		var xsltProcessor = null;
		try
		{
			xsltProcessor = new XSLTProcessor();
			if(xsltProcessor.reset)// new nsIXSLTProcessor is available
			{
            	xsltProcessor.importStylesheet(xslDoc);
            	var newFragment = xsltProcessor.transformToFragment(this, oResult);
            	//test is wrong if(!oResult)// initialize document,TODO: does IE react similarly?
            	//	oResult = document.implementation.createDocument("", "", null);
            	oResult._sarissa_copyDOM(newFragment);
			}
			else // only nsIXSLTProcessorObsolete is available
			{
            	xsltProcessor.transformDocument(this, xslDoc, oResult, null);
			}
		}
		catch(e)
		{
			if(xslDoc && oResult)
				throw "Failed to transform document. (original exception: "+e+")";
			else if(!xslDoc)
				throw "No Stylesheet Document was provided. (original exception: "+e+")";
			else if(!oResult)
				throw "No Result Document was provided. (original exception: "+e+")";
			else if(xsltProcessor == null)
                		throw "Could not instantiate an XSLTProcessor object. (original exception: "+e+")";
            		else
                		throw e;
		}
	};
	/** 
	* <p>Extends the Element class to emulate IE's transformNode</p>
	* <p><b>Note</b>: The result of your transformation must be well formed, otherwise you will get an error.</p>
	* @uses Mozilla's XSLTProcessor
	* @argument xslDoc The stylesheet to use (a DOM Document instance)
	* @returns the result of the transformation serialized to an XML String
	*/
	Element.prototype.transformNode = function(xslDoc)
	{
		var oDoc = document.implementation.createDocument("", "", null);
		oDoc._sarissa_copyDOM(this);
		return oDoc.transformNode(xslDoc);
	}
	/** 
	* <p>Extends the Document class to emulate IE's transformNode</p>
	* <p><b>Note</b>: The result of your transformation must be well formed, otherwise you will get an error.</p>
	* @uses Mozilla's XSLTProcessor
	* @argument xslDoc The stylesheet to use (a DOM Document instance)
	* @returns the result of the transformation serialized to an XML String
	*/
	Document.prototype.transformNode = function(xslDoc)
	{
		var out = document.implementation.createDocument("", "", null);
		this.transformNodeToObject(xslDoc, out);
		var str = null;
		try
		{
			var serializer = new XMLSerializer();
			str = serializer.serializeToString(out);
		}
		catch(e)
		{
			throw "Failed to serialize result document. (original exception: "+e+")";
		}
		return str;
	};
	/**
	* <p>The item method extends the Array to behave as a NodeList. (To use in XPath related operations)
	* Mozilla actually has implemented NodeList but there's no way AFAIK to create one manually.</p>
	* @argument i the index of the member to return
	* @returns the member corresponding to the given index
	*/
	Array.prototype.item = function(i)
	{
		return this[i];
	};
	/**
	* <p>The expr property extends the Array to emulate IE's expr property (Here the Array object is given as the result of
	* selectNodes).</p>
	* @returns the XPath expression passed to selectNodes that resulted in this Array (mimmicking NodeList)
	*/
	Array.prototype.expr = "";
	/** dummy, used to accept IE's stuff without throwing errors */
	XMLDocument.prototype.setProperty  = function(x,y){};
	/**
	* <p>Programmatically control namespace URI/prefix mappings for XPath queries.</p>
	* <p>This method comes especially handy when used to apply XPath queries on XML documents with a default namespace, as there is no other way of mapping that to a prefix.</p>
	* <p>Using no namespace prefix in DOM Level 3 XPath queries, implies you are looking for elements in the null namespace. If you need to look for nodes in the default namespace, you need to map a prefix to it first like:</p>
	* <pre>Sarissa.setXpathNamespaces(oDoc, &quot;xmlns:myprefix=&apos;http://mynsURI&apos;&quot;);</pre>
	* <p><b>Note 1</b>: Use this method only if the source document features a default namespace (without a prefix). You will need to map that namespace to a prefix for queries to work.</p>
	* <p><b>Note 2</b>: This method calls IE's setProperty method to set the appropriate namespace-prefix mappings, so you dont have to do that.</p>
	* @param oDoc The target XMLDocument to set the namespace mappings for.
	* @param sNsSet A whilespace-seperated list of namespace declarations as those would appear in an XML document. E.g.: <code>&quot;xmlns:xhtml=&apos;http://www.w3.org/1999/xhtml&apos; 
xmlns:&apos;http://www.w3.org/1999/XSL/Transform&apos;&quot;</code>
	* @throws An error if the format of the given namespace declarations is bad.
	*/
	Sarissa.setXpathNamespaces = function(oDoc, sNsSet)
	{
		//oDoc._sarissa_setXpathNamespaces(sNsSet);
		oDoc._sarissa_useCustomResolver = true;
		var namespaces = sNsSet.indexOf(" ")>-1?sNsSet.split(" "):new Array(sNsSet);
		oDoc._sarissa_xpathNamespaces = new Array(namespaces.length);
		for(i=0;i < namespaces.length;i++)
		{
			var ns = namespaces[i];
			var colonPos = ns.indexOf(":");
			var assignPos = ns.indexOf("=");
			if(colonPos == 5 && assignPos > colonPos+2)
			{
				var prefix = ns.substring(colonPos+1, assignPos);
				var uri = ns.substring(assignPos+2, ns.length-1);
				oDoc._sarissa_xpathNamespaces[prefix] = uri;
			}
			else
			{
				throw "Bad format on namespace declaration(s) given";
			}
		}
	};
	
	/** @private Flag to control whether a custom namespace resolver should be used, set to true by Sarissa.setXpathNamespaces */
	XMLDocument.prototype._sarissa_useCustomResolver = false;
	XMLDocument.prototype._sarissa_xpathNamespaces = new Array();
	/** 
	* <p>Extends the XMLDocument to emulate IE's selectNodes.</p>
	* @argument sExpr the XPath expression to use
	* @argument contextNode this is for internal use only by the same method when called on Elements
	* @returns the result of the XPath search as a NodeSet (actually, an extended Array)
	* @throws An error if no namespace URI is found for the given prefix.
	*/
	XMLDocument.prototype.selectNodes = function(sExpr, contextNode)
	{
		var nsDoc = this;
		var nsresolver = this._sarissa_useCustomResolver
		                   ? function(prefix)
				     {
				     	var s = nsDoc._sarissa_xpathNamespaces[prefix];
					if(s)return s;
					else throw "No namespace URI found for prefix: '" + prefix+"'"}
				   : this.createNSResolver(this.documentElement);
		var oResult = this.evaluate(sExpr, 
                                    (contextNode?contextNode:this), 
				    nsresolver, 
                                    XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
		var nodeList = new Array(oResult.snapshotLength);
		nodeList.expr = sExpr;
		for(i=0;i<nodeList.length;i++)
			nodeList[i] = oResult.snapshotItem(i);
		return nodeList;
	};
	/** 
	* <p>Extends the Element to emulate IE's selectNodes.</p>
	* @argument sExpr the XPath expression to use
	* @returns the result of the XPath search as an (extended) Array
	* @throws An error if invoked on an  HTML Element as this is only be available to XML Elements.
	*/
	Element.prototype.selectNodes = function(sExpr)
	{
		var doc = this.ownerDocument;
		if(doc.selectNodes)
			return doc.selectNodes(sExpr, this);
		else
			throw "Method selectNodes is only supported by XML Elements";
	};
	/** 
	* <p>Extends the XMLDocument to emulate IE's selectSingleNodes.</p>
	* @argument sExpr the XPath expression to use
	* @argument contextNode this is for internal use only by the same method when called on Elements
	* @returns the result of the XPath search as an (extended) Array
	*/
	XMLDocument.prototype.selectSingleNode = function(sExpr, contextNode)
	{
		var ctx = contextNode?contextNode:null;
		sExpr += "[1]";
		var nodeList = this.selectNodes(sExpr, ctx);
		if(nodeList.length > 0)
			return nodeList[0];
		else 
			return null;
	};
	/**
	* <p>Extends the Element to emulate IE's selectNodes.</p>
	* @argument sExpr the XPath expression to use
	* @returns the result of the XPath search as an (extended) Array
	* @throws An error if invoked on an  HTML Element as this is only be available to XML Elements.
	*/
	Element.prototype.selectSingleNode = function(sExpr)
	{
		var doc = this.ownerDocument;
		if(doc.selectSingleNode)
			return doc.selectSingleNode(sExpr, this);
		else
			throw "Method selectNodes is only supported by XML Elements";
	};
	
	/** 
	* <p>Returns a human readable description of the parsing error. Usefull 
	* for debugging. Tip: append the returned error string in a &lt;pre&gt; 
	* element if you want to render it.</p>
	* <p>Many thanks to  Christian Stocker for the reminder and code.</p>
	* @argument oDoc The target DOM document
	* @returns The parsing error description of the target Document in human readable form (preformated text)
	*/
	Sarissa.getParseErrorText = function (oDoc) 
	{
		if (oDoc.documentElement.tagName == "parsererror") 
		{
			var parseErrorText = oDoc.documentElement.firstChild.data;
			parseErrorText += "\n" +  oDoc.documentElement.firstChild.nextSibling.firstChild.data;
			return parseErrorText;
		}
	}
	
}
else if (_SARISSA_IS_IE)
{
	// Add NodeType constants; missing in IE4, 5 and 6
	if(!window.Node)
	{
		var Node = {
			ELEMENT_NODE: 1,
			ATTRIBUTE_NODE: 2,
			TEXT_NODE: 3,
			CDATA_SECTION_NODE: 4,
			ENTITY_REFERENCE_NODE: 5,
			ENTITY_NODE: 6,
			PROCESSING_INSTRUCTION_NODE: 7,
			COMMENT_NODE: 8,
			DOCUMENT_NODE: 9,
			DOCUMENT_TYPE_NODE: 10,
			DOCUMENT_FRAGMENT_NODE: 11,
			NOTATION_NODE: 12
		}
	}
	// implement importNode for IE
	if(!document.importNode)
	{
		/**
		* Implements importNode for IE using innerHTML. Main purpose it to be able 
		* to append Nodes from XMLDocuments to the current page in IE.
		* @param oNode the Node to import
		* @param bChildren whether to include the children of oNode
		* @returns the imported node for further use
		*/
		document.importNode = function(oNode, bChildren)
		{
			var importNode = document.createElement("div");
			if(bChildren)
				importNode.innerHTML = oNode.xml;
			else
				importNode.innerHTML = oNode.cloneNode(false).xml;
			return importNode.firstChild;
		};
	}//if(!document.importNode)
	
	// for XSLT parameter names
	_SARISSA_IEPREFIX4XSLPARAM = "xsl:";
	// used to store the most recent ProgID available out of the above
	var _SARISSA_DOM_PROGID = "";
	var _SARISSA_XMLHTTP_PROGID = "";
	/** Called when the Sarissa_xx.js file is parsed, to pick most recent ProgIDs for IE, then gets destroyed. */
	function pickRecentProgID(idList)
	{
		// found progID flag
		var bFound = false;
		for (var i=0; i < idList.length && !bFound; i++)
		{
			try
			{
				var oDoc = new ActiveXObject(idList[i]);
				o2Store = idList[i];
				bFound = true;
			}
			catch (objException)
			{
				// trap; try next progID
			}
		}
		if (!bFound)
			throw "Could not retreive a valid progID of Class: " + idList[idList.length-1]+". (original exception: "+e+")";
		idList = null;
		return o2Store;
	};
	// store proper progIDs
	_SARISSA_DOM_PROGID = pickRecentProgID(["Msxml2.DOMDocument.4.0", "Msxml2.DOMDocument.3.0", "MSXML2.DOMDocument", "MSXML.DOMDocument", "Microsoft.XmlDom"]);
	_SARISSA_XMLHTTP_PROGID = pickRecentProgID(["Msxml2.XMLHTTP.4.0", "MSXML2.XMLHTTP.3.0", "MSXML2.XMLHTTP", "Microsoft.XMLHTTP"]);
	// we dont need this anymore
	pickRecentProgID = null;
	//============================================
	// Factory methods (IE)
	//============================================
	// see mozilla version
	Sarissa.getDomDocument = function(sUri, sName)
	{
		var oDoc = new ActiveXObject(_SARISSA_DOM_PROGID);
		// if a root tag name was provided, we need to load it in the DOM object
		if (sName)
		{
			// if needed, create an artifical namespace prefix the way Moz does
			if (sUri)
			{
				oDoc.loadXML("<a" + _sarissa_iNsCounter + ":" + sName + " xmlns:a" + _sarissa_iNsCounter + "=\"" + sUri + "\" />");
				// don't use the same prefix again
				++_sarissa_iNsCounter;
			}
			else
				oDoc.loadXML("<" + sName + "/>");
		}
		return oDoc;
	};
	// see mozilla version
	Sarissa.getXmlHttpRequest = function()
	{
		return new ActiveXObject(_SARISSA_XMLHTTP_PROGID);
	};
	// see mozilla version
	Sarissa.getParseErrorText = function (oDoc) 
	{
		var parseErrorText = "XML Parsing Error: " + oDoc.parseError.reason +" \n";
		parseErrorText += "Location: " + oDoc.parseError.url + "\n";
		parseErrorText += "Line Number " + oDoc.parseError.line ;
		parseErrorText += ", Column " + oDoc.parseError.linepos + ":\n";
		parseErrorText += oDoc.parseError.srcText + "\n";
		for (var i = 0;  i < oDoc.parseError.linepos;i++)
			parseErrorText += "-";
		parseErrorText +=  "^\n";
		return parseErrorText;
	}
	// see mozilla version
	Sarissa.setXpathNamespaces = function(oDoc, sNsSet)
	{
		oDoc.setProperty("SelectionLanguage", "XPath");
		oDoc.setProperty("SelectionNamespaces", sNsSet);
	};
}
/** 
* <p>Factory Class.</p>
* @constructor
*/
function Sarissa(){}
// TODO: figure out how to implement support for both Mozilla's and IE's 
// XSL Processor objects to improove performance for reusable stylesheets.
/** 
 * <p>Factory method, used to set xslt parameters.</p>
 * <p><b>Note</b> that this method can only work for the main stylesheet and not any included/imported files.</p>
 * @argument oXslDoc the target XSLT DOM Document
 * @argument sParamName the name of the XSLT parameter
 * @argument sParamValue the value of the XSLT parameter
 * @returns whether the parameter was set succefully
 */
Sarissa.setXslParameter = function(oXslDoc, sParamQName, sParamValue)
{
	try
	{
		var params = oXslDoc.getElementsByTagName(_SARISSA_IEPREFIX4XSLPARAM+"param");
		var iLength = params.length;
		var bFound = false;
		var param;
		
		if(sParamValue)
		{
			for(i=0; i < iLength && !bFound;i++)
			{
				// match a param name attribute with the name given as argument
				if(params[i].getAttribute("name") == sParamQName)
				{
					param = params[i];
					// clean up the parameter
					while(param.firstChild)
						param.removeChild(param.firstChild);
					if(!sParamValue || sParamValue == null)
					{
						// do nothing; we've cleaned up the parameter anyway
					}
					// if String
					else if(typeof sParamValue == "string")
					{ 
						param.setAttribute("select", sParamValue);
						bFound = true;
					}
					// if node
					else if(sParamValue.nodeName)
					{
						param.removeAttribute("select");
						param.appendChild(sParamValue.cloneNode(true));
						bFound = true;
					}
					// if NodeList
					else if (sParamValue.item(0)
						&& sParamValue.item(0).nodeType)
					{
						for(j=0;j < sParamValue.length;j++)
						if(sParamValue.item(j).nodeType) // check if this is a Node
							param.appendChild(sParamValue.item(j).cloneNode(true));
						bFound = true;
					}
					// if Array or IE's IXMLDOMNodeList
					else
						throw "Failed to set xsl:param "+sParamQName+" (original exception: "+e+")";
				}
			}
		}
		return bFound;
	}
	catch(e)
	{
		throw e;
		return false;
	}
}
// EOF
