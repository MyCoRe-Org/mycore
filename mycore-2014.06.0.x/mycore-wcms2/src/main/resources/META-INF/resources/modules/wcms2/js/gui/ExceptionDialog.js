var wcms = wcms || {};
wcms.gui = wcms.gui || {};

wcms.gui.ExceptionDialog = function(/*String*/ i18nTitle, /*String*/ i18nText, /*JSON*/ exception) {
	this.constructor();
	
	this.type = this.Type.ok;
	this.i18nTitle = i18nTitle;
	this.i18nText = i18nText;
	this.imageUrl = imagePath + "/exception.png";
	this.exception = exception;
};

( function() {

	function createContent() {
		// super.createContent();
		wcms.gui.SimpleDialog.prototype.createContent.call(this);

		if(this.exception == null)
			return;

		var file = "[" + this.exception.lineNumber + "] " + this.exception.fileName;
		var tr = dojo.create("tr", {style: "color: red"});
		var emptyTd = dojo.create("td");
		var stackTraceTd = dojo.create("td", {
			innerHTML : "<p>"+ file + "<br />" + this.exception.message + "</p>"
		});
		tr.appendChild(emptyTd);
		tr.appendChild(stackTraceTd);
		this.contentTable.appendChild(tr);

		this.setWidth(700);
	}
	
	// inheritance
	wcms.gui.ExceptionDialog.prototype = new wcms.gui.SimpleDialog;

	wcms.gui.ExceptionDialog.prototype.createContent = createContent;

})();
