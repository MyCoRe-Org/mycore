var length = null;
var focus = null;//holds the element which is focused
//TODO Preload größe anhand der von den Kacheln bestimmen
var listener = []; // array who holds informations about the listeners (?)
NAVIGATE = 0;

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

function notifyListenerNavigate(value, viewID) {
	if (!listener[NAVIGATE]) {
		return;
	}
	for(var i = 0; i < listener[NAVIGATE].length; i++) {
		listener[NAVIGATE][i].navi(value,viewID);
	}
};

/**
 * @public
 * @function
 * @name	setFocus
 * @memberOf	iview.Thumbnails
 * @description	sets the focus to the given element
 * @param 	{object} element object which should be focused
 */
function setFocus(element){
	focus = element;
}

/**
 * @public
 * @function
 * @name	getFocus
 * @memberOf	iview.Thumbnails
 * @description	returns the element which has the focus
 */
function getFocus(e){
	return focus;
}

/**
 * @public
 * @function
 * @name	resetFocus
 * @memberOf	iview.Thumbnails
 * @description	delete the focus so that no element has it
 */
function resetFocus(){
	focus = null;
}

/**
 * @public
 * @function
 * @name	loadPage
 * @memberOf	iview.Thumbnails
 * @description	reads out the imageinfo.xml, set the correct zoomvlues and loads the page
 * @param 	{string} viewID ID of the derivate
 * @param	{function} callback
 */
function loadPage(viewID, callback) {
	var url;
	if (typeof(Iview[viewID].newMETS)=='undefined'){
		url = Iview[viewID].startFile;
	} else {
		url = Iview[viewID].PhysicalModel.getCurrent().getHref();
	}
	Iview[viewID].prefix  = url;
	var imagePropertiesURL=Iview[viewID].baseUri[0]+"/"+viewID+"/"+url+"/imageinfo.xml";
	jQuery.ajax({
		url: imagePropertiesURL,
  		success: function(response) {processImageProperties(response,viewID)},
  		error: function(request, status, exception) {alert("Error occured while loading image properties:\n"+exception);},
  		complete: function() {callBack(callback)}
	});
}

/**
 * @public
 * @function
 * @name	callBack
 * @memberOf	iview.Thumbnails
 * @description	execute the given function, if it's no function, do nothing or alert a comment
 * @param 	{function} func is the function whis is to be executed
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
 * @name	processImageProperties
 * @memberOf	iview.Thumbnails
 * @description	
 * @param 	{object} imageProperties
 * @param	{string} viewID ID of the derivate
 */
function processImageProperties(imageProperties, viewID){
	var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
	
	Iview[viewID].tiles = parseInt(values['tiles']);
	Iview[viewID].bildBreite = parseInt(values['width']);
	Iview[viewID].bildHoehe = parseInt(values['height']);
	var viewerBean = Iview[viewID].viewerBean;

	if (viewerBean) {
		// checks if current zoomlevel was "greater" as zoomMax, so the current zoomLevel wouldn't reach
		// update the initialZoom only if this is not the case
		if (viewerBean.zoomLevel != Iview[viewID].zoomMax || Iview[viewID].initialZoom <= viewerBean.zoomLevel) {
			Iview[viewID].initialZoom = viewerBean.zoomLevel;
		}
		
		// checks for enabled Modi & reset before
		Iview[viewID].initialModus[0] = false;
		Iview[viewID].initialModus[1] = false;
		if (Iview[viewID].zoomWidth) {
			Iview[viewID].initialModus[0] = true;
			Iview[viewID].initialModus[1] = false;
		}
		if (Iview[viewID].zoomScreen) {
			Iview[viewID].initialModus[0] = false;
			Iview[viewID].initialModus[1] = true;
		}
	}

	Iview[viewID].zoomMax = parseInt(values['zoomLevel']);

	if (!isNaN(parseInt(window.location.search.get("zoom")))) {
		Iview[viewID].zoomInit = parseInt(window.location.search.get("zoom"));
		if (Iview[viewID].zoomInit > Iview[viewID].zoomMax)
			Iview[viewID].zoomInit = Iview[viewID].zoomMax;
		if (Iview[viewID].zoomInit < 0)
			Iview[viewID].zoomInit = 0;
	} else {
		// zoomLevel 0 ist erstes Level
		Iview[viewID].zoomInit = Math.ceil((Iview[viewID].zoomMax + 1) / 2) - 1;
	}
	var preLoadEl=document.getElementById("preload"+viewID);
	preLoadEl.style.width = Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - Iview[viewID].zoomInit) + "px";
	preLoadEl.style.height = Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - Iview[viewID].zoomInit) + "px";
	preLoadEl.removeChild(document.getElementById("preloadImg"+viewID));
	var preload = new Image();
	preload.id = "preloadImg" + viewID;
	preLoadEl.appendChild(preload);

	if (viewerBean == null) {
		initializeGraphic(viewID);
		viewerBean = Iview[viewID].viewerBean;
		viewerBean.addViewerZoomedListener(new listenerZoom(viewID));
		viewerBean.addViewerMovedListener(new listenerMove(viewID));
		preload.src = viewerBean.tileUrlProvider.assembleUrl(0,0,0);
	} else {
		// prevents that (max) Zoomlevel will be reached which doesn't exists
		if (Iview[viewID].initialZoom < Iview[viewID].zoomMax) {
			Iview[viewID].zoomInit = Iview[viewID].initialZoom;
		} else {
			Iview[viewID].zoomInit = Iview[viewID].zoomMax;
		}
		viewerBean.tileUrlProvider.prefix = Iview[viewID].prefix;
		preload.src = viewerBean.tileUrlProvider.assembleUrl(0,0,0);
		viewerBean.resize();
	}
	// moves viewer to zoomLevel zoomInit
	viewerBean.maxZoomLevel=Iview[viewID].zoomMax;
	// handle special Modi for new Page
	if (Iview[viewID].initialModus[0] == true) {
		// letzte Seite war in fitToWidth
		// aktuell ist fuer die neue Page noch kein Modi aktiv
		Iview[viewID].zoomWidth = false;
		pictureWidth(viewID);
	} else if (Iview[viewID].initialModus[1] == true) {
		// letzte Seite war in fitToScreen
		// aktuelle ist fuer die neue Page noch kein Modi aktiv
		Iview[viewID].zoomScreen = false;
		pictureScreen(viewID);
	} else {
		// moves viewer to zoomLevel zoomInit
		viewerBean.zoom(Iview[viewID].zoomInit - viewerBean.zoomLevel);
	}
	
	// damit das alte zoomBack bei Modi-Austritt nicht verwendet wird
	Iview[viewID].zoomBack = Iview[viewID].zoomInit;
	var initX = toFloat(window.location.search.get("x"));
	var initY = toFloat(window.location.search.get("y"));
	
	Iview[viewID].roller = true;
	viewerBean.positionTiles ({'x' : initX, 'y' : initY}, true);
	
	preload.style.width = "100%";
	preload.style.height = "100%";
	//$("preload"+viewID).style.visibility = "visible";
	if (Iview[viewID].useCutOut) {
		Iview[viewID].cutOutModel.setSrc(viewerBean.tileUrlProvider.assembleUrl(0,0,0));
	}
	updateModuls(viewID);
	
	Iview[viewID].roller = false;
}

