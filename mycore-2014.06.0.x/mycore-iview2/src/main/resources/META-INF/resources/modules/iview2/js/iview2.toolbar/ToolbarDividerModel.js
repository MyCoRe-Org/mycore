/**
 * @class
 * @constructor
 * @name ToolbarDividerModel
 * @description simple model of a toolbar divider (vertical buttonset separator)
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the divider
 * 			String: 	type				//type divider, to differ between buttonsets, text and dividers
 * 			Object:		relatedToolbar		//related toolbar model to navigate from the divider to its toolbar
 * 		}
 */
var ToolbarDividerModel = function (elementName) {
    this.elementName = elementName;
    this.type = "divider";
    // will set indirectly while adding
    this.relatedToolbar = null;
};
