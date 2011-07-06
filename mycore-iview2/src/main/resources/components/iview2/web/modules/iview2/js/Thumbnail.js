var length = null;
var focus = null;//holds the element which is focused
//TODO Preload größe anhand der von den Kacheln bestimmen
//var listener = []; // array who holds informations about the listeners (?)
//NAVIGATE = 0;
//
//function addListener(type, theListener) {
//	if (!listener[type]) {
//		listener[type] = [];
//	}
//	listener[type].push(theListener);
//}
//
//function dropListener(type, theListener) {
//	for (var i = 0; i < listener[type].length; i++) {
//		if (listener[type][i] == theListener) {
//			listener[type].splice(i,1);
//		}
//	}
//}
//
//notifyListenerNavigate(value, viewID) {
//	if (!listener[NAVIGATE]) {
//		return;
//	}
//	for(var i = 0; i < listener[NAVIGATE].length; i++) {
//		listener[NAVIGATE][i].navi(value,viewID);
//	}
//};

/**
 * @public
 * @function
 * @name		loadPage
 * @memberOf	iview.General
 * @description	reads out the imageinfo.xml, set the correct zoomvlues and loads the page
 * @param		{function} callback
 */
genProto.loadPage = function(callback) {
	var url;
	if (typeof(this.iview.metsDoc)=='undefined'){
		url = this.iview.startFile;
	} else {
		url = this.iview.PhysicalModel.getCurrent().getHref();
	}
	this.iview.currentImage.setName(url);
	var imagePropertiesURL = this.iview.baseUri[0]+"/"+this.iview.viewID+"/"+url+"/imageinfo.xml";
	var that = this;
	jQuery.ajax({
		url: imagePropertiesURL,
  		success: function(response) {that.processImageProperties(response);},
  		error: function(request, status, exception) {
  			if(console){
  				console.log("Error occured while loading image properties:\n"+exception);
  			}
  		},
  		complete: function() {callBack(callback);}
	});
};

/**
 * @public
 * @function
 * @name		processImageProperties
 * @memberOf	iview.General
 * @description	
 * @param 		{object} imageProperties
 */
