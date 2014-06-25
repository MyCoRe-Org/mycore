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

function toggleReverseButton(){
	log("toggleReverseButton()");
	var tree = dijit.byId("itemTree");
	var model = tree.model;
	var itemList = null;

	model.getChildren(model.root, function(items) {
		var hasCategs = false;
		
		for(var i = 0; i < items.length; i++) {
			if(items[i].type == "item"){
				continue;
			}else if(items[i].type == "category"){
				hasCategs = true;
				break;
			}
		}
		disableReverseButton(hasCategs);
		
	}, function() {
		log("Error occured in toggleReverseButton()")
	});
}

function disableReverseButton(flag){
	log("disableReverseButton(" + flag + ")");
	var button = dijit.byId('toolbar1.reverse');
	button.attr("disabled", flag);
	
	if(flag == true) {
		button.attr("iconClass",'reverseOrderDisabled');
	}else {
		button.attr("iconClass",'reverseOrderEnabled');		
	}	
}


/* changes the order of the elements in a tree the 1st becomes the last one and vice versa */
function reverseTree() {
	log("reverseTree()");
	var tree = dijit.byId("itemTree");
	var model = tree.model;
	var itemList = null;

	model.getChildren(model.root, function(items) {
		itemList = new Array();
		for ( var i = 0; i < items.length; i++) {
			itemList.push(items[i]);
		}
	}, function() {
		log("Error occured in doFoliate()")
	});

	var store = model.store;
	/* remove all items from tree */
	for ( var i = 0; i < itemList.length; i++) {
		store.deleteItem(itemList[i]);
	}
	
	saveTreeStore(store);

	/* 
	 * simply adding the item does not work, 
	 * because the id of the prev. removed items remains within the store,
	 * thus we have to make a copy of the id string
	 * 
	 * */
	for (var i = itemList.length-1; i >= 0; i--) {
		var insertItem = {
						id: new String(itemList[i].id),
						path:itemList[i].path,
						name: itemList[i].name, 
						orderLabel:itemList[i].orderLabel,
						structureType:itemList[i].structureType,
						type:itemList[i].type
					   };
		
		store.newItem(insertItem, {parent : model.root,attribute : 'children'});
	}
	saveTreeStore(store);
}