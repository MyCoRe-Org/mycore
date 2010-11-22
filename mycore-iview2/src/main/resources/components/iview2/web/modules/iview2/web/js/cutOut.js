//TODO rewrite so das lokale Position auf min&maxWerte abgebildet wird
function cutOut() {
	cutOut.ONLOAD = 0;
	cutOut.SCROLL = 1;
	cutOut.DBL_CLICK = 2;
	cutOut.MOUSE_UP = 3;

	this.setWidth = setWidth;
	this.getWidthT = function() { return getWidth("T");};
	this.getWidth = function() { return getWidth("C");};
	this.setHeight = setHeight;
	this.getHeightT = function() { return getHeight("T");};
	this.getHeight = function() { return getHeight("C");};
	this.setPosition = setPosition;
	this.getPosition = getPosition;
	this.setSRC = setSRC;
	this.addListener = addListener;
	this.dropListener = dropListener;
	this.init = init;
	this.my = my;
	this.scroll = mouseScroll;
	this.mouseMove = mouseMove;
	this.mouseUp =  mouseUp;
	this.updateSize = updateSize;
	this.updatePos = updatePos;
	this.setViewID = setViewID
	
	var my = null;// cutOut-object
	var viewID = "";
	var ausschnittPosX = 0;// x-position of the left upper edge of the cutout
	var ausschnittPosY = 0;// y-position of the left upper edge of the cutout
	var curx = 0;// last x-position of the left upper edge of the cutout
	var cury = 0;// last y-osition of the left upper edge of the cutout
	var mouseIsDown = false;// indicator if the mouse is pressed
	var inited = false;// indicator if the cutout was already initialized
	var listener = [];// array who holds informations about the listeners (?)
	var dampered = true;//saves if the thumbnail is folded or folded up
	var setPosBlock = false;

	/**
	 * @public
	 * @function
	 * @name	setViewID
	 * @memberOf	iview.cutOut
	 * @description	sets the ID which is needed for multiple Viewers on a page so that they different components are connected properly together
	 * @param	{string} id the ID of the Viewer the class is connected to
	 */
	function setViewID(id) {
		viewID = id;
	}

	//TODO Doku ergänzen
	/**
	 * @public
	 * @function	
	 * @name	init
	 * @memberOf	iview.cutOut
	 * @description	creates with the given Informations a Thumbnail Element
	 * @param	{string} id the ID of the newly created Thumbnail
	 * @param	{string} idC the ID of the CutOut Div
	 * @param	{string} idT the ID of the Thumbnail Image Element
	 * @param	{string} parent the Parent Id or Object where the Thumbnail shall be added
	 * @param	{string} idD the ID for the Damper Element
	 * @param	{string} parentD the Parent Element for the Damper
	 * @param	{string} identer
	 */
	function init(id, idC, idT, parent, idD, parentD, identer) {
		var complete = document.createElement("div");
		complete.id = id;
		complete.className = "thumb" + identer;
		complete.style.overflow = "hidden";
		var ausschnitt = document.createElement("div");
		ausschnitt.id = idC;
		ausschnitt.className = "ausschnitt" + identer;
		complete.appendChild(ausschnitt);
		var thumb = new Image();
		complete.appendChild(thumb);
		document.getElementById(parent).appendChild(complete);
		var damp = document.createElement("div");
		damp.id = idD;
		damp.className = "damp" + identer;
		var hide = document.createElement("div");
		hide.className = "hide";
		var show = document.createElement("div");
		show.className = "show";
		damp.appendChild(hide);
		damp.appendChild(show);
		document.getElementById(parentD).appendChild(damp);

		ManageEvents.addEventListener(complete, "mouseScroll", mouseScroll, false);
		ManageEvents.addEventListener(complete, "dblclick", dblClick, false);
		ManageEvents.addEventListener(ausschnitt, "mousedown", mouseDown, false);
		ManageEvents.addEventListener(complete, "mouseup", mouseUp, false);
		ManageEvents.addEventListener(complete, "mousemove", mouseMove, false);
		ManageEvents.addEventListener(damp, "mousedown", damper, false);
		
		// Browser-Drag&Drop deaktiveren
		complete.onmousedown = function() { return false;};
		
		my = {'self':complete, 'cutOut':ausschnitt, 'thumbnail':thumb, 'damp':damp};
		this.my = my;
		inited = true;
	}

	/**
	 * @public
	 * @function
	 * @name	setWidth
	 * @memberOf	iview.cutOut
	 * @description	ets the Width of the CutOut div
	 * @param	{integer} value represents the new Width of the CutOut
	 */
	function setWidth(value) {
		value = toInt(value);
		my.cutOut.style.width = value + "px";
	}
	
	/**
	 * @public
	 * @function
	 * @name	getWidth
	 * @memberOf	iview.cutOut
	 * @description	returns the width of the requested Element
	 * @param	{string} which Value T or C which defines if the Width of the Thumbnail or CutOut will be returned
	 * @return	{integer} which holds the width of the requested Element
	 */
	function getWidth(which) {
		if (!inited) return 0;
		switch (which) {
			case "T":
				return my.thumbnail.width;
				break;
			case "C":
				return toInt(my.cutOut.style.width);
				break;
			default:
				return 0;
				break;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name	setHeight
	 * @memberOf	iview.cutOut
	 * @description	sets the Height of the CutOut div
	 * @param	{integer} value represents the new Height of the CutOut
	 */
	function setHeight(value) {
		value = toInt(value);
		my.cutOut.style.height = value + "px";
	}
	
	/**
	 * @public
	 * @function
	 * @name	getHeight
	 * @memberOf	iview.cutOut
	 * @description	returns the height of the requested Element
	 * @param	{string} which Value T or C which defines if the height of the Thumbnail or CutOut will be returned
	 * @return	{integer} which holds the height of the requested Element
	 */
	function getHeight(which) {
		if (!inited) return 0;
		switch (which) {
			case "T":
				return my.thumbnail.height;
				break;
			case "C":
				return toInt(my.cutOut.style.height);
				break;
			default:
				return 0;
		}
	}
	
	//TODO Doku ergänzen
	/**
	 * @public
	 * @function
	 * @name	setPosition
	 * @memberOf	iview.cutOut
	 * @description	returns the height of the requested Element
	 * @param	{object} vector
	 * @param	{event} e holds the Event which raised it
	 */
	function setPosition(vector, e) {
		if(setPosBlock == true){
			setPosBlock = false;
			return;
		}
		vector.x = toInt(vector.x);
		vector.y = toInt(vector.y);
		if (vector.x + getWidth("C") > getWidth("T")) {
			vector.x = getWidth("T") - getWidth("C");
		} else if (vector.x < 0) {
			vector.x = 0;
		}

		if (vector.y + getHeight("C") > getHeight("T")) {
			vector.y = getHeight("T") - getHeight("C");
		} else if (vector.y < 0) {
			vector.y = 0;
		}
		my.cutOut.style.left = vector.x + "px";
		my.cutOut.style.top = vector.y + "px";
		if(e) e.cancelBubble = true;
	}

	/**
	 * @public
	 * @function
	 * @name	getPosition
	 * @memberOf	iview.cutOut
	 * @description	returns the Left and Top Position of the CutOut Area
	 * @return	{object} .x|.y holding integers with the connected values
	 */
	function getPosition() {
		return {'x':parseInt(my.cutOut.style.left), 'y':parseInt(my.cutOut.style.top)};
	}
	
	/**
	 * @public
	 * @function
	 * @name	setSRC
	 * @memberOf	iview.cutOut
	 * @description	sets for the Image a new Image Source which is then loaded
	 * @param	{string} url represents the new URL of the Image
	 */
	function setSRC(url) {
		my.self.removeChild(my.thumbnail);
		my.thumbnail = new Image();
		my.self.appendChild(my.thumbnail);
		my.thumbnail.onload = function() {notifyOnload();};
		my.thumbnail.src = url;
		my.thumbnail.style.verticalAlign = "bottom";
	}

	/**
	 * @public
	 * @function
	 * @name	notifyOnload
	 * @memberOf	iview.cutOut
	 * @description	as soon as the image is loaded all listeners who listen to Onload Events will be noticed that a new Image is loaded
	 */
	function notifyOnload() {
		if (my.thumbnail.complete && isBrowser(["IE","Opera"]) || my.thumbnail.naturalWidth > 2 && !isBrowser(["IE", "opera"])) {
			if (!listener[cutOut.ONLOAD]) {
				return;
			}
			for(var i = 0; i < listener[cutOut.ONLOAD].length; i++) {
				listener[cutOut.ONLOAD][i](viewID);
			}
		} else {
			window.setTimeout(function() { notifyOnload();},100);
		}
	}

	/**
	 * @public
	 * @function
	 * @name	dblClick
	 * @memberOf	iview.cutOut
	 * @description	when a Doubleclick is raised within the Thumbnail the CutOut is centered to that point and all listeners are noticed about this event
	 * @param	{event} e holds the Event which raised it
	 */
	function dblClick(e) {
		if (!e.layerX) {
			var newX = e.offsetX;
			var newY = e.offsetY;
		} else {
			var newX = e.layerX;
			var newY = e.layerY;
		}
		for(var i = 0; i < listener[cutOut.DBL_CLICK].length; i++) {
			listener[cutOut.DBL_CLICK][i]({'x':newX, 'y':newY}, viewID);
		}
		setPosition({'x':newX - getWidth("C")/2, 'y':newY - getHeight("C")/2}, e);
	}

	/**
	 * @public
	 * @function
	 * @name	mouseScroll
	 * @memberOf	iview.cutOut
	 * @description	on mouseScroll all Listeners are noticed about this Event
	 * @param	{event} e event which was caused by the mousescroll
	 */
	function mouseScroll(e) {
		var delta = returnDelta(e, true);
		if (!listener[cutOut.SCROLL]) {
			return;
		}
		for(var i = 0; i < listener[cutOut.SCROLL].length; i++) {
			listener[cutOut.SCROLL][i](delta, viewID);
		}
	}

	/**
	 * @public
	 * @function
	 * @name	mouseMove
	 * @memberOf	iview.cutOut
	 * @description	captures mouse movement and resets the Mouse CutOut Position when the Mouse is pressed
	 * @param	{event} e Event which occured
	 */
	function mouseMove(e) {
		if (mouseIsDown) {
			if(isBrowser(["IE"])){//IE
				var positionX = curx + (e.clientX - ausschnittPosX);
				var positionY = cury + (e.clientY - ausschnittPosY);
			} else {//Mozilla
				var positionX = curx + (e.pageX - ausschnittPosX);
				var positionY = cury + (e.pageY - ausschnittPosY);
			}			
			setPosition({'x':positionX, 'y':positionY}, e);
		}
	}

	/**
	 * @public
	 * @function
	 * @name	mouseDown
	 * @memberOf	iview.cutOut
	 * @description	stores the Position where the mouse was pressed so that on Mousemovement this constellation is kept, mouseIsDown is set to true	so that the other functions know the current mousestate
	 * @param	{event} e Event which occured
	 * @return	{boolean} false to prevent Browser Default Drag&Drop behave
	 */
	function mouseDown(e) {
		if (e.button < 2) {//nur bei linker und mittlerer Maustaste auf True setzen
			mouseIsDown = true;
			if(isBrowser(["IE"])){//IE
				ausschnittPosX = e.clientX;
				ausschnittPosY = e.clientY;
			} else {//Mozilla
				ausschnittPosX = e.pageX;
				ausschnittPosY = e.pageY;
			}
			//Bestimmen der aktuellen oberen Ecke, und Bewegungsvektor
			curx = parseInt(my.cutOut.style.left);
			cury = parseInt(my.cutOut.style.top);
		}
		return false;
	}

	/**
	 * @public
	 * @function
	 * @name	mouseUp
	 * @memberOf	iview.cutOut
	 * @description	releases the current Mousestate(mouseIsDown = false) so that no further movement of CutOut happen, although all MouseUp Listeners will be noticed about it
	 * @param	{event} e Event which occured
	 */
	function mouseUp(e) {
		if (mouseIsDown && e.button < 2) {
			if(isBrowser(["IE"])){//IE
				var positionX = curx + (e.clientX - ausschnittPosX);
				var positionY = cury + (e.clientY - ausschnittPosY);
			} else {//Mozilla
				var positionX = curx + (e.pageX - ausschnittPosX);
				var positionY = cury + (e.pageY - ausschnittPosY);
			}

			setPosBlock = true;
			
			if (positionX != curx || positionY != cury) {
				for(var i = 0; i < listener[cutOut.MOUSE_UP].length; i++) {
					listener[cutOut.MOUSE_UP][i]({'x':positionX, 'y':positionY}, viewID);
				}
			}
			mouseIsDown = false;
		}
	}

	/**
	 * @public
	 * @function
	 * @name	damper
	 * @memberOf	iview.cutOut
	 * @description	on A mouseClick Event the Thumbnail will be toggled in display state
	 * @param	{event} e event which occured
	 */
	function damper(e) {
		if (dampered) {
			jQuery(my.self).fadeOut();
			my.damp.getElementsByTagName("div")[0].style.display = "none";
			my.damp.getElementsByTagName("div")[1].style.display = "block"; 
			my.damp.title = "Ausklappen";
		} else {
			jQuery(my.self).fadeIn();
			my.damp.getElementsByTagName("div")[0].style.display = "block";
			my.damp.getElementsByTagName("div")[1].style.display = "none"; 
			my.damp.title = "Einklappen";

		}
		dampered = !dampered;
	}

	/**
	 * @public
	 * @function
	 * @name	addListener
	 * @memberOf	iview.cutOut
	 * @description	Adds a Listener for the given Event,so the Listener will be noticed if any changes will happen for this Event
	 * @param	{integer} type constant which defines to which type of listener it shall be added
	 * @param	{function} theListener Object which holds a special function which is called if a Event happens
	 */
	function addListener(type, theListener) {
		if (!listener[type]) {
			listener[type] = [];
		}
		listener[type].push(theListener);
	}

	/**
	 * @public
	 * @function
	 * @name	dropListener
	 * @memberOf	iview.cutOut
	 * @description	Dropps a Listener for the given Event
	 * @param	{integer} type constant which defines to which type of listener it shall be dropped
	 * @param	{function} theListener Object which holds a special function which is called if a Event happens
	 */
	function dropListener(type, theListener) {
		for (var i = 0; i < listener[type].length; i++) {
			if (listener[type][i] == theListener) {
				listener[type].splice(i,1);
			}
		}
	}

	/**
	 * @public
	 * @function
	 * @name	updateSize
	 * @memberOf	iview.cutOut
	 * @description	updates the size of the cutout
	 * @param	{float} ratioX the conversion factorto change the width
	 * @param	{float} ratioY the conversion factorto change the height
	 */
	function updateSize(ratioX, ratioY) {
	
		//convert size to the thumbnail
		curBreite = getWidth("T") * ratioX;
		curHoehe = getHeight("T") * ratioY;
		
		//prevent that the cutout will be bigger as the thumbnail
		if (curBreite > getWidth("T")) {
			setWidth(getWidth("T"));
		} else {
			setWidth(curBreite);
		}
		if (curHoehe > getHeight("T")) {
			setHeight(getHeight("T"));
		} else {
			setHeight(curHoehe);
		}
	}

	//TODO Doku ergänzen
	/**
	 * @public
	 * @function
	 * @name	updatePos
	 * @memberOf	iview.cutOut
	 * @description	updates the position of the cutout
	 * @param	{} newLeft 
	 * @param	{} newTop
	 */
	function updatePos(newLeft, newTop) {
		//determine the new left upper edgeposition
		if ((newLeft + getWidth("C")) > getWidth("T")) {
			newLeft = getWidth("T") - getWidth("C");
		} else if (newLeft < 0) {
			newLeft = 0;
		}
		if ((newTop + getHeight("C")) > getHeight("T")) {
			newTop = getHeight("T") - getHeight("C");
		} else if (newTop < 0) {
			newTop = 0;
		}
		
		setPosition({'x': newLeft, 'y':newTop});
	}
	
}
