var blendings = new blendWorks();
var focus = null;//holds the element which is focused

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
	//return nodeProps(Iview[viewID].book, "child", getPage, absolute);
	return nodeProps(Iview[viewID].buchDaten, "mets:file", getPage, absolute);
}

/*
@description reads out the imageinfo.xml, set the correct zoomvalues and loads the page
*/
//TODO Loadpagedata in loadpage integrieren
function loadPage(pageData, viewID) {
	// Elemente ausblenden, die sich mit neuer Seite größenmäßig ändern
	// cutOut-Ausschnitt schiebt sich teilweise hinaus, wird bei zoom-Listener wieder visible
/*	if (Iview[viewID].useCutOut) {
		$("ausschnitt"+viewID).style.visibility = "hidden";
	}*/
	// Preload wird zu spät verkleinert
	$("preload"+viewID).style.visibility = "hidden";
	//console.log(findInArrayElement(pageData,"LOCTYPE", "URL").href);
	var imageProperties = loadXML(Iview[viewID].baseUri[0]+"/"+viewID+"/"+findInArrayElement(pageData, "LOCTYPE", "URL").href/*findInArrayElement(pageData,"LOCTYPE", "URL").href*/+"/imageinfo.xml", "xml");
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
	Iview[viewID].prefix  = findInArrayElement(pageData, "LOCTYPE", "URL").href;//findMETSEntry(pageData,"LOCTYPE", "URL").href;
	$("preload"+viewID).style.width = Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - Iview[viewID].zoomInit) + "px";
	$("preload"+viewID).style.height = Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - Iview[viewID].zoomInit) + "px";
	if (viewerBean == null) {
		initializeGraphic(viewID);
		viewerBean = Iview[viewID].viewerBean;
		viewerBean.addViewerZoomedListener(new listenerZoom(viewID));
		viewerBean.addViewerMovedListener(new listenerMove(viewID));
	} else {
		// prevents that (max) Zoomlevel will be reached which doesn't exists
		if (Iview[viewID].initialZoom < Iview[viewID].zoomMax) {
			Iview[viewID].zoomInit = Iview[viewID].initialZoom;
		} else {
			Iview[viewID].zoomInit = Iview[viewID].zoomMax;
		}
		viewerBean.tileUrlProvider.prefix = Iview[viewID].prefix;
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
	
	var initX = toFloat(window.location.search.get("x"));;
	var initY = toFloat(window.location.search.get("y"));
	viewerBean.positionTiles ({'x' : initX, 'y' : initY}, true);
	$("preload"+viewID).removeChild($("preloadImg"+viewID));
	var preload = new Image();
	preload.style.width = "100%";
	preload.style.height = "100%";
	preload.id = "preloadImg" + viewID;
	preload.src = viewerBean.tileUrlProvider.assembleUrl(0,0,0);
	
	$("preload"+viewID).appendChild(preload);
	$("preload"+viewID).style.visibility = "visible";
	if (Iview[viewID].useCutOut) {
		Iview[viewID].ausschnitt.setSRC(viewerBean.tileUrlProvider.assembleUrl(0,0,0));
	}
}

