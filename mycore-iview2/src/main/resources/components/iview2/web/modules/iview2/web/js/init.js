//IE and Opera doesn't accept our TileUrlProvider Instance as one of PanoJS
PanoJS.isInstance = function () { return true};
/*
 * calculate simple image name hash value to spread request over different servers
 * but allow browser cache to be used by allways return the same value for a given name 
 */
PanoJS.TileUrlProvider.prototype.imageHashes = [];
PanoJS.TileUrlProvider.prototype.getImageHash = function(image){
	if (this.imageHashes[image]){
		return this.imageHashes[image];
	}
	var hash=0;
	var pos=image.lastIndexOf(".");
	if (pos < 0)
		pos=image.length;
	for (var i=0;i<pos;i++){
		hash += 3 * hash + (image.charCodeAt(i)-48);
	}
	this.imageHashes[image]=hash;
	return hash;
}
/*
 * returns the URL of all tileimages
 */
PanoJS.TileUrlProvider.prototype.assembleUrl = function(xIndex, yIndex, zoom, image){
	image=(image == null)? this.prefix : image;
    return this.baseUri[(this.getImageHash(image)+xIndex+yIndex) % this.baseUri.length] + '/'+ this.derivate+'/' + 
        image + '/' + zoom + '/' + yIndex + '/' + xIndex + '.' + this.extension +
        (PanoJS.REVISION_FLAG ? '?r=' + PanoJS.REVISION_FLAG : '');
};

/**
 * @public
 * @function
 * @name		initializeGraphic
 * @memberOf	iview.init
 * @description	here some important values and listener are set correctly, calculate simple image name hash value to spread request over different servers and initialise the viewer
 * @param 		{string} viewID ID of the derivate
 */
function initializeGraphic(viewID) {
	Iview[viewID].zoomScale = 1;//init for the Zoomscale is changed within CalculateZoomProp
	Iview[viewID].loaded = false;//indicates if the window is finally loaded
	Iview[viewID].tilesize = tilesize;
	Iview[viewID].initialModus = [false, false];
	// if the viewer started with an image with an single zoomLevel 0, because zoomMax = zoomInit & so initialZoom wont set
	Iview[viewID].initialZoom = 0;
	Iview[viewID].maximized = maximized;
	Iview[viewID].images = [];
	PanoJS.USE_SLIDE = false;
	PanoJS.USE_LOADER_IMAGE = false;
	PanoJS.MOVE_THROTTLE = 10;
	
	// opera triggers the onload twice
	var iviewTileUrlProvider = new PanoJS.TileUrlProvider(Iview[viewID].baseUri, Iview[viewID].prefix, 'jpg');
	iviewTileUrlProvider.derivate = viewID;

	/**
	 * initialise the viewer
	 */
	if (Iview[viewID].viewerBean == null) {
		Iview[viewID].viewerBean = new PanoJS("viewer"+viewID, {
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: Iview[viewID].tilesize,//Kachelgroesse
			tileUrlProvider: iviewTileUrlProvider,
			maxZoom: Iview[viewID].zoomMax,
			initialZoom: Iview[viewID].zoomInit,//Anfangs-Zoomlevel
			blankTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
			loadingTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif'
		});

		Iview[viewID].viewerBean.viewID = viewID;//Add Viewer ID mit übergeben damit der Viewer darauf arbeiten kann

		Iview[viewID].viewerBean.init();
		//Newer Opera/Safari Versions need somehow something from reinitGraphics to show the viewer on Startup
		if (navigator.userAgent.match(/Opera(.*)?Version\/10\.[0-9]*/i) || isBrowser(["Safari"])) {
			reinitializeGraphic(viewID);
		}
	}
}

/**
 * @public
 * @function
 * @name		reinitializeGraphic
 * @memberOf	iview.init
 * @description	is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus (i.e. scrrenWidth)
 * @param 		{string} viewID ID of the derivate
 */
