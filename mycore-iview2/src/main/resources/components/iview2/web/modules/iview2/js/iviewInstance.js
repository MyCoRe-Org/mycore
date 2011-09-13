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
      this.viewerContainer.isMax = function() { return jQuery(this).hasClass("max");};
      this.overview = jQuery.extend(this.overview || {}, {'loaded': (this.overview || {}).loaded || false,  'parent': container});
      this.chapter = jQuery.extend(this.chapter | {}, {'loaded': (this.chapter || {}).loaded || false, 'parent': container});
      this.permalink = jQuery.extend(this.permalink | {}, {'loaded': (this.permalink || {}).loaded || false});
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
		
		iview.scrollbar.importScrollbars(this);
		
		PanoJS.USE_SLIDE = false;
		PanoJS.USE_LOADER_IMAGE = false;
		PanoJS.MOVE_THROTTLE = 10;
		PanoJS.BLANK_TILE_IMAGE = "../modules/iview2/" + styleFolderUri + 'blank.gif';
		
		// opera triggers the onload twice
		var iviewTileUrlProvider = new PanoJS.TileUrlProvider(this.properties.baseUri, this.currentImage.name, 'jpg');
		iviewTileUrlProvider.derivate = this.properties.derivateId;
		var that = this;
		iviewTileUrlProvider.getCurrentImage = function initializeGraphic_getCurrentImage(){
		  return that.currentImage;
		};

		/**
	   * initialise the viewer
	   */
		if (this.viewerBean == null) {
			this.viewerBean = new PanoJS(this.context.viewer[0], {
				initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
				tileSize: this.properties.tileSize,//Kachelgroesse
				tileUrlProvider: iviewTileUrlProvider,
				maxZoom: this.currentImage.zoomInfo.maxZoom,
				initialZoom: this.currentImage.zoomInfo.zoomInit,//Anfangs-Zoomlevel
				loadingTile: "../modules/iview2/" + styleFolderUri + 'blank.gif'
			});

			this.viewerBean.iview = this;//handle Viewer informations so PanoJS can work with it

			this.viewerBean.init();
			
			this.gen.reinitializeGraphic(function() {jQuery(that.viewerBean.viewer).trigger("init.viewer");});
		}
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
		
		this.preload = new iview.Preload.Controller(this);
		
		this.gen.loadPage(function(){
		  that.gen.startFileLoaded();
		}, startFile);
	};
	
	/**
	 * @public
	 * @function
	 * @name		toggleViewerMode
	 * @memberOf	iview.iviewInstance
	 * @description	maximize and show the viewer with the related image or minimize and close the viewer
	 */
	constructor.prototype.toggleViewerMode = function() {
		jQuery(this.viewerContainer).trigger((this.viewerContainer.isMax()? "minimize":"maximize") + ".viewerContainer");
		this.context.switchContext();
		/*IE causes resize already at class change (mostly because position: rel <> fix)
		 IE runs resize multiple times...but without this line he doesn't...*/
		this.gen.reinitializeGraphic();
	}
	
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

		this.gen.handleScrollbars("zoom");
	};

	/**
	 * @public
	 * @function
	 * @name		viewerMoved
	 * @description	is called if the picture is moving in the viewer and handles the size of the Overview accordingly the size of the picture
	 */
	function viewerMoved(jq, event) {
		this.currentImage.setPos(event);
		// set Roller this no circles are created, and we end in an endless loop
		this.roller = true;
		this.scrollbars.x.setCurValue(-event.x);
		this.scrollbars.y.setCurValue(-event.y);
		this.roller = false;
	};

    return constructor;
  })();
})();
