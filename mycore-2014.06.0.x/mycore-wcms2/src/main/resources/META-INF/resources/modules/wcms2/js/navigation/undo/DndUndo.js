/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Created when a new tree item is inserted.
 */
wcms.navigation.DndUndo = function(/*TreeModel*/ treeModel) {
	this.treeModel = treeModel;
	this.draggedItems = [];
};

( function() {

	function getLabel() {
		return "Dnd";
	}

	function dndStart(source, nodes) {
		this.draggedItems = [];
    	dojo.forEach(nodes, dojo.hitch(this, function(node) {
    		var sourceItem = source.getItem(node.id);
    		var item = sourceItem.data.item;
    		var parent = sourceItem.data.getParent();
    		if(parent == null)
    			return;
    		var parentItem = parent.item;
    		var wcmsId = item.wcmsId[0];
    		var index = this.treeModel.indexAt(parentItem, wcmsId);
    		if(index == -1) {
    			console.log("Unexcpected Error occur while getting index of dnd item.");
    			return;
    		}
    		this.draggedItems.push({
    			"wcmsId" : wcmsId,
    			"oldParent" : parentItem,
    			"oldIndex" : index
    		});
    	}));
	}

	function dndDrop(source, nodes) {
    	dojo.forEach(nodes, dojo.hitch(this, function(node) {
    		var sourceItem = source.getItem(node.id);
    		var item = sourceItem.data.item;
    		var parent = sourceItem.data.getParent();
    		if(parent == null)
    			return;
    		var parentItem = parent.item;
    		var itemId = item.wcmsId[0];
    		var index = this.treeModel.indexAt(parentItem, itemId);
    		if(index == -1) {
    			console.log("Unexcpected Error occur while getting index of dnd item.");
    			return;
    		}
    		for(var i = 0; i < this.draggedItems.length; i++) {
    			var draggedItem = this.draggedItems[i];
    			if(itemId == draggedItem.wcmsId) {
    				draggedItem.newParent = parentItem;
    				draggedItem.newIndex = index;
    				break;
    			}
    		}
    	}));
	}

	function undo() {
		this.draggedItems.sort(oldIndexSort);
		for(var i = 0; i < this.draggedItems.length; i++) {
			var di = this.draggedItems[i];
			this.treeModel.move(di.wcmsId, di.newParent, di.oldParent, di.oldIndex);
		}
	}

	function redo() {
		this.draggedItems.sort(newIndexSort);
		for(var i = 0; i < this.draggedItems.length; i++) {
			var di = this.draggedItems[i];
			this.treeModel.move(di.wcmsId, di.oldParent, di.newParent, di.newIndex);
		}
	}

	function oldIndexSort(a, b) {
		return parseInt(a.oldIndex) - parseInt(b.oldIndex);
	}
	function newIndexSort(a, b) {
		return parseInt(a.newIndex) - parseInt(b.newIndex);
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.navigation.DndUndo;

	wcms.navigation.DndUndo.prototype.getLabel = getLabel;
	wcms.navigation.DndUndo.prototype.dndStart = dndStart;
	wcms.navigation.DndUndo.prototype.dndDrop = dndDrop;
	wcms.navigation.DndUndo.prototype.undo = undo;
	wcms.navigation.DndUndo.prototype.redo = redo;
})();