function reinitializeGraphic(viewID) {
	// TODO: attention on the runtime, if to slow, then the viewer will be shown shortly
	// --> eventuell sogar rausschieben falls sinnvoll - moeglich
	var viewerBean = Iview[viewID].viewerBean;
	if (viewerBean == null) return;
		
	var curHeight = 0;
	var curWidth = 0;
	if (window.innerWidth) {
		curWidth = window.innerWidth;
		curHeight = window.innerHeight;
	}
	else {
		curWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth : document.body.clientWidth);
		curHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight : document.body.clientHeight);
	}

	// damit volle Höhe gewährleistet werden kann, height: 100% nicht verwendbar
	var viewerContainer=document.getElementById("viewerContainer"+viewID);
	var viewer=document.getElementById("viewer"+viewID);
	if (Iview[viewID].maximized == true) {
		viewerContainer.style.height = curHeight - viewerContainer.offsetTop + "px";
		viewer.style.height = curHeight - viewer.parentNode.offsetTop - Iview[viewID].barX.my.self[0].offsetHeight  + "px";
		viewerContainer.style.width = curWidth + "px";
		viewer.style.width = curWidth - Iview[viewID].barY.my.self[0].offsetWidth  + "px";
	} else {
		// Wert wieder herstellen
		viewerContainer.style.height = Iview[viewID].startHeight + "px";
		viewer.style.height = Iview[viewID].startHeight - ((Iview[viewID].barY.my.self.css("visibility") == "visible")? Iview[viewID].barY.my.self[0].offsetHeight : 0)  + "px";
		viewerContainer.style.width = Iview[viewID].startHeight + "px";
		viewer.style.width = Iview[viewID].startWidth - ((Iview[viewID].barX.my.self.css("visibility") == "visible")? Iview[viewID].scrollBarX.my.self[0].offsetWidth : 0)  + "px";
	}
	
	viewerBean.width = viewer.offsetWidth;
	viewerBean.height = viewer.offsetHeight;
	viewerBean.resize();
	
	// den Modus beibehalten & aktualisieren
	if(Iview[viewID].zoomScreen){
		Iview[viewID].zoomScreen = !Iview[viewID].zoomScreen;	
		pictureScreen(viewID);
	} else if(Iview[viewID].zoomWidth){
		Iview[viewID].zoomWidth = !Iview[viewID].zoomWidth;
		pictureWidth(viewID);
	}
	
	if (Iview[viewID].useOverview && Iview[viewID].overview && Iview[viewID].overview.getActive()) {
		// actualize Overview only if visible else delay it upto the reopening
		Iview[viewID].overview.setSelected(Iview[viewID].PhysicalModel.getCurPos());
	}
	
	handleResizeScrollbars(viewID);
	
	if (Iview[viewID].useCutOut) {
		Iview[viewID].cutOutModel.setRatio({
			'x': viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale),
			'y': viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)});
		Iview[viewID].cutOutModel.setPos({
			'x': - (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale,
			'y': - (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale});
	}
	
	// Actualize forward & backward Buttons
	var previewTbView = jQuery(Iview[viewID].getToolbarCtrl().getView("previewTbView").toolbar);
	var newTop = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
	if (Iview[viewID].viewerContainer.hasClass("viewerContainer min")) {
		Iview[viewID].viewerContainer.find(".toolbars .toolbar").css("top", newTop);
	}
}

/**
 * @public
 * @function
 * @name	maximizeHandler
 * @memberOf	iview.init
 * @description	maximize and show the viewer with the related image or minimize and close the viewer
 * @param 	{string} viewID ID of the derivate
 */
function maximizeHandler(viewID) {
	if (Iview[viewID].maximized) {
		if (window.location.search.get("jumpback") == "true"){
			history.back();
			return;
		}
		Iview[viewID].maximized = false;
		
		//close Overview when going to minimized mode 
		Iview[viewID].overview.hideView();

		// append viewer to dom again
		Iview[viewID].VIEWER = document.body.firstChild;
		
		// clear document content
		while (document.body.firstChild) {
			document.body.removeChild(document.body.firstChild);
		}
		
		// restore current document content
		var index = 0;
		while (index < Iview[viewID].DOCUMENT.length) {
			document.body.appendChild(Iview[viewID].DOCUMENT[index]);
			index++;
		}
		
		// add current Viewer
		document.getElementById("viewerParent").insertBefore(Iview[viewID].VIEWER, currentPos);
				
		// because of IE7 in
		document.documentElement.style.overflow="";
		
		document.body.style.overflow="";

		// class-change causes in IE resize
		document.getElementById("viewerContainer"+viewID).className = "viewerContainer min";
		
		if (!Iview[viewID].zoomScreen) {
			pictureScreen(viewID);
		}
		Iview[viewID].toolbarMgr.destroyModel('mainTb');
	} else {
		Iview[viewID].maximized = true;
		
		Iview[viewID].getToolbarCtrl().addView(new ToolbarView("mainTbView", Iview[viewID].viewerContainer.find(".toolbars")));
		Iview[viewID].getToolbarMgr().addModel(new StandardToolbarModelProvider("mainTb", Iview[viewID].getToolbarMgr().titles).getModel());
		if (Iview[viewID].PhysicalModel) {
			Iview[viewID].getToolbarCtrl().checkNavigation(Iview[viewID].PhysicalModel.getCurPos());
		}
		
		if (Iview[viewID].zoomWidth) {
			jQuery(".mainTbView .zoomHandles .fitToWidth")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToWidthLabel").addClass("ui-state-active");
		} else if (Iview[viewID].zoomScreen) {
			jQuery(".mainTbView .zoomHandles .fitToScreen")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToScreenLabel").addClass("ui-state-active");
		}
		
		// save document content
		Iview[viewID].DOCUMENT = new Array();
		Iview[viewID].VIEWER = document.getElementById("viewerContainer"+viewID).parentNode.parentNode.parentNode.parentNode;
		currentPos = Iview[viewID].VIEWER.nextSibling;
		document.getElementById("viewerContainer"+viewID).parentNode.parentNode.parentNode.parentNode.parentNode.id = "viewerParent";
		
		// clear document content
		var index = 0;
		while (document.body.firstChild) {
			Iview[viewID].DOCUMENT[index] = document.body.firstChild;
			document.body.removeChild(document.body.firstChild);
			index++;
		}

		// add Viewer
		document.body.appendChild(Iview[viewID].VIEWER);
		
		// because of IE7 in
		document.documentElement.style.overflow="hidden";
		
		document.body.style.overflow="hidden";

		// class-change causes in IE resize
		document.getElementById("viewerContainer"+viewID).className = "viewerContainer max";
		
	}

// IE löst resize bereits bei den Class-Wechsel (sicherlich wegen position rel <-> fix)
	//IE führt die zwar so irgendwie mehrfach aus... aber ohne die auch nicht...muss man wohl mit leben
//	if (!(isBrowser("IE"))) {
		reinitializeGraphic(viewID);
//	}
}

