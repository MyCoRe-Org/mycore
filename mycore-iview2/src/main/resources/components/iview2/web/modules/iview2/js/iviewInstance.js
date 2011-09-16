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
      var that = this;
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
      this.thumbnailPanel = jQuery.extend(this.thumbnailPanel | {}, {'loaded': (this.thumbnailPanel || {}).loaded || false})
      this.gen = new iview.General(this);
      
      jQuery(this.currentImage).bind(iview.CurrentImage.CHANGE_EVENT, function(){
    	  that.processImageProperties();
      });
      //TODO load toolbar after all resources (css, images) are ready
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
		
		//TODO move it into .one("maximize.viewerContainer" as scrollbars a
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
			
			this.reinitializeGraphic(function() {jQuery(that.viewerBean.viewer).trigger("init.viewer");});
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
		
		this.loadPage(function(){

			/**
			 * @public
			 * @function
			 * @name		startFileLoaded
			 * @description	
			 */
			//Blank needs to be loaded as blank, so the level is filled. Else it lays not ontop; needed for IE 
			that.context.viewer.find(".surface").css("backgroundImage", "url(" + that.properties.webappBaseUri + "modules/iview2/gfx/blank.gif" + ")");
	
			// PermaLink Handling
			// choice if zoomLevel or special; zoomMode only makes sense in maximized viewer
			if (that.properties.useParam && URL.getParam("maximized") == "true") {
				if (URL.getParam("tosize") == "width") {
					if (!that.currentImage.zoomInfo.zoomWidth) that.viewerBean.pictureWidth();
				} else if ((URL.getParam("tosize") == "screen" || isNaN(parseInt(URL.getParam("zoom"))))
						&& !that.currentImage.zoomInfo.zoomScreen) {
					that.viewerBean.pictureScreen();
				} else if (isNaN(parseInt(URL.getParam("zoom"))) && !that.currentImage.zoomInfo.zoomScreen){
					that.viewerBean.pictureScreen();
				}
				//Toolbar is initialized on dom-load event and may not yet ready
			  var waitForToolbar = function (self, iviewInst){
			    if (iviewInst.properties.initialized){
			      iviewInst.toggleViewerMode();
			    } else {
			      setTimeout(function(){self(self,iviewInst);}, 100);
			    }
			  };
			  waitForToolbar(waitForToolbar, that);
			} else {
				// in minimized viewer always pictureScreen
				if (!that.currentImage.zoomInfo.zoomScreen) that.viewerBean.pictureScreen();
			}
			
			var metsDocURI = that.properties.webappBaseUri + "servlets/MCRMETSServlet/" + that.properties.derivateId;
			jQuery.ajax({
				url: metsDocURI,
		  		success: function(response) {
					iview.METS.processMETS(that, response);
				},
		  		error: function(request, status, exception) {
		  			if(typeof console != "undefined"){
		  				console.log("Error Occured while Loading METS file:\n"+exception);
		  			}
		  		}
			});
			
			// Resize-Events registrieren
			jQuery(window).resize(function() { that.reinitializeGraphic()});
			
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
		this.reinitializeGraphic();
	}
	

	/**
	 * @public
	 * @function
	 * @name		loadPage
	 * @memberOf	iview.iviewInstance
	 * @description	reads out the imageinfo.xml, set the correct zoomvlues and loads the page
	 * @param		{function} callback
	 * @param		{String} [startFile] optional page to open
	 */
	constructor.prototype.loadPage = function(callback, startFile) {
		var url = (typeof startFile != "undefined")? startFile: this.PhysicalModel.getCurrent().getHref();
		this.currentImage.name = url;
		var imagePropertiesURL = this.properties.baseUri[0]+"/"+this.properties.derivateId+"/"+url+"/imageinfo.xml";
		var that = this;
		jQuery.ajax({
			url: imagePropertiesURL,
	  		success: function(response) {
	  		  that.currentImage.processImageProperties(response, url);
	  		  callBack(callback);
	  		},
	  		error: function(request, status, exception) {
	  			if(console){
	  				console.log("Error occured while loading image properties:\n"+exception);
	  			}
	  		}
		});
	};
	
	/**
	 * @public
	 * @function
	 * @name		processImageProperties
	 * @memberOf	iview.iviewInstance
	 * @description	
	 * @param 		{object} imageProperties
	 */
	constructor.prototype.processImageProperties = function(){
		var viewerBean = this.viewerBean;
		
		viewerBean.resize();
		var zoomInfo = this.currentImage.zoomInfo;
		// moves viewer to zoomLevel zoomInit
		viewerBean.maxZoomLevel = zoomInfo.maxZoom;
		// handle special Modi for new Page
		if (zoomInfo.zoomWidth) {
			zoomInfo.zoomWidth = false;
			viewerBean.pictureWidth();
		} else if (zoomInfo.zoomScreen) {
			zoomInfo.zoomScreen = false;
			viewerBean.pictureScreen();
		} else {
			// moves viewer to zoomLevel zoomInit
			viewerBean.zoom(zoomInfo.zoomInit - viewerBean.zoomLevel);
		}
		
		// damit das alte zoomBack bei Modi-Austritt nicht verwendet wird
		zoomInfo.zoomBack = zoomInfo.zoomInit;

		this.roller = true;
		viewerBean.positionTiles ({'x' : this.properties.useParam ? toFloat(URL.getParam("x")) : 0, 'y' : this.properties.useParam ? toFloat(URL.getParam("y")) : 0}, true);
		this.roller = false;
	};
	
	/**
	 * @public
	 * @function
	 * @name		reinitializeGraphic
	 * @memberOf	iview.iviewInstance
	 * @param		{function} callback which is called just before the event reinit.viewer is triggered
	 * @description	is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus (i.e. scrrenWidth)
	 */
	constructor.prototype.reinitializeGraphic = function(callback) {
		var viewerBean = this.viewerBean;
		if (viewerBean == null) return;
			
		var curHeight = 0;
		var curWidth = 0;
		if (window.innerWidth) {
			curWidth = window.innerWidth;
			curHeight = window.innerHeight;
		} else {
			curWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth : document.body.clientWidth);
			curHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight : document.body.clientHeight);
		}

		var viewerContainer = this.context.container;
		var viewer = this.context.viewer;
		var barX = this.scrollbars.x;
		var barY = this.scrollbars.y;

		if (jQuery(viewerContainer).hasClass("max")) {
			//to grant usage of the complete height it's not possible to simply use height:100%
			viewerContainer.css({'height': curHeight - viewerContainer.offset().top + "px",
								'width': curWidth + "px"});
			viewer.css({'height': curHeight - viewer.offset().top - barX.my.self.outerHeight()  + "px",
						'width': curWidth - barY.my.self.outerWidth()  + "px"});
		} else {
			//restore minimized size settings
			viewerContainer.css({'height': this.properties.startHeight + "px",
								'width': this.properties.startWidth + "px"});
			viewer.css({'height': this.properties.startHeight - ((barY.my.self.css("visibility") == "visible")? barY.my.self.outerHeight() : 0)  + "px",
						'width': this.properties.startWidth - ((barX.my.self.css("visibility") == "visible")? barX.my.self.outerWidth() : 0)  + "px"});
		}
		
		viewerBean.width = viewer.outerWidth();
		viewerBean.height = viewer.outerHeight();
		viewerBean.resize();
		
		// den Modus beibehalten & aktualisieren
		var zoomInfo = this.currentImage.zoomInfo;
		if(zoomInfo.zoomScreen){
			zoomInfo.zoomScreen = !zoomInfo.zoomScreen;	
			viewerBean.pictureScreen();
		} else if(zoomInfo.zoomWidth){
			zoomInfo.zoomWidth = !zoomInfo.zoomWidth;
			viewerBean.pictureWidth();
		}
		
		if (this.thumbnailPanel.loaded && this.thumbnailPanel.getActive()) {
			// actualize thumbnailPanel only if visible else delay it upto the reopening
			this.thumbnailPanel.setSelected(this.PhysicalModel.getCurPos());
		}
		
//		this.gen.handleScrollbars("resize");
		
		callBack(callback);
		//notify all listeners that the viewer was modified in such way that they possibly need adaptation of their own view
		jQuery(viewerBean.viewer).trigger("reinit.viewer");
		
		  //TODO: align image and toolbar to the center
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
