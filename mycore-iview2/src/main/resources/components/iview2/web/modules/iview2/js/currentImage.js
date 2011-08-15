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

    constructor.prototype.processImageProperties = function ci_processImageProperties(imageProperties, name) {
      var values = nodeAttributes(imageProperties.getElementsByTagName("imageinfo")[0]);
      // this.iview.tiles = parseInt(values['tiles']);
      this.name = name;
      this.width = parseInt(values['width']);
      this.height = parseInt(values['height']);
      this.zoomInfo.maxZoom = parseInt(values['zoomLevel']);
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
    return constructor;
  })();
})();
