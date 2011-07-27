(function() {
  "use strict";

  iview.IViewInstance = (function() {
    "use strict";
    function constructor(derivateId, container, options) {
      if (typeof options === "undefined") {
        options = {};
      }
      //TODO copy options to properties
      for (var prop in options) {
        this[prop] = options[prop];
      }
      this.derivateId = derivateId;
      this.viewerContainer = container;
      this.ausschnittParent = container; // TODO: get rid of this
      this.chapterParent = container; // TODO: get rid of this
      this.preload = container.find(".preload"); //TODO: move this somewhere
      this.gen = new iview.General(this);
      this.ToolbarImporter = new ToolbarImporter(this, i18n);
    }
    
    constructor.prototype.startViewer = function ii_startViewer(startFile){
      this.started = true; //TODO: check if still necessary
      // Load Page
      if (URL.getParam("page") != "") {
        //TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
        startFile = decodeURI(URL.getParam("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/,"§"));
      }
      //remove leading '/'
      startFile = encodeURI(startFile.replace(/^\/*/,""));
      this.gen.loading(startFile);
    };
    
    return constructor;
  })();
})();
