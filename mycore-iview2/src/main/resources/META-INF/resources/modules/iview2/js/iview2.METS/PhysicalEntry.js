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
	/**
	 * @private
	 * @name		contentId
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @type		string
	 * @description	urn of this file
	 */
	this._contentId = values.contentid || '';
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
	
	
	/**
	 * @public
	 * @function
	 * @memberOf	iview.METS.PhysicalEntry#
	 * @name 		getContentId
	 * @return		{string}
	 */
	function getContentId() {
		return this._contentId;
	}
	
	var prototype = iview.METS.PhysicalEntry.prototype;
	prototype.getHref = getHref;
	prototype.getID = getID;
	prototype.getOrder = getOrder;
	prototype.getOrderlabel = getOrderlabel;
	prototype.getContentId = getContentId;
})();