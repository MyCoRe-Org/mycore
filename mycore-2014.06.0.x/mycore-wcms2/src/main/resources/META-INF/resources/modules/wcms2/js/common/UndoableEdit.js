/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.common = wcms.common || {};

/**
 * An UndoableEdit represents an edit. The edit may be undone,
 * or if already undone the edit may be redone.
 */
wcms.common.UndoableEdit = function() {
};

( function() {

	function getLabel() {
		return "no label defined";
	}

	function undo() {
		// overwrite this method!
	}

	function redo() {
		// overwrite this method!
	}

	wcms.common.UndoableEdit.prototype.getLabel = getLabel;
	wcms.common.UndoableEdit.prototype.undo = undo;
	wcms.common.UndoableEdit.prototype.redo = redo;

})();
