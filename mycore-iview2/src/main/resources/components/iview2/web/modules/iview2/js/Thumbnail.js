//TODO Preload größe anhand der von den Kacheln bestimmen
/**
 * @public
 * @function
 * @name		handleScrollbars
 * @memberOf	iview.General#
 * @description	adapts the scrollbars to correctly represent the new view after a zoom or resize event occured to the viewer. The adaptations cover sizing the bar, the bar proportion, maxValue and currentValue depending on the given reason
 * @param		{string} [reason] the reason why the function was called, possible values are "resize" and "zoom" or void if you want to have all adaptations to be applied
 */
genProto.handleScrollbars = function(reason) {
	if (typeof reason === "undefined") reason = "all";
	
	var viewerBean = this.iview.viewerBean;
	var viewer = this.iview.context.viewer;
	var barX = this.iview.scrollbars.x;
	var barY = this.iview.scrollbars.y;
	var currentImage = this.iview.currentImage;
	// determine the current imagesize
	var zoomScale = currentImage.zoomInfo.scale;
	var curWidth = (currentImage.width / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomScale;
	var curHeight = (currentImage.height / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*zoomScale;

	var height = viewer.height();
	var width = viewer.width();
	var top = viewer.offset().top;
	
	// vertical bar
	var ymaxVal = curHeight - height;
	barY.setMaxValue((ymaxVal < 0)? 0:ymaxVal);
	barY.setProportion(height/curHeight);
	
	// horizontal bar
	var xmaxVal = curWidth - width;
	barX.setMaxValue(xmaxVal);
	barX.setProportion(width/curWidth);

	switch (reason) {
	case "all":
	case "zoom":
		// correctly represent the new view position
		barX.setCurValue(-viewerBean.x);
		barY.setCurValue(-viewerBean.y);
		if (!reason == "all") break;
	case "resize":
		// set the new size of the scrollbar
		barY.setSize(height);
		barY.my.self[0].style.top = top + "px";
		barX.setSize(width);
		if (!reason == "all") break;
	}
};

/**
 * @public
 * @function
 * @name		updateModules
 * @memberOf	iview.Thumbnails
 * @description	marks the correct picture in the chapterview and set zoombar to the correct zoomlevel
 */
genProto.updateModuls = function() {
  if (this.iview.viewerContainer.isMax()){
    // align/fit scrollbars
    this.handleScrollbars();
    try {
      //repaint Toolbar as if the width of the dropdown changes the spring needs to be adjusted
      this.iview.toolbar.ctrl.paint("mainTb");	
    } catch (e) {}
  } else {
    //TODO: align image and toolbar to the center
//    var previewTbView = jQuery(this.iview.getToolbarCtrl().getView("previewTbView").toolbar);
//    var currentImage = this.iview.currentImage;
//    var zoomScale=currentImage.zoomInfo.getScale();
//    var newTop = ((((currentImage.getHeight() / Math.pow(2, currentImage.zoomInfo.getMaxLevel() - 1)) * zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
//    if (this.iview.my.container.hasClass("viewerContainer min")) {
//      this.iview.getToolbarCtrl().toolbarContainer.find(".toolbar").css("top", newTop);
//    }
  }
}

/**
 * @public
 * @function
 * @name		processMETS
 * @memberOf	iview.General
 * @description	process the loaded mets and do all final configurations like setting the pagenumber, generating Chapter and so on
 * @param		{document} metsDoc holds in METS/MODS structure all needed informations to generate an chapter and ThumbnailPanel of of the supplied data
 */
genProto.processMETS = function(metsDoc) {
	var that = this;
	this.iview.metsDoc = metsDoc;
	//create the PhysicalModelProvider
	this.iview.PhysicalModelProvider = new iview.METS.PhysicalModelProvider(metsDoc);
	this.iview.PhysicalModel = this.iview.PhysicalModelProvider.createModel();
	var physicalModel = this.iview.PhysicalModel;
	var toolbarCtrl = this.iview.toolbar.ctrl;
	this.iview.amountPages = physicalModel.getNumberOfPages();
	physicalModel.setPosition(physicalModel.getPosition(this.iview.currentImage.name));
	jQuery(physicalModel).bind("select.METS", function(e, val) {
//			that.notifyListenerNavigate(val["new"]);
		that.iview.loadPage();
		toolbarCtrl.checkNavigation(val["new"]);
		that.updateModuls();
		if (jQuery('.navigateHandles .pageBox')[0]) {
			toolbarCtrl.updateDropDown(jQuery(pagelist.find("a")[val["new"] - 1]).html());
		}
	})

	// Toolbar Operation
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openChapter");
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openThumbnailPanel");
	toolbarCtrl.checkNavigation(this.iview.PhysicalModel.getCurPos());

	//Generating of Toolbar List
	var it = physicalModel.iterator();
	var curItem = null;
	var pagelist = jQuery('<div id="pages" style="visibility: hidden; z-index: 80; position: absolute; left: -9999px;" class="hidden">');
	var ul = jQuery("<ul>");
	while (it.hasNext()) {
		curItem = it.next();
		if (curItem != null) {
			var orderLabel='[' + curItem.getOrder() + ']' + ((curItem.getOrderlabel().length > 0) ? ' - ' + curItem.getOrderlabel():'');  
			ul.append(jQuery('<li><a href="index.html#" id='+curItem.getID()+' class="'+orderLabel+'">'+orderLabel+'</a></li>'));
		}
	}
	pagelist.append(ul);
	toolbarCtrl.toolbarContainer.append(pagelist);

	// if METS File is loaded after the drop-down-menu (in mainToolbar) its content needs to be updated
	if (jQuery('.navigateHandles .pageBox')[0]) {
		jQuery(toolbarCtrl.views['mainTbView']).trigger("new", {'elementName' : "pageBox", 'parentName' : "navigateHandles", 'view' : this.iview.context.container.find('.navigateHandles .pageBox')});
		// switch to current content
		toolbarCtrl.updateDropDown(jQuery(pagelist.find("a")[physicalModel.getCurPos() - 1]).html());
	}
	//at other positions Opera doesn't get it correctly (although it still doesn't look that smooth as in other browsers) 
	//TODO needs to be adapted to work correctly with the new structure
	window.setTimeout(function() {
    	toolbarCtrl.paint('mainTb');
  }, 10);
};

var URL = { "location": window.location};
/**
 * @public
 * @function
 * @name		getParam
 * @memberOf	URL
 * @description	additional function, look for a parameter into search string an returns it
 * @param		{string} param parameter whose value you want to have
 * @return		{string} if param was found it's value, else an empty string
 */
URL.getParam = function(param) {
	try {
		return(this.location.search.match(new RegExp("[?|&]?" + param + "=([^&]*)"))[1]);
	} catch (e) {
		return "";
	}
};
