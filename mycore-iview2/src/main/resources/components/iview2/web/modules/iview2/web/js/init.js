var iview = iview || {};
/**
 * @class
 * @constructor
 * @memberOf	iview.General
 * @name		General
 * @description All Viewer data and functions which don't fit in other packages
 */
iview.General = function(iview, viewID) {
	//TODO later it should be possible to remove all this.iview with just this
	this.iview = iview;
	//structure for all Viewer DOM-Objects
	this.iview.my = {'container': jQuery("#viewerContainer" + viewID),
					'viewer': jQuery("#viewer" + viewID),
					'preload': jQuery("#viewerContainer" + viewID + " .preload")};
	this.iview.viewID = viewID;
}

var genProto = iview.General.prototype;

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
 * @memberOf	iview.General
 * @name		zoomCenter
 * @description	Zooms the given Viewer so that the given point will be in center of view
 * @param		{integer} direction to zoom in = 1 out = -1
 * @param		{object} point coordinates to center
 * @param		{integer} point.x x-coordinate to center
 * @param		{integer} point.y y-coordinate to center
 */
genProto.zoomCenter = function(direction, point) {
	var viewer = this.iview.viewerBean;
	var preload = this.iview.my.preload;
	var preDim = {"x" :toInt(preload.css("left")),"y":toInt(preload.css("top")), "width":preload.width(), "height":preload.height()};
	viewer.zoom(direction);
	var newDim = {"width":preload.width(), "height":preload.height()};
	viewer.x = 0;
	viewer.y = 0;
	var npoint ={'x': ((-preDim.x + point.x) / preDim.width) * newDim.width,
				'y': ((-preDim.y + point.y) / preDim.height) * newDim.height};
	viewer.resetSlideMotion();
	viewer.recenter(npoint,true);
}

/**
 * @public
 * @function
 * @name		initializeGraphic
 * @memberOf	iview.General
 * @description	here some important values and listener are set correctly, calculate simple image name hash value to spread request over different servers and initialise the viewer
 */
genProto.initializeGraphic = function() {
	this.iview.zoomScale = 1;//init for the Zoomscale is changed within CalculateZoomProp
	this.iview.loaded = false;//indicates if the window is finally loaded
	this.iview.tilesize = tilesize;
	this.iview.initialModus = "none";
	// if the viewer started with an image with an single zoomLevel 0, because zoomMax = zoomInit & so initialZoom wont set
	this.iview.initialZoom = 0;
	this.iview.maximized = maximized;
	this.iview.images = [];
	PanoJS.USE_SLIDE = false;
	PanoJS.USE_LOADER_IMAGE = false;
	PanoJS.MOVE_THROTTLE = 10;
	PanoJS.BLANK_TILE_IMAGE = "../modules/iview2/web/" + styleFolderUri + 'blank.gif';
	
	// opera triggers the onload twice
	var iviewTileUrlProvider = new PanoJS.TileUrlProvider(this.iview.baseUri, this.iview.curImage, 'jpg');
	iviewTileUrlProvider.derivate = this.iview.viewID;

	/**
	 * initialise the viewer
	 */
	if (this.iview.viewerBean == null) {
		this.iview.viewerBean = new PanoJS("viewer"+this.iview.viewID, {
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: this.iview.tilesize,//Kachelgroesse
			tileUrlProvider: iviewTileUrlProvider,
			maxZoom: this.iview.zoomMax,
			initialZoom: this.iview.zoomInit,//Anfangs-Zoomlevel
			loadingTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif'
		});

		this.iview.viewerBean.iview = this.iview;//handle Viewer informations so PanoJS can work with it

		this.iview.viewerBean.init();
		
		this.reinitializeGraphic();
	}
}

/**
 * @public
 * @function
 * @name		reinitializeGraphic
 * @memberOf	iview.General
 * @description	is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus (i.e. scrrenWidth)
 */
