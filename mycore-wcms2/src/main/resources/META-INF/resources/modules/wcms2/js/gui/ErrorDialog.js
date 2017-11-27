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

var wcms = wcms || {};
wcms.gui = wcms.gui || {};

wcms.gui.ErrorDialog = function(/*String*/ i18nTitle, /*String*/ i18nText, /*String*/ error) {
	this.constructor();
	
	this.type = this.Type.ok;
	this.i18nTitle = i18nTitle;
	this.i18nText = i18nText;
	this.imageUrl = imagePath + "/warning.png";
	this.error = error;
};

( function() {

	function createContent() {
		// super.createContent();
		wcms.gui.SimpleDialog.prototype.createContent.call(this);

		if(this.error == null)
			return;

		var tr = dojo.create("tr", {style: "color: red"});
		var emptyTd = dojo.create("td");
		var stackTraceTd = dojo.create("td", {
			innerHTML : this.error
		});
		tr.appendChild(emptyTd);
		tr.appendChild(stackTraceTd);
		this.contentTable.appendChild(tr);

		this.setWidth(700);
	}

	// inheritance
	wcms.gui.ErrorDialog.prototype = new wcms.gui.SimpleDialog;

	wcms.gui.ErrorDialog.prototype.createContent = createContent;

})();
