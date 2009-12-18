var hasTextContent = (typeof(document.getElementsByTagName("body")[0].textContent) != "undefined")? true:false;

/*
 @description retrieves from an element like <div>Some</div> the inner text, in this case it would get "Some"
 @param elem the element to retrieve the innerText from
 @return String which represents the innerText of the supplied Element
 */
function getInnerText(elem) {
	if (!hasTextContent) {
		return elem.text;
	} else {
		return elem.textContent;
	}
}
/*
 @description generates from the given Information an Error Alert so that the user is able to supply better Error Traceing
 @param msg The own description so that a better location where and when the error occured is possible
 @param nr Number for the Error to display
 @param e the error Event from which the more detailed information will gained
 */
 function alertError(msg, nr, e) {
		alert("Error " + toInt(nr) + ": " + msg + "\n" 
				+ "Error Occured in File: " + e.fileName + ", line:" + e.lineNumber + "\n" 
				+ "JS-Error " + e.name + ": " + e.message + "\n"
				+ "Browser: " + navigator.userAgent);
	}
/*
@description proofs if e is set, if it isn't were on IE which doesn't give the event with it
@param e var which will be proofed if event is supplied or not
@return event var of the current event which happened
*/
function getEvent(e) {
	if (!e) {
		e = window.event;
	}
	return e;
}

/*
 @description returns all Elements which are set to the supplied class within the given Node. Further it's possible
  to return only the Elements which belong to the given tag Type
 @param searchClass String which represents the Class to search Tags for
 @param node Optional Node-Object which child's will be looked through for the supplied Class, can be left or set to null to
  use as start node the Document Object
 @param tag  Optional the Tagname to look for; if a Name is supplied only Elements matching the tag and Class will be returned
 which are child's of the given Node
 @return Array of Objects which matched the search Conditions
 */
function getElementsByClassName(searchClass, node, tag) {
	if (document.getElementsByClassName) {
		if (typeof arguments[1] == "undefined" || node == null )
			node = document;
		var nodes = $(node).getElementsByClassName(searchClass);
		if (typeof arguments[2] != "undefined" || tag != null) {
			var tagNode = new Array;
			var count = 0;
			for (var i = 0; i < nodes.length; i++) {
				if (nodes[i].nodeName.toLowerCase() == tag.toLowerCase()) {
					tagNode[count++] = nodes[i];
				}
			}
			return tagNode;//Return all Nodes with the given Tag and Class
		} else {
			return nodes;//Simply return all Found nodes which are childs of the Starting Node
		}
	} else {
		var classElements = new Array();
		if (typeof arguments[1] == "undefined" || node == null )
			node = document;
		if (typeof arguments[2] == "undefined" || tag == null )
			tag = '*';
		var els = $(node).getElementsByTagName(tag);
		var elsLen = els.length;
		var pattern = new RegExp("(^|\\s)"+searchClass+"(\\s|$)");
		for (i = 0, j = 0; i < elsLen; i++) {
			if ( pattern.test(els[i].className) ) {
				classElements[j] = els[i];
				j++;
			}
		}
		return classElements;
	}
}

/*
@decription proofs if the supplied ClassName has currently any object connected with it. If so it returns true, else false
@param className the ClassName which shall be checked if it's in use
@return boolean which tells if the ClassName has currenly any Object linked to it or not.
 */
function classIsUsed(className) {
	if (getElementsByClassName(className).length != 0) {
		return true;
	} else {
		return false;
	}
}

/*
@description get Each Element of the supplied ClassName and performs the given Action on it
@param className the String which holds the ClassName to work with
@param perform the action which will be applied to each Element of the Class
@param arguments[3] supplies the viewID so that only Elements in this group are touched
*/
function doForEachInClass(className, perform) {
	var viewID = "";
	if (typeof(arguments[2]) != "undefined") {
		viewID = arguments[2];
	}
	var elements = getElementsByClassName(className);
	for (var i = 0; i < elements.length; i++) {
		
		if (elements[i].className.lastIndexOf(viewID) != -1) {
			eval( "elements[i]" + perform);		
		}
	}
}

/*
@description returns the object of a given String/Object, Object is then directly accessable by $(foobar).MyFunction
@param id the Id or the object itself which we want to access
@return object which was requested
*/
function $(id) {
	if (typeof id == "object") {
		return id;
	} else {
		return document.getElementById(id);
	}
}

/*
@description converts a given String into an integer, but further doesn't give NAN for String was undefined and other cases
@param value which shall be converted into an integer
@return integer of parsed string, 0 if var was undefined
*/
function toInt(value) {
	if (isNaN(parseInt(value))) {
		return 0;
	} else {
		return parseInt(value);
	}
}

/*
@description converts a given String into an float, but further doesn't give NAN for String was undefined and other cases
@param value which shall be converted into a float
@return float of parsed string, 0 if var was undefined
*/
function toFloat(value) {
	if (isNaN(parseFloat(value))) {
		return 0;
	} else if (!isFinite(parseFloat(value))) {
		return Number.MAX_VALUE;
	} else {
		return parseFloat(value);
	}
}