/**
 * @public
 * @function
 * @name	openOverview
 * @memberOf	iview.Thumbnails
 * @description	blend in the overview and creates it by the first call
 * @param	{string} viewID ID of the derivate
 */
function openOverview(viewID) {
	Iview[viewID].overview.toggleView();
	openChapter(false, Iview[viewID]);
}

/**
 * @public
 * @function
 * @name	removeScaling
 * @memberOf	iview.Thumbnails
 * @description	saves the scaling of loaded tiles if picture fits to heigh or to width (for IE)
 * @param	{string} viewID ID of the derivate
 */
function removeScaling(viewID) {
	for (var img in Iview[viewID].images) {
		Iview[viewID].images[img]["scaled"] = false;
	}
}

/**
 * @public
 * @function
 * @name	isloaded
 * @memberOf	iview.Thumbnails
 * @description	checks if the picture is loaded
 * @param	{object} img
 * @param	{string} viewID ID of the derivate
 */
function isloaded(img, viewID) {
	/*
	NOTE tiles are not dispalyed correctly in Opera, because the used accuracy for pixelvalues only has 
	2 dezimal places, however 3 are neccessary for the correct representation as in FF
	*/
	if (!Iview[viewID].images[img.src]) {
		Iview[viewID].images[img.src] = new Object();
		Iview[viewID].images[img.src]["scaled"] = false;
		img.style.display = "none";
	}
	if (((img.naturalWidth == 0 && img.naturalHeight == 0)  && !isBrowser(["IE", "Opera"])) || (!img.complete && isBrowser(["IE", "Opera"]))) {
		if (img.src.indexOf("blank.gif") == -1) {//change
			window.setTimeout(function(image) { return function(){isloaded(image, viewID);} }(img), 100);
		}
	} else if (img.src.indexOf("blank.gif") == -1) {
		if (Iview[viewID].images[img.src]["scaled"] != true) {
			img.style.display = "inline";
			Iview[viewID].images[img.src]["scaled"] = true;//notice that this picture already was scaled
			//TODO math Floor rein bauen bei Höhe und Breite
			if (!isBrowser(["IE","Opera"])) {
				img.style.width = Iview[viewID].zoomScale * img.naturalWidth + "px";
				img.style.height = Iview[viewID].zoomScale * img.naturalHeight + "px";
			} else {
				if (!Iview[viewID].images[img.src]["once"]) {
					Iview[viewID].images[img.src]["once"] = true;
					Iview[viewID].images[img.src]["naturalheight"] = img.clientHeight;
					Iview[viewID].images[img.src]["naturalwidth"] = img.clientWidth;
				}
				img.style.width = Iview[viewID].zoomScale * Iview[viewID].images[img.src]["naturalwidth"] + "px";
				img.style.height = Iview[viewID].zoomScale * Iview[viewID].images[img.src]["naturalheight"] + "px";
			}
		}
	}
	img = null;
}

