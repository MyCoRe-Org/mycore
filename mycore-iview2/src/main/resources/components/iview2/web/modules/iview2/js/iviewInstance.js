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
  
  iview.addInstance = function(iViewInst){
    var derivateId=iViewInst.properties.derivateId;
    if (typeof Iview[derivateId] ==="undefined"){
      Iview[derivateId] = [];
    }
    return Iview[derivateId].push(iViewInst);
  };

  iview.IViewInstance = (function() {
    function constructor(container, options) {
      var defaultOpts = {
        "useChapter" : true,
        "useOverview" : true,
        "useThumbnailPanel" : true,
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
      var paramDerId=URL.getParam("derivate");
      this.properties.useParam = false;
      if (typeof Iview[this.properties.derivateId] === "undefined") {
        var first=true;
        for (var derId in Iview){
          first=false;
          break;
        }
        this.properties.useParam = (paramDerId === this.properties.derivateId) || (paramDerId.length === 0 && first);
      }
      this.initialized = false;
      this.viewerContainer = container;
      this.overview = jQuery.extend(this.overview || {}, {'loaded': (this.overview || {}).loaded || false,  'parent': container});
      this.chapter = jQuery.extend(this.chapter | {}, {'loaded': (this.chapter || {}).loaded || false, 'parent': container});
      this.preload = container.find(".preload"); // TODO: move this somewhere
      this.gen = new iview.General(this);
      //TODO load toolbar after all resources (css, images) are ready
      var that = this;
      jQuery(this.viewerContainer)
      	.bind("maximize.viewerContainer", function() {
      		that.toolbarCtrl.addView(new ToolbarView("mainTbView", that.toolbarCtrl.toolbarContainer, i18n));
    		that.toolbarMgr.addModel(new StandardToolbarModelProvider("mainTb", that).getModel());
    		if (that.PhysicalModel) {
    			that.toolbarCtrl.checkNavigation(that.PhysicalModel.getCurPos());
    		}
    		that.toolbarCtrl.paint("mainTb");
			if (that.currentImage.zoomInfo.zoomWidth) {
				/*TODO rebuild so that setActive of the corresponding Buttons is called, so the view can take care of the display part
    		needs rewriting of some parts within ToolbarController and View
				 */
				jQuery(".mainTbView .zoomHandles .fitToWidth")[0].checked = true;
				jQuery(".mainTbView .zoomHandles .fitToWidthLabel").addClass("ui-state-active");
			} else if (that.currentImage.zoomInfo.zoomScreen) {
				jQuery(".mainTbView .zoomHandles .fitToScreen")[0].checked = true;
				jQuery(".mainTbView .zoomHandles .fitToScreenLabel").addClass("ui-state-active");
			}
      })
      	.bind("minimize.viewerContainer", function() {
      		that.toolbarMgr.destroyModel('mainTb');
      })
      	//exploit that the init.viewer event bubbles up the DOM hierarchy
      	.bind("init.viewer", function(){
            that.toolbarMgr = new ToolbarManager();
            that.toolbarCtrl = new ToolbarController(that);
        // entweder Mgr macht alles und Übergabe des related... (Modelprovider) oder Models kümmern sich untereinander und schöne Form
        // (siehe unten)
        // vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?

        // Toolbar Manager
        that.toolbarMgr.addModel(new PreviewToolbarModelProvider("previewTb").getModel());
        // Toolbar Controller
        that.toolbarCtrl.addView(new ToolbarView("previewTbView", that.toolbarCtrl.toolbarContainer, i18n));

        // holt alle bisherigen Models in den Controller und setzt diese entsprechend um
        that.toolbarCtrl.catchModels();
        that.initialized = true;
      })
      	.bind("reinit.viewer", function() {
      		that.toolbarCtrl.paint("mainTb");
      })
      	.bind("zoom.viewer", function() {
      		that.toolbarCtrl.checkZoom(that.viewerBean.zoomLevel);
      })
    }

    constructor.prototype.startViewer = function ii_startViewer(startFile) {
      // Load Page
      if (this.properties.useParam && URL.getParam("page") != "") {
        // TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
        startFile = decodeURI(URL.getParam("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/, "§"));
      }
      // remove leading '/'
      startFile = encodeURI(startFile.replace(/^\/*/, ""));
      this.gen.loading(startFile);
    };

    return constructor;
  })();
})();