genProto.processImageProperties = function(imageProperties){
	var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
	
	this.iview.tiles = parseInt(values['tiles']);
	this.iview.currentImage.setWidth(parseInt(values['width']));
	this.iview.currentImage.setHeight(parseInt(values['height']));
	var viewerBean = this.iview.viewerBean;

	if (viewerBean) {
		// checks if current zoomlevel was "greater" as zoomMax, so the current zoomLevel wouldn't reach
		// update the initialZoom only if this is not the case
		if (viewerBean.zoomLevel != this.iview.zoomMax || this.iview.initialZoom <= viewerBean.zoomLevel) {
			this.iview.initialZoom = viewerBean.zoomLevel;
		}
		
		// checks for enabled Modi & reset before
		this.iview.initialModus = "none";
		if (this.iview.zoomWidth) {
			this.iview.initialModus = "width";
		}
		if (this.iview.zoomScreen) {
			this.iview.initialModus = "screen";
		}
	}

	this.iview.zoomMax = parseInt(values['zoomLevel']);

	if (!isNaN(parseInt(URL.getParam("zoom")))) {
		this.iview.zoomInit = parseInt(URL.getParam("zoom"));
		if (this.iview.zoomInit > this.iview.zoomMax)
			this.iview.zoomInit = this.iview.zoomMax;
		if (this.iview.zoomInit < 0)
			this.iview.zoomInit = 0;
	} else {
		// zoomLevel 0 ist erstes Level
		this.iview.zoomInit = Math.ceil((this.iview.zoomMax + 1) / 2) - 1;
	}
	var preload = new Image();
	preload.className = "preloadImg";
	var preloadCont=this.iview.my.preload;
	preloadCont.css({"width" : this.iview.currentImage.getWidth() / Math.pow(2, this.iview.zoomMax - this.iview.zoomInit) + "px",
					 "height" : this.iview.currentImage.getHeight() / Math.pow(2, this.iview.zoomMax - this.iview.zoomInit) + "px"})
			 .empty()
			 .append(preload);

	if (viewerBean == null) {
		this.initializeGraphic();
		viewerBean = this.iview.viewerBean;
		viewerBean.addViewerZoomedListener(this);
		viewerBean.addViewerMovedListener(this);
		preload.src = viewerBean.tileUrlProvider.assembleUrl(0,0,0);
	} else {
		// prevents that (max) Zoomlevel will be reached which doesn't exists
		if (this.iview.initialZoom < this.iview.zoomMax) {
			this.iview.zoomInit = this.iview.initialZoom;
		} else {
			this.iview.zoomInit = this.iview.zoomMax;
		}
		viewerBean.tileUrlProvider.prefix = this.iview.currentImage.getName();
		preload.src = viewerBean.tileUrlProvider.assembleUrl(0,0,0);
		viewerBean.resize();
	}
	// moves viewer to zoomLevel zoomInit
	viewerBean.maxZoomLevel = this.iview.zoomMax;
	// handle special Modi for new Page
	if (this.iview.initialModus == "width") {
		// letzte Seite war in fitToWidth
		// aktuell ist fuer die neue Page noch kein Modi aktiv
		this.iview.zoomWidth = false;
		this.pictureWidth();
	} else if (this.iview.initialModus == "screen") {
		// letzte Seite war in fitToScreen
		// aktuelle ist fuer die neue Page noch kein Modi aktiv
		this.iview.zoomScreen = false;
		this.pictureScreen();
	} else {
		// moves viewer to zoomLevel zoomInit
		viewerBean.zoom(this.iview.zoomInit - viewerBean.zoomLevel);
	}
	
	// damit das alte zoomBack bei Modi-Austritt nicht verwendet wird
	this.iview.zoomBack = this.iview.zoomInit;
	var initX = toFloat(URL.getParam("x"));
	var initY = toFloat(URL.getParam("y"));
	
	this.iview.roller = true;
	viewerBean.positionTiles ({'x' : initX, 'y' : initY}, true);
	
	preload.style.width = "100%";
	preload.style.height = "100%";
	if (this.iview.useCutOut) {
		this.iview.cutOutModel.setSrc(viewerBean.tileUrlProvider.assembleUrl(0,0,0));
	}
	this.updateModuls();
	
	this.iview.roller = false;
}

/**
 * @public
 * @function
 * @name		openOverview
 * @memberOf	iview.General
 * @description	blend in the overview and creates it by the first call
 * @param		{button} button to which represents the Overview in the toolbar
 */
genProto.openOverview = function(button) {
	var that = this;
	// check if overview was created yet
	if (typeof this.iview.overview === 'undefined') {
		button.setLoading(true);
		setTimeout(function(){
			var callback = function() {
				// try again openOverview (recursive call)
				that.openOverview(button);
				button.setLoading(false);
				
				that.iview.overview.attach("click.overview", function(e, val) {
					// type 1: click on overview div
					button.setSubtypeState(false);
				});
			};
			that.importOverview(callback);
		}, 10);
	} else {
		this.iview.overview.toggleView();
	}
}

/**
 * @public
 * @function
 * @name		openPermalink
 * @memberOf	iview.General
 * @description	switch between visibility of Permalink element, if needed it's created at first run
* @param		{button} button to which represents the Permalink in the toolbar
 */
genProto.openPermalink = function(button) {
	var that = this;
	if (typeof this.getPermalinkCtrl === "undefined") {
		button.setLoading(true);
		setTimeout(function() {
			var callback = function() {
				that.openPermalink(button);
				button.setLoading(false)};
			that.importPermalink(callback);
		}, 10);
	} else {
		this.getPermalinkCtrl().show();
	}
}

/**
 * @public
 * @function
 * @name		importPermalink
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the Permalink
 * @param		{function} callback function to call after the permalink was loaded successfully
 */
genProto.importPermalink = function(callback) {
	// Permalink
	this.getPermalinkCtrl = function() {
		if (!this.permalinkCtrl) {
			this.permalinkCtrl = new iview.Permalink.Controller(this);
			
			//iview.Permalink.Controller.prototype.getViewer = function() {
			this.permalinkCtrl.getViewer = function() {
				return this.parent;
			}
		}
		return this.permalinkCtrl;
	}

	this.getPermalinkCtrl().addView(new iview.Permalink.View("permalinkView", jQuery("#viewerContainer"+ this.iview.viewID + " .toolbars").parent()));
	callback();
}

