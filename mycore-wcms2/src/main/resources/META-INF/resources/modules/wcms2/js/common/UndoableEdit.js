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
