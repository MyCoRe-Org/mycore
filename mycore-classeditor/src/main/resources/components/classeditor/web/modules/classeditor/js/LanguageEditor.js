/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * 
 */
classeditor.LanguageEditor = function(settings) {
	var instance = this;
	this.domNode = dojo.create("div");
	this.table = dojo.create("table", {className: "languageEditor"});

	this.editable = settings.editable;
	this.addRowButton = new dijit.form.Button({
		iconClass: "icon12 addIcon12",
		showLabel: false,
		onClick: function() {
			instance.addRow("");
		}
	});
	this.rowList = new Array();

	this.eventHandler = new classeditor.EventHandler(this);
};

( function() {

	function create() {
		this.domNode.appendChild(this.table);
		// add add-button
		tr = dojo.create("tr", {}, this.table);
		dojo.create("td", {}, tr);
		var addRowTD = dojo.create("td", {className: "button addRow"}, tr);
		addRowTD.appendChild(this.addRowButton.domNode);
		// add one row as default
		this.addRow("");
	}

	function update(/*Array*/ languages) {
		// add and update rows
		var internalCount = 0;
		for (var internalCount = 0; internalCount < languages.length; internalCount++) {
			var language = languages[internalCount];
			if(internalCount < this.rowList.length) {
				var row = this.rowList[internalCount];
				row.update(language);
			} else {
				this.addRow(language);
			}
		}
		// remove rows
		while(this.rowList.length > internalCount) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function getValues() {
		var languages = [];
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			var language = row.languageBox.get("value");
			if(language == null || language == "")
				continue;
			languages.push(language);
		}
		return languages;
	}

	function reset() {
		while(this.rowList.length > 0) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function addRow(/*String*/ language) {
		var removeable = this.rowList.length > 0;
		var row = new classeditor.LanguageEditor.Row(language, removeable, this);
		row.create();
		dojo.place(row.domNode, this.table, this.table.childNodes.length - 1);
		this.rowList.push(row);
		this.eventHandler.notify({"type" : "categoryAdded", "row": row});
		return row;
	}

	function removeRow(/*Row*/ row) {
		var rowNumber = dojo.hitch(this, getRowNumber)(row);
		var rest1 = this.rowList.slice(0, rowNumber);
		var rest2 = this.rowList.slice(rowNumber + 1);
		this.rowList = rest1.concat(rest2);
		row.destroy();
		row = null;
		this.eventHandler.notify({"type" : "categoryRemoved", "row": row});
	}

	function setEditable(/*Boolean*/ editable) {
		this.editable = editable;
		this.addRowButton.set("disabled", !editable);
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			row.setEditable(editable);
		}
	}

	function getRowNumber(/*Row*/ row) {
		for(var i = 0; i < this.rowList.length; i++)
			if(this.rowList[i] == row)
				return i;
		return -1;
	}

	/**
	 * Fires a languageChanged event
	 */
	function languageChanged(/*Row*/ row, /*String*/ value) {
		this.eventHandler.notify({"type" : "languageChanged", "row": row, "value": value});
	}

	classeditor.LanguageEditor.prototype.create = create;
	classeditor.LanguageEditor.prototype.addRow = addRow;
	classeditor.LanguageEditor.prototype.removeRow = removeRow;
	classeditor.LanguageEditor.prototype.update = update;
	classeditor.LanguageEditor.prototype.getValues = getValues;
	classeditor.LanguageEditor.prototype.reset = reset;
	classeditor.LanguageEditor.prototype.setEditable = setEditable;
	classeditor.LanguageEditor.prototype.languageChanged = languageChanged;

})();

classeditor.LanguageEditor.Row = function Row(/*String*/ language, /*boolean*/ removeable, /*classeditor.LanguageEditor*/ languageEditor) {
	this.domNode = dojo.create("tr");

	this.language = language;
	this.removeable = removeable;
	this.languageEditor = languageEditor;

	this.languageBox = null;
	this.removeButton = null;
};

( function() {

	function create() {
		this.languageBox = new dijit.form.TextBox({
			value: this.language,
			intermediateChanges: true,
			onChange: dojo.hitch(this, function(/*String*/ newLanguage) {
				this.languageEditor.languageChanged(this, newLanguage);
			})
		});

		var languageTD = dojo.create("td", {className: "input language"}, this.domNode);
		languageTD.appendChild(this.languageBox.domNode);

		if(this.removeable) {
			this.removeButton = new dijit.form.Button({
				iconClass: "icon12 removeIcon12",
				showLabel: false,
				onClick: dojo.hitch(this, function() {
					this.languageEditor.removeRow(this);
				})
			});
			var rmBtTD = dojo.create("td", {className: "button removeRow"}, this.domNode);
			rmBtTD.appendChild(this.removeButton.domNode);
		}
	}

	function destroy() {
		// destroy widgets
		this.languageBox.destroy();
		this.removeButton.destroy();
		// remove from dom
		dojo.destroy(this.domNode);
	}

	function update(/*String*/ language) {
		this.languageBox.set("value", language == undefined ? null : language);
	}

	function setEditable(/*boolean*/ editable) {
		this.languageBox.set("disabled", !editable);
		if(this.removeButton)
			this.removeButton.set("disabled", !editable);
	}

	classeditor.LanguageEditor.Row.prototype.create = create;
	classeditor.LanguageEditor.Row.prototype.update = update;
	classeditor.LanguageEditor.Row.prototype.destroy = destroy;
	classeditor.LanguageEditor.Row.prototype.setEditable = setEditable;
})();