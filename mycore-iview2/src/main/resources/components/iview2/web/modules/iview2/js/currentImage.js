(function() {
  "use strict";

  iview.CurrentImage = (function() {

    function constructor(iviewInst) {
      this.name = null;
      this.height = 0;
      this.width = 0;
      this.maxZoom = 0;
      iview.IViewObject.call(this, iviewInst);
      jQuery(this).bind(iview.CurrentImage.CHANGE_EVENT, function(event) {
        if (console && console.log) {
          console.log("Catched event: " + iview.CurrentImage.CHANGE_EVENT);
          console.log(this);
          console.log(event);
        }
      });
    }

    constructor.prototype = Object.create(iview.IViewObject.prototype);

    constructor.prototype.getName = function ci_getName() {
      return this.name;
    };
    constructor.prototype.setName = function ci_setName(name) {
      var oldValue = this.name;
      this.name = name;
      return oldValue;
    };
    constructor.prototype.getWidth = function ci_getWidth() {
      return this.width;
    };
    constructor.prototype.getHeight = function ci_getHeight() {
      return this.height;
    };
    constructor.prototype.setWidth = function ci_setWidth(width) {
      this.width = width;
    };
    constructor.prototype.setHeight = function ci_setHeight(height) {
      this.height = height;
    };
    constructor.prototype.getMaxZoomLevel = function ci_getMaxZoomLevel() {
      return this.maxZoom;
    };
    constructor.prototype.setMaxZoomLevel = function ci_setMaxZoomLevel(maxZoom) {
      return this.maxZoom = maxZoom;
    };
    constructor.prototype.processImageProperties = function ci_processImageProperties(imageProperties, name) {
      var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
      // this.iview.tiles = parseInt(values['tiles']);
      this.setName(name);
      this.setWidth(parseInt(values['width']));
      this.setHeight(parseInt(values['height']));
      this.setMaxZoomLevel(parseInt(values['zoomLevel']));
      jQuery(this).trigger(iview.CurrentImage.CHANGE_EVENT);
    };

    return constructor;

  })();
  iview.CurrentImage.CHANGE_EVENT = "imageChanged";
})();

