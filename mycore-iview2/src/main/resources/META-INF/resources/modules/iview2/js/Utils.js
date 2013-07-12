var iview = iview || {}; // holds API
var Iview = Iview || {}; // holds instances

iview.utils = iview.utils || {};
/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.utils
 * @name 		SimpleIterator
 * @description Allows it to get java like the next object within a given Collection or Object
 */
iview.utils.SimpleIterator = function(objectIterate) {
	/**
	 * @private
	 * @name		items
	 * @memberOf	iview.utils.SimpleIterator#
	 * @type		Array
	 * @description	items to iterate over
	 */
	this._items = [];
	/**
	 * @private
	 * @name		curPos
	 * @memberOf	iview.utils.SimpleIterator#
	 * @type		integer
	 * @description	current Position within the elements
	 */
	this._curPos = 0;
	var that = this;
	
	//add all entries to our
	for (entry in objectIterate) {
		if (typeof entry != "object") {
			entry = objectIterate[entry];
		}
		if (entry != null) {
			that._items.push(jQuery.extend({}, entry));
		}
	}
};
(function() {
	/**
	 * @function
	 * @name		hasNext
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description proofs if there are any items remaining within the collection (in forward direction)
	 *  note once the end is reached this function can return true as soon as a getPrevious() was called
	 * @return		{boolean} which tells if there is any further object or not
	 */
	function hasNext() {
		if (this._curPos < this._items.length) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @function
	 * @name		next
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	gives the user the next object while moving forward. This function will return null objects
	 *  if the end is reached
	 * @return		{arbitrary type} entry which is next in iteration
	 */
	function next() {
		if (this._curPos < this._items.length) {
			return this._items[this._curPos++];
		} else {
			return null;
		}
	}
	
	/**
	 * @function
	 * @name		hasPrevious
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	proofs if there are any items remaining within the collection (in backward direction)
	 *  note once the beginning is reached this function can return true as soon as a getNext() was called
	 * @return		{boolean} which tells if there is a previous object or not
	 */
	function hasPrevious() {
		if (this._curPos > 0) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * @function
	 * @name		previous
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	gives the user the next object while moving backward. This function will return null objects
	 *  if the beginning is reached
	 * @return		{arbitrary type} entry which is previous in interation
	 */
	function previous() {
		if (this._curPos > 0) {
			return this._items[this._curPos--];
		} else {
			return null;
		}
	}
	
	var prototype = iview.utils.SimpleIterator.prototype;
	prototype.hasNext = hasNext;
	prototype.next = next;
	prototype.hasPrevious = hasPrevious;
	prototype.previous = previous;
})();

iview.IViewObject = (function(){
  "use strict";
  function constructor(iviewInst){
    this._iview=iviewInst;
    hideProperty(this,"_iview", false);
  }
  constructor.prototype = {
      getViewer: function iv_getViewer(){
        return this._iview;
      }
  };
  return constructor;
})();

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
  if (typeof Object.defineProperty === 'function' && false){
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

function log(){
  if (typeof(console)!=="undefined" && typeof(console.log)!=="undefined") {
	  try
	  {
		  console.log(arguments);
	  }
	  catch(error)
	  {
		  //nothing		 
	  }
  }
}

/**
 * Shows a Message to the User
 * @param {Object} i18nKey the i18n key for the message that should be shown
 */
function showMessage(i18nKey){
	var message = i18n.translate(i18nKey);
	jQuery("<div id=\"" + i18nKey + "_message\">" + message + "</div>").dialog({
		height : 140,
		modal : true
	});
}

var URL = { "location": window.location};
/**
 * @public
 * @function
 * @name		getParam
 * @memberOf	URL
 * @description	additional function, look for a parameter into search string an returns it
 * @param		{string} param parameter whose value you want to have
 * @return		{string} if param was found it's value, else an empty string
 */
URL.getParam = function(param) {
	try {
		return(this.location.search.match(new RegExp("[?|&]?" + param + "=([^&]*)"))[1]);
	} catch (e) {
		return "";
	}
};

/**
 * @public
 * @function
 * @memberOf URL
 * @description cleans a URL to prevent Remote File Inclusion
 * @param {Object} url the url that should be cleaned (if null then the current URL will be used)
 * @return the cleaned URL
 */
URL.getCleanUrl = function(url){
	if(url != null || typeof url !== undefined){
		return url.replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/, "§");
	} else {
		var currentUrl = window.location;
		return currentUrl.replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/, "§");
	}
	
}

/**
 * Provides requestAnimationFrame in a cross browser way.
 * @author paulirish / http://paulirish.com/
 */
if (!window.requestAnimationFrame) {
	window.requestAnimationFrame = (function() {
		return window.webkitRequestAnimationFrame ||
		window.mozRequestAnimationFrame ||
		window.oRequestAnimationFrame ||
		window.msRequestAnimationFrame ||
		function( /* function FrameRequestCallback */ callback, /* DOMElement Element */ element ) {
			window.setTimeout( callback, 1000 / 60 );
		};
	})();
}
https://github.com/twitter/bootstrap/issues/6094

// https://github.com/twitter/bootstrap/issues/6094
//var btn = $.fn.button.noConflict() // reverts $.fn.button to jqueryui btn
//$.fn.btn = btn // assigns bootstrap button functionality to $.fn.btn