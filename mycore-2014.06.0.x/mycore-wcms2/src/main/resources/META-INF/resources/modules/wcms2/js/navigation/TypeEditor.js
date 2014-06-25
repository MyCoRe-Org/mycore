/*
 * @package wcms.navigation
 * @description model data for internal tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

/**
 * Editor for internal or external webpages. Internal pages could be edited with a content dialog
 * 
 * @see wcms.navigation.EditContentDialog.
 */
wcms.navigation.TypeEditor = function() {
	this.domNode = dojo.create("div");
	this.disabled = false;

	this.Type = {
		intern : "intern",
		extern : "extern"
	};

	this.typeSelect = new dijit.form.Select();
	// dojo.addClass(this.typeSelect.focusNode, "smallComponent");

	// components
	this.extraDiv = dojo.create("div");
	this.moveContentButton = null;
	this.editContent = null;
	this.reloadContentButton = null;
	this.editContentDialog = new wcms.navigation.EditContentDialog();
	this.moveContentDialog = new wcms.navigation.MoveContentDialog();
	this.hrefTextBox = null;

	// current item
	this.itemId = null;

	// wcms.navigation.NavigationContent
	this.content = null;

	// events
	this.eventHandler = new wcms.common.EventHandler(this);
};

(function() {

	var editorContentText = "component.wcms.navigation.typeEditor.editContent";
	var moveContentText = "component.wcms.navigation.typeEditor.moveContent";

	var internText = "component.wcms.navigation.typeEditor.intern";
	var externText = "component.wcms.navigation.typeEditor.extern";

	function create(/* wcms.navigation.NavigationContent */content) {
		this.content = content;

		// href
		var typeEditorInstance = this;
		this.hrefTextBox = new dijit.form.ValidationTextBox({
			required : true,
			intermediateChanges : true,
			regExp : "(.+)\.xml",
			i18nMissingMessage : "component.wcms.navigation.typeEditor.hrefRequired",
			i18nInvalidMessage : "component.wcms.navigation.typeEditor.invalidFile",
			validator : function(value, constraints) {
				if (value == null || value == "")
					return false;
				if (typeEditorInstance.typeSelect.get("value") == typeEditorInstance.Type.extern)
					return true;
				return dijit.form.ValidationTextBox.prototype.validator.call(this, value, constraints);
			},
		});

		// add options
		this.typeSelect.addOption({
			value : this.Type.intern,
			i18n : internText
		});
		this.typeSelect.addOption({
			value : this.Type.extern,
			i18n : externText
		});

		// content
		this.moveContentButton = new dijit.form.Button({
			i18n : moveContentText
		});
		this.editContent = new dijit.form.Button({
			i18n : editorContentText
		});
		this.reloadContentButton = new dijit.form.Button({
			showLabel : false
		});
		// this.updateType(this.Type.intern);

		// append all childs to the nodes
		this.domNode.appendChild(this.typeSelect.domNode);
		this.domNode.appendChild(this.hrefTextBox.domNode);
		this.domNode.appendChild(this.extraDiv);
		this.extraDiv.appendChild(this.moveContentButton.domNode);
		this.extraDiv.appendChild(this.editContent.domNode);
		this.extraDiv.appendChild(this.reloadContentButton.domNode);

		// events
		dojo.connect(this.moveContentButton, "onClick", this, onMoveContent);
		dojo.connect(this.editContent, "onClick", this, onEditContent);
		dojo.connect(this.reloadContentButton, "onClick", this, onReloadContent);
		dojo.connect(this.hrefTextBox, "onChange", this, function(/* String */newValue) {
			this.eventHandler.notify({
				"type" : "hrefChanged",
				"value" : newValue
			});
		});
		dojo.connect(this.typeSelect, "onChange", this, this.updateType);

		this.editContentDialog.eventHandler.attach(dojo.hitch(this, function(/* EditContentDialog */source, /* Json */args) {
			if (args.type == "okButtonClicked" && !deepEquals(source.oldWebpageContent, source.webpageContent)) {
				this.eventHandler.notify({
					"type" : "contentChanged",
					"webpageContent" : source.webpageContent
				});
			}
		}));
		this.moveContentDialog.eventHandler.attach(dojo.hitch(this, function(/* MoveContentDialog */source, /* Json */args) {
			var from = this.hrefTextBox.get("value");
			var to = args.href;
			if (args.type == "moveButtonClicked" && to != from) {
				var xhrArgs = {
					url : wcms.settings.wcmsURL + "/navigation/move?from=" + from + "&to=" + to,
					load : dojo.hitch(this, function() {
						this.eventHandler.notify({
							"type" : "contentMoved",
							"from" : from,
							"to" : to
						});
					}),
					error : function(error, xhr) {
						console.log(error);
						alert("An error occur. Cannot move file from " + from + " to " + to);
					}
				};
				dojo.xhrPost(xhrArgs);
			}
		}));
	}

	function update(/* JSON */item) {
		this.itemId = item.wcmsId;
		this.typeSelect.set("value", item.type);
		if (item.href != undefined) {
			this.hrefTextBox.set("value", item.href);
		} else if(item.hrefStartingPage != undefined) {
			this.hrefTextBox.set("value", item.hrefStartingPage);
		} else {
			this.hrefTextBox.set("value", null);
		}
		dojo.hitch(this, onUpdateType)();
	}

	function reset() {
		this.typeSelect.set("value", null);
		this.editContent.set("value", null);
		this.hrefTextBox.set("value", null);
	}

	function setDisabled(/* boolean */value) {
		this.disabled = value;
		this.typeSelect.set("disabled", this.disabled);
		this.editContent.set("disabled", this.disabled);
		this.moveContentButton.set("disabled", this.disabled);
		this.hrefTextBox.set("disabled", this.disabled);
	}

	function updateType(/* String */type) {
		dojo.hitch(this, onUpdateType)();
		this.eventHandler.notify({
			"type" : "typeChanged",
			"value" : type
		});
	}

	function onUpdateType() {
		var item = this.content.getItem(this.itemId);
		var type = this.typeSelect.get("value");
		var showContentButton = (item != null && type != this.Type.extern && this.hrefTextBox.isValid());
		this.editContent.set("disabled", !showContentButton);
		this.moveContentButton.set("disabled", !showContentButton);
		this.hrefTextBox.set("disabled", showContentButton);
		if (showContentButton && item.content != null) {
			this.reloadContentButton.set("disabled", false);
			this.reloadContentButton.set("iconClass", "icon16 reloadIcon16");
		} else {
			this.reloadContentButton.set("disabled", true);
			this.reloadContentButton.set("iconClass", "icon16 reloadDisabledIcon16");
		}
	}

	function updateLang() {
		I18nManager.getInstance().updateI18nButton(this.moveContentButton);
		I18nManager.getInstance().updateI18nButton(this.editContent);
		I18nManager.getInstance().updateI18nSelect(this.typeSelect);
		I18nManager.getInstance().updateI18nValidationTextBox(this.hrefTextBox);
		this.editContentDialog.updateLang();
	}

	function onMoveContent() {
		var href = this.hrefTextBox.get("value");
		if (href == null || href == "") {
			console.log("Unexpected error: href is null or empty");
			return;
		}
		this.moveContentDialog.show(href);
	}

	function onEditContent() {
		// get content
		var href = this.hrefTextBox.get("value");
		if (href == null || href == "") {
			console.log("Unexpected error: href is null or empty");
			return;
		}
		this.content.getWebpageContent(this.itemId, dojo.hitch(this, function(content, item) {
			var href = this.hrefTextBox.get("value");
			this.editContentDialog.show(content, href);
		}), dojo.hitch(this, handleError));
	}

	function onReloadContent() {
		var reloadContentDialog = new wcms.gui.SimpleDialog("yesNo", "component.wcms.navigation.typeEditor.reloadContentCaption",
				"component.wcms.navigation.typeEditor.reloadContentLabel");
		reloadContentDialog.eventHandler.attach(dojo.hitch(this, function(/* SimpleDialog */source, /* JSON */args) {
			if (args.type == "yesButtonClicked") {
				var oldItem = this.content.getItem(this.itemId);
				// reload content
				this.content.reloadWebpageContent(this.itemId, dojo.hitch(this, function(content, item) {
					// fire event
					if (!deepEquals(oldItem.content, item.content)) {
						this.eventHandler.notify({
							"type" : "contentChanged",
							"webpageContent" : item.content
						});
					}
					// change icon
					this.reloadContentButton.set("iconClass", "icon16 tick16");
					this.reloadContentButton.set("disabled", true);
				}), dojo.hitch(this, handleError));
			}
		}));
		reloadContentDialog.show();
	}

	/**
	 * Handles all error.
	 * 
	 * @param error
	 *            json object looks like: { type: "error", errorType: "xxx" }
	 * @param item
	 */
	function handleError(error, xhr, item) {
		if (xhr.xhr.status == 401) {
			wcms.util.ErrorUtils.show("unauthorized");
		} else {
			var response = dojo.fromJson(xhr.xhr.response);
			if (response.type != null && response.type != "") {
				wcms.util.ErrorUtils.show(response.type);
			} else {
				wcms.util.ErrorUtils.show(error.errorType);
			}
		}
	}

	wcms.navigation.TypeEditor.prototype.create = create;
	wcms.navigation.TypeEditor.prototype.update = update;
	wcms.navigation.TypeEditor.prototype.reset = reset;
	wcms.navigation.TypeEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.TypeEditor.prototype.updateType = updateType;
	wcms.navigation.TypeEditor.prototype.updateLang = updateLang;
})();
