/* $Revision: 2658 $ 
 * $Date: 2010-06-01 13:17:58 +0200 (Tue, 01 Jun 2010) $ 
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

function saveTreeStore(store){
	console.log("saveTreeStore()")
	if(store == null){
		console.log("Cannot save store - is null");
		return;
	}
	store.save({onComplete: 
		   function saveDone() {
			   console.log("Modifying tree store...done.");
	   		}, 
	   		onError: function saveFailed() {
	   			console.log("Modifying tree store...failed.");
	   		}
	   	});
}