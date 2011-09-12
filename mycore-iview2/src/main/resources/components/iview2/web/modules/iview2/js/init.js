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
	this.iview.context = new iview.Context(iviewInst.viewerContainer, iviewInst);
	this.iview.currentImage = new iview.CurrentImage(iviewInst);
	var that = this;
};

var genProto = iview.General.prototype;

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
	var barX = this.iview.scrollbars.x;
	var barY = this.iview.scrollbars.y;

	if (jQuery(viewerContainer).hasClass("max")) {
		//to grant usage of the complete height it's not possible to simply use height:100%
		viewerContainer.css({'height': curHeight - viewerContainer.offset().top + "px",
							'width': curWidth + "px"});
		viewer.css({'height': curHeight - viewer.offset().top - barX.my.self.outerHeight()  + "px",
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
	
	  //TODO: align image and toolbar to the center
};
