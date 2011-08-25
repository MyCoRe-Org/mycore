//TODO Preload größe anhand der von den Kacheln bestimmen

/**
 * @public
 * @function
 * @name		loadPage
 * @memberOf	iview.General
 * @description	reads out the imageinfo.xml, set the correct zoomvlues and loads the page
 * @param		{function} callback
 * @param   {String} [startFile] optional page to open
 */
genProto.loadPage = function(callback, startFile) {
	var url;
	if (typeof(this.iview.metsDoc)=='undefined'){
		url = startFile;
	} else {
		url = this.iview.PhysicalModel.getCurrent().getHref();
	}
	this.iview.currentImage.name = url;
	var imagePropertiesURL = this.iview.properties.baseUri[0]+"/"+this.iview.properties.derivateId+"/"+url+"/imageinfo.xml";
	var that = this;
	jQuery.ajax({
		url: imagePropertiesURL,
  		success: function(response) {
  		  that.processImageProperties(response, url);
  		  callBack(callback);
  		},
  		error: function(request, status, exception) {
  			if(console){
  				console.log("Error occured while loading image properties:\n"+exception);
  			}
  		}
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
genProto.processImageProperties = function(imageProperties, url){
	this.iview.currentImage.processImageProperties(imageProperties, url);
	var viewerBean = this.iview.viewerBean;
	
	// checks for enabled Modi & reset before
  //TODO: check if zoomInit is still needed
	this.iview.currentImage.zoomInfo.zoomInit = Math.min(viewerBean.zoomLevel,this.iview.currentImage.zoomInfo.maxZoom);
	var thumbSource=viewerBean.tileUrlProvider.assembleUrl(0,0,0);
	
	var preload = new Image();
	preload.className = "preloadImg";
	var preloadCont=this.iview.context.preload;
	preloadCont.css({"width" : this.iview.currentImage.width / Math.pow(2, this.iview.currentImage.zoomInfo.maxZoom - this.iview.currentImage.zoomInfo.zoomInit) + "px",
					 "height" : this.iview.currentImage.height / Math.pow(2, this.iview.currentImage.zoomInfo.maxZoom - this.iview.currentImage.zoomInfo.zoomInit) + "px"})
			 .empty()
			 .append(preload);
	preload.src = thumbSource;
	preload.style.width = "100%";
	preload.style.height = "100%";
	
	viewerBean.resize();
	// moves viewer to zoomLevel zoomInit
	viewerBean.maxZoomLevel = this.iview.currentImage.zoomInfo.maxZoom;
	// handle special Modi for new Page
	if (this.iview.currentImage.zoomInfo.zoomWidth) {
		this.iview.currentImage.zoomInfo.zoomWidth=false;
		viewerBean.pictureWidth();
	} else if (this.iview.currentImage.zoomInfo.zoomScreen) {
		this.iview.currentImage.zoomInfo.zoomScreen=false;
		viewerBean.pictureScreen();
	} else {
		// moves viewer to zoomLevel zoomInit
		viewerBean.zoom(this.iview.currentImage.zoomInfo.zoomInit - viewerBean.zoomLevel);
	}
	
	// damit das alte zoomBack bei Modi-Austritt nicht verwendet wird
	this.iview.currentImage.zoomInfo.zoomBack = this.iview.currentImage.zoomInfo.zoomInit;
	var initX = this.iview.properties.useParam ? toFloat(URL.getParam("x")) : 0;
  var initY = this.iview.properties.useParam ? toFloat(URL.getParam("y")) : 0;
	
	this.iview.roller = true;
	viewerBean.positionTiles ({'x' : initX, 'y' : initY}, true);
	
  this.updateModuls();
	
	this.iview.roller = false;
};

/**
 * @public
 * @function
 * @name		openThumbnailPanel
 * @memberOf	iview.General
 * @description	blend in the ThumbnailPanel and creates it by the first call
 * @param		{button} button to which represents the ThumbnailPanel in the toolbar
 */
genProto.openThumbnailPanel = function(button) {
	var that = this;
	// check if ThumbnailPanel was created yet
	if (typeof this.iview.thumbnailPanel === 'undefined') {
		button.setLoading(true);
		setTimeout(function(){
			var callback = function() {
				// try again openThumbnailPanel (recursive call)
				that.openThumbnailPanel(button);
				button.setLoading(false);
				
				that.iview.thumbnailPanel.attach("click.thumbnailPanel", function(e, val) {
					// type 1: click on ThumbnailPanel div
					button.setSubtypeState(false);
				});
			};
			that.importThumbnailPanel(callback);
		}, 10);
	} else {
		this.iview.thumbnailPanel.toggleView();
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
	};

	this.getPermalinkCtrl().addView(new iview.Permalink.View("permalinkView", this.iview.viewerContainer));
	callback();
};

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
	var viewer = this.iview.context.viewer;
	var barX = this.iview.scrollbars.x;
	var barY = this.iview.scrollbars.y;
	var currentImage = this.iview.currentImage;
	// determine the current imagesize
	var zoomScale = currentImage.zoomInfo.scale;
	var curWidth = (currentImage.width / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomScale;
	var curHeight = (currentImage.height / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomScale;

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
};

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
	if (this.iview.chapter.loaded) {
		this.iview.chapter.toggleView();
	} else {
		button.setLoading(true);
		setTimeout(function(){
			var callback = function() {
				// try again openChapter (recursive call)
				that.openChapter(button);
				button.setLoading(false);
			};
			that.importChapter(callback);
		}, 10);
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
  if (this.iview.properties.maximized){
    // align/fit scrollbars
    this.handleScrollbars();
    try {
      //repaint Toolbar as if the width of the dropdown changes the spring needs to be adjusted
      this.iview.toolbar.ctrl.paint("mainTb");	
    } catch (e) {}
  } else {
    //TODO: align image and toolbar to the center
//    var previewTbView = jQuery(this.iview.getToolbarCtrl().getView("previewTbView").toolbar);
//    var currentImage = this.iview.currentImage;
//    var zoomScale=currentImage.zoomInfo.getScale();
//    var newTop = ((((currentImage.getHeight() / Math.pow(2, currentImage.zoomInfo.getMaxLevel() - 1)) * zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
//    if (this.iview.my.container.hasClass("viewerContainer min")) {
//      this.iview.getToolbarCtrl().toolbarContainer.find(".toolbar").css("top", newTop);
//    }
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
 * @name		importChapter
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the chapter
 * @param		{function} callback function which is called just before the function returns
 */
genProto.importChapter = function(callback) {
	this.iview.ChapterModelProvider = new iview.METS.ChapterModelProvider(this.iview.metsDoc);
	
	this.iview.chapter = jQuery.extend(this.iview.chapter, new iview.chapter.Controller(this.iview.ChapterModelProvider, this.iview.PhysicalModelProvider));

	this.iview.chapter.createView(this.iview.chapter.parent);
	this.iview.chapter.loaded = true;//signal that the chapter was loaded successfully
	callback();
};

/**
 * @public
 * @function
 * @name		importThumbnailPanel
 * @memberOf	iview.General
 * @description	calls the corresponding functions to create the ThumbnailPanel
 * @param		{function} callback function which is called just before the function returns
 */
genProto.importThumbnailPanel = function(callback) {
	var thumbnailPanel = new iview.ThumbnailPanel.Controller(this.iview.PhysicalModelProvider, iview.ThumbnailPanel.View, this.iview.viewerBean.tileUrlProvider);
	thumbnailPanel.createView({'mainClass':'thumbnailPanel', 'parent':this.iview.context.container, 'useScrollBar':true});
	this.iview.thumbnailPanel = thumbnailPanel;
	jQuery(this.iview.viewerContainer).bind("minimize.viewerContainer", function() {
		//close ThumbnailPanel when Viewer is going to minimized mode
		thumbnailPanel.hideView();
	})
	callback();
}

/**
 * @public
 * @function
 * @name		startFileLoaded
 * @memberOf	iview.General
 * @description	
 */
genProto.startFileLoaded = function(){
	var that = this;
	//Blank needs to be loaded as blank, so the level is filled. Else it lays not ontop; needed for IE 
	this.iview.context.viewer.find(".surface").css("backgroundImage", "url(" + this.iview.properties.webappBaseUri + "modules/iview2/gfx/blank.gif" + ")");

	// PermaLink Handling
	// choice if zoomLevel or special; zoomMode only makes sense in maximized viewer
	if (this.iview.properties.useParam && URL.getParam("maximized") == "true") {
		if (URL.getParam("tosize") == "width") {
			if (!this.iview.currentImage.zoomInfo.zoomWidth) this.iview.viewerBean.pictureWidth();
		} else if ((URL.getParam("tosize") == "screen" || isNaN(parseInt(URL.getParam("zoom"))))
				&& !this.iview.currentImage.zoomInfo.zoomScreen) {
			this.iview.viewerBean.pictureScreen();
		} else if (isNaN(parseInt(URL.getParam("zoom"))) && !this.iview.currentImage.zoomInfo.zoomScreen){
			this.iview.viewerBean.pictureScreen();
		}
		//Toolbar is initialized on dom-load event and may not yet ready
	  var waitForToolbar = function (self, iviewInst){
	    if (iviewInst.properties.initialized){
	      iviewInst.gen.maximizeHandler();
	    } else {
	      setTimeout(function(){self(self,iviewInst);}, 100);
	    }
	  };
	  waitForToolbar(waitForToolbar, this.iview);
	} else {
		// in minimized viewer always pictureScreen
		if (!this.iview.currentImage.zoomInfo.zoomScreen) this.iview.viewerBean.pictureScreen();
	}
	
	var metsDocURI = this.iview.properties.webappBaseUri + "servlets/MCRMETSServlet/" + this.iview.properties.derivateId;
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
 * @param		{document} metsDoc holds in METS/MODS structure all needed informations to generate an chapter and ThumbnailPanel of of the supplied data
 */
genProto.processMETS = function(metsDoc) {
	var that = this;
	this.iview.metsDoc = metsDoc;
	//create the PhysicalModelProvider
	this.iview.PhysicalModelProvider = new iview.METS.PhysicalModelProvider(metsDoc);
	this.iview.PhysicalModel = this.iview.PhysicalModelProvider.createModel();
	var physicalModel = this.iview.PhysicalModel;
	var toolbarCtrl = this.iview.toolbar.ctrl;
	this.iview.amountPages = physicalModel.getNumberOfPages();
	physicalModel.setPosition(physicalModel.getPosition(this.iview.currentImage.name));
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
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openThumbnailPanel");
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
	toolbarCtrl.toolbarContainer.append(pagelist);

	// if METS File is loaded after the drop-down-menu (in mainToolbar) its content needs to be updated
	if (jQuery('.navigateHandles .pageBox')[0]) {
		jQuery(toolbarCtrl.views['mainTbView']).trigger("new", {'elementName' : "pageBox", 'parentName' : "navigateHandles", 'view' : this.iview.context.container.find('.navigateHandles .pageBox')});
		// switch to current content
		toolbarCtrl.updateDropDown(jQuery(pagelist.find("a")[physicalModel.getCurPos() - 1]).html());
	}
	//at other positions Opera doesn't get it correctly (although it still doesn't look that smooth as in other browsers) 
	//TODO needs to be adapted to work correctly with the new structure
	window.setTimeout(function() {
    	toolbarCtrl.paint('mainTb');
  }, 10);
};

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
