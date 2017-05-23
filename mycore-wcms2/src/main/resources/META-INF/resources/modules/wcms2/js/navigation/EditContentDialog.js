/*
 * @package wcms.navigation
 * @description model data for internal tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.EditContentDialog = function() {
	this.constructor();

	this.type = "okCancel"
	this.i18nTitle = "component.wcms.navigation.itemEditor.editContent.title";

	this.selectedSection = null;
	
	this.href = null;

	// ck editor
	this.editorDiv = null;
	this.editor = null;
	this.canNotEdit = null;

	// section
	this.sectionSelect = null;
	this.sectionToolbar = null;
	this.addSection = null;
	this.removeSection = null;
	this.moveSectionUp = null;
	this.moveSectionDown = null;
	this.editSection = null;
	
	this.oldWebpageContent = null;
	this.webpageContent = null;

	this.sectionCount = 0;

	// internal dialogs
	this.removeSectionDialog = null;
	this.editSectionDialog = null;
};

( function() {

	var contentToolbar = [
				           ['Source', '-', 'NewPage', 'Preview', 'Templates'],
				           ['Undo', 'Redo', '-', 'Find', 'Replace'],
				           ['Link','Unlink','Anchor'],
				           ['Image', 'Table','HorizontalRule','SpecialChar'],
				           ['PastFromWord', 'Spellchecker', 'Maximize', '-','About'],
				           '/',
				           ['Format','Font','FontSize'],
				           ['Bold', 'Italic', 'Underline', 'Strike'],
				           ['JustifyLeft','JustifyCenter','JustifyRight','JustifyBlock'],
				           ['NumberedList','BulletedList'],
				           ['Outdent','Indent'],
				           ['TextColor','BGColor']];

	function createContent() {
		// create dijit components
		//  editor
		this.editorDiv = new dijit.layout.ContentPane({region:"center",style: "border:none;padding:0px"});
		//  section
		this.sectionSelect = new dijit.form.MultiSelect({region: "center",style: "width: 100%"});
		this.sectionToolbar = new dijit.Toolbar({region: "bottom"});
		this.addSection = new dijit.form.Button({iconClass: "icon16 addIcon16",	showLabel: false});
		this.removeSection = new dijit.form.Button({iconClass: "icon16 removeIcon16", showLabel: false});
		this.moveSectionUp = new dijit.form.Button({iconClass: "icon16 upIcon16", showLabel: false});
		this.moveSectionDown = new dijit.form.Button({iconClass: "icon16 downIcon16",showLabel: false});
		this.editSection= new dijit.form.Button({iconClass: "icon16 editIcon16",showLabel: false});
		//  internal dialogs
		this.removeSectionDialog = new wcms.gui.SimpleDialog("yesNo",
				"component.wcms.navigation.itemEditor.editContent.deleteSectionTitle",
				"component.wcms.navigation.itemEditor.editContent.deleteSection");
		this.editSectionDialog = new wcms.navigation.EditContentSectionDialog();

		// change ok button text
		this.okButton.i18n = "component.wcms.navigation.itemEditor.editContent.markChanges";

		// build layout
        var bc = new dijit.layout.BorderContainer({style:"height:600px; width:1000px"});

        var leftBC = new dijit.layout.BorderContainer({
        	region:"left",
        	style: "width:225px",
        	splitter: true,
        	gutters: false
        });
        this.canNotEdit = new dijit.layout.ContentPane({region:"center",style: "border:none;padding:0px; display:none", content: ""});
        
        leftBC.addChild(this.sectionSelect);
        leftBC.addChild(this.sectionToolbar);
		bc.addChild(leftBC);
		bc.addChild(this.editorDiv);
		bc.addChild(this.canNotEdit);

		// add toolbar items
        this.sectionToolbar.addChild(this.addSection);
        this.sectionToolbar.addChild(this.removeSection);
        this.sectionToolbar.addChild(new dijit.ToolbarSeparator());
        this.sectionToolbar.addChild(this.moveSectionUp);
        this.sectionToolbar.addChild(this.moveSectionDown);
        this.sectionToolbar.addChild(new dijit.ToolbarSeparator());
        this.sectionToolbar.addChild(this.editSection);

		bc.startup();
		this.content.appendChild(bc.domNode);

		// add listener
		// dialog
		dojo.connect(this.internalDialog, "onHide", this, function() {
			this.destroyCKEditor();
		});
		// section
		// use watch instead of onChange because onChange isn't called any time
		this.sectionSelect.watch("value", dojo.hitch(this, changeSection));
		dojo.connect(this.addSection, "onClick", this, addSection);
		dojo.connect(this.removeSection, "onClick", this, function() {
			this.removeSectionDialog.show();
		});
		dojo.connect(this.moveSectionUp, "onClick", this, moveSectionUp);
		dojo.connect(this.moveSectionDown, "onClick", this, moveSectionDown);
		dojo.connect(this.editSection, "onClick", this, function() {
			var selectedList = this.sectionSelect.getSelected();
			if(selectedList == null || selectedList.length == 0)
				return;
			var option = selectedList[0];
			var content = this.getSectionById(option.value);
			this.editSectionDialog.show(true, content);
		});
        // dialog events
		this.removeSectionDialog.eventHandler.attach(dojo.hitch(this, function(/*wcms.gui.SimpleDialog*/ source, /*Json*/ args) {
			if(args.type == "yesButtonClicked") {
				var removeSectionFunc = dojo.hitch(this, removeSelectedSections);
				removeSectionFunc();
			}
		}));
		this.editSectionDialog.eventHandler.attach(dojo.hitch(this, function(/*wcms.gui.SimpleDialog*/ source, /*Json*/ args) {
			if(args.type == "okButtonClicked") {
				// only edit
				if(source.isEditSection) {
					this.selectedSection.title = source.getTitle();
					this.selectedSection.lang = source.getLang();
					var updateSelectedOptionsFunc = dojo.hitch(this, updateSelectedOptions);
					updateSelectedOptionsFunc();
				} else { // add new section
					var newContent = {
						id: this.sectionCount++,
						data: "",
						title: source.getTitle(),
						lang: source.getLang()
					};
					this.webpageContent.push(newContent);
					var addOptionFunc = dojo.hitch(this, addOption);
					addOptionFunc(newContent);
				}
			}
		}));

		// ckeditor events
		CKEDITOR.on("instanceReady", dojo.hitch(this, function(e) {
			// set old webpageContent
			if(this.selectedSection != null) {
				this.selectedSection.data = this.editor.getData();
			}
			this.oldWebpageContent = dojo.clone(this.webpageContent);
			for(var i = 0; i < this.oldWebpageContent.length; i++) {
				delete(this.oldWebpageContent[i].id);
			}
		}));
	}

	function show(/*JSON*/ content, href) {
		// super.show();
		wcms.gui.AbstractDialog.prototype.show.call(this);
		// load content
		this.href = href;
		var loadContentFunc = dojo.hitch(this, loadContent);
		loadContentFunc(content);
		
		dojo.removeClass(this.editorDiv.id,"hidden");
		dojo.removeClass(this.canNotEdit.id, "visible");
		if(this.selectedSection == null || this.selectedSection.hidden == "true") {
			dojo.addClass(this.editorDiv.id, "hidden");
      var invalidText = I18nManager.getInstance().getI18nTextAsString("component.wcms.navigation.itemEditor.canNotEdit");
      var canNotEditLabel = "<p>" + invalidText + " '" + this.selectedSection.invalidElement + "'</p>";
      this.canNotEdit.set("content", canNotEditLabel);
			dojo.addClass(this.canNotEdit.id, "visible");
		}
	}

	function loadContent(/*JSON*/ webpageContent) {
		this.webpageContent = dojo.clone(webpageContent);

		var addOptionFunc = dojo.hitch(this, addOption);
		// update section
		this.sectionCount = 0;
		for(var i = 0; i < this.webpageContent.length; i++) {
			this.webpageContent[i].id = this.sectionCount++;
			addOptionFunc(this.webpageContent[i]);
		}
		// select option
		if(this.sectionSelect.domNode.hasChildNodes())
			dojo.attr(this.sectionSelect.domNode.firstChild,"selected", "selected");

		// set current content
		this.selectedSection = null;
		if(this.webpageContent.length > 0)
			this.selectedSection = this.webpageContent[0];

		// set ck editor content
		var contentData = "";
		if(this.selectedSection != null && this.selectedSection.data)
			contentData = this.selectedSection.data;
		// ck editor settings
		var lang=I18nManager.getInstance().getLang();
		var folderHref = this.href.substring(0, this.href.lastIndexOf("/"));
		var context = window.location.pathname.substring(0, window.location.pathname.indexOf("/modules"));
		this.editor = CKEDITOR.appendTo(this.editorDiv.domNode, {
			toolbar : contentToolbar,
			uiColor : '#9AB8F3',
			language: lang,
			height:"495px", width:"98%",
			entities: false,
			basicEntities: true,
			allowedContent: true,
			autoParagraph: false,
			filebrowserBrowseUrl: context + '/rsc/wcms2/filebrowser?href=' + folderHref + "&type=files" + "&basehref=" + context + folderHref + "/",
			filebrowserUploadUrl: context + '/rsc/wcms2/filebrowser/upload?href=' + folderHref + "&type=files" + "&basehref=" + context + folderHref + "/",
			filebrowserImageBrowseUrl: context + '/rsc/wcms2/filebrowser?href=' + folderHref + "&type=images",
		  	filebrowserImageUploadUrl: context + '/rsc/wcms2/filebrowser/upload?href=' + folderHref + "&type=images",
		  	baseHref: context + folderHref + "/"
		}, contentData);
	}

	function updateLang() {
		if(!this.created)
			return;
		// super.updateLang();
		wcms.gui.AbstractDialog.prototype.updateLang.call(this);
		// update text
		this.removeSectionDialog.updateLang();
		this.editSectionDialog.updateLang();
	}

	function selectSection(/*DomNode*/ node) {
		this.sectionSelect.set("value", node.value);
		var changeSectionFunc = dojo.hitch(this, changeSection);
		changeSectionFunc();
	}

	function changeSection() {
		// save old content data
		if(this.selectedSection != null && this.selectedSection.hidden != "true")
			this.selectedSection.data = this.editor.getData();
		
		// switch to new content
		dojo.removeClass(this.editorDiv.id,"hidden");
		dojo.removeClass(this.canNotEdit.id, "visible");
		var id = this.sectionSelect.get("value")[0];
		var content = this.getSectionById(id);
		if(!content.data)
			content.data = "";
		this.selectedSection = content;
		
		if(content.hidden != "true"){
			this.editor.setData(content.data);
		}
		else{
			dojo.addClass(this.editorDiv.id,"hidden");
			dojo.addClass(this.canNotEdit.id, "visible");
		}
	}

	function getSectionById(/*int*/ id) {
		for(var i in this.webpageContent)
			if(this.webpageContent[i].id == id)
				return this.webpageContent[i];
		return null;
	}

	function addSection() {
		var newContent = {
			title: "undefined",
			lang: "all"
		};
		this.editSectionDialog.show(false, newContent);
	}

	function addOption(/*JSON*/ content) {
		var title = content.title;
		if(content.lang)
			title += " (" + content.lang + ")";
		var option = dojo.create("option", {
			innerHTML: title,
			value: content.id
		});
		var selectedList = this.sectionSelect.getSelected();
		if(selectedList != null && selectedList.length > 0) {
			dojo.place(option, selectedList[0], "after");
		} else {
			dojo.place(option, this.sectionSelect.domNode);
		}
	}

	function updateSelectedOptions() {
		var selectedList = this.sectionSelect.getSelected();
		if(selectedList == null || selectedList.length == 0)
			return;
		for(var i = 0; i < selectedList.length; i++) {
			var option = selectedList[i];
			var id = option.value;
			var content = this.getSectionById(id);
			var title = content.title;
			if(content.lang)
				title += " (" + content.lang + ")";
			dojo.attr(option, "innerHTML", title);
		}
	}

	function removeSelectedSections() {
		// remove sections
		var selectedList = this.sectionSelect.getSelected();
		if(selectedList == null || selectedList.length == 0)
			return;
		for(var i = 0; i < selectedList.length; i++) {
			// remove from list
			var option = selectedList[i];
			var id = option.value;
			dojo.destroy(option);
			// remove from content array
			this.webpageContent = arrayRemoveById(this.webpageContent, id);
		}
		if(this.sectionSelect.domNode.hasChildNodes()) {
			var selectSectionFunc = dojo.hitch(this, selectSection);
			selectSectionFunc(this.sectionSelect.domNode.firstChild);
		} else {
			this.editor.setData("");
		}
	}

	function moveSectionUp() {
		var selectedList = this.sectionSelect.getSelected();
		if(selectedList == null || selectedList.length == 0)
			return;
		for(var i = 0; i < selectedList.length; i++) {
			var selectedOption = selectedList[i];
			// move option up
			var prev = selectedOption.previousSibling;	
			if(prev != null) {
				dojo.place(selectedOption, prev, "before");
			} else {
				dojo.place(selectedOption, this.sectionSelect.domNode, "last");
			}
		}
	}

	function moveSectionDown() {
		var selectedList = this.sectionSelect.getSelected();
		selectedList.reverse();
		if(selectedList == null || selectedList.length == 0)
			return;
		for(var i = 0; i < selectedList.length; i++) {
			var selected = selectedList[i];
			var next = selected.nextSibling;
			if(next != null) {
				dojo.place(selected, next, "after");
			} else {
				dojo.place(selected, this.sectionSelect.domNode, "first");
			}
		}
	}

	function orderSections() {
		var newSectionArray = [];
		// go through all options to get the correct order
		dojo.query("option",this.sectionSelect.containerNode).forEach(dojo.hitch(this, function(n) {
			var id = n.value;
			var content = this.getSectionById(id);
			newSectionArray.push(content);
		}));
		this.webpageContent = newSectionArray;
	}

	/**
	 * @Override
	 */
	function onBeforeOk() {
		// sort content array
		var orderSectionsFunc = dojo.hitch(this, orderSections);
		orderSectionsFunc();
		// save content data
		if (this.selectedSection.hidden != "true"){
			this.selectedSection.data = this.editor.getData();
		}
		// remove internal id from content
		for(var i = 0; i < this.webpageContent.length; i++) {
			delete(this.webpageContent[i].id);
		}
	}

	function destroyCKEditor() {
		console.log("destroy ck editor");
		// destroy all section options
		var sectionSelect = this.sectionSelect.domNode;
		while (sectionSelect.hasChildNodes())
			sectionSelect.removeChild(sectionSelect.lastChild);
		 // destroy ck editor
		 this.editor.destroy();
		 this.editor = null;
	}

	// inheritance
	wcms.navigation.EditContentDialog.prototype = new wcms.gui.AbstractDialog;

	wcms.navigation.EditContentDialog.prototype.createContent = createContent;
	wcms.navigation.EditContentDialog.prototype.destroyCKEditor = destroyCKEditor;
	wcms.navigation.EditContentDialog.prototype.getSectionById = getSectionById;
	wcms.navigation.EditContentDialog.prototype.show = show;
	wcms.navigation.EditContentDialog.prototype.updateLang = updateLang;
	wcms.navigation.EditContentDialog.prototype.onBeforeOk = onBeforeOk;
})();

