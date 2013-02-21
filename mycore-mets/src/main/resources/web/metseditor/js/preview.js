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

function onTreeLoaded(){
	log("Tree loaded");
	loadingMsgContainer = document.getElementById("c1");
	loadingMsg = document.getElementById("loadingMsg");
	if(loadingMsg != null){
		loadingMsgContainer.removeChild(loadingMsg);
	}
	toggleReverseButton();
}

function loadPreviewImage(selectedItem, source, event){
	log("loadPreviewImage()");
	/* the div containing the images */
	var previewBaseURL = webApplicationBaseURL + "servlets/MCRTileCombineServlet/MID/" + derivateId + "/";
	var container = document.getElementById('previewImageContainer');
	
	var manyImagesContainerKey = "manyImagesContainer";
	var previewImageKey = "previewImage"; 
	
	/* remove all old images */
	var olddiv = document.getElementById(manyImagesContainerKey);
	if(olddiv != null){
		container.removeChild(olddiv);
	}
	
	/* remove a single preview image */
	olddiv = null;
	olddiv = document.getElementById(previewImageKey);
	if(olddiv != null){
		container.removeChild(olddiv);
	}
	
	/* reference to tree */
	var tree = dijit.byId("itemTree");
	
	/* show the selected image(s) */
	if(selectedItem.type == "item"){
		var imgURL = previewBaseURL + escape(selectedItem.path);
		var previewImage = document.createElement('img');
		previewImage.id = previewImageKey;
		previewImage.src = imgURL;
		//set size
		previewImage.height = '512';
		
		container.appendChild(previewImage);
	} 
	/* avoid to display all images contained within a derivate */
	else if (selectedItem != tree.model.root) {
		var divWithManyImg = document.createElement('div');
		divWithManyImg.id = manyImagesContainerKey;
		container.appendChild(divWithManyImg);
		if(typeof selectedItem.children != "undefined"){
			for(var i = 0; i < selectedItem.children.length; i++){
				var currentItem = selectedItem.children[i];
				
				if(currentItem.type == "item"){
					var newImg = document.createElement('img');
					newImg.src = previewBaseURL + escape(currentItem.path);
					newImg.height = '512';
					log("Adding <img> element to document "+ newImg.src);
					divWithManyImg.appendChild(newImg);
				}
			}
		}
	} 
}