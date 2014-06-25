/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Created when a new tree item is inserted.
 */
wcms.navigation.EditUndo = function(newItem, oldItem, /*NavigationContent*/ content) {
	this.newItem = newItem;
	this.oldItem = oldItem;
	this.content = content;
};

( function() {

	function getLabel() {
		return "Edit Item";
	}

	function undo() {
		this.content.updateItem(this.oldItem);
	}

	function redo() {
		this.content.updateItem(this.newItem);
	}

	function merge(/*UndoableEdit*/ edit) {
		this.newItem = edit.newItem;
	}

	function isAssociated(/*UndoableEdit*/ edit) {
		if(!(edit instanceof wcms.navigation.EditUndo))
			return false;
		var item1 = this.oldItem;
		var item2 = edit.oldItem;
		if(item1.wcmsId != item2.wcmsId)
			return false;
		return true;
	}

	// inheritance
	wcms.common.UndoableMergeEdit.prototype = new wcms.navigation.EditUndo;

	wcms.navigation.EditUndo.prototype.getLabel = getLabel;
	wcms.navigation.EditUndo.prototype.undo = undo;
	wcms.navigation.EditUndo.prototype.redo = redo;
	wcms.navigation.EditUndo.prototype.merge = merge;
	wcms.navigation.EditUndo.prototype.isAssociated = isAssociated;
})();