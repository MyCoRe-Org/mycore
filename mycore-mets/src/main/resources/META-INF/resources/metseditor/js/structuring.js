/* $Revision$ 
 * $Date$ 
 * $LastChangedBy$
 * Copyright 2010 - Th�ringer Universit�ts- und Landesbibliothek Jena
 *  
 * Mets-Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mets-Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mets-Editor.  If not, see http://www.gnu.org/licenses/.
 */

/* * * * * * * * * * * * * * * * * * * *  */
/* Functions for creating a new structure */
/* * * * * * * * * * * * * * * * * * * *  */

function showAddStructureDialog() {
    dijit.byId('addStructureDialog').show();
}

function toggleStructureButtons(event) {
    var tree = dijit.byId("itemTree");
    var selectedItem = tree.lastFocused.item;

    disableEditStructureButton(false);
    disableDeleteStructureButton(false);
    disableAddStructureButton(false);

    var l = tree.selectedItems.length;

    /*
     * if there more then one item Selected you cannot edit the structure
     * 
     */
    if (l > 1) {
        disableEditStructureButton(true);
    }

    for ( var j = 0; j < l; j++) {
        var selectedItem = tree.selectedItems[j];

        /*
         * if there is one Item in selected items you cannot edit the structure
         * and you cannot delete the structure
         */
        if (selectedItem.type == "item") {
            disableEditStructureButton(true);
            disableDeleteStructureButton(true);
        }

        /*
         * if there is one category in selected items you cannot add a category
         * 
         * if( selectedItem.type != "category"){
         * disableAddStructureButton(true); }
         */
    }

}

function disableAddStructureButton(flag) {
    var strctBtn = dijit.byId('toolbar1.addStructure');
    strctBtn.attr("disabled", flag);
    if (flag == true) {
        strctBtn.attr("iconClass", 'addStructureIconDisabled');
    } else {
        strctBtn.attr("iconClass", 'addStructureIconEnabled');
    }
}

function addStructureToTree() {
    var textfield = dijit.byId("structureName");
    var combo = dijit.byId("structureType");

    if (textfield.getValue() == "" || combo.item == null) {
        log("Textfield or combo is empty");
        return;
    }
    /* adding the item to the tree */
    var tree = dijit.byId("itemTree");

    var selectedTypeId = combo.item.id;
    var selectedTypeLabel = combo.item.name;

    var unit = new Unit(textfield.getValue());
    unit.setStructureType(selectedTypeId);
    unit.setId(generateUUID());

    var tracker = new SelectionTracker.getInstance();
    var selectedItem = tracker.getSelectedStructure();

    var createdElement;

    /* avoid adding structures to pages */
    if (selectedItem != null && selectedItem.type == "category") {
        var store = tree.model.store;
        createdElement = store.newItem(unit, {
            parent : selectedItem,
            attribute : 'children'
        });
        store.save({
            onComplete : function saveDone() {
                textfield.setValue("");
                log("Modifying tree store...done.");
            },
            onError : function saveFailed() {
                log("Modifying tree store...failed.");
            }
        });

    } else {
        /* add a new Folder to */
        var store = tree.model.store;
        createdElement = store.newItem(unit, {
            parent : getRootItemFromStore(),
            attribute : 'children'
        });

        store.save({
            onComplete : function saveDone() {
                textfield.setValue("");
                log("Modifying tree store...done.");
            },
            onError : function saveFailed() {
                log("Modifying tree store...failed.");
            }
        });
    }

    var pages = new Array();
    var selectedItems = tree.selectedItems;

    /* push all items to pages */
    log("backup Elements");
    for ( var k = 0; k < selectedItems.length; k++) {
        if (selectedItems[k].type == "item") {
            pages.push(selectedItems[k]);
            log(selectedItems[k]);
        }
    }

    /* delete all items from store */
    log("delete Elements");
    for ( var i = 0; i < pages.length; i++) {
        log(pages[i]);
        store.deleteItem(pages[i]);
    }

    /* save the changes(delete items) */
    store.save({
        onComplete : function saveDone() {
            log("Modifying tree store...done.");
        },
        onError : function saveFailed() {
            log("Modifying tree store...failed.");
        }
    });

    /* add the old pages to the new category */
    for ( var i = 0; i < pages.length; i++) {
        var newItem = {
            id : new String(pages[i].id),
            name : pages[i].name,
            orderLabel : pages[i].orderLabel,
            structureType : pages[i].structureType,
            type : pages[i].type,
            path : pages[i].path,
            contentIds : pages[i].contentIds,
            hide : pages[i].hide
        };
        store.newItem(newItem, {
            parent : createdElement,
            attribute : 'children'
        });
    }

    /* save the new added items */
    store.save({
        onComplete : function saveDone() {
            log("Modifying tree store...done.");
        },
        onError : function saveFailed() {
            log("Modifying tree store...failed.");
        }
    });

    /* disable the reverse button */
    toggleReverseButton();

    /* close/hide the dialog */
    dijit.byId("addStructureDialog").hide();

    combo.setValue("");
}

