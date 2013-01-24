define([
	"dojo/_base/declare", // declare
	"dijit/Dialog",
	"dijit/_Templated",
	"mycore/classification/_SettingsMixin",
	"dojo/text!./templates/ExportDialog.html",
	"dojo/_base/lang", // hitch, clone
	"dojo/on", // on
	"dojo/request/xhr",
	"mycore/common/I18nManager",
	"dijit/form/Button",
	"dijit/form/SimpleTextarea"
], function(declare, Dialog, _Templated, _SettingsMixin, template, lang, on, xhr, i18n) {

return declare("mycore.classification.ExportDialog", [Dialog, _Templated, _SettingsMixin], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "exportDialog",

	classificationId: null,

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    onSettingsReady: function() {
		this.set("title", i18n.getFromCache("component.classeditor.export.dialog"));
		this.closeButtonNode.title = i18n.getFromCache("component.classeditor.dialog.close");
		i18n.resolve(this.exportButton);
		// events
		on(this.exportButton, "click", lang.hitch(this, this.openInNewWindow));
	},

	show: function(/*String*/ classId) {
		if(classId == null) {
			alert(i18n.getFromCache("component.classeditor.export.unkownclass") + ": " + classId);
			return;
		}
		this.inherited(arguments);
		if(classId != this.classificationId) {
			this.textArea.set("value", "");
			xhr(this.settings.resourceURL + "export/" + classId, {
				handleAs: "text"
			}).then(lang.hitch(this, function(data) {
				this.textArea.set("value", data);
			}), lang.hitch(this, function(error) {
				if(error.status == 404) {
					alert(sm.get("component.classeditor.export.unkownclasserror"));
				} else {
					alert(error);
				}
				this.hide();
			}));
		}
		this.classificationId = classId;
	},

	openInNewWindow: function() {
		window.open(this.settings.webAppBaseURL + "rsc/classifications/export/" + this.classificationId);
	}

});
});