/**
 * @public
 * @function
 * @name		removeScaling
 * @memberOf	iview.General
 * @description	saves the scaling of loaded tiles if picture fits to height or to width (for IE)
 */
genProto.removeScaling = function() {
	for (var img in this.iview.images) {
		this.iview.images[img]["scaled"] = false;
	}
}

/**
 * @public
 * @function
 * @name		isloaded
 * @memberOf	iview.General
 * @description	checks if the picture is loaded
 * @param		{object} img
 */
genProto.isloaded = function(img) {
	/*
	NOTE tiles are not dispalyed correctly in Opera, because the used accuracy for pixelvalues only has 
	2 dezimal places, however 3 are neccessary for the correct representation as in FF
	*/
	if (!this.iview.images[img.src]) {
		this.iview.images[img.src] = new Object();
		this.iview.images[img.src]["scaled"] = false;
		img.style.display = "none";
	}
	if (((img.naturalWidth == 0 && img.naturalHeight == 0)  && !isBrowser(["IE", "Opera"])) || (!img.complete && isBrowser(["IE", "Opera"]))) {
		if (img.src.indexOf("blank.gif") == -1) {//change
			var that = this;
			window.setTimeout(function(image) { return function(){that.isloaded(image);} }(img), 100);
		}
	} else if (img.src.indexOf("blank.gif") == -1) {
		if (this.iview.images[img.src]["scaled"] != true) {
			img.style.display = "inline";
			this.iview.images[img.src]["scaled"] = true;//notice that this picture already was scaled
			//TODO math Floor rein bauen bei Höhe und Breite
			if (!isBrowser(["IE","Opera"])) {
				img.style.width = this.iview.zoomScale * img.naturalWidth + "px";
				img.style.height = this.iview.zoomScale * img.naturalHeight + "px";
			} else {
				if (!this.iview.images[img.src]["once"]) {
					this.iview.images[img.src]["once"] = true;
					this.iview.images[img.src]["naturalheight"] = img.clientHeight;
					this.iview.images[img.src]["naturalwidth"] = img.clientWidth;
				}
				img.style.width = this.iview.zoomScale * this.iview.images[img.src]["naturalwidth"] + "px";
				img.style.height = this.iview.zoomScale * this.iview.images[img.src]["naturalheight"] + "px";
			}
		}
	}
	img = null;
}

/**
 * @public
 * @function
 * @name		calculateZoomProp
 * @memberOf	iview.General
 * @description	calculates how the TileSize and the zoomvalue needs to be if the given zoomlevel fits into the viewer
 * @param		{integer} level the zoomlevel which is used for testing
 * @param		{integer} totalSize the total size of the Picture Dimension X or Y
 * @param		{integer} viewerSize the Size of the Viewer Dimension X or Y
 * @param		{integer} scrollBarSize the Height or Width of the ScrollBar which needs to be dropped from the ViewerSize
 * @return		boolean which tells if it was successfull to scale the picture in the current zoomlevel to the viewer Size
 */
genProto.calculateZoomProp = function(level, totalSize, viewerSize, scrollBarSize) {
	if ((totalSize / Math.pow(2, level)) <= viewerSize) {
		var viewerBean = this.iview.viewerBean;
		if (level != 0) {
			level--;
		}
		var currentWidth = totalSize / Math.pow(2, level);
		var viewerRatio = viewerSize / currentWidth;
		var fullTileCount = Math.floor( currentWidth / this.iview.tilesize);
		var lastTileWidth = currentWidth - fullTileCount * this.iview.tilesize;
		this.iview.zoomScale = viewerRatio;//determine the scaling ratio
		level = this.iview.zoomMax - level;
		viewerBean.tileSize = Math.floor((viewerSize - viewerRatio * lastTileWidth) / fullTileCount);
		this.iview.zoomBack = viewerBean.zoomLevel;
		viewerBean.zoom(level - viewerBean.zoomLevel);
		return true;
	}
	return false;
}

