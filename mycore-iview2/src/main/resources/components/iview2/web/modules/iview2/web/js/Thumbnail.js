var blendings = new blendWorks();
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

/*
@description set focus to the given element
*/
function setFocus(element){
	focus = element;
}

/*
@description gets the element which is focused
@return the element which has the focus
*/
function getFocus(e){
	return focus;
}

/*
@description return the focus (no element is focused after function-call)
*/
function resetFocus(){
	focus = null;
}

/*
@description procure informations from a given XML File
@param page numeric value who should be holded; if none is indicated tried to get one via URL
@param absolute inicator if the value shold be handled absolute or relative
*/
function loadPageData(getPage, absolute, viewID) {
	return nodeProps(Iview[viewID].buchDaten, "mets:file", getPage, absolute);
}

/*
@description reads out the imageinfo.xml, set the correct zoomvalues and loads the page
*/
//TODO Loadpagedata in loadpage integrieren
function loadPage(viewID, callback) {
	var pageData;
	if (typeof(Iview[viewID].buchDaten)=='undefined'){
		pageData=[{"LOCTYPE":"URL","href":Iview[viewID].startFile}];
	} else {
		pageData=loadPageData(Iview[viewID].pagenumber - 1, true, viewID);
	}
	Iview[viewID].prefix  = findInArrayElement(pageData, "LOCTYPE", "URL").href;
	var imagePropertiesURL=Iview[viewID].baseUri[0]+"/"+viewID+"/"+findInArrayElement(pageData, "LOCTYPE", "URL").href+"/imageinfo.xml";
	jQuery.ajax({
		url: imagePropertiesURL,
  		success: function(response) {processImageProperties(response,viewID)},
  		error: function(request, status, exception) {alert("Error occured while loading image properties:\n"+exception);},
  		complete: function() {callBack(callback)}
	});
}

function callBack(func){
	if (func == null)
		return;
	if (typeof(func)=='function')
		func();
	else
		alert("Is not a function:\n"+func);
}

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
	//viewerBean.zoom(Iview[viewID].zoomInit-viewerBean.zoomLevel);
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
	viewerBean.positionTiles ({'x' : initX, 'y' : initY}, true);
	preload.style.width = "100%";
	preload.style.height = "100%";
	//$("preload"+viewID).style.visibility = "visible";
	if (Iview[viewID].useCutOut) {
		Iview[viewID].ausschnitt.setSRC(viewerBean.tileUrlProvider.assembleUrl(0,0,0));
	}
	updateModuls(viewID);
}
/*
@description blend in the overview an creates it by the first call
*/
function openOverview(viewID) {
	if (!Iview[viewID].overviewActive) {
		Iview[viewID].overviewActive = !Iview[viewID].overviewActive;
		// update overview
		Iview[viewID].overview1.actualize(Iview[viewID].pagenumber);

		// im VollBild Header mit ausblenden
		var overview=document.getElementById("overview1"+viewID);
		var viewerContainer=document.getElementById("viewerContainer"+viewID);
		overview.style.top = - (viewerContainer.offsetHeight) + "px";
		overview.style.visibility = 'visible';
		blendings.slide("overview1"+viewID, new Array(0,- (viewerContainer.offsetHeight),0,0),5,5,0,new Array(), "", "");

		openChapter(false, viewID);
	} else {
		Iview[viewID].overviewActive = !Iview[viewID].overviewActive;
		
		// im VollBild Header wieder mit einblenden
		blendings.slide("overview1"+viewID, new Array(0,0,0,- (document.getElementById("viewerContainer" + viewID).offsetHeight)),5,5,0,new Array(), "", "document.getElementById('overview1'+'"+viewID+"').style.visibility = 'hidden'");	
	
		openChapter(false, viewID);
	}
}

/*
@description  saves the scaling of loaded tiles if picture fits to height or to width (for IE)
*/
function removeScaling(viewID) {
	for (var img in Iview[viewID].images) {
		Iview[viewID].images[img]["scaled"] = false;
	}
}

