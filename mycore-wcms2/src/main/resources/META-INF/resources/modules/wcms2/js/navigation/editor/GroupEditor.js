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

wcms.navigation.GroupEditor = function() {
	this.constructor();

	this.idTextBox = null;
	this.i18nEditor = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.groupEditor.mainHeader";
	var idText = "component.wcms.navigation.groupEditor.id";
	var i18nText = "component.wcms.navigation.groupEditor.i18n";
	var idRequiredText = "component.wcms.navigation.groupEditor.idRequired";

	function create() {
		this.idTextBox = new dijit.form.ValidationTextBox({
			required: true,
			intermediateChanges: true,
			i18nMissingMessage: idRequiredText
		});
		
		this.i18nEditor = new wcms.navigation.I18nEditor();
		this.i18nEditor.create();

		this.setHeader(mainHeaderText);
		this.addElement(idText, this.idTextBox.domNode);
		this.addElement(i18nText, this.i18nEditor.domNode);
		this.updateLang();
		
		// events
		// -id
		dojo.connect(this.idTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.id, value)) {
				this.currentItem.id = value;
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

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		this.setValue(this.idTextBox, item.id);
		this.i18nEditor.update(item.i18nKey, item.labelMap);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		this.idTextBox.set("value", null);
		this.i18nEditor.reset();
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;
		this.idTextBox.set("disabled", this.disabled);
		this.i18nEditor.setDisabled(this.disabled);
	}

	function updateLang() {
		// update labels
		wcms.gui.ContentEditor.prototype.updateLang.call(this);
		// update id required
		I18nManager.getInstance().updateI18nValidationTextBox(this.idTextBox);
		this.i18nEditor.updateLang();
	}

	// inheritance
	wcms.navigation.GroupEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.GroupEditor.prototype.create = create;
	wcms.navigation.GroupEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.GroupEditor.prototype.reset = reset;
	wcms.navigation.GroupEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.GroupEditor.prototype.updateLang = updateLang;
})();
