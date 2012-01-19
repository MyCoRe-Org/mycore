(function() {
  "use strict";

  iview.IviewInstanceError = (function() {
    function constructor() {
      Error.apply(this, arguments);
      this.message = arguments[0];
      this.instance = arguments.length > 1 ? arguments[1] : null;
      for ( var a in this) {
        log("name:" + a, this[a]);
      }
    }
    constructor.prototype = new Error();
    constructor.prototype.constructor = constructor;
    constructor.prototype.name = "IviewInstanceError";
    return constructor;
  })();

  iview.addInstance = function(iViewInst) {
    var derivateId = iViewInst.properties.derivateId;
    if (typeof Iview[derivateId] === "undefined") {
      Iview[derivateId] = [];
    }
    var returnValue = Iview[derivateId].push(iViewInst);
    jQuery.event.trigger(iview.IViewInstance.INIT_EVENT,iViewInst);
    return returnValue;
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
      // check options
      if (typeof this.properties.derivateId === "undefined") {
        throw new iview.IviewInstanceError("No derivateId defined.", this);
      }
      // passed
      var paramDerId = URL.getParam("derivate");
      this.properties.useParam = false;
      if (typeof Iview[this.properties.derivateId] === "undefined") {
        var first = true;
        for ( var derId in Iview) {
          first = false;
          break;
        }
        this.properties.useParam = (paramDerId === this.properties.derivateId) || (paramDerId.length === 0 && first);
      }
      this.viewerContainer = container;
      this.viewerContainer.isMax = function() {
        return jQuery(this).hasClass("max");
      };
      this.overview = jQuery.extend(this.overview || {}, {
        'loaded' : (this.overview || {}).loaded || false,
        'parent' : container
      });
      this.chapter = jQuery.extend(this.chapter | {}, {
        'loaded' : (this.chapter || {}).loaded || false,
        'parent' : container
      });
      this.permalink = jQuery.extend(this.permalink | {}, {
        'loaded' : (this.permalink || {}).loaded || false
      });
      this.thumbnailPanel = jQuery.extend(this.thumbnailPanel | {}, {
        'loaded' : (this.thumbnailPanel || {}).loaded || false
      });
      this.context = new iview.Context(this.viewerContainer, this);
      this.currentImage = new iview.CurrentImage(this);
      this.substractsDimension = {
        'x' : {
          'total' : 0,
          'entries' : []
        },
        'y' : {
          'total' : 0,
          'entries' : []
        }
      };// other components which are lowering the width and the height of the viewer width and height
      // uncomment this line to activate canvas mode
      if (URL.getParam("iview2.canvas") == "true") {
    	  this.canvas = new iview.Canvas(this);
      }
      jQuery(this.currentImage).bind(iview.CurrentImage.CHANGE_EVENT, function() {
        that.processImageProperties();
      });
      // TODO load toolbar after all resources (css, images) are ready
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
      PanoJS.USE_SLIDE = false;
      PanoJS.USE_LOADER_IMAGE = false;
      PanoJS.MOVE_THROTTLE = 10;
      PanoJS.BLANK_TILE_IMAGE = "../modules/iview2/" + styleFolderUri + 'blank.gif';

      // opera triggers the onload twice
      var iviewTileUrlProvider = new PanoJS.TileUrlProvider(this.properties.baseUri, this.currentImage.name, 'jpg');
      iviewTileUrlProvider.derivate = this.properties.derivateId;
      var that = this;
      iviewTileUrlProvider.getCurrentImage = function initializeGraphic_getCurrentImage() {
        return that.currentImage;
      };

      /**
       * initialise the viewer
       */
      if (this.viewerBean == null) {
        this.viewerBean = new PanoJS(this.context.viewer[0], {
          initialPan : {
            'x' : 0,
            'y' : 0
          },// Koordianten der oberen linken Ecke
          tileSize : this.properties.tileSize,// Kachelgroesse
          tileUrlProvider : iviewTileUrlProvider,
          maxZoom : this.currentImage.zoomInfo.maxZoom,
          initialZoom : this.currentImage.zoomInfo.zoomInit,// Anfangs-Zoomlevel
          loadingTile : "../modules/iview2/" + styleFolderUri + 'blank.gif'
        });

        this.viewerBean.iview = this;// handle Viewer informations so PanoJS can work with it

        this.viewerBean.init();

        this.reinitializeGraphic(function() {
          jQuery(that.viewerBean.viewer).trigger("init.viewer");
        });
      }
      // needs to be registered before any other listener for this event
      var viewerBean = this.viewerBean;
      jQuery(viewerBean.viewer).bind("zoom.viewer", function() {
        viewerZoomed.apply(that, arguments);
      }).bind("move.viewer", function(jq, event) {
        /*
         * TODO somehow is this function triggered in IE7+ multiple times (the number of images in div.well often) after the first
         * mousepress+mousemove occured. The jsfiddle http://jsfiddle.net/cPjZV/3/ seems not to have this problem, which looks like its
         * caused from within our code. When those pseudo arguments occur they're missing our move object, so we check for that
         */
         //if (arguments.length < 2) return;
    	 if(event == null || event.x == null || event.y == null)
    	 {
    		 return;
    	 }
    	 
        that.currentImage.setPos({
          'x' : -event.x,
          'y' : -event.y
        });
      });

      jQuery(this.viewerContainer).one("maximize.viewerContainer", function() {
        if (that.properties.useOverview) {
          iview.overview.importOverview(that);
        }
        iview.scrollbar.importScrollbars(that);
      });

      if (this.properties.useParam && !isNaN(parseInt(URL.getParam("zoom")))) {
        viewerBean.zoomLevel = parseInt(URL.getParam("zoom"));
      } else {
        that.currentImage.zoomInfo.zoomScreen = true;
      }

      this.preload = new iview.Preload.Controller(this);
      this.loadPage(function() {

        /**
         * @public
         * @function
         * @name startFileLoaded
         * @description
         */
        // Blank needs to be loaded as blank, so the level is filled. Else it lays not ontop; needed for IE
        that.context.viewer.find(".surface").css("backgroundImage",
            "url(" + that.properties.webappBaseUri + "modules/iview2/gfx/blank.gif" + ")");

        // PermaLink Handling
        // choice if zoomLevel or special; zoomMode only makes sense in maximized viewer
        if (that.properties.useParam && URL.getParam("maximized") == "true") {
          if (URL.getParam("tosize") == "width") {
            if (!that.currentImage.zoomInfo.zoomWidth)
              that.viewerBean.pictureWidth();
          } else if ((URL.getParam("tosize") == "screen" || isNaN(parseInt(URL.getParam("zoom"))))
              && !that.currentImage.zoomInfo.zoomScreen) {
            that.viewerBean.pictureScreen();
          } else if (isNaN(parseInt(URL.getParam("zoom"))) && !that.currentImage.zoomInfo.zoomScreen) {
            that.viewerBean.pictureScreen();
          }
          // Toolbar is initialized on dom-load event and may not yet ready
          var waitForToolbar = function(self, iviewInst) {
            if (iviewInst.properties.initialized) {
              iviewInst.toggleViewerMode();
            } else {
              setTimeout(function() {
                self(self, iviewInst);
              }, 100);
            }
          };
          waitForToolbar(waitForToolbar, that);
        }

        var metsDocURI = that.properties.webappBaseUri + "servlets/MCRMETSServlet/" + that.properties.derivateId;
        jQuery.ajax({
          url : metsDocURI,
          success : function(response) {
            iview.METS.processMETS(that, response);
          },
          error : function(request, status, exception) {
            log("Error Occured while Loading METS file:\n" + exception);
          }
        });

        // Resize-Events registrieren
        jQuery(window).resize(function() {
          that.reinitializeGraphic();
        });

      }, startFile);
    };

    /**
     * @public
     * @function
     * @name toggleViewerMode
     * @memberOf iview.iviewInstance
     * @description maximize and show the viewer with the related image or minimize and close the viewer
     */
    constructor.prototype.toggleViewerMode = function() {
      this.context.switchContext();
      jQuery(this.viewerContainer).trigger((!this.viewerContainer.isMax() ? "minimize" : "maximize") + ".viewerContainer");
      /*
       * IE causes resize already at class change (mostly because position: rel <> fix) IE runs resize multiple times...but without this
       *  line he doesn't...
       */
      this.reinitializeGraphic();
    };

    /**
     * @public
     * @function
     * @name loadPage
     * @memberOf iview.iviewInstance
     * @description reads out the imageinfo.xml, set the correct zoomvalues and loads the page
     * @param {function}
     *          callback
     * @param {String}
     *          [startFile] optional page to open
     */
    constructor.prototype.loadPage = function(callback, startFile) {
      var url = (typeof startFile != "undefined") ? startFile : this.PhysicalModel.getCurrent().getHref();
      this.currentImage.name = url;
      var imagePropertiesURL = this.properties.baseUri[0] + "/" + this.properties.derivateId + "/" + url + "/imageinfo.xml";
      var that = this;
      jQuery.ajax({
        url : imagePropertiesURL,
        success : function(response) {
          that.currentImage.processImageProperties(response, url);
          callBack(callback);
        },
        error : function(request, status, exception) {
          log("Error occured while loading image properties:\n" + exception);
        }
      });
    };

    /**
     * @public
     * @function
     * @name processImageProperties
     * @memberOf iview.iviewInstance
     * @description
     * @param {object}
     *          imageProperties
     */
    constructor.prototype.processImageProperties = function() {
      var viewerBean = this.viewerBean;

      // viewerBean.resize();
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
      if (this.properties.useParam && (URL.getParam("x")!=="")) {
        viewerBean.positionTiles({
          'x' : toFloat(URL.getParam("x")),
          'y' : toFloat(URL.getParam("y"))
        }, true);
      }
      this.roller = false;
    };

    /**
     * @public
     * @function
     * @name reinitializeGraphic
     * @memberOf iview.iviewInstance
     * @param {function}
     *          callback which is called just before the event reinit.viewer is triggered
     * @description is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus
     *              (i.e. scrrenWidth)
     */
    constructor.prototype.reinitializeGraphic = function(callback) {
      var viewerBean = this.viewerBean;
      if (viewerBean == null)
        return;

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
      if (jQuery(viewerContainer).hasClass("max")) {
        // to grant usage of the complete height it's not possible to simply use height:100%
        viewerContainer.css({
          'height' : curHeight - viewerContainer.offset().top + "px",
          'width' : curWidth + "px"
        });
        viewer.css({
          'height' : curHeight - this.substractsDimension.x.total + "px",
          'width' : curWidth - this.substractsDimension.y.total + "px"
        });
      } else {
        // restore minimized size settings
        viewerContainer.css({
          'height' : this.properties.startHeight + "px",
          'width' : this.properties.startWidth + "px"
        });
        viewer.css({
          'height' : this.properties.startHeight - this.substractsDimension.x.total + "px",
          'width' : this.properties.startWidth - this.substractsDimension.y.total + "px"
        });
      }

      viewerBean.width = viewer.outerWidth();
      viewerBean.height = viewer.outerHeight();
      viewerBean.resize();

      // den Modus beibehalten & aktualisieren
      var zoomInfo = this.currentImage.zoomInfo;
      if (zoomInfo.zoomScreen) {
        zoomInfo.zoomScreen = !zoomInfo.zoomScreen;
        viewerBean.pictureScreen();
      } else if (zoomInfo.zoomWidth) {
        zoomInfo.zoomWidth = !zoomInfo.zoomWidth;
        viewerBean.pictureWidth();
      }

      if (this.thumbnailPanel.loaded && this.thumbnailPanel.getActive()) {
        // actualize thumbnailPanel only if visible else delay it upto the reopening
        this.thumbnailPanel.setSelected(this.PhysicalModel.getCurPos());
      }

      callBack(callback);
      // notify all listeners that the viewer was modified in such way that they possibly need adaptation of their own view
      jQuery(viewerBean.viewer).trigger("reinit.viewer");
      // TODO: align image and toolbar to the center
    };

    /**
     * @public
     * @function
     * @name viewerZoomed
     * @description is called if the viewer is zooming; handles the correct sizing and displaying of the preloadpicture, various buttons and
     *              positioning of the Overview accordingly the zoomlevel
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
    }
    ;

    /**
     * @public
     * @function
     * @name addDimensionSubstract
     * @memberOf iview.IviewInstance
     * @description allows it to modify the effective size of the viewer by telling what space some given components occupy
     * @param {boolean}
     *          horizontal tells if the given size will influence the width or the height of the viewer
     * @param {string}
     *          name under which the given value is will be stored
     * @param {float}
     *          value space the component will occupy, and therefore is removed from the viewer size
     * @return boolean returns true if added successfully, false in the case the given name is already used
     * 
     */
    constructor.prototype.addDimensionSubstract = function(horizontal, name, value) {
      var dim = this.substractsDimension[((horizontal) ? 'x' : 'y')];
      if (typeof dim.entries[name] != "undefined") {
        return false;
      }
      dim.entries[name] = toInt(value);
      dim.total += dim.entries[name];
    	  
      return true;
    };

    /**
     * @public
     * @function
     * @name removeDimensionSubstract
     * @memberOf iview.iviewInstance
     * @description deallocates a previously occupied space
     * @param {boolean}
     *          horizontal tells if the given name is used for a width or height occupation
     * @param {string}
     *          name entry to remove
     * @return boolean returns true if the element was successfully removed, false in the case the name wasn't used
     */
    constructor.prototype.removeDimensionSubstract = function(horizontal, name) {
      var dim = this.substractsDimension[((horizontal) ? 'x' : 'y')];
      if (typeof dim.entries[name] == "undefined") {
        return false;
      }
      dim.total -= dim.entries[name];
      delete dim.entries[name];
      this.reinitializeGraphic();
      return true;
    };

    constructor.prototype.comment = function ii_comment(msg) {
      var p = this.viewerContainer[0].getElementsByTagName("p");
      var commentNode;
      if (p.length==0) {
        commentNode = document.createElement("p");
        commentNode.setAttribute("style", "display:none;");
        this.viewerContainer[0].appendChild(commentNode);
      } else {
        commentNode=p[0];
      }
      commentNode.appendChild(document.createTextNode(this.properties.derivateId + ": " + msg + "\r\n"));
    };

    return constructor;
  })();
  
  iview.IViewInstance.INIT_EVENT="init.iview.instance";
    
})();
