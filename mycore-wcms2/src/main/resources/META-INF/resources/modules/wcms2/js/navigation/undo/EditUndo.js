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
