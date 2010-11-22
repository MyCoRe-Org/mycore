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
};

StandardToolbarModelProvider.prototype = {

	/**
	 * function
	 * @name getModel
	 * @memberOf StandardToolbarModelProvider#
	 * @description returns a complete toolbar model with main functionalities
	 * @return {Object} returns the complete model for a main iview2 toolbar
	 */
    getModel : function() {
    	
    	// only for testing
    	//$(".toolbars").append($('<div id="pages" style="visibility: hidden; z-index: 80; position: absolute; left: -9999px;" class="hidden"><ul><li><a href="index.html#">1</a></li><li><a onclick="testa()" href="index.html#">2</a></li><li><a href="index.html#">3</a></li><li><a href="index.html#">4</a></li><li><a href="index.html#">5</a></li><li><a href="index.html#">6</a></li><li><a href="index.html#">7</a></li><li><a href="index.html#">8</a></li><li><a href="index.html#">9</a></li><li><a href="index.html#">10</a></li><li><a href="index.html#">11</a></li><li><a href="index.html#">13</a></li><li><a href="index.html#">14</a></li><li><a href="index.html#">15</a></li><li><a href="index.html#">16</a></li><li><a href="index.html#">17</a></li><li><a href="index.html#">18</a></li><li><a href="index.html#">19</a></li><li><a href="index.html#">20</a></li><li><a href="index.html#">21</a></li></ul></div>'));  	
    	
    	var curButtonset;

		// zoomHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("zoomHandles"));
		curButtonset.addButton(new ToolbarButtonModel("zoomIn", {'type': 'buttonDefault'}, {'label': this.titles.zoomIn, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-zoomIn'}}, this.titles.zoomIn, true));
		curButtonset.addButton(new ToolbarButtonModel("zoomOut", {'type': 'buttonDefault'}, {'label': this.titles.zoomOut, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-zoomOut'}}, this.titles.zoomOut, true));
		curButtonset.addButton(new ToolbarButtonModel("fitToWidth", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.fitToWidth, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-fitToWidth'}}, this.titles.fitToWidth, true));
		curButtonset.addButton(new ToolbarButtonModel("fitToScreen", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.fitToScreen, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-fitToScreen'}}, this.titles.fitToScreen, true));
		
		// overviewHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("overviewHandles"));
		curButtonset.addButton(new ToolbarButtonModel("openOverview", {'type': 'buttonDefault'}, {'label': this.titles.openOverview, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-overview'}}, this.titles.openOverview, false));
		curButtonset.addButton(new ToolbarButtonModel("openChapter", {'type': 'buttonCheck', 'state': false}, {'label': this.titles.openChapter, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-chapter'}}, this.titles.openChapter, false));
		
		// navigateHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("navigateHandles"));
		curButtonset.addButton(new ToolbarButtonModel("backward", {'type': 'buttonDefault'}, {'label': this.titles.backward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-backward'}}, this.titles.backward, true));
		// label should be current (first loaded page)
		curButtonset.addButton(new ToolbarButtonModel("pageBox", {'type': 'buttonDefault'}, {'label': "", 'text': true, 'icons' : {primary: 'iview2-icon iview2-icon-empty', secondary : 'iview2-icon iview2-icon-dropDownArrow'}}, this.titles.pageBox, true))
		curButtonset.addButton(new ToolbarButtonModel("forward", {'type': 'buttonDefault'}, {'label': this.titles.forward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-forward'}}, this.titles.forward, true));
		
		// permalinkHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("permalinkHandles"));
		curButtonset.addButton(new ToolbarButtonModel("permalink", {'type': 'buttonCheck', 'state' : false}, {'label': this.titles.permalink, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-permalink'}}, this.titles.permalink, true));
		
		this.model.addElement(new ToolbarDividerModel("Strich"));
		
		// closeHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("closeHandles"));
		curButtonset.addButton(new ToolbarButtonModel("close", {'type': 'buttonDefault'}, {'label': this.titles.close, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-close'}}, this.titles.close, true));

		return this.model;
    }
};

