/* $Revision: 2988 $ 
 * $Date: 2010-09-23 12:09:04 +0200 (Thu, 23 Sep 2010) $ 
 * $LastChangedBy: shermann $
 * Copyright 2010 - Thüringer Universitäts- und Landesbibliothek Jena
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

	if (selectedItem.type == "item") {
		disableAddStructureButton(true);
		disableEditStructureButton(false);
		disableDeleteStructureButton(true);
	} else {
		disableAddStructureButton(false);
		/* the root of the tree must not be edited */
		var tree = dijit.byId("itemTree");
		var model = tree.model;

		if (model.root == selectedItem) {
			disableEditStructureButton(true);
			disableDeleteStructureButton(true);
		} else {
			disableEditStructureButton(false);
			disableDeleteStructureButton(false);
		}
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
		return;
	}
	/* adding the item to the tree */
	var tree = dijit.byId("itemTree");

	var selectedTypeId = combo.item.id;
	var selectedTypeLabel = combo.item.name;

	var unit = new Unit(textfield.getValue());
	unit.setStructureType(selectedTypeId);
	unit.setId(generateUUID());

	var selectedItem = tree._getSelectedItemAttr();

	/* avoid adding structures to pages */
	if (myStore.hasAttribute(selectedItem, "children") == true) {
		var store = tree.model.store;
		store.newItem(unit, {
			parent : selectedItem,
			attribute : 'children'
		});
		store.save( {
			onComplete : function saveDone() {
				textfield.setValue("");
				console.log("Modifying tree store...done.");
			},
			onError : function saveFailed() {
				console.log("Modifying tree store...failed.");
			}
		});

	} else {
		/* create a structure and add the selected items to the new structure */
		console
				.log("Illegal operation. Adding a structure to a page is not permitted.");
	}
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
	console.log("showEditStructureDialog()");
	var tracker = new SelectionTracker.getInstance();
	var selStruct = tracker.getSelectedStructure();
	var selItem = tracker.getFrom();
	
	if (selStruct == null) {
		console.log("The item currently selected is not a category/structure");
		console.log("The edit structure dialog cannot be displayed");
		
		if(selItem != null && selItem.type == 'item'){
			console.log("The type currently selected item '" + selItem.type +"'");
			console.log("Displaying the editItemPropertiesDialog");
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

function displayEditItemDiaglog(){
	console.log("displayEditItemDiaglog()");
	var dialog = dijit.byId("editItemDialog")
	var tracker = new SelectionTracker.getInstance();
	var selItem = tracker.getFrom();
	
	var textBox = dijit.byId("orderLabelTextBox");
	var commonLabelTextBox = dijit.byId("commonLabelTextBox");
	
	if(selItem.orderLabel != null){
		textBox.setValue(selItem.orderLabel);
	}
	
	if(commonLabelTextBox.name != null){
		commonLabelTextBox.setValue(selItem.name);
	}
	
	dialog.show();
}

function saveItemProperties(){
	console.log("saveItemProperties()");
	var tracker = new SelectionTracker.getInstance();
	var selItem = tracker.getFrom();
	var textBox = dijit.byId("orderLabelTextBox");
	var text = textBox.getValue(); 
	
	var commonLabelTextBox = dijit.byId("commonLabelTextBox");
	var commonLabelText = commonLabelTextBox.getValue();
	
	if(commonLabelText == null || commonLabelText.length == 0){
		console.log("Label for item is not set. Please provide a label");
		return;
	}
	
	if(text != null){
		selItem.orderLabel = text;
		textBox.setValue("");
	}
	

	if(commonLabelText != null){
		selItem.name = commonLabelText;
		
		var tree = dijit.byId("itemTree");
		var store = tree.model.store;
		store.setValue(selItem, "name", selItem.name);
		
		commonLabelTextBox.setValue("");

	}
	dijit.byId("editItemDialog").hide();
	displayItemProperties();	
}

function configureEditDialog() {
	console.log("configureEditDialog()");
	var tracker = new SelectionTracker.getInstance();
	var selStruct = tracker.getSelectedStructure();
	if (selStruct == null) {
		console.log("The item currently selected is not a category/structure");
		return;
	}
	var combo = dijit.byId("structureTypeEdit");
	var store = combo.store;

	store.fetchItemByIdentity( {
		identity : selStruct.structureType,
		onItem : function(item, request) {
			combo.setDisplayedValue(item.name);
			combo.item = item;
		},
		onError : function(item, request) {
			console.log("Error fetching item from store");
		}
	});

	var textfield = dijit.byId("structureNameEdit");
	textfield.setValue(selStruct.name);
}

function saveEditedStructure() {
	console.log("saveEditedStructure()");
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
	console.log("showEditDocTypeDialog()");
	configureEditDocTypeDialog();
	dijit.byId("setDocumentTypeAndTitleDialog").show();
}

/* sets default values if any available */
function configureEditDocTypeDialog() {
	console.log("configureEditDocTypeDialog()");
	var typeCombo = dijit.byId("docTypeCombo");
	var store = typeCombo.store;
	var rootItem = getRootItemFromStore();

	console.log("Got as root item");
	console.log(rootItem);

	store.fetchItemByIdentity( {
		identity : rootItem.structureType,
		onItem : function(item, request) {
			typeCombo.setDisplayedValue(item.name);
			typeCombo.item = item;
		},
		onError : function(item, request) {
			console.log("Error fetching item  from store in 'configureEditDocTypeDialog()'");
			console.log("Item was:");
			console.log(item);
			console.log("Request was:");
			console.log(request);
		}
	});

	var titleTextArea = dijit.byId("titleTextBox");
	titleTextArea.attr('displayedValue', rootItem.name);
}

/* saves document title and type to tree */
function saveDocTypeAndTitle() {
	console.log("saveDocTypeAndTitle()");
	var typeCombo = dijit.byId("docTypeCombo");
	var titleTextArea = dijit.byId("titleTextBox");

	if (typeCombo.item == null || typeCombo.item.id == "unit") {
		console.log("Could not save type of document, no valid type selected");
		return;
	}

	var titleEntered = titleTextArea.getValue();
	if (titleEntered.length == 0) {
		console.log("Could not save title of document, no title provided");
		return;
	}

	var tree = dijit.byId("itemTree");
	var store = tree.model.store;

	var rootItem = getRootItemFromStore();

	console.log("Setting title of document to \"" + titleEntered + "\"");
	store.setValue(rootItem, "name", titleEntered);

	console.log("Setting type of document to \"" + typeCombo.item.id + "\"");
	store.setValue(rootItem, "structureType", typeCombo.item.id);

	dijit.byId("setDocumentTypeAndTitleDialog").hide();
	/* refresh status bar */
	displayItemProperties();
}

/* returns the item representing the root from the store */
function getRootItemFromStore() {
	console.log("getRootItemFromStore()");

	var tree = dijit.byId("itemTree");
	var store = tree.model.store;
	var rootItem = null;

	var rootItemId = store._arrayOfTopLevelItems[0].id;
	
	/* fetch the root of the tree */
	store.fetchItemByIdentity( {
		/* derivateId has been defined in StartMetsEditor.xsl */
		identity : rootItemId,
		onItem : function(item, request) {
			console.log("Fetching item " + rootItemId + " from store");
			rootItem = item;
		},
		onError : function(item, request) {
			console.log("Error fetching root item from store");
		}
	});
	return rootItem;
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