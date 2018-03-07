define([
	"dojo/_base/declare",
	"dijit/_WidgetBase",
	"dijit/_TemplatedMixin",
	"dijit/_WidgetsInTemplateMixin",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/Editor.html",
	"dojo/on", // on
	"dojo/dom", // byId
	"dojo/query",
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct",
    "dojo/dom-style", // style
	"dojo/_base/json",
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
], function(declare, _WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, _SettingsMixin, template, on, dom, query, lang, domConstruct, domStyle, json, i18n) {

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

	disabled: false,

	constructor: function(/*Object*/ args) {
		declare.safeMixin(this, args);
	},

	create: function(args) {
		// i18n
		i18n.init(this.settings.webAppBaseURL + "rsc/locale");
		i18n.fetch("component.classeditor");
		// settings dialog
		this.settingsDialog = new mycore.classification.SettingsDialog();
		// create loader
		this.store = new mycore.classification.Store();
		// call inherited (includes distributing settings to other classes)
		this.inherited(arguments);
		// store events
		this.store.restStore.onRootLoadError = lang.hitch(this, this.onRootLoadError);
		this.store.restStore.onNodeLoadError = lang.hitch(this, this.onNodeLoadError);
		// toolbar tooltips
		i18n.resolveTooltip(this.saveButton);
		i18n.resolveTooltip(this.refreshButton);
		i18n.resolveTooltip(this.settingsButton);
		// toolbar events
		on(this.saveButton, "click", lang.hitch(this, this.onSaveClicked));
		on(this.refreshButton, "click", lang.hitch(this, this.onRefreshClicked));
		on(this.settingsButton, "click", lang.hitch(this, this.onSettingsClicked));
		on(this.fullscreenButton, "click", lang.hitch(this, this.onFullscreenClicked));
		// tree events
		on(this.treePane.tree, "itemSelected", lang.hitch(this, this.onTreeItemSelected));
		on(this.treePane.tree, "itemAdded", lang.hitch(this, this.onTreeItemAddedOrMoved));
		on(this.treePane.tree, "itemMoved", lang.hitch(this, this.onTreeItemAddedOrMoved));
		on(this.treePane.tree, "itemRemoved", lang.hitch(this, this.onTreeItemRemoved));
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
		on(this.store, "saveEvent", lang.hitch(this, this.onStoreSaveEvent));

		// on body change event for fullscreen
		MutationObserver = window.MutationObserver || window.WebKitMutationObserver;
		var observer = new MutationObserver(lang.hitch(this, this.onBodyChange));
		observer.observe(document.body, {
			childList: true
		});
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
		this.updateToolbar();
	},

	onTreeItemRemoved: function(args) {
		this.store.updateSaveArray("delete", args.item);
		this.categoryEditorPane.set("disabled", true);
		this.updateToolbar();
	},

	onCategoryEditorLabelChanged: function(args) {
		this._updateItem(args.item);
	},

	onCategoryEditorURLChanged: function(args) {
		this._updateItem(args.item);
	},

	onCategoryEditorIdChanged: function(args) {
		var item = args.item;
		if(!this.treePane.tree.updateIdOfNode(item, args.oldID)) {
			// id is invalid -> reset item id
			item.id.rootid = args.oldID.rootid;
			item.id.categid = args.oldID.categid;
			// TODO: we should mark the category text field as invalid
		}
		this._updateItem(item);
	},

	_updateItem: function(item) {
		item.modified = true;
		this.treePane.tree.updateLabel(item);
		this.store.updateSaveArray("update", item);
		this.updateToolbar();
	},

	onSaveClicked: function() {
		this.store.save();
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

	onFullscreenClicked: function() {
		this.toggleFullscreen();
		this.fullscreenButton.set("hovering", false);
		// because fullscreen is not applied instantly
		setTimeout(lang.hitch(this, function() {
			this.updateToolbar();
		}), 300);
	},

	onSettingsDialogClose: function() {
		var languages = this.settingsDialog.languageEditor.get("value");
		i18n.setLanguages(languages);
		this.categoryEditorPane.updateLanguages();
		// show Id's?
		var showIds = this.settingsDialog.showIdCheckBox.get("value");
		if(showIds) {
			this.treePane.showID();
			this.categoryEditorPane.showID();
		} else {
			this.treePane.hideID();
			this.categoryEditorPane.hideID();
		}
		// if a new classification was successfully imported
		if(this.settingsDialog.classificationImported) {
			this.settingsDialog.classificationImported = false;
			this.reloadClassification();
		}
	},

	onSaveBeforeImport: function() {
		this.store.save();
	},

	onStoreSaved: function() {
		this.updateToolbar();
		this.categoryEditorPane.update();
		this.treePane.tree.updateLabels();
		alert(i18n.getFromCache("component.classeditor.save.successful"));
	},

	onStoreSaveError: function(error) {
		var xhr = error.response.xhr;
		var status = xhr.status;
		var responseText = xhr.responseText;
		if(status == 401) {
	        alert(i18n.getFromCache("component.classeditor.save.nopermission"));
	    } else if(status == 409) {
	    	var responseAsJSON = json.fromJson(responseText);
	    	if(responseAsJSON.type == "duplicateID") {
	    		var msg = i18n.getFromCache("component.classeditor.save.duplicateID");
	    		alert(msg + ": ClassificationID: " + responseAsJSON.rootid + "; CategoryID: " + responseAsJSON.categid);
	    	}
	    } else {
	    	alert(i18n.getFromCache("component.classeditor.save.generalerror") + ": " + xhr.statusText);
	    }
		console.log(error);
	},

	onStoreSaveEvent: function(evt) {
		// nothing todo
	},

	onRootLoadError: function(error) {
		if(error.xhr.status == 401) {
			alert(i18n.getFromCache("component.classeditor.error.noReadPermission"));
		} else {
			alert(error.xhr.statusText);
			console.log(error);
		}
		domConstruct.empty(this.treePane.tree.domNode);
		this.set("disabled", true);
	},

	onNodeLoadError: function(error) {
		alert(i18n.getFromCache("component.classeditor.error.loadError") + " " + error.xhr.statusText);
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
		this.store.reset();
		this.treePane.tree.createTree(this.store.restStore);
		this.treePane.updateToolbar();
		this.updateToolbar();
		this.categoryEditorPane.set("disabled", true);
	},

	updateToolbar: function() {
		var disabled = this.get("disabled") == true;
		var dirty = this.store.isDirty();
		var disabledSave = disabled || !dirty;
		this.saveButton.set("disabled", disabledSave);
		this.saveButton.set("iconClass", "icon16 " + (disabledSave ? "saveDisabledIcon" : "saveIcon"));
		this.refreshButton.set("disabled", disabled);
		this.refreshButton.set("iconClass", "icon16 " + (disabled ? "refreshDisabledIcon" : "refreshIcon"));
		this.settingsButton.set("disabled", disabled);
		this.settingsButton.set("iconClass", "icon16 " + (disabled ? "settingsDisabledIcon" : "settingsIcon"));
		this.fullscreenButton.set("disabled", disabled);
		if(this.isFullscreen()) {
			this.fullscreenButton.set("iconClass", "icon16 " + (disabled ? "minimizeDisabledIcon" : "minimizeIcon"));
		} else {
			this.fullscreenButton.set("iconClass", "icon16 " + (disabled ? "fullscreenDisabledIcon" : "fullscreenIcon"));
		}
	},

	openSettingsDialog: function() {
		this.settingsDialog.show(this.store.isDirty());
	},

	onBodyChange: function(mutation, observer) {
		if(!this.isFullscreen()) {
			return;
		}
		var selector = ".dijitDialog, .dijitPopup, .dijitTooltip, .dijitDialogUnderlayWrapper";
		for(var m = 0; m < mutation.length; m++) {
			var nodes = mutation[m].addedNodes;
			for(var n = 0; n < nodes.length; n++) {
				domConstruct.place(nodes[n], this.domNode);
			}
		}
	},

	toggleFullscreen: function() {
		var fullscreen = this.isFullscreen();
		// toggle fullscreen
		if (this.domNode.requestFullScreen) {
			if (!fullscreen) {
				this.domNode.requestFullscreen();
			} else {
				document.exitFullScreen();
			}
		} else if (this.domNode.mozRequestFullScreen) {
			if (!fullscreen) {
				this.domNode.mozRequestFullScreen();
			} else {
				document.mozCancelFullScreen();
			}
		} else if (this.domNode.webkitRequestFullScreen) {
			if (!fullscreen) {
				this.domNode.webkitRequestFullScreen();
			} else {
				document.webkitCancelFullScreen();
			}
		}
		// move outer div's
		var body = query("body")[0];
		var classEditorNode = this.domNode;
		var selector = ".dijitDialog, .dijitPopup, .dijitTooltip, .dijitDialogUnderlayWrapper";
		if(fullscreen) {
            domStyle.set(this.domNode, "height", "700px");
			query(selector, this.domNode).forEach(function(node) {
				domConstruct.place(node, body);
			});
		} else {
            domStyle.set(this.domNode, "height", "100%");
			query(selector, body).forEach(function(node) {
				domConstruct.place(node, classEditorNode);
			});
		}
	},

	isFullscreen: function() {
		return document.fullScreen || document.mozFullScreen || document.webkitIsFullScreen;
	},

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.disabled = disabled;
		this.updateToolbar();
		this.treePane.set("disabled", disabled);
		this.categoryEditorPane.set("disabled", disabled);
	}

});
});
