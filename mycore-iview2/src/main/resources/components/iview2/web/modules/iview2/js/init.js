var iview = iview || {}; // holds API
var Iview = Iview || {}; // holds instances

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
 * @memberOf iview
 * @description	adjusts image css style properties so that image is resized with correct aspect ration
 * @param		{object} image Object
 * @param		{string} max-width (css-value)
 * @param		{string} max-height (css-value)
 */
iview.resizeImage = function (img, width, height) {
	if (img.height > img.width) {
		img.style.height = height;
		img.style.width = "auto";
	} else {
		img.style.height = "auto";
		img.style.width = width;
	}
};

/**
 * @class
 * @constructor
 * @memberOf	iview.General
 * @name		General
 * @description All Viewer data and functions which don't fit in other packages
 */
iview.General = function(iviewInst) {
	//TODO later it should be possible to remove all this.iview with just this
	this.iview = iviewInst;
	//structure for all Viewer DOM-Objects
	this.iview.context = new iview.Context(iviewInst.viewerContainer);
	this.iview.currentImage = new iview.CurrentImage(iviewInst);
	this.inputHandlerEnabled=true;
};

var genProto = iview.General.prototype;

/**
 * @function
 * @memberOf iview.General
 * @name isInputHandlerEnabled
 * @returns true if input events (keyboard, mouse) are captured
 */
genProto.isInputHandlerEnabled = function() {
  return this.inputHandlerEnabled;
};

/**
 * @function
 * @memberOf iview.General
 * @name disableInputHandler
 * @description disable input events
 */
genProto.disableInputHandler = function() {
  this.inputHandlerEnabled=false;
};

/**
 * @function
 * @memberOf iview.General
 * @name enableInputHandler
 * @description enable input events
 */
genProto.enableInputHandler = function() {
  this.inputHandlerEnabled=true;
};

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
};
/*
 * returns the URL of all tileimages
 */
PanoJS.TileUrlProvider.prototype.assembleUrl = function(xIndex, yIndex, zoom, image){
	image=(image == null)? this.getCurrentImage().name : image;
    return this.baseUri[(this.getImageHash(image)+xIndex+yIndex) % this.baseUri.length] + '/'+ this.derivate+'/' + 
        image + '/' + zoom + '/' + yIndex + '/' + xIndex + '.' + this.extension +
        (PanoJS.REVISION_FLAG ? '?r=' + PanoJS.REVISION_FLAG : '');
};

/**
 * @public
 * @function
 * @name		initializeGraphic
 * @memberOf	iview.General
 * @description	here some important values and listener are set correctly, calculate simple image name hash value to spread request over different servers and initialise the viewer
 */
genProto.initializeGraphic = function() {
	this.iview.images = [];
	PanoJS.USE_SLIDE = false;
	PanoJS.USE_LOADER_IMAGE = false;
	PanoJS.MOVE_THROTTLE = 10;
	PanoJS.BLANK_TILE_IMAGE = "../modules/iview2/" + styleFolderUri + 'blank.gif';
	
	// opera triggers the onload twice
	var iviewTileUrlProvider = new PanoJS.TileUrlProvider(this.iview.properties.baseUri, this.iview.currentImage.name, 'jpg');
	iviewTileUrlProvider.derivate = this.iview.properties.derivateId;
	var that = this;
	iviewTileUrlProvider.getCurrentImage = function initializeGraphic_getCurrentImage(){
	  return that.iview.currentImage;
	};

	/**
   * initialise the viewer
   */
	if (this.iview.viewerBean == null) {
		this.iview.viewerBean = new PanoJS(this.iview.context.viewer[0], {
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: this.iview.properties.tileSize,//Kachelgroesse
			tileUrlProvider: iviewTileUrlProvider,
			maxZoom: this.iview.currentImage.zoomInfo.maxZoom,
			initialZoom: this.iview.currentImage.zoomInfo.zoomInit,//Anfangs-Zoomlevel
			loadingTile: "../modules/iview2/" + styleFolderUri + 'blank.gif'
		});

		this.iview.viewerBean.iview = this.iview;//handle Viewer informations so PanoJS can work with it

		this.iview.viewerBean.init();
		
		this.reinitializeGraphic(function() {jQuery(that.iview.viewerBean.viewer).trigger("init.viewer");});
	}
};

