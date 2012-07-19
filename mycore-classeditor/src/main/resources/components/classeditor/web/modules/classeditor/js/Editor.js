/*
 * @package classeditor
 */
var classeditor = classeditor || {};

/**
 * Create a new instance of the classification editor.
 * 
 * @param settings json object to configure the classification editor.
 * The following parameters are required:
 *   webAppBaseURL: base url of web application (e.g. http://localhost:8291/)
 *   resourceURL: url of resource (e.g. http://localhost:8291/rsc/classifications/)
 * The following parameters are optional:
 *   showId: are classification id and category id are editable (true | false)
 *   supportedLanguages: which languages are available for selection (json array ["de", "en", "pl"])
 *   language: the current language (e.g. "de")
 *   editable: if the user can edit and dnd
 */
classeditor.Editor = function(settings) {
	this.settings = settings;
	// divs
	this.mainContentPane = new dijit.layout.ContentPane();
	this.domNode = this.mainContentPane.domNode;
	dojo.addClass(this.domNode, "classeditor");

	// classification & category
	this.classificationId = null;
	this.categoryId = null;

	// content
	this.treePane = null;
	this.categoryEditorPane = null;

	// settings
	this.settingsDialog;
	
	// misc
	this.saveArray = [];
	this.created = false;
};

( function() {

	/**
	 * Creates all important instances and the dom structure.
	 */
	function create() {
		// I18nManager
		var sm = SimpleI18nManager.getInstance();
		sm.initialize(this.settings);

		// toolbar
		this.navigationToolbar = new dijit.Toolbar({
			splitter: false,
			region: "top"
		});
		// create tree & category editor
		this.treePane = new classeditor.TreePane(this.settings);
		this.treePane.create();
		this.categoryEditorPane = new classeditor.CategoryEditorPane(this.settings);
		this.categoryEditorPane.create();

		// create borderlayout in tab
		var borderContainer = new dijit.layout.BorderContainer();
		this.mainContentPane.set("content", borderContainer.domNode);
		// create panes
		this.treePane.mainPane.set("region", "left");
		this.categoryEditorPane.mainPane.set("region", "center");
		// add to dom
		borderContainer.addChild(this.navigationToolbar);
		borderContainer.addChild(this.treePane.mainPane);
		borderContainer.addChild(this.categoryEditorPane.mainPane);
		borderContainer.layout();
		// toolbar buttons
		this.saveButton = new dijit.form.Button({
			showLabel: false,
			disabled: true,
			iconClass: "icon16 saveDisabledIcon",
			onClick: dojo.hitch(this, save)
		});
		var saveButtonTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.save.changes")
		});
		saveButtonTT.addTarget(this.saveButton.domNode);
		this.refreshButton = new dijit.form.Button({
			showLabel: false,
			iconClass: "icon16 refreshIcon",
			onClick: dojo.hitch(this, function() {
				if(!confirm(sm.get("component.classeditor.refresh.warning"))) {
					return;
				}
				this.reloadClassification();
			})
		});
		var refreshButtonTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.refresh.dialog")
		});
		refreshButtonTT.addTarget(this.refreshButton.domNode);
		this.settingsButton = new dijit.form.Button({
			showLabel: false,
			iconClass: "icon16 settingsIcon",
			onClick: dojo.hitch(this, openSettingsDialog)
		});
		var settingsButtonTT = new dijit.Tooltip({
			label: sm.get("component.classeditor.settings.dialog")
		});
		settingsButtonTT.addTarget(this.settingsButton.domNode);

		this.navigationToolbar.addChild(this.saveButton);
		this.navigationToolbar.addChild(this.refreshButton);
		this.navigationToolbar.addChild(this.settingsButton);

		// settings
		this.settingsDialog = new classeditor.SettingsDialog(this.settings);
		this.settingsDialog.eventHandler.attach(dojo.hitch(this, handleSettingsDialogEvents));

		// events
		this.treePane.tree.eventHandler.attach(dojo.hitch(this, handleTreeEvents));
		this.categoryEditorPane.eventHandler.attach(dojo.hitch(this, handleCategoryEditorEvents));
		dojo.connect(this.settingsDialog.internalDialog, "onHide", this, onSettingsDialogClose);

		// render
		this.mainContentPane.startup();
		this.created = true;
	}

	/**
	 * Loads a new classification - if this string is empty, all
	 * classifications are loaded.
	 */
	function loadClassification(/*String*/ classificationId, /*String*/ categoryId) {
		if(!this.created) {
			alert("you have to call the create() method before you can load a classification");
			return;
		}
		this.classificationId = classificationId;
		this.categoryId = categoryId;
		this.reloadClassification();
	}

	/**
	 * Reloads the tree - be aware that loadClassification has to be called before!
	 */
	function reloadClassification() {
		this.categoryEditorPane.setEditable(false);
		this.treePane.loadClassification(this.classificationId, this.categoryId);
		this.saveArray = [];
		this.updateToolbar(false);
	}

	function handleTreeEvents(/*LazyLoadingTree*/ source, /*JSON*/ args) {
		if(args.type == "itemSelected") {
			if(args.item == null) {
				this.categoryEditorPane.setEditable(false);
			} else {
				this.categoryEditorPane.setEditable(this.settings.editable || this.settings.editable == undefined);
				this.categoryEditorPane.update(args.item);
			}
		} else if(args.type == "itemAdded") {
			this.updateSaveArray("update", args.item, args.parent);
			this.updateToolbar(true);
			this.categoryEditorPane.update();
		} else if(args.type == "itemMoved") {
			this.updateSaveArray("update", args.item, args.parent);
			this.updateToolbar(true);
		} else if(args.type == "itemsRemoved") {
			for(var i = 0; i < args.items.length; i++) {
				this.updateSaveArray("delete", args.items[i]);
			}
			this.categoryEditorPane.setEditable(false);
			this.updateToolbar(true);
		}
	}

	function handleCategoryEditorEvents(/*CategoryEditorPane*/ source, /*JSON*/ args) {
		if(args.type == "labelChanged" || args.type == "urlChanged" || args.type == "idChanged") {
			var key = undefined;
			if(args.type == "labelChanged") {
				key = "labels";
			} else if(args.type == "urlChanged") {
				key = "uri";
			} else if(args.type == "idChanged") {
				key = "id";
			}
			this.treePane.tree.update(args.item, key, args.value);
			this.updateSaveArray("update", args.item);
			this.updateToolbar(true);
		}
	}

	function handleSettingsDialogEvents(/*SettingsDialog*/ source, /*JSON*/ args) {
		if(args.type == "saveBeforeImport") {
			this.save();
		}
	}

	function updateToolbar(/*boolean*/ dirty) {
		if(dirty) {
			this.saveButton.set("disabled", false);
			this.saveButton.set("iconClass", "icon16 saveIcon");
		} else {
			this.saveButton.set("disabled", true);
			this.saveButton.set("iconClass", "icon16 saveDisabledIcon");
		}
	}

	function updateSaveArray(/*String*/ state, /*dojo.data.item*/ item, /*dojo.data.item*/ parent) {
		// get object from array
		var saveObject = null;
		for(var i = 0; i < this.saveArray.length; i++) {
			if(isIdEqual(this.saveArray[i].item.id[0], item.id[0])) {
				saveObject = this.saveArray[i];
			}
		}
		// if not defined -> create new and add to array
		if(saveObject == null) {
			saveObject = {};
			this.saveArray.push(saveObject);
		}
		// set new data
		saveObject.item = item;
		saveObject.state = state;
		if(parent != null) {
			saveObject.parent = parent;
		}
	}

	function save() {
		var finalArray = [];
		for(var i = 0; i < this.saveArray.length; i++) {
			var saveObject = this.saveArray[i];
			var cleanedSaveObject = {
				item: cloneAndCleanUp(saveObject.item),
				state: saveObject.state
			}
			if(saveObject.state == "update" && saveObject.parent) {
				if(saveObject.parent.children) {
					cleanedSaveObject.parentId = saveObject.parent.id[0];
					var level = this.treePane.tree.getLevel(saveObject);
					var index = this.treePane.tree.indexAt(saveObject.parent, saveObject.item.id[0]);
					cleanedSaveObject.depthLevel = level;
					cleanedSaveObject.index = index;
				} else {
					cleanedSaveObject.state = "delete";
				}
			}
			finalArray.push(cleanedSaveObject);
		}

		var sm = SimpleI18nManager.getInstance();
		var navXhrArgs = {
			url :  this.settings.resourceURL + "save",
			postData : dojo.toJson(finalArray),
			handleAs : "json",
			headers: { "Content-Type": "application/json; charset=utf-8"},
			error : dojo.hitch(this, function(error) {
			    if(error.status === 401) {
			        alert(sm.get("component.classeditor.error.nopermissions"))
			    } else{
			        alert(sm.get("component.classeditor.save.generalerror") + " - " + error);
			    }
				console.log("error while saving");
				console.log(error);
			}),
			load : dojo.hitch(this, function(data) {
				console.log("saving done");
				console.log(data);
				for(var i = 0; i < this.saveArray.length; i++) {
					var saveObject = this.saveArray[i];
					if(saveObject.item.added) {
						saveObject.item.added = false;
					}
				}
				this.saveArray = [];
				this.updateToolbar(false);
				this.categoryEditorPane.update();
				alert(sm.get("component.classeditor.save.successfull"));
			})
		};
		dojo.xhrPost(navXhrArgs);
	}

	function cloneAndCleanUp(/*dojo.data.item*/ item) {
		var newItem = {
			id: item.id[0],
			labels: item.labels
		};
		if(item.uri) {
			newItem.uri = item.uri[0];
		}
		return newItem;
	}

	function openSettingsDialog() {
		this.settingsDialog.open(this.saveArray.length > 0);
	}

	function onSettingsDialogClose() {
		var sd = this.settingsDialog;
		SimpleI18nManager.getInstance().setSupportedLanguages(sd.languageEditor.getValues());
		this.categoryEditorPane.labelEditor.updateLanguages();
		var showIds = sd.showIdCheckBox.get("value");
		if(showIds) {
			this.treePane.showId();
			this.categoryEditorPane.showId();
		} else {
			this.treePane.hideId();
			this.categoryEditorPane.hideId();
		}
		// if a new classification was successfully imported
		if(sd.classificationImported) {
			this.reloadClassification();
		}
	}

	function setEditable(editable) {
		this.settings.editable = editable == undefined ? true : editable;
		this.treePane.setEditable(this.settings.editable);
		this.categoryEditorPane.setEditable(this.settings.editable);
	}

	classeditor.Editor.prototype.create = create;
	classeditor.Editor.prototype.loadClassification = loadClassification;
	classeditor.Editor.prototype.reloadClassification = reloadClassification;
	classeditor.Editor.prototype.updateToolbar = updateToolbar;
	classeditor.Editor.prototype.updateSaveArray = updateSaveArray;
	classeditor.Editor.prototype.save = save;
	classeditor.Editor.prototype.setEditable = setEditable;

})();
