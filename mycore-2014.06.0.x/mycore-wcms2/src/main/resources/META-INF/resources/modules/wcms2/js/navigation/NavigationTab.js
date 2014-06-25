var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.NavigationTab = function(navContent) {
	// dom
	this.domNode = null;
	this.navigationToolbar = null;
	this.saveChangesButton = null;
	this.undoButton = null;
	this.redoButton = null;
	this.contentPane = null;

	// vars
	this.tree = null;

	// item editors
	this.rootItemEditor = null;
	this.menuItemEditor = null;
	this.itemEditor = null;
	this.insertItemEditor = null;
	this.groupEditor = null;

	this.currentEditor = null;

	// content
	this.content = navContent;

	// undo manager
	this.undoManager = null;
};

( function() {
	
	function preload() {
		console.log("load navTab");
		// create tree
		this.tree = new wcms.navigation.Tree();
		// create item editors
		this.rootItemEditor = new wcms.navigation.RootItemEditor();
		this.menuItemEditor = new wcms.navigation.MenuItemEditor();
		this.itemEditor = new wcms.navigation.ItemEditor();
		this.insertItemEditor = new wcms.navigation.InsertItemEditor();
		this.groupEditor = new wcms.navigation.GroupEditor();
		this.currentEditor = this.rootItemEditor;
		
		// create undo manager
		this.undoManager = new wcms.common.UndoManager();
		this.undoManager.eventHandler.attach(dojo.hitch(this, handleUndoManagerEvents));

		// create dom
		var createDomFunc = dojo.hitch(this, createDom);
		createDomFunc();
		
		// create editor
		this.rootItemEditor.create(this.content);
		this.rootItemEditor.eventHandler.attach(dojo.hitch(this, handleEditorEvents));
		this.menuItemEditor.create();
		this.menuItemEditor.eventHandler.attach(dojo.hitch(this, handleEditorEvents));
		this.itemEditor.create(this.content);
		this.itemEditor.eventHandler.attach(dojo.hitch(this, handleEditorEvents));
		this.insertItemEditor.create();
		this.insertItemEditor.eventHandler.attach(dojo.hitch(this, handleEditorEvents));
		this.groupEditor.create();
		this.groupEditor.eventHandler.attach(dojo.hitch(this, handleEditorEvents));

		// button events
		this.saveChangesButton.onClick = dojo.hitch(this, saveChanges);
		this.undoButton.onClick = dojo.hitch(this, function() {
			this.undoManager.undo();
		});
		this.redoButton.onClick = dojo.hitch(this, function() {
			this.undoManager.redo();
		});

		// load content
		// create content
		this.content.eventHandler.attach(dojo.hitch(this, handleContentEvents));
	}
	
	function getPreloadName() {
		return "Navigation";
	}
	function getPreloadWeight() {
		return 10;
	}

	function createDom() {
		this.domNode = new dijit.layout.ContentPane({
			id: "navigation",
			title: "Navigation",
            selected: true
        });
		// create borderlayout in tab
		var borderContainer = new dijit.layout.BorderContainer({
			id: "navigationContainer",
			style: "border: none",
		});
		// create head toolbar
		var createHeadToolbarFunc = dojo.hitch(this, createHeadToolbar);
		createHeadToolbarFunc();

		// create empty content
		this.contentPane = new dijit.layout.ContentPane({
			region: "center",
			id: "navigationItemEditorPane",
			splitter: true
		});

		// add to dom
		this.domNode.set("content", borderContainer);
		borderContainer.addChild(this.navigationToolbar);
		borderContainer.addChild(this.tree.domNode);
		borderContainer.addChild(this.contentPane);
		this.contentPane.set("content", this.currentEditor.domNode);
	}

	function createHeadToolbar() {
		this.navigationToolbar = new dijit.Toolbar({
			id: "navigationToolbar",
			splitter: false,
			region: "top",
		});
		this.saveChangesButton = new dijit.form.Button({
			showLabel: false,
			disabled: true,
			iconClass: "icon16 saveDisabledIcon",
			tooltip: "Änderungen speichern"
		});
		this.undoButton = new dijit.form.Button({
			showLabel: false,
			disabled: true,
			iconClass: "icon16 undoDisabledIcon16",
			tooltip: "Undo"
		});
		this.redoButton = new dijit.form.Button({
			showLabel: false,
			disabled: true,
			iconClass: "icon16 redoDisabledIcon16",
			tooltip: "Redo"
		});

		this.navigationToolbar.addChild(this.undoButton);
		this.navigationToolbar.addChild(this.redoButton);
		this.navigationToolbar.addChild(new dijit.ToolbarSeparator());
		this.navigationToolbar.addChild(this.saveChangesButton);
	}

	function handleTreeEvents(/*Tree*/ source, /*Json*/ args) {
		if(args.type == "itemSelected") {
			var item = args.item;
			this.updateItemEditor(item);
		} else if(args.type == "treeChanged") {
			this.updateToolbar();
		}
	}

	function handleContentEvents(/*NavigationContent*/ source, /*Json*/ args) {
		if(args.type == "loadError") {
			this.updateItemEditor(null);
		} else if(args.type == "loaded") {
			if(args.navigation.hierarchy == null) {
				console.log("Error while loading: navigation.hierarchy is null!");
			}
			// tree
			this.tree.create(this.content, this.undoManager, args.navigation.hierarchy);
			this.tree.eventHandler.attach(dojo.hitch(this, handleTreeEvents));	
			this.tree.updateLang();
			var rootItem = this.tree.getRootItem();
			if(rootItem == null) {
				console.log("Unexpected error: root item is null! Cannot update root item editor!");
				return;
			}
			// select root item in tree
			this.tree.tree.set("selectedItem", this.tree.treeModel.getRoot());
			// update editor
			this.updateItemEditor(rootItem);
		} else if(args.type == "itemRestored") {
			var newItem = args.newItem;
			// update item editor
			this.updateItemEditor(newItem);
			// update tree
			this.tree.updateItemLang(newItem.wcmsId);
			this.tree.updateToolbar(newItem);
			// update toolbars
			this.updateToolbar();
		} else if(args.type == "itemUpdated") {
			var item = args.item;
			// update tree
			this.tree.updateItemLang(item.wcmsId);
			this.tree.updateToolbar(item);
			// update toolbar
			this.updateToolbar();
			// update item editor - only when item is currently selected
			var selectedItem = this.tree.getSelectedItem();
			if(selectedItem != null && item.wcmsId == this.tree.getSelectedItem().wcmsId) {
				this.updateItemEditor(item);
			}
		} else if(args.type == "itemDeleted") {
			if(this.currentEditor == null || this.currentEditor.currentItem == null || args.item == null) {
				return;
			}
			if(this.currentEditor.currentItem.wcmsId == args.item.wcmsId) {
				this.currentEditor.setDisabled(true);
			}
		} else if(args.type == "itemsRestored") {
			// removes the '>' signs in tree
			this.tree.updateLang();
		} else if(args.type == "saved") {
			this.updateToolbar();
		} else if(args.type == "contentLoaded") {
			this.updateItemEditor(args.item);
		}
	}

	function handleEditorEvents(/*AbstractItemEditor*/ source, /*Json*/ args) {
		if(args.type == "itemUpdated") {
			var item = args.item;
			var oldItem = this.content.getItem(item.wcmsId);
			// update navigation content
			this.content.updateItem(item);
			// add new undo editable
			var editUndo = new wcms.navigation.EditUndo(item, oldItem, this.content);
			if(args.forceNoMerge)
				this.undoManager.forceNoMerge();
			this.undoManager.add(editUndo);
		}
	}

	function handleUndoManagerEvents(/*UndoManager*/ source, /*Json*/ args) {
		var canUndo = this.undoManager.canUndo();
		var canRedo = this.undoManager.canRedo();
		
		this.undoButton.set("disabled", !canUndo);
		if(canUndo) {
			this.undoButton.set("iconClass", "icon16 undoIcon16");
		} else {
			this.undoButton.set("iconClass", "icon16 undoDisabledIcon16");
		}
		this.redoButton.set("disabled", !canRedo);
		if(canRedo) {
			this.redoButton.set("iconClass", "icon16 redoIcon16");
		} else {
			this.redoButton.set("iconClass", "icon16 redoDisabledIcon16");
		}
	}

	function updateItemEditor(/*JSON*/ item) {
		var switchEditorFunc = dojo.hitch(this, switchEditor);
		if(item == null) {
			this.currentEditor.setDisabled(true);
			return;
		}
		this.currentEditor.setDisabled(false);

		if(item.wcmsType == "item" && !(this.currentEditor instanceof wcms.navigation.ItemEditor)) {
			switchEditorFunc(this.itemEditor);
		} else if(item.wcmsType == "menu" && !(this.currentEditor instanceof wcms.navigation.MenuItemEditor)) {
			switchEditorFunc(this.menuItemEditor);
		} else if(item.wcmsType == "root" && !(this.currentEditor instanceof wcms.navigation.RootItemEditor)) {
			switchEditorFunc(this.rootItemEditor);
		} else if(item.wcmsType == "insert" && !(this.currentEditor instanceof wcms.navigation.InsertItemEditor)) {
			switchEditorFunc(this.insertItemEditor);
		} else if(item.wcmsType == "group" && !(this.currentEditor instanceof wcms.navigation.GroupEditor)) {
			switchEditorFunc(this.groupEditor);
		}
		this.currentEditor.updateEditor(item);
	}

	function switchEditor(/*wcms.gui.ContentEditor*/ editor) {
		var editorPane = dojo.byId("navigationItemEditorPane");
		editorPane.removeChild(this.currentEditor.domNode);
		this.currentEditor = editor;
		editorPane.appendChild(this.currentEditor.domNode);
	}

	function updateToolbar() {
		// general
		var dirty = this.isDirty()
		this.saveChangesButton.set("disabled", !dirty);
		if(dirty) {
			this.saveChangesButton.set("iconClass", "icon16 saveIcon");
		} else {
			this.saveChangesButton.set("iconClass","icon16 saveDisabledIcon");
		}
	}

	function isDirty() {
		return this.tree.treeModel != null && (this.tree.treeModel.isDirty() || this.content.dirty);
	}

	function updateLang() {
		this.tree.updateLang();
		this.itemEditor.updateLang();
		this.menuItemEditor.updateLang();
		this.rootItemEditor.updateLang();
		this.insertItemEditor.updateLang();
	}

	function saveChanges() {
		if(!this.isDirty())
			return;

		this.content.save([this.tree.getItemHierarchy()]);

		// reset dirty
		this.tree.treeModel.reset();
		this.undoManager.forceNoMerge();

		// update
		this.updateToolbar();
	}

	wcms.navigation.NavigationTab.prototype.isDirty = isDirty;
	wcms.navigation.NavigationTab.prototype.updateLang = updateLang;
	wcms.navigation.NavigationTab.prototype.updateItemEditor = updateItemEditor;
	wcms.navigation.NavigationTab.prototype.updateToolbar = updateToolbar;
	wcms.navigation.NavigationTab.prototype.saveChanges = saveChanges;
	wcms.navigation.NavigationTab.prototype.preload = preload;
	wcms.navigation.NavigationTab.prototype.getPreloadName = getPreloadName;
	wcms.navigation.NavigationTab.prototype.getPreloadWeight = getPreloadWeight;
	
})();