/**
 * @public
 * @function
 * @name	calculateZoomProp
 * @memberOf	iview.Thumbnails
 * @description	calculates how the TileSize and the zoomvalue needs to be if the given zoomlevel fits into the viewer
 * @param	{integer} level the zoomlevel which is used for testing
 * @param	{integer} totalSize the total size of the Picture Dimension X or Y
 * @param	{integer} viewerSize the Size of the Viewer Dimension X or Y
 * @param	{integer} scrollBarSize the Height or Width of the ScrollBar which needs to be dropped from the ViewerSize
 * @param	{string} viewID ID of the derivate
 * @return	boolean which tells if it was successfull to scale the picture in the current zoomlevel to the viewer Size
 */
function calculateZoomProp(level, totalSize, viewerSize, scrollBarSize, viewID) {
	if ((totalSize / Math.pow(2, level)) <= viewerSize) {
		var viewerBean = Iview[viewID].viewerBean;
		if (level != 0) {
			level--;
		}
		var currentWidth = totalSize / Math.pow(2, level);
		var viewerRatio = viewerSize / currentWidth;
		var fullTileCount = Math.floor( currentWidth / Iview[viewID].tilesize);
		var lastTileWidth = currentWidth - fullTileCount * Iview[viewID].tilesize;
		Iview[viewID].zoomScale = viewerRatio;//determine the scaling ratio
		level = Iview[viewID].zoomMax - level;
		viewerBean.tileSize = Math.floor((viewerSize - viewerRatio * lastTileWidth) / fullTileCount);
		Iview[viewID].zoomBack = viewerBean.zoomLevel;
		viewerBean.zoom(level - viewerBean.zoomLevel);
		if (Iview[viewID].useZoomBar) Iview[viewID].zoomBar.moveBarToLevel(level);
		return true;
	}
	return false;
}

/**
 * @public
 * @function
 * @name	switchDisplayMode
 * @memberOf	iview.Thumbnails
 * @description	calculates how the picture needs to be scaled so that it can be displayed within the display-area as the mode requires it
 * @param	{boolean} screenZoom defines which displaymode will be calculated
 * @param	{boolean} statebool holds the value which defines if the current mode is set or needs to be set
 * @param 	{boolean}arguments[3] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
 * @param	{string} viewID ID of the derivate
 * @return	boolean which holds the new StateBool value, so it can be saved back into the correct variable
 */
function switchDisplayMode(screenZoom, stateBool, viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	if (typeof(viewerBean)=='undefined' && typeof(console)!='undefined'){
		console.log("undefined property viewerBean");
		console.log(Iview[viewID]);
		console.trace();
		return;
	}
	if (screenZoom) {
		Iview[viewID].zoomWidth = false;
	} else {
		Iview[viewID].zoomScreen = false;
	}
	stateBool = (stateBool)? false: true;
	viewerBean.clear();
	removeScaling(viewID);
	var preload = document.getElementById("preload"+viewID);
	if (stateBool) {
		for (var i = 0; i <= Iview[viewID].zoomMax; i++) {
			if(Iview[viewID].bildBreite/viewerBean.width > Iview[viewID].bildHoehe/document.getElementById("viewer"+viewID).offsetHeight || (stateBool && !screenZoom)){
			//Width > Height Or ZoomWidth is true
				if (calculateZoomProp(i, Iview[viewID].bildBreite, viewerBean.width, 0, viewID)) {
					break;
				}
			} else {
				if (calculateZoomProp(i, Iview[viewID].bildHoehe, viewerBean.height, 0, viewID)) {
					break;
				}
			}
		}
		viewerBean.init();
	} else {
		Iview[viewID].zoomScale = 1;
		viewerBean.tileSize = /*Iview[viewID].*/tilesize;
		viewerBean.init();
		
		//an infinite loop would arise if the repeal of the zoombar comes
		if (typeof (arguments[3]) == "undefined" || arguments[3] == false) {
			viewerBean.zoom(Iview[viewID].zoomBack - viewerBean.zoomLevel);
			if (Iview[viewID].useZoomBar) Iview[viewID].zoomBar.moveBarToLevel(viewerBean.zoomLevel);
		}
	}

	Iview[viewID].barX.setCurValue(-parseInt(preload.offsetLeft));
	Iview[viewID].barY.setCurValue(-parseInt(preload.offsetTop));
	if (Iview[viewID].useCutOut) Iview[viewID].cutOutModel.setPos({'x':preload.offsetLeft, 'y':preload.offsetTop});
	return stateBool;
}

/**
 * @public
 * @function
 * @name	pictureWidth
 * @memberOf	iview.Thumbnails
 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerwidth which is smaller than the viewerwidth
 * @param	{string} viewID ID of the derivate
 */
function pictureWidth(viewID){
	var bool = (typeof (arguments[1]) != undefined)? arguments[1]:false;
	Iview[viewID].zoomWidth = switchDisplayMode(false, Iview[viewID].zoomWidth, viewID, bool);
}

/**
 * @public
 * @function
 * @name	pictureScreen
 * @memberOf	iview.Thumbnails
 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerspace which is smaller than the viewerspace
 * @param	{string} viewID ID of the derivate
 */
function pictureScreen(viewID){
	var bool = (typeof (arguments[1]) != undefined)? arguments[1]:false;
	Iview[viewID].zoomScreen = switchDisplayMode(true, Iview[viewID].zoomScreen, viewID, bool);
}

