define([
	"dojo/_base/declare", // declare
	"dijit/_Widget",
	"dojo/Evented", // to use on.emit
	"dijit/_Templated",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/CategoryEditorPane.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dojo/dom-style", // style
	"mycore/util/DOJOUtil",
	"mycore/classification/Util",
	"mycore/common/I18nManager",
	"dijit/form/ValidationTextBox",
	"mycore/dijit/Repeater",
	"mycore/dijit/I18nRow"
], function(declare, _Widget, Evented, _Templated, _SettingsMixin, template, on, lang, domConstruct, domStyle, dojoUtil, classUtil, i18n) {

return declare("mycore.classification.CategoryEditorPane", [_Widget, Evented, _Templated, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "categoryEditorPane",

	labelEditor: null,

	currentItem: null,
	
	disabled: false,

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    onSettingsReady: function(args) {
    	this.inherited(arguments);
		this.labelEditor = new mycore.dijit.Repeater({
			row: {
				className: "mycore.dijit.I18nRow",
				args: {
					languages: i18n.getLanguages()
				}
			},
			head: domConstruct.create("tr", {
				innerHTML: "<th>Sprache</th><th>Label</th><th>Beschreibung</th>"
			}),
			minOccurs: 1,
			disabled: true
		});
		domConstruct.place(this.labelEditor.domNode, this.labelEditorCell);
		// show id
		if(this.settings.showId) {
			this.showID();
		} else {
			this.hideID();
		}
		// events
		on(this.labelEditor, "change", lang.hitch(this, function() {
			this.currentItem.labels = this.labelEditor.get("value");
			on.emit(this, "labelChanged", {"item": this.currentItem});
		}));
		on(this.urlEditor, "change", lang.hitch(this, function(newURL) {
			if((newURL == null || newURL == "") && !this.currentItem.uri) {
				return;
			}
			if(!this.currentItem.uri || newURL != this.currentItem.uri[0]) {
				this.currentItem.uri = this.urlEditor.get("value");
				on.emit(this, "urlChanged", {"item": this.currentItem});	
			}
		}));
		on(this.classIdEditor, "change", lang.hitch(this, this._handleIdChanged));
		on(this.categIdEditor, "change", lang.hitch(this, this._handleIdChanged));
    },

    update: function(item) {
		if(item != null) {
			this.currentItem = item;
		}
		// label editor
		this.labelEditor.set("value", this.currentItem.labels, false);
		this.updateLanguages();
		// url editor
		this.urlEditor.set("value", this.currentItem.uri != undefined ? this.currentItem.uri : null, false);
		// get id
		var classId = classUtil.getClassificationId(this.currentItem);
		var categId = classUtil.getCategoryId(this.currentItem);
		// set classification and category id
		this.classIdEditor.set("value", classId, false);
		this.categIdEditor.set("value", categId, false);
		// set editable
		var isClass = classUtil.isClassification(this.currentItem);
		var hasChilds = classUtil.hasChildren(this.currentItem);
		var isAdded = this.currentItem.added;
		this.classIdEditor.set("disabled", this.disabled || !(isAdded && isClass && !hasChilds));
		this.categIdEditor.set("disabled", this.disabled || !(isAdded && !isClass));
    },

    updateLanguages: function() {
    	var languages = i18n.getLanguages();
    	this.labelEditor.row.args.languages = languages;
    	this.labelEditor.broadcast({id: "resetLang", languages: languages});
    },

    _setDisabledAttr: function(/*boolean*/ disabled) {
    	this.disabled = disabled;
		this.labelEditor.set("disabled", disabled);
		this.urlEditor.set("disabled", disabled);
		this.classIdEditor.set("disabled", disabled);
		this.categIdEditor.set("disabled", disabled);
    },

	showID: function() {
		domStyle.set(this.classIdRow, "display", "table-row");
		domStyle.set(this.categIdRow, "display", "table-row");
		if(this.currentItem != null) {
			this.update(this.currentItem);
		}
	},

    hideID: function() {
		domStyle.set(this.classIdRow, "display", "none");
		domStyle.set(this.categIdRow, "display", "none");
	},

	_handleIdChanged: function() {
		var newClassId = this.classIdEditor.get("value");
		var newCategId = this.categIdEditor.get("value");
		var isClass = classUtil.isClassification(this.currentItem);
		// check if editors are valid
		if(!this.classIdEditor.isValid() || (!isClass && !this.categIdEditor.isValid())) {
			return;
		}
		// check if something has changed
		var classID = classUtil.getClassificationId(this.currentItem);
		var categID = classUtil.getCategoryId(this.currentItem);
		if(classID != newClassId || categID != newCategId) {
			this.currentItem.id.rootid = newClassId;
			this.currentItem.id.categid = newCategId;
			on.emit(this, "idChanged", {
				"item": this.currentItem,
				"oldID": {
					rootid: classID,
					categid: categID
				}
			});
		}
	}

});
});
