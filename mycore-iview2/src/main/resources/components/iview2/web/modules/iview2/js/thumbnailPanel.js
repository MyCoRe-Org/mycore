/**
 * @namespace	Package for ThumbnailPanel, contains Default ThumbnailPanel-View and -Controller
 * @memberOf 	iview
 * @name		ThumbnailPanel
 */
iview.ThumbnailPanel = iview.ThumbnailPanel || {};

/**
 * @class
 * @constructor
 * @version	1.0
 * @memberOf	iview.ThumbnailPanel
 * @name	 	View
 * @description View to display with a given template the underlying model
 */
iview.ThumbnailPanel.View = function() {
	this._mainClass;
	this._customClass;	
	this._divSize = {};
	this._previewSize = {};
	this._amount = {"width":0, "height":0};
	this._scrollBarWidth = 0;
	this._numberOfPages = -1;
	this._currentFirstRow = -1;
	this._selected = 0;
	this._visible = false;
	this._pages = [];
	this._tileUrlProvider = null;
	this._useScrollBar = true;
	this.my = null;
};

(function() {
	/**
	 * @public
	 * @function
	 * @name		disableScrollBar
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	disables the use of scrollbar. If the View was already created the scrollbar will be removed
	 */
	function disableScrollBar() {
		this._useScrollBar = false;
		this._scrollBarWidth = 0;
		if (this.my.bar) {
			this.my.bar.detach();
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setDivSize
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	sets the size of the seperate which includes the image and the related infos
	 * @param 		{object} divSize
	 * @param		{float} divSize.width holds the width of the Divs
	 * @param		{float} divSize.height holds the height of the Divs
	 */
	function setDivSize(divSize) {
		this._divSize = {'width':toInt(divSize.width),
						'height':toInt(divSize.height)};
	}

	/**
	 * @public
	 * @function
	 * @name		setPreviewSize
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	sets the according size for every preview image div
	 * @param 		{float} previewSize
	 * @param		{float} previewSize.width width of the preview Image div
	 * @param		{float} previewSize.height height of the preview Image div
	 */
	function setPreviewSize(previewSize) {
		this._previewSize = {'width':toInt(previewSize.width),
							'height':toInt(previewSize.height)};
	}
	
	/**
	 * @public
	 * @function
	 * @name		setNumberOfPages
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	sets the number of pages the document has
	 * @param	 	{float} value number of pages 
	 */
	function setNumberOfPages(value) {
		this._numberOfPages = toInt(value);
		if (this._numberOfPages < 0) {
			this._numberOfPages *= -1;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		addPage
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	adds another page to the list
	 * @param 		{integer} id holds the id of the page which is added
	 * @param		{string} href path to the image which is added
	 */
	function addPage(id, href) {
		this._pages[id] = href;
	}
		
	/**
	 * @public
	 * @function
	 * @name		resize
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	resizes the ThumbnailPanel when the size of the browser is changing
	 */
	function resize() {
		createContainer(this);
		//calculates the new correct height so the toolbar is visible into the ThumbnailPanel-View
		if (window.innerWidth) {
			this.my.self.css("height", window.innerHeight - 44 + "px"); //44 is the Height of the Toolbar
		} else {
			this.my.self.css("height", document.documentElement.clientHeight - 44 + "px"); //44 is the Height of the Toolbar
		}
		posContainer(this);
		if (this._visible) {
			loadImages(this);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSelected
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	takes the given pagenumber and adapts the view in that way that the selected one is visible
	 * @param 		{integer} value pagenumber of the newly selected entry
	 */
	function setSelected(value) {
		this._selected = toInt(value);
		calculateFirstRow(this);
		if (this.my.bar) {
			this.my.barObj.setCurValue(this._currentFirstRow);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		visible
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	makes the View visible depending on the given boolean value, if no value is given the View will switch in the opposite mode than it's currently
	 * @param 		{boolean} bool holds the state into which the View shall switch
	 */
	function visible(bool) {
		if (typeof bool === "undefined") {
			bool = !this._visible;
		}
		if (bool == true) {
			this._visible = true;
			//we're getting displayed so show the User the latest stuff
			this.resize();
			this.my.self.slideDown("slow");
		} else {
			this._visible = false;
			var that = this.my.self;
			this.my.self.slideUp("slow");
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		loadImages
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	load the ThumbnailPanel so that the actually picture is in first line
	 * @param 		{instance} that
	 */	
	function loadImages(that) {
		// for later check initialized
		var delFrom = that._amount.height;		
		
		var divBox;
		// proceed line wise
		for (var i = 0; i < that._amount.height; i++) {
			for (var j = 0; j < that._amount.width; j++) {
				divBox= that.my.pictures[(i * (that._amount.width)) + j];
				//get back previously hidden div's and set the picPos it represents
				divBox.css("display", "block").attr("page",((i + that._currentFirstRow) * that._amount.width) + j);
				
				//load needed Previews
				if ((((i + that._currentFirstRow) * that._amount.width) + j) < that._numberOfPages) {
					loadSingleImage(that, divBox);
				}
				// last line who contains pages
				if ((i + that._currentFirstRow) >= (Math.floor((that._numberOfPages) / that._amount.width))) {
					// page not existing???
					if ((((that._currentFirstRow + i) * that._amount.width)+j) > (that._numberOfPages - 1)) {
						divBox.css("display", "none");
						if (i <= that._amount.height) {
							delFrom = i + 1;
						}
					}
				}
			}
		}
		// to remove redundant divs when the pagenumbers are small
		if (delFrom < that._amount.height) {
			for (var i = delFrom * that._amount.width; i < that.my.pictures.length; i++) {
				that.my.pictures[(i * (that._amount.width)) + j].css("display", "none");
			}
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		loadSingleImage
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	load the separate pictures in the according divboxes
	 * @param 		{instance} that image that is loaded
	 * @param 		{object} divBox the according div box which contains one image
	 */
	function loadSingleImage(that, divBox) {
		var pageName = that._pages[toInt(divBox.attr("page"))+1];
		var source = that._tileUrlProvider.assembleUrl(0, 0, 0, pageName);
		var preview = jQuery(divBox.children("img")[0]);
		// original Values needed, because Img will scale automatic in each Props
		var origImage = new Image;
		origImage.onload = function() {trimImage(preview, source, {'height':origImage.height, 'width':origImage.width}, that);};
		origImage.src = source;
		
		// fill Info div
		var infoDiv=jQuery(divBox.children("div.infoDiv")[0]);
		infoDiv.html(decodeURI(pageName));
		infoDiv.attr("title", decodeURI(pageName));
	}
	
	/**
	 * @private
	 * @function
	 * @name		trimImage
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	fits picture to the correct size within the divBox
	 * @param 		{object} preview image which is displayed
	 * @param 		{string} source path to the image
	 * @param		{object} orig original image
	 * @param		{instance} that
	 */
	function trimImage(preview, source, orig, that) {
		preview.attr("src", source);
	
		// scale preview-images
		var scaleFactorH = (that._previewSize.height / orig.height);
		var scaleFactorW = (that._previewSize.width / orig.width);
		
		if (scaleFactorH <= 1) {
			// image is higher then div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				if (scaleFactorW < scaleFactorH) {
					preview.css("width", that._previewSize.width + "px");
					preview.css("height", orig.height * scaleFactorW + "px");
				} else {
					preview.css("width", orig.width * scaleFactorH + "px");
					preview.css("height", that._previewSize.height + "px");
				}
			} else {
				// image is smaller than the div
				preview.css("width", orig.width * scaleFactorH + "px");
				preview.css("height", that._previewSize.height + "px");
			}
		} else {
			// image is lower than the div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				preview.css("width", that._previewSize.width + "px");
				preview.css("height", orig.height * scaleFactorW + "px");
			} else {
				// image is smaller than the div
				if (scaleFactorW < scaleFactorH) {
					preview.css("width", that._previewSize.width + "px");
					preview.css("height", orig.height * scaleFactorW + "px");
				} else {
					preview.css("width", orig.width * scaleFactorH + "px");
					preview.css("height", that._previewSize.height + "px");
				}
			}
		}
		
		// center previews horz & vert
		// (infoDivs are all same within with width and size)
		preview.css("left", (preview.parent().width() - preview.outerWidth(true)) / 2 + "px");
	}
	
	/**
	 * @private
	 * @function
	 * @name		calculateFirstRow
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	if ThumbnailPanel is already created and is called so load loadImageFromLine() and adjust scrollbar
	 * @param	 	{instance} that 
	 */
	function calculateFirstRow(that) {
		that._currentFirstRow = Math.floor((parseInt(that._selected) - 1) / that._amount.width);
		// if ThumbnailPanel is to big for remaining pages
		if (that._currentFirstRow + that._amount.height - 1 > Math.ceil(that._numberOfPages / that._amount.width) - 1) {
			that._currentFirstRow = Math.ceil(that._numberOfPages / that._amount.width) - that._amount.height;
		}
		// if all pages fit in ThumbnailPanel
		if (that._currentFirstRow < 0) {
			that._currentFirstRow = 0;
		}
		loadImages(that);
		// shift scrollbar to the actually start-line
		if (that._useScrollBar) {
			that.my.barObj.setCurValue(currentFirstRow);
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		createContainer
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	creates all containers which are used for the ThumbnailPanel (#container == #previewImages)
	 * @param 		{instance} that 
	 */
	function createContainer(that) {
		//calculate the number of horizontal and vertical div-boxes
		var el=that.my.self;
		var width = Math.floor((el.width() - that._scrollBarWidth) / that._divSize.width);
		var height = Math.floor(el.height() / that._divSize.height);
		//dont do not needed work if everything is just fine
		if (width == that._amount.width && height == that._amount.height) return;
		that._amount = {
			'width': width,
			'height': height};
		
		if (that.my.bar) {
			that.my.barObj.setMaxValue(Math.ceil(that._numberOfPages/width)-height);
			that.my.barObj.setProportion(1/Math.abs(Math.ceil(that._numberOfPages/width)-height+1));
		}
		
		//clear the old pictures if there
		jQuery(that.my.pictures).each(function(pos, element) {
			if (!element) return;//needed as the resize can happen more often than this element exists
			element.detach();
			delete that.my.pictures[pos];
		});
		// create target Div's
		for (var i = 0; i < that._amount.height; i++) {
			for (var j = 0; j < that._amount.width; j++) {
				var infoDiv = jQuery("<div>")
					.addClass("infoDiv");
				
				var prevImg = jQuery("<img>")
					.addClass("previewDiv")
					.css("cursor", "pointer");
				//adding them to the list of available containers so we can access them easily
				that.my.pictures[i*that._amount.width + j] = jQuery("<div>")
					.addClass("divBox")
					.attr("no",(i * that._amount.width) + j)
					.css("float", "left")
					.appendTo(that.my.picContainer)
					.append(infoDiv)
					.append(prevImg)
					.click(function() {
						jQuery(that).trigger("click.thumbnailPanel", {"new": toInt(jQuery(this).attr("page"))});
					});
			}
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		posContainer
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	positions nicely the divBoxes within the available Space
	 * @param	 	{instance} that the ThumbnailPanel object where the code is run in
	 */
	function posContainer(that) {
		that._scrollBarWidth = ((that.my.bar)? that.my.bar.outerWidth(true): 0);
	
		if (that.my.bar) {
			that.my.barObj.setSize(that.my.self.height());
		}
		//reset everything else it does subsum and we screw everything up
		that.my.picContainer.css({"width": that.my.self.innerWidth() - that._scrollBarWidth,
			"padding": 0,
			"padding-left": (that.my.self.innerWidth() - (that.my.pictures[0].outerWidth(true)*that._amount.width))/2 + "px",
			"padding-top": (that.my.self.innerHeight() - (that.my.pictures[0].outerHeight(true)*that._amount.height))/2 + "px"});
	}
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	create the view in the ThumbnailPanel
	 * @param	 	{object} args
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the ThumbnailPanel is added
	 * @param 		{string} [id] tells the id of the ThumbnailPanel. This property isn't needed as the
	 *  scrollbar works just fine without ids. The id maybe only needed if you plan to perform custom
	 *  transformations on the scrollbar DOM
	 */
	function createView(args, id) {
		this._mainClass = args.mainClass || "";
		this._customClass = args.customClass || "";
		
		var main = jQuery("<div>").addClass(this._mainClass + " " + this._customClass)
		.appendTo(args.parent);
		
		if (typeof id !== "undefined") {
			main.attr("id", id);
		}
		
		//deactivate Browser Drag&Drop
		main.mousedown(function() {return false;});
		
		var picContainer = jQuery("<div>").addClass("picContainer").appendTo(main);
		
		this.my = {'self':main, 'picContainer': picContainer, 'pictures': []};

		this._useScrollBar = args._useScrollBar;
		if (args.useScrollBar) {
			prepareScrollBar(this);
		}
		
		createContainer(this);
		posContainer(this);
		this.setSelected(args.selected || 0);
		var that = this;
		jQuery(window).resize(function() {that.resize()});
		loadImages(this);
	}
	
	/**
	 * @private
	 * @function
	 * @name		prepareScrollBar
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	create Scrollbar in the ThumbnailPanel
	 * @param 		{instance} that 
	 */
	function prepareScrollBar(that) {
		var scrollbar = new iview.scrollbar.Controller();
		var parent = that.my.self;
		scrollbar.createView({ 'direction': 'vertical', 'parent': parent, 'mainClass': 'scroll', 'type':'stepper'});
		scrollbar.attach("curVal.scrollbar", function(e,val){
			that._currentFirstRow = val["new"];
			loadImages(that);
		});
		scrollbar.setSize(parent.height());
		scrollbar.setStepByClick(1);
		scrollbar.setJumpStep(1);
		
		// register additional Events
		scrollbar.addEventFrom("mousemove", parent);
		scrollbar.addEventFrom("mouseup", parent);
		scrollbar.addEventFrom("mousescroll", parent);
		that.my.bar = jQuery(parent.find(".scrollV:first")[0]);
		that.my.barObj = scrollbar;
	}
	
	/**
	 * @public
	 * @function
	 * @name		setTileUrlProvider
	 * @memberOf	iview.ThumbnailPanel.View#
	 * @description	set the tileUrlProvider from which the tiles are taken
	 * @param 		{tileUrlProvider} provider which gives preview tiles
	 */
	function setTileUrlProvider(provider) {
		this._tileUrlProvider = provider;
	}
	
	var prototype = iview.ThumbnailPanel.View.prototype
	prototype.createView = createView;
	prototype.setDivSize = setDivSize;
	prototype.resize = resize;
	prototype.setNumberOfPages = setNumberOfPages;
	prototype.setSelected = setSelected;
	prototype.visible = visible;
	prototype.addPage = addPage;
	prototype.setTileUrlProvider = setTileUrlProvider;
	prototype.setPreviewSize = setPreviewSize;
	prototype.disableScrollBar = disableScrollBar;
})();

/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.ThumbnailPanel
 * @name 		Controller
 * @description Controller for ThumbnailPanel
 * @param		{iview.METS.PhysicalModelProvider, API-equal Object} model for ThumbnailPanel data
 * @param		{iview.ThumbnailPanel.View, API-equal Object} [view=iview.ThumbnailPanel.View] Viewtype to use for
 *  this ThumbnailPanel, if not the package Type is used be sure to use a compatible one
 * @param		{tileUrlProvider} tileUrlProvider to retrieve urls for thumbnails from
 */
iview.ThumbnailPanel.Controller = function(modelProvider, view, tileUrlProvider) {
	this._model = modelProvider.createModel();
	this._view = new (view || iview.ThumbnailPanel.View)();
	this._tileUrlProvider = tileUrlProvider;
	var that = this;
	
	jQuery(this._model).bind("select.METS", function(e, val) {
		that._view.setSelected(val["new"]);
	});
	
	jQuery(this._view).bind("click.thumbnailPanel", function(e, val) {
		that._view.visible(false);
		that._model.setPosition(val["new"]+1);
	});
};

(function() {
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	creates the view for the ThumbnailPanel
	 * @param 		{object} args
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the ThumbnailPanel is added
	 * @param		{boolean} args.useScrollBar tells if the ThumbnailPanel will use a scrollbar or not
	 */
	function createView(args) {
		this._view.setNumberOfPages(this._model.getNumberOfPages())
		this._view.setDivSize({'width':200, 'height':200});
		this._view.setPreviewSize({'width':180, 'height':160});
		this._view.setTileUrlProvider(this._tileUrlProvider);
		var iter = this._model.iterator();
		var temp;
		while (iter.hasNext()) {
			temp = iter.next();
			this._view.addPage(temp.getOrder(), temp.getHref())
		}
		this._view.createView({
			'mainClass': args.mainClass,
			'customClass':args.customClass,
			'useScrollBar':args.useScrollBar,
			'selected': this._model.getCurPos(),
			'parent':args.parent});
	}
	
	/**
	 * @public
	 * @function
	 * @name		showView
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	tells the view to hide itself
	 */
	function showView() {
		this._view.visible(true);
	}
	
	/**
	 * @public
	 * @function
	 * @name		hideView#
	 * @memberOf	iview.ThumbnailPanel.Controller
	 * @description	tells the view to hide itself
	 */	
	function hideView() {
		this._view.visible(false);
	}

	/**
	 * @public
	 * @function
	 * @name		toggleView
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	tells the View to change it's display mode to the currently opposite mode
	 */
	function toggleView() {
		this._view.visible();
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSelected
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	takes the given pagenumber and adapts the view in that way that the selected one is visible
	 * @param 		{integer} value pagenumber of the newly selected entry 
	 */
	function setSelected(value) {
		this._view.setSelected(value);
	}
	
	/**
	 * @public
	 * @function
	 * @name		getActive
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	returns the current state of the ThumbnailPanelView (if its visible or not)
	 */
	function getActive() {
		return this._view._visible;
	}
	
	/**
	 * @public
	 * @function
	 * @name		attach
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	attach Eventlistener to used ThumbnailPanel view
	 * @param		{string} event name of events to register the listener to
	 * @param		{function} listener to add to the view
	 */
	function attach(event, listener) {
		jQuery(this._view).bind(event, listener);
	}
	
	/**
	 * @public
	 * @function
	 * @name		detach
	 * @memberOf	iview.ThumbnailPanel.Controller#
	 * @description	detach previously attached Eventlistener from ThumbnailPanel view
	 * @param		{string} event name of events to detach the listener from
	 * @param		{function} listener to add to the view
	 */
	function detach(event, listener) {
		jQuery(this._view).unbind(event, listener);
	}
	
	var prototype = iview.ThumbnailPanel.Controller.prototype;
	prototype.createView = createView;
	prototype.showView = showView;
	prototype.hideView = hideView;
	prototype.toggleView = toggleView;
	prototype.setSelected = setSelected;
	prototype.getActive = getActive;
	prototype.attach = attach;
	prototype.detach = detach;
})();

/**
 * @public
 * @function
 * @name		importThumbnailPanel
 * @memberOf	iview.ThumbnailPanel
 * @description	calls the corresponding functions to create the ThumbnailPanel
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{Deferred} def to set as resolved after the ThumbnailPanel was imported
 */
iview.ThumbnailPanel.importThumbnailPanel = function(viewer, def) {
	var thumbnailPanel = new iview.ThumbnailPanel.Controller(viewer.PhysicalModelProvider, iview.ThumbnailPanel.View, viewer.viewerBean.tileUrlProvider);
	thumbnailPanel.createView({'mainClass':'thumbnailPanel', 'parent': viewer.context.container, 'useScrollBar':true});
	viewer.thumbnailPanel = jQuery.extend(viewer.thumbnailPanel, thumbnailPanel);
	jQuery(viewer.viewerContainer).bind("minimize.viewerContainer", function() {
		//close ThumbnailPanel when Viewer is going to minimized mode
		thumbnailPanel.hideView();
	})
	viewer.thumbnailPanel.loaded = true;
	def.resolve();
}
/**
 * @public
 * @function
 * @name		openThumbnailPanel
 * @memberOf	iview.ThumbnailPanel
 * @description	blend in the ThumbnailPanel and creates it by the first call
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{button} button to which represents the ThumbnailPanel in the toolbar
 */
iview.ThumbnailPanel.openThumbnailPanel = function(viewer, button) {
	var that = this;
	// check if ThumbnailPanel was created yet
	if (viewer.thumbnailPanel.loaded) {
		viewer.thumbnailPanel.toggleView();
	} else {
		button.setLoading(true);
		setTimeout(function(){
			that.importThumbnailPanel(viewer, new jQuery.Deferred().done(function() {
				button.setLoading(false);
				viewer.thumbnailPanel.attach("click.thumbnailPanel", function(e, val) {
					// type 1: click on ThumbnailPanel div
					button.setSubtypeState(false);
				});
				viewer.thumbnailPanel.toggleView();
			}));
		}, 100);
	}
}
