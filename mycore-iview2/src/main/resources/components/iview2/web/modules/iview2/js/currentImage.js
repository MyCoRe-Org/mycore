(function() {
  "use strict";

  iview.CurrentImage = (function() {

    function constructor(iviewInst) {
      this.name = null;
      this.height = 0;
      this.width = 0;
      this.x = 0;
      this.y = 0;
	  this.curWidth = 0;
      this.curHeight = 0;
      this.viewer = iviewInst;
      var zoomInfo = this.zoomInfo = new iview.ZoomInformation(iviewInst);
      var that = this;
      this.rotation = 0;
      
      jQuery(iviewInst.viewerContainer).bind("zoom.viewer", function(jq, event) {
    	  /*listen to changes of zoomLevel and adapt curWidth & -Height depending on that,
    	   * notify all listeners about the change*/
    	that.zoomInfo.curZoom = event.zoomLevel
		that.curWidth = (that.width / Math.pow(2, that.zoomInfo.maxZoom - event.zoomLevel))*that.zoomInfo.scale,
		that.curHeight = (that.height / Math.pow(2, that.zoomInfo.maxZoom - event.zoomLevel))*that.zoomInfo.scale
		jQuery(that).trigger(iview.CurrentImage.DIMENSION_EVENT);
      });
      
      iview.IViewObject.call(this, iviewInst);
      jQuery(this).bind(iview.CurrentImage.CHANGE_EVENT, function(event) {
        log("Catched event: " + iview.CurrentImage.CHANGE_EVENT);
        log(this);
        log(event);
      });
    }

    constructor.prototype = Object.create(iview.IViewObject.prototype);

    constructor.prototype.processImageProperties = function ci_processImageProperties(imageProperties, name) {
      var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
      // this.iview.tiles = parseInt(values['tiles']);
      this.name = name;
      this.width = parseInt(values['width']);
      this.height = parseInt(values['height']);
      this.zoomInfo.maxZoom = parseInt(values['zoomLevel']);
      //TODO: check if zoomInit is still needed
      this.zoomInfo.zoomInit = Math.min(this.viewer.viewerBean.zoomLevel,this.zoomInfo.maxZoom);
      jQuery(this).trigger(iview.CurrentImage.CHANGE_EVENT);
    };
    
    constructor.prototype.setPos = function ci_setPos(position) {
    	this.x = position.x;
    	this.y = position.y;
    	jQuery(this).trigger(iview.CurrentImage.POS_CHANGE_EVENT);
    }
    
    return constructor;

  })();
  iview.CurrentImage.CHANGE_EVENT = "imageChanged";
  iview.CurrentImage.POS_CHANGE_EVENT = "positionChanged";
  iview.CurrentImage.DIMENSION_EVENT = "dimensionChanged";

  iview.ZoomInformation = (function() {
    function constructor() {
      this.maxZoom = 0;
      this.scale = 1;
      this.zoomBack = 0;
      this.zoomScreen = 0;
      this.zoomWidth = 0;
      this.zoomInit = 0;
      this.curZoom = 0;
    }
    
    return constructor;
  })();
})();
