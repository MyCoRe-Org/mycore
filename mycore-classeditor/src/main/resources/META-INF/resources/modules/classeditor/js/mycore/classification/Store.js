define([
	"dojo/_base/declare",
	"dojo/Evented", // to use on.emit
	"mycore/classification/_SettingsMixin",
	"dojo/_base/lang", // hitch, clone
	"dojo/on", // on
	"dojo/request/xhr", // xhr
	'dojo/_base/json',
	"mycore/common/I18nManager",
	"mycore/classification/Util",
	"mycore/classification/SimpleRESTStore"
], function(declare, Evented, _SettingsMixin, lang, on, xhr, json, i18n, classUtil) {

/**
 * 
 */
return declare("mycore.classification.Store", [Evented, _SettingsMixin], {

	classificationId: "",

	categoryId: "",

	items: null,

	restStore: null,

	saveArray: null,

    constructor: function(/*Object*/ args) {
		this.saveArray = [];
    	declare.safeMixin(this, args);
    },

    load: function(/*function*/ onSuccess, /*function*/ onEvent) {
		var url = this.settings.resourceURL + this.classificationId;
		if(this.classificationId != null && this.classificationId != "" &&
				this.categoryId != null && this.categoryId != "") {
			url += "/" + categoryId;
		}
		xhr(url, {handleAs: "json"}).then(lang.hitch(this, function(items) {
			if(lang.isArray(items)) {
				items = {
					id: "_placeboid_",
					labels: [
						{lang: "de", text: "Klassifikationen"},
						{lang: "en", text: "Classifications"}
					],
					notAnItem: true,
					children: items
				};
			}
			this.items = items;
			this.restStore = new mycore.classification.SimpleRESTStore({
				settings: this.settings,
				data: {items: [items]}
			});
			this.saveArray = [];
			if(onSuccess) {
				onSuccess(this.restStore);
			}
		}), function(error) {
			console.log("error while retrieving classification items from url " + url + "! " + error);
		}, function(evt) {
			if(onEvent) {
				onEvent(evt);
			}
		});
    },

    setValue: function(item, type, value) {
    	this.restStore.setValue(item, type, value);
    },

	updateSaveArray: function(/*String*/ state, /*dojo.data.item*/ item, /*dojo.data.item*/ parent) {
		// get object from array
		var saveObject = null;
		for(var i = 0; i < this.saveArray.length; i++) {
			if(classUtil.isIdEqual(this.saveArray[i].item.id[0], item.id[0])) {
				saveObject = this.saveArray[i];
			}
		}
		// if not defined -> create new and add to array
		if(saveObject == null) {
			saveObject = {};
			this.saveArray.push(saveObject);
		}
		// set new data
		saveObject.item = item;
		saveObject.state = state;
		if(parent != null) {
			saveObject.parent = parent;
		}
	},

	/**
	 * Commit all changes to the MyCoRe Application.
	 * @tree TODO bad practice using the lazyloadingtree here
	 */
	save: function(tree) {
		var finalArray = [];
		for(var i = 0; i < this.saveArray.length; i++) {
			var saveObject = this.saveArray[i];
			var cleanedSaveObject = {
				item: this._cloneAndCleanUp(saveObject.item),
				state: saveObject.state
			}
			if(saveObject.state == "update" && saveObject.parent) {
				if(saveObject.parent.children) {
					cleanedSaveObject.parentId = saveObject.parent.id[0];
					var level = tree.getLevel(saveObject);
					var index = tree.indexAt(saveObject.parent, saveObject.item.id[0]);
					cleanedSaveObject.depthLevel = level;
					cleanedSaveObject.index = index;
				} else {
					cleanedSaveObject.state = "delete";
				}
			}
			finalArray.push(cleanedSaveObject);
		}
		xhr(this.settings.resourceURL + "save", {
			method: "POST",
			data : json.toJson(finalArray),
			handleAs : "xml", // a possible error is returned as xml 
			headers: {"Content-Type": "application/json; charset=utf-8"}
		}).then(lang.hitch(this, function() {
			console.log("saving done");
			for(var i = 0; i < this.saveArray.length; i++) {
				var saveObject = this.saveArray[i];
				if(saveObject.item.added) {
					saveObject.item.added = false;
				}
			}
			this.saveArray = [];
			on.emit(this, "saved");
		}), lang.hitch(this, function(error) {
			on.emit(this, "saveError", error);
		}), lang.hitch(this, function(event) {
			on.emit(this, "saveEvent", event);
		}));
	},

	set: function(param, value) {
		if(param == "settings") {
			this.settings = value;
		}
	},

	isDirty: function() {
		return this.saveArray.length > 0;
	},

	_cloneAndCleanUp: function(/*dojo.data.item*/ item) {
		var newItem = {
			id: item.id[0],
			labels: item.labels
		};
		if(item.uri) {
			newItem.uri = item.uri[0];
		}
		return newItem;
	}

});
});
