/*
 * @package wcms.navigation
 * @description editor for a tree item
 */
var wcms = wcms || {};
wcms.gui = wcms.gui || {};

wcms.gui.ContentEditor = function() {
	// internal components
	this.domNode = dojo.create("div");
	this.table = dojo.create("table", {cellspacing:"5", cellpadding:"0", width: "100%"});
	this.domNode.appendChild(this.table);

	// status
	this.disabled = false;
	// i18n
	this.i18nList = [];
	// events
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function setHeader(/*String*/ i18nLabel) {
		var header = dojo.create("caption", {i18n: i18nLabel});
		dojo.addClass(header, "editorHeader");
		this.i18nList.push(header);
		this.table.appendChild(header);
	}

	function addCaption(/*String*/ i18nLabel) {
		var tr = dojo.create("tr");
		var td = dojo.create("td", {colspan: "2"});
		var div = dojo.create("div", {i18n: i18nLabel});
		dojo.addClass(div, "editorCaption");
		this.i18nList.push(div);
		tr.appendChild(td);
		td.appendChild(div);
		this.table.appendChild(tr);
	}

	function addElement(/*String*/ i18nLabel, /*DomNode*/ node) {
		var tr = dojo.create("tr");
		var td = dojo.create("td", {i18n: i18nLabel});
		this.i18nList.push(td);
		dojo.addClass(td, "editorDescription");
		tr.appendChild(td);
		td = dojo.create("td");
		dojo.addClass(td, "editorValue");
		td.appendChild(node);
		tr.appendChild(td);
		this.table.appendChild(tr);
	}

	function setValue(/*dijit.component*/ component, /*Object*/ value) {
		if(value == undefined)
			value = null;
		component.set("value", value);
	}

	function updateLang() {
		// update labels
		for(var i = 0; i < this.i18nList.length; i++) {
			I18nManager.getInstance().updateI18nNode(this.i18nList[i]);
		}
	}

	wcms.gui.ContentEditor.prototype.setHeader = setHeader;
	wcms.gui.ContentEditor.prototype.addCaption = addCaption;
	wcms.gui.ContentEditor.prototype.addElement = addElement;
	wcms.gui.ContentEditor.prototype.setValue = setValue;
	wcms.gui.ContentEditor.prototype.updateLang = updateLang;

})();