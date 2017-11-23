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
