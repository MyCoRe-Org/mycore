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
 * @description model data for internal tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.Tree = function() {
	// dom
	this.domNode = new dijit.layout.ContentPane({
		region: "left",
		gutters: false,
		splitter: true,
		style: "width: 350px"
	});
	this.treePane = null;
	this.toolbar = null;

	this.addTreeItemPopupButton = null;
	this.addTreeItemButton = null;
	this.addTreeInsertItemButton = null;
	this.addTreeMenuItemButton = null;
	this.addTreeGroupItemButton = null;
	this.removeTreeItemButton = null;
	this.restoreTreeItemButton = null;

	this.popupMenu = undefined;
	this.deleteItemDialog = null;
	this.restoreItemDialog = null;

	// content
	this.content = null;

	// undo manager
	this.undoManager = null;

	// event
	this.eventHandler = new wcms.common.EventHandler(this);

	// tree
	this.tree = null;
	this.treeModel = null;

	// id counter for new item
	this.idCounter = 10000;
	
	// dnd
	this.dndUndo = undefined;
};

( function() {

	function create(/* wcms.navigation.NavigationContent */ navigationContent, /* UndoManager */ undoManager, /* JSON */ treeData) {
		this.content = navigationContent;
		this.undoManager = undoManager;

		// create dom
		var createDomFunc = dojo.hitch(this, createDom);
		createDomFunc();

		// create dijit components
		// tree
		this.treeModel = new wcms.navigation.TreeModel(treeData);		
		this.tree = new dijit.Tree({
			model: this.treeModel.storeModel,
			dndController: "dojoclasses.TreeDndSource",
			betweenThreshold: "5",
			dragThreshold: "5",
			persist:true,
			getLabel: dojo.hitch(this, getLabel),
			getLabelStyle: dojo.hitch(this, getLabelStyle),
			checkItemAcceptance: dojo.hitch(this, checkItemAcceptance),
			getIconClass: dojo.hitch(this, getIconClass),
			getIconStyle: function () {
				return {
					height: "22px",
					width: "22px",
				};
			},
			onMouseDown : dojo.hitch(this, handleMouseEvents),
			onLoad : dojo.hitch(this, onLoad)
		});
		this.treeModel.create(this.tree);
		this.treeModel.eventHandler.attach(dojo.hitch(this, function(/**/ source, /* JSON */ args) {
			if(args.type == "itemRemoved") {
				this.content.deleteItem(args.id);
			}
		}));
		this.treePane.set("content", this.tree.domNode);

		this.deleteItemDialog = new wcms.gui.SimpleDialog("yesNo",
				"component.wcms.navigation.tree.dialog.removeItemTitle",
				"component.wcms.navigation.tree.dialog.removeItem");
		this.restoreItemDialog = new wcms.gui.SimpleDialog("yesNo",
				"component.wcms.navigation.tree.dialog.restoreItemTitle",
				"component.wcms.navigation.tree.dialog.restoreItem");

		// popupmenu
		var menuStore = new dojo.data.ItemFileReadStore({
			url: resourcesPath + "/navigationPopupMenu.json"
		});
        var menuBuilder = new wcms.common.MenuBuilder(menuStore, this.tree.domNode, this);
        menuBuilder.eventHandler.attach(dojo.hitch(this, function(/* wcms.common.MenuBuilder */ source, /* Json */ args) {
			if(args.type == "complete") {
				this.popupMenu = source.menu;
				I18nManager.getInstance().updateI18nMenu(this.popupMenu);
			}
		}));
        menuBuilder.build();

        // tree events
        dojo.connect(this.tree, "focusNode", this, itemFocused);
        dojo.connect(this.treeModel.storeModel, "onChildrenChange", this, function() {
        	this.eventHandler.notify({"type" : "treeChanged"});
        });
        dojo.connect(this.tree.dndController, "onDndStart", this, onDndStart);
        dojo.connect(this.tree.dndController, "onDndDrop", this, onDndDrop);

        // dialog events
		this.deleteItemDialog.eventHandler.attach(dojo.hitch(this, function(/* wcms.gui.SimpleDialog */ source, /* Json */ args) {
			if(args.type == "yesButtonClicked") {
				 var selectedTreeItems = this.treeModel.getSelectedItems();
				 this.removeTreeItems(selectedTreeItems);
			}
		}));
		this.restoreItemDialog.eventHandler.attach(dojo.hitch(this, function(/* wcms.gui.SimpleDialog */ source, /* Json */ args) {
			if(args.type == "yesButtonClicked") {
				var selectedItem = this.getSelectedItem();
				if(selectedItem != null) {
					var id = selectedItem.wcmsId;
					var item = this.content.getItem(id);
					var oldItem = this.content.getOldItem(id);
					// restore item
					this.content.restoreItem(id);
					// undo handling
					var restoreUndo = new wcms.navigation.RestoreUndo(item, oldItem, this.content);
					this.undoManager.add(restoreUndo);
				}
			}
		}));

		// button style
		dojo.removeClass(this.addTreeItemPopupButton.domNode, "dijitDownArrowButton");
		dojo.addClass(this.addTreeItemPopupButton.domNode, "dijitUpArrowButton");

		// button events
		this.addTreeItemButton.onClick = dojo.hitch(this, addNewItem);
		this.addTreeInsertItemButton.onClick = dojo.hitch(this, addNewInsertItem);
		this.addTreeMenuItemButton.onClick = dojo.hitch(this, addNewMenuItem);
		this.addTreeGroupItemButton.onClick = dojo.hitch(this, addNewGroupItem);
		this.removeTreeItemButton.onClick = dojo.hitch(this, removeSelectedItem);
		this.restoreTreeItemButton.onClick = dojo.hitch(this, restoreSelectedItem);

		// button i18n
		this.addTreeItemButton.set("i18n", "component.wcms.navigation.tree.addItem");
		this.addTreeInsertItemButton.set("i18n", "component.wcms.navigation.tree.addInsertItem");
		this.addTreeMenuItemButton.set("i18n", "component.wcms.navigation.tree.addMenu");
		this.addTreeGroupItemButton.set("i18n", "component.wcms.navigation.tree.addGroup");

		I18nManager.getInstance().updateI18nObject(this.addTreeItemButton);
		I18nManager.getInstance().updateI18nObject(this.addTreeInsertItemButton);
		I18nManager.getInstance().updateI18nObject(this.addTreeMenuItemButton);
	}

	/**
	 * This method is called when the dijit.Tree is successfully initialized.
	 */
	function onLoad() {
		this.treeModel.reset();
	}
	
	function createDom() {
		// border container
		var treeContainer = new dijit.layout.BorderContainer({
			id: "navigationTreeContainer",
			gutters: false,
			splitter: false
		});
		// tree
		this.treePane = new dijit.layout.ContentPane({
			id: "navigationTreePane",
			region: "center",
			gutters: false,
			splitter: false
		});

		// toolbar
		this.toolbar = new dijit.Toolbar({
			id: "navigationTreeToolbar",
			region: "bottom",
			splitter: false
		});
		var createToolbarFunc = dojo.hitch(this, createToolbar);
		createToolbarFunc();

		// add to dom
		this.domNode.set("content", treeContainer);
		treeContainer.addChild(this.treePane);
		treeContainer.addChild(this.toolbar);
	}

	function createToolbar() {
		var addMenu = new dijit.Menu();

	    // toolbar buttons
		this.addTreeItemPopupButton = new dijit.form.DropDownButton({
			showLabel: false, iconClass: "icon16 addIcon16", dropDown: addMenu
		});
		this.removeTreeItemButton = new dijit.form.Button({
			showLabel: false, iconClass: "icon16 removeDisabledIcon16",
			disabled: true, tooltip: "Eintrag löschen"
		});
		this.restoreTreeItemButton = new dijit.form.Button({
			showLabel: false, iconClass: "icon16 restoreDisabledIcon16",
			disabled: true, tooltip: "Eintrag zurücksetzen"
		});
		this.addTreeItemButton = new dijit.MenuItem({
			showLabel: false, iconClass: "icon16 item16", disabled: true
		});
		this.addTreeInsertItemButton = new dijit.MenuItem({
			showLabel: false, iconClass: "icon16 insertItem"
		});
		this.addTreeMenuItemButton = new dijit.MenuItem({
			showLabel: false, iconClass: "icon16 menuItem16"
		});
		this.addTreeGroupItemButton = new dijit.MenuItem({
			showLabel: false, iconClass: "icon16 groupItem16"
		});

		// hierarchy
		this.toolbar.addChild(this.addTreeItemPopupButton);
		this.toolbar.addChild(this.removeTreeItemButton);
		this.toolbar.addChild(new dijit.ToolbarSeparator());
		this.toolbar.addChild(this.restoreTreeItemButton);
		addMenu.addChild(this.addTreeItemButton);
		addMenu.addChild(this.addTreeInsertItemButton);
		addMenu.addChild(this.addTreeMenuItemButton);
		addMenu.addChild(this.addTreeGroupItemButton);
	}

	/**
	 * Returns the root item.
	 * 
	 * @return json item
	 */
	function getRootItem() {
		return this.content.getItem(this.treeModel.getRoot().wcmsId);
	}

	function getSelectedItem() {
		var lastFocusedItem = this.treeModel.getLastFocusedItem();
		if(lastFocusedItem == null)
			return null;
		return this.content.getItem(lastFocusedItem.wcmsId);
	}

	/**
	 * Checks if a tree item is a child of another tree item.
	 */
	function isChildOf(/* TreeItem */ parent, /* TreeItem */ child) {
		for(var i in parent.children)
			if(parent.children[i].wcmsId[0] == child.wcmsId[0])
				return true;
		return false;
	}

	/**
	 * Returns the item hierarchy of the tree. This looks like: { wcmsId: "0",
	 * children: [ { wcmsId: "1" }, { wcmsId: "2", children: [...] }, ]}
	 */
	function getItemHierarchy() {
		var rootTreeItem = this.treeModel.getRoot();
		var rootItem = {wcmsId: rootTreeItem.wcmsId[0]};
		rootItem.children = this.treeModel.getItemHierarchy(rootTreeItem);
		return rootItem;
	}

	/**
	 * This method is called if a tree item is selected. It updates the toolbar
	 * and fires an "itemSelected" event.
	 */
	function itemFocused(/* EventObject */ evt) {
		// get item
		var treeItem = this.treeModel.getLastFocusedItem();
		var item = this.content.getItem(treeItem.wcmsId);
		// update toolbar
		this.updateToolbar(item);
		// fire event
		this.eventHandler.notify({"type" : "itemSelected", "item": item});
	}

	/**
	 * Adds a new item as child of the selected one.
	 */
	function addNewItem() {
		var item = {
			wcmsType: "item",
			href: "/content/folder/myfile.xml",
			labelMap: {
				de: "neuer Eintrag",
				en: "new entry"
			}
		};
		var addTreeItemFunc = dojo.hitch(this, addTreeItem);
		addTreeItemFunc(item);
	}

	/**
	 * Adds a new insert item as child of the selected one.
	 */
	function addNewInsertItem() {
		var item = {
			wcmsType: "insert",
			uri: "component:search"
		};
		var addTreeItemFunc = dojo.hitch(this, addTreeItem);
		addTreeItemFunc(item);
	}

	/**
	 * Adds a new menu item to the root.
	 */
	function addNewMenuItem() {
		var item = {
			wcmsType: "menu",
			id: "new-menu",
			labelMap: {
				de: "neues Menü",
				en: "new menu"
			}
		};
		var addTreeItemFunc = dojo.hitch(this, addTreeItem);
		addTreeItemFunc(item);
	}

	function addNewGroupItem() {
		var item = {
			wcmsType: "group",
			id: "new-group",
			labelMap: {
				de: "neue Gruppe",
				en: "new group"
			}
		};
		var addTreeItemFunc = dojo.hitch(this, addTreeItem);
		addTreeItemFunc(item);
	}

	function addTreeItem(/* JSON */ item) {
		// set a new id
		item.wcmsId = "" + this.idCounter++;
		this.content.addItem(item);
		// add to tree model
		var parentId = this.treeModel.addToSelected(item.wcmsId);

		console.log("wcms.navigation.Tree: item added");

		var insertUndo = new wcms.navigation.InsertUndo(this.content, this.treeModel, item, parentId);
		this.undoManager.add(insertUndo);

		// fire event
		this.eventHandler.notify({"type" : "itemAdded", "item": item});
	}

	function removeSelectedItem() {
		this.deleteItemDialog.show();
	}

	function removeTreeItems(/* Array<dojo.data.item> */ treeItems) {
		var removeUndo = new wcms.navigation.RemoveUndo(this.content, this.treeModel);
		removeUndo.create(treeItems);
		this.undoManager.add(removeUndo);

		// remove from model
		for(var i = 0; i < treeItems.length; i++) {
			this.treeModel.remove(treeItems[i].wcmsId[0]);
		}
	}

	function restoreSelectedItem() {
		this.restoreItemDialog.show();
	}

	function getLabel(/* TreeItem */ treeItem) {
		var item = this.content.getItem(treeItem.wcmsId);
		if(item == undefined) {
			return "Error: no linked item found (wcmsId: " + treeItem.wcmsId + ")";
		}
		var label = "";
		if(item.dirty == true)
			label ="> ";

		if(item.wcmsType == "root") {
			return label + "Navigation";
		} else if(item.wcmsType == "insert") {
			return label + item.uri;
		}

		var currentLang = I18nManager.getInstance().getLang();
		var langText = eval("item.labelMap." + currentLang);
		if(langText != undefined) {
			return label + langText;
		}

		if(item.wcmsType == "group") {
			return item.id;
		}

		// no i18n label for the current lang
		return label + "undefined";
	}
	
	function getLabelStyle(/* TreeItem */ treeItem) {
		var item = this.content.getItem(treeItem.wcmsId);
		if(item == undefined)
			return;
		if(item.wcmsType == "insert")
			return {color: "green"};
	}

	function checkItemAcceptance(/* Node */ node, source, position) {
		var treeItem = dijit.getEnclosingWidget(node).item;
		var item = this.content.getItem(treeItem.wcmsId);
		var nodes = source.getSelectedTreeNodes();
		for(var i = 0; i < nodes.length; i++) {
    		var sourceItem = source.getItem(nodes[i].id);
			var draggedTreeItem = sourceItem.data.item;
			var draggedItem = this.content.getItem(draggedTreeItem.wcmsId);
			if(draggedItem == null)
				return false;
			// nothing can be dropped on an insert element
			if(item.wcmsType == "insert" && position == "over")
				return false;
			// item and group cannot be dropped on root
			if(item.wcmsType == "root" && (draggedItem.wcmsType == "item" || draggedItem.wcmsType == "group"))
				return false;
			// a menu cannot be dropped on another menu
			if(item.wcmsType == "menu" && draggedItem.wcmsType == "menu" && position == "over")
				return false;
			// a menu cannot be dropped on an item
			if(item.wcmsType == "item" && draggedItem.wcmsType == "menu") {
				return false;
			}
			// item and group cannot be dropped behind or after a menu
			if((draggedItem.wcmsType == "item" || draggedItem.wcmsType == "group") && item.wcmsType == "menu" && position != "over") {
				return false;
			}
			// group cannot be dropped on item
			if(draggedItem.wcmsType == "group" && item.wcmsType == "item" && position == "over") {
				return false;
			}
			// group cannot be dropped before/behind an item which is not in a menu
			if(draggedItem.wcmsType == "group" && item.wcmsType == "item" && position != "over") {
				var parent = this.treeModel.getParent(item.wcmsId);
				var parentItem = this.content.getItem(parent.wcmsId);
				if(parentItem.wcmsType != "menu") {
					return false;
				}
			}
			// a menu cannot be dropped behind or after a insert item if its not
			// a child of the root element
			if(item.wcmsType == "insert" && draggedItem.wcmsType == "menu") {
				var isChildOfFunc = dojo.hitch(this, isChildOf);
				if(!isChildOfFunc(this.treeModel.getRoot(), treeItem)) {
					return false;
				}
			}
		}
		return true;
	}

	function getIconClass(/* TreeItem */ treeItem) {
		var item = this.content.getItem(treeItem.wcmsId);
		if(item == null) {
			return;
		}
		if(item.wcmsType == "root") {
			return "icon24 navigationItem";
		} else if(item.wcmsType == "menu") {
			return "icon24 menuItem";
		} else if(item.wcmsType == "insert") {
			return "icon24 insertItem";
		} else if(item.wcmsType == "group") {
			return "icon24 groupItem";
		}
		return "icon24 item";
	}

	function handleMouseEvents(evt) {
		if(evt.button == dojo.mouseButtons.RIGHT) {
			// get nodes & items
			var treeNode = dijit.getEnclosingWidget(evt.target);
			var treeItem = treeNode.item;
			var item = this.content.getItem(treeItem.wcmsId);
			// update popupmenu
			this.updatePopupMenu(item);
			// handle focus and selection stuff
			var selectedItems = this.tree.get("selectedItems");
			if(dojo.indexOf(selectedItems, treeItem) == -1) {
				// not selected
				this.tree.focusNode(treeNode);
				this.tree.set("selectedItem", treeItem);
			}
		}
	}

	/**
	 * Updates the label of the tree.
	 */
	function updateItemLang(/* String */ id) {
		this.treeModel.getTreeItem(id, dojo.hitch(this, function(treeItem) {
			var getLabelFunc = dojo.hitch(this, getLabel);
			var label = getLabelFunc(treeItem);
			this.treeModel.setLabel(treeItem, label);
		}));
	}

	function updateLang() {
		// update tree items lang
		var itemList = this.content.getItemList();
		for(var i in itemList) {
			this.updateItemLang(itemList[i].wcmsId);
		}
		// update popup menu
		if(this.popupMenu != null) {
			I18nManager.getInstance().updateI18nMenu(this.popupMenu);
		}
		// update toolbar
		I18nManager.getInstance().updateI18nObject(this.addTreeItemButton);
		I18nManager.getInstance().updateI18nObject(this.addTreeInsertItemButton);
		I18nManager.getInstance().updateI18nObject(this.addTreeMenuItemButton);
		I18nManager.getInstance().updateI18nObject(this.addTreeGroupItemButton);
		// update dialogs
		this.deleteItemDialog.updateLang();
		this.restoreItemDialog.updateLang();
	}

	function updatePopupMenu(/* JSON */ item) {			
		// popup item
		var mainChildren = this.popupMenu.getChildren();
		var addChildren = mainChildren[0].popup.getChildren();

		// add item
		if(item == null || item.wcmsType == "root") {
			addChildren[0].set("disabled", true);
		} else {
			addChildren[0].set("disabled", false);
		}
		// add menu
		if(item == null || item.wcmsType != "root") {
			addChildren[2].set("disabled", true);
		} else {
			addChildren[2].set("disabled", false);
		}
		if(item == null || item.wcmsType == "insert") {
			mainChildren[0].set("disabled", true);
		} else {
			mainChildren[0].set("disabled", false);
		}
		// add group
		addChildren[3].set("disabled", (item == null || item.wcmsType != "menu"));

		// restore item
		if(item == null || item.dirty == undefined || item.dirty == false) {
			mainChildren[3].set("disabled", true);
		} else {
			mainChildren[3].set("disabled", false);
		}
		// remove item
		if(item == null || item.wcmsType == "root") {
			mainChildren[1].set("disabled", true);
		} else {
			mainChildren[1].set("disabled", false);
		}
	}

	function updateToolbar(/* JSON */ item) {
		// restore button
		var itemDirty = (item != null) ? !item.dirty : true;
		this.restoreTreeItemButton.set("disabled", itemDirty);
		if(this.restoreTreeItemButton.disabled) {
			this.restoreTreeItemButton.set("iconClass", "icon16 restoreDisabledIcon16");
		} else {
			this.restoreTreeItemButton.set("iconClass", "icon16 restoreIcon16");
		}
		// remove button
		var removeDisabled = (item == null || item.wcmsType == "root") ? true : false;
		this.removeTreeItemButton.set("disabled", removeDisabled);
		if(this.removeTreeItemButton.disabled) {
			this.removeTreeItemButton.set("iconClass", "icon16 removeDisabledIcon16");
		} else {
			this.removeTreeItemButton.set("iconClass", "icon16 removeIcon16");
		}
		// add button
		var addDisabled = (item == null || item.wcmsType == "insert") ? true : false;
		dojo.attr(this.addTreeItemPopupButton, "disabled", addDisabled);
		if(addDisabled) {
			this.addTreeItemPopupButton.set("iconClass", "icon16 addDisabledIcon16");
			dojo.removeClass(this.addTreeItemPopupButton.domNode, "dijitDownArrowButton");
			dojo.addClass(this.addTreeItemPopupButton.domNode, "dijitUpArrowButton");
		} else {
			this.addTreeItemPopupButton.set("iconClass", "icon16 addIcon16");
			dojo.removeClass(this.addTreeItemPopupButton.domNode, "dijitDownArrowButton");
			dojo.addClass(this.addTreeItemPopupButton.domNode, "dijitUpArrowButton");
		}
		// add menu
		this.addTreeMenuItemButton.set("disabled", (item == null || item.wcmsType != "root"));
		// add item
		this.addTreeItemButton.set("disabled", (item == null || item.wcmsType == "root"));
		// add group
		this.addTreeGroupItemButton.set("disabled", (item == null || item.wcmsType != "menu"));
	}

	/**
	 * This method is called before a dnd operation is started. It sets the
	 * draggedItems variable which is needed for undo/redo support.
	 * 
	 * @param source
	 * @param nodes
	 * @return
	 */
	function onDndStart(source, nodes) {
    	// create new dnd undo editable
    	this.dndUndo = new wcms.navigation.DndUndo(this.treeModel);
    	this.dndUndo.dndStart(source, nodes);
    	
    	var index = this.dndUndo.draggedItems.length - 1;
	    if(index != -1) {
	    	var draggedItem = this.dndUndo.draggedItems[index];
	    	if(draggedItem != null) {
	    		var item = this.content.getItem(draggedItem.wcmsId);
	    		this.eventHandler.notify({"type" : "itemSelected", "item": item});
	    	}
    	}
	}

	/**
	 * Create a new dnd undo edit.
	 * 
	 * @param source
	 * @param nodes
	 * @return
	 */
    function onDndDrop(source, nodes) {
    	if(this.dndUndo == undefined || nodes == null || nodes.length <= 0)
    		return;
    	this.dndUndo.dndDrop(source, nodes);
    	this.undoManager.add(this.dndUndo);
	}

	wcms.navigation.Tree.prototype.getRootItem = getRootItem;
	wcms.navigation.Tree.prototype.getSelectedItem = getSelectedItem;
	wcms.navigation.Tree.prototype.getItemHierarchy = getItemHierarchy;
	wcms.navigation.Tree.prototype.addNewItem = addNewItem;
	wcms.navigation.Tree.prototype.addNewInsertItem = addNewInsertItem;
	wcms.navigation.Tree.prototype.addNewMenuItem = addNewMenuItem;
	wcms.navigation.Tree.prototype.addNewGroupItem = addNewGroupItem;
	wcms.navigation.Tree.prototype.addTreeItem = addTreeItem;
	wcms.navigation.Tree.prototype.removeTreeItems = removeTreeItems;
	wcms.navigation.Tree.prototype.removeSelectedItem = removeSelectedItem;
	wcms.navigation.Tree.prototype.restoreSelectedItem = restoreSelectedItem;
	wcms.navigation.Tree.prototype.create = create;
	wcms.navigation.Tree.prototype.updateItemLang = updateItemLang;
	wcms.navigation.Tree.prototype.updateLang = updateLang;
	wcms.navigation.Tree.prototype.updateToolbar = updateToolbar;
	wcms.navigation.Tree.prototype.updatePopupMenu = updatePopupMenu;

})();

