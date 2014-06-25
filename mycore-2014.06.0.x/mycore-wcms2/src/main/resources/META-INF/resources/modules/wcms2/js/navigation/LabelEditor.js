/*
 * @package wcms.navigation
 * @description component that can be used to create or manipulate i18n text.
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.LabelEditor = function() {
	var instance = this;
	this.domNode = dojo.create("div");
	this.disabled = false;
	this.addRowButton = new dijit.form.Button({
		iconClass: "icon12 addIcon12",
		showLabel: false,
		onClick: function() {
			instance.addRow("", "");
		}
	});
	this.domNode.appendChild(this.addRowButton.domNode);
	this.rowList = new Array();

	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {
	function update(/*Array[lang]*/ valueArray) {
		// add and update rows
		var internalCount = 0;
		for (var lang in valueArray) {
			if(internalCount < this.rowList.length) {
				var row = this.rowList[internalCount];
				row.update(lang, valueArray[lang]);
			} else {
				var addRowFunc = dojo.hitch(this, addRow);
				addRowFunc(lang, valueArray[lang]);
			}
			internalCount++;
		}
		// remove rows
		while(this.rowList.length > internalCount) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function getValues() {
		var returnObject = {};
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			var lang = row.langBox.value;
			if(lang == null || lang == "")
				continue;
			var label = row.labelBox.value;
			eval("returnObject." + lang + "=\"" + label + "\";");
		}
		return returnObject;
	}

	function reset() {
		while(this.rowList.length > 0) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function addRow(/*String*/ lang, /*String*/ label) {
		var row = new wcms.navigation.Row(lang, label, this);
		dojo.place(row.domNode, this.addRowButton.domNode, "before");
		this.rowList.push(row);
		this.eventHandler.notify({"type" : "rowAdded", "row": row});
		return row;
	}

	function removeRow(/*Row*/ row) {
		var getRowNumberFunc = dojo.hitch(this, getRowNumber);
		var rowNumber = getRowNumberFunc(row); 
		var rest1 = this.rowList.slice(0, rowNumber);
		var rest2 = this.rowList.slice(rowNumber + 1);
		this.rowList = rest1.concat(rest2);
		row.destroy();
		row = null;
		this.eventHandler.notify({"type" : "rowRemoved", "row": row});
	}

	function setDisabled(/*Boolean*/ disabled) {
		this.disabled = disabled;
		this.addRowButton.set("disabled", disabled);
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			row.setDisabled(disabled);
		}
	}

	function getRowNumber(/*Row*/ row) {
		for(var i = 0; i < this.rowList.length; i++)
			if(this.rowList[i] == row)
				return i;
		return -1;
	}

	function textChanged(/*Row*/ row, /*String*/ langOrLabel, /*String*/ newText) {
		this.eventHandler.notify({"type" : "rowChanged", "row": row, "langOrLabel" : langOrLabel, "text": newText});
	}

	wcms.navigation.LabelEditor.prototype.addRow = addRow;
	wcms.navigation.LabelEditor.prototype.removeRow = removeRow;
	wcms.navigation.LabelEditor.prototype.update = update;
	wcms.navigation.LabelEditor.prototype.getValues = getValues;
	wcms.navigation.LabelEditor.prototype.reset = reset;
	wcms.navigation.LabelEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.LabelEditor.prototype.textChanged = textChanged;
	
})();

wcms.navigation.Row = function Row(/*String*/ lang, /*String*/ label, /*wcms.navigation.LabelEditor*/ labelEditor) {
	var instance = this;
	this.domNode = dojo.create("div");
	this.langBox = new dijit.form.TextBox({
		value: lang,
		intermediateChanges: true,
		onChange: function(/*String*/ newText) {
			this.set("value", newText);
			labelEditor.textChanged(instance, "lang", newText);
		}
	});
	this.langBox.set("class", "i18nLang");

	this.labelBox = new dijit.form.TextBox({
		value: label,
		intermediateChanges: true,
		onChange: function(/*String*/ newText) {
			this.set("value", newText);
			labelEditor.textChanged(instance, "label", newText);
		}
	});
	this.labelBox.set("class", "i18nLabel");

	this.removeButton = new dijit.form.Button({
		iconClass: "icon12 removeIcon12",
		showLabel: false,
		onClick: function() {
			labelEditor.removeRow(instance);
		}
	});
	
	this.domNode.appendChild(this.langBox.domNode);
	this.domNode.appendChild(this.labelBox.domNode);
	this.domNode.appendChild(this.removeButton.domNode);
};

( function() {

	function destroy() {
		// destroy widgets
		this.langBox.destroy();
		this.labelBox.destroy();
		this.removeButton.destroy();
		// remove from dom
		dojo.destroy(this.domNode);
	}

	function update(/*String*/ lang, /*String*/ label) {
		this.langBox.set("value", lang);
		this.labelBox.set("value", label);
	}

	function setDisabled(/*boolean*/ value) {
		this.langBox.set("disabled", value);
		this.labelBox.set("disabled", value);
		this.removeButton.set("disabled", value);
	}
	
	wcms.navigation.Row.prototype.update = update;
	wcms.navigation.Row.prototype.destroy = destroy;
	wcms.navigation.Row.prototype.setDisabled = setDisabled;
})();