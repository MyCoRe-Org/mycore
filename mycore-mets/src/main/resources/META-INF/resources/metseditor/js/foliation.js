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

/**
 * Shows the foliation dialog
 * */
function showFoliationDialog() {
	var tracker = new SelectionTracker.getInstance();
	log("showFoliationDialog()");
	document.getElementById('labelFrom').innerHTML = tracker.getFrom().path;
	document.getElementById('labelTo').innerHTML = tracker.getTo().path;
	dijit.byId('foliationDialog').show();
}

/**
 * Toggles the follation button in the toolbar
 * */
function toggleFoliateButton() {
	var tracker = new SelectionTracker.getInstance();
	var foliateButton = dijit.byId('toolbar1.foliation');

	if (tracker.hasFrom() && tracker.hasTo()) {
		foliateButton.attr("disabled", false);
		foliateButton.attr("iconClass", 'foliateIconEnabled');
	} else {
		foliateButton.attr("disabled", true);
		foliateButton.attr("iconClass", 'foliateIconDisabled');
	}
}

/**
 * Validates the user input in the foliation dialog, if the input is valid,
 * it calles doFoliate()
 * */
function validateFoliationSettings(){
	var isValid = true;
	var typeCombo = dijit.byId('foliationTypeCombo');
	var startsWithBox = dijit.byId('startValueTextBox');
	
	var startVal = startsWithBox.getValue();
	var typeVal = typeCombo.getValue(); 
	
	if(! startVal){
		isValid = false;
	}
	
	if(typeCombo.item == null){
		isValid = false;
	}
	
	if(isValid){
		doFoliate(startVal, typeCombo.item.id);
	}
}

function doFoliate(start, ty ) {
	log("doFoliate()");
	var tracker = new SelectionTracker.getInstance();
	var from = tracker.getFrom();
	var to = tracker.getTo();
	var model = dijit.byId("itemTree").model;
	var labelProvider = new OrderLabelProvider(start, ty); 
	var checkbox = dijit.byId("enableReverseFoliation")
	
	model.getChildren(model.root, 
			function(items) {
				if(checkbox.checked == false){
					log("performFoliatonPreOrder(" + from.id + ", " + to.id +", items[], false, " + labelProvider.type + " OrderLabelProvider)...");
					performFoliatonPreOrder(from, to, items, false, labelProvider);	
				}else{
					log("performFoliatonPostOrder(" + from.id + ", " + to.id +", items[], false, " + labelProvider.type + " OrderLabelProvider)...");
					performFoliatonPostOrder(from, to, items, false, labelProvider);
				}
			},
			function() {
				log("Error occured in doFoliate()")
			});

	tracker.reset();
	dijit.byId('foliationDialog').hide();
	
	/* refresh status bar */
	displayItemProperties();
}

/* actually performs the foliation */
function performFoliatonPreOrder(from, to, items, doFoliate, labelProvider){
	/* for some reason we must carry the flag in a variable of its own for each recursive call */
	flag = doFoliate;
	for(var i = 0; i < items.length; i++) {
		if(items[i].type == "item"){
			if(items[i].id == from.id){
				flag = true;
				log("Foliation has started...");
			}
			if(flag){
				items[i].orderLabel = labelProvider.getNext();
				log(items[i].id +" -> " + items[i].orderLabel);
			}
			if(items[i].id == to.id){
				flag = false;
				log("Foliating has ended...");
				return;
			}
		} else {
			if(items[i].type == "category"){
				performFoliatonPreOrder(from, to, items[i].children, flag, labelProvider);
			}
		}
	}
}

/* actually performs the foliation */
function performFoliatonPostOrder(from, to, items, doFoliate, labelProvider){
	/* for some reason we must carry the flag in a variable of its own for each recursive call */
	flag = doFoliate;
	for(var i = items.length-1; i >= 0; i--) {
		if(items[i].type == "item"){
			if(items[i].id == to.id){
				flag = true;
				log("Foliation has started...");
			}
			if(flag){
				items[i].orderLabel = labelProvider.getNext();
				log(items[i].id +" -> " + items[i].orderLabel);
			}
			if(items[i].id == from.id){
				flag = false;
				log("Foliating has ended...");
				return;
			}
		} else {
			if(items[i].type == "category"){
				performFoliatonPostOrder(from, to, items[i].children, flag, labelProvider);
			}
		}
	}
}

