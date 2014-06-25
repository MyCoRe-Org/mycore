/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Created when a new tree item is inserted.
 */
wcms.navigation.RestoreUndo = function(newItem, oldItem, /*NavigationContent*/ content) {
	this.newItem = newItem;
	this.oldItem = oldItem;
	this.content = content;
};

( function() {

	function getLabel() {
		return "Restore Item";
	}

	function undo() {
		this.content.updateItem(this.newItem);
	}

	function redo() {
		this.content.updateItem(this.oldItem);
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.navigation.RestoreUndo;

	wcms.navigation.EditUndo.prototype.getLabel = getLabel;
	wcms.navigation.RestoreUndo.prototype.undo = undo;
	wcms.navigation.RestoreUndo.prototype.redo = redo;

})();