/*
 * @package classeditor
 * @description 
 */
var classeditor = classeditor || {};

classeditor.SettingsDialog = function(settings) {
	this.settings = settings;

	this.internalDialog = new dijit.Dialog();
	dojo.addClass(this.internalDialog.domNode, "settingsDialog");

	this.dialogContent = dojo.create("div", {className: "settingsContent"});
	this.internalDialog.set("content", this.dialogContent);

	this.isCreated = false;
	this.classificationImported = false;
	this.dirty = false;

	this.languageEditor;
	this.showIdCheckBox;
	this.importInput;
	this.importOpenButton;
	this.importStartButton;
	this.hiddenFileDialog;
	this.confirmImportDialog;
	
	this.eventHandler = new classeditor.EventHandler(this);
};

( function() {

	function create() {
		var  sm = SimpleI18nManager.getInstance();
		this.internalDialog.set("title", sm.get("component.classeditor.settings.dialog"));
		this.internalDialog.closeButtonNode.title = sm.get("component.classeditor.dialog.close");

		// Import Classification
		this.confirmImportDialog = new classeditor.SettingsDialog.ConfirmImportDialog(this.settings, this);
		this.confirmImportDialog.eventHandler.attach(dojo.hitch(this, handleConfirmImportDialog));

		var importGroup = new dijit.form.Form({
			id: "classImportForm",
			encType: "multipart/form-data",
			action: this.settings.resourceURL + "import",
			className: "group",
			method: "POST"
		});
		dojo.place(importGroup.domNode, this.dialogContent);
		var importLabel = dojo.create("h1", {innerHTML: sm.get("component.classeditor.settings.import")});

		this.importInput = new dijit.form.TextBox({disabled: true});
		this.importOpenButton = new dijit.form.Button({iconClass: "icon16 folder", showLabel: false, onClick: dojo.hitch(this, openClassFileDialog)});
		this.importStartButton = new dijit.form.Button({label: "Import", onClick: dojo.hitch(this, onSubmitClassification), disabled: true});

		// this is kinda hack @see http://stackoverflow.com/questions/210643/in-javascript-can-i-make-a-click-event-fire-programmatically-for-a-file-input
		// create a new input button to open the dialog
		this.hiddenFileDialog = dojo.create("input", {
			type: "file",
			name: "classificationFile",
			style: "position: absolute; z-index: -1; overflow: hidden; width: 1px",
			onchange: dojo.hitch(this, updateClassInput),
			accept: "text/xml"
		});

		dojo.place(importLabel, importGroup.domNode);
		dojo.place(this.importInput.domNode, importGroup.domNode);
		dojo.place(this.importOpenButton.domNode, importGroup.domNode);
		dojo.place(this.importStartButton.domNode, importGroup.domNode);
		dojo.place(this.hiddenFileDialog, importGroup.domNode);

		// Edit Language
		var languageGroup = dojo.create("div", {className: "group"}, this.dialogContent);
		var languageLabel = dojo.create("h1", {innerHTML: sm.get("component.classeditor.settings.language")});
		this.languageEditor = new classeditor.LanguageEditor(this.settings);
		this.languageEditor.create();

		dojo.place(languageLabel, languageGroup);
		dojo.place(this.languageEditor.domNode, languageGroup);

		// Settings
		var settingsGroup = dojo.create("div", {className: "group"}, this.dialogContent);
		var settingsLabel = dojo.create("h1", {innerHTML: sm.get("component.classeditor.settings.additionalSettings")});
		this.showIdCheckBox = new dijit.form.CheckBox({checked: this.settings.showId});
		var showIdLabel = dojo.create("label", {"for":"fieldId", innerHTML: sm.get("component.classeditor.settings.showId")});

		dojo.place(settingsLabel, settingsGroup);
		dojo.place(this.showIdCheckBox.domNode, settingsGroup);
		dojo.place(showIdLabel, settingsGroup);

		this.isCreated = true;
	}

	function open(/*boolean*/ dirty) {
		if(!this.isCreated) {
			dojo.hitch(this, create)();
		}
		this.dirty = dirty;
		this.classificationImported = false;
		this.languageEditor.update(SimpleI18nManager.getInstance().getSupportedLanguages());
		this.internalDialog.show();
	}

	function openClassFileDialog() {
		this.hiddenFileDialog.click();
	}

	function updateClassInput(evt) {
        this.importInput.set("value", this.hiddenFileDialog.value);
        this.importStartButton.set("disabled", this.hiddenFileDialog.value == "");
	}

	function onSubmitClassification() {
		if(this.dirty) {
			this.confirmImportDialog.open();		
		} else {
			dojo.hitch(this, submitClassification)();
		}
	}

	function handleConfirmImportDialog(/*ConfirmImportDialog*/ source, /*JSON*/ args) {
		if(args.type == "saveAndImport") {
			this.eventHandler.notify({"type" : "saveBeforeImport"});
			dojo.hitch(this, submitClassification)();
		} else if(args.type == "noSaveAndImport") {
			dojo.hitch(this, submitClassification)();
		}
	}

	function submitClassification() {
		var sm = SimpleI18nManager.getInstance();
		dojo.io.iframe.send({
			form: dojo.byId("classImportForm"),
			method: "post",
			handleAs: "html",
			handle: dojo.hitch(this, function(data, test) {
				var status = data.body.children[0].value;
				if(status == "200") {
					alert(sm.get("component.classeditor.settings.importsuccessfull"));
					this.classificationImported = true;
					this.importInput.set("value", null);
					this.importStartButton.set("disabled", true);
				} else {
					alert(sm.get("component.classeditor.settings.submiterror"));
				}
			}), error: function(error) {
		    	alert(sm.get("component.classeditor.settings.unknownsubmiterror") + " - " + error);
				console.log(error);
			}
		});
	}

	classeditor.SettingsDialog.prototype.open = open;

})();

