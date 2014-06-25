var wcms = wcms || {};
wcms.gui = wcms.gui || {};

wcms.gui.ErrorDialog = function(/*String*/ i18nTitle, /*String*/ i18nText, /*String*/ error) {
	this.constructor();
	
	this.type = this.Type.ok;
	this.i18nTitle = i18nTitle;
	this.i18nText = i18nText;
	this.imageUrl = imagePath + "/warning.png";
	this.error = error;
};

( function() {

	function createContent() {
		// super.createContent();
		wcms.gui.SimpleDialog.prototype.createContent.call(this);

		if(this.error == null)
			return;

		var tr = dojo.create("tr", {style: "color: red"});
		var emptyTd = dojo.create("td");
		var stackTraceTd = dojo.create("td", {
			innerHTML : this.error
		});
		tr.appendChild(emptyTd);
		tr.appendChild(stackTraceTd);
		this.contentTable.appendChild(tr);

		this.setWidth(700);
	}

	// inheritance
	wcms.gui.ErrorDialog.prototype = new wcms.gui.SimpleDialog;

	wcms.gui.ErrorDialog.prototype.createContent = createContent;

})();