/* * * * * * * * * * * * * * * * * * * * * * * * */
/* Functions for modifying an existing structure */
/* * * * * * * * * * * * * * * * * * * * * * * * */

function disableEditStructureButton(flag) {
    var strctBtn = dijit.byId('toolbar1.editStructure');
    strctBtn.attr("disabled", flag);
    if (flag == true) {
        strctBtn.attr("iconClass", 'editStructureIconDisabled');
    } else {
        strctBtn.attr("iconClass", 'editStructureIconEnabled');
    }
}

function showEditStructureDialog() {
    log("showEditStructureDialog()");
    var tracker = new SelectionTracker.getInstance();
    var selStruct = tracker.getSelectedStructure();
    var selItem = tracker.getFrom();

    if (selStruct == null) {
        log("The item currently selected is not a category/structure");
        log("The edit structure dialog cannot be displayed");

        if (selItem != null && selItem.type == 'item') {
            log("The type currently selected item '" + selItem.type + "'");
            log("Displaying the editItemPropertiesDialog");
            displayEditItemDiaglog();
        }
        return;
    }

    if (selStruct == getRootItemFromStore()) {
        showEditDocTypeDialog();
        return;
    }

    configureEditDialog();
    dijit.byId('editStructureDialog').show();
}

function displayEditItemDiaglog() {
    log("displayEditItemDiaglog()");
    var dialog = dijit.byId("editItemDialog")
    var tracker = new SelectionTracker.getInstance();
    var selItem = tracker.getFrom();

    var textBox = dijit.byId("orderLabelTextBox");
    var commonLabelTextBox = dijit.byId("commonLabelTextBox");

    if (selItem.orderLabel != null) {
        textBox.setValue(selItem.orderLabel);
    }

    if (commonLabelTextBox.name != null) {
        commonLabelTextBox.setValue(selItem.name);
        commonLabelTextBox.attr('disabled', 'true');
    }

    var checkDisplay = dijit.byId("checkDisplay");
    var hide = selItem.hide;
    checkDisplay.set("checked", hide == "false" ? false : true);

    dialog.show();
}

function saveItemProperties() {
    log("saveItemProperties()");
    var tracker = new SelectionTracker.getInstance();
    var selItem = tracker.getFrom();
    var textBox = dijit.byId("orderLabelTextBox");
    var text = textBox.getValue();

    var commonLabelTextBox = dijit.byId("commonLabelTextBox");
    var commonLabelText = commonLabelTextBox.getValue();

    if (commonLabelText == null || commonLabelText.length == 0) {
        log("Label for item is not set. Please provide a label");
        return;
    }

    if (text != null) {
        selItem.orderLabel = text;
        textBox.setValue("");
    }

    if (commonLabelText != null) {
        var tree = dijit.byId("itemTree");
        var store = tree.model.store;
        selItem.name = commonLabelText;
        store.setValue(selItem, "name", selItem.name);
        commonLabelTextBox.setValue("");
    }

    var checkDisplay = dijit.byId("checkDisplay");
    var hide = checkDisplay.get("checked");
    selItem.hide = hide;
    checkDisplay.set("checked", false);

    dijit.byId("editItemDialog").hide();
    displayItemProperties();
}

function configureEditDialog() {
    log("configureEditDialog()");
    var tracker = new SelectionTracker.getInstance();
    var selStruct = tracker.getSelectedStructure();
    if (selStruct == null) {
        log("The item currently selected is not a category/structure");
        return;
    }
    var combo = dijit.byId("structureTypeEdit");
    var store = combo.store;

    store.fetchItemByIdentity({
        identity : selStruct.structureType,
        onItem : function(item, request) {
            combo.setDisplayedValue(item.name);
            combo.item = item;
        },
        onError : function(item, request) {
            log("Error fetching item from store");
        }
    });

    var textfield = dijit.byId("structureNameEdit");
    textfield.setValue(selStruct.name);
}

