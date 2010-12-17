/**
 * @class
 * @name PreviewToolbarModelProvider
 * @description provides the model of a preview toolbar with only navigation functionalities/buttons,
 *  especially forward and backward buttons and of course a possibility to switch to the main viewer screen
 * @param {String} id is the identifier of the model provider
 * @param {String} model provides the toolbar model with fix defined structure
 * @param {AssoArray} tites defines an array of the neccesary button titles, parsed out of the iview2 xsl
 * 		}
 */
var PreviewToolbarModelProvider = function (id, titles) {
	this.id = id;
    this.model = new ToolbarModel(id);
    this.titles = titles;
};

PreviewToolbarModelProvider.prototype = {

	/**
	 * @function
	 * @name getModel
	 * @memberOf PreviewToolbarModelProvider#
	 * @description returns a complete toolbar model with only navigation functionalities
	 * @return {Object} returns the complete model for a preview iview2 toolbar
	 */
    getModel : function() {

    	var curButtonset;

		// previewHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("previewBack", this.model));
		curButtonset.addButton(new ToolbarButtonModel("backward", {'type': 'buttonDefault'}, {'label': this.titles.backward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-backward'}}, this.titles.backward, true, false));

		curButtonset = this.model.addElement(new ToolbarButtonsetModel("previewForward", this.model));
		curButtonset.addButton(new ToolbarButtonModel("forward", {'type': 'buttonDefault'}, {'label': this.titles.forward, 'text': false, 'icons': {primary : 'iview2-icon iview2-icon-forward'}}, this.titles.forward, true, false));
		
		return this.model;
    }
};

