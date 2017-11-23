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
