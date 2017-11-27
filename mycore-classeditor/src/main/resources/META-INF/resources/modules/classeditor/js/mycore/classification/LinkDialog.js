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
	"dijit/Dialog",
	"dijit/_Templated",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/LinkDialog.html",
	"dojo/_base/lang", // hitch, clone
	"dojo/on", // on
	"dojo/request/xhr",
	"dojo/dom-construct",
	"mycore/common/I18nManager",
	"mycore/classification/Util",
	"dijit/form/Button",
	"dijit/layout/ContentPane",
	"dijit/layout/BorderContainer",
	"dijit/Toolbar"
], function(declare, Dialog, _Templated, _SettingsMixin, template, lang, on, xhr, domConstruct, i18n, classUtil) {

return declare("mycore.classification.LinkDialog", [Dialog, _Templated, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "linkDialog",

	item: null,
	
	data: null,
	
	start: 0,

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    onSettingsReady: function() {
		this.set("title", i18n.getFromCache("component.classeditor.linkdialog.title"));
		this.closeButtonNode.title = i18n.getFromCache("component.classeditor.linkdialog.close");
		
		on(this.nextButton, "click", lang.hitch(this, this.onNextDocuments));
		on(this.allButton, "click", lang.hitch(this, this.onAllDocuments));
	},

	show: function(/*dojo.data.item*/ item) {
		if(item == null) {
			alert(i18n.getFromCache("component.classeditor.linkdialog.unkownitem"));
			return;
		}
		this.inherited(arguments);
		if(this.item == null || !classUtil.isIdEqual(item.id, this.item.id)) {
			this.start = 0;
			this.data = null;
			this.item = item;
			domConstruct.empty(this.linkTable);
			this.updateToolbar();
			this.onNextDocuments();
		}
	},

	printDocuments : function() {
		if (this.data.numFound == 0) {
			this.infoPane.set("content", "Keine Treffer");
			return;
		} else {
			var docs = this.data.docs;
			this.infoPane.set("content", "Dokumente: " + this.start + "/" + this.data.numFound);
			var tr = null;
			for ( var i = 0; i < docs.length; i++) {
				if(i % 2 == 0) {
					tr = domConstruct.create("tr", {}, this.linkTable);
				}
				domConstruct.place("<td><a target='_blank' href='" + this.settings.webAppBaseURL + "receive/" + docs[i] + "'>" + docs[i] + "</a></td>", tr); 
			}
		}
		this.updateToolbar();
		this.borderContainer.resize();
	},

	onNextDocuments : function() {
		this.fetchDocuments(this.item, this.start);
	},

	onAllDocuments : function() {
		this.start = 0;
		domConstruct.empty(this.linkTable);
		this.fetchDocuments(this.item, this.start, this.data.numFound);
	},

	fetchDocuments : function(item, start, rows) {
		var url = this.settings.resourceURL + "link/" + classUtil.formatId(item) + "?start=" + start;
		if(rows != null) {
			url += "&rows=" + rows;
		}
		xhr(url, {
			handleAs: "json"
		}).then(lang.hitch(this, function(data) {
			this.data = data;
			this.start += data.docs.length;
			this.printDocuments();
		}), lang.hitch(this, function(error) {
			alert(error);
			this.hide();
		}));
	},

	updateToolbar : function() {
		var disabled = this.data == null || this.start == 0 || this.start == this.data.numFound;
		this.nextButton.set("disabled", disabled);
		this.allButton.set("disabled", disabled);
	}

});
});
