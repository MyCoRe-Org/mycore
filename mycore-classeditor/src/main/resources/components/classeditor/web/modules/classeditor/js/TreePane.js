/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * 
 */
classeditor.TreePane = function(settings) {
	this.settings = settings;
	this.editable = settings.editable == undefined ? true : settings.editable;
	
	// dom
	this.mainPane = new dijit.layout.ContentPane({
		gutters: false,
		splitter: true
	});
	dojo.addClass(this.mainPane.domNode, "treePane");
	this.treePane = null;
	this.toolbar = null;

	// buttons
	this.addTreeItemButton = null;
	this.removeTreeItemButton = null;
	this.exportClassificationButton = null;

	// tree
	this.tree = null;
	
	// dialog
	this.exportDialog = null;
};

( function() {

	function create() {
		// create tree
		this.tree = new classeditor.LazyLoadingTree(this.settings);
		this.tree.eventHandler.attach(dojo.hitch(this, handleTreeEvents));
		// dialog
		this.exportDialog = new classeditor.ExportDialog(this.settings);
		// create dom
		dojo.hitch(this, createDom)();
	}

	function loadClassification(/*String*/ classificationId, /*String*/ categoryId) {
		this.tree.create(classificationId, categoryId,
			dojo.hitch(this, function() {
				this.updateToolbar();
			}),
			dojo.hitch(this, function(error) {
				alert(error);
				this.updateToolbar();
			})
		);
	}

	function handleTreeEvents(/*LazyLoadingTree*/ source, /*JSON*/ args) {
		if(args.type == "treeCreated") {
			dojo.hitch(this, addTreeToDOM)();
		} else if(args.type == "itemSelected") {
			this.updateToolbar();
		}
	}

	function createDom() {
		// border container
		var treeContainer = new dijit.layout.BorderContainer({
			gutters: false,
			splitter: false
		});
		// tree
		this.treePane = new dijit.layout.ContentPane({
			region: "center",
			gutters: false,
			splitter: false
		});

		// toolbar
		this.toolbar = new dijit.Toolbar({
			className: "toolbar",
			region: "bottom",
			splitter: false
		});
		dojo.hitch(this, createToolbar)();

		// add to dom
		this.mainPane.set("content", treeContainer);
		treeContainer.addChild(this.treePane);
		treeContainer.addChild(this.toolbar);

		// add loading gif
		var loading = dojo.create("div", {className: "loading"});
		this.treePane.set("content", loading);
	}

	function addTreeToDOM() {
		// surrounding div fixes bug 10585 @see
		// http://bugs.dojotoolkit.org/ticket/10585
		// TODO: enable this for correct dnd support -> check scrollbars!!
		// EDIT 21.06.12: seems to working now without surrounding div
//		var surroundingTreeDiv = dojo.create("div");
//		surroundingTreeDiv.appendChild(this.tree.tree.domNode);
		this.treePane.set("content", this.tree.tree.domNode);
		this.tree.tree.startup();
	}

	function createToolbar() {
		var sm = SimpleI18nManager.getInstance();
		var addMenu = new dijit.Menu();

	    // toolbar buttons
		this.addTreeItemButton = new dijit.form.Button({
			showLabel: false, iconClass: "icon16 addIcon16", disabled: false,
			onClick: dojo.hitch(this, add)
		});
		var addTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.treepane.add")
		});
		addTT.addTarget(this.addTreeItemButton.domNode);

		this.removeTreeItemButton = new dijit.form.Button({
			showLabel: false, iconClass: "icon16 removeDisabledIcon16", disabled: true,
			onClick: dojo.hitch(this, remove)
		});
		var removeTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.treepane.remove")
		});
		removeTT.addTarget(this.removeTreeItemButton.domNode);

		this.exportClassificationButton = new dijit.form.Button({
			showLabel: false, iconClass: "icon16 exportClassDisabledIcon16", disabled: true,
			onClick: dojo.hitch(this, exportClassification)
		});
		var exportTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.treepane.export")
		});
		exportTT.addTarget(this.exportClassificationButton.domNode);

		// add
		this.toolbar.addChild(this.addTreeItemButton);
		this.toolbar.addChild(this.removeTreeItemButton);
		this.toolbar.addChild(new dijit.ToolbarSeparator());
		this.toolbar.addChild(this.exportClassificationButton);
	}

	function updateToolbar() {
		// remove button
		var selectedItems = this.tree.getSelectedItems();
		var removeVisable = selectedItems && selectedItems.length > 0 && this.editable;
		if(selectedItems) {
			for(var i = 0; i < selectedItems.length; i++) {
				if(selectedItems[i]._RI || (selectedItems[i].haslink && selectedItems[i].haslink[0] == true)) {
					removeVisable = false;
					break;
				}
			}
		}
		this.removeTreeItemButton.set("disabled", !removeVisable);
		if(removeVisable) {
			this.removeTreeItemButton.set("iconClass", "icon16 removeIcon16");
		} else {
			this.removeTreeItemButton.set("iconClass", "icon16 removeDisabledIcon16");
		}

		// add button
		this.addTreeItemButton.set("disabled", !this.editable);
		if(this.editable) {
			this.addTreeItemButton.set("iconClass", "icon16 addIcon16");
		} else {
			this.addTreeItemButton.set("iconClass", "icon16 addDisabledIcon16");
		}

		// export button
		var exportVisable = selectedItems && selectedItems.length > 0 && isClassification(selectedItems[0]);
		this.exportClassificationButton.set("disabled", !exportVisable);
		if(exportVisable) {
			this.exportClassificationButton.set("iconClass", "icon16 exportClassIcon16");
		} else {
			this.exportClassificationButton.set("iconClass", "icon16 exportClassDisabledIcon16");			
		}
	}

	function add() {
		this.addTreeItemButton.set("disabled", true);
		this.addTreeItemButton.set("iconClass", "icon16 addDisabledIcon16");
		this.tree.addToSelected();
		this.addTreeItemButton.set("disabled", false);
		this.addTreeItemButton.set("iconClass", "icon16 addIcon16");	
	}

	function remove() {
		this.tree.removeSelected();
		this.updateToolbar();
	}

	function exportClassification() {
		var selectedItems = this.tree.getSelectedItems();
		this.exportDialog.open(getClassificationId(selectedItems[0]));
	}

	function showId() {
		this.tree.showId();
	}

	function hideId() {
		this.tree.hideId();
	}

	function setEditable(editable) {
		this.editable = editable == undefined ? true : editable;
		this.tree.setDnD(this.editable);
		this.updateToolbar();
	}

	classeditor.TreePane.prototype.create = create;
	classeditor.TreePane.prototype.loadClassification = loadClassification;
	classeditor.TreePane.prototype.updateToolbar = updateToolbar;
	classeditor.TreePane.prototype.showId = showId;
	classeditor.TreePane.prototype.hideId = hideId;
	classeditor.TreePane.prototype.setEditable = setEditable;

})();