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
	"dojo/store/JsonRest",
	"dojo/_base/lang", // hitch, clone
	"dojo/_base/array", // forEach
	"dojo/request/xhr",
	"mycore/classification/Util"
], function(declare, JsonRest, lang, array, xhr, classUtil) {

return declare("mycore.classification.RestStore", JsonRest, {

	root: null,

	rootQuery: null,

	getRoot: function(onItem, onError) {
		if(this.root) {
			onItem(this.root);
		} else {
			this.get(this.rootQuery).then(lang.hitch(this, function(items) {
				if(lang.isArray(items)) {
					this.root = {
						id: "_placeboid_",
						labels: [
							{lang: "de", text: "Klassifikationen"},
							{lang: "en", text: "Classifications"}
						],
						fakeRoot: true,
						children: items
					};
				} else {
					this.root = items;
				}
				onItem(this.root);
			}), lang.hitch(this, function(err) {
				this.onRootLoadError(err);
				if(onError != null) {
					onError(err);
				}
			}));
		}
	},

	onRootLoadError: function(err) {
		console.log("Unable to load node.");
		console.log(err);
	},

	onNodeLoadError: function() {
		alert("Unable to load node.");
	},

	getIdentity: function(item) {
		return classUtil.toString(item);
	},

	mayHaveChildren: function(object) {
		return "children" in object || object.haschildren;
	},

	resolveChildren: function(object, onComplete, onError) {
		if(!this.mayHaveChildren(object) || "children" in object) {
			onComplete(object);
			return;
		}
		this.get(this.getIdentity(object)).then(function(resolvedObject) {
			object.children = resolvedObject.children;
			onComplete(object);
		}, lang.hitch(this, function(err) {
			if(onError) {
				onError(err)
			}
			this.onNodeLoadError(err);
		}), function(evt) {
			console.log(evt);
		});
	},

	getChildren: function(object, onComplete, onError) {
		this.resolveChildren(object, function() {
			onComplete(object.children);
		}, onError);
	},

	pasteItem: function(child, oldParent, newParent, copy, insertIndex) {
		if(!copy) {
			// remove from oldParent
			if(!this._remove(child, oldParent)) {
				console.log("unable to paste item " + classUtil.toString(child) + " from "
						+ classUtil.toString(oldParent) + " to " + classUtil.toString(newParent) + ". Abort DnD!");
				return;
			}
		} else {
			// clone for copy
			child = lang.clone(child);
			child.added = true;
			child.id.categid = child.id.categid + "_copy";
		}
		// append to newParent
		this.resolveChildren(newParent, lang.hitch(this, function() {
			this._insert(child, newParent, insertIndex);
			if(!copy) {
				this.onChildrenChange(oldParent, oldParent.children);
			}
			this.onChildrenChange(newParent, newParent.children);
		}), function(err) {
			console.log("unable to resolve children of object " + classUtil.toString(newParent));
			console.log(err);
		});
	},

	insert: function(child, parent, index) {
		this.resolveChildren(parent, lang.hitch(this, function() {
			this._insert(child, parent, index);
			this.onChildrenChange(parent, parent.children);
		}), function(err) {
			console.log("unable to resolve children of object " + classUtil.toString(parent));
			console.log(err);
		});
	},

	remove: function(item) {
		var parent = this.getParent(item);
		if(parent == null) {
			console.log("unable to get parent of item " + classUtil.toString(item));
			return;
		}
		if(this._remove(item, parent)) {
			this.onChildrenChange(parent, parent.children);
		} else {
			console.log("unable to remove item " + item.id + " of parent " + classUtil.toString(parent));
		}
	},

	getParent: function(child) {
		return this._getParent(this.root, child);
	},

	_getParent: function(parent, toFind) {
		if(parent.children == null) {
			return null;
		}
		for(var i = 0; i < parent.children.length; i++) {
			var child = parent.children[i];
			if(classUtil.isIdEqual(child.id, toFind.id)) {
				return parent;
			}
			var possibleParent = this._getParent(child, toFind);
			if(possibleParent != null) {
				return possibleParent;
			}
		}
	},

	_remove: function(child, parent) {
		return array.some(parent.children, function(oldChild, i) {
			if(classUtil.isIdEqual(child.id, oldChild.id)) {
				parent.children.splice(i, 1);
				return true;
			}
		});
	},

	_insert: function(child, parent, index) {
		if(!parent.children) {
			parent.children = [];
		}
		parent.children.splice(index || 0, 0, child);
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

	getDescendants: function(item) {
		var descendants = [];
		if(item.children == null) {
			return descendants;
		}
		for(var i = 0; i < item.children.length; i++) {
			var child = item.children[i];
			descendants.push(child);
			descendants.push.apply(descendants, this.getDescendants(child));
		}
		return descendants;
	},

	/**
	 * Returns the index position of the item.
	 * 
	 * @param item
	 * @param parent
	 * @return position as integer
	 */
    indexAt: function(item, parent) {
    	parent = (parent != null) ? parent : this.getParent(item);
    	if(parent != null) {
			for(var index = 0; index < parent.children.length; index++) {
				var childItem = parent.children[index];
				if(classUtil.isIdEqual(childItem.id, item.id)) {
					return index;
				}
			}
    	}
		return -1;
    },

	getLevel: function(item) {
    	if(classUtil.isIdEqual(this.root.id, item.id)) {
    		return 0;
    	}
    	return this._getLevel(1, item, this.root);
    },

    _getLevel: function (level, item, ancestor) {
		if(!ancestor.children)
			return -1;
    	for(var i = 0; i < ancestor.children.length; i++) {
    		var child = ancestor.children[i];
			if(classUtil.isIdEqual(child.id, item.id)) {
				return level;
			}
			var ancestorLevel = this._getLevel(level + 1, item, child);
			if(ancestorLevel != -1) {
				return ancestorLevel;
			}
    	}
    	return -1;
    },

    /**
     * Removes "added" and "modified" attributes from all items.
     */
    reset: function() {
    	var items = this.getDescendants(this.root);
		for(var i = 0; i < items.length; i++) {
			delete (items[i].added);
			delete (items[i].modified);
		}
    }

});
});
