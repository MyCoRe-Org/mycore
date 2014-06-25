/**
 * @class
 * @constructor
 * @name 		PreviewToolbarModelProvider
 * @description provides the model of a preview toolbar with only navigation functionalities/buttons,
 *  especially forward and backward buttons and of course a possibility to switch to the main viewer screen
 * @param		{String} id is the identifier of the model provider
 */
var PreviewToolbarModelProvider = function (id) {
	this.id = id;
    this.model = new ToolbarModel(id);
};

PreviewToolbarModelProvider.prototype = {

	/**
	 * @function
	 * @name		getModel
	 * @memberOf	PreviewToolbarModelProvider#
	 * @description returns a complete toolbar model with only navigation functionalities
	 * @return		{Object} returns the complete model for a preview iview2 toolbar
	 */
    getModel : function() {
    	var curButtonset;

		// previewHandles
		curButtonset = this.model.addElement(new ToolbarButtonsetModel("previewBack", this.model));
		curButtonset.addButton(new ToolbarButtonModel("backward", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-backward'}}, "toolbar.backward", true, false));

		curButtonset = this.model.addElement(new ToolbarButtonsetModel("previewForward", this.model));
		curButtonset.addButton(new ToolbarButtonModel("forward", {'type': 'buttonDefault'}, {'icons': {primary : 'iview2-icon iview2-icon-forward'}}, "toolbar.forward", true, false));
		
		return this.model;
    }
};