PanoJS.doubleClickHandler = function(e) {
	var viewID = this.backingBean.viewID;
	if (Iview[viewID].maximized) {
		e = getEvent(e);
		var self = this.backingBean;
		coords = self.resolveCoordinates(e);
		if (!self.pointExceedsBoundaries(coords)) {
			self.resetSlideMotion();
			self.recenter(coords);
		}
		if (self.zoomLevel < self.maxZoomLevel) {
			self.zoom(1);
			if (Iview[viewID].useZoomBar) Iview[viewID].zoomBar.moveBarToLevel(self.zoomLevel);
		}
	}
};

PanoJS.mousePressedHandler = function(e) {
	var viewID = this.backingBean.viewID;
	e = getEvent(e);
	if (Iview[viewID].maximized) {
		// only grab on left-click
		if (e.button < 2) {
			var self = this.backingBean;
			var coords = self.resolveCoordinates(e);
			self.press(coords);
		}
	} else {
		maximizeHandler(viewID);
	}
	// NOTE: MANDATORY! must return false so event does not propagate to well!
	return false;
}

//Listener need to be notified and position has to be performed correctly
PanoJS.keyboardMoveHandler = function(e) {
	e = getEvent(e);
	if (e.keyCode >= 37 && e.keyCode <=40) {
		//cursorkey movement
		var motion = {
				'x': PanoJS.MOVE_THROTTLE * (e.keyCode % 2) * (38 - e.keyCode),
				'y': PanoJS.MOVE_THROTTLE * ((39 - e.keyCode) % 2)};
		var viewer;
		for (var pos in PanoJS.VIEWERS) {
			viewer = PanoJS.VIEWERS[pos];
			if (!Iview[viewer.viewID].maximized) break;
			viewer.positionTiles(motion, true);
			viewer.notifyViewerMoved(motion);
		}
		preventDefault(e);
		return false;
	}
};

PanoJS.keyboardZoomHandler = function(e) {
	e = getEvent(e);
	console.log(e.keyCode)
	for (var i = 0; i < PanoJS.VIEWERS.length; i++) {
		var viewer = PanoJS.VIEWERS[i];
		var zoomDir = 0;
		//+/- Buttons for Zooming
		//107 and 109 NumPad +/- supported by all, other keys are standard keypad codes of the given Browser
		if (e.keyCode == 109 || (e.keyCode == 45 && isBrowser("opera")) || e.keyCode == 189) {
			zoomDir = -1;
		} else if (e.keyCode == 107 || e.keyCode == 61 || (isBrowser(["Chrome", "IE"]) && e.keyCode == 187) || (isBrowser("Safari") && e.keyCode == 144)) {
			zoomDir = 1;
		} else if (e.keyCode == 27) {
			if (Iview[viewer.viewID].maximized){
				maximizeHandler(viewer.viewID);
			}
		}
		
		if (zoomDir != 0 && Iview[viewer.viewID].maximized) {
			viewer.zoom(zoomDir);
			if (Iview[viewer.viewID].useZoombar) {
				Iview[viewer.viewID].zoomBar.moveBarToLevel(viewer.zoomLevel);
			}
			preventDefault(e);
			e.cancelBubble = true;
			return false;
		} else {
			//Safari does not support "onkeypress" for cursor keys but "onkeydown", so move over
			PanoJS.keyboardMoveHandler(e);
		}
	}
};