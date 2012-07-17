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
	 * @description retrieves the href for all physicalIDs so it can be added to the physicalEntries. Hidden files excluded.
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
					// checks if the fileid of the filegrp could be found in the struct link
					var fileName = jQuery(this).attr('ID');
					var isHidden = typeof this._doc !== "undefined" && jQuery(that._doc).find("*[ID=phys_" + fileName + "]").length != 1;
					
					if(!isHidden){
						that._links[fileName] = jQuery(jQuery(element).contents().filter(function() {
							if ((this).nodeName.indexOf("FLocat") != -1 && jQuery(this).attr('LOCTYPE') == 'URL') {
								return 1;
							} 
							
						})[0]).attr(attributeCheck('xlink:href'));
					} 
					
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
								'contentid': jQuery(this).attr('CONTENTIDS') || '',
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