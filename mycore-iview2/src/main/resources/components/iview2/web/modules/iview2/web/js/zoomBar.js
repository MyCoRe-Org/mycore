//TODO: aktuelles ZoomLevel innerhalb des Balken zeigen
function zoomBar(newId, parent, identer) {
	//self calculated
	var id = newId;
	var identer = identer;
	var blockSize = null;
	var scaleSize = null;
	var barPosGlo = null;
	var barPosLok = null;
	var barMove = null;
	var mouseOnBar = false;
	var barTopBorder = null;
	var barBottomBorder = null;
	// set by Main Class
	var horz = null; // true: horizontal, false: vertical
	var amountLevel = null;
	var zoomLevelRef = null;
	var zoomFunction = null;
	//var zoomDirection = 1; // '1' - zoomIn at zoomBar Start || '-1' - zoomOut at zoomBar Start
	var zoomDirection = true; // true zoomIn on Start || false zoomIn on End
//	var displayMode = new Array();
	
	// visual Cursor
	var zoomBarGrabCursor = (navigator.userAgent.search(/KHTML|Opera/i) >= 0 ? 'pointer' : (document.attachEvent ? 'url(grab.cur)' : '-moz-grab'));
	var zoomBarGrabbingCursor = PanoJS.GRABBING_MOUSE_CURSOR = (navigator.userAgent.search(/KHTML|Opera/i) >= 0 ? 'move' : (document.attachEvent ? 'url(grabbing.cur)' : '-moz-grabbing'));

	// Listener
	var listener = [];
	zoomBar.DISP_MODE = 0;
	

	//Function declarations:	
	this.init = init;
//	this.createScale = this.setAmountLevel = this.moveBarToLevel = this.mouseUpZoombar = this.mouseMoveZoombar = this.setZoomLevelReference = this.setZoomDirection = this.setZoomFunction = this.setDisplayMode = this.addListener = doNothing;
	this.createScale = createScale;
	this.moveBarToLevel = moveBarToLevel;
	this.mouseUpZoombar = mouseUpZoombar;
	this.mouseMoveZoombar = mouseMoveZoombar;
	
	this.setAmountLevel = setAmountLevel;
	this.setZoomLevelReference = setZoomLevelReference;
	this.setZoomDirection = setZoomDirection;
	this.setZoomFunction = setZoomFunction;
//	this.setDisplayMode = setDisplayMode;
	this.addListener = addListener;
	
	
	function addListener(type, theListener) {
		if (!listener[type]) {
			listener[type] = [];
		}
		listener[type].push(theListener);
	}

	function dropListener(type, theListener) {
		for (var i = 0; i < listener[type].length; i++) {
			if (listener[type][i] == theListener) {
				listener[type].splice(i,1);
			}
		}
	}

	function notifyListenerMode(value) {
		if (!listener[zoomBar.DISP_MODE]) {
			return;
		}
		for(var i = 0; i < listener[zoomBar.DISP_MODE].length; i++) {
			listener[zoomBar.DISP_MODE][i].change(value);
		}
	}
	
	
	
	function init(horizontal/*, active*/) {
	
		horz = horizontal;
	
		var main = document.createElement("div");
		main.id = id;
		main.className = "zoomBar" + identer;
		$(parent).appendChild(main);

		var startBehind = document.createElement("div");
		startBehind.className = "startBehind";
		startBehind.alt = "Vergroessern";
		startBehind.title = "Vergroessern";
		main.appendChild(startBehind);
		
		var start = document.createElement("div");
		start.className = "start";
		start.alt = "Vergroessern";
		start.title = "Vergroessern";
		getElementsByClassName("startBehind",id,"div")[0].appendChild(start);
		getElementsByClassName("start",id,"div")[0].onclick = function() {eval(zoomFunction+"("+((zoomDirection)? "1" : "-1")+")");moveBarToLevel(eval(zoomLevelRef));};

		var center = document.createElement("div");
		center.className = "center";
		main.appendChild(center);

		var endBehind = document.createElement("div");
		endBehind.className = "endBehind";
		endBehind.alt = "Verkleinern";
		endBehind.title = "Verkleinern";
		main.appendChild(endBehind);
		
		var end = document.createElement("div");
		end.className = "end";
		end.alt = "Verkleinern";
		end.title = "Verkleinern";
		getElementsByClassName("endBehind",id,"div")[0].appendChild(end);
		getElementsByClassName("end",id,"div")[0].onclick = function() {eval(zoomFunction+"("+((zoomDirection)? "-1" : "1")+")");moveBarToLevel(eval(zoomLevelRef));};

		var spaceStart = document.createElement("div");
		spaceStart.className = "spaceStart";
		getElementsByClassName("center",id,"div")[0].appendChild(spaceStart);
		
		var scale = document.createElement("div");
		scale.className = "scale";
		getElementsByClassName("center",id,"div")[0].appendChild(scale);
		
		var spaceEnd = document.createElement("div");
		spaceEnd.className = "spaceEnd";
		getElementsByClassName("center",id,"div")[0].appendChild(spaceEnd);

		var bar = document.createElement("div");
		bar.className = "bar";
		bar.onmousedown = function() {return false;};
		bar.style.cursor = zoomBarGrabCursor;
		getElementsByClassName("center",id,"div")[0].appendChild(bar);

		if (horz) {
			start.style.cssFloat = "left";
			start.style.styleFloat = "left";
			startBehind.style.cssFloat = "left";
			startBehind.style.styleFloat = "left";
			center.style.cssFloat = "left";
			center.style.styleFloat = "left";
			end.style.cssFloat = "left";
			end.style.styleFloat = "left";
			endBehind.style.cssFloat = "left";
			endBehind.style.styleFloat = "left";
			spaceStart.style.cssFloat = "left";
			spaceStart.style.styleFloat = "left";
			spaceEnd.style.cssFloat = "left";
			spaceEnd.style.styleFloat = "left";
			scale.style.cssFloat = "left";
			scale.style.styleFloat = "left";
		}

		//init Border's for Moving zoomBar
		if (horz) {
			barTopBorder = getElementsByClassName("scale",id,"div")[0].offsetLeft;
			barBottomBorder = getElementsByClassName("scale",id,"div")[0].offsetWidth + getElementsByClassName("scale",id,"div")[0].offsetLeft -
					  getElementsByClassName("bar",id,"div")[0].offsetWidth;
		} else {
			barTopBorder = getElementsByClassName("scale",id,"div")[0].offsetTop;
			barBottomBorder = getElementsByClassName("scale",id,"div")[0].offsetHeight + getElementsByClassName("scale",id,"div")[0].offsetTop -
					  getElementsByClassName("bar",id,"div")[0].offsetHeight;
		}

		ManageEvents.addEventListener(getElementsByClassName("bar",id,"div")[0], 'mousedown', mouseDownZoombar, false);
		ManageEvents.addEventListener(main, 'mouseup', mouseUpZoombar, false);
		ManageEvents.addEventListener(main, 'mousemove', mouseMoveZoombar, false);
		ManageEvents.addEventListener(main, 'mouseScroll', mouseScrollZoombar, false);
		ManageEvents.addEventListener(getElementsByClassName("scale",id,"div")[0], 'dblclick', mouseDbClick, false);
		
/*		if (active==true) {
			this.createScale = createScale;
			this.moveBarToLevel = moveBarToLevel;
			this.mouseUpZoombar = mouseUpZoombar;
			this.mouseMoveZoombar = mouseMoveZoombar;
			
			this.setAmountLevel = setAmountLevel;
			this.setZoomLevelReference = setZoomLevelReference;
			this.setZoomDirection = setZoomDirection;
			this.setZoomFunction = setZoomFunction;
			this.setDisplayMode = setDisplayMode;
			this.addListener = addListener;
		} else {
			main.style.display = "none";
		}*/
	}

	/*
	@description set amountLevel
	*/
	function setAmountLevel(amount) {
		amountLevel = amount;
	}

	/*
	@desciption returns height of the scalelines
	*/
	function getScaleHeight() {
		var tmpDiv = document.createElement("div");
		tmpDiv.className = "scaleLine";
		$(id).appendChild(tmpDiv);
		var x = parseInt(getElementsByClassName("scaleLine",id,"div")[0].offsetHeight);
		$(id).removeChild(tmpDiv);
		return x;
	}
	
	/*
	@desciption returns width of the scalelines
	*/
	function getScaleWidth() {
		var tmpDiv = document.createElement("div");
		tmpDiv.className = "scaleLine";
		$(id).appendChild(tmpDiv);
		var x = parseInt(getElementsByClassName("scaleLine",id,"div")[0].offsetWidth);
		$(id).removeChild(tmpDiv);
		return x;
	}

	/*
	@description creates the visual zoomlevellines on the zoombar, removes previously the old ones and then sets the new ones
	*/
	function createScale() {

		while (getElementsByClassName("scale",id,"div")[0].childNodes.length > 0) {
			getElementsByClassName("scale",id,"div")[0].removeChild(getElementsByClassName("scale",id,"div")[0].childNodes[0]);
		}

		if (horz) {
			scaleSize = getScaleWidth();
			blockSize = (getElementsByClassName("scale",id,"div")[0].offsetWidth - ((amountLevel - 1) * scaleSize)) / (amountLevel);
		} else {
			scaleSize = getScaleHeight();
			blockSize = (getElementsByClassName("scale",id,"div")[0].offsetHeight - ((amountLevel - 1) * scaleSize)) / (amountLevel);
		}
		
		var currentLine = 0;
		while (currentLine < amountLevel - 1) {
			var newLine = document.createElement("div");
			newLine.className = "scaleLine";
			var value = blockSize + (currentLine * (blockSize + scaleSize)) + "px";
			(horz)? newLine.style.left = value : newLine.style.top = value;
			getElementsByClassName("scale",id,"div")[0].appendChild(newLine);
			currentLine++;
		}
	}

	/*
	@description set zoombar to the correct position
	*/
	function moveBarToLevel (zoomLevel) {
	
		var targetZoom;
		if (zoomDirection) {
			targetZoom = amountLevel - 1 - zoomLevel;
		} else {
			targetZoom = zoomLevel;
		}
	
		if (horz) {				      
			getElementsByClassName("bar",id,"div")[0].style.left = (blockSize / 2) + (targetZoom * (blockSize + scaleSize)) -
				 			      (getElementsByClassName("bar",id,"div")[0].offsetWidth / 2) +
							      getElementsByClassName("scale",id,"div")[0].offsetLeft + "px";
		} else {
			getElementsByClassName("bar",id,"div")[0].style.top = (blockSize / 2) + (targetZoom * (blockSize + scaleSize)) -
				 			      (getElementsByClassName("bar",id,"div")[0].offsetHeight / 2) +
							      getElementsByClassName("scale",id,"div")[0].offsetTop + "px";
		}			

		// cancels the special zoommode

		// checks + - buttons
		if (zoomDirection) {
			var inClass = "start";
			var outClass = "end";
		} else {
			var inClass = "end";
			var outClass = "start";
		}
		if (eval(zoomLevelRef) == 0) {
			getElementsByClassName(outClass,id,"div")[0].style.display = "none";
			getElementsByClassName(inClass,id,"div")[0].style.display = "block";
		} else if (eval(zoomLevelRef) == amountLevel -1) {
			getElementsByClassName(outClass,id,"div")[0].style.display = "block";
			getElementsByClassName(inClass,id,"div")[0].style.display = "none";
		} else {
			getElementsByClassName(outClass,id,"div")[0].style.display = "block";
			getElementsByClassName(inClass,id,"div")[0].style.display = "block";
		}
	}

	/*
	@description stores the current Position where the mousebutton was pressed over the zoombar bar stored in barPosY, this value allows it to keep the bar under the mouse
	@param e event which was raised and which holds the needed position value
	*/
	function mouseDownZoombar(e) {
		if (e.button < 2) {
			mouseOnBar = true;
			if (isBrowser("IE")) { // IE
				(horz)? barPosGlo = e.clientX : barPosGlo = e.clientY;
			} else { // Firefox
				(horz)? barPosGlo = e.pageX : barPosGlo = e.pageY;
	 		}
			if (horz) {
				barPosLok = parseInt(getElementsByClassName("bar",id,"div")[0].style.left);
			} else {
				barPosLok = parseInt(getElementsByClassName("bar",id,"div")[0].style.top);
			}
						
			// otherwise barMove will stay at the last zoomlevel which was pressed via button
			barMove = barPosLok;

			// Cursor
			getElementsByClassName("bar",id,"div")[0].style.cursor = zoomBarGrabbingCursor;
		}
	}

	/*
	@description realizes that the zoombar bar follows the mouse, when mouseOnBar is set
	@param e event which holds the current mousemove informations
	*/
	function mouseMoveZoombar(e) {
		if (mouseOnBar) {

			if (isBrowser("IE")) { // IE
				(horz)? barMove = barPosLok + (e.clientX - barPosGlo) : barMove = barPosLok + (e.clientY - barPosGlo);
			} else { // Firefox
				(horz)? barMove = barPosLok + (e.pageX - barPosGlo) : barMove = barPosLok + (e.pageY - barPosGlo);
			}

			if (barMove < barTopBorder) {
				barMove = barTopBorder;
			} else if (barMove > barBottomBorder) {
				barMove = barBottomBorder;
			}

			if (horz) {
				getElementsByClassName("bar",id,"div")[0].style.left = barMove + "px";
			} else {
				getElementsByClassName("bar",id,"div")[0].style.top = barMove + "px";
			}
			
			
		}
	}

	/*
	@description is called as soon as the mousebutton goes up, calculates depending on the zoombar Bar which zoomlevel will be zoomed to as well as positions the bar correctly to this level
	@param e event which was raised by mouseup
	*/
	function mouseUpZoombar(e) {
		if(mouseOnBar) {
			mouseOnBar = false;
			var fits = false;
			var block = 0;

			while (!fits) {
			
				if (horz) {
					conditionValue = blockSize + block * (blockSize + scaleSize) + (scaleSize / 2) - 
					     			 barMove + getElementsByClassName("scale",id,"div")[0].offsetLeft - 
					     			 (getElementsByClassName("bar",id,"div")[0].offsetWidth / 2);
				} else {
					conditionValue = blockSize + block * (blockSize + scaleSize) + (scaleSize / 2) - 
					     			 barMove + getElementsByClassName("scale",id,"div")[0].offsetTop - 
					     			 (getElementsByClassName("bar",id,"div")[0].offsetHeight / 2);
				}
				
				if (conditionValue > 0) {
					fits = true;
					
					var targetZoom;
					if (zoomDirection) {
						targetZoom = amountLevel - 1 - block;
					} else {
						targetZoom = block;
					}
					
					eval(zoomFunction+"(targetZoom - eval(zoomLevelRef))");
					moveBarToLevel(targetZoom);
				}
				block++;
			}
			// Cursor
			getElementsByClassName("bar",id,"div")[0].style.cursor = zoomBarGrabCursor;	
		} 
	}

	/*
	@description gains the Scrollevent delta normalized it and depending on it's value it will be zoomed in or out one step
	@param e window (scroll)Event which occured, the mouse delta will be gained and normalized and is used to zoom depending on it
	*/
	function mouseScrollZoombar(e) {
		e = getEvent(e);
		var delta = returnDelta(e, true);
		if (delta != 0) {
			if (delta < 0) {
				eval(zoomFunction+"(-1)");
			} else {
				eval(zoomFunction+"(1)");				
			}
			moveBarToLevel(eval(zoomLevelRef));
		}
	}
	
	function mouseDbClick(e) {
		var pos = 0;
		var className = (e.srcElement)? e.srcElement.className:e.target.className;
		var x = (e.layerX)? e.layerX:e.offsetX;
		var y = (e.layerY)? e.layerY:e.offsetY;
		//scale
		if (className == "scale") {
			pos = (horz)? x:y;			
		} else if (className == "scaleLine") {//ScaleLine
			pos = (horz)? x+e.target.offsetLeft:y+e.target.offsetTop;
		}
		
	 	var level = 0;
	 	
	 	/* "start nicht mehr benötigt nach Umstellung auf layer und offset*/
		
	 	while (/*start + */level * (blockSize + scaleSize) - (scaleSize / 2) < pos) {
			level ++;
		}

		if (zoomDirection) {
			eval(zoomFunction+"((amountLevel - level) - eval(zoomLevelRef))");
		} else {
			eval(zoomFunction+"(level - eval(zoomLevelRef) - 1)");
		}
	 	moveBarToLevel(eval(zoomLevelRef));
	}
	
	function setZoomLevelReference(value) {
		zoomLevelRef = value;
	}
	
	function setZoomFunction(value) {
		zoomFunction = value;
	}
	
	function setZoomDirection(value) {
		zoomDirection = value;
	}
	
	function setDisplayMode(width, screen) {
		displayMode[0] = width;
		displayMode[1] = screen;
	}
}