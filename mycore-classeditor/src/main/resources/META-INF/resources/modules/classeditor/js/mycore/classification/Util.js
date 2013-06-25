/**
 * Contains a bunch of utility functions.
 */
define([
    "exports",
	"dojo/_base/declare" // declare
], function(exports, declare) {

	exports.isClassification = function(/*dojo.data.item*/ item) {
		var id = item.id[0];
		return id.categid == null || id.categid == "";
	}

	exports.getClassificationId = function(/*dojo.data.item*/ item) {
		var id = item.id[0];
		return id.rootid != undefined ? id.rootid : "";
	}

	exports.getCategoryId = function(/*dojo.data.item*/ item) {
		var id = item.id[0];
		return id.categid != undefined ? id.categid : "";
	}

	exports.hasChildren = function(/*dojo.data.item*/ item) {
		return (item.haschildren && item.haschildren[0] == true) || item.children;
	}

	exports.hasChildrenLoaded = function(/*dojo.data.item*/ item) {
		return this.hasChildren(item) && item.children;
	}

	exports.isIdEqual = function(/*JSON*/ id, /*JSON*/ id2) {
		return id.rootid == id2.rootid && id.categid == id2.categid;
	}

});
