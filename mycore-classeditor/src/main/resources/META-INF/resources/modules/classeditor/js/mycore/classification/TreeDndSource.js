define([
	"dojo/_base/declare", // declare
	"dijit/tree/dndSource",
	"dojo/_base/lang" // hitch, clone
], function(declare, dndSource, lang) {

return declare("mycore.classification.TreeDndSource", dndSource, {

    enabled: true,

	onMouseDown: function(e) {
		// this is a workaround to fix dnd support in tree
		// the id is only set if the scrollbar is hit, this allows us
		// to abort the event and don't do an invalid drag
		let element = document.elementFromPoint(e.clientX, e.clientY);
		let id = element.id;
        if(id !== undefined && id !== null && id !== "" && !id.startsWith("dijit__TreeNode")) {
			return;
		}
		this.inherited("onMouseDown", arguments);		
	},

	onMouseMove: function(e) {
		if(!enabled) {
			return;
		}
		this.inherited("onMouseMove", arguments);
	},

	setEnabled: function(e) {
		enabled = e;
	},

	getSelectedTreeNodes: function(){
        let nodes = this.inherited("getSelectedTreeNodes", arguments);
		// sort items by index not time of selection
		nodes.sort(lang.hitch(this, function(nodeA, nodeB) {
			return this.tree.model.indexAt(nodeB.item) - this.tree.model.indexAt(nodeA.item);
		}));
		return nodes;
	}

});
});
