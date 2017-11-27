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
	"dojo/_base/declare",
	"dojo/Evented", // to use on.emit
	"mycore/classification/_SettingsMixin",
	"dojo/_base/lang", // hitch, clone
	"dojo/on", // on
	"dojo/request/xhr", // xhr
	'dojo/_base/json',
	"mycore/common/I18nManager",
	"mycore/classification/Util",
	"mycore/classification/RestStore",
], function(declare, Evented, _SettingsMixin, lang, on, xhr, json, i18n, classUtil, RestStore) {

/**
 * 
 */
return declare("mycore.classification.Store", [Evented, _SettingsMixin], {

	restStore: null,

	saveArray: [],

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    onSettingsReady: function() {
		var url = this.settings.resourceURL;
		var classID = classeditor.classId;
		var categID = classeditor.categoryId;
		var query = classID;
		if(classID != null && classID != "" && categID != null && categID != "") {
			query += "/" + categID;
		}
		this.restStore = new RestStore({
			target: this.settings.resourceURL,
			rootQuery: query
		});
    },

    reset: function() {
    	this.saveArray = [];
    	this.restStore.root = null;
    },

	updateSaveArray: function(/*String*/ state, /*dojo.data.item*/ item, /*dojo.data.item*/ parent) {
		// get object from array
		var saveObject = null;
		for(var i = 0; i < this.saveArray.length; i++) {
			if(classUtil.isIdEqual(this.saveArray[i].item.id, item.id)) {
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
	 */
	save: function() {
		var finalArray = [];
		for(var i = 0; i < this.saveArray.length; i++) {
			var saveObject = this.saveArray[i];
			var cleanedSaveObject = {
				item: this._cloneAndCleanUp(saveObject.item),
				state: saveObject.state
			}
			if(saveObject.state == "update" && saveObject.parent) {
				if(saveObject.parent.children) {
					cleanedSaveObject.parentId = saveObject.parent.id;
					var level = this.restStore.getLevel(saveObject.item);
					var index = this.restStore.indexAt(saveObject.item, saveObject.parent);
					cleanedSaveObject.depthLevel = level;
					cleanedSaveObject.index = index;
					cleanedSaveObject.added = saveObject.item.added;
				} else {
					cleanedSaveObject.state = "delete";
				}
			}
			finalArray.push(cleanedSaveObject);
		}
		xhr(this.settings.resourceURL + "save", {
			method: "POST",
			data : json.toJson(finalArray),
			headers: {"Content-Type": "application/json; charset=utf-8"},
			handleAs: "json"
		}).then(lang.hitch(this, function() {
			console.log("saving done");
			this.saveArray = [];
			this.restStore.reset();
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
			id: item.id,
			labels: item.labels
		};
		if(item.uri) {
			newItem.uri = item.uri;
		}
		return newItem;
	}

});
});
