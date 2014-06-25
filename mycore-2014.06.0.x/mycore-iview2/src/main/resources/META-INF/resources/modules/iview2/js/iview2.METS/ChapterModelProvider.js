var iview = iview || {};

/**
 * @namespace	Contains all to METS belonging classes
 * @memberOf 	iview
 * @name		METS
 */
iview.METS = iview.METS || {};

/**
 * @name ModelProvider
 * @package iview.METS
 * @description reads in a default Model from the supplied METS Document
 */
iview.METS.ChapterModelProvider = function(metsDoc) {
	this._model = null;
	this._metsDoc = metsDoc || null;
};

( function() {
	/**
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

	/**
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
			orders[jQuery(this).attr("ID")] = toInt(jQuery(this).attr("ORDER"));
		});
		jQuery(getNodes(that._metsDoc, "mets:smLink", structLink)).each(function() {
			that._model.addContent(orders[jQuery(this).attr(attributeCheck("xlink:to"))],jQuery(this).attr(attributeCheck("xlink:from")));
		});
	}
	
	/**
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
	
	/**
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