/**
 * @public
 * @function
 * @name		reinitializeGraphic
 * @memberOf	iview.General
 * @param		{function} callback which is called just before the event reinit.viewer is triggered
 * @description	is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus (i.e. scrrenWidth)
 */
genProto.reinitializeGraphic = function(callback) {
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

	var viewerContainer = this.iview.context.container;
	var viewer = this.iview.context.viewer;
	var barX=this.iview.scrollbars.barX;
	var barY=this.iview.scrollbars.barY;

	if (this.iview.properties.maximized == true) {
		//to grant usage of the complete height it's not possible to simply use height:100%
		viewerContainer.css({'height': curHeight - viewerContainer.offset().top + "px",
							'width': curWidth + "px"});
		viewer.css({'height': curHeight - viewer.parent().offset().top - barX.my.self.outerHeight()  + "px",
					'width': curWidth - barY.my.self.outerWidth()  + "px"});
	} else {
		//restore minimized size settings
		viewerContainer.css({'height': this.iview.properties.startHeight + "px",
							'width': this.iview.properties.startWidth + "px"});
		viewer.css({'height': this.iview.properties.startHeight - ((barY.my.self.css("visibility") == "visible")? barY.my.self.outerHeight() : 0)  + "px",
					'width': this.iview.properties.startWidth - ((barX.my.self.css("visibility") == "visible")? barX.my.self.outerWidth() : 0)  + "px"});
	}
	
	viewerBean.width = viewer.outerWidth();
	viewerBean.height = viewer.outerHeight();
	viewerBean.resize();
	
	// den Modus beibehalten & aktualisieren
	if(this.iview.currentImage.zoomInfo.zoomScreen){
		this.iview.currentImage.zoomInfo.zoomScreen = !this.iview.currentImage.zoomInfo.zoomScreen;	
		viewerBean.pictureScreen();
	} else if(this.iview.currentImage.zoomInfo.zoomWidth){
		this.iview.currentImage.zoomInfo.zoomWidth = !this.iview.currentImage.zoomInfo.zoomWidth;
		viewerBean.pictureWidth();
	}
	
	if (this.iview.properties.useThumbnailPanel && this.iview.thumbnailPanel && this.iview.thumbnailPanel.getActive()) {
		// actualize thumbnailPanel only if visible else delay it upto the reopening
		this.iview.thumbnailPanel.setSelected(this.iview.PhysicalModel.getCurPos());
	}
	
	this.handleScrollbars("resize");
	
	if (typeof arguments[0] === "function") {
		arguments[0]();
	}
	//notify all listeners that the viewer was modified in such way that they possibly need adaptation of their own view
	jQuery(this.iview.viewerBean.viewer).trigger("reinit.viewer");
	
	// Actualize forward & backward Buttons
//	if (!this.iview.maximized) {
	  //TODO: align image and toolbar to the center
	  //TODO: compare redundant code with gen.updateModuls()
	  //var previewTbView = jQuery(this.iview.getToolbarCtrl().getView("previewTbView").toolbar);
	  //var newTop = ((((this.iview.currentImage.getHeight() / Math.pow(2, this.iview.currentImage.zoomInfo.getMaxLevel() - 1)) * this.iview.currentImage.zoomInfo.getScale()) - (previewTbView.height() + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
		//this.iview.getToolbarCtrl().toolbarContainer.find(".toolbar").css("top", newTop);
//	}
};

/**
 * @public
 * @function
 * @name	maximizeHandler
 * @memberOf	iview.General
 * @description	maximize and show the viewer with the related image or minimize and close the viewer
 */
genProto.maximizeHandler = function() {
	if (this.iview.properties.maximized) {
		if (URL.getParam("jumpback") == "true"){
			history.back();
			return;
		}
		this.iview.properties.maximized = false;
		jQuery(this.iview.viewerContainer).trigger("minimize.viewerContainer");
		
		// append viewer to dom again
		this.iview.context.switchContext();

		if (!this.iview.currentImage.zoomInfo.zoomScreen) {
			this.pictureScreen();
		}
	} else {
		this.iview.properties.maximized = true;
		jQuery(this.iview.viewerContainer).trigger("maximize.viewerContainer");
		
		// save document content
		this.iview.context.switchContext();

	}

	/*IE causes resize already at class change (mostly because position: rel <> fix)
	 IE runs resize multiple times...but without this line he doesn't...*/
	this.reinitializeGraphic();
};
