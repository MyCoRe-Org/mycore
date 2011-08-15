/**
 * @namespace
 * @name		iview
 */
var iview = iview || {};
/**
 * @namespace	Package for Util Classes
 * @memberOf 	iview
 * @name		utils
 */
iview.utils = iview.utils || {};
//TODO PhysicalModel expand physicalModel so that we can generate from it the chapter view so that the chapterModel gets useless
/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.utils
 * @name 		SimpleIterator
 * @description Allows it to get java like the next object within a given Collection or Object
 */
iview.utils.SimpleIterator = function(objectIterate) {
	/**
	 * @private
	 * @name		items
	 * @memberOf	iview.utils.SimpleIterator#
	 * @type		Array
	 * @description	items to iterate over
	 */
	this._items = [];
	/**
	 * @private
	 * @name		curPos
	 * @memberOf	iview.utils.SimpleIterator#
	 * @type		integer
	 * @description	current Position within the elements
	 */
	this._curPos = 0;
	var that = this;
	
	//add all entries to our
	jQuery(objectIterate).each(function(pos, entry) {
		if (entry != null) {
			that._items.push(jQuery.extend({}, entry));
		}
	})
};
//TODO what style is better this one or the one like permalink
(function() {
	/**
	 * @function
	 * @name		hasNext
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description proofs if there are any items remaining within the collection (in forward direction)
	 *  note once the end is reached this function can return true as soon as a getPrevious() was called
	 * @return		{boolean} which tells if there is any further object or not
	 */
	function hasNext() {
		if (this._curPos < this._items.length) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * @function
	 * @name		next
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	gives the user the next object while moving forward. This function will return null objects
	 *  if the end is reached
	 * @return		{arbitrary type} entry which is next in iteration
	 */
	function next() {
		if (this._curPos < this._items.length) {
			return this._items[this._curPos++];
		} else {
			return null;
		}
	}
	
	/**
	 * @function
	 * @name		hasPrevious
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	proofs if there are any items remaining within the collection (in backward direction)
	 *  note once the beginning is reached this function can return true as soon as a getNext() was called
	 * @return		{boolean} which tells if there is a previous object or not
	 */
	function hasPrevious() {
		if (this._curPos > 0) {
			return true;
		} else {
			return false;
		}
	}
	/**
	 * @function
	 * @name		previous
	 * @memberOf	iview.utils.SimpleIterator#
	 * @description	gives the user the next object while moving backward. This function will return null objects
	 *  if the beginning is reached
	 * @return		{arbitrary type} entry which is previous in interation
	 */
	function previous() {
		if (this._curPos > 0) {
			return this._items[this._curPos--];
		} else {
			return null;
		}
	}
	
	var prototype = iview.utils.SimpleIterator.prototype;
	prototype.hasNext = hasNext;
	prototype.next = next;
	prototype.hasPrevious = hasPrevious;
	prototype.previous = previous;
})();

/**
 * @namespace	Contains all to METS belonging classes
 * @memberOf 	iview
 * @name		METS
 */
iview.METS = iview.METS || {};
/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.METS
 * @name 		PhysicalModel
 * @description Represents the files and order within a METS File
 */
iview.METS.PhysicalModel = function () {
	/**
	 * @private
	 * @name		entries
	 * @memberOf	iview.METS.PhysicalModel#
	 * @type		associative array
	 * @description	Array to access from physicalID the requested Object
	 */
	this._entries = [];
	/**
	 * @private
	 * @name		order
	 * @memberOf	iview.METS.PhysicalModel#
	 * @type		array
	 * @description	Array to access from position within document the requested Object
	 */
	this._order = [];
	/**
	 * @private
	 * @name		curPos
	 * @memberOf	iview.METS.PhysicalModel#
	 * @type		integer
	 * @description	currently selected page within Model
	 */
	this._curPos = -1;
	/**
	 * @private
	 * @name		pageCount
	 * @memberOf	iview.METS.PhysicalModel#
	 * @type		integer
	 * @description	amount of pages within current Model
	 */
	this._pageCount = 0;
};

(function() {
	/**
	 * @public
	 * @function
	 * @name		addPage
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description adds a new element to the model
	 * @param		{object} values properties of the new page to be added
	 * @param		{string} values.ID physicalID of the Entry to add
	 * @param		{URL} values.href URL to the entry
	 * @param		{integer} values.order Position of the Entry within the collection
	 */
	function addPage(values) {
		var entry = new iview.METS.PhysicalEntry(values);
		this._entries[values.ID] = entry;
		this._order[values.order] = entry;
		this._pageCount++;
	}
	
	/**
	 * @public
	 * @function
	 * @name		getEntryAt
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description returns the physicalentry which is located at the given Position, will return the entry at
	 *  position 0 if no valid number was supplied
	 * @param		{integer} pos Integer which says which element we want to retrieve
	 * @return		{PhysicalEntry} entry who is located at the given pos
	 */
	function getEntryAt(pos) {
		return this._order[toInt(pos)];
	}
	
	/**
	 * @public
	 * @function
	 * @name		getCurPos
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description returns the currently selected entry within the model
	 * @return		{integer} currently selected entry
	 */
	function getCurPos() {
		return this._curPos;
	}
	
	/**
	 * @public
	 * @function
	 * @name		getPosition
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description returns the order id which belongs to the given physicalID. Returns -1
	 *  if no matching physical ID was found
	 * @param		{string} physID where we want to gain the order number from
	 * @return		{integer} at which position the physicalID is located or -1 if no matching ID was found
	 */
	function getPosition(physID) {
        var res = '';
        for(var i in this._entries){
            if(this._entries[i].getHref() == physID){
                res = this._entries[i].getOrder();
            }
        }
        if (typeof res === "undefined") {
            return -1;
        } else {
            return res;
        }
    }
	
	/**
	 * @public
	 * @function
	 * @name		setPosition
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description sets the current Pointer to the supplied entry/position
	 * @param		{integer} value value Integer entry (its position) where the next entry shall be read from
	 */
	function setPosition(value) {
		value = toInt(value);
		value = (value < 0)? -value:value;
		value = (value > this._order.length)? this._order.length:value;
		if (value != this._curPos) {
			this._curPos = value;
			jQuery(this).trigger("select.METS", {'old': this._curPos, 'new': value});
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setEnd
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description sets the current Pointer to the last element within the entries
	 */
	function setEnd() {
		if (this._curPos != this._order.length) {
			var old = this._curPos;
			this._curPos = this._order.length;
			jQuery(this).trigger("select.METS", {'old': old, 'new': this._curPos});
		}
	}

	/**
	 * @public
	 * @function
	 * @name		setStart
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description sets the current Pointer to the last element within the entries
	 */
	function setStart() {
		if (this._curPos != 0) {
			var old = this._curPos;
			this._curPos = 0;
			jQuery(this).trigger("select.METS", {'old': old, 'new': this._curPos});
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setNext
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description increase the current Position but do not return the Object, can be read later by getCurrent
	 * @return		{integer} position which is now the current one
	 */
	function setNext() {
		if (this._curPos < this._order.length) {
			this._curPos++;
			jQuery(this).trigger("select.METS", {'old':this._curPos-1, 'new':this._curPos});
			return this._curPos;
		} else {
			return this._order.length;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		getNext
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description reads the element from the currently selected position and sets the pointer one position further
	 * @return		{PhysicalEntry} entry which is located at the current pointer position or null if the end is reached
	 */
	function getNext() {
		if (this._curPos < this._order.length) {
			this._curPos++;
			jQuery(this).trigger("select.METS", {'old':this._curPos-1, 'new':this._curPos});
			return this._order[this._curPos];
		} else {
			return null;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setPrevious
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description decrease the current Position but do not return the Object, can be read later by getCurrent
	 * @return		{integer} position which is now the current one
	 */
	function setPrevious() {
		if (this._curPos > 0) {
			this._curPos--;
			jQuery(this).trigger("select.METS", {'old':this._curPos+1, 'new':this._curPos});
			return this._curPos;
		} else {
			return 0;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		getPrevious
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description reads the element from the currently selected position and sets the pointer one position further
	 * @return		{PhysicalEntry} entry which is located at the current pointer position or null if the end is reached
	 */
	function getPrevious() {
		if (this._curPos > 0) {
			this._curPos--;
			jQuery(this).trigger("select.METS", {'old':this._curPos+1, 'new':this._curPos});
			return this._order[this._curPos];
		} else {
			return null;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		getCurrent
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description reads the currently selected entry in the physicalModel
	 * @return		{PhysicalEntry} entry which is located at the current pointer position or null if the end is reached
	 */
	function getCurrent() {
		if (this._curPos > 0 && this._curPos <= this._order.length) {
			//check if we're within the array bounds and return the matching object
			return this._order[this._curPos];
		} else {
			return null;
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		getNumberOfPages
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description returns the current amount of stored physicalEntries. Attention that this number hasn't to be the highest used number
	 * @return		{integer} which represents the current amount of entries within this Object
	 */
	function getNumberOfPages() {
		return this._pageCount;
	}
	
	/**
	 * @public
	 * @function
	 * @name		iterator
	 * @memberOf	iview.METS.PhysicalModel#
	 * @description returns an iterator to iterate over the stored entries
	 * @return		{SimpleIterator} iterator who allows it to iterate over the elements
	 */
	function iterator() {
		return new iview.utils.SimpleIterator(this._order);
	}
	
	var prototype = iview.METS.PhysicalModel.prototype;
	prototype.addPage = addPage;
	prototype.setPosition = setPosition;
	prototype.setEnd = setEnd;
	prototype.setStart = setStart;
	prototype.getNumberOfPages = getNumberOfPages;
	prototype.getCurPos = getCurPos;
	prototype.getCurrent = getCurrent;
	prototype.getEntryAt = getEntryAt;
	prototype.getNext = getNext;
	prototype.setNext = setNext;
	prototype.getPosition = getPosition;
	prototype.getPrevious = getPrevious;
	prototype.setPrevious = setPrevious;
	prototype.iterator = iterator;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.METS
 * @name 		PhysicalEntry
 * @description represents a single Page from the METS File/Derivate
 * @param		{object} values all parameters the entry needs to know
 * @param		{URL} [values.href=""] location where the file resides
 * @param		{string} [values.id=""] physicalID of this file
 * @param		{integer} [values.order=0] position of this file within the document
 * @param		{string} [values.orderlabel=""] label which is showed instead of the arabic numbering
 */
iview.METS.PhysicalEntry = function(values) {
	/**
	 * @private
	 * @name		href
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @type		URL
	 * @description	location where the file resides
	 */
	this._href = values.href || "";
	/**
	 * @private
	 * @name		ID
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @type		string
	 * @description	physicalID of this file
	 */
	this._id = values.ID || "";
	/**
	 * @private
	 * @name		order
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @type		integer
	 * @description	position of this file within the document
	 */
	this._order = toInt(values.order);
	/**
	 * @private
	 * @name		orderlabel
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @type		string
	 * @description	label which is showed instead of the arabic numbering
	 */
	this._orderlabel = values.orderlabel || '';
};

(function() {
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @name 		getHref
	 * @return		{URL}
	 */
	function getHref() {
		return this._href;
	}
	
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @name 		getID
	 * @return		{string}
	 */
	function getID() {
		return this._id;
	}
	
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @name 		getOrder
	 * @return		{integer}
	 */
	function getOrder() {
		return this._order;
	}
	
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @name 		getOrderlabel
	 * @return		{string}
	 */
	function getOrderlabel() {
		return this._orderlabel;
	}
	
	var prototype = iview.METS.PhysicalEntry.prototype;
	prototype.getHref = getHref;
	prototype.getID = getID;
	prototype.getOrder = getOrder;
	prototype.getOrderlabel = getOrderlabel;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.METS
 * @name 		PhysicalModelProvider
 * @description reads in a default PhysicalModel from the supplied METS Document
 */
iview.METS.PhysicalModelProvider = function(doc) {
	this._model = null;
	this._doc = doc || null;
	this._links = {};
};

(function() {

	/**
	 * @private
	 * @function
	 * @memberOf	iview.METS.PhysicalModelProvider
	 * @name 		getHrefs
	 * @description retrieves the href for all physicalIDs so it can be added to the physicalEntries
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 */
	function getHrefs(that) {
		//get all available fileGrps (where files are stored)
		var groups = getNodes(that._doc, 'mets:fileGrp');
		for (var i = 0; i < groups.length; i++) {
			//use the master filegrp
			if (jQuery(groups[i]).attr('USE').toLowerCase() == 'master') {
				//Foreach file in the master area retrieve it's url and add it to the links object
				/*
				 * @structure:that._links
				 * 		Array [
				 * 			physicalID:	href
				 * 		] 
				 */
				var files = getNodes(groups[i], 'mets:file');
				jQuery(files).each(function(pos, element) {
					that._links[jQuery(element).attr('ID')] = jQuery(jQuery(element).contents().filter(function() {
						if ((this).nodeName.indexOf("FLocat") != -1 && jQuery(this).attr('LOCTYPE') == 'URL') {
							return 1;
					}
					})[0]).attr(attributeCheck('xlink:href'));
				});
				return;
			}
	}
	}
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalModelProvider
	 * @name 		createModel
	 * @description creates a new Model if none exists or returns the existing one
	 */
	function createModel() {
		if (this._model == null) {
			var that = this;
			this._model = new iview.METS.PhysicalModel();
			var structures = getNodes(this._doc, 'mets:structMap');
			getHrefs(this);
			for (var i = 0; i < structures.length; i++) {
				if (jQuery(structures[i]).attr("TYPE") == "PHYSICAL") {
					var entries = getNodes(this._doc, "mets:div", structures[i]);
					jQuery(entries).each(function() {
						//Foreach page which is contained within the physical area retrieve its information and add it to the physical model
						if (jQuery(this).attr("TYPE").toLowerCase() == "page") {
							that._model.addPage({
								'ID': jQuery(this).attr('ID'),
								'href': that._links[jQuery(jQuery(this.childNodes).filter(function() {
									if (this.nodeName.indexOf('fptr') && this.nodeType == 1) {
										return 1;
									}
								})[0]).attr('FILEID')],
								'order': jQuery(this).attr('ORDER'),
								'orderlabel': jQuery(this).attr('ORDERLABEL') || ''
							});
						}
				    });
					delete(that._links);//Drop the no longer needed array
					break;
				}
			}
		}
		return this._model;
	}

	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalModelProvider
	 * @name 		setModel
	 * @description sets the document from which the Model will be created
	 * @param		{DOM-XML-Document} document to create Model from
	 */
	function setDocument(doc) {
		this._doc = doc;
	}

	var prototype = iview.METS.PhysicalModelProvider.prototype;
	prototype.setDocument = setDocument;
	prototype.createModel = createModel;
})();
//TODO Chapter METS Information could be merged into PhysicalModel
/*********************************************
 * 				Chapter METS				 *
 *********************************************/
/*
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
	
	/*
	 * @description Adds to the Model a new Page with the supplied data within element
	 * @param element Object which contains all needed informations to create a new Page within the Model
	 */
	function addPage(element) {
		var page = new iview.METS.ChapterPage(element, this);
		this._entries.push(page);
		addHash.call(this, page);
	}

	/*
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
	
	/*
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
	
	/*
	 * @description to easily add entries of subchapter to the global Hashlist give childs Hashlist
	 */
	function getContainer() {
		return this;
	}
	
	/*
	 * @description adds to the list of Elements within the list the given entry if it's not already there,
	 *  otherwise throws an alert. Within this Hashlist it's easy to find out if a given Element exists
	 *  within the Model
	 * @param entry Object with function getID which will be added to the Hashlist
	 */
	function addHash(entry) {
		if (getHash.call(this, entry.getID()) != null) {
			if (typeof console != "undefined") {
				console.log("Entry with the ID "+entry.getID() +" already exists. Element will not be added to List");
			}
			return;
		}
		this._hashList[entry.getID()] = entry;
	}
	
	/*
	 * @description Proves if the supplied Hash within hash is already in use within the Model,
	 *  which is supplied through that
	 * @param hash Hash which shall be added to the hashList
	 * @return null if the Element doesn't exists, else it returns the content of the given Hash Position
	 */
	function getHash(hash) {
		//check if Object with this hash is available
		return (typeof this._hashList[hash] === "undefined")? null:this._hashList[hash];
	}
	
	/*
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

/*
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

/*
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

/*
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

	/*
	 * @description Adds to the Model a new Page with the supplied data within element
	 * @param element Object which contains all needed informations to create a new Page within the Model
	 */
	function addPage(element) {
		var page = new iview.METS.ChapterPage(element, this);
		this._entries.push(page);
		this._container.addHash(page);
	}
	
	/*
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

/*
 * @name ModelProvider
 * @package iview.METS
 * @description reads in a default Model from the supplied METS Document
 */
iview.METS.ChapterModelProvider = function(metsDoc) {
	this._model = null;
	this._metsDoc = metsDoc || null;
};

( function() {
	/*
	 * @description Iterate through the current Nodes childs and add them to the model. If some of these childs 
	 *  contain it's own childs, getStructure is recursively executed for these as well 
	 * @param metsNode the current Node which childs are added, if a branch is detected getStructure is called
	 *  to iterate these branched entries
	 * @param parentEntry Optional the Parent Node where to add the current entries within the parent 
	 */
	function generateModelFromMets(metsNode, parentEntry) {
		//Filter all Entries which are no Elementnodes, as they're no "data"
		var childNodes = jQuery(metsNode.childNodes).filter(function() {return this.nodeType == 1});
		var label;
		var logid;
		for (var i = 0; i < childNodes.length; i++) {
			var child = childNodes[i];
			var type = jQuery(child).attr("TYPE").toLowerCase();
			if (type != "page") {
				//If we're on a branch, move the branch down and add all Elements at the current Position within it's parent
				generateModelFromMets(child, parentEntry.addBranch({'label':jQuery(child).attr("LABEL"), 'logid':jQuery(child).attr("ID")}));			
			} else {
				//Just an ordinary Entry so add it to the current Level
				logid = jQuery(child).attr("ID");
				label = jQuery(child).attr("LABEL");
				parentEntry.addPage({'label':label,'logid': logid});
			}
		}
	}

	/*
	 * @description	creates the physical to chapter logic links of the model
	 */
	function addPhysicalToChapterLinks(that) {
		var structLink = getNodes(that._metsDoc, "mets:structLink")[0];
		var physicals = jQuery(getNodes(that._metsDoc, "mets:structMap")).filter(function () {
			return jQuery(this).attr("TYPE") == "PHYSICAL"
		});
		physicals = getNodes(that._metsDoc, "mets:div", physicals[0]);
		var orders = [];
		jQuery(physicals).each(function() {
			orders[jQuery(this).attr("ID")] = jQuery(this).attr("ORDER");
		});
		jQuery(getNodes(that._metsDoc, "mets:smLink", structLink)).each(function() {
			that._model.addContent(orders[jQuery(this).attr(attributeCheck("xlink:to"))],jQuery(this).attr(attributeCheck("xlink:from")));
		});
	}
	
	/*
	 *@description Starts the Reading Process of the METS file
	 */	
	function createModel() {
		if (this._model == null) {
			this._model = new iview.METS.ChapterModel();
			//As the Mets file can contain multiple structMap Tags find the one we're using
			var structures = getNodes(this._metsDoc, "mets:structMap");
			for (var i = 0; i < structures.length; i++) {
				if (jQuery(structures[i]).attr("TYPE").toLowerCase() == "logical") {
					generateModelFromMets(structures[i], this._model);//Correct one found, start reading of Document
					addPhysicalToChapterLinks(this);
					break;
				}
				
			}
//			alert("No Matching Structure Info was delivered with the given XML Document.");
		}
		return this._model;
	}
	
	/*
	 * @description Starts the Reading Process of the given XML document, at the end the model is filled with all Data which the XML contained
	 * @param document the XML Document which contains the METS Data which is read  
	 */
	function setDocument(metsDoc) {
		this._metsDoc = metsDoc;	
	}
	
	var prototype = iview.METS.ChapterModelProvider.prototype;
	prototype.createModel = createModel;
	prototype.setDocument = setDocument;
})();