/*
@description checks if the picture is loaded
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

/*
@description calculates how the TileSize and the Zoomvalue needs to be if the given Zoomlevel fits into the Viewer
@param level the zoomlevel which is used for testing
@param totalSize the total size of the Picture Dimension X or Y
@param viewerSize the Size of the Viewer Dimension X or Y
@param scrollBarSize the Height or Width of the ScrollBar which needs to be dropped from the ViewerSize
@return boolean which tells if it was successfull to scale the picture in the current zoomlevel to the viewer Size
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

/*
@description calculates how the picture needs to be scaled so that it can be displayed within the display-area as the mode requires it
@param screenZoom boolean which defines which display mode will be calculated
@param stateBool boolean which holds the value which defines if the current mode is set or needs to be set.
@param arguments[3] boolean optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
@return boolean which holds the new StateBool value, so it can be saved back into the correct variable
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
				//Siehe TODO oben
				if (calculateZoomProp(i, Iview[viewID].bildBreite, viewerBean.width, /*toInt(getStyle("scrollV"+viewID, "width"))*/0, viewID)) {
					break;
				}
			} else {
				if (calculateZoomProp(i, Iview[viewID].bildHoehe, viewerBean.height, /*toInt(getStyle("scrollH"+viewID, "height"))*/0, viewID)) {
					break;
				}
			}
		}
		viewerBean.init();//TODO not working here
		// zoomIn-Button einblenden, da min ein "groesseres" ZoomLevel existiert, von dem aus runterskaliert wurde
		if (classIsUsed("BSE_zoomIn")) doForEachInClass("BSE_zoomIn", ".style.display = 'block';", viewID);
		
		if (getElementsByClassName("buttonSurface min", document.getElementById("viewerContainer"+viewID), "div")[0]) {
			getElementsByClassName("buttonSurface min", document.getElementById("viewerContainer"+viewID), "div")[0].style.width = preload.offsetWidth + "px";
		}
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
	//TODO zu machen?
	if (Iview[viewID].useCutOut) Iview[viewID].ausschnitt.setPosition({'x':preload.offsetLeft, 'y':preload.offsetTop});
	return stateBool;
}
/*
@description calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerwidth which is smaller than the viewerwidth
*/
function pictureWidth(viewID){
	var bool = (typeof (arguments[1]) != undefined)? arguments[1]:false;
	Iview[viewID].zoomWidth = switchDisplayMode(false, Iview[viewID].zoomWidth, viewID, bool);
}

/*
@description calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerspace which is smaller than the viewerspace
*/
function pictureScreen(viewID){
	var bool = (typeof (arguments[1]) != undefined)? arguments[1]:false;
	Iview[viewID].zoomScreen = switchDisplayMode(true, Iview[viewID].zoomScreen, viewID, bool);
}

/*
@description loads the tiles accordingly the position of the scrollbar if they is moving
*/
function scrollMove(valueX, valueY, viewID) {
	Iview[viewID].scroller = true;
	Iview[viewID].viewerBean.positionTiles ({'x' : valueX, 'y' : valueY}, true);
	Iview[viewID].viewerBean.notifyViewerMoved({'x' : valueX, 'y' : valueY});
	Iview[viewID].scroller = false;
}