/**
 * @public
 * @function
 * @name		switchDisplayMode
 * @memberOf	iview.General
 * @description	calculates how the picture needs to be scaled so that it can be displayed within the display-area as the mode requires it
 * @param		{boolean} screenZoom defines which displaymode will be calculated
 * @param		{boolean} statebool holds the value which defines if the current mode is set or needs to be set
 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
 * @return		boolean which holds the new StateBool value, so it can be saved back into the correct variable
 */
genProto.switchDisplayMode = function(screenZoom, stateBool, preventLooping) {
	var viewerBean = this.iview.viewerBean;
	if (typeof(viewerBean)=='undefined' && typeof(console)!='undefined'){
		console.log("undefined property viewerBean");
		console.log(this);
		console.trace();
		return;
	}
	if (screenZoom) {
		this.iview.zoomWidth = false;
	} else {
		this.iview.zoomScreen = false;
	}
	stateBool = (stateBool)? false: true;
	viewerBean.clear();
	this.removeScaling();
	var preload = this.iview.my.preload;
	if (stateBool) {
		for (var i = 0; i <= this.iview.zoomMax; i++) {
			if(this.iview.currentImage.getWidth()/viewerBean.width > this.iview.currentImage.getHeight()/this.iview.my.viewer.outerHeight(true) || (stateBool && !screenZoom)){
			//Width > Height Or ZoomWidth is true
				if (this.calculateZoomProp(i, this.iview.currentImage.getWidth(), viewerBean.width, 0)) {
					break;
				}
			} else {
				if (this.calculateZoomProp(i, this.iview.currentImage.getHeight(), viewerBean.height, 0)) {
					break;
				}
			}
		}
		viewerBean.init();
	} else {
		this.iview.zoomScale = 1;
		viewerBean.tileSize = tilesize;
		viewerBean.init();
		
		//an infinite loop would arise if the repeal of the zoombar comes
		if (typeof (preventLooping) == "undefined" || preventLooping == false) {
			viewerBean.zoom(this.iview.zoomBack - viewerBean.zoomLevel);
		}
	}

	var offset = preload.offset();
	this.iview.my.barX.setCurValue(-offset.left);
	this.iview.my.barY.setCurValue(-offset.top);
	if (this.iview.useCutOut) this.iview.cutOutModel.setPos({'x':offset.left, 'y':offset.top});
	return stateBool;
}

/**
 * @public
 * @function
 * @name		pictureWidth
 * @memberOf	iview.General
 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerwidth which is smaller than the viewerwidth
 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
 */
genProto.pictureWidth = function(preventLooping){
	var bool = (typeof (preventLooping) != undefined)? preventLooping:false;
	this.iview.zoomWidth = this.switchDisplayMode(false, this.iview.zoomWidth, bool);
}

/**
 * @public
 * @function
 * @name		pictureScreen
 * @memberOf	iview.General
 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerspace which is smaller than the viewerspace
 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
 */
genProto.pictureScreen = function(preventLooping){
	var bool = (typeof (preventLooping) != undefined)? preventLooping:false;
	this.iview.zoomScreen = this.switchDisplayMode(true, this.iview.zoomScreen, bool);
}

/**
 * @public
 * @function
 * @name		scrollMove
 * @memberOf	iview.General
 * @description	loads the tiles accordingly the position of the scrollbar if they is moving
 * @param		{integer} valueX number of pixels how far the bar has been moved horizontal
 * @param		{integer} valueY number of pixels how far the bar has been moved vertical
 */
genProto.scrollMove = function(valueX, valueY) {
	this.iview.scroller = true;
	this.iview.viewerBean.positionTiles ({'x' : valueX, 'y' : valueY}, true);
	this.iview.viewerBean.notifyViewerMoved({'x' : valueX, 'y' : valueY});
	this.iview.scroller = false;
}

