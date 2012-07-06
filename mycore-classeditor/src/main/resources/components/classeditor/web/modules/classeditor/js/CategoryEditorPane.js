/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * 
 */
classeditor.CategoryEditorPane = function(settings) {
	this.settings = settings;
	// nodes
	this.mainPane = new dijit.layout.ContentPane({
		splitter: true
	});
	this.mainTable;
	this.classIdRow;
	this.categIdRow;
	
	// editor
	this.labelEditor = null;
	this.urlEditor = null;
	this.classIdEditor = null;
	this.categIdEditor = null;

	// event
	this.eventHandler = new classeditor.EventHandler(this);

	// item
	this.currentItem = null;

	// editable
	this.editable = true;
};

( function() {

	function create() {
		// create div
		this.mainTable = dojo.create("table", {className: "categoryEditorMainTable"});
		this.mainPane.set("content", this.mainTable);
		// create label editor
		this.labelEditor = new classeditor.LabelEditor(this.settings);
		this.labelEditor.create();
		var tr = dojo.create("tr", {}, this.mainTable);
		dojo.create("td", {innerHTML: "Label", className: "categoryEditorDescription"}, tr);
		var labelEditorTD = dojo.create("td", {className: "categoryEditorValue"}, tr);
		labelEditorTD.appendChild(this.labelEditor.domNode);
		// url
		this.urlEditor = new dijit.form.ValidationTextBox({
			intermediateChanges: true,
			onChange: dojo.hitch(this, handleURLChanged),
			regExp: "(https?:\/\/)?[\\w-\.]+[\.]+[\\w-:]+[\/\\w-]+"
		});
		//dojo.addClass(this.urlEditor.domNode, "largeComponent");
		var tr2 = dojo.create("tr", {}, this.mainTable);
		dojo.create("td", {innerHTML: "URL", className: "categoryEditorDescription"}, tr2);
		var urlEditorTD = dojo.create("td", {className: "categoryEditorValue"}, tr2);
		urlEditorTD.appendChild(this.urlEditor.domNode);
		// id
		this.classIdEditor = new dijit.form.ValidationTextBox({
			required: true,
			intermediateChanges: true,
			regExp: "[a-zA-Z_\\-0-9]*",
			onChange: dojo.hitch(this, handleIdChanged)
	    });
		this.categIdEditor = new dijit.form.ValidationTextBox({
			required: true,
			intermediateChanges: true,
			regExp: "[a-zA-Z_\\-0-9]*",
			onChange: dojo.hitch(this, handleIdChanged)
		});
		if(this.settings.showId) {
			dojo.hitch(this, showId)();
		}
		// handle events
		this.labelEditor.eventHandler.attach(dojo.hitch(this, handleLabelEditorEvents));
	}

	function update(/*dojo.data.item*/ treeItem) {
		if(treeItem != null) {
			this.currentItem = treeItem;
		}
		// label editor
		this.labelEditor.update(this.currentItem.labels);
		// url editor
		this.urlEditor.set("value", this.currentItem.uri != undefined ? this.currentItem.uri : null);
		// id editors
		if(this.classIdRow != null && this.categIdRow != null) {
			// get id
			var classId = getClassificationId(this.currentItem);
			var categId = getCategoryId(this.currentItem);
			// set classification and category id
			this.classIdEditor.set("value", classId);
			this.categIdEditor.set("value", categId);
			// set editable
			var isClass = isClassification(this.currentItem);
			var hasChilds = hasChildren(this.currentItem);
			var isAdded = this.currentItem.added;
			this.classIdEditor.set("disabled", !this.editable || !(isAdded && isClass && !hasChilds));
			this.categIdEditor.set("disabled", !this.editable || !(isAdded && !isClass));
		}
		// focus
		this.labelEditor.focus();
	}

	function showId() {
		if(this.classIdRow == null) {
			this.classIdRow = dojo.create("tr", {}, this.mainTable);
			dojo.create("td", {innerHTML: "Classification Id", className: "categoryEditorDescription"}, this.classIdRow);
			var classIdEditorTD = dojo.create("td", {className: "categoryEditorValue"}, this.classIdRow);
			classIdEditorTD.appendChild(this.classIdEditor.domNode);
		}
		if(this.categIdRow == null) {
			this.categIdRow = dojo.create("tr", {}, this.mainTable);
			dojo.create("td", {innerHTML: "Category Id", className: "categoryEditorDescription"}, this.categIdRow);
			var categIdEditorTD = dojo.create("td", {className: "categoryEditorValue"}, this.categIdRow);
			categIdEditorTD.appendChild(this.categIdEditor.domNode);
		}
		if(this.currentItem != null) {
			this.update(this.currentItem);
		}
	}

	function hideId() {
		dojo.destroy(this.classIdRow);
		dojo.destroy(this.categIdRow);
		this.classIdRow = null;
		this.categIdRow = null;
	}

	function handleLabelEditorEvents(/*LabelEditor*/ source, /*JSON*/ args) {
		if(args.type == "categoryRemoved" || args.type == "categoryChanged") {
			if(this.currentItem == null) {
				return;
			}
			var labels = this.labelEditor.getValues();
			// check if something changed
			if(deepEquals(labels, this.currentItem.labels)) {
				return;
			}
			// fire event
			this.eventHandler.notify({"type" : "labelChanged", "item": this.currentItem, "value": labels});
		}
	}

	function handleURLChanged(/*String*/ newURL) {
		if((newURL == null || newURL == "") && !this.currentItem.uri) {
			return;
		}
		if(!this.currentItem.uri || newURL != this.currentItem.uri[0]) {
			this.eventHandler.notify({"type" : "urlChanged", "item": this.currentItem, "value": newURL});
		}
	}

	function handleIdChanged() {
		var newClassId = this.classIdEditor.get("value");
		var newCategId = this.categIdEditor.get("value");
		var isClass = isClassification(this.currentItem);
		// check if editors are valid
		if(!this.classIdEditor.isValid() || (!isClass && !this.categIdEditor.isValid())) {
			return;
		}
		// check if something has changed
		var classId = getClassificationId(this.currentItem);
		var categId = getCategoryId(this.currentItem);
		if(classId != newClassId || categId != newCategId) {
			this.eventHandler.notify({
				"type" : "idChanged",
				"item": this.currentItem,
				"value": {
					rootid: newClassId,
					categid: newCategId
				}
			});
		}
	}

	function setEditable(/*boolean*/ editable) {
		this.editable = editable;
		this.labelEditor.setEditable(editable);
		this.urlEditor.set("disabled", !editable);
		if(this.classIdEditor != null) {
			this.classIdEditor.set("disabled", !editable);
		}
		if(this.categIdEditor != null) {
			this.categIdEditor.set("disabled", !editable);
		}
	}

	classeditor.CategoryEditorPane.prototype.create = create;
	classeditor.CategoryEditorPane.prototype.update = update;
	classeditor.CategoryEditorPane.prototype.showId = showId;
	classeditor.CategoryEditorPane.prototype.hideId = hideId;
	classeditor.CategoryEditorPane.prototype.setEditable = setEditable;

})();