/*
@description fit the scrollbar to the viewer-zoom
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

/*
@description fit the scrollbar to the viewer-resize
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
	barY.setMaxValue(curHoehe - viewerBean.height);
	// size of the scrollbar
	barY.setSize(viewerBean.height);
	// length of the bar
	barY.setProportion(viewerBean.height/curHoehe);
	
	// horizontal
	barX.setMaxValue(curBreite - viewerBean.width);
	barX.setSize(viewerBean.width);
	barX.setProportion(viewerBean.width/curBreite)
}

/*
@description is called if viewer is zooming; handles the correct sizing and displaying of the preloadpicture,
various buttons and positioning of the cutOut accordingly the zoomlevel
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
	
		handleZoomScrollbars(viewID);

		if (Iview[viewID].useCutOut) {
			Iview[viewID].ausschnitt.updateSize((viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)), (viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)));
			Iview[viewID].ausschnitt.updatePos((- (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale), (- (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale));
		}
		// zoomOut prüfen
		if (viewerBean.zoomLevel == 0) {
			if (classIsUsed("BSE_zoomOut")) doForEachInClass("BSE_zoomOut", ".style.display = 'none';", viewID);
		} else {
			if (classIsUsed("BSE_zoomOut")) doForEachInClass("BSE_zoomOut", ".style.display = 'block';", viewID);
		}
		// zoomIn prüfen
		if (viewerBean.zoomLevel == Iview[viewID].zoomMax) {
			if (classIsUsed("BSE_zoomIn")) doForEachInClass("BSE_zoomIn", ".style.display = 'none';", viewID);
		} else {
			if (classIsUsed("BSE_zoomIn")) doForEachInClass("BSE_zoomIn", ".style.display = 'block';", viewID);
		}
	}
}

/*
@description is calling if the picture is moving in the viewer and handles the size of the cutout accordingly the size of the picture
*/
function listenerMove(viewID) {
	this.viewID=viewID;
	this.viewerMoved = function (event) {
		// calculate via zoomlevel to the preview the left top point
		var viewID = this.viewID;
		var newX = - (event.x / Math.pow(2, Iview[viewID].viewerBean.zoomLevel))/Iview[viewID].zoomScale;
		var newY = - (event.y / Math.pow(2, Iview[viewID].viewerBean.zoomLevel))/Iview[viewID].zoomScale;

		if (Iview[viewID].useCutOut) {
			Iview[viewID].ausschnitt.setPosition({'x':newX, 'y':newY});
		}
		// set Roller that no circles are created, and we end in an endless loop
		Iview[viewID].roller = true;
		var preload=document.getElementById("preload"+viewID);
		Iview[viewID].barX.setCurValue(-parseInt(preload.offsetLeft));
		Iview[viewID].barY.setCurValue(-parseInt(preload.offsetTop));
		Iview[viewID].roller = false;
	}
}

/*
 @description Generates for the calling BSE_permalink Objects BSE_url Sibling the Permalink url and shows the BSE_url afterwards, if not already displayed
 For a BSE_permalink Object it's enough to call the function with displayURL(this)  
 @param element BSE_permalink DOM Object which BSE_url sibling shall be filled with the Permalink and displayed afterwards
 */
function displayURL(element, viewID) {	
	if (classIsUsed('BSE_url')) {
		getElementsByClassName("BSE_permaUrl", element.parentNode)[0].value = generateURL(viewID);
		getElementsByClassName("BSE_url", element.parentNode)[0].style.display = "block";
	}
}

/*
  @description Hides the BSE_url Sibling of the calling BSE_permalink DOM Object
  For a BSE_permalink Object it's enough to call the function with hideURL(this)
  @param element BSE_permalink DOM Object which BSE_url sibling shall be hidden
 */
function hideURL(element) {
	if (classIsUsed('BSE_url')) {
		element.parentNode.style.display = "none";	
	}
}
/*
@description Function generates a permalink which contains all needed informations to display the same Picture&Position and other things
@return string which contains the generated URL
*/
function generateURL(viewID) {
	var url = window.location.href.substring(0, ((window.location.href.indexOf("?") != -1)? window.location.href.indexOf(window.location.search): window.location.href.length))+ "?";
	url += "&page="+Iview[viewID].prefix;
	url += "&zoom="+Iview[viewID].viewerBean.zoomLevel;
	url += "&x="+Iview[viewID].viewerBean.x;
	url += "&y="+Iview[viewID].viewerBean.y;
	var size = "none";
	if (Iview[viewID].zoomWidth)
		size = "width";
	if (Iview[viewID].zoomScreen)
		size = "screen";
	url += "&tosize="+size;
	url += "&maximized="+Iview[viewID].maximized;
	url += "&css="+styleName;
	return url;
}

function openChapterAndInitialize(major, viewID, button){
	if (typeof Iview[viewID].chapter === 'undefined'){
		var oldClassName=button.className;
		button.className+=" loading";
		var start=new Date().getTime();
		setTimeout(function(){
			importChapter(viewID);
			updateModuls(viewID);
			var end=new Date().getTime() - start;
			button.className=oldClassName;
			if (typeof(console)!='undefined'){
				var msg="import chapters took "+end+"ms"
				console.log(msg);
			}
			openChapter(major, viewID);
		}, 50);
		return;
	} else {
		openChapter(major, viewID);
	}
}

