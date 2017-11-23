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

var wcms = wcms || {};
wcms.access = wcms.access || {};

wcms.access.RuleDialog = function(rules) {
	this.internalDialog = new dijit.Dialog({
		title: I18nManager.getInstance().getI18nTextAsString("component.wcms.access.ruleDialog.header")
	});
	this.ruleSet = rules;
	this.selectedRule = undefined;
	this.ruleSelect = undefined;
	this.parameter = undefined;

	this.okButton = new dijit.form.Button({label: I18nManager.getInstance().getI18nTextAsString("component.wcms.general.ok")});
	this.cancelButton = new dijit.form.Button({label: I18nManager.getInstance().getI18nTextAsString("component.wcms.general.cancel")});

	this.created = false;
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function show(parameter) {
		this.parameter = parameter;
		if (parameter.access == "read"){
			this.selectedRule = parameter.node.config.readId;
		}
		if (parameter.access == "write"){
			this.selectedRule = parameter.node.config.writeId;
		}

		if(!this.created) {			
			// create dialog
			var table = dojo.create("table");
			this.internalDialog.set("content", table);

			// content - table
			var tr = dojo.create("tr");
			var td = dojo.create("td");
			dojo.addClass(td, "dialogContentPanel");
			tr.appendChild(td);
			table.appendChild(tr);

			// content - label + dropdown
			var contentTable = dojo.create("table", {
				style: "width: 200px"
			});
			var innerTr = dojo.create("tr");
			var innerTd = dojo.create("td", {
				innerHTML: I18nManager.getInstance().getI18nTextAsString("component.wcms.access.ruleDialog.rule")
			});
			innerTr.appendChild(innerTd);
			innerTd = dojo.create("td", {
				align: "right"
			});
			
			this.ruleSelect = new dijit.form.Select();

			for(var i in this.ruleSet){
				this.ruleSelect.addOption({value: i, label: this.ruleSet[i] + " (" + i +")" });	
	        }
			this.ruleSelect.set("maxHeight", -1);
			
			dojo.addClass(this.ruleSelect, "smallComponent");

			innerTd.appendChild(this.ruleSelect.domNode);
			innerTr.appendChild(innerTd);
			contentTable.appendChild(innerTr);
			td.appendChild(contentTable);

			// ok cancel
			tr = dojo.create("tr");
			td = dojo.create("td");
			dojo.addClass(td, "dialogOkPanel");

			td.appendChild(this.cancelButton.domNode);
			td.appendChild(this.okButton.domNode);
			tr.appendChild(td);
			table.appendChild(tr);

			// add button listener
			dojo.connect(this.okButton, "onClick", this, function() {
				this.internalDialog.hide();
				this.eventHandler.notify({"type" : "okButtonClicked", "perm" : this.parameter.access, "ruleId" : this.ruleSelect.get("value"), "node" : this.parameter.node});
			});
			dojo.connect(this.cancelButton, "onClick", this, function() {
				this.internalDialog.hide();
			});
			this.created = true;
		}
		this.ruleSelect.set("Value", this.selectedRule);
		this.internalDialog.show();
	}

	function updateLang() {
		// TODO
	}

	wcms.access.RuleDialog.prototype.show = show;
	wcms.access.RuleDialog.prototype.updateLang = updateLang;
})();
