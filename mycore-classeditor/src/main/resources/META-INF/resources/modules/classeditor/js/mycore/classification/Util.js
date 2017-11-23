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
