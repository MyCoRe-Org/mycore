var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.NavigationContent = function() {
	// item lists
	this.itemList = [];
	this.oldItemList = [];
	this.hierarchy = [];

	// dirty
	this.dirty = false;
	
	// event handler
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {
	function preload() {
		console.log("load nav");
		this.loadFromUrl(wcms.settings.wcmsURL + "/navigation");
	}
	function getPreloadName() {
		return "NavigationContent";
	}
	function getPreloadWeight() {
		return 1;
	}

	function loadFromUrl(url) {
		// load items
		var xhrArgs = {
			url :  url,
			handleAs : "json",
			load : dojo.hitch(this, function(navigation) {
				var items = navigation.items;
				this.hierarchy = navigation.hierarchy;
				// items
				for(var i in items) {
					this.itemList.push(items[i]);
					this.oldItemList.push(clone(items[i]));
				}
				this.eventHandler.notify({"type" : "loaded", "navigation" : navigation});
			}),
			error : dojo.hitch(this, function(error, xhr) {
				var statusCode = xhr.xhr.status;
				var errorDialog = null;
				if (statusCode == 401) {
					wcms.util.ErrorUtils.show("unauthorized");
				} else {
					wcms.util.ErrorUtils.show();
				}
				this.eventHandler.notify({"type" : "loadError", "error" : error, "xhr" : xhr});
			})
		};
		dojo.xhrGet(xhrArgs);	
	}

	function checkDirty() {
		if(this.dirty) {
			for(var i = 0; i < this.itemList.length; i++) {
				if(this.itemList[i].dirty) {
					return;
				}
			}
			this.dirty = false;
		} else {
			for(var i = 0; i < this.itemList.length; i++) {
				if(this.itemList[i].dirty) {
					this.dirty = true;
					return;
				}
			}
		}
	}

	function updateItem(/*JSON*/ updatedItem) {
		var oldItem = getItemFromList(updatedItem.wcmsId, this.oldItemList);
		// update item
		this.itemList = removeItemFromList(updatedItem.wcmsId, this.itemList);
		var updatedItem = clone(updatedItem);
		this.itemList.push(updatedItem);
		// delete dirty flag because of deep equals check!
		delete(updatedItem.dirty);
		updatedItem.dirty = !deepEquals(updatedItem, oldItem);
		// check if item list is dirty
		var checkDirtyFunc = dojo.hitch(this, checkDirty);
		checkDirtyFunc();
		// fire event
		this.eventHandler.notify({"type" : "itemUpdated", "item" : clone(updatedItem)});
	}

	function addItem(/*JSON*/ item) {
		if(item == null) {
			console.log("Error: try to add null or undefined item!");
			return;
		}
		// create new instances of the item
		var newItem = clone(item);
		var oldItem = clone(item);
		// add to list
		this.itemList.push(newItem);
		this.oldItemList.push(oldItem);
		this.eventHandler.notify({"type" : "itemAdded", "item" : clone(item)});
	}

	function deleteItem(/*Integer*/ id) {
		if(id == null)
			return;
		var itemToDelete = getItemFromList(id, this.itemList);
		if(itemToDelete == null)
			return;
		this.itemList = removeItemFromList(id, this.itemList);
		this.eventHandler.notify({"type" : "itemDeleted", "item" : clone(itemToDelete)});
	}

	/**
	 * Restores the item with the given id.
	 */
	function restoreItem(/*Integer*/ id) {
		// get items
		var itemToRestore = getItemFromList(id, this.itemList);
		var oldItem = getItemFromList(id, this.oldItemList);
		var newItem = dojo.clone(oldItem);
		// update itemList
		var listSize = this.itemList.length;

		this.itemList = removeItemFromList(id, this.itemList);
		if(listSize != this.itemList.length) {
			this.itemList.push(newItem);
			// check if itemlist is dirty
			var checkDirtyFunc = dojo.hitch(this, checkDirty);
			checkDirtyFunc();
			this.eventHandler.notify({"type" : "itemRestored", "oldItem" : itemToRestore, "newItem" : clone(newItem)});
		} else {
			console.log("Unable to restore item " + itemToRestore);
		}
	}

	/**
	 * 
	 */
	function reset() {
		var newOldItems = [];
		// remove dirty flag from items
		for(var i = 0; i < this.itemList.length; i++) {
			var item = this.itemList[i];
			delete(item.dirty);
			newOldItems.push(clone(item));
		}
		this.oldItemList = newOldItems;
		this.dirty = false;
		this.eventHandler.notify({"type" : "itemsRestored"});
	}

	/**
	 * Returns a navigation item by id. Be aware that the
	 * returning item is only a copy! Manipulating the copy has no
	 * effect on the original item. Call updateItem() to do this!
	 */
	function getItem(/*Integer*/ id) {
		return clone(getItemFromList(id, this.itemList));
	}

	/**
	 * Returns a copy of an "old" item by id.
	 */
	function getOldItem(/*Integer*/ id) {
		return clone(getItemFromList(id, this.oldItemList));
	}

	/**
	 * Returns a copy of the navigation item list.
	 */
	function getItemList() {
		return cloneList(this.itemList);
	}
	
	/**
	 * Returns a copy of the navigation hierarchy.
	 */
	function getHierarchy() {
		return cloneList(this.hierarchy);
	}

	/**
	 * Get the webpage content of an item.
	 */
	function getWebpageContent(/*String*/ id, /*function*/ onSuccess, /*function*/ onError) {
		var item = this.getItem(id);
		if(item.content != null) {
			onSuccess(item.content, item);
		} else {
			var getWebpageContentFromServerFunc = dojo.hitch(this, getWebpageContentFromServer);
			getWebpageContentFromServerFunc(item, onSuccess, onError, true);
		}
	}

	/**
	 * Reloads the webpage content of an item from server.
	 */
	function reloadWebpageContent(/*String*/ id, /*function*/ onSuccess, /*function*/ onError) {
		var item = this.getItem(id);
		var getWebpageContentFromServerFunc = dojo.hitch(this, getWebpageContentFromServer);
		getWebpageContentFromServerFunc(item, onSuccess, onError, false);
	}

	function getWebpageContentFromServer(/*JSON*/ item, /*function*/ onSuccess, /*function*/ onError, /*boolean*/ initialize) {
		var href = item.href != null ? item.href : (item.hrefStartingPage != null ? item.hrefStartingPage : null);
		var xhrArgs = {
			url : wcms.settings.wcmsURL + "/navigation/content?webpagePath=" + href,
			handleAs : "json",
			load : dojo.hitch(this, function(returnData) {
				var content = returnData.content;
				item.content = clone(content);
				// set content for curItem and oldItem-> doesn't make them dirty!
				if(initialize) {
					var curItem = getItemFromList(item.wcmsId, this.itemList);
					var oldItem = getItemFromList(item.wcmsId, this.oldItemList);
					curItem.content = clone(content);
					oldItem.content = clone(content);
				}
				// fire event
				this.eventHandler.notify({type : "contentLoaded", item : item});
				// callback
				onSuccess(content, item);
			}),
			error : function(error, xhr) {
				onError(error, xhr, item);
			}
		};
		dojo.xhrGet(xhrArgs);
	}

	// maybe its better to write a own class for loading and saving navigation
	// this class shouldn't use xhrget/post methods or error dialogs
	function save(/*JSON*/ treeHierarchy) {
		// delete all undefined and null properties
		for(var i = 0; i < this.itemList.length; i++) {
			deleteUndefinedProperties(this.itemList[i]);
		}
		// save navigation
		var saveObject = {
			items: this.itemList,
			hierarchy: treeHierarchy
		};
		var navXhrArgs = {
			url :  wcms.settings.wcmsURL + "/navigation/save",
			postData : dojo.toJson(saveObject),
			handleAs : "json",
			headers: { "Content-Type": "application/json; charset=utf-8"},
			error : dojo.hitch(this, function(error, xhr) {
				wcms.util.ErrorUtils.show("couldNotSave");
			}),
			load : dojo.hitch(this, function(data) {
				this.reset();
				this.eventHandler.notify({type : "saved", data : data});					
			})
		};
		dojo.xhrPost(navXhrArgs);
	}

	/*-------------------------------------------------------------------------
	 * Item Utils
	 *------------------------------------------------------------------------*/
	function isItemInList(/*Object*/ id, /*Array*/ list) {
		for(var i = 0; i < list.length; i++)
			if(id == list[i].wcmsId)
				return true;
		return false;
	}

	function getItemFromList(/*Object*/ id, /*Array*/ list) {
		for(var i = 0; i < list.length; i++)
			if(id == list[i].wcmsId)
				return list[i];
		return null;
	}

	function removeItemFromList(/*Object*/ id, /*Array*/ list) {
		for (var i = 0; i < list.length; i++) {
			if (list[i].wcmsId == id) {
				var cut1 = list.slice(0, i);
				var cut2 = list.slice(i + 1, list.length);
				return cut1.concat(cut2);
			}
		}
		return list;
	}

	wcms.navigation.NavigationContent.prototype.preload = preload;
	wcms.navigation.NavigationContent.prototype.getPreloadName = getPreloadName;
	wcms.navigation.NavigationContent.prototype.getPreloadWeight = getPreloadWeight;
	wcms.navigation.NavigationContent.prototype.loadFromUrl = loadFromUrl;
	wcms.navigation.NavigationContent.prototype.updateItem = updateItem;
	wcms.navigation.NavigationContent.prototype.addItem = addItem;
	wcms.navigation.NavigationContent.prototype.deleteItem = deleteItem;
	wcms.navigation.NavigationContent.prototype.restoreItem = restoreItem;
	wcms.navigation.NavigationContent.prototype.reset = reset;
	wcms.navigation.NavigationContent.prototype.getItem = getItem;
	wcms.navigation.NavigationContent.prototype.getOldItem = getOldItem;
	wcms.navigation.NavigationContent.prototype.getItemList = getItemList;
	wcms.navigation.NavigationContent.prototype.getWebpageContent = getWebpageContent;
	wcms.navigation.NavigationContent.prototype.reloadWebpageContent = reloadWebpageContent;
	wcms.navigation.NavigationContent.prototype.save = save;
})();