/*
@description open and close the chapterview
*/
function openChapter(major, viewID){
	if (chapterEmbedded) {
		//alert(warnings[0])
		return;
	}

	//TODO Positionierung klappt bei WebKit nicht, da die irgendwie CSS nicht einlesen durch Chapter einbau in Viewer kÃƒÂ¶nnte das behoben werden
	if (major) {
		// für major (Button) always reaction
		if (!Iview[viewID].chapterActive) {
			Iview[viewID].chapterActive = true;
			Iview[viewID].chapter.showView();
		} else {
			Iview[viewID].chapterActive = false;
			Iview[viewID].chapter.hideView();
		}
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

/*
@description marks the correct picture in the chapterview and set zoombar to the correct zommlevel
*/
function updateModuls(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	// align/fit scrollbars
	handleZoomScrollbars(viewID);
	handleResizeScrollbars(viewID);

	// Actualize forward & backward Buttons
	getElementsByClassName("BSE_forwardBehind "+viewID, "viewerContainer"+viewID, "div")[0].style.top = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - toInt(getStyle(getElementsByClassName("BSE_forwardBehind "+viewID, "viewerContainer"+viewID, "div")[0],"height"))) / 2) + "px";
	getElementsByClassName("BSE_backwardBehind "+viewID, "viewerContainer"+viewID, "div")[0].style.top = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - toInt(getStyle(getElementsByClassName("BSE_backwardBehind "+viewID, "viewerContainer"+viewID, "div")[0],"height"))) / 2) + "px";
	
	// Actualize Chapter
	if (Iview[viewID].useChapter && !(typeof Iview[viewID].chapter === "undefined")) {
		//prevent endless loop
		Iview[viewID].chapterReaction = true;
		Iview[viewID].chapter._model.setSelected(Iview[viewID].prefix);

	}

	// Actualize zoomBar
	if (Iview[viewID].useZoomBar) {
		Iview[viewID].zoomBar.setAmountLevel(Iview[viewID].zoomMax + 1);
		Iview[viewID].zoomBar.createScale();
		Iview[viewID].zoomBar.moveBarToLevel(Iview[viewID].viewerBean.zoomLevel);
	}
}

/*
@description handles if the scrollbar was moved up or down and calls the functions to load the corresponding tiles and movement
*/
function viewerScroll(delta, viewID) {
	Iview[viewID].viewerBean.positionTiles({'x': delta.x*PanoJS.MOVE_THROTTLE,
											'y': delta.y*PanoJS.MOVE_THROTTLE}, true);
	Iview[viewID].viewerBean.notifyViewerMoved({'x': delta.x*PanoJS.MOVE_THROTTLE,
												'y': delta.y*PanoJS.MOVE_THROTTLE});
}

