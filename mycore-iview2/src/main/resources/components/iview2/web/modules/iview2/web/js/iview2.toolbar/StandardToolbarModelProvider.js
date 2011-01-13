/**
 * @class
 * @name StandardToolbarModelProvider
 * @description provides the model of a toolbar with main functionalities/buttons,
 *  especially zoom, overview, navigate, permalink and close capabilities
 * @strcuture	
 * 		Object {
 * 			String:		id,					//identifier of the modelProvider
 * 			String: 	model				//provided toolbar model with fix defined structure
 * 			AssoArray:	buttons				//array of the neccesary button titles, parsed out of the iview2 xsl
 * 		}
 */
var StandardToolbarModelProvider = function (id, titles) {
	this.id = id;
    this.model = new ToolbarModel(id);
    this.titles = titles;
    
    this.init();
};

StandardToolbarModelProvider.prototype = {

	/**
	 * function
	 * @name init
	 * @memberOf StandardToolbarModelProvider#
	 * @description builds a complete toolbar model with main functionalities
	 */
	init : function() {
		var curButtonset;
	
		// to show an text sample "Iview2 Bildbetrachter" within the toolbar
		// this.model.addElement(new ToolbarTextModel("Text", "Iview2 Bildbetrachter"));
		
		// zoomHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("zoomHandles"));
		curButtonset.addButton(new ToolbarButtonModel("zoomIn", {'type': 'buttonDefault'}, {'label': this.titles.zoomIn, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-zoomIn'}}, this.titles.zoomIn, true, false));
		curButtonset.addButton(new ToolbarButtonModel("zoomOut", {'type': 'buttonDefault'}, {'label': this.titles.zoomOut, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-zoomOut'}}, this.titles.zoomOut, true));
		curButtonset.addButton(new ToolbarButtonModel("fitToWidth", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.fitToWidth, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-fitToWidth'}}, this.titles.fitToWidth, true, false));
		curButtonset.addButton(new ToolbarButtonModel("fitToScreen", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.fitToScreen, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-fitToScreen'}}, this.titles.fitToScreen, true, false));
		
		// overviewHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("overviewHandles"));
		curButtonset.addButton(new ToolbarButtonModel("openOverview", {'type': 'buttonCheck', 'state' : false}, {'label': this.titles.openOverview, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-overview'}}, this.titles.openOverview, false, false));
		curButtonset.addButton(new ToolbarButtonModel("openChapter", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.openChapter, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-chapter'}}, this.titles.openChapter, false, false));
		
		// navigateHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("navigateHandles"));
		curButtonset.addButton(new ToolbarButtonModel("backward", {'type': 'buttonDefault'}, {'label': this.titles.backward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-backward'}}, this.titles.backward, true, false));
		// label should be current (first loaded page)
		curButtonset.addButton(new ToolbarButtonModel("pageBox", {'type': 'buttonDefault'}, {'label': "", 'text': true, 'icons' : {primary: 'iview2-icon iview2-icon-empty', secondary : 'iview2-icon iview2-icon-dropDownArrow'}}, this.titles.pageBox, true, false))
		curButtonset.addButton(new ToolbarButtonModel("forward", {'type': 'buttonDefault'}, {'label': this.titles.forward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-forward'}}, this.titles.forward, true, false));
		
		// permalinkHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("permalinkHandles"));
		curButtonset.addButton(new ToolbarButtonModel("permalink", {'type': 'buttonCheck', 'state' : false}, {'label': this.titles.permalink, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-permalink'}}, this.titles.permalink, true, false));
		
		this.model.addElement(new ToolbarDividerModel("line"));
		this.model.addElement(new ToolbarSpringModel("spring", 1));
		
		// closeHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("closeHandles"));
		curButtonset.addButton(new ToolbarButtonModel("close", {'type': 'buttonDefault'}, {'label': this.titles.close, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-close'}}, this.titles.close, true, false));
	},
		
	/**
	 * function
	 * @name getModel
	 * @memberOf StandardToolbarModelProvider#
	 * @description returns a complete toolbar model with main functionalities
	 * @return {Object} returns the complete model for a main iview2 toolbar
	 */
    getModel : function() {
		return this.model;
    }
};