/*
@description additional function, look for a parameter into a (query) string an returns it
@param e Parameter dessen Wert man haben will
@return Bei Erfolg der Parameterwert, bei Misserfolg ein leerer String
*/
String.prototype.get = function(p){
	try {
		return(this.match(new RegExp("[?|&]?" + p + "=([^&]*)"))[1]);
	} catch (e) {
		return "";
	}
}

/*
@description tests if a given Array of Browsertypes match the used browser
@param Array with the Names of the Browsers or some parts of the useragent which shall be looked up
@return boolean true if within the supplied array the current browser was named or false if it wasn't
*/
var theBrowser = "";
function getBrowser(browser) {
	var browser = browser.toLowerCase();
	if (window.opera) { theBrowser = "Opera"; return;}
	if (window.attachEvent && !window.opera) { theBrowser = "IE"; return;}
	if (window.addEventListener && !window.attachEvent && navigator.userAgent.indexOf("Firefox") != -1) { theBrowser = "Firefox"; return;}
	if (navigator.userAgent.indexOf(browser) != -1) { theBrowser = browser; return;}
	if (navigator.userAgent.indexOf("Safari") != -1) { theBrowser = "Safari"; return;}
	theBrowser = "unknown";
}

function isBrowser(browsers) {

	if (theBrowser == "") {
		if (!browsers.sort) {
			getBrowser(browsers);
		} else {
			for (var i = 0 ; i < browsers.length; i++) {
				getBrowser(browsers[i]);
			}
		}
	}
	if ((browsers.sort && browsers.join(";").match(new RegExp(theBrowser + "(;|$)", "gi"))) || (!browsers.sort && browsers.indexOf(theBrowser) != -1)) {
		return true;
	} else { 
		return false;
	}
}

/*
@description calculates normalized the mousedelta of a supplied event and prevents if requested the default behavior
@param e MouseScroll Event from which the delta shall be gained
@param prevent prevent default behave, yes or no
@return das mousedelta normalized
*/
function returnDelta(e, prevent) {
        e = getEvent(e);
        var delta = {x : 0, y : 0};
        if (e.wheelDeltaX && e.wheelDeltaY) { /* fine in Safari */
        	delta.x = e.wheelDeltaX/50;
        	delta.y = e.wheelDeltaY/50;
        } else if (e.wheelDelta) { /* IE/Opera. */
                delta.y = e.wheelDelta/50;
                /** In Opera 9, delta differs in sign as compared to IE.
                 */
        } else if (e.detail) { /** Mozilla case. */
                /** In Mozilla, sign of delta is different than in IE.
                 * Also, delta is multiple of 3.
                 */
				if (e.axis && e.axis == e.HORIZONTAL_AXIS)
					delta.x = -e.detail/2;
				else
					delta.y = -e.detail/2;
        }
	
	if (prevent) {
        preventDefault(e);
	}
	return delta;
}

/*
@description realize to read out values from the CSS
@return the value for the inquired stylepropertiy
*/
function getStyle(el,styleProp)
{
	var x = $(el);
	//If the Browser is IE or Opera it's needed remove the - within the Property Identifier, so they can correctly gain these values
	if (isBrowser(['IE', 'Opera']) && styleProp.indexOf("-") != -1) {
		var parts = styleProp.split("-");
		styleProp = parts[0];
		for (var i = 1; i < parts.length; i++) {
			styleProp = styleProp + parts[i].charAt(0).toUpperCase() + parts[i].substr(1);
		}
	}
	if (x.currentStyle) {
		var y = x.currentStyle[styleProp];//For IE
	} else if (window.getComputedStyle) {
		var y = document.defaultView.getComputedStyle(x,null).getPropertyValue(styleProp);
	}
	return y;
}

/*
@description realize to read out values from the CSS for classes --- unused
@return the value for the inquired stylepropertiy
*/
function getStyleClass(el, elType ,styleProp)
{
	var x = getElementByClass(el, elType)[0];
	if (x.currentStyle) {
		var y = x.currentStyle[styleProp];
	} else if (window.getComputedStyle) {
		var y = document.defaultView.getComputedStyle(x,null).getPropertyValue(styleProp);
	}
	// IE ließt sonst Null statt Auto
	// otherwise IE reads 0 instead of auto
	if (y==null) { y=0 };
	return y;
}


function getCssProps(id, classType, parentId, properties) {
	var tmpDiv = document.createElement("div");
	tmpDiv.id = id;
	tmpDiv.className = classType;
	$(parentId).appendChild(tmpDiv);
	
	var value = new Array();
	for (i = 0; i < properties.length; i++) {
		if (arguments[4]) {
			value[i] = eval(arguments[4]+"(getStyle(tmpDiv,'"+properties[i]+"'))");
		} else {
			value[i] = getStyle(tmpDiv, properties[i]);
		}
	}
	$(parentId).removeChild(tmpDiv);
	return value;
}

