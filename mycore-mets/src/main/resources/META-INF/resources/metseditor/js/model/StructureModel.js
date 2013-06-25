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

function Structure(identifier) {
    this.id = identifier;
    this.name = identifier;
    this.children = [];
    this.logicalOrder;
    this.structureType;
    this.setId = function(identifier) {
        this.id = identifier;
    }

    this.getId = function() {
        return this.id;
    }

    this.setName = function(name) {
        this.name = name;
    }

    this.getName = function() {
        return this.name;
    }

    this.addChild = function(obj) {
        this.children.push(obj);
    }

    this.setLogicalOrder = function(orderToSet) {
        this.logicalOrder = orderToSet;
    }

    this.getLogicalOrder = function() {
        return this.logicalOrder;
    }

    this.setStructureType = function(type) {
        this.structureType = type;
    }

    this.getStructureType = function() {
        return this.structureType;
    }
}

function Page(identifier, name) {
    this.id = identifier;
    this.name = name;
    this.pageNumber;
    this.physicalOrder;
    this.orderLabel;
    this.logicalOrder;
    this.structureType;
    this.path;
    this.hide;
    this.contentIds;
    
    this.setHide = function(hideToSet) {
    	if(typeof this.hide === "undefined"){
    		this.hide = new Array();
    		this.hide.push(hideToSet);
    		return;
    	}
        this.hide[0] = hideToSet;
    }

    this.getHide = function() {
        return this.hide[0];
    }

    this.setPath = function(pathToSet) {
        this.path = pathToSet;
    }

    this.getPath = function() {
        return this.path;
    }

    this.setOrderLabel = function(labelToSet) {
        this.orderLabel = labelToSet;
    }

    this.getOrderLabel = function() {
        return this.orderLabel;
    }

    this.setPhysicalOrder = function(orderToSet) {
        this.physicalOrder = orderToSet;
    }

    this.getPhysicalOrder = function() {
        return this.physicalOrder;
    }

    this.setPageNumber = function(pn) {
        this.pageNumber = pn;
    }

    this.getPageNumber = function() {
        return this.pageNumber;
    }

    this.setLogicalOrder = function(orderToSet) {
        this.logicalOrder = orderToSet;
    }

    this.getLogicalOrder = function() {
        return this.logicalOrder;
    }

    this.setStructureType = function(type) {
        this.structureType = type;
    }

    this.getStructureType = function() {
        return this.structureType;
    }
    
	this.setContentIds = function(contentIds){
		this.contentIds = contentIds;	
	}
	
	this.getcontentIds = function(){
		return this.contentIds;	
	}
}

function Unit(name) {
    this.name = name;
    this.id = name;
    this.type = "category";
    this.structureType = "section";
    this.children = [];

    this.getName = function() {
        return this.name;
    }

    this.getId = function() {
        return this.id;
    }

    this.setId = function(idToSet) {
        this.id = idToSet;
    }

    this.setType = function(typeToSet) {
        this.type = typeToSet;
    }

    this.setStructureType = function(typeToSet) {
        this.structureType = typeToSet;
    }

    this.getStructureType = function() {
        return this.structureType;
    }

    this.getType = function() {
        return this.type;
    }

    this.getChildren = function() {
        return this.children;
    }
}