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

function getIcon(item) {
	if (!item || item.type[0] == "category" || myStore.hasAttribute(item, "children")) {
		return "myFolder";
	}
	return "myItem"
}

function dndAccept(source, nodes){
  return true;
}

function isDropAllowed(node , source, position) {
	var item = dijit.getEnclosingWidget(node).item;
	/* only categories may accept a drop, when an item is placed over them */
	if(position == "over" && item.type == "item"){
		return false;	
	} else {
		return true;
	}
}