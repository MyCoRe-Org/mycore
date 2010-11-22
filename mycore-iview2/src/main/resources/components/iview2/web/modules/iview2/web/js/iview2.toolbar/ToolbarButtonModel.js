/**
 * @class
 * @name ToolbarButtonModel
 * @description model of a toolbar button with its functionalities to manipulate button properties
 * @strcuture	
 * 		Object {
 * 			String:		elementName,		//name of the button
 * 			String: 	type				//type button, to differ between other elements
 * 			String: 	subtype				//defines if the button is a checkbutton or a standard one
 * 			AssoArray: 	ui					//jQuery button ui informations (label, text, icons) to render the button into the view
 * 			String:		title				//title of the button
 * 			Boolean:	active				//describes if a button is enabled or disabled currently
 * 			Object:		relatedButtonset	//related buttonset model to navigate from the button to its buttonset
 * 			Event:		events				//to trigger defined actions, while manipulate button properties
 * 		}
 */
var ToolbarButtonModel = function (elementName, subtype, ui, title, active) {
    this.elementName = elementName;
    this.type = "button";
    this.subtype = subtype;
    this.ui = ui;
    this.title = title;
    this.active = active;
    // will set indirectly while adding
    this.relatedButtonset = null;
    
    this.events = new iview.Event(this);
};

/**
 * @function
 * @name setActive
 * @memberOf ToolbarButtonModel#
 * @description enables or disables the button and triggers the related event
 * @param {boolean} active defines the target state of activation (true or false)
 */
ToolbarButtonModel.prototype.setActive = function(active) {
	this.active = active;
	this.events.notify({'type' : "changeActive", 'active' : this.active});
}

/**
 * @function
 * @name setSubtypeState
 * @memberOf ToolbarButtonModel#
 * @description press or unpress a checkbutton and triggers the related event
 * @param {boolean} state defines the target state of pressing (true or false)
 */
ToolbarButtonModel.prototype.setSubtypeState = function(state) {
	this.subtype.state = state;
	this.events.notify({'type' : "changeState", 'state' : this.subtype.state});
};

/**
 * @function
 * @name changeSubtypeState
 * @memberOf ToolbarButtonModel#
 * @description simply changes the pressing state of a checkbutton and triggers the related event
 */
ToolbarButtonModel.prototype.changeSubtypeState = function() {
	this.subtype.state = !this.subtype.state;
	this.events.notify({'type' : "changeState", 'state' : this.subtype.state});
};