/*
@description determine the new css-name if the design is changing
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

/*
@description calls the corresponding functions to create the scrollbar
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

/*
@description calls the corresponding functions to create the cutout
*/
function importCutOut(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	// CutOut
	Iview[viewID].ausschnitt = new cutOut(Iview[viewID]);
	var ausschnitt = Iview[viewID].ausschnitt;
	ausschnitt.setViewID(viewID);
	ausschnitt.init("thumb"+viewID, "ausschnitt"+viewID, "thumbnail"+viewID, Iview[viewID].ausschnittParent, "damp"+viewID, Iview[viewID].ausschnittParent/*"viewer"+viewID*/, "Normal");
	// Additional Listener
	// also in zoom-Listener, but to early without onload
	ausschnitt.addListener(cutOut.ONLOAD, function(viewID) {
			var viewerBean = Iview[viewID].viewerBean;
			ausschnitt.updateSize((viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)), (viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)));
			ausschnitt.updatePos((- (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale), (- (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale));
	});
	ausschnitt.addListener(cutOut.DBL_CLICK, function(vector, viewID) {
			Iview[viewID].viewerBean.recenter(
			{'x' : ((vector.x)*Math.pow(2, Iview[viewID].viewerBean.zoomLevel))*Iview[viewID].zoomScale ,
			 'y' : ((vector.y)*Math.pow(2, Iview[viewID].viewerBean.zoomLevel))*Iview[viewID].zoomScale
		}, true);});
	// calculate the position on the image and center new, according to the middle of the cutout and the zoomlevel
	ausschnitt.addListener(cutOut.MOUSE_UP, function(vector, viewID) {
				Iview[viewID].viewerBean.recenter(
				{'x' : (Iview[viewID].ausschnitt.getWidth()/2 + Iview[viewID].ausschnitt.getPosition().x)*Math.pow(2, Iview[viewID].viewerBean.zoomLevel)*Iview[viewID].zoomScale,
				 'y' : (Iview[viewID].ausschnitt.getHeight()/2 + Iview[viewID].ausschnitt.getPosition().y)*Math.pow(2, Iview[viewID].viewerBean.zoomLevel)*Iview[viewID].zoomScale
				}, true);
				});
	// Additional Events
	ManageEvents.addEventListener(document, 'mouseup', ausschnitt.mouseUp, false);
	ManageEvents.addEventListener(document, 'mousemove', ausschnitt.mouseMove, false);
	// wird in Klasse für cutOut bzw. in loading für viewer gemacht
	// ManageEvents.addEventListener($("viewer"+viewID), 'mouseScroll', ausschnitt.scroll, false);
}

/*
@description calls the corresponding functions to create the chapter
*/
function importChapter(viewID) {
	Iview[viewID].chapModelProvider = new iview.chapter.ModelProvider(Iview[viewID].buchDaten);
	var chapView = new iview.chapter.View();
	
	Iview[viewID].chapter = new iview.chapter.Controller(Iview[viewID].chapModelProvider, chapView);
	
	//Create Listener which changes after a Page click all needed informations within Viewer
	Iview[viewID].chapModelProvider.createModel().onevent.attach(function() {
		if (Iview[viewID].chapterReaction) {
			Iview[viewID].chapterReaction = false;
			return;
		}
		Iview[viewID].chapterReaction = true;
		Iview[viewID].prefix = arguments[1]["new"];
		getPageNumberFromPic(viewID);
		navigatePage(Iview[viewID].pagenumber, viewID);
	});
	Iview[viewID].chapter.createView("#viewerContainer"+viewID);
	Iview[viewID].chapterReaction = false;
}

/*
@description calls the corresponding functions to create the overview
*/
function importOverview(viewID) {
	Iview[viewID].overview1 = new overview("overview1"+viewID, "viewerContainer"+viewID, "");
	var overview1 = Iview[viewID].overview1;
	overview1.setViewID(viewID);
	//TODO wird hier lediglich Referenz gespeichert? Wenn ja ok ansonsten evtl unnötigen Kram entfernen und nur die Seiteninfos speichern
	overview1.setBook(Iview[viewID].buchDaten)
	overview1.setBaseUri(Iview[viewID].baseUri);
	overview1.init();
	overview1.setNumberOfPages(Iview[viewID].amountPages);

	overview1.addListener(overview.PAGE_NUMBER, new function() { this.click = function(value) {
			//Iview[viewID].pagenumber = value;
			openOverview(viewID);
			navigatePage(value, viewID);
		}});
	// should blend in via effect
	document.getElementById("overview1"+viewID).style.visibility = "hidden";
}

function importPageInput(viewID, parentID) {
	if (!classIsUsed("BSE_pageInput1")) {
		Iview[viewID].pageInputObj = new pageInput("BSE_pageInput"+viewID, parentID, "BSE_pageInput");
		var pageInputObj = Iview[viewID].pageInputObj;
		pageInputObj.setViewID(viewID);
		pageInputObj.init();
		pageInputObj.addListener(pageInput.PAGE_NUMBER, new function() {
			this.change = function(value) {
				navigatePage(value, viewID);
			}
		});
	} else {
		Iview[viewID].pageInputObj.initNext();
	}
}

