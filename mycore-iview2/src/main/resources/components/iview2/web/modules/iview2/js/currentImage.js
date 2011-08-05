(function() {
  "use strict";

  iview.CurrentImage = (function() {

    function constructor(iviewInst) {
      this.name = null;
      this.height = 0;
      this.width = 0;
      this.zoomInfo = new iview.ZoomInformation();
      iview.IViewObject.call(this, iviewInst);
      jQuery(this).bind(iview.CurrentImage.CHANGE_EVENT, function(event) {
        log("Catched event: " + iview.CurrentImage.CHANGE_EVENT);
        log(this);
        log(event);
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
    constructor.prototype.processImageProperties = function ci_processImageProperties(imageProperties, name) {
      var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
      // this.iview.tiles = parseInt(values['tiles']);
      this.setName(name);
      this.setWidth(parseInt(values['width']));
      this.setHeight(parseInt(values['height']));
      this.zoomInfo.setMaxLevel(parseInt(values['zoomLevel']));
      jQuery(this).trigger(iview.CurrentImage.CHANGE_EVENT);
    };

    return constructor;

  })();
  iview.CurrentImage.CHANGE_EVENT = "imageChanged";

  iview.ZoomInformation = (function() {
    function constructor() {
      this.maxZoom = 0;
      this.scale = 1;
    }
    constructor.prototype = {
      getMaxLevel : function zi_getMaxLevel() {
        return this.maxZoom;
      },
      setMaxLevel : function zi_setMaxLevel(maxZoom) {
        return this.maxZoom = maxZoom;
      },
      getScale : function zi_getScale() {
        return this.scale;
      },
      setScale : function zi_setScale(scale) {
        return this.scale = scale;
      }
    };
    return constructor;
  })();
})();
