define([
	"dojo/_base/declare", // declare
	"dijit/layout/ContentPane",
	"dojo/Evented", // to use on.emit
	"mycore/classification/_SettingsMixin",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"mycore/classification/Util",
	"mycore/classification/TreeDndSource",
	"mycore/common/I18nManager",
	"dijit/Tree",
	"dijit/tree/TreeStoreModel"
], function(declare, ContentPane, Evented, _SettingsMixin, on, lang, domConstruct, classUtil, dndSource, i18n) {

return declare("mycore.classification.LazyLoadingTree", [ContentPane, Evented, _SettingsMixin], {

	classificationId: null,

	categoryId: null,

	enableDnD: true,
	
	store: null,

	disabled: false,

    constructor: function(/*Object*/ args) {
    	this.gutters = false;
    	this.splitter = false;
    	declare.safeMixin(this, args);
    },

    create: function(args) {
    	this.inherited(arguments);
    },

    onSettingsReady: function() {
    	this.showIdInLabel = this.settings.showId ? this.settings.showId : false;
    },

    createTree: function(store) {
    	this.store = store;

		this.treeModel = new dijit.tree.TreeStoreModel({
			store: this.store,
			deferItemLoadingUntilExpand: false,
			mayHaveChildren: classUtil.hasChildren
		});

		this.tree = new dijit.Tree({
			model: this.treeModel,
			dndController: dndSource,
			betweenThreshold: "5",
			dragThreshold: "5",
			persist: false,
			showRoot: true,
			getLabel: lang.hitch(this, this.getLabel),
			getIconClass: lang.hitch(this, this.getIconClass),
			checkItemAcceptance: lang.hitch(this, this.checkItemAcceptance),
			expandNode: function(/*TreeNode*/ node) {
				// TODO - maybe there is a better solution than calling a private method
				this._expandNode(node, false);
			},
			getIconStyle: function () {
				return {
					height: "22px",
					width: "22px",
				};
			}
		});
		this.tree.dndController.setEnabled(this.enableDnD);
		this.set("content", this.tree.domNode);
		this.tree.startup();

		// events
        dojo.connect(this.tree.dndController, "onDndDrop", this, this._onDndDrop);
		dojo.connect(this.tree, "focusNode", this, this._onFocusNode);
		on.emit(this, "treeCreated");
	},

	/**
	 * Set moved attribute to dropped items.
	 * 
	 * @param source
	 * @param nodes
	 */
    _onDndDrop: function(source, nodes) {
    	var parent = null;
    	dojo.forEach(nodes, lang.hitch(this, function(node) {
    		var sourceItem = source.getItem(node.id);
    		var item = sourceItem.data.item;
    		if(parent == null) {
    			var parent = sourceItem.data.getParent().item;
    		}
    		var index = this.indexAt(parent, item.id[0]);
    		on.emit(this, "itemMoved", {item: item, parent: parent, index: index});
    	}));
	},

	/**
	 * Returns the position of the children.
	 * 
	 * @param parent
	 * @param id
	 *            id of children
	 * @return position as integer
	 */
    indexAt: function(/* dojo.data.item */ parent, /* JSON */ id) {
		for(var index = 0; index < parent.children.length; index++) {
			var childItem = parent.children[index];
			if(classUtil.isIdEqual(childItem.id[0], id)) {
				return index;
			}
		}
		return -1;
    },

	getLevel: function(obj) {
    	var item = obj.item, root = this.treeModel.root;
    	if(classUtil.isIdEqual(root.id[0], item.id[0])) {
    		return 0;
    	}
    	return this._getLevel(1, item, root);
    },

    _getLevel: function (level, item, ancestor) {
		if(!ancestor.children)
			return -1;
    	for(var i = 0; i < ancestor.children.length; i++) {
    		var child = ancestor.children[i];
			if(classUtil.isIdEqual(child.id[0], item.id[0])) {
				return level;
			}
			var ancestorLevel = this._getLevel(level + 1, item, child);
			if(ancestorLevel != -1) {
				return ancestorLevel;
			}
    	}
    	return -1;
    },

    updateLabels: function() {
    	var root = this.treeModel.root;
    	var label = this.getLabel(root);
    	this.store.setValue(root, "name", label);
    	this._updateLabels(root);
    },

    _updateLabels: function(/*TreeItem*/ item) {
    	if(!item.children) {
    		return;
    	}
    	for(var i = 0; i < item.children.length; i++) {
    		var child = item.children[i];
    		this.store.setValue(child, "name", this.getLabel(child));
    		this._updateLabels(child);
    	}
    },

    getLabel: function(/* TreeItem */ treeItem) {
		var currentLang = i18n.getLanguage();
		if(treeItem.labels && treeItem.labels.length > 0) {
			label = this.getLabelText(currentLang, treeItem.labels);
			if(this.showIdInLabel && treeItem.id[0].rootid) {
				label += " [" + this.getLabelId(treeItem.id[0]) + "]";
			}
			return label;
		}
		return "undefined";
	},

	getLabelText: function(/*String*/ currentLang, /*Array*/ labels) {
		for(var i = 0; i < labels.length; i++) {
			if(labels[i].lang == currentLang)
				return labels[i].text;
		}
		return labels[0].text;
	},

	getLabelId: function(/*JSON*/ id) {
		if(!id.categid) {
			return id.rootid;
		}
		return id.categid;
	},

	getIconClass: function(/* TreeItem */ treeItem) {
		// root
		if(treeItem.notAnItem && treeItem.notAnItem[0] == true) {
			return "icon22 classification";
		}
		// klassifikation (ends with a point)
		if(classUtil.isClassification(treeItem)) { 
			return "icon22 classification";
		} else {
			if(classUtil.hasChildren(treeItem)) {
				return "icon22 internalNode"
			} else {
				return "icon22 leafNode";
			}
		}
	},

	checkItemAcceptance: function(/*TreeNode*/ node, source, position) {
		var item = dijit.getEnclosingWidget(node).item;
		var nodes = source.getSelectedTreeNodes();
		for(var i = 0; i < nodes.length; i++) {
    		var sourceItem = source.getItem(nodes[i].id);
			var draggedItem = sourceItem.data.item;
			if(draggedItem == null)
				return false;
			// unknown ids
			if(!item.id || !draggedItem.id)
				return false;
			// cannot drop an item behind/after a classification
			if(classUtil.isClassification(item) && position != "over")
				return false;
			// cannot drop an item in another classification
			if(item.id[0].rootid != draggedItem.id[0].rootid)
				return false;
		}
		return true;
	},

	focusItem: function(/*dojo.data.item*/ item) {
		// select it
		this.tree.set("selectedItem", item);
		// try to focus
		var treeNode = this.tree.getNodesByItem(item)[0];
		var parentNode = treeNode.getParent();
		// expand parent node if its not expanded yet
		if(parentNode.isExpandable && !parentNode.isExpanded) {
			this.tree.expandNode(parentNode);
		}
		// focus it!
		this.tree.focusNode(treeNode);
	},

	_onFocusNode: function() {
		var treeItem = this.tree.lastFocused.item;
		if(treeItem.notAnItem && treeItem.notAnItem[0] == true) {
			treeItem = null;
		}
		on.emit(this, "itemSelected", {item: treeItem});
	},

	/**
	 * Creates a new tree item and adds them to the last focused item.
	 */
	addToSelected: function() {
		var selectedNode = this.tree.lastFocused;
		var selectedItem = selectedNode.item;
		// get root id
		var rootId = classUtil.getClassificationId(selectedItem);
		var rootIdRequestPath = rootId.length > 0 ? "/" + rootId : "";
		// get new category id
		var xhrArgs = {
			url :  this.settings.resourceURL + "newID" + rootIdRequestPath,
			handleAs : "json",
			load : lang.hitch(this, function(newId) {
				this.newItem(selectedItem, newId);
				// expand node if its not expanded yet
				if(selectedNode.isExpandable && !selectedNode.isExpanded) {
					this.tree.expandNode(selectedNode);
				}
			}),
			error : lang.hitch(this, function(error) {
				console.log("error while retrieving new id: " + error);
			})
		};
		dojo.xhrGet(xhrArgs);
	},

	/**
	 * Removes all selected tree items.
	 */
	removeSelected: function() {
		var selectedTreeItems = this.getSelectedItems();
		// remove only items which are not a descendant of another selected
		// this is important to avoid side effects
		var itemsToRemoveArray = [];
		for(var i = 0; i < selectedTreeItems.length; i++) {
			var descendant = false;
			for(var j = 0; j < selectedTreeItems.length; j++) {
				if(this.isDescendant(selectedTreeItems[i], selectedTreeItems[j])) {
					descendant = true;
					break;
				}
			}
			if(!descendant) {
				itemsToRemoveArray.push(selectedTreeItems[i]);
			}
		}
		// remove from tree
		for(var i = 0; i < itemsToRemoveArray.length; i++) {
			this.removeTreeItem(itemsToRemoveArray[i]);
		}
		on.emit(this, "itemsRemoved", {items: itemsToRemoveArray});
	},

	newItem: function(/*dojo.data.item*/ parent, /*JSON*/ newId) {
		if(parent.children && parent.children[0] == false) {
			delete(parent.children);
		}
		var newItem = {
			id: newId,
			labels: [
			    {lang: "de", text: "neuer Eintrag"}
			],
			added: true	
		};
		newItem = this.store.newItem(newItem, {parent: parent, attribute: "children"});
		this.store.save();
		on.emit(this, "itemAdded", {item: newItem, parent: parent});
	},

	/**
	 * This method deletes an item and all its children from the store. That is
	 * necessary because store.deleteItem() doesn't delete recursive.
	 */
	removeTreeItem: function(/* dojo.data.item */ item) {
		// delete children
		while(classUtil.hasChildrenLoaded(item)) {
			this.removeTreeItem(item.children[0]);
		}
		// delete tree item
		this.store.deleteItem(item);
		// FIRE!!!!!
		on.emit(this, "itemRemoved", {item: item});
	},

	/**
	 * Checks if an item is an descendant of another item.
	 */
	isDescendant: function(/*dojo.data.item*/ item, /*dojo.data.item*/ ancestor) {
		// same item
		if(item.id == ancestor.id)
			return false;
		if(ancestor.children && (typeof ancestor.children[0]) != "boolean") {
			for(var i = 0; i < ancestor.children.length; i++) {
				var childItem = ancestor.children[i];
				if(item.id == childItem.id || this.isDescendant(item, childItem)) {
					return true;
				}
			}
		}
		return false;
	},

	/**
	 * Get all items selected in tree.
	 */
	getSelectedItems: function() {
		return this.tree != null ? this.tree.selectedItems : null;
	},

	showId: function() {
		this.showIdInLabel = true;
		this.updateLabels();
	},

	hideId: function() {
		this.showIdInLabel = false;
		this.updateLabels();
	},

	setDnD: function(enableDnD) {
		this.enableDnD = enableDnD;
		if(this.tree) {
			this.tree.dndController.setEnabled(this.enableDnD);
		}
	},

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.disabled = disabled;
		if(this.tree != null) {
			this.tree.set("disabled", disabled);
		}
	}

});
});
