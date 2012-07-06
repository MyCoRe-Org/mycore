/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * 
 */
classeditor.LabelEditor = function(settings) {
	this.settings = settings;
	this.editable = settings.editable == undefined ? true : settings.editable;
	this.domNode = dojo.create("table", {className: "labelEditorTable"});

	this.addRowButton = new dijit.form.Button({
		iconClass: "icon12 addIcon12",
		showLabel: false,
		onClick: dojo.hitch(this, function() {
			this.addRow("de", "", "");
		})
	});
	this.rowList = [];

	this.eventHandler = new classeditor.EventHandler(this);
};

( function() {

	function create() {
		var sm = SimpleI18nManager.getInstance();
		// create table header
		var tr = dojo.create("tr", {}, this.domNode);
		dojo.create("th", {innerHTML: sm.get("component.classeditor.language")}, tr);
		dojo.create("th", {innerHTML: sm.get("component.classeditor.text")}, tr);
		dojo.create("th", {innerHTML: sm.get("component.classeditor.description")}, tr);
		dojo.create("th", {}, tr);

		// add add-button
		tr = dojo.create("tr", {}, this.domNode);
		dojo.create("td", {}, tr);
		dojo.create("td", {}, tr);
		dojo.create("td", {}, tr);
		var addRowTD = dojo.create("td", {className: "button addRow"}, tr);
		addRowTD.appendChild(this.addRowButton.domNode);

		// add one row as default
		this.addRow("de", "", "");
	}

	function update(/*Array*/ labels) {
		// add and update rows
		var internalCount = 0;
		for (var internalCount = 0; internalCount < labels.length; internalCount++) {
			var label = labels[internalCount];
			if(internalCount < this.rowList.length) {
				var row = this.rowList[internalCount];
				row.update(label.lang, label.text, label.description);
			} else {
				this.addRow(label.lang, label.text, label.description);
			}
		}
		// remove rows
		while(this.rowList.length > internalCount) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function getValues() {
		var labels = [];
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			var lang = row.langSelect.get("value");
			var text = row.textBox.get("value");
			if(lang == null || lang == "" || text == null || text == "")
				continue;
			var desc = row.descBox.get("value");
			var label = {
				lang: lang,
				text: text
			};
			if(desc != null && desc != "") {
				label.description = desc;
			}
			labels.push(label);
		}
		return labels;
	}

	function reset() {
		while(this.rowList.length > 0) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function focus() {
		if(this.rowList.length > 0) {
			this.rowList[0].focus();
		}
	}

	function addRow(/*String*/ lang, /*String*/ text, /*String*/ desc) {
		var removeable = this.rowList.length > 0;
		var row = new classeditor.LabelEditor.Row({
			lang: lang,
			text: text,
			desc: desc,
			removeable: removeable,
			labelEditor: this,
			editable: this.editable
		});
		row.create();
		dojo.place(row.domNode, this.domNode, this.domNode.childNodes.length - 1);
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
		this.editable = editable == undefined ? true : editable;
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
	 * Updates the language select boxes with all valid languages from SimpleI18nMananger.
	 */
	function updateLanguages() {
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			row.updateLanguage();
		}
	}

	/**
	 * Fires a categoryChanged event. Possible types are:
	 * -lang
	 * -text
	 * -description
	 */
	function categoryChanged(/*Row*/ row, /*String*/ editType, /*String*/ value) {
		this.eventHandler.notify({"type" : "categoryChanged", "row": row, "editType" : editType, "value": value});
	}

	classeditor.LabelEditor.prototype.create = create;
	classeditor.LabelEditor.prototype.addRow = addRow;
	classeditor.LabelEditor.prototype.removeRow = removeRow;
	classeditor.LabelEditor.prototype.update = update;
	classeditor.LabelEditor.prototype.getValues = getValues;
	classeditor.LabelEditor.prototype.reset = reset;
	classeditor.LabelEditor.prototype.focus = focus;
	classeditor.LabelEditor.prototype.setEditable = setEditable;
	classeditor.LabelEditor.prototype.categoryChanged = categoryChanged;
	classeditor.LabelEditor.prototype.updateLanguages = updateLanguages;

})();

classeditor.LabelEditor.Row = function Row(args) {
	this.domNode = dojo.create("tr");

	this.lang = args.lang;
	this.text = args.text;
	this.desc = args.desc;
	this.removeable = args.removeable;
	this.labelEditor = args.labelEditor;
	this.editable = args.editable;

	this.langSelect = null;
	this.textBox = null;
	this.descBox = null;
	this.removeButton = null;
};

( function() {

	function create() {
		this.langSelect = new dijit.form.Select({
			onChange: dojo.hitch(this, onLanguageChange)
		});
		this.updateLanguage();

		this.textBox = new dijit.form.TextBox({
			value: this.text,
			intermediateChanges: true,
			selectOnClick: true,
			onChange: dojo.hitch(this, function(/*String*/ newText) {
				this.labelEditor.categoryChanged(this, "text", newText);
			})
		});

		this.descBox = new dijit.form.TextBox({
			value: this.desc,
			intermediateChanges: true,
			selectOnClick: true,
			onChange: dojo.hitch(this, function(/*String*/ newDesc) {
				this.labelEditor.categoryChanged(this, "description", newDesc);
			})
		});

		var langTD = dojo.create("td", {className: "input language"}, this.domNode);
		var textTD = dojo.create("td", {className: "input text"}, this.domNode);
		var descTD = dojo.create("td", {className: "input description"}, this.domNode);
		langTD.appendChild(this.langSelect.domNode);
		textTD.appendChild(this.textBox.domNode);
		descTD.appendChild(this.descBox.domNode);

		if(this.removeable) {
			this.removeButton = new dijit.form.Button({
				iconClass: "icon12 removeIcon12",
				showLabel: false,
				onClick: dojo.hitch(this, function() {
					this.labelEditor.removeRow(this);
				})
			});
			var rmBtTD = dojo.create("td", {className: "button removeRow"}, this.domNode);
			rmBtTD.appendChild(this.removeButton.domNode);
		}
		this.setEditable(this.editable);
	}

	function updateLanguage() {
		var im = SimpleI18nManager.getInstance();
		var languages = im.getSupportedLanguages();
		this.langSelect.removeOption(this.langSelect.getOptions());
		for(var j = 0; j < languages.length; j++) {
			this.langSelect.addOption({value: languages[j], label: languages[j]});
		}
		if(!this.containsLanguage(this.lang)) {
			this.langSelect.addOption({value: this.lang, label: this.lang});
		}
		this.langSelect.set("value", this.lang);
	}

	function containsLanguage(/*String*/ lang) {
		var options = this.langSelect.getOptions();
		for(var i = 0; i < options.length; i++) {
			if(options[i].value == lang) {
				return true;
			}
		}
		return false;
	}

	function onLanguageChange(/*String*/ newLang) {
		this.labelEditor.categoryChanged(this, "lang", newLang);
	}
	
	function destroy() {
		// destroy widgets
		this.langSelect.destroy();
		this.textBox.destroy();
		this.descBox.destroy();
		this.removeButton.destroy();
		// remove from dom
		dojo.destroy(this.domNode);
	}

	function update(/*String*/ lang, /*String*/ text, /*String*/ desc) {
		this.lang = lang;
		this.text = text;
		this.desc = desc;
		
		this.updateLanguage();
		this.textBox.set("value", text == undefined ? null : text);
		this.descBox.set("value", desc == undefined ? null : desc);
	}

	function focus() {
		if(this.textBox != null) {
			this.textBox.focusNode.select();
		}
	}

	function setEditable(/*boolean*/ editable) {
		this.langSelect.set("disabled", !editable);
		this.textBox.set("disabled", !editable);
		this.descBox.set("disabled", !editable);
		if(this.removeButton) {
			this.removeButton.set("disabled", !editable);
		}
	}

	classeditor.LabelEditor.Row.prototype.create = create;
	classeditor.LabelEditor.Row.prototype.update = update;
	classeditor.LabelEditor.Row.prototype.destroy = destroy;
	classeditor.LabelEditor.Row.prototype.setEditable = setEditable;
	classeditor.LabelEditor.Row.prototype.updateLanguage = updateLanguage;
	classeditor.LabelEditor.Row.prototype.containsLanguage = containsLanguage;
	classeditor.LabelEditor.Row.prototype.focus = focus;
})();