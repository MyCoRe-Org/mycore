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

//TODO rename Function and make proper use where needed of it name callFunc
/**
 * @public
 * @function
 * @name		callBack
 * @description	execute the given function, if it's no function, do nothing or alert a comment
 * @param 		{function} func is the function whis is to be executed
 */
function callBack(func){
	if (func == null)
		return;
	if (typeof(func)=='function')
		func();
	else
		alert("Is not a function:\n"+func);
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

//};

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

function loadCssFile(pathToFile, id) {
    var css = jQuery("<link>");
    css.attr({
      rel:  "stylesheet",
      type: "text/css",
      href: pathToFile
    });
    if (id) {
    	css.attr('id',id);
    }
    jQuery("head").append(css);
}

function preventDefault(e) {
	if (e.preventDefault) {
		e.preventDefault();
	} else {
		e.returnValue = false;
	}
}

function hideProperty(o,name,writeable){
  if (typeof Object.defineProperty === 'function'){
    try {
      Object.defineProperty(o, name, {
        writable: (typeof writeable !== "undefined")? writeable : true,
        enumerable: false,
        configurable: false
      });
    } catch (ignoreException){
      log({"msg:":"Error while hiding property",
        "object:":o,
        "property:":name,
        "error:":ignoreException});
    }
  }
}

function log(msg){
  if (typeof(console)!=="undefined") {
    console.log(msg);
  }
}