/**
 * @class
 * @constructor
 * @name		ToolbarSpringModel
 * @description simple model for a tension spring to keep space between Toolbar elements
 * @structure	
 * 		Object {
 * 			String:		elementName,		//name of the text element
 * 			String:		type				//type text, to differ between buttonsets, dividers and text
 * 			float:		weight				//the percentage of space the spring will hold, value between 0.0 <= w <= 1.0
 * 			Object:		relatedToolbar		//related toolbar model to navigate from the text to its toolbar
 * 		}
 */
var ToolbarSpringModel = function (elementName, weight) {
    this.elementName = elementName;
    this.type = "spring";
    this.weight = toFloat(weight);
    // will set indirectly while adding
    this.relatedToolbar = null;
};