/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Created when a new tree item is inserted.
 */
wcms.navigation.InsertUndo = function(/*NavigationContent*/ content, /**/ treeModel, /*JSON*/ item, /*String*/ parentId) {
	this.content = content;
	this.treeModel = treeModel;
	this.item = item;
	this.parentId = parentId;
};

( function() {

	function getLabel() {
		return "Insert Item";
	}

	function undo() {
		this.content.deleteItem(this.item.wcmsId);
		this.treeModel.remove(this.item.wcmsId);
	}

	function redo() {
		this.content.addItem(this.item);
		this.treeModel.add(this.item.wcmsId, this.parentId);
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.navigation.InsertUndo;

	wcms.navigation.InsertUndo.prototype.getLabel = getLabel;
	wcms.navigation.InsertUndo.prototype.undo = undo;
	wcms.navigation.InsertUndo.prototype.redo = redo;
})();