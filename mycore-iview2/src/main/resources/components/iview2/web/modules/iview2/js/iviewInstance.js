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
        "initialized" : false,
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
      this.viewerContainer = container;
      this.overview = jQuery.extend(this.overview || {}, {'loaded': (this.overview || {}).loaded || false,  'parent': container});
      this.chapter = jQuery.extend(this.chapter | {}, {'loaded': (this.chapter || {}).loaded || false, 'parent': container});
      this.preload = container.find(".preload"); // TODO: move this somewhere
      this.gen = new iview.General(this);
      //TODO load toolbar after all resources (css, images) are ready
      var that = this;
      createToolbars(this);
    }

    constructor.prototype.startViewer = function ii_startViewer(startFile) {
      // Load Page
      if (this.properties.useParam && URL.getParam("page") != "") {
        // TODO may be incomplete: Prevent Remote File Inclusion, but never Ever drop
        startFile = decodeURI(URL.getParam("page").replace(/(:|\.\.|&#35|&#46|&#58|&#38|&#35|&amp)/, "§"));
      }
      // remove leading '/'
      startFile = encodeURI(startFile.replace(/^\/*/, ""));
      this.loading(startFile);
    };
    
    constructor.prototype.loading = function ii_loading(startFile) {
		var that = this;
		
		createScrollbars(this);
			
		that.gen.initializeGraphic();
		//needs to be registered before any other listener for this event
		var viewerBean = this.viewerBean;
		jQuery(viewerBean.viewer).bind("zoom.viewer", function() { viewerZoomed.apply(that, arguments)})
			.bind("move.viewer", function() {viewerMoved.apply(that,arguments)});
	
		jQuery(this.viewerContainer).one("maximize.viewerContainer", function() {
			if (that.properties.useOverview)
				createOverview(that);
		})
		
		if (this.properties.useParam && !isNaN(parseInt(URL.getParam("zoom")))) {
			viewerBean.zoomLevel= parseInt(URL.getParam("zoom"));
		}
		this.gen.loadPage(function(){
		  that.gen.startFileLoaded();
		}, startFile);
	};
	
	/**
	 * @public
	 * @function
	 * @name		viewerZoomed
	 * @description	is called if the viewer is zooming; handles the correct sizing and displaying of the preloadpicture, various buttons and positioning of the Overview accordingly the zoomlevel
	 */
	function viewerZoomed() {
		var viewerBean = this.viewerBean;
		// handle special Modes, needs to close
		if (this.currentImage.zoomInfo.zoomWidth) {
			viewerBean.pictureWidth(true);
		}
		if (this.currentImage.zoomInfo.zoomScreen) {
			viewerBean.pictureScreen(true);
		}
		var preload = this.context.preload;
		var currentImage=this.currentImage;
		var zoomInfo=currentImage.zoomInfo;
		preload.css({"width": (currentImage.width / Math.pow(2, zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomInfo.scale +  "px",
					 "height": (currentImage.height / Math.pow(2, zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomInfo.scale + "px"});

		this.gen.handleScrollbars("zoom");
	};

	/**
	 * @public
	 * @function
	 * @name		viewerMoved
	 * @description	is called if the picture is moving in the viewer and handles the size of the Overview accordingly the size of the picture
	 */
	function viewerMoved(jq, event) {
		// set Roller this no circles are created, and we end in an endless loop
		this.roller = true;
		var preload = this.context.preload;
		var pos = preload.position();
		this.scrollbars.x.setCurValue(-pos.left);
		this.scrollbars.y.setCurValue(-pos.top);
		this.roller = false;
	};

    return constructor;
  })();
})();