classeditor.SettingsDialog.ConfirmImportDialog = function(settings, settingsDialog) {
	this.settings = settings;
	this.settingsDialog = settingsDialog;
	this.created = false;
	this.internalDialog = null;
	this.eventHandler = new classeditor.EventHandler(this);
};

( function() {

	function create() {
		var  sm = SimpleI18nManager.getInstance();
		this.internalDialog = new dijit.Dialog();
		this.internalDialog.set("title", sm.get("component.classeditor.settings.beforeSubmitTitle"));
		dojo.addClass(this.internalDialog.domNode, "confirmImportDialog");
		var contentDiv = dojo.create("div");
		this.internalDialog.set("content", contentDiv);

		dojo.create("p", {innerHTML: sm.get("component.classeditor.settings.beforesubmit")}, contentDiv);
		this.saveAndImportButton = new dijit.form.Button({label: sm.get("component.classeditor.settings.saveAndImport")});
		this.noSaveAndImportButton = new dijit.form.Button({label: sm.get("component.classeditor.settings.noSaveAndImport")});
		this.cancelButton = new dijit.form.Button({label: sm.get("component.classeditor.settings.cancelImport")});
		dojo.place(this.cancelButton.domNode, contentDiv);
		dojo.place(this.saveAndImportButton.domNode, contentDiv);
		dojo.place(this.noSaveAndImportButton.domNode, contentDiv);
		dojo.addClass(this.saveAndImportButton.domNode, "align-right");
		dojo.addClass(this.noSaveAndImportButton.domNode, "align-right");

		dojo.connect(this.saveAndImportButton, "onClick", this, function() {
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "saveAndImport"});
		});
		dojo.connect(this.noSaveAndImportButton, "onClick", this, function() {
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "noSaveAndImport"});
		});
		dojo.connect(this.cancelButton, "onClick", this, function() {
			this.internalDialog.hide();
			this.eventHandler.notify({"type" : "cancel"});
		});
	}

	function open() {
		if(!this.created) {
			dojo.hitch(this, create)();
		}
		this.internalDialog.show();
	}

	classeditor.SettingsDialog.ConfirmImportDialog.prototype.open = open;
})();
