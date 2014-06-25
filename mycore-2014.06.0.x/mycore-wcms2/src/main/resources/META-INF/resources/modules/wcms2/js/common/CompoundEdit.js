/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.common = wcms.common || {};

/**
 * A collection of undoable edits.
 */
wcms.common.CompoundEdit = function() {
	this.undoableEditList = [];
};

( function() {

	function addEdit(/*UndoableEdit*/ edit) {
		this.undoableEditList.push(edit);
	}

	function undo() {
		for(var i = this.undoableEditList.length - 1; i >= 0; i--) {
			this.undoableEditList[i].undo();
		}
	}

	function redo() {
		for(var i = 0; i < this.undoableEditList.length; i++) {
			this.undoableEditList[i].redo();
		}
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.common.CompoundEdit;

	wcms.common.CompoundEdit.prototype.addEdit = addEdit;
	wcms.common.CompoundEdit.prototype.undo = undo;
	wcms.common.CompoundEdit.prototype.redo = redo;

})();
