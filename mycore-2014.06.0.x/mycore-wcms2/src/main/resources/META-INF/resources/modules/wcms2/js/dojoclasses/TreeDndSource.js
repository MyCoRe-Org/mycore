dojo.provide("dojoclasses.TreeDndSource");
dojo.require("dijit.tree.dndSource");

dojo.declare("dojoclasses.TreeDndSource", dijit.tree.dndSource, {

	onMouseDown: function(e) {
		// this is a workaround to fix dnd support in tree
		// the id is only set if the scrollbar is hit, this allows us
		// to abort the event and don't do an invalid drag
		var element = document.elementFromPoint(e.clientX, e.clientY);
		var id = element.id;
		if(id != null && id != "") {
			return;
		}
		this.inherited("onMouseDown", arguments);		
	},

});
