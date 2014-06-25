var iview = iview || {};

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
	 * @param		{integer} pos Integer which says which element we want to retrieve (attr sort)
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
	
	function hasNext() {
		return (typeof this._order[this._curPos+1] !== "undefined");
	}
	
	function hasPrevious() {
		return (typeof this._order[this._curPos-1] !== "undefined");
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
			var oldPos = this._curPos;
			this._curPos = value;
			jQuery(this).trigger("select.METS", {'old': oldPos, 'new': value});
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
		var old = this._curPos;
		while(this.hasNext()){
			this._curPos++;
		}
		if(old!=this._curPos){
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
		var old = this._curPos;
		while(this.hasPrevious()){
			this._curPos--;
		}
		if(old!=this._curPos){
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
		if (typeof this._order[this._curPos] !== "undefined") {
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
	prototype.hasPrevious = hasPrevious;
	prototype.hasNext = hasNext;
	prototype.setPrevious = setPrevious;
	prototype.iterator = iterator;
})();