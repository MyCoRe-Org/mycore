/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