/**
 * @public
 * @function
 * @name	scrollMove
 * @memberOf	iview.Thumbnails
 * @description	loads the tiles accordingly the position of the scrollbar if they is moving
 * @param	{integer} valueX number of pixels how far the bar has been moved horizontal
 * @param	{integer} valueY number of pixels how far the bar has been moved vertical
 * @param	{string} viewID ID of the derivate
 */
function scrollMove(valueX, valueY, viewID) {
	Iview[viewID].scroller = true;
	Iview[viewID].viewerBean.positionTiles ({'x' : valueX, 'y' : valueY}, true);
	Iview[viewID].viewerBean.notifyViewerMoved({'x' : valueX, 'y' : valueY});
	Iview[viewID].scroller = false;
}

/**
 * @public
 * @function
 * @name	handleZoomScrollbars
 * @memberOf	iview.Thumbnails
 * @description	fit the scrollbar to the viewer-zoom
 * @param	{string} viewID ID of the derivate
 */
function handleZoomScrollbars(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	var barX = Iview[viewID].barX;
	var barY = Iview[viewID].barY;
	// determine the current imagesize
	var curBreite = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;
	var curHoehe = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;

	// horizontal
	// max scaling
	var xmaxVal = curBreite - viewerBean.width;
	barX.setMaxValue((xmaxVal < 0)? 0:xmaxVal);
	// current position
	barX.setCurValue(-viewerBean.x);
	// length of the bar
	var damp=document.getElementById("damp"+viewID);
	barX.setProportion(viewerBean.width/curBreite);
	// vertical
	var ymaxVal = curHoehe - viewerBean.height;
	barY.setMaxValue((ymaxVal < 0)? 0:ymaxVal);
	barY.setCurValue(-viewerBean.y);
	barY.setProportion(viewerBean.height/curHoehe);
}

/**
 * @public
 * @function
 * @name	handleResizeScrollbars
 * @memberOf	iview.Thumbnails
 * @description	fit the scrollbar to the viewer-resize
 * @param	{string} viewID ID of the derivate
 */
function handleResizeScrollbars(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	var barX = Iview[viewID].barX;
	var barY = Iview[viewID].barY;
	// determine the current imagesize
	var curBreite = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;
	var curHoehe = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;

	// vertical
	// max scaling
	var height = jQuery(viewerBean.viewer).height();
	var width = jQuery(viewerBean.viewer).width();
	var top = jQuery(viewerBean.viewer).offset().top;

	barY.setMaxValue(curHoehe - height);
	// size of the scrollbar
	barY.setSize(height - top);
	barY.my.self[0].style.top = top + "px";
	// length of the bar
	barY.setProportion(height/curHoehe);
	
	// horizontal
	barX.setMaxValue(curBreite - width);
	barX.setSize(width);
	barX.setProportion(width/curBreite);
}

/**
 * @public
 * @function
 * @name	listenerZoom
 * @memberOf	iview.Thumbnails
 * @description	is called if the viewer is zooming; handles the correct sizing and displaying of the preloadpicture, various buttons and positioning of the cutOut accordingly the zoomlevel
 * @param	{string} viewID ID of the derivate
 */
