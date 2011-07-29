(function() {
  "use strict";

  iview.IviewInstanceError = (function() {
    function constructor() {
      Error.apply(this, arguments);
      this.message = arguments[0];
      this.instance = arguments.length > 1 ? arguments[1] : null;
      for ( var a in this) {
        console.log("name:" + a);
        console.log(this[a]);
      }
    }
    constructor.prototype = new Error();
    constructor.prototype.constructor = constructor;
    constructor.prototype.name = "IviewInstanceError";
    return constructor;
  })();

  iview.IViewInstance = (function() {
    function constructor(container, options) {
      var defaultOpts = {
        "useChapter" : true,
        "useCutOut" : true,
        "useOverview" : true,
        "maximized" : false,
        "zoomWidth" : false,
        "zoomScreen" : false,
        "tileSize" : 256,
        "startHeight" : 256,
        "startWidth" : 256
      };
      this.properties = new iview.Properties(defaultOpts);
      this.properties.set(options);
      //check options
      if (typeof this.properties.derivateId === "undefined") {
        throw new iview.IviewInstanceError("No derivateId defined.", this);
      }
      //passed
      var paramDerId=URL.getParam("derivateId");
      this.properties.useParam = false;
      if (Iview[this.properties.derivateId].length === 0) {
        var cnt=0;
        for (var derId in Iview){
          cnt++;
        }
        this.properties.useParam = (paramDerId === this.properties.derivateId) || (paramDerId.length === 0 && cnt === 1);
      }
      this.initialized = false;
      this.viewerContainer = container;
      this.ausschnittParent = container; // TODO: get rid of this
      this.chapterParent = container; // TODO: get rid of this
      this.preload = container.find(".preload"); // TODO: move this somewhere
      this.gen = new iview.General(this);
      this.toolbarMgr = new ToolbarManager();
      this.toolbarCtrl = new ToolbarController(this);
      // load toolbar after all resources (css, images) are ready
      var that = this;
      jQuery(window).load(function ii_initToolbars() {
        // entweder Mgr macht alles und Übergabe des related... (Modelprovider) oder Models kümmern sich untereinander und schöne Form
        // (siehe unten)
        // Iview[viewID].getToolbarCtrl() oder Iview[viewID].toolbarCtrl verwenden?
        // vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?

        // Toolbar Manager
        that.toolbarMgr.addModel(new PreviewToolbarModelProvider("previewTb").getModel());
        // Toolbar Controller
        that.toolbarCtrl.addView(new ToolbarView("previewTbView", that.toolbarCtrl.toolbarContainer, i18n));

        // holt alle bisherigen Models in den Controller und setzt diese entsprechend um
        that.toolbarCtrl.catchModels();
        that.initialized = true;
      });
    }

    constructor.prototype.startViewer = function ii_startViewer(startFile) {
      this.started = true; // TODO: check if still necessary
      // Load Page
      if (URL.getParam("page") != "") {
        // TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
        startFile = decodeURI(URL.getParam("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/, "§"));
      }
      // remove leading '/'
      startFile = encodeURI(startFile.replace(/^\/*/, ""));
      this.gen.loading(startFile);
    };

    constructor.prototype.getToolbarMgr = function ii_getToolbarMgr() {
      return this.toolbarMgr;
    };

    constructor.prototype.getToolbarCtrl = function ii_getToolbarCtrl() {
      return this.toolbarCtrl;
    };

    return constructor;
  })();
})();
