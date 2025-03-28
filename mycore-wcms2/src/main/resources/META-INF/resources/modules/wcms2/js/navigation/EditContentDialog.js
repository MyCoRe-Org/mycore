/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
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

  this.editorDiv = null;
  this.editor = null;

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
  this.shouldNotEditDialog = null;
};

(function() {

  function createContent() {
    // create dijit components
    //  editor
    this.editorDiv = new dijit.layout.ContentPane({
      region: "center",
      style: "border:none;padding:0px",
      content: "<textarea></textarea>"
    });
    //  section
    this.sectionSelect = new dijit.form.MultiSelect({ region: "center", style: "width: 100%" });
    this.sectionToolbar = new dijit.Toolbar({ region: "bottom" });
    this.addSection = new dijit.form.Button({ iconClass: "icon16 addIcon16", showLabel: false });
    this.removeSection = new dijit.form.Button({ iconClass: "icon16 removeIcon16", showLabel: false });
    this.moveSectionUp = new dijit.form.Button({ iconClass: "icon16 upIcon16", showLabel: false });
    this.moveSectionDown = new dijit.form.Button({ iconClass: "icon16 downIcon16", showLabel: false });
    this.editSection = new dijit.form.Button({ iconClass: "icon16 editIcon16", showLabel: false });
    //  internal dialogs
    this.removeSectionDialog = new wcms.gui.SimpleDialog("yesNo",
      "component.wcms.navigation.itemEditor.editContent.deleteSectionTitle",
      "component.wcms.navigation.itemEditor.editContent.deleteSection");
    this.editSectionDialog = new wcms.navigation.EditContentSectionDialog();
    this.shouldNotEditDialog = new wcms.gui.SimpleDialog("Ok",
      "component.wcms.general.warning",
      "component.wcms.navigation.itemEditor.shouldNotEdit");

    // change ok button text
    this.okButton.i18n = "component.wcms.navigation.itemEditor.editContent.markChanges";

    // build layout
    let bc = new dijit.layout.BorderContainer({ style: "height:600px; width:1000px" });

    let leftBC = new dijit.layout.BorderContainer({
      region: "left",
      style: "width:225px",
      splitter: true,
      gutters: false
    });

    leftBC.addChild(this.sectionSelect);
    leftBC.addChild(this.sectionToolbar);
    bc.addChild(leftBC);
    bc.addChild(this.editorDiv);

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
    dojo.connect(this.internalDialog, "onHide", this, () => {
      this.destroyEditor();
    });
    // section
    // use watch instead of onChange because onChange isn't called any time
    this.sectionSelect.watch("value", dojo.hitch(this, changeSection));
    dojo.connect(this.addSection, "onClick", this, addSection);
    dojo.connect(this.removeSection, "onClick", this, () => {
      this.removeSectionDialog.show();
    });
    dojo.connect(this.moveSectionUp, "onClick", this, moveSectionUp);
    dojo.connect(this.moveSectionDown, "onClick", this, moveSectionDown);
    dojo.connect(this.editSection, "onClick", this, () => {
      const selectedList = this.sectionSelect.getSelected();
      if (selectedList == null || selectedList.length === 0) {
        return;
      }
      const id = parseInt(selectedList[0].value);
      const content = this.getSectionById(id);
      this.editSectionDialog.show(true, content);
    });
    // dialog events
    this.removeSectionDialog.eventHandler.attach((/*wcms.gui.SimpleDialog*/ source, /*Json*/ args) => {
      if (args.type === "yesButtonClicked") {
        dojo.hitch(this, removeSelectedSections)();
      }
    });
    this.editSectionDialog.eventHandler.attach(dojo.hitch(this, (/*wcms.gui.SimpleDialog*/ source, /*Json*/ args) => {
      if (args.type === "okButtonClicked") {
        // only edit
        if (source.isEditSection) {
          this.selectedSection.title = source.getTitle();
          this.selectedSection.lang = source.getLang();
          dojo.hitch(this, updateSelectedOptions)();
        } else { // add new section
          const newContent = {
            id: this.sectionCount++,
            data: "",
            title: source.getTitle(),
            lang: source.getLang()
          };
          this.webpageContent.push(newContent);
          dojo.hitch(this, addOption)(newContent);
        }
      }
    }));
  }

  function show(/*JSON*/ content, href) {
    // super.show();
    wcms.gui.AbstractDialog.prototype.show.call(this);
    // load content
    this.href = href;
    dojo.hitch(this, loadContent)(content);

    if (this.selectedSection !== null && this.selectedSection.unknownHTMLTags !== null && this.selectedSection.unknownHTMLTags.length > 0) {
      console.log(this.selectedSection.unknownHTMLTags);
      this.shouldNotEditDialog.show();
    }
  }

  function loadContent(/*JSON*/ webpageContent) {
    this.webpageContent = dojo.clone(webpageContent);

    // editor settings
    const lang = I18nManager.getInstance().getLang();
    const context = window.location.pathname.substring(0, window.location.pathname.indexOf("/modules"));
    const folder = this.href.substring(1, this.href.lastIndexOf("/"));
    const imagePrepend = context + "/" + folder + "/";

    const unknownHTMLTags = new Set();

    // update section
    this.sectionCount = 0;
    for (let i = 0; i < this.webpageContent.length; i++) {
      this.webpageContent[i].id = this.sectionCount++;
      this.webpageContent[i].data = updateImageUrls(this.webpageContent[i].data, context, imagePrepend);
      this.webpageContent[i].unknownHTMLTags.forEach(tag => unknownHTMLTags.add(tag));
      dojo.hitch(this, addOption)(this.webpageContent[i]);
    }

    // select option
    if (this.sectionSelect.domNode.hasChildNodes()) {
      dojo.attr(this.sectionSelect.domNode.firstChild, "selected", "selected");
    }

    // set current content
    this.selectedSection = null;
    if (this.webpageContent.length > 0) {
      this.selectedSection = this.webpageContent[0];
    }

    // set editor content
    let contentData = "";
    if (this.selectedSection != null && this.selectedSection.data) {
      contentData = this.selectedSection.data;
    }

    tinymce.init({
      target: this.editorDiv.domNode.firstChild,
      license_key: "gpl",
      promotion: false,
      height: 599,
      language: lang,
      plugins: [
        "advlist", "anchor", "autolink", "code", "fullscreen", "help",
        "image", "lists", "link", "media", "preview",
        "searchreplace", "table", "visualblocks", "wordcount"
      ],
      toolbar: "blocks | bold italic | forecolor backcolor | alignleft aligncenter alignright alignjustify | bullist numlist | link image",
      toolbar_mode: "wrap",
      entity_encoding: "raw",
      convert_urls: false,
      verify_html: false,
      custom_elements: Array.from(unknownHTMLTags).join(","),
      extended_valid_elements: Array.from(unknownHTMLTags).map(tag => `${tag}[*]`).join(","),
      protect: [
        /<head[\s\S]*?head>/gi,
        /<script[\s\S]*?script>/gi
      ],
      element_format: "xhtml",
      file_picker_types: "image",
      file_picker_callback: (callback, value, meta) => {
        if (meta.filetype === "image") {
          const url = context + "/rsc/wcms2/filebrowser?href=/" + folder + "&type=images";
          const uploadDialogWindow = popupWindow(url, "UploadDialog", window, 1000, 750);
          uploadDialogWindow.callback = (result) => {
            callback(imagePrepend + result);
          };
          return;
        }
        callback();
      },
      setup: (editor) => {
        // init()
        editor.on("init", () => {
          editor.setContent(contentData);
          if (this.selectedSection != null) {
            this.selectedSection.data = editor.getContent();
          }
          this.oldWebpageContent = dojo.clone(this.webpageContent);
          for (let i = 0; i < this.oldWebpageContent.length; i++) {
            delete (this.oldWebpageContent[i].id);
          }
        });
        this.editor = editor;
      }
    });
  }

  function updateImageUrls(content, context, imagePrepend) {
    // Parse the HTML content
    const parser = new DOMParser();
    const doc = parser.parseFromString(content, "text/html");

    // Access the head and body elements
    const head = doc.head;
    const body = doc.body;

    // Function to check if the head is empty
    function isHeadEmpty(headElement) {
      // Check if the head element has any child nodes that are not whitespace
      return !Array.from(headElement.childNodes).some(node => {
        return node.nodeType !== Node.TEXT_NODE || node.nodeValue.trim() !== '';
      });
    }

    // Modify the image URLs in the body
    body.querySelectorAll("img").forEach(img => {
      const src = img.getAttribute("src");
      if (!src.startsWith(context) && !src.startsWith("http")) {
        img.setAttribute("src", imagePrepend + src);
      }
    });

    // Serialize the modified document back to a string
    const serializer = new XMLSerializer();
    return isHeadEmpty(head) ? serializer.serializeToString(body) : serializer.serializeToString(doc.documentElement);
  }

  function popupWindow(url, windowName, win, w, h) {
    const y = win.top.outerHeight / 2 + win.top.screenY - (h / 2);
    const x = win.top.outerWidth / 2 + win.top.screenX - (w / 2);
    return win.open(url, windowName, `width=${w}, height=${h}, top=${y}, left=${x}`);
  }

  function updateLang() {
    if (!this.created)
      return;
    // super.updateLang();
    wcms.gui.AbstractDialog.prototype.updateLang.call(this);
    // update text
    this.removeSectionDialog.updateLang();
    this.editSectionDialog.updateLang();
  }

  function selectSection(/*DomNode*/ node) {
    this.sectionSelect.set("value", node.value);
    dojo.hitch(this, changeSection)();
  }

  function changeSection() {
    // save old content data
    if (this.selectedSection != null) {
      this.selectedSection.data = this.editor.getContent();
    }
    // switch to new content
    dojo.removeClass(this.editorDiv.id, "hidden");
    let id = parseInt(this.sectionSelect.get("value")[0]);
    let content = this.getSectionById(id);
    if (!content.data) {
      content.data = "";
    }
    this.selectedSection = content;
    this.editor.setContent(content.data);
  }

  function getSectionById(/*int*/ id) {
    for (let i in this.webpageContent) {
      if (this.webpageContent[i].id === id) {
        return this.webpageContent[i];
      }
    }
    return null;
  }

  function addSection() {
    const newContent = {
      title: "undefined",
      lang: "all"
    };
    this.editSectionDialog.show(false, newContent);
  }

  function addOption(/*JSON*/ content) {
    let title = content.title;
    if (content.lang) {
      title += " (" + content.lang + ")";
    }
    const option = dojo.create("option", {
      innerHTML: title,
      value: content.id
    });
    const selectedList = this.sectionSelect.getSelected();
    if (selectedList != null && selectedList.length > 0) {
      dojo.place(option, selectedList[0], "after");
    } else {
      dojo.place(option, this.sectionSelect.domNode);
    }
  }

  function updateSelectedOptions() {
    const selectedList = this.sectionSelect.getSelected();
    if (selectedList == null || selectedList.length === 0) {
      return;
    }
    for (let i = 0; i < selectedList.length; i++) {
      const option = selectedList[i];
      const id = parseInt(option.value);
      const content = this.getSectionById(id);
      let title = content.title + (content.lang ? " (" + content.lang + ")" : "");
      dojo.attr(option, "innerHTML", title);
    }
  }

  function removeSelectedSections() {
    // remove sections
    const selectedList = this.sectionSelect.getSelected();
    if (selectedList == null || selectedList.length === 0) {
      return;
    }
    for (let i = 0; i < selectedList.length; i++) {
      // remove from list
      const option = selectedList[i];
      const id = option.value;
      dojo.destroy(option);
      // remove from content array
      this.webpageContent = arrayRemoveById(this.webpageContent, id);
    }
    if (this.sectionSelect.domNode.hasChildNodes()) {
      dojo.hitch(this, selectSection)(this.sectionSelect.domNode.firstChild);
    } else {
      this.editor.setContent("");
    }
  }

  function moveSectionUp() {
    const selectedList = this.sectionSelect.getSelected();
    if (selectedList == null || selectedList.length === 0) {
      return;
    }
    for (let i = 0; i < selectedList.length; i++) {
      const selectedOption = selectedList[i];
      // move option up
      const prev = selectedOption.previousSibling;
      if (prev != null) {
        dojo.place(selectedOption, prev, "before");
      } else {
        dojo.place(selectedOption, this.sectionSelect.domNode, "last");
      }
    }
  }

  function moveSectionDown() {
    const selectedList = this.sectionSelect.getSelected();
    if (selectedList == null || selectedList.length === 0) {
      return;
    }
    selectedList.reverse();
    for (let i = 0; i < selectedList.length; i++) {
      const selected = selectedList[i];
      const next = selected.nextSibling;
      if (next != null) {
        dojo.place(selected, next, "after");
      } else {
        dojo.place(selected, this.sectionSelect.domNode, "first");
      }
    }
  }

  function orderSections() {
    const newSectionArray = [];
    // go through all options to get the correct order
    dojo.query("option", this.sectionSelect.containerNode).forEach(n => {
      const id = parseInt(n.value);
      const content = this.getSectionById(id);
      newSectionArray.push(content);
    });
    this.webpageContent = newSectionArray;
  }

  /**
   * @Override
   */
  function onBeforeOk() {
    // sort content array
    dojo.hitch(this, orderSections)();
    // save content data
    this.selectedSection.data = this.editor.getContent();
    // remove internal id from content
    for (let i = 0; i < this.webpageContent.length; i++) {
      delete (this.webpageContent[i].id);
    }
  }

  function destroyEditor() {
    console.log("destroy editor");
    // destroy all section options
    const sectionSelect = this.sectionSelect.domNode;
    while (sectionSelect.hasChildNodes()) {
      sectionSelect.removeChild(sectionSelect.lastChild);
    }
    // destroy editor
    this.editor.remove();
    this.editor = null;
  }

  // inheritance
  wcms.navigation.EditContentDialog.prototype = new wcms.gui.AbstractDialog;

  wcms.navigation.EditContentDialog.prototype.createContent = createContent;
  wcms.navigation.EditContentDialog.prototype.destroyEditor = destroyEditor;
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

(function() {

  const editSectionText = "component.wcms.navigation.itemEditor.editContent.editSection.dialogTitleEdit";
  const newSectionText = "component.wcms.navigation.itemEditor.editContent.editSection.dialogTitleNew";

  const titleText = "component.wcms.navigation.itemEditor.editContent.editSection.title";
  const langText = "component.wcms.navigation.itemEditor.editContent.editSection.lang";

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
    if (!this.created)
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

    this.setTitle(editSection ? editSectionText : newSectionText);
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