function importPageForm(viewID, parentID) {
	//if (!classIsUsed("BSE_pageForm1")) {
		Iview[viewID].pageFormObj = new pageForm("BSE_pageForm"+viewID, parentID, "BSE_pageForm");
		var pageFormObj = Iview[viewID].pageFormObj;
		pageFormObj.setViewID(viewID);
		pageFormObj.init();
		pageFormObj.addListener(pageForm.PAGE_NUMBER, new function() {
			this.change = function(value) {
	    		navigatePage(value, viewID);
			}
		});
	/*} else {
		Iview[viewID].pageFormObj.initNext();
	}*/
}

/*
@description creates for the Styling or whatever Reason Objects within the header, which hold the given String as ClassName.
The data is taken from Iview[viewID].headerObjects
@param viewID the ID of the Viewer Header to apply
*/
function splitHeader(viewID) {
	var createElements = Iview[viewID].headerObjects.split(",");
	var element = null;
	for (var i = 0; i < createElements.length;i++) {
		element = document.createElement("div");
		element.className = createElements[i];
		document.getElementById("header"+viewID).appendChild(element);
	}
}

/*
@description After Loading the first Page this function is called to gain the currently displayed PageNumber so all other
 Elements are initialized correctly as well
 */
function getPageNumberFromPic(viewID) {
	var files = Iview[viewID].buchDaten.getElementsByTagName(namespaceCheck("mets:file"));
	for (var i = 0; i < files.length;i++) {
		if (files[i].attributes.getNamedItem("ID").value == Iview[viewID].prefix) {
			var nodes = files[i].getElementsByTagName(namespaceCheck("mets:FLocat"));
			for (var j = 0; j < nodes.length; j++) {
				if (nodes[j].attributes.getNamedItem("LOCTYPE").value.toLowerCase() == "other" && nodes[j].attributes.getNamedItem("OTHERLOCTYPE") != null && nodes[j].attributes.getNamedItem("OTHERLOCTYPE").value.toLowerCase() == "pagenumber") {
					var ref = isBrowser(["Opera","Firefox/2"])? "href":"xlink:href";
					Iview[viewID].pagenumber = parseInt(nodes[j].attributes.getNamedItem(ref).value)+1;
					break;
				}
			}
		}
	}
}
// direction: true = in, false = out
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
/*
@description is calling to the load-event of the window; serve for the further registration of events likewise as initioator for various objects
@param e Daten vom Load Event
*/
function loading(viewID) {
	//TODO Viewer nimmt wieder 100% des ViewerContainers eins....muss behoben werden
	Iview[viewID].chapterActive = false;
	Iview[viewID].overviewActive = false;
	var cssSheet=document.getElementById("cssSheet"+viewID);
	if (cssSheet!=null){
		//Opera fix: link css style to head to fix maximizeHandler()
		cssSheet.parentNode.removeChild(cssSheet);
		document.getElementsByTagName("head")[0].appendChild(cssSheet);
	}
	
	Iview[viewID].startHeight = toInt(jQuery("#viewerContainer"+viewID).css("height"));
	Iview[viewID].startWidth = toInt(jQuery("#viewerContainer"+viewID).css("width"));
	
	//Create new Header Elements as specified within caller xsl
	splitHeader(viewID);
	
	style = styleFolderUri + styleName + "/";
	//retrieves the mets File depending on the fact if it's exists or it request a simple one

	blendings.useEffects(blendEffects);

	// ScrollBars
	// horizontal
	Iview[viewID].barX = new iview.scrollbar.Controller();
	var barX = Iview[viewID].barX;
	barX.createView({ 'direction':'horizontal', 'id':'scrollH'+viewID,'parent':'#viewerContainer'+viewID, 'mainClass':'scroll'});
	barX._model.onevent.attach(function(sender, args) {
		if (args.type == "curVal" && !Iview[viewID].roller) {
			scrollMove(- (args["new"]-args["old"]), 0, viewID);
		}
	});
	// vertical
	Iview[viewID].barY = new iview.scrollbar.Controller();
	var barY = Iview[viewID].barY;
	barY.createView({ 'direction':'vertical', 'id':'scrollV'+viewID,'parent':'#viewerContainer'+viewID, 'mainClass':'scroll'});
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
}