function listenerZoom(viewID) {
	this.viewID=viewID;
	this.viewerZoomed = function (zoomEvent) {
		var viewID = this.viewID;
		var viewerBean = Iview[viewID].viewerBean;
		
		// handle special Modes, needs to close
		if (Iview[viewID].zoomWidth) {
			pictureWidth(viewID, true);
		}
		if (Iview[viewID].zoomScreen) {
			pictureScreen(viewID, true);
		}
		var perLoadEl=document.getElementById("preload"+viewID);
		perLoadEl.style.width = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale +  "px";
		perLoadEl.style.height = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale + "px";
	
		// Actualize forward & backward Buttons
		jQuery(".viewerContainer.min .toolbars .toolbar").css("width", (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale +  "px");
	
		handleZoomScrollbars(viewID);

		if (Iview[viewID].useCutOut) {
			Iview[viewID].cutOutModel.setSize({
				'x': jQuery(perLoadEl).width(),
				'y': jQuery(perLoadEl).height()});
			Iview[viewID].cutOutModel.setRatio({
				'x': viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale),
				'y': viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)});
			Iview[viewID].cutOutModel.setPos({
				'x': - (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale,
				'y': - (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale});
		}
		
		// Buttons pürfen
		Iview[viewID].getToolbarCtrl().checkZoom(viewerBean.zoomLevel);
	}
}

/**
 * @public
 * @function
 * @name	listenerMove
 * @memberOf	iview.Thumbnails
 * @description	is calling if the picture is moving in the viewer and handles the size of the cutout accordingly the size of the picture
 * @param	{string} viewID ID of the derivate
 */
function listenerMove(viewID) {
	this.viewID=viewID;
	this.viewerMoved = function (event) {
		// calculate via zoomlevel to the preview the left top point
		var viewID = this.viewID;
		var newX = - (event.x / Math.pow(2, Iview[viewID].viewerBean.zoomLevel))/Iview[viewID].zoomScale;
		var newY = - (event.y / Math.pow(2, Iview[viewID].viewerBean.zoomLevel))/Iview[viewID].zoomScale;

		if (Iview[viewID].useCutOut) {
			Iview[viewID].cutOutModel.setPos({'x':newX, 'y':newY});
		}
		// set Roller that no circles are created, and we end in an endless loop
		Iview[viewID].roller = true;
		var preload=document.getElementById("preload"+viewID);
		Iview[viewID].barX.setCurValue(-parseInt(preload.offsetLeft));
		Iview[viewID].barY.setCurValue(-parseInt(preload.offsetTop));
		Iview[viewID].roller = false;
	}
}

/**
 * @public
 * @function
 * @name	openChapter
 * @memberOf	iview.Thumbnails
 * @description	open and close the chapterview
 * @param	{} major
 * @param	{Object} viewer of the derivate
 */
function openChapter(major, viewID){
	
	var viewer = Iview[viewID];
	
	if (chapterEmbedded) {
		//alert(warnings[0])
		return;
	}

	//TODO Positionierung klappt bei WebKit nicht, da die irgendwie CSS nicht einlesen durch Chapter einbau in Viewer kÃƒÂ¶nnte das behoben werden
	if (major) {
		// für major (Button) always reaction
		viewer.chapter.toggleView();
	} else {
/*		// nur dann einblenden, wenn es durch Modus ausgeblendet wurde
		if (Iview[viewID].chapterActive && !Iview[viewID].overviewActive && Iview[viewID].maximized && chapter.style.visibility == "hidden") {
			chapter.style.visibility = "visible";
			Iview[viewID].chapter1.showCurrentPageCenter(Iview[viewID].pagenumber);
			var chapterOut=document.getElementById("chapter1"+viewID+"Out");
			var chapterOut=document.getElementById("chapter1"+viewID+"In");
			blendings.slide("chapter1"+viewID,new Array(toFloat(getStyle(chapterOut, "left")) - toFloat(getStyle(chapterOut, "right")), toFloat(getStyle(chapterOut,"top")), toFloat(getStyle(chapterIn, "left")) - toFloat(getStyle(chapterIn, "right")), toFloat(getStyle(chapterIn, "top"))),5,5,0,new Array("chapter1"+viewID+":in"));
		} else if (Iview[viewID].chapterActive && !Iview[viewID].overviewActive && Iview[viewID].maximized && chapter.style.visibility == "visible") {
			// nothing to do
		} else if (chapter.style.visibility == "visible" && Iview[viewID].overviewActive) {
			// do nothing for Overview
		} else if (chapter.style.visibility == "visible") {
			//blendings.slide("chapter1"+viewID,new Array(toFloat(getStyle($("chapter1"+viewID+"In"), "left")) - toFloat(getStyle($("chapter1"+viewID+"In"), "right")), toFloat(getStyle($("chapter1"+viewID+"In"), "top")), toFloat(getStyle($("chapter1"+viewID+"Out"), "left")) - toFloat(getStyle($("chapter1"+viewID+"Out"), "right")), toFloat(getStyle($("chapter1"+viewID+"Out"), "top"))),60,10,0,new Array("chapter1"+viewID+":out"), "", "$('chapter1'+'"+viewID+"').style.visibility = 'hidden'");
			// chapter soll sofort weg sein, nicht erst noch blenden, bspw. wenn vom Vollbild zurück ins normale
			chapter.style.visibility = 'hidden';
			chapter.style.top = getStyle(document.getElementById("chapter1"+viewID+"Out"), "top");
		}
		// last possible case (bool=false & vis=hidden) only for major
		*/
	}
}

/**
 * @public
 * @function
 * @name	updateModules
 * @memberOf	iview.Thumbnails
 * @description	marks the correct picture in the chapterview and set zoombar to the correct zoomlevel
 * @param	{string} viewID ID of the derivate
 */
function updateModuls(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	// align/fit scrollbars
	handleZoomScrollbars(viewID);
	handleResizeScrollbars(viewID);

	// Actualize forward & backward Buttons
	var previewTbView = jQuery(Iview[viewID].getToolbarCtrl().getView("previewTbView").toolbar);
	var newTop = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
	if (Iview[viewID].viewerContainer.hasClass("viewerContainer min")) {
		Iview[viewID].viewerContainer.find(".toolbars .toolbar").css("top", newTop);
	}
	
	// Actualize Chapter
	if (Iview[viewID].useChapter && !(typeof Iview[viewID].chapter === "undefined")) {
		//prevent endless loop
		Iview[viewID].chapterReaction = true;
//		Iview[viewID].chapter._model.setSelected(Iview[viewID].PhysicalModel.getCurPos());//Iview[viewID].prefix);
	}

	// Actualize zoomBar
	if (Iview[viewID].useZoomBar) {
		Iview[viewID].zoomBar.setAmountLevel(Iview[viewID].zoomMax + 1);
		Iview[viewID].zoomBar.createScale();
		Iview[viewID].zoomBar.moveBarToLevel(Iview[viewID].viewerBean.zoomLevel);
	}
}

/**
 * @public
 * @function
 * @name	viewerScroll
 * @memberOf	iview.Thumbnails
 * @description	handles if the scrollbar was moved up or down and calls the functions to load the corresponding tiles and movement
 * @param 	{} delta
 * @param	{string} viewID ID of the derivate
 */
function viewerScroll(delta, viewID) {
	Iview[viewID].viewerBean.positionTiles({'x': delta.x*PanoJS.MOVE_THROTTLE,
											'y': delta.y*PanoJS.MOVE_THROTTLE}, true);
	Iview[viewID].viewerBean.notifyViewerMoved({'x': delta.x*PanoJS.MOVE_THROTTLE,
												'y': delta.y*PanoJS.MOVE_THROTTLE});
}

/**
 * @public
 * @function
 * @name	changeCSS
 * @memberOf	iview.Thumbnails
 * @description	determine the new css-name if the design is changing
 */
function changeCss() {
	curDesign = styleName;

	if (curDesign == "red") {
		curDesign = "standard";
	} else {
		curDesign = "red";
	}
	if (window.location.search.indexOf("&css=") != -1)  {
		window.location.search = window.location.search.substring(0,window.location.search.indexOf("&css=")) + "&css="+curDesign;
	} else {
		window.location.search += "&css="+curDesign;
	}
}

/**
 * @public
 * @function
 * @name	importZoomBar
 * @memberOf	iview.Thumbnails
 * @description	calls the corresponding functions to create the scrollbar
 * @param	{string} viewID ID of the derivate
 */
function importZoomBar(viewID) {
	// ZoomBar
	Iview[viewID].zoomBar = new zoomBar("zoomBar"+viewID, document.getElementById(Iview[viewID].zoomBarParent), "");
	var zoombar = Iview[viewID].zoomBar;
	
	zoombar.init(Iview[viewID].zoomBarHorz/*, Iview[viewID].useZoomBar*/);
	zoombar.setZoomLevelReference("Iview['"+viewID+"'].viewerBean.zoomLevel"); // Requires a String, not the current Value
	zoombar.setZoomFunction("Iview['"+viewID+"'].viewerBean.zoom");
	zoombar.setZoomDirection(Iview[viewID].zoomBarDirection);
	// Listener
	zoombar.addListener(zoomBar.DISP_MODE, new function() { this.change = function(width, screen) {
		Iview[viewID].zoomWidth = width;
		Iview[viewID].zoomScreen = screen;
	}});
	// additional Events
	ManageEvents.addEventListener(document.getElementById("viewer"+viewID), 'mouseup', zoombar.mouseUpZoombar, false);
	ManageEvents.addEventListener(document.getElementById("viewer"+viewID), 'mousemove', zoombar.mouseMoveZoombar, false);
}

/**
 * @public
 * @function
 * @name	importCutOut
 * @memberOf	iview.Thumbnails
 * @description	calls the corresponding functions to create the cutout
 * @param	{string} viewID ID of the derivate
 */
function importCutOut(viewID) {
	Iview[viewID].cutOutMP = new iview.cutOut.ModelProvider();
	Iview[viewID].cutOutModel = Iview[viewID].cutOutMP.createModel();
	Iview[viewID].ausschnitt = new iview.cutOut.Controller(Iview[viewID].cutOutMP);
	Iview[viewID].ausschnitt.createView({'thumbParent': Iview[viewID].ausschnittParent, 'dampParent': Iview[viewID].ausschnittParent});
	Iview[viewID].ausschnitt.attach(function(sender, args) {
		if (args.type == "move") {
			Iview[viewID].viewerBean.recenter(
					{'x' : args.x["new"]*Iview[viewID].zoomScale,
					 'y' : args.y["new"]*Iview[viewID].zoomScale
					}, true);
		}
	});
	var preload = document.getElementById("preload"+viewID);
	Iview[viewID].cutOutModel.setSize({
		'x': jQuery(preload).width(),
		'y': jQuery(preload).height()});
}

/**
 * @public
 * @function
 * @name		importChapter
 * @memberOf	iview.Thumbnails
 * @description	calls the corresponding functions to create the chapter
 * @param		{string} viewID ID of the derivate
 * @param		{function} callback function which is called just before the function returns
 */
function importChapter(viewID, callback) {
	var viewer = Iview[viewID];

	$LAB.script("chapter.js", "jquery.tree.min.js").wait(function() {
		viewer.ChapterModelProvider = new iview.METS.ChapterModelProvider(viewer.newMETS);
		
		viewer.chapter = new iview.chapter.Controller(viewer.ChapterModelProvider, viewer.PhysicalModelProvider);

		viewer.chapter.createView(viewer.chapterParent);
		viewer.chapterReaction = false;
		
		updateModuls(viewID);
		openChapter(true, viewID);
		callback();
	});
}

/**
 * @public
 * @function
 * @name		importOverview
 * @memberOf	iview.Thumbnails
 * @description	calls the corresponding functions to create the overview
 * @param		{string} viewID ID of the derivate
 * @param		{function} callback function which is called just before the function returns
 */
function importOverview(viewID, callback) {
	$LAB.script("overview.js").wait(function() {
		//overview loading
		var ov = new iview.overview.Controller(Iview[viewID].PhysicalModelProvider, iview.overview.View, Iview[viewID].viewerBean.tileUrlProvider);
		ov.createView({'mainClass':'overview', 'parent':"#viewerContainer"+viewID, 'useScrollBar':true});
		Iview[viewID].overview = ov;
		openOverview(viewID);
		callback();
	});
}

/**
 * @public
 * @function
 * @name	zoomViewer
 * @memberOf	iview.Thumbnails
 * @description	handels the direction of zooming in the viewer
 * @param 	{boolean} direction: true = zoom in, false = zoom out
 * @param	{string} viewID ID of the derivate
 */
function zoomViewer(viewID, direction) {
	if (direction) {
		// gesetzt den Fall, es wird vom obersten Level weiter gezoomt (falls Screen oder Width aktiv waren)
		if (Iview[viewID].viewerBean.zoomLevel == Iview[viewID].zoomMax) {
			Iview[viewID].viewerBean.zoom(0);
		} else {
			Iview[viewID].viewerBean.zoom(1);
		}
	} else {
		Iview[viewID].viewerBean.zoom(-1);
	}
	if(Iview[viewID].useZoomBar) {Iview[viewID].zoomBar.moveBarToLevel(Iview[viewID].viewerBean.zoomLevel)};
}

/**
 * @public
 * @function
 * @name	loading
 * @memberOf	iview.Thumbnails
 * @description	is calling to the load-event of the window; serve for the further registration of events likewise as initioator for various objects
 * @param	{string} viewID ID of the derivate
 */
function loading(viewID) {
	
	var cssSheet=document.getElementById("cssSheet"+viewID);
	if (cssSheet!=null){
		//Opera fix: link css style to head to fix maximizeHandler()
		cssSheet.parentNode.removeChild(cssSheet);
		document.getElementsByTagName("head")[0].appendChild(cssSheet);
	}
	
	Iview[viewID].startHeight = toInt(jQuery("#viewerContainer"+viewID).css("height"));
	Iview[viewID].startWidth = toInt(jQuery("#viewerContainer"+viewID).css("width"));
		
	style = styleFolderUri + styleName + "/";
	//retrieves the mets File depending on the fact if it's exists or it request a simple one

	// ScrollBars
	// horizontal
	Iview[viewID].barX = new iview.scrollbar.Controller();
	var barX = Iview[viewID].barX;
	barX.createView({ 'direction':'horizontal', 'parent':'#viewerContainer'+viewID, 'mainClass':'scroll'});
	barX._model.onevent.attach(function(sender, args) {
		if (args.type == "curVal" && !Iview[viewID].roller) {
			scrollMove(- (args["new"]-args["old"]), 0, viewID);
		}
	});
	// vertical
	Iview[viewID].barY = new iview.scrollbar.Controller();
	var barY = Iview[viewID].barY;
	barY.createView({ 'direction':'vertical', 'parent':'#viewerContainer'+viewID, 'mainClass':'scroll'});
	barY._model.onevent.attach(function(sender, args) {
		if (args.type == "curVal" && !Iview[viewID].roller) {
			scrollMove( 0, -(args["new"]-args["old"]), viewID);
		}
	});

	// Additional Events
	// register to scroll into the viewer
	ManageEvents.addEventListener(document.getElementById("viewer"+viewID), 'mouseScroll', function(e) { e = getEvent(e); preventDefault(e); viewerScroll(returnDelta(e), viewID);}, false);
	
	// damit viewer ueber scrollBarX endet, fortan in reinitialize
	document.getElementById("viewer"+viewID).style.width = Iview[viewID].startWidth - ((Iview[viewID].barX.my.self.css("visibility") == "visible")? Iview[viewID].barX.my.self.css("offsetWidth") : 0)  + "px";
	document.getElementById("viewer"+viewID).style.height = Iview[viewID].startHeight - ((Iview[viewID].barY.my.self.css("visibility") == "visible")? Iview[viewID].barY.my.self.css("offsetHeight") : 0)  + "px";
	if (Iview[viewID].useCutOut) {
		importCutOut(viewID);
	}

	// Load Page
	if (window.location.search.get("page") != "") {
		//TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
		Iview[viewID].startFile = window.location.search.get("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/,"§"); 
	}
	//remove leading '/'
	Iview[viewID].startFile = Iview[viewID].startFile.replace(/^\/*/,"");
	loadPage(viewID, function(){startFileLoaded(viewID)});
	
	
	
	
	
	// should be replaced while constructing MVC concept
	Iview[viewID].pictureScreen = function() {
		pictureScreen(viewID);
	}
	
	Iview[viewID].pictureWidth = function() {
		pictureWidth(viewID);
	}
	
	Iview[viewID].zoomViewer = function(direction) {
		zoomViewer(viewID, direction);
	}
	

	Iview[viewID].modules = new Object;

	Iview[viewID].modules.importOverview = function(callback) {
		importOverview(viewID, callback);
	}
	
	Iview[viewID].modules.openOverview = function() {
		openOverview(viewID);
	}

	Iview[viewID].modules.importChapter = function(callback) {
		importChapter(viewID, callback);
	}
	
	Iview[viewID].modules.openChapter = function(major) {
		openChapter(major, viewID);
	}
	
	
	Iview[viewID].maximizeHandler = function() {
		maximizeHandler(viewID);
	}
	
}

/**
 * @public
 * @function
 * @name	startFileLoaded
 * @memberOf	iview.Thumbnails
 * @description	
 * @param	{string} viewID ID of the derivate
 */
function startFileLoaded(viewID){
	Iview[viewID].loaded = true;
	// surface muss als Blank geladen werden, damit Ebene gefüllt und es im Vordergrund des Viewers liegt
	// hauptsächlich wegen IE notwendig
	getElementsByClassName("surface","viewer"+viewID,"div")[0].style.backgroundImage = "url("+Iview[viewID].webappBaseUri+"modules/iview2/web/gfx/blank.gif"+")";
	// PermaLink Handling

	// choice of zoomLevel or special zoomMode only makes sense in maximized viewer
	if (window.location.search.get("maximized") == "true") {
		if (window.location.search.get("tosize") == "width") {
			if (!Iview[viewID].zoomWidth) pictureWidth(viewID);
		} else if (window.location.search.get("tosize") == "screen") {
			if (!Iview[viewID].zoomScreen) pictureScreen(viewID);
		} else if (isNaN(parseInt(window.location.search.get("zoom")))){
			if (!Iview[viewID].zoomScreen) pictureScreen(viewID);
		}
		
		maximizeHandler(viewID);
	} else {
		// in minimized viewer always pictureScreen
		if (!Iview[viewID].zoomScreen) pictureScreen(viewID);
	}
	
	var newMetsURI = Iview[viewID].webappBaseUri + "servlets/MCRMETSServlet/" + viewID;
	jQuery.ajax({
		url: newMetsURI,
  		success: function(response) {
			processMETS(response,viewID);
		},
  		error: function(request, status, exception) {alert("Error Occured while Loading METS file:\n"+exception);}
	});
	
	// Resize-Events registrieren
	if (isBrowser("IE")) {
		window.onresize = function() {reinitializeGraphic(viewID)};
	} else {
		ManageEvents.addEventListener(window, 'resize', function() { reinitializeGraphic(viewID);}, false);
	}
	
	if (Iview[viewID].useZoomBar) {
		importZoomBar(viewID);
	}
	
	updateModuls(viewID);
	
}

/**
 * @public
 * @function
 * @name	processMETS
 * @memberOf	iview.Thumbnails
 * @description	process the loaded mets and do all final configurations like setting the pagenumber, generating Chapter and so on
 * @param	{document} metsDoc holds in METS/MODS structure all needed informations to generate an chapter and overview of of the supplied data
 * @param	{string} viewID ID of the derivate
 */
function processMETS(metsDoc, viewID) {
	Iview[viewID].newMETS = metsDoc;
	//create the PhysicalModelProvider
	Iview[viewID].PhysicalModelProvider = new iview.METS.PhysicalModelProvider(Iview[viewID].newMETS);
	Iview[viewID].PhysicalModel = Iview[viewID].PhysicalModelProvider.createModel();
	var physicalModel = Iview[viewID].PhysicalModel;
	Iview[viewID].amountPages = physicalModel.getNumberOfPages();
	physicalModel.setPosition(physicalModel.getPosition("phys_"+Iview[viewID].prefix));
	physicalModel.onevent.attach(function(sender, args) {
		if (args.type == physicalModel.SELECT) {
			notifyListenerNavigate(args["new"], viewID);
			loadPage(viewID);
			Iview[viewID].getToolbarCtrl().checkNavigation(args["new"]);
			updateModuls(viewID);
			if (jQuery('.navigateHandles .pageBox')[0]) {
				Iview[viewID].getToolbarCtrl().updateDropDown($(pagelist.find("a")[args["new"] - 1]).html());
			}
		}
	})

	// Toolbar Operation
	Iview[viewID].getToolbarCtrl().setState("overviewHandles", "openChapter", true);
	Iview[viewID].getToolbarCtrl().setState("overviewHandles", "openOverview", true);
	
	Iview[viewID].getToolbarCtrl().checkNavigation(Iview[viewID].PhysicalModel.getCurPos());

	//Generating of Toolbar List
	var it = physicalModel.iterator();
	var curItem = null;
	var pagelist = jQuery('<div id="pages" style="visibility: hidden; z-index: 80; position: absolute; left: -9999px;" class="hidden">');
	var ul = jQuery("<ul>");
	while (it.hasNext()) {
		curItem = it.next();
		if (curItem != null) {
			var orderLabel='[' + curItem.getOrder() + ']' + ((curItem.getOrderlabel().length > 0) ? ' - ' + curItem.getOrderlabel():'');  
			ul.append(jQuery('<li><a href="index.html#" id='+curItem.getID()+' class="'+orderLabel+'">'+orderLabel+'</a></li>'));
		}
	}
	pagelist.append(ul);
	Iview[viewID].viewerContainer.find(".toolbars").append(pagelist);

	// if METS File is loaded after the drop-down-menu (in mainToolbar) its content needs to be updated
	if (jQuery('.navigateHandles .pageBox')[0]) {
		Iview[viewID].getToolbarCtrl().getView('mainTbView').events.notify({'type' : "new", 'elementName' : "pageBox", 'parentName' : "navigateHandles", 'view' : Iview[viewID].viewerContainer.find('.navigateHandles .pageBox')});
		// switch to current content
		Iview[viewID].getToolbarCtrl().updateDropDown(jQuery(pagelist.find("a")[physicalModel.getCurPos() - 1]).html());
	}

//	if (Iview[viewID].useOverview) {
//		$LAB.script("overview.js").wait(function() {
//			importOverview(viewID);
//		});
//	}
}