/**
 * @param startIndex first number the provider will deliver
 * @param type the type of foliation e.g. roman, arabic and so on
 * */
function OrderLabelProvider(startIndex, ty) {
	log("Initialising OrderLabelProvider");
	log("OrderLabelProvider startIndex = " + startIndex);
	log("OrderLabelProvider type = " + ty);
	this.startIndex = startIndex;
	this.type = ty;
	this.postfix = null;

	if(this.type == "leafnumberVerso" || this.type == "romanVersoLowercase" || this.type == "romanVersoUppercase"){
		this.postfix = "v";
	} else if(this.type == "leafnumberRecto" || this.type == "romanRectoLowercase" || this.type == "romanRectoUppercase"){
			this.postfix = "r";
	} else if(this.type == "leafnumberA"){
			this.postfix = "a";
	} else if(this.type == "leafnumberB"){
			this.postfix = "b";
	} else if(this.type == "leafnumberB"){
            this.postfix = "b";
    }
	
	this.getNext = function(){
		if(this.type == "romanUppercase"){
			return this.asRoman(this.startIndex++);
		}
		
		if(this.type == "romanLowercase"){
			var temp = this.asRoman(this.startIndex++);
			return temp.toLowerCase();
		}
		
		if(this.type == "leafnumberVerso" || this.type == "leafnumberRecto"){
			return this.asLeaf(this.startIndex);
		}
		
		if(this.type == "romanVersoLowercase" || this.type == "romanRectoLowercase"){
            var temp = this.asLeafRoman(this.startIndex, "lowercase");
		    return temp;
        }
		
		if(this.type == "romanVersoUppercase" || this.type == "romanRectoUppercase"){
            var temp = this.asLeafRoman(this.startIndex, "uppercase");
            return temp;
        }
		
		if(this.type == "leafnumberA" || this.type == "leafnumberB"){
			return this.asLeafSermon(this.startIndex);
		}
		
		if(this.type == "cover"){
			return this.asCover(this.startIndex++);
		}
		
		if(this.type == "arabic"){
			return this.startIndex++;
		}
		log("ERROR - Unsupported Foliation type");
	}
	
	this.asRoman = function(N, s, b, a, o, t){
		  t=N/1e3|0;N%=1e3;
		  for(s=b='',a=5;N;b++,a^=7)
		    for(o=N%a,N=N/a^0;o--;)
		      s='IVXLCDM'.charAt(o>2?b+N-(N&=~1)+(o=1):b)+s;
		  return Array(t+1).join('M')+s;
	}
	
	this.asLeaf = function(anInt){
		var toReturn = anInt + this.postfix;
		if(this.postfix == "v"){
			this.postfix = "r";
			this.startIndex++;
		} else {
			this.postfix = "v";
		}
		return toReturn;
	}
	
	this.asLeafRoman = function(anInt, mode){
        var upOrLow = this.asRoman(this.startIndex);
        if(mode == "uppercase"){
            upOrLow = upOrLow.toUpperCase();
        } else {
            upOrLow = upOrLow.toLowerCase();    
        }
	    
	    var toReturn = upOrLow + this.postfix;
        
        if(this.postfix == "v"){
            this.postfix = "r";
            this.startIndex++;
        } else {
            this.postfix = "v";
        }
        return toReturn;
    }
	
	this.asLeafSermon = function(anInt){
		var toReturn = anInt + this.postfix;
		if(this.postfix == "a"){
			this.postfix = "b";
			this.startIndex++;
		} else {
			this.postfix = "a";
		}
		return toReturn;
	}
	
	this.asCover = function(anInt){
		return "U" + anInt;
	}
}