function saveEditedStructure() {
    log("saveEditedStructure()");
    var tracker = new SelectionTracker.getInstance();
    var selStruct = tracker.getSelectedStructure();

    var combo = dijit.byId("structureTypeEdit");
    var textfield = dijit.byId("structureNameEdit");

    if (textfield.getValue() == "" || combo.item == null) {
        console
                .log("Cannot save changes to selected item. Textfield or combo have no value.");
        return;
    }

    selStruct.structureType = combo.item.id;
    selStruct.name = textfield.getValue();

    var tree = dijit.byId("itemTree");
    var store = tree.model.store;

    store.setValue(selStruct, "name", selStruct.name);
    dijit.byId('editStructureDialog').hide();

    /* refresh status bar */
    displayItemProperties();
}

/* shows the edit document type and title dialog */
function showEditDocTypeDialog() {
    log("showEditDocTypeDialog()");
    configureEditDocTypeDialog();
    dijit.byId("setDocumentTypeAndTitleDialog").show();
}

/* sets default values if any available */
function configureEditDocTypeDialog() {
    log("configureEditDocTypeDialog()");
    var typeCombo = dijit.byId("docTypeCombo");
    var store = typeCombo.store;
    var rootItem = getRootItemFromStore();

    log("Got as root item");
    log(rootItem);

    store
            .fetchItemByIdentity({
                identity : rootItem.structureType,
                onItem : function(item, request) {
                    typeCombo.setDisplayedValue(item.name);
                    typeCombo.item = item;
                },
                onError : function(item, request) {
                    log("Error fetching item  from store in 'configureEditDocTypeDialog()'");
                    log("Item was:");
                    log(item);
                    log("Request was:");
                    log(request);
                }
            });

    var titleTextArea = dijit.byId("titleTextBox");
    titleTextArea.attr('displayedValue', rootItem.name);
}

/* saves document title and type to tree */
function saveDocTypeAndTitle() {
    log("saveDocTypeAndTitle()");
    var typeCombo = dijit.byId("docTypeCombo");
    var titleTextArea = dijit.byId("titleTextBox");

    if (typeCombo.item == null || typeCombo.item.id == "unit") {
        log("Could not save type of document, no valid type selected");
        return;
    }

    var titleEntered = titleTextArea.getValue();
    if (titleEntered.length == 0) {
        log("Could not save title of document, no title provided");
        return;
    }

    var tree = dijit.byId("itemTree");
    var store = tree.model.store;

    var rootItem = getRootItemFromStore();

    log("Setting title of document to \"" + titleEntered + "\"");
    store.setValue(rootItem, "name", titleEntered);

    log("Setting type of document to \"" + typeCombo.item.id + "\"");
    store.setValue(rootItem, "structureType", typeCombo.item.id);

    dijit.byId("setDocumentTypeAndTitleDialog").hide();
    /* refresh status bar */
    displayItemProperties();
}

/* returns the item representing the root from the store */
function getRootItemFromStore() {
    log("getRootItemFromStore()");

    var tree = dijit.byId("itemTree");
    var store = tree.model.store;
    var rootItem = null;

    var rootItemId = store._arrayOfTopLevelItems[0].id;

    /* fetch the root of the tree */
    store.fetchItemByIdentity({
        /* derivateId has been defined in StartMetsEditor.xsl */
        identity : rootItemId,
        onItem : function(item, request) {
            log("Fetching item " + rootItemId + " from store");
            rootItem = item;
        },
        onError : function(item, request) {
            log("Error fetching root item from store");
        }
    });
    return rootItem;
}

/*
 * Checks if treePart contains element (childs included). Element can be a
 * normal Item or a Category. TreePart should be a Category or the Root Element.
 */
function storeContainsElement(element, treePart) {
    log("StoreContainsElement()");

    var root = treePart;

    var pages = new Array();
    var sections = new Array();

    if (treePart.children == null) {
        log("StoreContainsElement(): Children are null return: false");
        return false;
    }

    if (getPagesForStructure(treePart.children, pages, sections)) {
        for ( var i = 0; i < pages.length; i++) {
            if (pages[i] == element) {
                log("StoreContainsElement(): found element return: true");
                return true;
            }
        }

        for ( var i = 0; i < sections.length; i++) {
            if (sections[i].id == element.id
                    || storeContainsElement(element, sections[i])) {
                log("StoreContainsElement(): found element return: true");
                return true;
            }
        }
    }

    log("StoreContainsElement(): cannot find element return: false");
    return false;
}

function disableDeleteStructureButton(flag) {
    var strctBtn = dijit.byId('toolbar1.deleteStructure');
    strctBtn.attr("disabled", flag);
    if (flag == true) {
        strctBtn.attr("iconClass", 'deleteStructureIconDisabled');
    } else {
        strctBtn.attr("iconClass", 'deleteStructureIconEnabled');
    }
}