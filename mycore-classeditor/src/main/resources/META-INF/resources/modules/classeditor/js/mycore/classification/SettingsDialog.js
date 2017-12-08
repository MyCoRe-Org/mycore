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

define([
	"dojo/_base/declare", // declare
	"dijit/Dialog",
	"dijit/_Templated",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/SettingsDialog.html",
	"dojo/_base/lang", // hitch, clone
	"dojo/on", // on
	"dojo/dom",
	"dojo/dom-attr",
	"dojo/request/xhr",
	"dojo/dom-construct",
	"dojo/io/iframe",
	"mycore/common/I18nManager",
	"dijit/form/Button",
	"dijit/form/Form",
	"dijit/form/TextBox",
	"dijit/form/CheckBox",
	"mycore/dijit/Repeater",
	"mycore/dijit/TextRow"
], function(declare, Dialog, _Templated, _SettingsMixin, template, lang, on, dom, domAttr, xhr, domConstruct, ioIframe, i18n) {

return declare("mycore.classification.SettingsDialog", [Dialog, _Templated, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "settingsDialog",

	dirty: false,

	classificationImported: false,

	languageEditor: null,
	
    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    onSettingsReady: function() {
    	// language editor
		this.languageEditor = new mycore.dijit.Repeater({
			row: {
				className: "mycore.dijit.TextRow"
			},
			minOccurs: 1
		});
		domConstruct.place(this.languageEditor.domNode, this.languageEditorDiv);
    	// header
		this.set("title", i18n.getFromCache("component.classeditor.settings.dialog"));
		this.closeButtonNode.title = i18n.getFromCache("component.classeditor.dialog.close");
		// i18n
		i18n.resolve(this.importClassLabel);
		i18n.resolve(this.languageSelectLabel);
		i18n.resolve(this.miscLabel);
		i18n.resolve(this.showIdCheckBox);
		// set defaults
	    this.showIdCheckBox.set("value", this.settings.showId ? this.settings.showId : false);
		// confirm dialog
		this.confirmImportDialog.set("title", i18n.getFromCache("component.classeditor.settings.beforeSubmitTitle"));
		this.confirmImportDialog.closeButtonNode.title = i18n.getFromCache("component.classeditor.dialog.close");
		i18n.resolve(this.confirmImportContent);
		i18n.resolve(this.saveAndImportButton);
		i18n.resolve(this.noSaveAndImportButton);
		i18n.resolve(this.cancelImportButton);
		// events
		on(this.hiddenFileDialog, "change", lang.hitch(this, this.updateClassInput));
		on(this.importOpenButton, "click", lang.hitch(this, this.onOpenFileDialog));
		on(this.importStartButton, "click", lang.hitch(this, this.onSubmitClassification));
		on(this.saveAndImportButton, "click", lang.hitch(this, this.onSaveAndImport));
		on(this.noSaveAndImportButton, "click", lang.hitch(this, this.onNoSaveAndImport));
		on(this.cancelImportButton, "click", lang.hitch(this, this.onCancelImport));
    },

    show: function(/*boolean*/ dirty) {
		this.dirty = dirty;
		this.classificationImported = false;
		this.languageEditor.set("value", i18n.getLanguages());
		this.inherited(arguments);
    },

    updateClassInput: function () {
        let value = this.hiddenFileDialog.value;
        value = value !== null ? value.replace(/^.*\\/, "") : null;
        this.importInput.set("value", value);
        this.importStartButton.set("disabled", value === "");
    },

    onOpenFileDialog: function() {
    	this.hiddenFileDialog.click();
    },

    onSubmitClassification: function() {
		if(this.dirty) {
			this.confirmImportDialog.show();		
		} else {
			this.submitClassification();
		}
    },

	submitClassification: function() {
		ioIframe.send({
			form: dom.byId("classImportForm"),
			method: "post",
			handleAs: "html",
			url: this.settings.resourceURL + "import"
		}).then(lang.hitch(this, function(data) {
			let status = data.body.children[0].value;
			if(status == "200") {
				alert(i18n.getFromCache("component.classeditor.settings.importsuccessful"));
				this.classificationImported = true;
				this.importInput.set("value", null);
				this.importStartButton.set("disabled", true);
			} else {
				alert(i18n.getFromCache("component.classeditor.settings.submiterror"));
			}
		}), function(error) {
	    	alert(i18n.getFromCache("component.classeditor.settings.unknownsubmiterror") + " - " + error);
			console.log(error);
		});
	},

	onSaveAndImport: function() {
		this.confirmImportDialog.hide();
		on.emit(this, "saveBeforeImport");
		this.submitClassification();
	},

	onNoSaveAndImport: function() {
		this.confirmImportDialog.hide();
		this.submitClassification();
	},

	onCancelImport: function() {
		this.confirmImportDialog.hide();
	}

});
});
