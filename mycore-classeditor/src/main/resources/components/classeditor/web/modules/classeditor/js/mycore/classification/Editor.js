define([
	"dojo/_base/declare",
	"dijit/_WidgetBase",
	"dijit/_TemplatedMixin",
	"dijit/_WidgetsInTemplateMixin",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/Editor.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"mycore/common/I18nManager",
	"dijit/Toolbar",
	"dijit/layout/ContentPane",
	"dijit/layout/BorderContainer",
	"dijit/form/Button",
	"dijit/Tooltip",
	"mycore/classification/Store",
	"mycore/classification/CategoryEditorPane",
	"mycore/classification/TreePane",
	"mycore/common/I18nStore",
	"mycore/classification/SettingsDialog"
], function(declare, _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, _SettingsMixin, template, on, lang, i18n) {

	/**
	 * Create a new instance of the classification editor.
	 * 
	 * @param settings json object to configure the classification editor.
	 * The following parameters are required:
	 *   webAppBaseURL: base url of web application (e.g. http://localhost:8291/)
	 *   resourceURL: url of resource (e.g. http://localhost:8291/rsc/classifications/)
	 * The following parameters are optional:
	 *   showId: are classification id and category id are editable (true | false)
	 *   supportedLanguages: which languages are available for selection (json array ["de", "en", "pl"])
	 *   language: the current language (e.g. "de")
	 *   editable: if the user can edit and dnd
	 */
return declare("mycore.classification.Editor", [_WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "classeditor",

	store: null,

	settingsDialog: null,

	constructor: function(/*Object*/ args) {
		declare.safeMixin(this, args);
	},

	create: function(args) {
		// i18n
		var i18nStore = new mycore.common.I18nStore({
			url: this.settings.webAppBaseURL + "servlets/MCRLocaleServlet/"
		});
		i18n.setLanguage(this.settings.language);
		i18n.init(i18nStore);
		i18n.fetch("component.classeditor");
		// settings dialog
		this.settingsDialog = new mycore.classification.SettingsDialog();
		// create loader
		this.store = new mycore.classification.Store();
		// call inherited (includes distributing settings to other classes)
		this.inherited(arguments);
		// toolbar tooltips
		i18n.resolveTooltip(this.saveButton);
		i18n.resolveTooltip(this.refreshButton);
		i18n.resolveTooltip(this.settingsButton);
		// toolbar events
		on(this.saveButton, "click", lang.hitch(this, this.onSaveClicked));
		on(this.refreshButton, "click", lang.hitch(this, this.onRefreshClicked));
		on(this.settingsButton, "click", lang.hitch(this, this.onSettingsClicked));
		// tree events
		on(this.treePane.tree, "itemSelected", lang.hitch(this, this.onTreeItemSelected));
		on(this.treePane.tree, "itemAdded", lang.hitch(this, this.onTreeItemAddedOrMoved));
		on(this.treePane.tree, "itemMoved", lang.hitch(this, this.onTreeItemAddedOrMoved));
		on(this.treePane.tree, "itemsRemoved", lang.hitch(this, this.onTreeItemsRemoved));
		// category editor pane events
		on(this.categoryEditorPane, "labelChanged", lang.hitch(this, this.onCategoryEditorLabelChanged));
		on(this.categoryEditorPane, "urlChanged", lang.hitch(this, this.onCategoryEditorURLChanged));
		on(this.categoryEditorPane, "idChanged", lang.hitch(this, this.onCategoryEditorIdChanged));
		// settings dialog event
		on(this.settingsDialog, "hide", lang.hitch(this, this.onSettingsDialogClose));
		on(this.settingsDialog, "saveBeforeImport", lang.hitch(this, this.onSaveBeforeImport));
		// store events
		on(this.store, "saved", lang.hitch(this, this.onStoreSaved));
		on(this.store, "saveError", lang.hitch(this, this.onStoreSaveError));
	},

	startup: function() {
		this.inherited(arguments);
		this.borderContainer.resize();
	},

	onTreeItemSelected: function(args) {
		if(args.item == null) {
			this.categoryEditorPane.set("disabled", true);
		} else {
			this.categoryEditorPane.set("disabled", !(this.settings.editable || this.settings.editable == undefined));
			this.categoryEditorPane.update(args.item);
		}
	},

	onTreeItemAddedOrMoved: function(args) {
		this.store.updateSaveArray("update", args.item, args.parent);
		this.updateToolbar(true);
	},

	onTreeItemsRemoved: function(args) {
		for(var i = 0; i < args.items.length; i++) {
			this.store.updateSaveArray("delete", args.items[i]);
		}
		this.categoryEditorPane.set("disabled", true);
		this.updateToolbar(true);
	},

	onCategoryEditorLabelChanged: function(args) {
		this._updateItem(args.item, "labels", args.value);
	},

	onCategoryEditorURLChanged: function(args) {
		this._updateItem(args.item, "uri", args.value);
	},

	onCategoryEditorIdChanged: function(args) {
		this._updateItem(args.item, "id", args.value);
	},

	_updateItem: function(item, type, value) {
		this.store.setValue(item, type, value);
		this.store.setValue(item, "modified", true);
		this.store.updateSaveArray("update", item);
		this.updateToolbar(true);
	},

	onSaveClicked: function() {
		this.store.save(this.treePane.tree);
	},

	onRefreshClicked: function() {
		if(!confirm(i18n.getFromCache("component.classeditor.refresh.warning"))) {
			return;
		}
		this.reloadClassification();
	},

	onSettingsClicked: function() {
		this.openSettingsDialog();
	},

	onSettingsDialogClose: function() {
		var languages = this.settingsDialog.languageEditor.get("value");
		i18n.setLanguages(languages);
		this.categoryEditorPane.updateLanguages();
		// show Id's?
		var showIds = this.settingsDialog.showIdCheckBox.get("value");
		if(showIds) {
			this.treePane.showId();
			this.categoryEditorPane.showId();
		} else {
			this.treePane.hideId();
			this.categoryEditorPane.hideId();
		}
		// if a new classification was successfully imported
		if(this.settingsDialog.classificationImported) {
			this.settingsDialog.classificationImported = false;
			this.reloadClassification();
		}
	},

	onSaveBeforeImport: function() {
		this.store.save(this.treePane.tree);
	},

	onStoreSaved: function() {
		this.updateToolbar(false);
		this.categoryEditorPane.update();
		alert(i18n.getFromCache("component.classeditor.save.successfull"));
	},

	onStoreSaveError: function(evt) {
	    if(evt.error.status === 401) {
	        alert(i18n.getFromCache("component.classeditor.error.nopermissions"));
	    } else{
	        alert(i18n.getFromCache("component.classeditor.save.generalerror") + " - " + evt.error);
	    }
		console.log("error while saving");
		console.log(evt.error);
	},

	/**
	 * Loads a new classification - if this string is empty, all
	 * classifications are loaded.
	 */
	loadClassification: function(/*String*/ classificationId, /*String*/ categoryId) {
		this.store.classificationId = classificationId;
		this.store.categoryId = categoryId;
		this.reloadClassification();
	},

	/**
	 * Reloads the tree.
	 */
	reloadClassification: function() {
		this.store.load(
			lang.hitch(this, function(store) {
				this.treePane.tree.createTree(store);
				this.treePane.updateToolbar();
			}),
			lang.hitch(this, function(error) {
				alert(error);
				this.treePane.updateToolbar();
			})
		);
		this.categoryEditorPane.set("disabled", true);
		this.updateToolbar(false);
	},

	updateToolbar: function(/*boolean*/ dirty) {
		if(dirty) {
			this.saveButton.set("disabled", false);
			this.saveButton.set("iconClass", "icon16 saveIcon");
		} else {
			this.saveButton.set("disabled", true);
			this.saveButton.set("iconClass", "icon16 saveDisabledIcon");
		}
	},

	openSettingsDialog: function() {
		this.settingsDialog.show(this.store.isDirty());
	}

});
});
