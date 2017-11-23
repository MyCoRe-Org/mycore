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

wcms.gui.SimpleDialog = function(/*Type*/ type, /*String*/ i18nTitle, /*String*/ i18nText, /*String*/ imageUrl) {
	this.constructor();

	this.type = type;
	this.i18nTitle = i18nTitle;
	this.i18nText = i18nText;
	this.imageUrl = imageUrl;
	
	this.contentTable = null;
	this.textTd = null;
	this.imageElement = null;
};

( function() {

	function setText(/*String*/ i18nText) {
		this.i18nText = i18nText;
		this.updateText();
	}

	function setImage(/*String*/ imageUrl) {
		this.image = imageUrl;
		this.updateImage();
	}

	function createContent() {
		// create
		this.contentTable = dojo.create("table");
		var tr = dojo.create("tr");
		var imageTd = dojo.create("td");
		this.textTd = dojo.create("td");
		this.imageElement = dojo.create("img", {style: "padding-right: 10px;"});
		// structure
		this.content.appendChild(this.contentTable);
		this.contentTable.appendChild(tr);
		tr.appendChild(imageTd);
		tr.appendChild(this.textTd);
		imageTd.appendChild(this.imageElement);
		// set image & text
		this.setImage(this.imageUrl);
		this.setText(this.i18nText);
	}

	function updateText() {
		I18nManager.getInstance().getI18nText(this.i18nText, null, dojo.hitch(this, function(/*String*/ text) {
			dojo.attr(this.textTd, {innerHTML: text});
		}));
	}

	function updateImage() {
		if(this.imageUrl == null) {
			dojo.style(this.imageElement, "display", "none");
			dojo.attr(this.imageElement, {src: ""});
		} else {
			dojo.style(this.imageElement, "display", "block");
			dojo.attr(this.imageElement, {src: this.imageUrl});
		}
	}

	function updateLang() {
		if(!this.created)
			return;
		// super.updateLang();
		wcms.gui.AbstractDialog.prototype.updateLang.call(this);
		// update text
		var updateTextFunc = dojo.hitch(this, updateText);
		updateTextFunc();
	}

	// inheritance
	wcms.gui.SimpleDialog.prototype = new wcms.gui.AbstractDialog;

	
	wcms.gui.SimpleDialog.prototype.updateText = updateText;
	wcms.gui.SimpleDialog.prototype.updateImage = updateImage;
	wcms.gui.SimpleDialog.prototype.updateLang = updateLang;
	wcms.gui.SimpleDialog.prototype.createContent = createContent;
	wcms.gui.SimpleDialog.prototype.setText = setText;
	wcms.gui.SimpleDialog.prototype.setImage = setImage;
})();
