/**
 * @class
 * @constructor
 * @name		StandardToolbarModelProvider
 * @description provides the model of a toolbar with main functionalities/buttons,
 *  especially zoom, overview, navigate, permalink and close capabilities
 * @structure	
 * 		Object {
 * 			String:		id,					//identifier of the modelProvider
 * 			String: 	model				//provided toolbar model with fix defined structure
 * 			AssoArray:	buttons				//array of the neccesary button titles, parsed out of the iview2 xsl
 * 		}
 */
var StandardToolbarModelProvider = function (id, iviewRef) {
	this.id = id;
    this.model = new ToolbarModel(id);
    
    this.init(iviewRef);
};

StandardToolbarModelProvider.prototype = {

	/**
	 * @public
	 * @function
	 * @name init
	 * @memberOf StandardToolbarModelProvider#
	 * @description builds a complete toolbar model with main functionalities
	 */
	init : function(iviewRef) {
		var curButtonset;
	
		// to show an text sample "Iview2 Bildbetrachter" within the toolbar
		// this.model.addElement(new ToolbarTextModel("Text", "Iview2 Bildbetrachter"));
		
		// zoomHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("zoomHandles"));
		curButtonset.addButton(new ToolbarButtonModel("zoomIn", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-zoomIn'}}, "toolbar.zoomIn", true, false));
		curButtonset.addButton(new ToolbarButtonModel("zoomOut", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-zoomOut'}}, "toolbar.zoomOut", true));
		var fitToWidth = new ToolbarButtonModel("fitToWidth", {'type': 'buttonCheck', 'state': false}, {'text': false, 'icons': {primary : 'iview2-icon iview2-icon-fitToWidth'}}, "toolbar.toWidth", true, false);
		var fitToScreen = new ToolbarButtonModel("fitToScreen", {'type': 'buttonCheck', 'state': false}, {'icons': {primary : 'iview2-icon iview2-icon-fitToScreen'}}, "toolbar.toScreen", true, false);
		curButtonset.addButton(fitToWidth);
		curButtonset.addButton(fitToScreen);
		
		// overviewHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("overviewHandles"));
		curButtonset.addButton(new ToolbarButtonModel("openThumbnailPanel", {'type': 'buttonCheck', 'state' : false}, {'icons': {primary : 'iview2-icon iview2-icon-thumbnailPanel'}}, "toolbar.openThumbnailPanel", false, false));
		curButtonset.addButton(new ToolbarButtonModel("openChapter", {'type': 'buttonCheck', 'state': false}, {'icons': {primary : 'iview2-icon iview2-icon-chapter'}}, "toolbar.openChapter", false, false));
		
		// navigateHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("navigateHandles"));
		curButtonset.addButton(new ToolbarButtonModel("backward", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-backward'}}, "toolbar.backward", true, false));
		// label should be current (first loaded page)
		curButtonset.addButton(new ToolbarButtonModel("pageBox", {'type': 'buttonDefault'}, {'text': true, 'icons' : {primary: 'iview2-icon iview2-icon-empty', secondary : 'iview2-icon iview2-icon-dropDownArrow'}}, "toolbar.pageBox", true, false))
		curButtonset.addButton(new ToolbarButtonModel("forward", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-forward'}}, "toolbar.forward", true, false));
		
		// permalinkHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("permalinkHandles"));
		curButtonset.addButton(new ToolbarButtonModel("permalink", {'type': 'buttonCheck', 'state' : false}, {'icons': {primary : 'iview2-icon iview2-icon-permalink'}}, "toolbar.permalink", true, false));

		// urnHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("urnHandles"));
		curButtonset.addButton(new ToolbarButtonModel("urn", {'type': 'buttonDefault'}, { 'text' : true, 'label' : "URN"}, "display.urn", true, false));
		
		if (typeof iviewRef.properties.pdfCreatorURI !== "undefined" && iviewRef.properties.pdfCreatorURI.length>0){
			//TODO: PDF check
			curButtonset = this.model.addElement(new ToolbarButtonsetModel("pdfHandles"));
			curButtonset.addButton(new ToolbarButtonModel("createPdf", {'type': 'buttonDefault'}, {'label' : "PDF"}, "toolbar.pdf", true, false));
		}
		
		this.model.addElement(new ToolbarDividerModel("line"));
		this.model.addElement(new ToolbarSpringModel("spring", 1));
		
		// closeHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("closeHandles"));
		curButtonset.addButton(new ToolbarButtonModel("close", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-close'}}, "toolbar.normalView", true, false));
	
		//change the displayed state of the buttons
		jQuery(iviewRef.viewerContainer).bind("zoom.viewer maximize.viewerContainer reinit.viewer", function() {
			//TODO normally this should work without delay, lets see if further code changes allow it
			//without delay the states arent set correctly
			setTimeout(function() {
				fitToWidth.setSubtypeState(iviewRef.currentImage.zoomInfo.zoomWidth);
				fitToScreen.setSubtypeState(iviewRef.currentImage.zoomInfo.zoomScreen);
			},10);
		});
	},
		
	/**
	 * @public
	 * @function
	 * @name getModel
	 * @memberOf StandardToolbarModelProvider#
	 * @description returns a complete toolbar model with main functionalities
	 * @return {Object} returns the complete model for a main iview2 toolbar
	 */
    getModel : function() {
		return this.model;
    }
};
