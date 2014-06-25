define([
	"dojo/_base/declare", // declare
	"dijit/layout/ContentPane",
	"dijit/_Templated",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/TreePane.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"mycore/util/DOJOUtil",
	"mycore/classification/Util",
	"dijit/Toolbar",
	"dijit/ToolbarSeparator",
	"dijit/layout/ContentPane",
	"dijit/layout/BorderContainer",
	"dijit/form/Button",
	"mycore/classification/LazyLoadingTree",
	"mycore/classification/ExportDialog",
	"mycore/classification/LinkDialog"
], function(declare, ContentPane, _Templated, _SettingsMixin, template, on, lang, domConstruct, dojoUtil, classUtil) {

return declare("mycore.classification.TreePane", [ContentPane, _Templated, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "treePane",

	exportDialog: null,

	linkDialog: null,

	disabled: false,

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    create: function(args) {
    	this.inherited(arguments);
    	// dialogs
    	this.exportDialog = new mycore.classification.ExportDialog();
    	this.linkDialog = new mycore.classification.LinkDialog();
    	// toolbar events
    	on(this.addTreeItemButton, "click", lang.hitch(this, this.add));
    	on(this.removeTreeItemButton, "click", lang.hitch(this, this.remove));
    	on(this.exportClassificationButton, "click", lang.hitch(this, this.exportClassification));
    	on(this.linkDialogButton, "click", lang.hitch(this, this.openLinkDialog));
    	// tree event
    	on(this.tree, "itemSelected", lang.hitch(this, this.updateToolbar));
    },

	updateToolbar: function() {
		var disabled = this.get("disabled") == true;
		// remove button
		var selectedItems = this.tree.getSelectedItems();
		var removeVisable = selectedItems != null && selectedItems.length > 0 && !disabled;
		if(selectedItems) {
			for(var i = 0; i < selectedItems.length; i++) {
				if(selectedItems[i].haslink || selectedItems[i].fakeRoot) {
					removeVisable = false;
					break;
				}
			}
		}
		this.removeTreeItemButton.set("disabled", !removeVisable);
		this.removeTreeItemButton.set("iconClass", "icon16 " + (removeVisable ? "removeIcon" : "removeDisabledIcon"));

		// add button
		this.addTreeItemButton.set("disabled", disabled);
		this.addTreeItemButton.set("iconClass", "icon16 " + (disabled ? "addDisabledIcon" : "addIcon"));

		// export button
		this.exportClassificationButton.set("disabled", !(selectedItems != null  && selectedItems.length > 0 && classUtil.isClassification(selectedItems[0])));

		// link dialog button
		this.linkDialogButton.set("disabled", !(selectedItems != null  && selectedItems.length > 0 && !selectedItems[0].fakeRoot));
	},

	add: function() {
		this.addTreeItemButton.set("disabled", true);
		this.addTreeItemButton.set("iconClass", "icon16 addDisabledIcon");
		this.tree.addToSelected();
		this.addTreeItemButton.set("disabled", false);
		this.addTreeItemButton.set("iconClass", "icon16 addIcon");	
	},

	remove: function() {
		this.tree.removeSelected();
		this.updateToolbar();
	},

	exportClassification: function() {
		var selectedItems = this.tree.getSelectedItems();
		this.exportDialog.show(classUtil.getClassificationId(selectedItems[0]));
	},
	
	openLinkDialog: function() {
		var selectedItems = this.tree.getSelectedItems();
		this.linkDialog.show(selectedItems[0]);
	},

	showID: function() {
		this.tree.showID();
	},

	hideID: function() {
		this.tree.hideID();
	},

	_setDisabledAttr: function(/* boolean */ disabled) {
		this.disabled = disabled;
		this.updateToolbar();
		this.tree.set("disabled", disabled);
	}

});
});
