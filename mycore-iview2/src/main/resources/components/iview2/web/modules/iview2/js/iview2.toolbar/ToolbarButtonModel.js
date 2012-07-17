/**
 * @class
 * @constructor
 * @name		ToolbarButtonModel
 * @description model of a toolbar button with its functionalities to manipulate button properties
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the button (must not have any space)
 * 			String: 	type				//type button, to differ between other elements
 * 			String: 	subtype				//defines if the button is a checkbutton or a standard one
 * 			AssoArray: 	ui					//jQuery button ui informations (label, text, icons) to render the button into the view
 * 			String:		captionId			//id for the translation to use on the button
 * 			Boolean:	active				//describes if a button is enabled or disabled currently
 * 			Boolean:	loading				//describes if a button is shows loading symbol or the defined ui content
 * 			Object:		relatedButtonset	//related buttonset model to navigate from the button to its buttonset
 * 			Event:		events				//to trigger defined actions, while manipulate button properties
 * 		}
 */
var ToolbarButtonModel = function (elementName, subtype, ui, captionId, active, loading) {
    this.elementName = elementName;
    this.type = "button";
    this.subtype = subtype;
    this.ui = (!ui.text && !ui.label)? jQuery.extend(ui, {'label':"a", "text":false}): ui;//if label is omitted the button is displayed too small
    this.captionId = captionId;
    this.active = active;
    this.loading = loading;
    // will set indirectly while adding
    this.relatedButtonset = null;
};

ToolbarButtonModel.prototype = {
		/**
		 * @public
		 * @function
		 * @name		setActive
		 * @memberOf	ToolbarButtonModel#
		 * @description enables or disables the button and triggers the related event
		 * @param		{boolean} active defines the target state of activation (true or false)
		 */
		setActive: function(active) {
			this.active = active;
			jQuery(this).trigger("changeActive", {'active' : this.active});
		},
		
		/**
		 * @public
		 * @function
		 * @name		setLoading
		 * @memberOf	ToolbarButtonModel#
		 * @description enables or disables the loading facility of a button and triggers the related event
		 * @param		{boolean} active defines the loading state (true or false)
		 */
		setLoading: function(loading) {
			this.loading = loading;
			jQuery(this).trigger("changeLoading", {'loading' : this.loading});
		},
		
		/**
		 * @public
		 * @function
		 * @name		setSubtypeState
		 * @memberOf	ToolbarButtonModel#
		 * @description press or unpress a checkbutton and triggers the related event
		 * @param		{boolean} state defines the target state of pressing (true or false)
		 */
		setSubtypeState: function(state) {
			this.subtype.state = state;
			jQuery(this).trigger("changeState", {'state' : this.subtype.state});
		},
		
		/**
		 * @public
		 * @function
		 * @name		changeSubtypeState
		 * @memberOf	ToolbarButtonModel#
		 * @description simply changes the pressing state of a checkbutton and triggers the related event
		 */
		changeSubtypeState: function() {
			this.subtype.state = !this.subtype.state;
			jQuery(this).trigger("changeState", {'state' : this.subtype.state});
		},
		
		remove: function() {
			jQuery(this).trigger("del", {'button' : this});
		}
};