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
 * @package wcms.navigation
 * @description editor for a tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.MenuItemEditor = function() {
	this.constructor();

	// general
	this.i18nEditor = null;

	// link
	this.idTextBox = null;
	this.dirTextBox = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.menuItemEditor.mainHeader";
	var i18nText = "component.wcms.navigation.itemEditor.name";
	var idText = "component.wcms.navigation.menuItemEditor.id";
	var dirText = "component.wcms.navigation.rootItemEditor.dir";
	var idRequiredText = "component.wcms.navigation.menuItemEditor.idRequired";

	function create() {
		// create dijit componets
		this.idTextBox = new dijit.form.ValidationTextBox({
			required: true,
			intermediateChanges: true,
			i18nMissingMessage: idRequiredText
		});
		this.i18nEditor = new wcms.navigation.I18nEditor();
		this.i18nEditor.create();
		this.dirTextBox = new dijit.form.TextBox({intermediateChanges: true});

		var buildTableFunc = dojo.hitch(this, buildTable);
		buildTableFunc();

		// -id
		dojo.connect(this.idTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.id, value)) {
				this.currentItem.id = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -dir
		dojo.connect(this.dirTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.dir, value)) {
				this.currentItem.dir = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -i18n editor
		this.i18nEditor.eventHandler.attach(dojo.hitch(this, function(/*I18nEditor*/ source, /*Json*/ args) {
			if(this.currentItem == null)
				return;
			if(args.type == "i18nChanged") {
				if( this.currentItem.i18nKey == args.i18nKey ||
					(this.currentItem.i18nKey == undefined && args.i18nKey == "")) {
					return;
				}
				this.currentItem.i18nKey = args.i18nKey;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			} else if(args.type == "rowChanged" || args.type =="rowRemoved") {
				var newLabels = this.i18nEditor.labelEditor.getValues();
				if(!deepEquals(newLabels, this.currentItem.labelMap)) {
					this.currentItem.labelMap = newLabels;
					this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
				}
			}
		}));
	}

	function buildTable() {
		// caption
		this.setHeader(mainHeaderText);

		// general
		this.addElement(idText, this.idTextBox.domNode);
		this.addElement(dirText, this.dirTextBox.domNode);
		this.addElement(i18nText, this.i18nEditor.domNode);

		// update i18n texts
		this.updateLang();
	}

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		// general
		this.setValue(this.idTextBox, item.id);
		this.setValue(this.dirTextBox, item.dir);
		this.i18nEditor.update(item.i18nKey, item.labelMap);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		// general
		this.idTextBox.set("value", null);
		this.dirTextBox.set("value", null);
		this.i18nEditor.reset();
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;
		// general
		this.idTextBox.set("disabled", this.disabled);
		this.dirTextBox.set("disabled", this.disabled);
		this.i18nEditor.setDisabled(value);
	}

	function updateLang() {
		// update labels
		wcms.gui.ContentEditor.prototype.updateLang.call(this);
		// update id required
		I18nManager.getInstance().updateI18nValidationTextBox(this.idTextBox);
		this.i18nEditor.updateLang();
	}

	// inheritance
	wcms.navigation.MenuItemEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.MenuItemEditor.prototype.create = create;
	wcms.navigation.MenuItemEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.MenuItemEditor.prototype.reset = reset;
	wcms.navigation.MenuItemEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.MenuItemEditor.prototype.updateLang = updateLang;
})();
