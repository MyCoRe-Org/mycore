/* $Revision: 2948 $ 
 * $Date: 2010-09-08 15:12:34 +0200 (Wed, 08 Sep 2010) $ 
 * $LastChangedBy: shermann $
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

function handleKeyDown(event){
	log("handleKeyDown()");
	if(event.keyCode == dojo.keys.DELETE) {
		deleteStructure();
	}
}

function deleteStructure(){
	log("deleteStructure()");
	var tree = dijit.byId("itemTree");
	
	var tracker = new SelectionTracker.getInstance();
	var selectedItem = tracker.getSelectedStructure();

	if(selectedItem == null){
		log("No structure selected, delete canceled");
		return;
	}
	
	var pages = new Array();
	var sections = new Array();
	
	getPagesForStructure(selectedItem.children, pages, sections);
		
	var store = tree.model.store;

	/* delete pages */
	for(var i = 0; i < pages.length; i++){
		store.deleteItem(pages[i]);
	}
	
	/* delete child folders*/
	for(var i = 0; i < sections.length; i++){
		store.deleteItem(sections[i]);
	}
	
	/* delete originally selected section */ 
	store.deleteItem(selectedItem);
	
	saveTreeStore(store);
	
	var root = getRootItemFromStore();

	/* add the items to the root of the tree */
	for(var i = 0; i < pages.length; i++){
		var newItem = {
				id: new String(pages[i].id), 
				name: pages[i].name, 
				orderLabel:pages[i].orderLabel,
				structureType:pages[i].structureType,
				type:pages[i].type,
				path:pages[i].path
			   };
		store.newItem(newItem,{parent:root, attribute:'children'});
	}
	saveTreeStore(store);
	tracker.reset();
	toggleStructureButtons();
	/* may be we can now enable the reverse button */
	toggleReverseButton();
}

/* returns all pages a structure has */
function getPagesForStructure(children, pages, sections){
	for(var i = 0; i < children.length; i++){
		if(children[i].type == 'item'){
			pages.push(children[i]);
		}else if(children[i].type == 'category'){
					sections.push(children[i]);		
					getPagesForStructure(children[i].children, pages, sections);
				}
		
	}
}