/*
@description creates new objects with absolute style position
*/
function createAbsoluteObject(object, id, target) {

	var newObject = document.createElement(object);
	newObject.id = id;
	newObject.className = id;
	newObject.style.position = "absolute";
	$(target).appendChild(newObject);
}

function copyArray(array) {
	return array.slice(0, array.length);
}

/*
@description cut one sign after the next of a textline in the chapter until the correct width is reached
*/
function getTextWidth(txt, font, size) {
	var tmpBox = document.createElement("tmpBox");
	tmpBox.id = "tmpBox";
	tmpBox.style.fontFamily = "'"+font+"'";
	tmpBox.style.fontSize = parseInt(size) + "px";
	tmpBox.innerHTML = txt;
	document.getElementsByTagName("body")[0].appendChild(tmpBox);
	var returnValue = tmpBox.offsetWidth;
	document.getElementsByTagName("body")[0].removeChild(tmpBox);
	return returnValue;		
}

/*
@description cut Text to width
*/
function cutTxtToWidth(txt, font, size, width) {
	while (getTextWidth(txt, font, size) > width && txt != "") {
		//txt != "" da substring kein Fehler wirft bei Negativen Indizes
		txt = txt.substring(0,txt.length - 1);
		// if last sign == space, remove, because spaces are not a part of the offsetwidth
		if (txt.charAt(txt.length - 1) == ' ') {
			txt = txt.substring(0, txt.length - 1);
		}
	}
	return txt;
}

/*
 @description converts a string which holds a Color somehow encoded and converts it to an RGB Array
 @param color String which holds the encoded Color information which will be decoded
 */
function colorToRGBArray(color) {
	var red = 0;
	var green = 0;
	var blue = 0;
	if (color.indexOf("(") == -1 && color.indexOf(",") == -1) {//Hex Color to Convert
		red = hexCharToZif(color.toString().charAt(1)) * 16 + hexCharToZif(color.toString().charAt(2));
		green = hexCharToZif(color.toString().charAt(3)) * 16 + hexCharToZif(color.toString().charAt(4));
		blue = hexCharToZif(color.toString().charAt(5)) * 16 + hexCharToZif(color.toString().charAt(6));
	} else {
		color = color.substring(4, color.length - 1).split(', ');
		red = parseInt(color[0]);
		green = parseInt(color[1]);
		blue = parseInt(color[2]);
	}
	return new Array (red, green, blue);
}

/*
@description loops through the given Array and searches for the Element with
 the given Name and Value and returns it, if more Elements match the criteria
 only the first found is returned, if none is found Null is returned.
 The Function works with Arrays of Objects and with HTML-Nodes
@param values the Array where the Item is located in
@param name the Name of the Property which is used for search
@oaram value the Value of the Property to match the Element
@result the Element which matched the criteria first, or null if none was found
 */
function findInArrayElement(values, name, value) {
	for (var i = 0; i < values.length; i++) {
		if (values[i].attributes) {
			if (values[i].attributes.getNamedItem(name).value == value) {
				return values[i];
			}
		} else {
			if (values[i][name] == value) {
				return values[i];
			}
		}
	}
	return null;
}

/*
@description determine the decimal value of a hex value
*/
function hexCharToZif(x) {
	if (x == 'a') {
		x = 10;
	} else if (x == 'b') {
		x = 11;
	} else if (x == 'c') {
		x = 12;
	} else if (x == 'd') {
		x = 13;
	} else if (x == 'e') {
		x = 14;
	} else if (x == 'f') {
		x = 15;
	}
	return parseInt(x);
}

function arabToRoem(ArabischeZahl) {
	/* Konstante und Variable definieren */
	var EinheitRoemisch = new Array( "M", "D", "C", "L", "X", "V", "I");
	var EinheitArabisch = new Array(1000, 500, 100,  50,  10,   5,   1);
	var ArabischeZahl = parseInt(ArabischeZahl);      // Umwandeln der Eingabe in eine Ganzzahl
	var RoemischeZahl = "", Pos, Wert;

	// if (isNaN(ArabischeZahl) || (ArabischeZahl <= 0)) { return "Fehler"; }

	for (var i = 0; i < EinheitArabisch.length; i++) {
		while (ArabischeZahl >= EinheitArabisch[i]) {
			RoemischeZahl += EinheitRoemisch[i];
			ArabischeZahl -= EinheitArabisch[i];
		}
		for (pos = EinheitArabisch.length; pos > i; pos--) {
			Wert = EinheitArabisch[i] - EinheitArabisch[pos];
			if ((EinheitArabisch[pos] < Wert) && ( Wert <= ArabischeZahl)) {
				RoemischeZahl += EinheitRoemisch[pos] + EinheitRoemisch[i];
				ArabischeZahl -= Wert;
			}
		}
	}
	
	return RoemischeZahl;
}