wcms.navigation.TreeModel = function(/* JSON */ itemHierarchy) {
	this.tree = null;
	this.store = new dojo.data.ItemFileWriteStore({
		data: { items: itemHierarchy }
	});
	this.storeModel = new dijit.tree.TreeStoreModel({
		store: this.store
	});
	this.eventHandler = new wcms.common.EventHandler(this);
	this.oldHierarchy = null;
};

( function() {

	function create(/* dijit.Tree */ tree) {
		this.tree = tree;
	}

	function add(/* String */ id, /* String */ idOfParent) {
		this.getTreeItem(idOfParent, dojo.hitch(this, function(parent) {
			var addAndSaveFunc = dojo.hitch(this, addAndSave);
			addAndSaveFunc(id, parent);
		}));
	}

	function addToSelected(/* String */ id) {
		var parent = this.getLastFocusedItem();
		var addAndSaveFunc = dojo.hitch(this, addAndSave);
		addAndSaveFunc(id, parent);
		return parent.wcmsId[0];
	}

	function addAndSave(/* String */ id, /* dojo.data.item */ parent) {
		this.eventHandler.notify({"type" : "beforeItemAdded", "id" : id, "parent" : parent});
		// create a new tree item
		var treeItem = {wcmsId: id};
		// add the item to the selectedItem as child
		this.storeModel.newItem(treeItem, parent);
		this.eventHandler.notify({"type" : "itemAdded", "id" : id, "parent" : parent});
	}

	function remove(/* String */ id) {
		this.getTreeItem(id, dojo.hitch(this, function(treeItem) {
			var removeAndSaveFunc = dojo.hitch(this, removeAndSave);
			removeAndSaveFunc(treeItem);
		}));
	}

	function removeSelected() {
		var removeAndSaveFunc = dojo.hitch(this, removeAndSave);
		removeAndSaveFunc(this.getLastFocusedItem());
	}

	function removeAndSave(/* dojo.data.item */ treeItem) {
		// remove tree item + children
		var removeTreeItemFunc = dojo.hitch(this, removeTreeItem);
		removeTreeItemFunc(treeItem, this.store);
		// save tree
		this.store.save();
	}

	/**
	 * This method deletes an item and all its children from the store. That is
	 * necessary because store.deleteItem() doesn't delete recursive.
	 */
	function removeTreeItem(/* dojo.data.item */ treeItem) {
		this.eventHandler.notify({"type" : "beforeItemRemoved", "id" : treeItem.wcmsId});
		// delete children
		var removeTreeItemFunc = dojo.hitch(this, removeTreeItem);
		while(treeItem.children != undefined) {
			removeTreeItemFunc(treeItem.children[0]);
		}
		// delete tree item
		this.store.deleteItem(treeItem);
		// remove from content
		this.eventHandler.notify({"type" : "itemRemoved", "id" : treeItem.wcmsId});
	}

	/**
	 * Moves the item and all its children to the new parent node.
	 */
	function move(/*String*/ wcmsId, /* dojo.data.item */ from,/* dojo.data.item */ to, /* int */ insertAt) {
		this.getTreeItem(wcmsId, dojo.hitch(this, function(item) {
			this.storeModel.pasteItem(item, from, to, false, insertAt);
		}));
	}

	function paste(/* JSON */ item, /* dojo.data.item */ parent, /* int */ insertAt) {
		var treeItem = {wcmsId: item.wcmsId};
		// add the item to the selectedItem as child
		this.storeModel.newItem(treeItem, parent, insertAt);
		if(item.children) {
			this.getTreeItem(item.wcmsId, dojo.hitch(this, function(newParent) {
				// add children recursive
				for(var i = 0; i < item.children.length; i++) {
					var childItem = item.children[i];
					this.paste(childItem, newParent, i);
				}
			}));
		}
	}

	function setLabel(/* dojo.data.item */ treeItem, /* String */ label) {
		if(label != null && treeItem != null)
			this.store.setValue(treeItem, "name", label);
	}

	function getTreeItem(/* String id */ wcmsId, /* function */ callback) {
		this.store.fetch({
			query: {wcmsId: wcmsId},
			scope: this,
			queryOptions: {deep: true},
			onItem: callback
		});
	}

	function getLastFocusedItem() {
		return this.tree.lastFocused.item;
	}

	function getSelectedItems() {
		return this.tree.selectedItems;
	}

	/**
	 * Returns the root tree item.
	 * 
	 * @return dojo.data.item treeItem
	 */
	function getRoot() {
		return this.storeModel.root;
	}

	function getParent(/* String */ id) {
		var root = this.getRoot();
		return _getParent(root, id);
	}

	function _getParent(/* dojo.data.item */ parent, /* String */ id) {
		if(!parent.children)
			return;
		for(var i = 0; i < parent.children.length; i++) {
			var child = parent.children[i];
			if(child.wcmsId[0] == id)
				return parent;
			var parentOfChild = _getParent(child, id);
			if(parentOfChild != null)
				return parentOfChild;
		}
		return null;
	}

	/**
	 * Returns the position of the children.
	 * 
	 * @param parent
	 * @param id
	 *            id of children
	 * @return position as integer
	 */
    function indexAt(/* dojo.data.item */ parent, /* String */ id) {
		for(var index = 0; index < parent.children.length; index++) {
			var childItem = parent.children[index];
			var childId = childItem.wcmsId[0];
			if(childId == id) {
				return index;
			}
		}
		return -1;
    }

	/**
	 * Returns the tree item hierarchy as json.
	 */
	function getItemHierarchy(/* dojo.data.item */ parent, /* Callback */ onItem) {
		var children = [];
		dojo.forEach(parent.children, function(treeItem, index) {
			var hierarchyItem = {wcmsId: treeItem.wcmsId[0]};
			// call it recursive to add all children
			if(treeItem.children) {
				hierarchyItem.children = getItemHierarchy(treeItem, onItem);
			}
			// callback function for each item
			if(onItem) {
				onItem(hierarchyItem, treeItem, index);
			}
			children.push(hierarchyItem);
		});
		return children;
	}

	function reset() {
		this.oldHierarchy = getItemHierarchy(this.getRoot());
	}

	function isDirty() {
		if(this.oldHierarchy == null) {
			console.log("Cannot check if tree is dirty. Tree is not correct initialized!");
			return false;
		}
		var currentHierarchy = getItemHierarchy(this.getRoot());
		return !(deepEquals(this.oldHierarchy, currentHierarchy));
	}

	wcms.navigation.TreeModel.prototype.create = create;
	wcms.navigation.TreeModel.prototype.add = add;
	wcms.navigation.TreeModel.prototype.addToSelected = addToSelected;
	wcms.navigation.TreeModel.prototype.remove = remove;
	wcms.navigation.TreeModel.prototype.removeSelected = removeSelected;
	wcms.navigation.TreeModel.prototype.move = move;
	wcms.navigation.TreeModel.prototype.paste = paste;
	wcms.navigation.TreeModel.prototype.setLabel = setLabel;
	wcms.navigation.TreeModel.prototype.getTreeItem = getTreeItem;
	wcms.navigation.TreeModel.prototype.getLastFocusedItem = getLastFocusedItem;
	wcms.navigation.TreeModel.prototype.getSelectedItems = getSelectedItems;
	wcms.navigation.TreeModel.prototype.getRoot = getRoot;
	wcms.navigation.TreeModel.prototype.getParent = getParent;
	wcms.navigation.TreeModel.prototype.indexAt = indexAt;
	wcms.navigation.TreeModel.prototype.getItemHierarchy = getItemHierarchy;
	wcms.navigation.TreeModel.prototype.reset = reset;
	wcms.navigation.TreeModel.prototype.isDirty = isDirty;
})();
