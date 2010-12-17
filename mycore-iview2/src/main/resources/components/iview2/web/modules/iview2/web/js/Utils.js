/**
 * @public
 * @function
 * @name		alertError
 * @memberOf	iview.Utils
 * @description	generates from the given Information an Error Alert so that the user is able to supply better Error Traceing
 * @param		{string} msg own the description so that a better location where and when the error occured is possible
 * @para,		{} nr number of the error to display
 * @param		{} e the error event from which the more detailed information will gained
 */
 function alertError(msg, nr, e) {
		alert("Error " + toInt(nr) + ": " + msg + "\n" 
				+ "Error Occured in File: " + e.fileName + ", line:" + e.lineNumber + "\n" 
				+ "JS-Error " + e.name + ": " + e.message + "\n"
				+ "Browser: " + navigator.userAgent);
	}

/**
 * @public
 * @function
 * @name		getEvent
 * @memberOf	iview.Utils
 * @description	proofs if e is set, if it isn't were on IE which doesn't give the event with it
 * @param		{} e var which will be proofed if event is supplied or not
 * @return		{event} which just happened
 */
function getEvent(e) {
	if (!e) {
		e = window.event;
	}
	return e;
}

/**
 * @public
 * @function
 * @name		getElementsByClassName
 * @memberOf	iview.Utils
 * @description	returns all elements which are set to the supplied class within the given node. further it's possible
  to return only the elements which belong to the given tag type
 * @param	{string} searchClass represents the Class to search Tags for
 * @param	{object} node optional node-object which child's will be looked through for the supplied class, can be left or set to null to
  use as start node the document object
 * @param	{} tag optional the tagname to look for; if a name is supplied only elements matching the tag and class will be returned
 which are child's of the given node
 * @return	{array} of objects which matched the search conditions
 */
var getElementsByClassName = function (searchClass, node, tag){
	if (Array.filter && document.getElementsByClassName){
		// Fast JS 1.6 Implementation
		getElementsByClassName = function (searchClass, node, tag) {
			if (node!=null && typeof(node)!="object")
				node=document.getElementById(node);
			node = node || document;
			if (tag!=null){
				tag=tag.toUpperCase();
				return Array.filter(node.getElementsByClassName(searchClass), function(elem){
					return elem.nodeName == tag; 
				});
			}
			return node.getElementsByClassName(searchClass);
		};
	} else if (document.evaluate) {                                                                                                                                                                   
        getElementsByClassName = function (searchClass, node, tag){                                                                                                                               
        	if (node!=null && typeof(node)!="object")
        		node=document.getElementById(node);
        	node = node || document;
            tag = tag || "*";                                                                                                                                                               
            var classes = searchClass.split(" "),                                                                                                                                             
                    classesToCheck = "",                                                                                                                                                    
                    xhtmlNamespace = "http://www.w3.org/1999/xhtml",                                                                                                                        
                    namespaceResolver = (document.documentElement.namespaceURI === xhtmlNamespace)? xhtmlNamespace : null,                                                                  
                    returnElements = [],                                                                                                                                                    
                    elements,                                                                                                                                                               
                    node;                                                                                                                                                                   
            for(var j=0, jl=classes.length; j<jl; j+=1){                                                                                                                                    
                    classesToCheck += "[contains(concat(' ', @class, ' '), ' " + classes[j] + " ')]";                                                                                       
            }                                                                                                                                                                               
            try     {                                                                                                                                                                       
                    elements = document.evaluate(".//" + tag + classesToCheck, node, namespaceResolver, 0, null);
            }
            catch (e) {
                    elements = document.evaluate(".//" + tag + classesToCheck, node, null, 0, null);
            }
            while ((node = elements.iterateNext())) {
                    returnElements.push(node);
            }
            return returnElements;
        };
	} else {
        getElementsByClassName = function (searchClass, node, tag) {
        	if (node!=null && typeof(node)!="object")
        		node=document.getElementById(node);
        	node = node || document;
            tag = tag || "*";
            var classes = searchClass.split(" "),
                    classesToCheck = [],
                    elements = (tag === "*" && node.all)? node.all : node.getElementsByTagName(tag),
                    current,
                    returnElements = [],
                    match;
            for(var k=0, kl=classes.length; k<kl; k+=1){
                    classesToCheck.push(new RegExp("(^|\\s)" + classes[k] + "(\\s|$)"));
            }
            for(var l=0, ll=elements.length; l<ll; l+=1){
                    current = elements[l];
                    match = false;
                    for(var m=0, ml=classesToCheck.length; m<ml; m+=1){
                            match = classesToCheck[m].test(current.className);
                            if (!match) {
                                    break;
                            }
                    }
                    if (match) {
                            returnElements.push(current);
                    }
            }
            return returnElements;
        };
	}
	return getElementsByClassName(searchClass, node, tag);
};

