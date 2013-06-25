var iview = iview || {};

/**
 * @namespace	Contains all to METS belonging classes
 * @memberOf 	iview
 * @name		METS
 */
iview.METS = iview.METS || {};

//TODO Chapter METS Information could be merged into PhysicalModel
/*********************************************
 * 				Chapter METS				 *
 *********************************************/
/**
 * @name 		Model
 * @proto		Object
 * @description Represents a structure area within a METS File
 */
iview.METS.ChapterModel = function(element) {
	this._selected = null;
	this._entries = [];
	this._hashList = [];
	this._containedIn = [];/*holds the smlinks to xlink:to as id and as content the object it belongs to,
	*  so that pages which are not displayed within the chapter structure explicitly will be mapped to the
	*  chapter where they lay in
	*/
};

( function() {
	
	function getEntries() {
		return this._entries;
	}
	
	/**
	 * @description Adds to the Model a new Page with the supplied data within element
	 * @param element Object which contains all needed informations to create a new Page within the Model
	 */
	function addPage(element) {
		var page = new iview.METS.ChapterPage(element, this);
		this._entries.push(page);
		addHash.call(this, page);
	}

	/**
	 * @description Adds to the Model a new Branch, the branch will be filled with the supplied data within element
	 * @param element Object which contains all needed informations to create a branch
	 * @return Object Branch which was created, so Subentries can be added to this branch as well
	 */
	function addBranch(element) {
		var branch = new iview.METS.ChapterBranch(element, this);
		this._entries.push(branch);
		addHash.call(this, branch);
		return branch;
	}
	
	/**
	 * @description Sets the given entry as new selected entry within the Model, if the supplied entry
	 *  isn't valid nothing happens. Else all Listeners will be notified about the changed Selection by
	 *  raising an Event of the type 'select.METS' with the previously selected entry
	 *  in old and the new Selection within new. If no entry was selected before, old holds null
	 * @param logid the ID/Hash of the Element which is the new selected one
	 */
	function setSelected(logid) {
		logid = this._containedIn[logid].getID();
		var newSelected = getHash.call(this, logid);
		if (newSelected != null) {
			var oldSelected = (this._selected != null)? this._selected.getOrder(): null;
			var oldID = (this._selected != null)? this._selected.getID(): null;
			this._selected = newSelected;
			jQuery(this).trigger("select.METS", {"old": oldSelected, "new": newSelected.getOrder()});//, "oldID":oldID, "newID":newSelected.getID()});
		}
	}
	
	/**
	 * @description to easily add entries of subchapter to the global Hashlist give childs Hashlist
	 */
	function getContainer() {
		return this;
	}
	
	/**
	 * @description adds to the list of Elements within the list the given entry if it's not already there,
	 *  otherwise throws an alert. Within this Hashlist it's easy to find out if a given Element exists
	 *  within the Model
	 * @param entry Object with function getID which will be added to the Hashlist
	 */
	function addHash(entry) {
		if (getHash.call(this, entry.getID()) != null) {
			log("Entry with the ID "+entry.getID() +" already exists. Element will not be added to List");
			return;
		}
		this._hashList[entry.getID()] = entry;
	}
	
	/**
	 * @description Proves if the supplied Hash within hash is already in use within the Model,
	 *  which is supplied through that
	 * @param hash Hash which shall be added to the hashList
	 * @return null if the Element doesn't exists, else it returns the content of the given Hash Position
	 */
	function getHash(hash) {
		//check if Object with this hash is available
		return (typeof this._hashList[hash] === "undefined")? null:this._hashList[hash];
	}
	
	/**
	 * @description	as it's possible that many physical pages link to one single logical structure we need
	 *  to map those physicals to the logical ones. This function offers the basic functionality to display
	 *  those relations. To achieve this the given childname which represents a physical page to the given
	 *  parentID which represents a physical page
	 * @param		orderNo a physical METS-Page ordernumber which will be mapped to the given parent (logical) ID
	 * @param		parentID a (valid) logical METS Div id where the given physical one will link to
	 */
	function addContent(orderNo, parentID) {
		var entry = getHash.call(this, parentID);
		this._containedIn[orderNo] = entry;
		if (entry.getOrder() > orderNo) {
			entry.setOrder(orderNo);
		}
	}
	
	var prototype = iview.METS.ChapterModel.prototype;
	prototype.getEntries = getEntries;
	prototype.addPage = addPage;
	prototype.addBranch = addBranch;
	prototype.getContainer = getContainer;
	prototype.addHash = addHash;
	prototype.getHash = getHash;
	prototype.setSelected = setSelected;
	prototype.addContent = addContent;
})();




