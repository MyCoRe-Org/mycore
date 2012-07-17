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

function buildDataStructure() {
    var structure = new Structure("root");
    var tree = dijit.byId("itemTree");
    var model = tree.model;

    model
            .getChildren(
                    model.root,
                    function(items) {
                        log("All items retrieved");
                        /* setting id and name of the structure */
                        structure.setId(items[0]._S._arrayOfAllItems[0].id);
                        structure.setName(items[0]._S._arrayOfAllItems[0].name);
                        structure
                                .setStructureType(items[0]._S._arrayOfAllItems[0].structureType);
                        log("Building structure...");
                        build(structure, items, 1);
                        log("Building structure...done");
                    }, function() {
                        log("Error occured in buildDataStructure()")
                    });
    return structure;
}

function build(structure, items, counter) {
    c = counter;
    for ( var i = 0; i < items.length; i++) {
        // log(i + " / "+ items.length);
        if (items[i].type == "item") {
            // create page
            // log("Creating Page -> " + items[i].id, items[i].name);
            var page = new Page(items[i].id, items[i].name);
            /* just the order of the files */
            page.setPhysicalOrder(c);
            if( typeof items[i].hide === "undefined" || !items[i].hide[0]){
            	++c;
            }
            /* the order within a structure (logical) */
            page.setLogicalOrder(i + 1);
            // set other properties if any
            page.setStructureType("page");
            // set order label
            page.setOrderLabel(items[i].orderLabel);
            // set the path attribute (path + filename)
            page.setPath(items[i].path);
            // set the hidden attribute
            page.setHide(typeof items[i].hide === "undefined" ? false : items[i].hide);
            // set the urn attribute
            if(!(typeof items[i].contentIds == "undefined")){
				page.setContentIds(items[i].contentIds);
			}
            // add the child
            structure.addChild(page);
        } else {
            if (items[i].type == "category") {
                /*
                 * create structure, add it to parent structure, call method
                 * with new structure and its children
                 */
                // log("Creating Structure -> " + items[i].id+" with "
                // +items[i].children.length + " children");
                var categ = new Structure(items[i].id);
                categ.setName(items[i].name);
                /* the order within a structure */
                categ.setLogicalOrder(i + 1);
                /* set the type of the structure as defined by the dfg */
                categ.setStructureType(items[i].structureType);
                structure.addChild(categ);
                build(categ, items[i].children, c);
            }
        }
    }
}