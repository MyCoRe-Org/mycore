require(["dojo/ready"], function(ready) {
	ready(function() {
		require([
		    "dojo/topic",
		    "dojo/query",
         	"dojo/NodeList-manipulate",
         	"dojo/data/ItemFileReadStore",
         	"dojo/data/ItemFileWriteStore",

         	"dojo/io/iframe",

         	"dijit/dijit",
         	"dijit/Dialog",

         	"dijit/Tree",
         	"dijit/tree/TreeStoreModel",
         	"dijit/tree/dndSource",

         	"dijit/layout/ContentPane",
         	"dijit/layout/BorderContainer",

         	"dijit/form/Form",
         	"dijit/form/Button",
         	"dijit/form/TextBox",
         	"dijit/form/Textarea",
         	"dijit/form/ValidationTextBox",
         	"dijit/form/Select",
         	"dijit/form/CheckBox",
         	"dijit/form/SimpleTextarea",

         	"dijit/Tooltip",
         	"dijit/Toolbar",
         	"dijit/ToolbarSeparator"
		], function(topic) {
			ready(function() {
				topic.publish("dojo/included");
			});
		});
	});
});
