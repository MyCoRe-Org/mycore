define([
	"dojo/_base/declare", // declare
	"dijit/layout/ContentPane",
	"dojo/Evented", // to use on.emit
	"mycore/classification/_SettingsMixin",
	"dojo/on", // on
	"dojo/aspect",
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dojo/dom-style",
	"dojo/_base/array", // forEach
	"dojo/request/xhr",
	"mycore/classification/Util",
	"mycore/classification/TreeDndSource",
	"mycore/common/I18nManager",
	"dijit/Tree"
], function(declare, ContentPane, Evented, _SettingsMixin, on, aspect, lang, domConstruct, domStyle, array, xhr, classUtil, dndSource, i18n) {

return declare("mycore.classification.LazyLoadingTree", [ContentPane, Evented, _SettingsMixin], {

	classificationId: null,

	categoryId: null,

	enableDnD: true,
	
	disabled: false,
	
	filterList: null,

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

    createTree: function(model) {
    	model.onChildrenChange = lang.hitch(this, this.onChildrenChange);

		this.tree = new dijit.Tree({
			model: model,
			dndController: dndSource,
			betweenThreshold: "5",
			dragThreshold: "5",
			persist: false,
			showRoot: true,
			getLabel: lang.hitch(this, this.getLabel),
			getIconClass: lang.hitch(this, this.getIconClass),
			checkItemAcceptance: lang.hitch(this, this.checkItemAcceptance),
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
		aspect.after(this.tree.dndController, "onDndDrop", lang.hitch(this, this.onDndDrop));
		aspect.after(this.tree, "onOpen", lang.hitch(this, this.onOpen));
		aspect.after(this.tree, "focusNode", lang.hitch(this, this.onFocusNode));
		on.emit(this, "treeCreated");
	},

	/**
	 * Set moved attribute to dropped items.
	 * 
	 * @param source
	 * @param nodes
	 */
    onDndDrop: function(deferred, args) {
    	var parent = null;
    	var source = args[0];
    	var nodes = args[1];
    	array.forEach(nodes, lang.hitch(this, function(node) {
    		var sourceItem = source.getItem(node.id);
    		var item = sourceItem.data.item;
    		if(parent == null) {
    			var parent = sourceItem.data.getParent().item;
    		}
    		on.emit(this, "itemMoved", {
    			item: item,
    			parent: parent,
    			index: this.tree.model.indexAt(item, parent)
    		});
    	}));
	},

	/**
	 * Is called when the children of an item are changed.
	 */
	onChildrenChange: function(item) {
		if(item.fakeRoot) {
			return;
		}
		item.modified = true;
		if(item.children && item.children.length <= 0) {
			delete (item.children);
			delete (item.haschildren);
		}
		// just expand the nodes
		var nodes = this.tree.getNodesByItem(item);
		array.forEach(nodes, function(node) {
			node.expand();
		});
		this.updateLabel(item);
	},

	onOpen: function(undefinedArgh, node) {
		this._updateLabels(node[0]);
	},

	setLabelColor: function(item, color) {
		var nodes = this.tree.getNodesByItem(item);
    	array.forEach(nodes, function(node) {
    		if(node != null) {
    			domStyle.set(node.labelNode, "color",  color);
    		} else {
    			console.log("Unable to get node of item " + classUtil.toString(item));
    		}
    	});
	},

	getLabelColor: function(item) {
		return (item.fakeRoot || this.filterList == null || this.filterList.indexOf(classUtil.formatId(item)) >= 0) ? "#000000" : "#BBBBBB";
	},

	updateLabel: function(item) {
		this.setLabel(item, this.getLabel(item));
		this.setLabelColor(item, this.getLabelColor(item));
	},

    updateLabels: function() {
    	var rootItem = this.tree.model.root;
    	var label = this.getLabel(rootItem);
    	this.setLabel(rootItem, label);
    	this._updateLabels(rootItem);
    },

    _updateLabels: function(item) {
    	if(!item.children) {
    		return;
    	}
    	for(var i = 0; i < item.children.length; i++) {
    		this.updateLabel(item.children[i]);
    		this._updateLabels(item.children[i]);
    	}
    },

    setLabel: function(item, label) {
    	var nodes = this.tree.getNodesByItem(item);
    	array.forEach(nodes, function(node) {
    		if(node != null) {
    			node.set("label", label);
    		} else {
    			console.log("Unable to get node of item " + classUtil.toString(item));
    		}
    	});
    },

    getLabel: function(item) {
		var currentLang = i18n.getLanguage();
		if(item.labels && item.labels.length > 0) {
			label = this.getLabelText(currentLang, item.labels);
			if(this.showIdInLabel && item.id.rootid) {
				label += " [" + this.getLabelId(item.id) + "]";
			}
			if(item.modified || item.added) {
				label = "> " + label;
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

	getIconClass: function(item) {
		// root
		if(item.fakeRoot) {
			return "icon22 classification";
		}
		// klassifikation (ends with a point)
		if(classUtil.isClassification(item)) { 
			return "icon22 classification";
		} else {
			if(classUtil.hasChildren(item)) {
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
			if(item.id.rootid != draggedItem.id.rootid)
				return false;
		}
		return true;
	},

	onFocusNode: function() {
		var treeItem = this.tree.lastFocused.item;
		if(treeItem.fakeRoot) {
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
		xhr(this.settings.resourceURL + "newID" + rootIdRequestPath, {
			handleAs : "json"
		}).then(lang.hitch(this, function(newID) {
			this.newItem(selectedItem, newID);
		}), function(err) {
			console.log("error while retrieving new id: " + err);
		});
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
				if(this.tree.model.isDescendant(selectedTreeItems[i], selectedTreeItems[j])) {
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
	},

	newItem: function(/*dojo.data.item*/ parent, /*JSON*/ newID) {
		if(parent.children && parent.children[0] == false) {
			delete(parent.children);
		}
		var newItem = {
			id: newID,
			labels: [],
			added: true	
		};
		var languages = i18n.getLanguages();
		for(var i = 0; i < languages.length; i++) {
			newItem.labels.push({lang: languages[i], text: ""});
		}
		this.tree.model.insert(newItem, parent);
		on.emit(this, "itemAdded", {item: newItem, parent: parent});
	},

	/**
	 * This method deletes an item and all its children from the store. That is
	 * necessary because store.deleteItem() doesn't delete recursive.
	 */
	removeTreeItem: function(/* dojo.data.item */ item) {
		this.tree.model.remove(item);
		on.emit(this, "itemRemoved", {item: item});
	},

	/**
	 * Get all items selected in tree.
	 */
	getSelectedItems: function() {
		return this.tree != null ? this.tree.selectedItems : null;
	},

	showID: function() {
		this.showIdInLabel = true;
		this.updateLabels();
	},

	hideID: function() {
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
	},

	/**
	 * Updates the id of a node. False is returned if a node couldn't
	 * be updated cause another node has the same id.
	 */
	updateIdOfNode: function(item, oldID) {
		var newIdentity = this.tree.model.getIdentity(item);
		// well we have to use the internal array _itemNodesMap cause there is no alternative
		if(this.tree._itemNodesMap[newIdentity] != null) {
			// a node with the same id already exists
			return false;
		}
		var oldIdentity = this.tree.model.getIdentity(oldID);
		this.tree._itemNodesMap[newIdentity] = this.tree._itemNodesMap[oldIdentity];
		delete this.tree._itemNodesMap[oldIdentity];
		return true;
	},

	filter: function(value) {
		if(value == "") {
			this.filterList = null;
			this.updateLabels();
		} else {
			xhr(this.settings.resourceURL + "filter/" + value, {
				handleAs : "json"
			}).then(lang.hitch(this, function(filterArray) {
				this.filterList = filterArray;
				this.updateLabels();
			}), function(err) {
				console.log("error while filtering: " + err);
			});
		}
	}

});
});
