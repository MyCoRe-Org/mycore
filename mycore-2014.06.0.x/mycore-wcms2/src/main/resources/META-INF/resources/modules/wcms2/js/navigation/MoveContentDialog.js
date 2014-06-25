var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.MoveContentDialog = function() {
	this.internalDialog = new dijit.Dialog({
		title: I18nManager.getInstance().getI18nTextAsString("component.wcms.navigation.moveContentDialog.header")
	});
	this.moveButton = new dijit.form.Button({label: I18nManager.getInstance().getI18nTextAsString("component.wcms.navigation.moveContentDialog.move")});
	this.cancelButton = new dijit.form.Button({label: I18nManager.getInstance().getI18nTextAsString("component.wcms.general.cancel")});

	this.created = false;
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function show(href) {
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
				style: "width: 250px"
			});
			var innerTr = dojo.create("tr");
			var innerTd = dojo.create("td", {
				innerHTML: I18nManager.getInstance().getI18nTextAsString("component.wcms.navigation.moveContentDialog.path")
			});
			innerTr.appendChild(innerTd);
			innerTd = dojo.create("td", {
				align: "right"
			});

			this.hrefTextBox = new dijit.form.TextBox();
			dojo.addClass(this.hrefTextBox, "smallComponent");

			innerTd.appendChild(this.hrefTextBox.domNode);
			innerTr.appendChild(innerTd);
			contentTable.appendChild(innerTr);
			td.appendChild(contentTable);

			// ok cancel
			tr = dojo.create("tr");
			td = dojo.create("td");
			dojo.addClass(td, "dialogOkPanel");

			td.appendChild(this.cancelButton.domNode);
			td.appendChild(this.moveButton.domNode);
			tr.appendChild(td);
			table.appendChild(tr);

			// add button listener
			dojo.connect(this.moveButton, "onClick", this, function() {
				this.internalDialog.hide();
				this.eventHandler.notify({"type" : "moveButtonClicked", "href" : this.hrefTextBox.get("value")});
			});
			dojo.connect(this.cancelButton, "onClick", this, function() {
				this.internalDialog.hide();
			});
			this.created = true;
		}
		this.hrefTextBox.set("value", href);
		this.internalDialog.show();
	}

	function updateLang() {
		// TODO
	}

	wcms.navigation.MoveContentDialog.prototype.show = show;
	wcms.navigation.MoveContentDialog.prototype.updateLang = updateLang;
})();