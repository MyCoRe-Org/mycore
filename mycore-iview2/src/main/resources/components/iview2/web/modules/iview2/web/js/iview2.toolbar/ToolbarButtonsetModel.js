/**
 * @class
 * @name ToolbarButtonsetModel
 * @description model of a toolbar buttonset with its functionalities to manage contained buttons
 * @strcuture	
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

    this.events = new iview.Event(this);
};

ToolbarButtonsetModel.prototype = {

	/**
	 * @function
	 * @name getButton
	 * @memberOf ToolbarButtonsetModel#
	 * @description returns a single button model out of the buttonset containing buttons
	 * @param buttonName name that identifies a single button model
	 * @return {Object} returns a single button model, identified by its buttonName
	 */
    getButton : function(buttonName) {
	    for (var i = 0; i < this.buttons.length; i++) {
			if (this.buttons[i].elementName == buttonName) {
				return this.buttons[i];
			}
		}
    },

	/**
	 * @function
	 * @name addButton
	 * @memberOf ToolbarButtonsetModel#
	 * @description adds a single button model to the buttonset,
	 *  notify the event-listener (to pass the informations to the toolbar model)
	 * @param {Object} button defines the whole button model
	 * @param {integer} index if set, defines the special position where to add the button between the other predefined buttons
	 */
    addButton : function(button, index) {
    	var button = jQuery.extend(button, {'relatedButtonset' : this});
    	
    	var myself = this;
    	// Events aus dem Button-Model "weiterleiten"
    	button.events.attach(function (sender, args) {
	    	myself.events.notify(jQuery.extend(args, {'buttonName' : button.elementName}));
	    });
    	
     	if (!isNaN(index)) {
     		this.buttons = this.buttons.slice(0, index).concat(button, this.buttons.slice(index, this.buttons.length));
     		this.events.notify({'type' : "add", 'button' : jQuery.extend(button, {'index' : index})});
     	} else {
     		this.buttons.push(button);
			this.events.notify({'type' : "add", 'button' : button});
     	}
    },

	/**
	 * @function
	 * @name removeButton
	 * @memberOf ToolbarButtonsetModel#
	 * @description removes a single button model from the buttonset,
	 *  notify the event-listener (to pass the informations to the toolbar model)
	 * @param buttonName name that identifies a single button model
	 */     
    removeButton : function(buttonName) {
     	var button = this.getButton(buttonName);
     	this.buttons.splice(button.index, 1);
     	this.events.notify({'type' : "del", 'button' : button});
    }
};
