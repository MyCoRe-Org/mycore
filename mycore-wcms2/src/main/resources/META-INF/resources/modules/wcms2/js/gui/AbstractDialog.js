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

/**
 * Dialog which supports three types of options:
 * -Ok
 * -Cancel
 * -Ok, Cancel
 * -Yes, No
 * -Yes, No, Cancel
 * 
 * How to use this class @see SimpleDialog.js.
 */
wcms.gui.AbstractDialog = function() {
	this.Type = {
		ok:"ok",
		cancel:"cancel",
		okCancel:"okCancel",
		yesNo:"yesNo",
		yesNoCancel:"yesNoCancel"
	};

	this.i18nTitle = null;
	this.type = null;

	this.internalDialog = null;

	this.content = null;

	this.okButton = null;
	this.cancelButton = null;
	this.yesButton = null;
	this.noButton = null;

	this.created = false;
	this.eventHandler = new wcms.common.EventHandler(this);

	// help data
	this.additionalData = undefined;
	
	// style
	this.DEFAULT_WIDTH = 500;
};

( function() {

	function setTitle(/*String*/ i18nTitle) {
		this.i18nTitle = i18nTitle;
		var updateTitleFunc = dojo.hitch(this, updateTitle);
		updateTitleFunc();
	}

	function getTitle() {
		return this.internalDialog.get("title");
	}

	function updateTitle() {
		I18nManager.getInstance().getI18nText(this.i18nTitle, null, dojo.hitch(this, function(/*String*/ title) {
			this.internalDialog.set("title", title);
		}));
	}

	function create() {
		// create dijit components
		this.internalDialog = new dijit.Dialog();
		this.okButton = new dijit.form.Button({i18n: "component.wcms.general.ok"});
		this.cancelButton = new dijit.form.Button({i18n: "component.wcms.general.cancel"});
		this.yesButton = new dijit.form.Button({i18n: "component.wcms.general.yes"});
		this.noButton = new dijit.form.Button({i18n: "component.wcms.general.no"});

		// create dialog
		var table = dojo.create("table");
		this.internalDialog.set("content", table);

		var tr = dojo.create("tr");
		this.content = dojo.create("td");
		dojo.addClass(this.content, "dialogContentPanel");
		this.setWidth(this.DEFAULT_WIDTH);
		tr.appendChild(this.content);
		table.appendChild(tr);

		tr = dojo.create("tr");
		var td = dojo.create("td");
		dojo.addClass(td, "dialogOkPanel");

		if(this.type == this.Type.ok) {
			td.appendChild(this.okButton.domNode);
		} else if(this.type == this.Type.cancel) {
			td.appendChild(this.cancelButton.domNode);
		} else if(this.type == this.Type.okCancel) {
			td.appendChild(this.cancelButton.domNode);
			td.appendChild(this.okButton.domNode);
		} else if(this.type == this.Type.yesNo) {
			td.appendChild(this.noButton.domNode);
			td.appendChild(this.yesButton.domNode);				
		} else if(this.type == this.Type.yesNoCancel) {
			td.appendChild(this.cancelButton.domNode);
			td.appendChild(this.noButton.domNode);
			td.appendChild(this.yesButton.domNode);				
		}
		tr.appendChild(td);
		table.appendChild(tr);

		// add button listener
		dojo.connect(this.okButton, "onClick", this, function() {
			this.onBeforeOk();
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "okButtonClicked", "additionalData": this.additionalData});
		});
		dojo.connect(this.cancelButton, "onClick", this, function() {
			this.onBeforeCancel();
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "cancelButtonClicked", "additionalData": this.additionalData});
		});
		dojo.connect(this.yesButton, "onClick", this, function() {
			this.onBeforeYes();
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "yesButtonClicked", "additionalData": this.additionalData});
		});
		dojo.connect(this.noButton, "onClick", this, function() {
			this.onBeforeNo();
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "noButtonClicked", "additionalData": this.additionalData});
		});
		this.created = true;
	}

	function show() {
		if(!this.created) {
			var createFunc = dojo.hitch(this, create);
			createFunc();
			if(this.createContent) {
				this.createContent();
			} else {
				console.log("TODO: add a 'createContent' method to your dialog!");
			}
			this.updateLang();
		}

		// show
		if(this.beforeShow) {
			this.beforeShow();
		}
		this.internalDialog.show();
	}

	function updateLang() {
		if(!this.created)
			return;
		var updateTitleFunc = dojo.hitch(this, updateTitle);
		updateTitleFunc();
		I18nManager.getInstance().updateI18nButton(this.okButton);
		I18nManager.getInstance().updateI18nButton(this.cancelButton);
		I18nManager.getInstance().updateI18nButton(this.yesButton);
		I18nManager.getInstance().updateI18nButton(this.noButton);
	}

	function setWidth(/*Integer*/ newWidth) {
		dojo.style(this.content, "width", newWidth + "px");
	}

	// override this method
	function onBeforeOk() {
	}
	// override this method
	function onBeforeCancel() {
	}
	// override this method
	function onBeforeYes() {
	}
	// override this method
	function onBeforeNo() {
	}

	wcms.gui.AbstractDialog.prototype.updateLang = updateLang;
	wcms.gui.AbstractDialog.prototype.setTitle = setTitle;
	wcms.gui.AbstractDialog.prototype.getTitle = getTitle;
	wcms.gui.AbstractDialog.prototype.show = show;
	wcms.gui.AbstractDialog.prototype.setWidth = setWidth;
	
	// methods to override
	wcms.gui.AbstractDialog.prototype.onBeforeOk = onBeforeOk;
	wcms.gui.AbstractDialog.prototype.onBeforeCancel = onBeforeCancel;
	wcms.gui.AbstractDialog.prototype.onBeforeYes = onBeforeYes;
	wcms.gui.AbstractDialog.prototype.onBeforeNo = onBeforeNo;
})();