/**
 * @name 		ChapterEntry
 * @proto		Object
 * @abstract	no direct instantiation
 * @description ancestor Object for Page and Chapter Object
 */
iview.METS.ChapterEntry = function() {
	this._parent = null;
};

iview.METS.ChapterEntry.prototype = {
	getParent: function() {
		return this._parent;
	}
};

/**
 * @name 		ChapterPage
 * @proto 		iview.chapter.ChapterEntry
 * @description Represents a single Page within a METS-Document
 * @structure
 * 		Object {
 * 			string:	_label,					//label of the object
 * 			string: _id,					//logicalID of the object
 * 			string/DOMObject: _parent,		//parentNode of the Chapterpage so we can easily navigate up
 * 		}
 */
iview.METS.ChapterPage = function(properties, parent) {
	this._label = properties.label || properties.logid || "";
	this._id = properties.logid || "";
	this._order = Number.MAX_VALUE;
	this._parent = parent;
}; iview.METS.ChapterPage.prototype = new iview.METS.ChapterEntry;

( function() {
		
	function getID() {
		return this._id;
	}

	function getLabel() {
		return this._label;
	}
		
	function getOrder() {
		return this._order;
	}
	
	function setOrder(value) {
		this._order = value;
	}
	
	var prototype = iview.METS.ChapterPage.prototype;
	prototype.getID = getID;
	prototype.getLabel = getLabel;
	prototype.getOrder = getOrder;
	prototype.setOrder = setOrder;
})();

/**
 * @name		ChapterBranch
 * @proto		iview.METS.ChapterEntry
 * @description	Represents a branch within a METS-structure area
 * @structure
 * 		Object {
 * 			Array:				_entries,			//holds all elements which this object contains
 * 			ChapterPage:		_entry,				//holds the element for the branch which contains informations like caption or logid
 * 			String/DOMObject:	_parent
 * 			Object:				_container			//the first element in the tree, this element is needed to add hashes to the whole tree for fast access to it's elements
 * 		}
 */
iview.METS.ChapterBranch = function(entry, parent) {
	this._entries = [];
	this._entry = new iview.METS.ChapterPage(entry, "", this);
	this._parent = parent;
	//Set the root Element so we're able to add it elements to the global Hashlist
	this._container = parent.getContainer();
}; iview.METS.ChapterBranch.prototype = new iview.METS.ChapterEntry;

( function() {

	/**
	 * @description Adds to the Model a new Page with the supplied data within element
	 * @param element Object which contains all needed informations to create a new Page within the Model
	 */
	function addPage(element) {
		var page = new iview.METS.ChapterPage(element, this);
		this._entries.push(page);
		this._container.addHash(page);
	}
	
	/**
	 * @description Adds to the Model a new Branch, the branch will be filled with the supplied data within element
	 * @param element Object which contains all needed informations to create a branch
	 * @return Object Branch which was created, so Subentries can be added to this branch as well
	 */
	function addBranch(element) {
		var branch = new iview.METS.ChapterBranch(element, this);
		this._entries.push(branch);
		this._container.addHash(branch);
		return branch;	
	}
	
	function getEntries() {
		return jQuery.extend( {}, this._entries);
	}
	
	function getInfo() {
		return jQuery.extend( {}, this._entry);
	}
	
	function reset() {
		this._entries = [];
		this._entry = null;
	}
	
	function getLabel() {
		return this._entry.getLabel();
	}
	
	function getID() {
		return this._entry.getID();
	}
	
	function getOrder() {
		return this._entry.getOrder();
	}
	
	function setOrder(value) {
		this._entry.setOrder(value);
	}
	
	function getContainer() {
		return this._container;
	}
	
	var prototype = iview.METS.ChapterBranch.prototype;
	prototype.addPage = addPage;
	prototype.addBranch = addBranch;
	prototype.getEntries = getEntries;
	prototype.getInfo = getInfo;
	prototype.reset = reset;
	prototype.getLabel = getLabel;
	prototype.getID = getID;
	prototype.getOrder = getOrder;
	prototype.setOrder = setOrder;
	prototype.getContainer = getContainer;
})();