/**
 * @public
 * @function
 * @name		handleScrollbars
 * @memberOf	iview.General#
 * @description	adapts the scrollbars to correctly represent the new view after a zoom or resize event occured to the viewer. The adaptations cover sizing the bar, the bar proportion, maxValue and currentValue depending on the given reason
 * @param		{string} [reason] the reason why the function was called, possible values are "resize" and "zoom" or void if you want to have all adaptations to be applied
 */
genProto.handleScrollbars = function(reason) {
	if (typeof reason === "undefined") reason = "all";
	
	var viewerBean = this.iview.viewerBean;
	var viewer = this.iview.my.viewer;
	var barX = this.iview.my.barX;
	var barY = this.iview.my.barY;
	// determine the current imagesize
	var curWidth = (this.iview.currentImage.getWidth() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale;
	var curHeight = (this.iview.currentImage.getHeight() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale;

	var height = viewer.height();
	var width = viewer.width();
	var top = viewer.offset().top;
	
	// vertical bar
	var ymaxVal = curHeight - height;
	barY.setMaxValue((ymaxVal < 0)? 0:ymaxVal);
	barY.setProportion(height/curHeight);
	
	// horizontal bar
	var xmaxVal = curWidth - width;
	barX.setMaxValue(xmaxVal);
	barX.setProportion(width/curWidth);

	switch (reason) {
	case "all":
	case "zoom":
		// correctly represent the new view position
		barX.setCurValue(-viewerBean.x);
		barY.setCurValue(-viewerBean.y);
		if (!reason == "all") break;
	case "resize":
		// set the new size of the scrollbar
		barY.setSize(height - top);
		barY.my.self[0].style.top = top + "px";
		barX.setSize(width);
		if (!reason == "all") break;
	}
}

/**
 * @public
 * @function
 * @name		viewerZoomed
 * @memberOf	iview.General
 * @description	is called if the viewer is zooming; handles the correct sizing and displaying of the preloadpicture, various buttons and positioning of the cutOut accordingly the zoomlevel
 */
genProto.viewerZoomed = function (zoomEvent) {
	var viewerBean = this.iview.viewerBean;
	
	// handle special Modes, needs to close
	if (this.iview.zoomWidth) {
		this.pictureWidth(true);
	}
	if (this.iview.zoomScreen) {
		this.pictureScreen(true);
	}
	var preload = this.iview.my.preload;
	preload.css({"width": (this.iview.currentImage.getWidth() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale +  "px",
				 "height": (this.iview.currentImage.getHeight() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale + "px"});

	// Actualize forward & backward Buttons
	jQuery(".viewerContainer.min .toolbars .toolbar").css("width", (this.iview.currentImage.getWidth() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale +  "px");

	this.handleScrollbars("zoom");

	if (this.iview.useCutOut) {
		this.iview.cutOutModel.setSize({
			'x': preload.width(),
			'y': preload.height()});
		this.iview.cutOutModel.setRatio({
			'x': viewerBean.width / ((this.iview.currentImage.getWidth() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale),
			'y': viewerBean.height / ((this.iview.currentImage.getHeight() / Math.pow(2, this.iview.zoomMax - viewerBean.zoomLevel))*this.iview.zoomScale)});
		this.iview.cutOutModel.setPos({
			'x': - (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*this.iview.zoomScale,
			'y': - (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*this.iview.zoomScale});
	}
	
	// check buttons
	this.iview.getToolbarCtrl().checkZoom(viewerBean.zoomLevel);
}

/**
 * @public
 * @function
 * @name		viewerMoved
 * @memberOf	iview.General
 * @description	is called if the picture is moving in the viewer and handles the size of the cutout accordingly the size of the picture
 */
genProto.viewerMoved = function (event) {
	// calculate via zoomlevel to the preview the left top point
	var newX = - (event.x / Math.pow(2, this.iview.viewerBean.zoomLevel))/this.iview.zoomScale;
	var newY = - (event.y / Math.pow(2, this.iview.viewerBean.zoomLevel))/this.iview.zoomScale;

	if (this.iview.useCutOut) {
		this.iview.cutOutModel.setPos({'x':newX, 'y':newY});
	}
	// set Roller this no circles are created, and we end in an endless loop
	this.iview.roller = true;
	var preload = this.iview.my.preload;
	var pos = preload.position();
	this.iview.my.barX.setCurValue(-pos.left);
	this.iview.my.barY.setCurValue(-pos.top);
	this.iview.roller = false;
}

/**
 * @public
 * @function
 * @name		openChapter
 * @memberOf	iview.General
 * @description	open and close the chapterview
 * @param		{button} button to which represents the chapter in the toolbar
 */
genProto.openChapter = function(button){
	if (chapterEmbedded) {
		//alert(warnings[0])
		return;
	}
	var that = this;
	// chapter isn't created
	if (typeof this.iview.chapter === 'undefined') {
		button.setLoading(true);
		setTimeout(function(){
			var callback = function() {
				// try again openChapter (recursive call)
				that.openChapter(button);
				button.setLoading(false);
			};
			that.importChapter(callback);
		}, 10);
	} else {
		this.iview.chapter.toggleView();
	}
}

/**
 * @public
 * @function
 * @name		updateModules
 * @memberOf	iview.Thumbnails
 * @description	marks the correct picture in the chapterview and set zoombar to the correct zoomlevel
 */
genProto.updateModuls = function() {
	var viewerBean = this.iview.viewerBean;
	// align/fit scrollbars
	this.handleScrollbars();

	// Actualize forward & backward Buttons
	var previewTbView = jQuery(this.iview.getToolbarCtrl().getView("previewTbView").toolbar);
	var newTop = ((((this.iview.currentImage.getHeight() / Math.pow(2, this.iview.zoomMax - 1)) * this.iview.zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
	if (this.iview.my.container.hasClass("viewerContainer min")) {
		this.iview.my.container.find(".toolbars .toolbar").css("top", newTop);
	}
	
	try {
		//repaint Toolbar as if the width of the dropdown changes the spring needs to be adjusted
		this.iview.getToolbarCtrl().paint("mainTb");	
	} catch (e) {}
	
	// Actualize Chapter
	if (this.iview.useChapter && !(typeof this.iview.chapter === "undefined")) {
		//prevent endless loop
		this.iview.chapterReaction = true;
	}
}

/**
 * @public
 * @function
 * @name		viewerScroll
 * @memberOf	iview.General
 * @description	handles if the scrollbar was moved up or down and calls the functions to load the corresponding tiles and movement
 * @param 		{} delta
 */
genProto.viewerScroll = function(delta) {
	this.iview.viewerBean.positionTiles({'x': delta.x*PanoJS.MOVE_THROTTLE,
											'y': delta.y*PanoJS.MOVE_THROTTLE}, true);
	this.iview.viewerBean.notifyViewerMoved({'x': delta.x*PanoJS.MOVE_THROTTLE,
												'y': delta.y*PanoJS.MOVE_THROTTLE});
}

/**
 * @public
 * @function
 * @name		importCutOut
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the cutout
 */
genProto.importCutOut = function() {
	var that = this;
	this.iview.cutOutMP = new iview.cutOut.ModelProvider();
	this.iview.cutOutModel = this.iview.cutOutMP.createModel();
	this.iview.ausschnitt = new iview.cutOut.Controller(this.iview.cutOutMP, i18n);
	this.iview.ausschnitt.createView({'thumbParent': this.iview.ausschnittParent, 'dampParent': this.iview.ausschnittParent});
	this.iview.ausschnitt.attach("move.cutOut", function(e, val) {
		that.iview.viewerBean.recenter(
			{'x' : val.x["new"]*that.iview.zoomScale,
			 'y' : val.y["new"]*that.iview.zoomScale
			}, true);
	});
	var preload = this.iview.my.preload;
	this.iview.cutOutModel.setSize({
		'x': preload.width(),
		'y': preload.height()});
}

/**
 * @public
 * @function
 * @name		importChapter
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the chapter
 * @param		{function} callback function which is called just before the function returns
 */
genProto.importChapter = function(callback) {
	this.iview.ChapterModelProvider = new iview.METS.ChapterModelProvider(this.iview.metsDoc);
	
	this.iview.chapter = new iview.chapter.Controller(this.iview.ChapterModelProvider, this.iview.PhysicalModelProvider);

	this.iview.chapter.createView(this.iview.chapterParent);
	this.iview.chapterReaction = false;

	callback();
}

/**
 * @public
 * @function
 * @name		importOverview
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the overview
 * @param		{function} callback function which is called just before the function returns
 */
genProto.importOverview = function(callback) {
	var ov = new iview.overview.Controller(this.iview.PhysicalModelProvider, iview.overview.View, this.iview.viewerBean.tileUrlProvider);
	ov.createView({'mainClass':'overview', 'parent':this.iview.my.container, 'useScrollBar':true});
	this.iview.overview = ov;
	callback();
}

/**
 * @public
 * @function
 * @name		zoomViewer
 * @memberOf	iview.General
 * @description	handles the direction of zooming in the viewer
 * @param 		{boolean} direction: true = zoom in, false = zoom out
 */
genProto.zoomViewer = function(direction) {
	var viewerBean = this.iview.viewerBean;
	var dir = 0;
	if (direction) {
		//if zoomWidth or zoomScreen was active and we're already in the max zoomlevel just reset the displayMode
		if (this.iview.zoomScreen) {
			this.pictureScreen(true);
		} else if (this.iview.zoomWidth) {
			this.pictureWidth(true);
		}
		if (viewerBean.zoomLevel != this.iview.zoomMax) {
			dir = 1;
		}
	} else {
		dir = -1;
	}
	this.zoomCenter(dir, {"x":viewerBean.width/2, "y":viewerBean.height/2});
}

/**
 * @public
 * @function
 * @name		loading
 * @memberOf	iview.General
 * @description	is calling to the load-event of the window; serve for the further registration of events likewise as initiator for various objects
 */
genProto.loading = function() {
	var that = this;
	
	var cssSheet=document.getElementById("cssSheet"+this.iview.viewID);
	if (cssSheet!=null){
		//Opera fix: link css style to head to fix maximizeHandler()
		cssSheet.parentNode.removeChild(cssSheet);
		document.getElementsByTagName("head")[0].appendChild(cssSheet);
	}
	//TODO don't use the global one rather than the instance tilesize
	this.iview.startHeight = this.iview.startWidth = tilesize;
	// ScrollBars
	// horizontal
	this.iview.my.barX = new iview.scrollbar.Controller();
	var barX = this.iview.my.barX;
	barX.createView({ 'direction':'horizontal', 'parent':this.iview.my.container, 'mainClass':'scroll'});
	barX.attach("curVal.scrollbar", function(e, val) {
		if (!that.iview.roller) {
			that.scrollMove(- (val["new"]-val["old"]), 0);
		}
	});
	// vertical
	this.iview.my.barY = new iview.scrollbar.Controller();
	var barY = this.iview.my.barY;
	barY.createView({ 'direction':'vertical', 'parent':this.iview.my.container, 'mainClass':'scroll'});
	barY.attach("curVal.scrollbar", function(e, val) {
		if (!that.iview.roller) {
			that.scrollMove( 0, -(val["new"]-val["old"]));
		}
	});

	// Additional Events
	// register to scroll into the viewer
	this.iview.my.viewer.mousewheel(function(e, delta, deltaX, deltaY) {e.preventDefault(); that.viewerScroll({"x":deltaX, "y":deltaY});})
		.css({	'width':this.iview.startWidth - ((this.iview.my.barX.my.self.css("visibility") == "visible")? this.iview.my.barX.my.self.outerWidth() : 0)  + "px",
				'height':this.iview.startHeight - ((this.iview.my.barY.my.self.css("visibility") == "visible")? this.iview.my.barY.my.self.outerHeight() : 0)  + "px"
		});
	
	if (this.iview.useCutOut) {
		this.importCutOut();
	}

	// Load Page
	if (URL.getParam("page") != "") {
		//TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
		this.iview.startFile = decodeURI(URL.getParam("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/,"§"));
	}
	
	
	//remove leading '/'
	this.iview.startFile = encodeURI(this.iview.startFile.replace(/^\/*/,""));
	this.loadPage(function(){that.startFileLoaded()});
}

/**
 * @public
 * @function
 * @name		startFileLoaded
 * @memberOf	iview.General
 * @description	
 */
genProto.startFileLoaded = function(){
	this.iview.loaded = true;
	var that = this;
	//Blank needs to be loaded as blank, so the level is filled. Else it lays not ontop; needed for IE 
	this.iview.my.viewer.find(".surface").css("backgroundImage", "url(" + this.iview.webappBaseUri + "modules/iview2/gfx/blank.gif" + ")");

	// PermaLink Handling
	// choice if zoomLevel or special; zoomMode only makes sense in maximized viewer
	if (URL.getParam("maximized") == "true") {
		if (URL.getParam("tosize") == "width") {
			if (!this.iview.zoomWidth) this.pictureWidth();
		} else if (URL.getParam("tosize") == "screen") {
			if (!this.iview.zoomScreen) this.pictureScreen();
		} else if (isNaN(parseInt(URL.getParam("zoom")))){
			if (!this.iview.zoomScreen) this.pictureScreen();
		}
		
		this.maximizeHandler();
	} else {
		// in minimized viewer always pictureScreen
		if (!this.iview.zoomScreen) this.pictureScreen();
	}
	
	var metsDocURI = this.iview.webappBaseUri + "servlets/MCRMETSServlet/" + this.iview.viewID;
	jQuery.ajax({
		url: metsDocURI,
  		success: function(response) {
			that.processMETS(response);
		},
  		error: function(request, status, exception) {
  			if(typeof console != "undefined"){
  				console.log("Error Occured while Loading METS file:\n"+exception);
  			}
  		}
	});
	
	// Resize-Events registrieren
	var that = this;
	jQuery(window).resize(function() { that.reinitializeGraphic()});
	
	this.updateModuls();
}

/**
 * @public
 * @function
 * @name		processMETS
 * @memberOf	iview.General
 * @description	process the loaded mets and do all final configurations like setting the pagenumber, generating Chapter and so on
 * @param		{document} metsDoc holds in METS/MODS structure all needed informations to generate an chapter and overview of of the supplied data
 */
genProto.processMETS = function(metsDoc) {
	var that = this;
	this.iview.metsDoc = metsDoc;
	//create the PhysicalModelProvider
	this.iview.PhysicalModelProvider = new iview.METS.PhysicalModelProvider(metsDoc);
	this.iview.PhysicalModel = this.iview.PhysicalModelProvider.createModel();
	var physicalModel = this.iview.PhysicalModel;
	var toolbarCtrl = this.iview.getToolbarCtrl();
	this.iview.amountPages = physicalModel.getNumberOfPages();
	physicalModel.setPosition(physicalModel.getPosition(this.iview.currentImage.getName()));
	jQuery(physicalModel).bind("select.METS", function(e, val) {
//			that.notifyListenerNavigate(val["new"]);
		that.loadPage();
		toolbarCtrl.checkNavigation(val["new"]);
		that.updateModuls();
		if (jQuery('.navigateHandles .pageBox')[0]) {
			toolbarCtrl.updateDropDown(jQuery(pagelist.find("a")[val["new"] - 1]).html());
		}
	})

	// Toolbar Operation
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openChapter");
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openOverview");
	toolbarCtrl.checkNavigation(this.iview.PhysicalModel.getCurPos());

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
	this.iview.my.container.find(".toolbars").append(pagelist);

	// if METS File is loaded after the drop-down-menu (in mainToolbar) its content needs to be updated
	if (jQuery('.navigateHandles .pageBox')[0]) {
		jQuery(toolbarCtrl.getView('mainTbView')).trigger("new", {'elementName' : "pageBox", 'parentName' : "navigateHandles", 'view' : this.iview.my.container.find('.navigateHandles .pageBox')});
		// switch to current content
		toolbarCtrl.updateDropDown(jQuery(pagelist.find("a")[physicalModel.getCurPos() - 1]).html());
	}
	//at other positions Opera doesn't get it correctly (although it still doesn't look that smooth as in other browsers) 
	//TODO needs to be adapted to work correctly with the new structure
	window.setTimeout("Iview['"+this.iview.viewID+"'].toolbarCtrl.paint('mainTb')",10);
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
}