/*
@description blend in the overview an creates it by the first call
*/
function openOverview(viewID) {
	if (!Iview[viewID].overviewActive) {
		Iview[viewID].overviewActive = !Iview[viewID].overviewActive;
		// update overview
		Iview[viewID].overview1.actualize(Iview[viewID].pagenumber);
		/*var doBefore = "setOpacity($('blackBlank"+viewID+"'),0); $('blackBlank"+viewID+"').style.display ='block'";
		var doBetween = "$('overview1"+viewID+"').style.visibility ='visible'; $('viewer"+viewID+"').style.visibility = 'hidden';";
		var doAfter = "$('blackBlank"+viewID+"').style.display ='none'";
		*/

		// im VollBild Header mit ausblenden
		//if (Iview[viewID].maximized) doBetween = doBetween + "$('viewer"+viewID+"').parentNode.style.top = '0px'";

		$('overview1'+viewID).style.top = - ($("viewerContainer" + viewID).offsetHeight) + "px";
		$('overview1'+viewID).style.visibility = 'visible';
		blendings.slide("overview1"+viewID, new Array(0,- ($("viewerContainer" + viewID).offsetHeight),0,0),5,5,0,new Array(), "", "");
		
		//blendings.blend(['blackBlank'+viewID], ['blackBlank'+viewID], 7, 100, doAfter, doBetween, doBefore);

		openChapter(false, viewID);
	} else {
		Iview[viewID].overviewActive = !Iview[viewID].overviewActive;
		
		/*var doBefore = "setOpacity($('blackBlank"+viewID+"'),0); $('blackBlank"+viewID+"').style.display ='block';";
		var doBetween = "$('overview1"+viewID+"').style.visibility ='hidden'; $('viewer"+viewID+"').style.visibility = 'visible'; navigatePage("+Iview[viewID].pagenumber+",'"+viewID+"');";
		var doAfter = "$('blackBlank"+viewID+"').style.display = 'none';";
		*/
		
		// im VollBild Header wieder mit einblenden
		//if (Iview[viewID].maximized) doBetween = doBetween + "$('viewer"+viewID+"').parentNode.style.top = ''";
	
		blendings.slide("overview1"+viewID, new Array(0,0,0,- ($("viewerContainer" + viewID).offsetHeight)),5,5,0,new Array(), "", "$('overview1'+'"+viewID+"').style.visibility = 'hidden'");	
		//blendings.blend(['blackBlank'+viewID],  ['blackBlank'+viewID], 7, 100, doAfter, doBetween, doBefore);
	
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
	if (screenZoom) {
		Iview[viewID].zoomWidth = false;
	} else {
		Iview[viewID].zoomScreen = false;
	}
	stateBool = (stateBool)? false: true;
	viewerBean.clear();
	removeScaling(viewID);
	if (stateBool) {
		for (var i = 0; i <= Iview[viewID].zoomMax; i++) {
			if(Iview[viewID].bildBreite/viewerBean.width > Iview[viewID].bildHoehe/$("viewer"+viewID).offsetHeight || (stateBool && !screenZoom)){
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

	Iview[viewID].scrollBarX.setValue(-parseInt($("preload"+viewID).offsetLeft));
	Iview[viewID].scrollBarY.setValue(-parseInt($("preload"+viewID).offsetTop));
	//TODO zu machen?
	if (Iview[viewID].useCutOut) Iview[viewID].ausschnitt.setPosition({'x':$("preload"+viewID).offsetLeft, 'y':$("preload"+viewID).offsetTop});
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
	var scrollBarX = Iview[viewID].scrollBarX;
	var scrollBarY = Iview[viewID].scrollBarY;
	// determine the current imagesize
	var curBreite = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;
	var curHoehe = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;

	// horizontally
	// max scaling
	scrollBarX.setMaxValue(curBreite - viewerBean.width);
	// current position
	scrollBarX.setValue(-viewerBean.x);
	// length of the bar
	scrollBarX.setLength((viewerBean.width - ((Iview[viewID].useCutOut && DampInViewer)? toFloat(getStyle($("damp"+viewID), "width")):0))/ (curBreite/viewerBean.width));	
	
	// vertically
	scrollBarY.setMaxValue(curHoehe - viewerBean.height);
	scrollBarY.setValue(-viewerBean.y);
	scrollBarY.setLength((viewerBean.height - ((Iview[viewID].useCutOut && DampInViewer)? toFloat(getStyle($("damp"+viewID), "height")):0))/ (curHoehe/viewerBean.height));
}

/*
@description fit the scrollbar to the viewer-resize
*/
function handleResizeScrollbars(viewID) {
	var viewerBean = Iview[viewID].viewerBean;
	scrollBarX = Iview[viewID].scrollBarX;
	scrollBarY = Iview[viewID].scrollBarY;
	// determine the current imagesize
	var curBreite = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;
	var curHoehe = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale;

	// vertically
	// max scaling
	scrollBarY.setMaxValue(curHoehe - viewerBean.height);
	// size of the scrollbar
	scrollBarY.setSize(viewerBean.height);
	// length of the bar
	scrollBarY.setLength((viewerBean.height /*- ((Iview[viewID].useCutOut && DampInViewer)? toFloat(getStyle($("damp"+viewID), "height")):0)*/)/ (curHoehe/viewerBean.height));
	
	// horizontally
	scrollBarX.setMaxValue(curBreite - viewerBean.width);
	scrollBarX.setSize(viewerBean.width);
	scrollBarX.setLength((viewerBean.width /*- ((Iview[viewID].useCutOut && DampInViewer)? toFloat(getStyle($("damp"+viewID), "width")):0)*/)/ (curBreite/viewerBean.width));	
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
	
		$("preload"+viewID).style.width = (Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale +  "px";
		$("preload"+viewID).style.height = (Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale + "px";
	
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
		// block is only executed if the function not by scrollbar is calling, otherwise there are ugly interference
		if (!Iview[viewID].scroller) {
			Iview[viewID].scrollBarX.setValue(-parseInt($("preload"+viewID).offsetLeft));
			Iview[viewID].scrollBarY.setValue(-parseInt($("preload"+viewID).offsetTop));
		}
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
	//url += "book="+book_uri;
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
		if ($("chapter1"+viewID).style.visibility == "hidden") {
			$("chapter1"+viewID).style.visibility = "visible";
			Iview[viewID].chapter1.showCurrentPageCenter(Iview[viewID].pagenumber);
			blendings.slide("chapter1"+viewID, new Array(getStyle("chapter1"+viewID,"left"),0,getStyle("chapter1"+viewID,"left"),($("chapter1" + viewID).offsetHeight)),5,5,0,new Array(), "");
			Iview[viewID].chapterActive = true;
		} else {
			blendings.slide("chapter1"+viewID, new Array(getStyle("chapter1"+viewID,"left"),0,getStyle("chapter1"+viewID,"left"),- ($("chapter1" + viewID).offsetHeight)),5,5,0,new Array(), "",  "$('chapter1"+viewID+"').style.visibility = 'hidden';");
			Iview[viewID].chapterActive = false;
		}
	} else {
		// nur dann einblenden, wenn es durch Modus ausgeblendet wurde
		if (Iview[viewID].chapterActive && !Iview[viewID].overviewActive && Iview[viewID].maximized && $("chapter1"+viewID).style.visibility == "hidden") {
			$("chapter1"+viewID).style.visibility = "visible";
			Iview[viewID].chapter1.showCurrentPageCenter(Iview[viewID].pagenumber);
			blendings.slide("chapter1"+viewID,new Array(toFloat(getStyle($("chapter1"+viewID+"Out"), "left")) - toFloat(getStyle($("chapter1"+viewID+"Out"), "right")), toFloat(getStyle($("chapter1"+viewID+"Out"),"top")), toFloat(getStyle($("chapter1"+viewID+"In"), "left")) - toFloat(getStyle($("chapter1"+viewID+"In"), "right")), toFloat(getStyle($("chapter1"+viewID+"In"), "top"))),5,5,0,new Array("chapter1"+viewID+":in"));
		} else if (Iview[viewID].chapterActive && !Iview[viewID].overviewActive && Iview[viewID].maximized && $("chapter1"+viewID).style.visibility == "visible") {
			// nothing to do
		} else if ($("chapter1"+viewID).style.visibility == "visible" && Iview[viewID].overviewActive) {
			// do nothing for Overview
		} else if ($("chapter1"+viewID).style.visibility == "visible") {
			//blendings.slide("chapter1"+viewID,new Array(toFloat(getStyle($("chapter1"+viewID+"In"), "left")) - toFloat(getStyle($("chapter1"+viewID+"In"), "right")), toFloat(getStyle($("chapter1"+viewID+"In"), "top")), toFloat(getStyle($("chapter1"+viewID+"Out"), "left")) - toFloat(getStyle($("chapter1"+viewID+"Out"), "right")), toFloat(getStyle($("chapter1"+viewID+"Out"), "top"))),60,10,0,new Array("chapter1"+viewID+":out"), "", "$('chapter1'+'"+viewID+"').style.visibility = 'hidden'");
			// chapter soll sofort weg sein, nicht erst noch blenden, bspw. wenn vom Vollbild zurück ins normale
			$("chapter1"+viewID).style.visibility = 'hidden';
			$("chapter1"+viewID).style.top = getStyle($("chapter1"+viewID+"Out"), "top");
		}
		// last possible case (bool=false & vis=hidden) only for major
	}
}

/*
@description marks the correct picture in the chapterview and set zoombar to the correct zommlevel
*/
function updateModuls(viewID) {
	// align/fit scrollbars
	handleZoomScrollbars(viewID);
	handleResizeScrollbars(viewID);
	
	var viewerBean = Iview[viewID].viewerBean;
	// Actualize Chapter
	if (Iview[viewID].useChapter) {
		Iview[viewID].chapter1.changePage(Iview[viewID].pagenumber);
		//Iview[viewID].chapter1.posScroll(viewID);
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
	Iview[viewID].zoomBar = new zoomBar("zoomBar"+viewID, $(Iview[viewID].zoomBarParent), "");
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
	ManageEvents.addEventListener($("viewer"+viewID), 'mouseup', zoombar.mouseUpZoombar, false);
	ManageEvents.addEventListener($("viewer"+viewID), 'mousemove', zoombar.mouseMoveZoombar, false);
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
				}, true);});
	// Additional Events
	ManageEvents.addEventListener(document.getElementsByTagName("body")[0], 'mouseup', ausschnitt.mouseUp, false);
	ManageEvents.addEventListener(/*$("viewer"+viewID)*/$(Iview[viewID].ausschnittParent), 'mousemove', ausschnitt.mouseMove, false);
	// wird in Klasse für cutOut bzw. in loading für viewer gemacht
	// ManageEvents.addEventListener($("viewer"+viewID), 'mouseScroll', ausschnitt.scroll, false);
}

/*
@description calls the corresponding functions to create the chapter
*/
function importChapter(viewID) {
	// Chapter
	Iview[viewID].chapter1 = new chapter("chapter1"+viewID, Iview[viewID].chapterParent);
	var chapter1 = Iview[viewID].chapter1;
	
	chapter1.setViewID(viewID);
	chapter1.setBookData(Iview[viewID].buchDaten);
	chapter1.init("chapter1");
	
	// needed by IE
	//TODO: muss wieder rein, wenn slide auch ohne fade geht
	// blendings.setOpacity($("chapter1"+viewID),100);
	if (chapDynResize) {
		chapter1.setSize(null, chapter1.my.self.parentNode.offsetHeight * chapResizeMul + chapResizeAdd);
	}
	chapter1.useEffects(chapHover);
	chapter1.displayOutAllEntries($("chapter1"+viewID+"_content").firstChild, 0);
	// Additional Listener
	chapter1.addListener(chapter.PAGE_NUMBER, new function() { this.change = function(value) {
		navigatePage(value, viewID);
	}});
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
	$("overview1"+viewID).style.visibility = "hidden";
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
	if (!classIsUsed("BSE_pageForm1")) {
		Iview[viewID].pageFormObj = new pageForm("BSE_pageForm"+viewID, parentID, "BSE_pageForm");
		var pageFormObj = Iview[viewID].pageFormObj;
		pageFormObj.setViewID(viewID);
		pageFormObj.init();
		pageFormObj.addListener(pageForm.PAGE_NUMBER, new function() {
			this.change = function(value) {
	    		navigatePage(value, viewID);
			}
		});
	} else {
		Iview[viewID].pageFormObj.initNext();
	}
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
		$("header"+viewID).appendChild(element);
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
			var nodes = files[i].childNodes;
			for (var j = 0; j < nodes.length; j++) {
				if (nodes[j].attributes.getNamedItem("LOCTYPE").value == "OTHER" && nodes[j].attributes.getNamedItem("OTHERLOCTYPE").value.toLowerCase() == "pagenumber") {
					Iview[viewID].pagenumber = parseInt(nodes[j].attributes.getNamedItem(namespaceCheck("xlink:href")).value)+1;
					break;
				}
			}
		}
	}
}
/*
@description is calling to the load-event of the window; serve for the further registration of events likewise as initioator for various objects
@param e Daten vom Load Event
*/
function loading(viewID) {
	Iview[viewID].chapterActive = false;
	Iview[viewID].overviewActive = false;
	//Create new Header Elements as specified within caller xsl
	splitHeader(viewID);
	
	style = styleFolderUri + styleName + "/";
	//TODO gucken ob vars evtl wo anders geladen werden
	//loadVars("../modules/iview2/web/" + style + "design.xml");//Laden der Informationen je nach entsprechendem Design
	//retrieves the mets File depending on the fact if it's exists or it request a simple one

	blendings.useEffects(blendEffects);

	// ScrollBars
	// horizontally

	Iview[viewID].scrollBarX = new scrollBar("scrollH"+viewID, "scroll");
	var scrollBarX = Iview[viewID].scrollBarX;
	scrollBarX.init(true);
	scrollBarX.addListener(scrollBar.LIST_MOVE, new function() { this.moved = function(vector) { scrollMove(-vector.x, -vector.y, viewID);}});
	scrollBarX.setParent("viewerContainer"+viewID);
	// vertically
	Iview[viewID].scrollBarY = new scrollBar("scrollV"+viewID, "scroll");
	var scrollBarY = Iview[viewID].scrollBarY;
	scrollBarY.init(false);
	scrollBarY.addListener(scrollBar.LIST_MOVE, new function() { this.moved = function(vector) { scrollMove(-vector.x, -vector.y, viewID);}});
	scrollBarY.setParent("viewerContainer"+viewID);

	// Additional Events
	// move and down auf viewer
	ManageEvents.addEventListener($("viewerContainer"+viewID), 'mouseMove', Iview[viewID].scrollBarX.mouseMove, false);
	ManageEvents.addEventListener($("viewerContainer"+viewID), 'mouseUp', Iview[viewID].scrollBarX.mouseUp, false);
	ManageEvents.addEventListener($("viewerContainer"+viewID), 'mouseMove', Iview[viewID].scrollBarY.mouseMove, false);
	ManageEvents.addEventListener($("viewerContainer"+viewID), 'mouseUp', Iview[viewID].scrollBarY.mouseUp, false);
	// register to scroll into the viewer
	ManageEvents.addEventListener($("viewer"+viewID), 'mouseScroll', function(e) { e = getEvent(e); preventDefault(e); viewerScroll(returnDelta(e), viewID);}, false);
	
	// damit viewer über scrollBarX endet, fortan in reinitialize
	$("viewer"+viewID).style.width = $("viewerContainer"+viewID).offsetWidth - Iview[viewID].scrollBarY.my.self.offsetWidth  + "px";
	$("viewer"+viewID).style.height = $("viewerContainer"+viewID).offsetHeight - Iview[viewID].scrollBarX.my.self.offsetHeight  + "px";

	if (Iview[viewID].useCutOut) {
		importCutOut(viewID);
	}

	// Load Page
	if (window.location.search.get("page") != "") {
		//TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
		Iview[viewID].startFile = window.location.search.get("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/,"§"); 
	}
	loadPage([{"LOCTYPE":"URL","href":Iview[viewID].startFile}],viewID);
	Iview[viewID].loaded = true;
	
	var mets_uri = Iview[viewID].webappBaseUri + "servlets/" + ((Iview[viewID].hasMets)? "MCRFileNodeServlet/": "MCRDirectoryXMLServlet/") + viewID + ((Iview[viewID].hasMets)? "/mets.xml":"");
	Iview[viewID].buchDaten = (Iview[viewID].hasMets)? loadXML(mets_uri):loadXML(mets_uri, "mets");
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
	
	if (Iview[viewID].useZoomBar) {
		importZoomBar(viewID);
	}


	if (Iview[viewID].useChapter) {
		importChapter(viewID);

		//if (!chapterEmbedded) {
			// actually be changed manually in CSS
			//if (classIsUsed("BSE_chapterOpener")) doForEachInClass("BSE_chapterOpener" ,".style.display = 'block';");
		//}
	}
	
	if (Iview[viewID].useOverview) {
		importOverview(viewID);
		// actually be changed manually in CSS
		if (classIsUsed("BSE_openThumbs")) doForEachInClass("BSE_openThumbs", ".style.display = 'block';", viewID);
	}
	
	//The currently not correct used Pagenumber is set to correct value
	navigatePage(Iview[viewID].pagenumber,viewID,false);
	
	// Resize-Events registrieren
	if (isBrowser("IE")) {
		$("viewer"+viewID).parentNode.onresize = function() {reinitializeGraphic(viewID)};
	} else {
		ManageEvents.addEventListener(window, 'resize', function() { reinitializeGraphic(viewID);}, false);
	}
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
}
