/**
 * Contains a bunch of utility functions.
 */
define([
    "exports",
	"dojo/_base/declare", // declare
	"dojo/_base/lang" // hitch, clone
], function(exports, declare, lang) {

	exports.isClassification = function(/*dojo.data.item*/ item) {
		var id = item.id;
		return (id.categid == null || id.categid == "") && !item.fakeRoot;
	}

	exports.getClassificationId = function(/*dojo.data.item*/ item) {
		var id = item.id;
		return id.rootid != null ? id.rootid : "";
	}

	exports.getCategoryId = function(/*dojo.data.item*/ item) {
		var id = item.id;
		return id.categid != null ? id.categid : "";
	}

	exports.formatId = function(/*dojo.data.item*/ item) {
		if(item.fakeRoot || item.id == null) {
			return "";
		}
		var id = item.id.rootid;
		if(item.id.categid != null && item.id.categid != "") {
			id += ":" + item.id.categid;
		}
		return id;
	}

	exports.hasChildren = function(/*dojo.data.item*/ item) {
		return item.haschildren || item.children;
	}

	exports.isIdEqual = function(/*JSON*/ id, /*JSON*/ id2) {
		return id.rootid == id2.rootid && id.categid == id2.categid;
	}

	exports.toString = function(item) {
		var id = item.id != null ? item.id : item;
		if(lang.isString(id) || id.rootid == null) {
			return id;
		}
		return id.rootid + "/" + (id.categid != null ? id.categid : "");
	}

});
