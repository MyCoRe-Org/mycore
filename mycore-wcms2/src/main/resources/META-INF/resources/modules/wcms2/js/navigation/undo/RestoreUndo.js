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
