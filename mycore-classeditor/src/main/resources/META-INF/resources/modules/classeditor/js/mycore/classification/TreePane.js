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
	"dijit/form/TextBox",
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
	
	filterTextBox: null,
	
    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    create: function(args) {
    	// dialogs
    	this.exportDialog = new mycore.classification.ExportDialog();
    	this.linkDialog = new mycore.classification.LinkDialog();
    	// create
    	this.inherited(arguments);
    },

    onSettingsReady: function() {
    	// filter
    	if(this.settings.solrEnabled) {
    		var filterBoxContainer = new ContentPane({region: 'top', style: "padding-bottom: 5px;"});
    		this.filterTextBox = new dijit.form.TextBox({intermediateChanges: true, disabled: true, placeHolder: "Filter", style: "width: 99%;"});
    		filterBoxContainer.set("content", this.filterTextBox.domNode);
    		this.mainContainer.addChild(filterBoxContainer);
    	}

    	/*	<!-- Filter -->
    	<div data-dojo-type="dijit.layout.ContentPane" data-dojo-props="region: 'top'" style="padding-bottom: 5px;">
    		<input data-dojo-type="dijit.form.TextBox" data-dojo-props="intermediateChanges: true, disabled: true" data-dojo-attach-point="filterTextBox" placeHolder="Filter" style="width: 99%;"/>
    	</div>*/
    		
    	// toolbar events
    	on(this.addTreeItemButton, "click", lang.hitch(this, this.add));
    	on(this.removeTreeItemButton, "click", lang.hitch(this, this.remove));
    	on(this.exportClassificationButton, "click", lang.hitch(this, this.exportClassification));
    	on(this.linkDialogButton, "click", lang.hitch(this, this.openLinkDialog));
    	if(this.filterTextBox != null) {
    		on(this.filterTextBox, "change", lang.hitch(this, this.filterTree));
    	}
    	// tree event
    	on(this.tree, "itemSelected", lang.hitch(this, this.updateToolbar));
    },

	updateToolbar: function() {
		var disabled = this.get("disabled") == true;

		// filter
		if(this.filterTextBox != null) {
			this.filterTextBox.set("disabled", disabled);
		}

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

	filterTree: function(filterValue) {
		this.tree.filter(filterValue);
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
