/**
 * @namespace
 * @name		iview
 */
var iview = iview || {};
/**
 * @namespace	Package for Util Classes
 * @memberOf 	iview
 * @name		utils
 */
//TODO PhysicalModel expand physicalModel so that we can generate from it the chapter view so that the chapterModel gets useless
iview.METS = iview.METS || {};


/**
 * @public
 * @function
 * @name		processMETS
 * @memberOf	iview.METS
 * @description	process the loaded mets and do all final configurations like setting the pagenumber, generating Chapter and so on
 * @param		{iviewInst} viewer in which the bars shall be created in
 * @param		{document} metsDoc holds in METS/MODS structure all needed informations to generate an chapter and ThumbnailPanel of of the supplied data
 */
iview.METS.processMETS = function(viewer, metsDoc) {
	var that = this;
	viewer.metsDoc = metsDoc;
	//create the PhysicalModelProvider
	viewer.PhysicalModelProvider = new iview.METS.PhysicalModelProvider(metsDoc);
	var physicalModel = viewer.PhysicalModel = viewer.PhysicalModelProvider.createModel();
	var toolbarCtrl = viewer.toolbar.ctrl;
	var currentPosition = physicalModel.getPosition(viewer.currentImage.name);
	var fakeEntryPresent = false;
	var fakeEntry; 
	if(currentPosition==0){
		// The start file is a Hidden File
		// Create a "Fake" Physical Entry
		//showMessage("startFileHidden");
		fakeEntry = this.createFakeEntry(viewer.currentImage, physicalModel);
		fakeEntryPresent = true;
		currentPosition = physicalModel.getPosition(viewer.currentImage.name);
	}
	
	physicalModel.setPosition(currentPosition);
	jQuery(physicalModel).bind("select.METS", function(e, val) {
		//that.notifyListenerNavigate(val["new"]);
		viewer.loadPage();
	})
	
	// Toolbar Operation
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openChapter");
	toolbarCtrl.perform("setActive", true, "overviewHandles", "openThumbnailPanel");
	toolbarCtrl.checkNavigation(viewer.PhysicalModel);

	//Generating of Toolbar List
	var it = physicalModel.iterator();
	var curItem = null;
	var pagelist = viewer.toolbar.pagelist = jQuery('<div id="pages" style="visibility: hidden; z-index: 80; position: absolute; left: -9999px;" class="hidden">');
	var ul = jQuery("<ul>");
	
	while (it.hasNext()  ) {	
		curItem = it.next();
		if (curItem != null) {
			var orderLabel='[' + curItem.getOrder() + ']' + ((curItem.getOrderlabel().length > 0) ? ' - ' + curItem.getOrderlabel():'');  
			ul.append(jQuery('<li><a href="index.html#" id='+curItem.getID()+' class="'+orderLabel+'">'+orderLabel+'</a></li>'));
		}
	}
	pagelist.append(ul);
	toolbarCtrl.toolbarContainer.append(pagelist);

	// if METS File is loaded after the drop-down-menu (in mainToolbar) its content needs to be updated
	if (jQuery(viewer.viewerContainer).find('.navigateHandles .pageBox')) {
		jQuery(toolbarCtrl.views['mainTbView']).trigger("new", {'elementName' : "pageBox", 'parentName' : "navigateHandles", 'view' : viewer.context.container.find('.navigateHandles .pageBox')});
		// switch to current content
		// a:contains('[" + viewer.PhysicalModel.getCurPos()+"]')
		toolbarCtrl.updateDropDown(jQuery(pagelist.find("a:contains('[" + viewer.PhysicalModel.getCurPos()+"]')")).html());
	}
	//at other positions Opera doesn't get it correctly (although it still doesn't look that smooth as in other browsers) 
	window.setTimeout(function() {
    	toolbarCtrl.paint('mainTb');
  }, 10);
};


/**
 * @public
 * @function
 * @name		createFakeEntry
 * @memberOf	iview.METS
 * @description	Adds a hidden element wich is not present in the structure section to the model
 * @param		{curImage} the current Image wich should be the fake Entry
 * @param		{physicalModel} the physical model were to insert the fake entry
 */
iview.METS.createFakeEntry = function(curImage, physicalModel) {
	var href = curImage.name;
	var id = curImage.name;
	var values = { "href" : href, "ID" : id, "order" : 0, "orderlabel" : 0, "contentId" : ''};
	var fakeEntry = physicalModel.addPage(values);
	return fakeEntry;
};
