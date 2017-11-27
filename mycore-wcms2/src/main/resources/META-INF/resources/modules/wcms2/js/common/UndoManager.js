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
 * UndoManager manages a list of UndoableEdits, providing a way to
 * undo or redo the appropriate edits.
 */
wcms.common.UndoManager = function() {
	this.pointer = -1;
	this.limit = 20;
	this.list = [];

	this.eventHandler = new wcms.common.EventHandler(this);
	
	this.forceNoMergeSwitch = false;
};

( function() {

	/**
	 * Adds a new edit action to the list
	 */
	function add(/*wcms.common.UndoableEdit*/ undoableEdit) {
		if(this.pointer < this.list.length - 1) {
			this.list = this.list.slice(0, this.pointer + 1);
		}
		if(this.list.length >= this.limit) {
			// to much undoable edits - remove first in row
			this.list = this.list.slice(1);
			this.pointer--;
		}

		// merge
		var lastUndoableEdit = this.list[this.pointer];
		if(	!this.forceNoMergeSwitch &&
			lastUndoableEdit != null &&
			undoableEdit.merge &&
			lastUndoableEdit.merge &&
			lastUndoableEdit.isAssociated(undoableEdit))
		{
			lastUndoableEdit.merge(undoableEdit);
			this.eventHandler.notify({
				"type" : "merged",
				"mergedEdit" : undoableEdit,
				"undoableEdit" : lastUndoableEdit
			});
		} else {
			// default case - add to list
			this.list.push(undoableEdit);
			this.pointer++;
			this.eventHandler.notify({"type" : "add", "undoableEdit" : undoableEdit});
		}
		this.forceNoMergeSwitch = false;
	}
	/**
	 * Checks if an undo operation is possible.
	 */
	function canUndo() {
		return this.pointer >= 0;
	}
	/**
	 * Checks if a redo operation is possible.
	 */
	function canRedo() {
		return this.pointer < this.list.length - 1;
	}

	function undo() {
		if(!this.canUndo())
			return;
		this.list[this.pointer].undo();
		this.pointer--;
		this.forceNoMerge();
		this.eventHandler.notify({"type" : "undo"});
	}
	function redo() {
		if(!this.canRedo())
			return;
		this.pointer++;
		this.list[this.pointer].redo();
		this.forceNoMerge();
		this.eventHandler.notify({"type" : "redo"});
	}

	/**
	 * This method forces the NEXT added undoable edit to
	 * be not merged.
	 */
	function forceNoMerge() {
		this.forceNoMergeSwitch = true;
	}

	wcms.common.UndoManager.prototype.add = add;
	wcms.common.UndoManager.prototype.canUndo = canUndo;
	wcms.common.UndoManager.prototype.canRedo = canRedo;
	wcms.common.UndoManager.prototype.undo = undo;
	wcms.common.UndoManager.prototype.redo = redo;
	wcms.common.UndoManager.prototype.forceNoMerge = forceNoMerge;

})();
