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

/*
 * @package wcms.navigation
 * @description component that can be used to create or manipulate i18n text.
 */
var wcms = wcms || {};
wcms.gui = wcms.gui || {};

/**
 * This is an simple editor for javascript arrays.
 */
wcms.gui.SimpleArrayEditor = function() {
	var instance = this;
	this.domNode = dojo.create("div");
	this.disabled = false;
	this.addRowButton = new dijit.form.Button({
		iconClass: "icon12 addIcon12",
		showLabel: false,
		onClick: function() {
			instance.addRow("");
		}
	});
	this.domNode.appendChild(this.addRowButton.domNode);
	this.rowList = new Array();

	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {
	function update(/*Array*/ valueArray) {
		// add and update rows
		var internalCount = 0;
		for (var i = 0; i < valueArray.length; i++) {
			if(internalCount < this.rowList.length) {
				var row = this.rowList[internalCount];
				row.update(valueArray[i]);
			} else
				this.addRow(valueArray[i]);
			internalCount++;
		}
		// remove rows
		while(this.rowList.length > internalCount) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function getValues() {
		var returnArray = new Array();
		for(var i = 0; i < this.rowList.length; i++) {
			var row = this.rowList[i];
			returnArray[i] = row.getValue();
		}
		return returnArray;
	}

	function reset() {
		while(this.rowList.length > 0) {
			var row = this.rowList.pop();
			row.destroy();
		}
	}

	function addRow(/*String*/ value) {
		var row = new wcms.gui.SimpleArrayRow(value, this);
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

	function rowChanged(/*Row*/ row) {
		this.eventHandler.notify({"type" : "rowChanged", "row": row});
	}

	wcms.gui.SimpleArrayEditor.prototype.addRow = addRow;
	wcms.gui.SimpleArrayEditor.prototype.removeRow = removeRow;
	wcms.gui.SimpleArrayEditor.prototype.update = update;
	wcms.gui.SimpleArrayEditor.prototype.getValues = getValues;
	wcms.gui.SimpleArrayEditor.prototype.reset = reset;
	wcms.gui.SimpleArrayEditor.prototype.setDisabled = setDisabled;
	wcms.gui.SimpleArrayEditor.prototype.rowChanged = rowChanged;
	
})();

wcms.gui.SimpleArrayRow = function(/*String*/ value, /*wcms.navigation.LabelEditor*/ arrayEditor) {
	var instance = this;
	this.domNode = dojo.create("div");
	
	this.valueBox = new dijit.form.TextBox({
		value: value,
		intermediateChanges: true,
		onChange: function(/*String*/ newValue) {
			this.set("value", newValue);
			arrayEditor.rowChanged(instance);
		}
	});

	this.removeButton = new dijit.form.Button({
		iconClass: "icon12 removeIcon12",
		showLabel: false,
		onClick: function() {
			console.log("remove clicked");
			arrayEditor.removeRow(instance);
		}
	});

	this.domNode.appendChild(this.valueBox.domNode);
	this.domNode.appendChild(this.removeButton.domNode);
};

( function() {

	function destroy() {
		// destroy widgets
		this.valueBox.destroy();
		this.removeButton.destroy();
		// remove from dom
		dojo.destroy(this.domNode);
		console.log("I18nEditor - Row: destroyed");
	}

	function update(/*String*/ lang, /*String*/ label) {
		this.valueBox.set("value", label);
	}

	function setDisabled(/*boolean*/ value) {
		this.valueBox.set("disabled", value);
		this.removeButton.set("disabled", value);
	}
	
	function getValue() {
		return this.valueBox.get("value");
	}

	wcms.gui.SimpleArrayRow.prototype.update = update;
	wcms.gui.SimpleArrayRow.prototype.destroy = destroy;
	wcms.gui.SimpleArrayRow.prototype.setDisabled = setDisabled;
	wcms.gui.SimpleArrayRow.prototype.getValue = getValue;
})();
