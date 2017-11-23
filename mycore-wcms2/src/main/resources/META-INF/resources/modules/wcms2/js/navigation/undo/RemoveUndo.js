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

/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Created when one or more tree items are removed.
 */
wcms.navigation.RemoveUndo = function(/*NavigationContent*/ content, treeModel) {
	this.content = content;
	this.treeModel = treeModel;
	this.removedItems = [];
};

( function() {

	function create(/*Array<dojo.data.item>*/ treeItems) {
		var subIdList = [];
		// add to undo manager
		for(var i = 0; i < treeItems.length; i++) {
			var treeItem = treeItems[i];
			var id = treeItem.wcmsId[0];
			var parent = this.treeModel.getParent(id);
			var index = this.treeModel.indexAt(parent, id);
			var itemList = [];
			itemList.push(this.content.getItem(id));
			var hierarchy = {
				wcmsId: id,
				children : this.treeModel.getItemHierarchy(treeItem, dojo.hitch(this, function(hierarchyItem) {
					itemList.push(this.content.getItem(hierarchyItem.wcmsId));
					subIdList.push(hierarchyItem.wcmsId);
				}))
			};
			var removedItem = {
				wcmsId: id,
				parent : parent,
				index: index,
				hierarchy : hierarchy,
				itemList : itemList
			};
			this.removedItems.push(removedItem);
		}
		// only top items are important -> filter sub items
		this.removedItems = dojo.filter(this.removedItems, function(item, index){
		    return dojo.indexOf(subIdList, item.wcmsId) == -1;
		});
	}

	function getLabel() {
		return "Remove Item";
	}

	function undo() {
		for(var i = 0; i < this.removedItems.length; i++) {
			var removedItem = this.removedItems[i];
			// restore navigation content items		
			for(var j = 0; j < removedItem.itemList.length; j++) {
				this.content.addItem(removedItem.itemList[j]);
			}
			// restore tree hierarchy
			this.treeModel.paste(removedItem.hierarchy, removedItem.parent, removedItem.index);
		}
	}

	function redo() {
		for(var i = 0; i < this.removedItems.length; i++) {
			var removedItem = this.removedItems[i];
			// delete items
			for(var j = 0; j < removedItem.itemList.length; j++) {
				this.content.deleteItem(removedItem.itemList[i].wcmsId);
			}
			// remove items from tree
			this.treeModel.remove(removedItem.wcmsId);
		}
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.navigation.RemoveUndo;

	wcms.navigation.RemoveUndo.prototype.create = create;
	wcms.navigation.RemoveUndo.prototype.getLabel = getLabel;
	wcms.navigation.RemoveUndo.prototype.undo = undo;
	wcms.navigation.RemoveUndo.prototype.redo = redo;
})();
