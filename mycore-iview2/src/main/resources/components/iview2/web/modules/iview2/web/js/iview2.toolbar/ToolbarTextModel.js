/**
 * @class
 * @constructor
 * @name		ToolbarTextModel
 * @description simple model of a toolbar text element (plain text within toolbar)
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the text element
 * 			String: 	type				//type text, to differ between buttonsets, dividers and text
 * 			String: 	text				//the shown content of the text element
 * 			Object:		relatedToolbar		//related toolbar model to navigate from the text to its toolbar
 * 		}
 */
var ToolbarTextModel = function (elementName, text) {
    this.elementName = elementName;
    this.type = "text";
    this.text = text;
    // will set indirectly while adding
    this.relatedToolbar = null;
};