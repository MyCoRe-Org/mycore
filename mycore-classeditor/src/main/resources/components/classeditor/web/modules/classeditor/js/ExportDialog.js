/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * 
 */
classeditor.ExportDialog = function(settings) {
	this.internalDialog = new dijit.Dialog();
	dojo.addClass(this.internalDialog.domNode, "exportDialog");

	this.dialogContent = dojo.create("div", {className: "content"});
	this.internalDialog.set("content", this.dialogContent);
	
	this.created = false;
	this.classificationId = null;
	this.textArea;
	this.openButton;
	this.settings = settings;
};

( function() {

	function create() {
		var sm = SimpleI18nManager.getInstance();
		this.internalDialog.set("title", sm.get("component.classeditor.export.dialog"));
		this.internalDialog.closeButtonNode.title = sm.get("component.classeditor.dialog.close");

		this.textArea = new dijit.form.SimpleTextarea({
			rows: 50
		});
		dojo.addClass(this.textArea.domNode, "classTextArea");
		dojo.place(this.textArea.domNode, this.dialogContent);
		
		this.openButton = new dijit.form.Button({
			label: sm.get("component.classeditor.export.newwindow"),
			onClick: dojo.hitch(this, openInNewWindow),
		});
		dojo.addClass(this.openButton.domNode, "exportButton");
		dojo.place(this.openButton.domNode, this.dialogContent);
		this.created = true;
	}

	function open(/*String*/ classId) {
		var sm = SimpleI18nManager.getInstance();
		if(classId == null) {
			alert(sm.get("component.classeditor.export.unkownclass") + ": " + classId);
			return;
		}
		if(!this.created) {
			dojo.hitch(this, create)();
		}
		this.internalDialog.show();
		if(classId != this.classificationId) {
			this.textArea.set("value", "");
			dojo.xhrGet({
				url: this.settings.resourceURL + "export/" + classId,
				handleAs: "text",
				load: dojo.hitch(this, function(data) {
					this.textArea.set("value", data);
				}),
				error: dojo.hitch(this, function(error) {
					if(error.status == 404) {
						alert(sm.get("component.classeditor.export.unkownclasserror"));
					} else {
						alert(error);
					}
					this.internalDialog.hide();
				})
			});
		}
		this.classificationId = classId;
	}

	function openInNewWindow() {
		window.open(this.settings.webAppBaseURL + "servlets/MCRClassExportServlet?id=" + this.classificationId);
	}

	classeditor.ExportDialog.prototype.open = open;

})();