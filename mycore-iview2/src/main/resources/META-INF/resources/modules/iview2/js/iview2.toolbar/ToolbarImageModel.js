/**
 * @class
 * @constructor
 * @name		ToolbarImageModel
 * @description simple model for images within Toolbar
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the text element
 * 			String:		type				//type text, to differ between buttonsets, dividers and text
 * 			integer:	src					//src of the picture which shall be displayed
 * 			Object:		relatedToolbar		//related toolbar model to navigate from the text to its toolbar
 * 		}
 */
var ToolbarImageModel = function (elementName, src) {
    this.elementName = elementName;
    this.type = "image";
    this.src = src;
    // will set indirectly while adding
    this.relatedToolbar = null;
};
