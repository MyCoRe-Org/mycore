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

wcms.navigation.RootItemEditor = function() {
	this.constructor();

	// links
	this.hrefTextBox = null;
	this.hrefStartingPageEditor = null;
	
	this.dirTextBox = null;
	// titles & layout
	this.mainTitleTextBox = null;
	this.historyTitleTextBox = null;
	this.templateSelect = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.rootItemEditor.mainHeader";
	var titleAndLayoutCaption = "component.wcms.navigation.rootItemEditor.titleAndLayoutCaption";

	var hrefText = "component.wcms.navigation.rootItemEditor.href";
	var hrefStartingPageText = "component.wcms.navigation.rootItemEditor.hrefStartingPage";
	var dirText = "component.wcms.navigation.rootItemEditor.dir";
	var mainTitleText = "component.wcms.navigation.rootItemEditor.mainTitle";
	var historyTitleText = "component.wcms.navigation.rootItemEditor.historyTitle";
	var templateText = "component.wcms.navigation.itemEditor.template";
	var templateNoneText = "component.wcms.navigation.itemEditor.template.none";

	function create(/*wcms.navigation.NavigationContent*/ content) {
		// create dijit components
		// links
		this.hrefTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.hrefStartingPageEditor = new wcms.navigation.TypeEditor();
		this.dirTextBox = new dijit.form.TextBox({intermediateChanges: true});
		// titles & layout
		this.mainTitleTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.historyTitleTextBox = new dijit.form.TextBox({intermediateChanges: true});
		this.templateSelect = new dijit.form.Select({
			maxHeight: "300",
		});

		// call create methods
		this.hrefStartingPageEditor.create(content);

		// layout
		getTemplateList(dojo.hitch(this, function(data) {
			this.templateSelect.addOption({i18n: templateNoneText});
			this.templateSelect.addOption({});
			for(var i in data) {
				this.templateSelect.addOption({value: data[i], label: data[i]});
			}
			if(this.currentItem != null && this.currentItem.template != "none") {
				this.templateSelect.set("value", this.currentItem.template);
				var value = this.templateSelect.get("value");
				if(this.currentItem.template != value) {
					console.log("Unable to set template to " + this.currentItem.template +
								". This template does not exist!");
				}
			}
			I18nManager.getInstance().updateI18nSelect(this.templateSelect);
		}));

		var buildTableFunc = dojo.hitch(this, buildTable);
		buildTableFunc();

		// -href
		dojo.connect(this.hrefTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.href, value)) {
				this.currentItem.href = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -href starting page
		this.hrefStartingPageEditor.eventHandler.attach(dojo.hitch(this, function(/*TypeEditor*/ source, /*Json*/ args) {
			if(this.currentItem == null)
				return;
			var dirty = false;
			var forceNoMerge = false;
			if(args.type == "contentChanged") {
				this.currentItem.content = args.webpageContent;
				dirty = true;
				forceNoMerge = true;
			} else if(args.type == "hrefChanged" && this.currentItem.hrefStartingPage != args.value &&
					  !(this.currentItem.hrefStartingPage == undefined && args.value == "")) {
				this.currentItem.hrefStartingPage = args.value;
				dirty = true;
			} else if(args.type == "contentMoved") {
				this.currentItem.hrefStartingPage = args.to;
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
		// -directory
		dojo.connect(this.dirTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.dir, value)) {
				this.currentItem.dir = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -main title
		dojo.connect(this.mainTitleTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.mainTitle, value)) {
				this.currentItem.mainTitle = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});
		// -history title
		dojo.connect(this.historyTitleTextBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.historyTitle, value)) {
				this.currentItem.historyTitle = value;
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
	}

	function buildTable() {
		// caption
		this.setHeader(mainHeaderText);

		// links
		this.addElement(hrefText, this.hrefTextBox.domNode);
		this.addElement(hrefStartingPageText, this.hrefStartingPageEditor.domNode);
		this.addElement(dirText, this.dirTextBox.domNode);
		// titles and layouts
		this.addCaption(titleAndLayoutCaption);
		this.addElement(mainTitleText, this.mainTitleTextBox.domNode);
		this.addElement(historyTitleText, this.historyTitleTextBox.domNode);
		this.addElement(templateText, this.templateSelect.domNode);
		// update i18n texts
		this.updateLang();
	}

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		// general
		this.setValue(this.hrefTextBox, item.href);
		this.hrefStartingPageEditor.update(item);
		this.setValue(this.dirTextBox, item.dir);
		// titles & layout
		this.setValue(this.mainTitleTextBox, item.mainTitle);
		this.setValue(this.historyTitleTextBox, item.historyTitle);
		this.setValue(this.templateSelect, item.template);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		// links
		this.hrefTextBox.set("value", null);
		this.hrefStartingPageEditor.reset();
		this.dirTextBox.set("value", null);
		// titles and layouts
		this.mainTitleText.set("value", null);
		this.historyTitleTextBox.set("value", null);
		this.templateSelect.set("value", null);
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;
		// links
		this.hrefTextBox.set("disabled", this.disabled);
		this.hrefStartingPageEditor.setDisabled(this.disabled);
		this.dirTextBox.set("disabled", this.disabled);
		// titles and layouts
		this.mainTitleTextBox.set("disabled", this.disabled);
		this.historyTitleTextBox.set("disabled", this.disabled);
		this.templateSelect.set("disabled", this.disabled);
	}

	function updateLang() {
		// update labels
		wcms.gui.ContentEditor.prototype.updateLang.call(this);
		// editors
		this.hrefStartingPageEditor.updateLang();
		// update drop down labels
		I18nManager.getInstance().updateI18nSelect(this.templateSelect);
	}

	// inheritance
	wcms.navigation.RootItemEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.RootItemEditor.prototype.create = create;
	wcms.navigation.RootItemEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.RootItemEditor.prototype.reset = reset;
	wcms.navigation.RootItemEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.RootItemEditor.prototype.updateLang = updateLang;
})();
