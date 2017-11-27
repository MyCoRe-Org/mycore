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

wcms.navigation.ItemEditor = function() {
	this.constructor();

	// general
	this.i18nEditor = null;
	this.typeEditor = null;
	// navigation
	this.targetSelect = null;
	this.replaceMenuCheckBox = null;
	this.constrainPopUpCheckBox = null;
	// layout
	this.templateSelect = null;
	this.styleInput = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.itemEditor.mainHeader";
	var nameText = "component.wcms.navigation.itemEditor.name";
	var typeText = "component.wcms.navigation.itemEditor.type";
	var navHeaderText = "component.wcms.navigation.itemEditor.navHeader";
	var targetText = "component.wcms.navigation.itemEditor.target";
	var targetSelfText = "component.wcms.navigation.itemEditor.target.self";
	var targetBlankText = "component.wcms.navigation.itemEditor.target.blank";
	var replaceMenuText = "component.wcms.navigation.itemEditor.replaceMenu";
	var constrainPopUpText = "component.wcms.navigation.itemEditor.constrainPopUp";
	var layoutHeaderText = "component.wcms.navigation.itemEditor.layoutHeader";
	var templateText = "component.wcms.navigation.itemEditor.template";
	var templateNoneText = "component.wcms.navigation.itemEditor.template.none";
	var styleText = "component.wcms.navigation.itemEditor.style";

	function create(/*wcms.navigation.NavigationContent*/ content) {
		// create dijit components
		this.i18nEditor = new wcms.navigation.I18nEditor();
		this.i18nEditor.create();
		this.typeEditor = new wcms.navigation.TypeEditor();
		// navigation
		this.targetSelect = new dijit.form.Select();
//		dojo.addClass(this.targetSelect.focusNode, "smallComponent");
		this.replaceMenuCheckBox = new dijit.form.CheckBox();
		this.constrainPopUpCheckBox = new dijit.form.CheckBox();
		// layout
		this.templateSelect = new dijit.form.Select({
			maxHeight: 300,
		});
//		dojo.addClass(this.templateSelect.focusNode, "mediumComponent");

		this.styleInput = new dijit.form.TextBox();

		// call create methods
		this.typeEditor.create(content);

		// navigation
		this.targetSelect.addOption({value: "_self", i18n: targetSelfText});
		this.targetSelect.addOption({value: "_blank", i18n: targetBlankText});

		// layout
		getTemplateList(dojo.hitch(this, function(data) {
			this.templateSelect.addOption({i18n: templateNoneText});
			this.templateSelect.addOption({});
			for(var i in data)
				this.templateSelect.addOption({value: data[i], label: data[i]});

			I18nManager.getInstance().updateI18nSelect(this.templateSelect);
		}));

		var buildTableFunc = dojo.hitch(this, buildTable);
		buildTableFunc();

		// events
		// -i18n
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
		// -type
		this.typeEditor.eventHandler.attach(dojo.hitch(this, function(/*TypeEditor*/ source, /*Json*/ args) {
			if(this.currentItem == null)
				return;
			var dirty = false;
			var forceNoMerge = false;
			if(args.type == "contentChanged") {
				this.currentItem.content = args.webpageContent;
				dirty = true;
				forceNoMerge = true;
			} else if(args.type == "typeChanged" && this.currentItem.type != args.value &&
					  !(this.currentItem.type == undefined && args.value == source.Type.intern)) {
				this.currentItem.type = args.value;
				dirty = true;
			} else if(args.type == "hrefChanged" && this.currentItem.href != args.value &&
					  !(this.currentItem.href == undefined && args.value == "")) {
				this.currentItem.href = args.value;
				dirty = true;
			} else if(args.type == "contentMoved") {
				this.currentItem.href = args.to;
				dirty = true;
			}
			if(dirty) {
				this.eventHandler.notify({
					"type" : "itemUpdated",
					"item": this.currentItem,
					"forceNoMerge" : forceNoMerge
				});
			}
		}));
		// -target
		dojo.connect(this.targetSelect, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(this.currentItem.target == undefined && value == "_self")
				return;
			if(!equal(this.currentItem.target, value)) {
				this.currentItem.target = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -replace menu
		dojo.connect(this.replaceMenuCheckBox, "onChange", this, function(/*boolean*/ value) {
			if(this.currentItem == null)
				return;
			if(this.currentItem.replaceMenu == undefined && value == false)
				return;
			if(!equal(this.currentItem.replaceMenu, value)) {
				this.currentItem.replaceMenu = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -constrain popup
		dojo.connect(this.constrainPopUpCheckBox, "onChange", this, function(/*boolean*/ value) {
			if(this.currentItem == null)
				return;
			if(this.currentItem.constrainPopUp == undefined && value == false)
				return;
			if(!equal(this.currentItem.constrainPopUp, value)) {
				this.currentItem.constrainPopUp = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -template select
		dojo.connect(this.templateSelect, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.template, value)) {
				this.currentItem.template = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -style input
		dojo.connect(this.styleInput, "onChange", this, function(value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.style, value)) {
				this.currentItem.style = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
	}

	function buildTable() {
		// caption
		this.setHeader(mainHeaderText);

		// general
		this.addElement(nameText, this.i18nEditor.domNode);
		this.addElement(typeText, this.typeEditor.domNode);
		// navigation
		this.addCaption(navHeaderText);
		this.addElement(targetText, this.targetSelect.domNode);
		this.addElement(replaceMenuText, this.replaceMenuCheckBox.domNode);
		this.addElement(constrainPopUpText, this.constrainPopUpCheckBox.domNode);
		// layout
		this.addCaption(layoutHeaderText);
		this.addElement(templateText, this.templateSelect.domNode);
		this.addElement(styleText, this.styleInput.domNode);
		
		// update i18n texts
		this.updateLang();
	}

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		// label
		this.i18nEditor.update(item.i18nKey, item.labelMap);
		// type
		this.typeEditor.update(item);
		// navigation
		this.setValue(this.targetSelect, item.target);
		if(item.replaceMenu)
			this.replaceMenuCheckBox.set("checked", item.replaceMenu);
		else
			this.replaceMenuCheckBox.set("checked", false);
		if(item.constrainPopUp)
			this.constrainPopUpCheckBox.set("checked", item.constrainPopUp);
		else
			this.constrainPopUpCheckBox.set("checked", false);
		// layout
		this.setValue(this.templateSelect, item.template);
		this.setValue(this.styleInput, item.style);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		this.i18nEditor.reset();
		this.typeEditor.reset();
		this.targetSelect.set("value", null);
		this.replaceMenuCheckBox.set("checked", false);
		this.constrainPopUpCheckBox.set("checked", false);
		this.templateSelect.set("value", null);
		this.styleInput.set("value", null);
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;

		this.i18nEditor.setDisabled(this.disabled);
		this.typeEditor.setDisabled(this.disabled);
		this.targetSelect.set("disabled", this.disabled);
		this.replaceMenuCheckBox.set("disabled", this.disabled);
		this.constrainPopUpCheckBox.set("disabled", this.disabled);
		this.templateSelect.set("disabled", this.disabled);
		this.styleInput.set("disabled", this.disabled);
	}

	function updateLang() {
		// update labels
		wcms.gui.ContentEditor.prototype.updateLang.call(this);
		// update type editor & i18n editor
		this.i18nEditor.updateLang();
		this.typeEditor.updateLang();
		// update drop down labels
		I18nManager.getInstance().updateI18nSelect(this.targetSelect);
		I18nManager.getInstance().updateI18nSelect(this.templateSelect);
	}

	// inheritance
	wcms.navigation.ItemEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.ItemEditor.prototype.create = create;
	wcms.navigation.ItemEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.ItemEditor.prototype.reset = reset;
	wcms.navigation.ItemEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.ItemEditor.prototype.updateLang = updateLang;
})();