function startFileLoaded(viewID){
	Iview[viewID].loaded = true;

	// surface muss als Blank geladen werden, damit Ebene gefüllt und es im Vordergrund des Viewers liegt
	// hauptsächlich wegen IE notwendig
	getElementsByClassName("surface","viewer"+viewID,"div")[0].style.backgroundImage = "url("+Iview[viewID].webappBaseUri+"modules/iview2/web/gfx/blank.gif"+")";
	// PermaLink Handling
	
	if (window.location.search.get("tosize") == "width") {
		pictureWidth(viewID);
	} else if (window.location.search.get("tosize") == "screen") {
		pictureScreen(viewID);
	} else if (isNaN(parseInt(window.location.search.get("zoom")))){
		pictureScreen(viewID);
	}
	if (window.location.search.get("maximized") == "true") {
		maximizeHandler(viewID);
	}
	
	// nochmals notwendig, da Scale erst hier verf�gbar f�r erstes Bild
	// Actualize forward & backward Buttons
	getElementsByClassName("BSE_forwardBehind "+viewID, "viewerContainer"+viewID, "div")[0].style.top = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - toInt(getStyle(getElementsByClassName("BSE_forwardBehind "+viewID, "viewerContainer"+viewID, "div")[0],"height"))) / 2) + "px";
	getElementsByClassName("BSE_backwardBehind "+viewID, "viewerContainer"+viewID, "div")[0].style.top = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - toInt(getStyle(getElementsByClassName("BSE_backwardBehind "+viewID, "viewerContainer"+viewID, "div")[0],"height"))) / 2) + "px";
	
	var metsURI = Iview[viewID].webappBaseUri + "servlets/" + ((Iview[viewID].hasMets)? "MCRFileNodeServlet/": "MCRDirectoryXMLServlet/") + viewID + ((Iview[viewID].hasMets)? "/mets.xml":"?XSL.Style=mets");
	jQuery.ajax({
		url: metsURI,
  		success: function(response) {processMETS(response,viewID)},
  		error: function(request, status, exception) {alert("Error Occured while Loading METS file:\n"+exception);}
	});
	
	// Resize-Events registrieren
	if (isBrowser("IE")) {
		document.getElementById("viewer"+viewID).parentNode.onresize = function() {reinitializeGraphic(viewID)};
	} else {
		ManageEvents.addEventListener(window, 'resize', function() { reinitializeGraphic(viewID);}, false);
	}
	
	if (Iview[viewID].useZoomBar) {
		importZoomBar(viewID);
	}
	
	updateModuls(viewID);
	
}

/*
 @description process the loaded mets and do all final configurations like setting the pagenumber, generating Chapter and so on
 @param metsDoc Document which holds in METS/MODS structure all needed informations to generate an chapter and overview of of the supplied data
 */
function processMETS(metsDoc, viewID) {
	Iview[viewID].buchDaten = metsDoc;
	Iview[viewID].amountPages = getPageCount(Iview[viewID].buchDaten);
	
	getPageNumberFromPic(viewID);
	
	// additional loadings - preloads
	//Edge-Element
	//TODO: müsste beim import des CutOut entsprechend gesetzt werden, oder ähnlich
	//createAbsoluteObject("div", "dampEmpty"+viewID, "viewer"+viewID);
	
	if (classIsUsed("BSE_pageInput1")) {
		Iview[viewID].pageInputObj.setNumberOfPages(Iview[viewID].amountPages);
		//doForEachInClass("amount",".value = '"+arabToRoem(Iview[viewID].amountPages)+"';", viewID); // write data next to the input-field
	}
	
	if (classIsUsed("BSE_pageForm1")) {
		Iview[viewID].pageFormObj.fill(Iview[viewID].amountPages);
	}

	//The currently not correct used Pagenumber is set to correct value
	navigatePage(Iview[viewID].pagenumber,viewID,false);
	
	if (Iview[viewID].useOverview) {
		importOverview(viewID);
		// actually be changed manually in CSS
		if (classIsUsed("BSE_openThumbs")) doForEachInClass("BSE_openThumbs", ".style.display = 'block';", viewID);
	}
}