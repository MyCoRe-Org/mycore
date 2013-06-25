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

function handleKeyDown(event) {
	log("handleKeyDown()");
	if (event.keyCode == dojo.keys.DELETE) {
		deleteStructure();
	}
}

function deleteStructure() {
	log("deleteStructure()");
	var tree = dijit.byId("itemTree");
	var tracker = new SelectionTracker.getInstance();

	log("Elements to delete : " + tree.selectedItems.length);
	var l = tree.selectedItems.length;
	for ( var j = 0; j < l; j++) {
		var selectedItem = tree.selectedItems[j];
		log("try to delete " + selectedItem);
		
		/* Checks selected element is (not) empty */
		if (selectedItem == null) {
			log("No structure selected, jumping to next element");
			continue;
		}
		
		/* checks selected element is (not) a category */
		if(selectedItem.type != "category"){
			log("Selected item is no category, jumping to next element");
			continue;
		}
		
		/* Checks selected element is (not) root element */
		if (selectedItem == getRootItemFromStore()) {
			log("Selected Item is Root Element, jumping to next element");
			continue;
		}

		var root = getRootItemFromStore();
		/* Checks selected element (not) exist */
		if (!storeContainsElement(selectedItem, root)) {
			log("Store doesnt Contains this Element, jumping to next element");
			continue;
		}

		log(selectedItem);
		var pages = new Array();
		var sections = new Array();
		var store = tree.model.store;
		
		if (getPagesForStructure(selectedItem.children, pages, sections)) {
			/* delete pages */
			for ( var i = 0; i < pages.length; i++) {
				store.deleteItem(pages[i]);
			}
			/* delete child folders */
			for ( var i = 0; i < sections.length; i++) {
				store.deleteItem(sections[i]);
			}
		}
		
		/* delete originally selected section */
		store.deleteItem(selectedItem);

		saveTreeStore(store);

		/* add the items to the root of the tree */
		for ( var i = 0; i < pages.length; i++) {
			var newItem = {
				id : new String(pages[i].id),
				name : pages[i].name,
				orderLabel : pages[i].orderLabel,
				structureType : pages[i].structureType,
				type : pages[i].type,
				path : pages[i].path
			};
			store.newItem(newItem, {
				parent : root,
				attribute : 'children'
			});
		}
		saveTreeStore(store);

	}

	tracker.reset();
	toggleStructureButtons();
	/* may be we can now enable the reverse button */
	toggleReverseButton();
}

/* returns all pages a structure has */
function getPagesForStructure(children, pages, sections) {
	if (children == null) {
		log("getPagesForStructure(): children are null return: false");
		return false;
	}
	for ( var i = 0; i < children.length; i++) {
		if (children[i].type == 'item') {
			pages.push(children[i]);
		} else if (children[i].type == 'category') {
			sections.push(children[i]);
			getPagesForStructure(children[i].children, pages, sections);
		}

	}
	return true;
}