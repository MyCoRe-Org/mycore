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
		jQuery(document).trigger(iview.IViewInstance.INIT_EVENT, iViewInst);
		return returnValue;
	};

	iview.removeInstance = function(iViewInst) {
		var derivateId = iViewInst.properties.derivateId;
		if (typeof Iview[derivateId] !== "undefined") {
			Iview[derivateId].pop();
		}
	};

	/**
	 * @description The instance of a ImageViewer
	 * @name iview.IViewInstance
	 * @memberof iview
	 */
	iview.IViewInstance = (function() {

		/**
		 * @description Creates a new Iview Instance
		 * @memberOf iview.IViewInstance
		 * @param {Object}
		 *            container were the viewer should be
		 * @param {Object}
		 *            options the options like derivateId
		 */
		function constructor(container, options) {
			var that = this;
			
			this.processProperties(options);

			// check if first instance
			var paramDerId = URL.getParam("derivate");
			this.properties.useParam = false;

			if (typeof Iview[this.properties.derivateId] === "undefined") {
				var first = true;
				for ( var derId in Iview) {
					first = false;
					break;
				}
				this.properties.useParam = (paramDerId === this.properties.derivateId)
						|| (paramDerId.length === 0 && first);
			}

			this.initaliseComponents(container);

			// Check if we could use the canvas image viewer
			if (iview.isCanvasAvailable) {
				this.canvas = new iview.Canvas(this);
			}

			jQuery(this.currentImage).bind(iview.CurrentImage.CHANGE_EVENT,
					function() {
						that.onImageChangeHandler();
					});

			// TODO load toolbar after all resources (css, images) are ready
			createToolbars(this);
		}
		


		constructor.prototype.changeScrollbar = function(size, position) {
			var size = size || true;
			var position = position || true;
			if (this.scrollBar != null) {
				if (size) {
					var imageWidth = typeof this.viewerBean.getCurrentImageWidth != "undefined"
							&& this.viewerBean.getCurrentImageWidth()
							|| this.currentImage.curWidth;
					var imageHeight = typeof this.viewerBean.getCurrentImageHeight != "undefined"
							&& this.viewerBean.getCurrentImageHeight()
							|| this.currentImage.curHeight;
					this.scrollBar.setSize({
						"width" : imageWidth,
						"height" : imageHeight
					});
				}
				if (position) {
					this.scrollBar.setPosition({
						"x" : this.viewerBean.x,
						"y" : this.viewerBean.y
					});
				}
			}
		}

		constructor.prototype.onImageDimesionEvent = function(event) {
			this.changeScrollbar(true, true);
		};

		/**
		 * @description Handler that is automatic called when the image changes.
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.onImageChangeHandler = function() {
			this.processImageProperties();
			this.changeScrollbar(true, true);

			if (this.ThumbnailPanel != null) {
				this.ThumbnailPanel.setSelected(this.PhysicalModel.getCurPos());
			}
		};

		/**
		 * @description Handler that is automatic called when the viewer zooms
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.onViewerZoomHandler = function() {
			this.changeScrollbar(true, true);
		};

		/**
		 * @description Handler that is automatic called when the viewer moves
		 * @param {Object}
		 *            jq
		 * @param {Object}
		 *            event
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.onViewerMoveHandler = function(jq, event) {
			this.changeScrollbar(false, true);
			
			if(this.scrollBar != null){
				this.scrollBar.setPosition(event);
			};
		};

		/**
		 * Will be called if the Viewer will be maximized
		 * @method
		 * @param one
		 *            (true if first time maximized)
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.onMaximizeViewerContainer = function(one) {
			var that = this;
			if (one) {
				if (this.properties.useOverview) {
					iview.overview.importOverview(this);
				}

				var scrollbarContainer = jQuery("<div class=\"scrollBar\" />");

				that.scrollBar = new iview.Scrollbar.Controller(
						scrollbarContainer.appendTo(this.viewerContainer));

				var scrollbarEvent = function(jq, e) {
					if (e.getEventType() == "positionChanged") {

						that.viewerBean.positionTiles({
							"x" : that.viewerBean.x - e.getXValue(),
							"y" : that.viewerBean.y - e.getYValue()
						});

					}
				};
				this.scrollBar.registerEventHandler(scrollbarEvent);

				jQuery(window)
						.resize(
								function() {
									var sizeX = that.viewerBean.width
											+ iview.Scrollbar.Controller.SCROLLBAR_WIDTH;
									var sizeY = that.viewerBean.height
											+ iview.Scrollbar.Controller.SCROLLBAR_HEIGHT;

									scrollbarContainer.css({
										"width" : sizeX + "px"
									});
									scrollbarContainer.css({
										"height" : sizeY + "px"
									});

								});
			}

			var scrollbarContainer = this.scrollBar.getContainer();

			this.changeScrollbar(true, false);

			var sizeX = that.viewerBean.width
					+ iview.Scrollbar.Controller.SCROLLBAR_WIDTH;
			var sizeY = that.viewerBean.height
					+ iview.Scrollbar.Controller.SCROLLBAR_HEIGHT;

			scrollbarContainer.css({
				"width" : sizeX + "px"
			});
			scrollbarContainer.css({
				"height" : sizeY + "px"
			});

			this.addDimensionSubstract(true, 'scrollbar',
					iview.Scrollbar.Controller.SCROLLBAR_WIDTH);
			this.addDimensionSubstract(false, 'scrollbar',
					iview.Scrollbar.Controller.SCROLLBAR_HEIGHT);

		};
		/**
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.onMinimizeViewerContainer = function() {
			if (this.ThumbnailPanel != null) {
				this.ThumbnailPanel.hideView(false);
			}
		};

		/**
		 * @method
		 * @memberOf iview.IViewInstance
		 * @description Assigns the the Components if there already loaded.
		 *              Otherwise they get parent and loaded assigned.
		 * @param {Object}
		 *            container the viewer container were the components
		 *            located.
		 */
		constructor.prototype.initaliseComponents = function(container) {
			var that = this;

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
				'loaded' : (this.permalink || {}).loaded || false,
				'observer' : new Array()
			});

			this.scrollBar = null;
			this.thumbnailPanel = null;

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
			};

			jQuery(this.viewerContainer).one(
					"maximize.viewerContainer",
					function() {
						that.onMaximizeViewerContainer(true);
						jQuery(that.viewerContainer).bind(
								"maximize.viewerContainer", function() {
									that.onMaximizeViewerContainer(false);
								});
					}).bind("minimize.viewerContainer", function() {
				that.onMinimizeViewerContainer();
			});

			// other components which are lowering the width and the height of
			// the viewer, can be applyed here
			jQuery(this).trigger(iview.IViewInstance.INIT_COMPONENTS_EVENT,
					this);

		};

		/**
		 * @description Sets the Default properties and applys extra properties.
		 * @exception throws
		 *                IviewInstanceError if properties.derivateId is
		 *                undefined.
		 * @memberOf iview.IViewInstance
		 * @param {Object}
		 *            properties the extra properties that should be set.
		 */
		constructor.prototype.processProperties = function(properties) {
			// apply the default properties
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
			// overwrite default properties with permited properties
			this.properties.set(properties);

			// check if there is a derivate id setted
			if (typeof this.properties.derivateId === "undefined") {
				throw new iview.IviewInstanceError(
						"No derivateId defined. (should be passed to the IviewInstance constructor)",
						this);
			}
		};

		/**
		 * @description applys the properties: maximized, tosize, zoominfo and
		 *              zoom
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.applyProperties = function() {
			var that = this;

			// IE ignores surface if background is transparent
			var surface = this.context.viewer.find(".surface");
			surface.css({
				"background" : "white",
				"opacity" : 0.001
			});

			surface.mousewheel(function(jq, e) {

				var oldPos = that.scrollBar.getPosition();
				oldPos.y -= (e * 10);
				that.scrollBar.setPosition(oldPos);
			});

			// PermaLink Handling
			// choice if zoomLevel or special; zoomMode only makes sense in
			// maximized viewer
			if (this.properties.useParam
					&& URL.getParam("maximized") == "true"
					|| (typeof this.properties.maximized !== "undefined" && this.properties.maximized == "true")) {
				if (URL.getParam("tosize") == "width") {
					if (!this.currentImage.zoomInfo.zoomWidth)
						this.viewerBean.pictureWidth();
				} else if ((URL.getParam("tosize") == "screen" || isNaN(parseInt(URL
						.getParam("zoom"))))
						&& !this.currentImage.zoomInfo.zoomScreen) {
					this.viewerBean.pictureScreen();
				} else if (isNaN(parseInt(URL.getParam("zoom")))
						&& !this.currentImage.zoomInfo.zoomScreen) {
					this.viewerBean.pictureScreen();
				}

				setTimeout(function() {
					that.toggleViewerMode();
				}, 100);

			}

			// Resize-Events registrieren
			jQuery(window).resize(function() {
				that.reinitializeGraphic();
			});

		};

		/**
		 * @param {Object}
		 *            startFile
		 * @param {boolean}
		 *            should the viewer start maximized (overwrites container
		 *            properties)
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.startViewer = function(startFile, maximized) {
			if (typeof maximized !== "undefined") {
				this.properties.maximized = maximized;
			}

			// Load Page
			if (this.properties.useParam && URL.getParam("page") != "") {
				startFile = decodeURIComponent(URL.getCleanUrl(URL.getParam("page")));
			}
			// remove leading '/'
			startFile = encodeURIComponent(startFile.replace(/^\/*/, ""));
			this.loading(startFile);
		};

		/**
		 * @description Initialise the TileUrlProvider Initialise the ViewerBean
		 * 
		 * @param {Object}
		 *            startFile
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.loading = function ii_loading(startFile) {
			PanoJS.USE_SLIDE = false;
			PanoJS.USE_LOADER_IMAGE = false;
			PanoJS.MOVE_THROTTLE = 10;
			PanoJS.BLANK_TILE_IMAGE = "../modules/iview2/" + styleFolderUri
					+ 'blank.gif';
			var that = this;
			// opera triggers the onload twice
			var iviewTileUrlProvider = new PanoJS.TileUrlProvider(
					this.properties.baseUri, this.currentImage.name, 'jpg');
			iviewTileUrlProvider.derivate = this.properties.derivateId;

			iviewTileUrlProvider.getCurrentImage = function initializeGraphic_getCurrentImage() {
				return that.currentImage;
			};

			this.initViewerBean(iviewTileUrlProvider);

			// needs to be registered before any other listener for this event
			var viewerBean = this.viewerBean;

			jQuery(document).delegate(viewerBean.viewer, "zoom.viewer",
					function() {
						that.onViewerZoomHandler(arguments);
					});
			jQuery(viewerBean.viewer).bind("move.viewer", function(jq, event) {
				that.onViewerMoveHandler(jq, event);
			}).bind("zoom.viewer", function() { // onViewerZoom handler runs to
				// late cause its a delegate
				viewerZoomed.apply(that, arguments);
			});

			if (this.properties.useParam
					&& !isNaN(parseInt(URL.getParam("zoom")))) {
				this.viewerBean.zoomLevel = parseInt(URL.getParam("zoom"));
			} else {
				this.currentImage.zoomInfo.zoomScreen = true;
			}

			this.preload = new iview.Preload.Controller(this);

			this.loadPage(function() {
				that.applyProperties();
				that.loadMetsFile();
			}, startFile);
		};

		/**
		 * Initialises the ViewerBean.
		 * 
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.initViewerBean = function(iviewTileUrlProvider) {
			var that = this;
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
					loadingTile : "../modules/iview2/" + styleFolderUri
							+ 'blank.gif'
				});

				this.viewerBean.iview = this;// handle Viewer informations so
				// PanoJS can work with it
				this.viewerBean.init();

				this.reinitializeGraphic(function() {
					jQuery(that.viewerBean.viewer).trigger("init.viewer");
				});
			}
		};

		/**
		 * @description loads the Mets file from MCRMETSServlet and runs
		 *              iview.METS.processMETS
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.loadMetsFile = function () {
			var metsDocURI = this.properties.webappBaseUri
					+ "servlets/MCRMETSServlet/" + this.properties.derivateId;
			var that = this;
			jQuery
					.ajax({
						url : metsDocURI,
						success : function(response) {
							iview.METS.processMETS(that, response);
							that.urn = new iview.urn.Controller(that);
							that.ThumbnailPanel = new iview.thumbnailPanel.Controller(
									that.viewerContainer, that.PhysicalModel,
									that.viewerBean.tileUrlProvider);
							
							function resize(){
								if(typeof that.ThumbnailPanel != "undefined" && typeof that.ThumbnailPanel._container != "undefined"){
									var sizeY = that.viewerBean.height;
									that.ThumbnailPanel._container.css({
										"width" : "100%",
										"height" : sizeY + "px"
									});
								}
							}
							
							jQuery(window).resize(function() {
								resize();
							});
							resize();
							
							jQuery(that)
									.trigger(
											iview.IViewInstance.INIT_PHYSICAL_MODEL_EVENT,
											this);

						},
						error : function(request, status, exception) {
							that.removeNavigationButtons();
							log("Error Occured while  METS file:\n" + exception);
							showMessage("component.iview2.noMets");
						}
					});
		};

		/**
		 * @memberOf iview.IViewInstance
		 */
		constructor.prototype.removeNavigationButtons = function() {
			var that = this;
			if (this.viewerContainer.isMax()) {
				this.toolbar.ctrl.perform("remove", "", 'navigateHandles',
						'backward');
				this.toolbar.ctrl.perform("remove", "", 'navigateHandles',
						'pageBox');
				this.toolbar.ctrl.perform("remove", "", 'navigateHandles',
						'forward ');
			} else {
				this.toolbar.ctrl.perform("remove", "", 'previewBack',
						'backward');
				this.toolbar.ctrl.perform("remove", "", 'previewForward',
						'forward');
			}
			jQuery(this.viewerContainer).bind(
					"minimize.viewerContainer",
					function() {
						that.toolbar.ctrl.perform("remove", "", 'previewBack',
								'backward');
						that.toolbar.ctrl.perform("remove", "",
								'previewForward', 'forward');
					});

			jQuery(this.viewerContainer).bind(
					"maximize.viewerContainer",
					function() {
						that.toolbar.ctrl.perform("remove", "",
								'navigateHandles', 'backward');
						that.toolbar.ctrl.perform("remove", "",
								'navigateHandles', 'pageBox');
						that.toolbar.ctrl.perform("remove", "",
								'navigateHandles', 'forward');
					});
		};

		/**
		 * @public
		 * @function
		 * @name toggleViewerMode
		 * @memberOf iview.IViewInstance
		 * @description maximize and show the viewer with the related image or
		 *              minimize and close the viewer
		 */
		constructor.prototype.toggleViewerMode = function() {
			var waitForToolbar = function(self, iviewInst) {
				if (iviewInst.properties.initialized
						&& typeof iviewInst.currentImage.zoomInfo.dimensions[0] == "object") {

					iviewInst.context.switchContext();
					jQuery(iviewInst.viewerContainer).trigger(
							(!iviewInst.viewerContainer.isMax() ? "minimize"
									: "maximize")
									+ ".viewerContainer");

					/*
					 * IE causes resize already at class change (mostly because
					 * position: rel <> fix) IE runs resize multiple times...but
					 * without this line he doesn't...
					 */

					iviewInst.reinitializeGraphic();
				} else {
					setTimeout(function() {
						self(self, iviewInst);
					}, 100);
				}
			};
			waitForToolbar(waitForToolbar, this);
		};

		/**
		 * @public
		 * @function
		 * @name loadPage
		 * @memberOf iview.IViewInstance
		 * @description reads out the imageinfo.xml, set the correct zoomvalues
		 *              and loads the page
		 * @param {function}
		 *            callback
		 * @param {String}
		 *            [startFile] optional page to open
		 */
		constructor.prototype.loadPage = function(callback, startFile) {
			var url = (typeof startFile != "undefined") ? startFile
					: this.PhysicalModel.getCurrent().getHref();
			this.currentImage.name = url;
			var imagePropertiesURL = this.properties.baseUri[0] + "/"
					+ this.properties.derivateId + "/" + url + "/imageinfo.xml";
			var that = this;
			jQuery.ajax({
				url : imagePropertiesURL,
				success : function(response) {
					that.currentImage.processImageProperties(response, url);
					callBack(callback);
					jQuery(that.currentImage).bind(
							iview.CurrentImage.DIMENSION_EVENT, function() {
								that.onImageDimesionEvent();
							});
				},
				error : function(request, status, exception) {
					log("Error occured while loading image properties:\n"
							+ exception);
				}
			});
		};

		/**
		 * @public
		 * @function
		 * @name processImageProperties
		 * @memberOf iview.IViewInstance
		 * @description
		 * @param {object}
		 *            imageProperties
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
			viewerBean.x = 0;
			viewerBean.y = 0;
			if (!isNaN(URL.getParam("x")) && !isNaN(URL.getParam("x"))) {
				viewerBean.positionTiles({
					'x' : -toFloat(URL.getParam("x")),
					'y' : -toFloat(URL.getParam("y"))
				}, true);
			}
			this.roller = false;
		};

		/**
		 * @public
		 * @function
		 * @name reinitializeGraphic
		 * @memberOf iview.IViewInstance
		 * @param {function}
		 *            callback which is called just before the event
		 *            reinit.viewer is triggered
		 * @description is called if the viewer size is resized and
		 *              calculates/set therefore all values for the current
		 *              zoomlevel and viewModus (i.e. scrrenWidth)
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
				curWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth
						: document.body.clientWidth);
				curHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight
						: document.body.clientHeight);
			}

			var viewerContainer = this.context.container;
			var viewer = this.context.viewer;
			if (jQuery(viewerContainer).hasClass("max")) {
				// to grant usage of the complete height it's not possible to
				// simply use height:100%
				viewerContainer.css({
					'height' : curHeight - viewerContainer.offset().top + "px",
					'width' : curWidth + "px"
				});
				viewer.css({
					'height' : curHeight - this.substractsDimension.x.total
							+ "px",
					'width' : curWidth - this.substractsDimension.y.total
							+ "px"
				});
			} else {
				// restore minimized size settings
				viewerContainer.css({
					'height' : this.properties.startHeight + "px",
					'width' : this.properties.startWidth + "px"
				});
				viewer.css({
					'height' : this.properties.startHeight
							- this.substractsDimension.x.total + "px",
					'width' : this.properties.startWidth
							- this.substractsDimension.y.total + "px"
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

			callBack(callback);
			// notify all listeners that the viewer was modified in such way
			// that they possibly need adaptation of their own view
			jQuery(viewerBean.viewer).trigger("reinit.viewer");

			var sizeX = viewerBean.width
					+ iview.Scrollbar.Controller.SCROLLBAR_WIDTH;
			var sizeY = viewerBean.height
					+ iview.Scrollbar.Controller.SCROLLBAR_HEIGHT;

			if (scrollbarContainer != null) {
				var scrollbarContainer = this.scrollBar.getContainer();
				scrollbarContainer.css({
					"width" : sizeX + "px"
				});
				scrollbarContainer.css({
					"height" : sizeY + "px"
				});

			}

			viewerBean.x = 0;
			viewerBean.y = 0;
			if (!isNaN(URL.getParam("x")) && !isNaN(URL.getParam("x"))) {
				viewerBean.positionTiles({
					'x' : -toFloat(URL.getParam("x")),
					'y' : -toFloat(URL.getParam("y"))
				}, true);
			} else {
				viewerBean.positionTiles();
			}

			// TODO: align image and toolbar to the center
		};

		/**
		 * @public
		 * @function
		 * @name viewerZoomed
		 * @description is called if the viewer is zooming; handles the correct
		 *              sizing and displaying of the preloadpicture, various
		 *              buttons and positioning of the Overview accordingly the
		 *              zoomlevel
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
		 * @memberOf iview.IViewInstance
		 * @description allows it to modify the effective size of the viewer by
		 *              telling what space some given components occupy
		 * @param {boolean}
		 *            horizontal tells if the given size will influence the
		 *            width or the height of the viewer
		 * @param {string}
		 *            name under which the given value is will be stored
		 * @param {float}
		 *            value space the component will occupy, and therefore is
		 *            removed from the viewer size
		 * @return boolean returns true if added successfully, false in the case
		 *         the given name is already used
		 * 
		 */
		constructor.prototype.addDimensionSubstract = function(horizontal,
				name, value) {
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
		 * @memberOf iview.IViewInstance
		 * @description deallocates a previously occupied space
		 * @param {boolean}
		 *            horizontal tells if the given name is used for a width or
		 *            height occupation
		 * @param {string}
		 *            name entry to remove
		 * @return boolean returns true if the element was successfully removed,
		 *         false in the case the name wasn't used
		 */
		constructor.prototype.removeDimensionSubstract = function(horizontal,
				name) {
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
			if (p.length == 0) {
				commentNode = document.createElement("p");
				commentNode.setAttribute("style", "display:none;");
				this.viewerContainer[0].appendChild(commentNode);
			} else {
				commentNode = p[0];
			}
			commentNode.appendChild(document
					.createTextNode(this.properties.derivateId + ": " + msg
							+ "\r\n"));
		};

		return constructor;
	})();

	iview.IViewInstance.INIT_EVENT = "init.iview.instance";
	iview.IViewInstance.INIT_COMPONENTS_EVENT = "init.iview.components";
	iview.IViewInstance.INIT_PHYSICAL_MODEL_EVENT = "init.iview.physical.model";
})();
