/* $Revision: 3080 $ 
 * $Date: 2010-11-01 11:30:18 +0100 (Mon, 01 Nov 2010) $ 
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

/* returns an Array of invalid items (categories having not at least on child (a page)) */
function getInvalidItems(){
	console.log("getInvalidItems()");
	
	var tree = dijit.byId("itemTree");
	var model = tree.model;

	var invalidItems;
	
	model.getChildren(model.root, 
		function(items) {
			console.log("Validating tree");	
			invalidItems = new Array();
			performValidation(items, invalidItems);
			console.log("Validating tree...done");
		}, 
		function() {
			console.log("Error occured in performValidation()")
	    });
	return invalidItems;
}

/* actually performs the validation */
function performValidation(items, invalidItems){
	for(var i = 0; i < items.length; i++) {
		if(items[i].type == "item"){
			continue;
		} else {
			if(items[i].type == "category"){
				if(items[i].children.length == 0){
					console.log("Found invalid item " + items[i].id);
					invalidItems.push(items[i]);
				} 
				performValidation(items[i].children, invalidItems);
			}
		}
	}
}

function containsPages(anItem){
	var children = anItem.children;
	for(var c = 0; c < children.length; c++){
		if(children[c].type == "item"){
			return true;
		}
	}
	return false;
}

/* displays the invalid items to the user */
function displaySaveFailedDialog(invalidItems){
	console.log("displaySaveFailedDialog()");
	var msg = "";
	
	for(var i = 0; i < invalidItems.length; i++){
		msg += invalidItems[i].name;
		if(i + 1 < invalidItems.length){
			msg +=", ";
		}
	}
	document.getElementById('affectedItems').innerHTML = msg;  
	var dialog = dijit.byId("saveFailedDialog");
	dialog.show();
}

/* saves the tree/structure */
function save(){
   console.log("save()");
   var invalidItems = getInvalidItems(); 
   
   if(invalidItems.length > 0){
	   console.log("Mets tree is in an invalid state");
	   displaySaveFailedDialog(invalidItems);
	   return;
   }
   var tree = buildDataStructure();
   console.log(dojo.toJson(tree));

   var data = dojo.toJson(tree);
   
   console.log("Submitting to Server...");
   console.log(data);
   dojo.xhrPost({
	   url: webApplicationBaseURL + "servlets/SaveMetsServlet",
	   handleAs: "text",
	   postData: "jsontree=" + data + "&derivate=" + derivateId,
	   load: function(response) {
	   		console.log('Mets successfully saved');
   		},
   		error: function(err, ioArgs){
   			console.log('Please log in to use Mets Editor');
   			var secondDlg = new dijit.Dialog({
   				title: "Zugriff verweigert",
   				style: "width: 300px"
   			});
	   	    secondDlg.attr("content", "Mets konnte nicht erzeugt und gespeichert werden. Bitte loggen Sie sich ein und versuchen Sie es erneut.");
	   	    secondDlg.show();
   	    }
   });
}