/**
 * @public
 * @function
 * @name	toInt
 * @memberOf	iview.Utils
 * @description	converts a given String into an integer, but further doesn't give NAN for String was undefined and other cases
 * @param	{string} value which shall be converted into an integer
 * @return	{integer} of parsed string, 0 if var was undefined
 */
function toInt(value) {
	if (isNaN(parseInt(value))) {
		return 0;
	} else {
		return parseInt(value);
	}
}

/**
 * @public
 * @function
 * @name	toFloat
 * @memberOf	iview.Utils
 * @description	converts a given String into an float, but further doesn't give NAN for String was undefined and other cases
 * @param	{string} value which shall be converted into a float
 * @return	{float} of parsed string, 0 if var was undefined
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

/**
 * @public
 * @function
 * @name		get
 * @memberOf	String
 * @description	additional function, look for a parameter into a (query) string an returns it
 * @param		{string} p parameter whose value you want to have
 * @return		{string} if p was found it'S value else an empty string
 */
String.prototype.get = function(p){
	try {
		return(this.match(new RegExp("[?|&]?" + p + "=([^&]*)"))[1]);
	} catch (e) {
		return "";
	}
}

var theBrowser = "";
/**
 * @public
 * @function
 * @name		getBrowser
 * @memberOf	iview.Utils
 * @description	tests if a given Array of Browsertypes match the used browser
 * @param		{string} browser Array with the Names of the Browsers or some parts of the useragent which shall be looked up
 * @return		{boolean} true if within the supplied array the current browser was named or false if it wasn't
 */
function getBrowser(browser) {
	var browser = browser.toLowerCase();
	if (window.opera) { theBrowser = "Opera"; return;}
	if (window.attachEvent && !window.opera) { theBrowser = "IE"; return;}
	if (window.addEventListener && !window.attachEvent && navigator.userAgent.indexOf("Firefox") != -1) { theBrowser = "Firefox"; return;}
//	if (navigator.userAgent.indexOf("Chrome") != -1) { theBrowser = "Chrome"; return;}
	if (navigator.userAgent.indexOf(browser) != -1) { theBrowser = browser; return;}
	if (navigator.userAgent.indexOf("Safari") != -1) { theBrowser = "Safari"; return;}
	theBrowser = "unknown";
}

/**
 * @public
 * @function
 * @name	isBrowser
 * @memberOf	iview.Utils
 * @description	
 * @param	{array} browsers 
 * @return	
 */
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

/**
 * @public
 * @function
 * @deprecated	use jQuery Mousewheel Plugin instead
 * @name		returnDelta
 * @memberOf	iview.Utils
 * @description	calculates normalized the mousedelta of a supplied event and prevents if requested the default behavior
 * @param		{} e MouseScroll Event from which the delta shall be gained
 * @param		{} prevent prevent default behave, yes or no
 * @return	the mousedelta normalized
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

/**
 * @public
 * @function
 * @name	colorToRGBArray
 * @memberOf	iview.Utils
 * @description	converts a string which holds a Color somehow encoded and converts it to an RGB Array
 * @param		{string} color String which holds the encoded Color information which will be decoded
 * @return		{array} withe the RGB-color-values
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


/**
 * @public
 * @function
 * @name		hexCharToZif
 * @memberOf	iview.Utils
 * @description	determines the decimal value of a hex value
 * @param		{string} x holds the hexadecimal letter for which the decimal-number should be determined
 * @return		the decimal-number as integer
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

/**
 * @public
 * @function
 * @name		arabToRoem
 * @memberOf	iview.Utils
 * @description	determine a roman number from a arabic number
 * @param	{float} ArabischeZahl is the arabic number
 * @return	the roman number
 */
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