wcms.navigation.EditContentSectionDialog = function() {
	this.constructor();

	this.type = "okCancel"

	this.contentEditor = null;
	this.titleBox = null;
	
	this.isEditSection = true;
};

( function() {

	var editSectionText = "component.wcms.navigation.itemEditor.editContent.editSection.dialogTitleEdit";
	var newSectionText = "component.wcms.navigation.itemEditor.editContent.editSection.dialogTitleNew";

	var titleText = "component.wcms.navigation.itemEditor.editContent.editSection.title";
	var langText = "component.wcms.navigation.itemEditor.editContent.editSection.lang";

	function createContent() {
		// create dijit components
		this.contentEditor = new wcms.gui.ContentEditor();

		this.titleBox = new dijit.form.TextBox();
		this.langBox = new dijit.form.TextBox();

		// build layout
		this.contentEditor.addElement(titleText, this.titleBox.domNode);
		this.contentEditor.addElement(langText, this.langBox.domNode);

		this.content.appendChild(this.contentEditor.domNode);
	}

	function updateLang() {
		if(!this.created)
			return;
		// super.updateLang();
		wcms.gui.AbstractDialog.prototype.updateLang.call(this);
		// update content
		this.contentEditor.updateLang();
	}

	function show(/*boolean*/ editSection, /*JSON*/ content) {
		this.isEditSection = editSection; 

		// show dialog
		wcms.gui.AbstractDialog.prototype.show.call(this);

		this.titleBox.set("value", content.title);
		this.langBox.set("value", content.lang);
		
		if(editSection == true)
			this.setTitle(editSectionText);
		else
			this.setTitle(newSectionText);
	}
	
	function getLang() {
		return this.langBox.get("value");
	}
	function getTitle() {
		return this.titleBox.get("value");
	}

	// inheritance
	wcms.navigation.EditContentSectionDialog.prototype = new wcms.gui.AbstractDialog;

	wcms.navigation.EditContentSectionDialog.prototype.createContent = createContent;
	wcms.navigation.EditContentSectionDialog.prototype.show = show;
	wcms.navigation.EditContentSectionDialog.prototype.updateLang = updateLang;
	wcms.navigation.EditContentSectionDialog.prototype.getLang = getLang;
	wcms.navigation.EditContentSectionDialog.prototype.getTitle = getTitle;
})();
