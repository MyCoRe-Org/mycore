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
function trackSelection(selectedItem, source, event) {
	var tracker = new SelectionTracker.getInstance();
	if(tracker.getSelectedItems() == null){
		tracker.clearSelectedItems();
	}
		
	if ((event.ctrlKey == true || event.metaKey) == true && selectedItem.type == "item") {
	    tracker.setTo(selectedItem);
		tracker.setSelectedStructure(null);
		tracker.addSelectedItems(selectedItem);
		
	} else if(( !(event.ctrlKey || event.metaKey)) && selectedItem.type == "item") {
			tracker.setFrom(selectedItem);
			tracker.setSelectedStructure(null);
			tracker.clearSelectedStuff;
			tracker.addSelectedItems(selectedItem);
			
	} else if(selectedItem.type == "category"){
			if(tracker.selectedItems==null || (!event.ctrlKey && !event.shiftkey) ){
				tracker.clearSelectedItems();
				log("clearSelectedItems()");
			}
			log("add category to Selected Stuff");
			tracker.addSelectedItems(selectedItem);
			tracker.setSelectedStructure(selectedItem);
	} else {
		tracker.reset();
	}
	
	toggleFoliateButton();
}

var SelectionTracker = (function() {
	var instance = null;

	function PrivateConstructor() {
		var from = null;
		var to = null;
		var selectedStructure = null;
		var selectedItems = new Array();
		
		this.addSelectedItems = function(obj) {
			this.selectedItems.push(obj);
		}
		
		this.getSelectedItems = function() {
			return this.selectedItems;
		}
		
		this.clearSelectedItems = function() 
		{
			this.selectedItems
			this.selectedItems = new Array();
		}
		
		this.getSelectedStructure = function() {
			return this.selectedStructure;
		}
		
		this.setSelectedStructure = function(obj) {
			log("SelectionTracker.setSelectedStructure()");
			this.selectedStructure = obj;
		}
		
		this.getFrom = function() {
			return this.from;
		}
		this.setFrom = function(obj) {
			log("SelectionTracker.setFrom()");
			this.from = obj;
		}

		this.getTo = function() {
			return this.to;
		}

		this.setTo = function(obj) {
			log("SelectionTracker.setTo()");
			this.to = obj;
		}

		this.hasFrom = function() {
			if (this.from != null) {
				return true;
			}
			return false;
		}

		this.hasTo = function() {
			if (this.to != null) {
				return true;
			}
			return false;
		}

		this.reset = function() {
			log("SelectionTracker.reset()");
			this.from = null;
			this.to = null;
			this.selectedStructure = null;
		}
	}

	return new function() {
		this.getInstance = function() {
			if (instance == null) {
				instance = new PrivateConstructor();
				instance.constructor = null;
			}
			return instance;
		}
	}
})();
