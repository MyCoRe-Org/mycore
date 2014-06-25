/**
 * @class
 * @constructor
 * @name		ToolbarButtonsetModel
 * @description model of a toolbar buttonset with its functionalities to manage contained buttons
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the buttonset
 * 			String: 	type				//type buttonset, to differ between buttonsets, text and dividers
 * 			Array:		buttons				//array of the contained buttons within the buttonset
 * 			Object:		relatedToolbar		//related toolbar model to navigate from the buttonset to its toolbar
 * 			Event:		events				//to trigger defined actions, while managing contained buttons
 * 		}
 */
var ToolbarButtonsetModel = function (elementName) {
    this.elementName = elementName;
    this.type = "buttonset";
    this.buttons = [];
    // will set indirectly while adding
    this.relatedToolbar = null;
};

ToolbarButtonsetModel.prototype = {

	/**
	 * @public
	 * @function
	 * @name		getButton
	 * @memberOf	ToolbarButtonsetModel#
	 * @description returns a single button model out of the buttonset containing buttons
	 * @param		{string} buttonName name that identifies a single button model
	 * @return		{Object} returns a single button model, identified by its buttonName
	 */
    getButton : function(buttonName) {
	    for (var i = 0; i < this.buttons.length; i++) {
			if (this.buttons[i].elementName == buttonName) {
				return this.buttons[i];
			}
		}
    },

	/**
	 * @public
	 * @function
	 * @name		addButton
	 * @memberOf	ToolbarButtonsetModel#
	 * @description adds a single button model to the buttonset,
	 *  notify the event-listener (to pass the informations to the toolbar model)
	 * @param		{Object} button defines the whole button model
	 * @param		{integer} index if set, defines the special position where to add the button between the other predefined buttons
	 */
    addButton : function(button, index) {
    	button.relatedButtonset = this;
    	
    	var that = this;
    	// Events aus dem Button-Model "weiterleiten"
    	jQuery(button).bind("changeState changeLoading changeActive del", function (e, val) {
	    	jQuery(that).trigger(e.type, jQuery.extend(val, {'buttonName' : button.elementName}));
	    });
    	
     	if (!isNaN(index)) {
     		this.buttons = this.buttons.slice(0, index).concat(button, this.buttons.slice(index, this.buttons.length));
     		jQuery(this).trigger("add", {'button' : jQuery.extend(button, {'index' : index})});
     	} else {
     		this.buttons.push(button);
			jQuery(this).trigger("add", {'button' : button});
     	}
    },

	/**
	 * @function
	 * @name		removeButton
	 * @memberOf	ToolbarButtonsetModel#
	 * @description removes a single button model from the buttonset,
	 *  notify the event-listener (to pass the informations to the toolbar model)
	 * @param		{string} buttonName name that identifies a single button model
	 */     
    removeButton : function(buttonName) {
     	var button = this.getButton(buttonName);
     	this.buttons.splice(button.index, 1);
     	jQuery(this).trigger("del", {'button' : button});
    }
};