genProto.reinitializeGraphic = function() {
	var viewerBean = this.iview.viewerBean;
	if (viewerBean == null) return;
		
	var curHeight = 0;
	var curWidth = 0;
	if (window.innerWidth) {
		curWidth = window.innerWidth;
		curHeight = window.innerHeight;
	} else {
		curWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth : document.body.clientWidth);
		curHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight : document.body.clientHeight);
	}

	var viewerContainer = this.iview.my.container;
	var viewer = this.iview.my.viewer;

	if (this.iview.maximized == true) {
		//to grant usage of the complete height it's not possible to simply use height:100%
		viewerContainer.css({'height': curHeight - viewerContainer.offset().top + "px",
							'width': curWidth + "px"});
		viewer.css({'height': curHeight - viewer.parent().offset().top - this.iview.my.barX.my.self.outerHeight()  + "px",
					'width': curWidth - this.iview.my.barY.my.self.outerWidth()  + "px"});
	} else {
		//restore minimized size settings
		viewerContainer.css({'height': this.iview.startHeight + "px",
							'width': this.iview.startWidth + "px"});
		viewer.css({'height': this.iview.startHeight - ((this.iview.my.barY.my.self.css("visibility") == "visible")? this.iview.my.barY.my.self.outerHeight() : 0)  + "px",
					'width': this.iview.startWidth - ((this.iview.my.barX.my.self.css("visibility") == "visible")? this.iview.my.barX.my.self.outerWidth() : 0)  + "px"});
	}
	
	viewerBean.width = viewer.outerWidth();
	viewerBean.height = viewer.outerHeight();
	viewerBean.resize();
	
	// den Modus beibehalten & aktualisieren
	if(this.iview.zoomScreen){
		this.iview.zoomScreen = !this.iview.zoomScreen;	
		this.pictureScreen();
	} else if(this.iview.zoomWidth){
		this.iview.zoomWidth = !this.iview.zoomWidth;
		this.pictureWidth();
	}
	
	if (this.iview.useOverview && this.iview.overview && this.iview.overview.getActive()) {
		// actualize Overview only if visible else delay it upto the reopening
		this.iview.overview.setSelected(this.iview.PhysicalModel.getCurPos());
	}
	
	this.handleScrollbars("resize");
	
	if (this.iview.useCutOut) {
		this.iview.cutOutModel.setRatio({
			'x': viewerBean.width / ((this.iview.picWidth / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale),
			'y': viewerBean.height / ((this.iview.picHeight / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale)});
		this.iview.cutOutModel.setPos({
			'x': - (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*this.iview.zoomScale,
			'y': - (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*this.iview.zoomScale});
	}
	
	// Actualize forward & backward Buttons
	var previewTbView = jQuery(this.iview.getToolbarCtrl().getView("previewTbView").toolbar);
	var newTop = ((((this.iview.picHeight / Math.pow(2, this.iview.zoomMax - 1)) * this.iview.zoomScale) - (previewTbView.height() + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
	if (this.iview.viewerContainer.hasClass("viewerContainer min")) {
		this.iview.viewerContainer.find(".toolbars .toolbar").css("top", newTop);
	}
	this.iview.toolbarCtrl.paint("mainTb");
}

/**
 * @public
 * @function
 * @name	maximizeHandler
 * @memberOf	iview.General
 * @description	maximize and show the viewer with the related image or minimize and close the viewer
 */
genProto.maximizeHandler = function() {
	if (this.iview.maximized) {
		if (URL.getParam("jumpback") == "true"){
			history.back();
			return;
		}
		this.iview.maximized = false;
		
		//close Overview when going to minimized mode
		if (this.iview.overview) {
			this.iview.overview.hideView();
		}
		// append viewer to dom again
		this.iview.VIEWER = document.body.firstChild;
		
		// clear document content
		while (document.body.firstChild) {
			document.body.removeChild(document.body.firstChild);
		}
		
		// restore current document content
		var index = 0;
		while (index < this.iview.DOCUMENT.length) {
			document.body.appendChild(this.iview.DOCUMENT[index]);
			index++;
		}
		
		// add current Viewer
		document.getElementById("viewerParent").insertBefore(this.iview.VIEWER, currentPos);
				
		// because of IE7 in
		document.documentElement.style.overflow="";
		
		document.body.style.overflow="";

		// class-change causes in IE resize
		this.iview.my.container.removeClass("max").addClass("min");
		
		if (!this.iview.zoomScreen) {
			this.pictureScreen();
		}
		this.iview.toolbarMgr.destroyModel('mainTb');
	} else {
		this.iview.maximized = true;
		
		this.iview.getToolbarCtrl().addView(new ToolbarView("mainTbView", this.iview.viewerContainer.find(".toolbars"), i18n));
		this.iview.getToolbarMgr().addModel(new StandardToolbarModelProvider("mainTb", this.iview.getToolbarMgr().titles).getModel());
		if (this.iview.PhysicalModel) {
			this.iview.getToolbarCtrl().checkNavigation(this.iview.PhysicalModel.getCurPos());
		}
		
		if (this.iview.zoomWidth) {
			jQuery(".mainTbView .zoomHandles .fitToWidth")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToWidthLabel").addClass("ui-state-active");
		} else if (this.iview.zoomScreen) {
			jQuery(".mainTbView .zoomHandles .fitToScreen")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToScreenLabel").addClass("ui-state-active");
		}
		
		// save document content
		this.iview.DOCUMENT = new Array();
		this.iview.VIEWER = this.iview.my.container[0].parentNode.parentNode.parentNode.parentNode;
		currentPos = this.iview.VIEWER.nextSibling;
		this.iview.VIEWER.parentNode.id = "viewerParent";
		
		// clear document content
		var index = 0;
		while (document.body.firstChild) {
			this.iview.DOCUMENT[index] = document.body.firstChild;
			document.body.removeChild(document.body.firstChild);
			index++;
		}

		// add Viewer
		document.body.appendChild(this.iview.VIEWER);
		
		// because of IE7 in
		document.documentElement.style.overflow="hidden";
		
		document.body.style.overflow="hidden";

		// class-change causes in IE resize
		this.iview.my.container.removeClass("min").addClass("max");
		this.iview.toolbarCtrl.paint("mainTb");
	}

	/*IE causes resize already at class change (mostly because position: rel <> fix)
	 IE runs resize multiple times...but without this line he doesn't...*/
	this.reinitializeGraphic();
}

//TODO drop Iview[viewID] commands and replace them with own General Object access
PanoJS.doubleClickHandler = function(e) {
	var iview = this.backingBean.iview;
	if (iview.maximized) {
		e = getEvent(e);
		var self = this.backingBean;
		coords = self.resolveCoordinates(e);
		if (self.zoomLevel < self.maxZoomLevel) {
			iview.gen.zoomCenter(1,coords);
		} else {
			self.resetSlideMotion();
			self.recenter(coords);
		}
	}
};

PanoJS.mousePressedHandler = function(e) {
	var that = this.backingBean.iview.gen;
	e = getEvent(e);
	if (that.iview.maximized) {
		// only grab on left-click
		if (e.button < 2) {
			var self = this.backingBean;
			var coords = self.resolveCoordinates(e);
			self.press(coords);
		}
	} else {
		that.maximizeHandler();
	}
	// NOTE: MANDATORY! must return false so event does not propagate to well!
	return false;
}

//Listener need to be notified and position has to be performed correctly
PanoJS.keyboardHandler = function(e) {
	e = getEvent(e);
	if (iview.credits)
		iview.credits(e);
	if (e.keyCode >= 37 && e.keyCode <=40) {
		//cursorkey movement
		var motion = {
				'x': PanoJS.MOVE_THROTTLE * (e.keyCode % 2) * (38 - e.keyCode),
				'y': PanoJS.MOVE_THROTTLE * ((39 - e.keyCode) % 2)};
		var viewer;
		for (var pos in PanoJS.VIEWERS) {
			viewer = PanoJS.VIEWERS[pos];
			if (!viewer.iview.maximized) break;
			viewer.positionTiles(motion, true);
			viewer.notifyViewerMoved(motion);
		}
		preventDefault(e);
		return false;
	}
	if ([109,45,189,107,61,187,144,27].indexOf(e.keyCode)>=0) {
		for (var i = 0; i < PanoJS.VIEWERS.length; i++) {
			var viewer = PanoJS.VIEWERS[i];
			var dir = 0;
			//+/- Buttons for Zooming
			//107 and 109 NumPad +/- supported by all, other keys are standard keypad codes of the given Browser
			if (e.keyCode == 109 || (e.keyCode == 45 && isBrowser("opera")) || e.keyCode == 189) {
				dir = -1;
			} else if (e.keyCode == 107 || e.keyCode == 61 || (isBrowser(["Chrome", "IE"]) && e.keyCode == 187) || (isBrowser("Safari") && e.keyCode == 144)) {
				dir = 1;
			} else if (e.keyCode == 27) {
				if (viewer.iview.maximized){
					viewer.iview.maximizeHandler();
				}
			}
			
			if (dir != 0 && viewer.iview.maximized) {
				viewer.iview.zoomCenter(dir,{"x":viewer.width/2, "y":viewer.height/2}); 
				preventDefault(e);
				e.cancelBubble = true;
				return false;
			}
		}
	}
};
