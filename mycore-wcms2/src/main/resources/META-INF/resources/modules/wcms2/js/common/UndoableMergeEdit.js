/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.common = wcms.common || {};

/**
 *
 */
wcms.common.UndoableMergeEdit = function() {
};

( function() {

	function merge(/*UndoableEdit*/ mergeWith) {
		// overwrite this method!
	}

	/**
	 * This method checks if the given edit is associated with
	 * the merge edit. In general this method is called by the
	 * UndoManager to check if both edits are merged.
	 */
	function isAssociated(/*UndoableEdit*/ edit) {
		// overwrite this method!
		return false;
	}

	// inheritance
	wcms.common.UndoableEdit.prototype = new wcms.common.UndoableMergeEdit;

	wcms.common.UndoableMergeEdit.prototype.merge = merge;
	wcms.common.UndoableMergeEdit.prototype.isAssociated = isAssociated;

})();
