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
      this.toolbarMgr = new ToolbarManager();
      this.toolbarCtrl = new ToolbarController(this);
      //TODO: load in jQuery(document).load() so that all resources are ready
      // entweder Mgr macht alles und Übergabe des related... (Modelprovider) oder Models kümmern sich untereinander und schöne Form (siehe unten)
      // Iview[viewID].getToolbarCtrl() oder Iview[viewID].toolbarCtrl verwenden?
      // vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?
      
      // Toolbar Manager
      this.toolbarMgr.addModel(new PreviewToolbarModelProvider("previewTb").getModel());
      // Toolbar Controller
      this.toolbarCtrl.addView(new ToolbarView("previewTbView", this.toolbarCtrl.toolbarContainer, i18n));
      
      // holt alle bisherigen Models in den Controller und setzt diese entsprechend um
      this.toolbarCtrl.catchModels();
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
    
    constructor.prototype.getToolbarMgr = function ii_getToolbarMgr(){
      return this.toolbarMgr;
    };
    
    constructor.prototype.getToolbarCtrl = function ii_getToolbarCtrl(){
      return this.toolbarCtrl;
    };
    
    return constructor;
  })();
})();
