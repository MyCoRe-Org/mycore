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
wcms.navigation = wcms.navigation || {};

wcms.navigation.I18nEditor = function() {
	this.domNode = dojo.create("div");

	this.Type = {
		i18n:"i18n",
		label:"label"
	};

	this.disabled = false;

	// i18n text box
	this.i18nRadio = null;
	this.i18nTextBox = null;
	// label editor
	this.labelRadio = null;
	this.labelEditor = null;
	// description text
	this.i18nDescriptionTd = null;
	this.labelDescriptionTd = null;

	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	var i18nText = "component.wcms.navigation.i18nEditor.i18n";
	var labelText = "component.wcms.navigation.i18nEditor.label";

	function create() {
		this.i18nTextBox = new dijit.form.TextBox({
			intermediateChanges: true
		});
		this.labelEditor = new wcms.navigation.LabelEditor();

		// i18n
		dojo.connect(this.i18nTextBox, "onChange", this, function(/*String*/ i18nKey) {
			this.eventHandler.notify({"type" : "i18nChanged", "i18nKey": i18nKey});
		});
		// label editor
		this.labelEditor.eventHandler.attach(dojo.hitch(this, function(/*LabelEditor*/ source, /*Json*/ args) {
			args.labelEditor = source;
			this.eventHandler.notify(args);
		}));

		var buildTableFunc = dojo.hitch(this, buildTable);
		buildTableFunc();
	}

	function buildTable() {
		// create nodes
		var table = dojo.create("table");
		var i18nTr = dojo.create("tr");
		var labelTr = dojo.create("tr");
		this.i18nDescriptionTd = dojo.create("td", {i18n: i18nText});
		var i18nTextBoxTd = dojo.create("td");
		this.labelDescriptionTd = dojo.create("td", {i18n: labelText});
		var labelEditorTd = dojo.create("td");

		// set styles
		dojo.style(this.domNode, {"float" : "right"});
		dojo.style(table, {"width": "100%"});
		dojo.style(labelEditorTd, {"textAlign": "right", "paddingLeft": "15px"});
		dojo.style(i18nTextBoxTd, {"textAlign": "right", "paddingLeft": "15px"});
		dojo.style(this.labelDescriptionTd, {"verticalAlign": "top", "paddingTop": "5px"});

		// add to dom
		this.domNode.appendChild(table);
		table.appendChild(i18nTr);
		table.appendChild(labelTr);
		i18nTr.appendChild(this.i18nDescriptionTd);
		i18nTr.appendChild(i18nTextBoxTd);
		labelTr.appendChild(this.labelDescriptionTd);
		labelTr.appendChild(labelEditorTd);
		i18nTextBoxTd.appendChild(this.i18nTextBox.domNode);
		labelEditorTd.appendChild(this.labelEditor.domNode);
	}

	function update(/*String*/ i18nKey,/*Array[lang]*/ labels) {
		if(i18nKey != undefined) {
			this.i18nTextBox.set("value", i18nKey);
		} else {
			this.i18nTextBox.set("value", null);
		}
		this.labelEditor.update(labels);
	}

	function setDisabled(/*String*/ disabled) {
		this.disabled = disabled;
		this.i18nTextBox.set("disabled", disabled);
		this.labelEditor.setDisabled(disabled);		
	}

	function updateLang() {
		I18nManager.getInstance().updateI18nNode(this.i18nDescriptionTd);
		I18nManager.getInstance().updateI18nNode(this.labelDescriptionTd);
	}

	wcms.navigation.I18nEditor.prototype.create = create;
	wcms.navigation.I18nEditor.prototype.update = update;
	wcms.navigation.I18nEditor.prototype.setDisabled = setDisabled;
	wcms.navigation.I18nEditor.prototype.updateLang = updateLang;
})();
