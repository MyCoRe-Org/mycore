var iview = iview || {};
iview.chapter = iview.chapter || {};
//TODO Add Event like: creation done
/*
 * @package iview.chapter
 * @description Modeldata for internal METS Representation 
 */

/*
 * @name 		Model
 * @proto		Object
 * @description
 */
iview.chapter.Model = function(element) {
	this._selected = null;
	this._entries = [];
	this.onevent = new iview.Event(this);
	this._hashList = [];
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
		var page = new iview.chapter.METSPage(element.label, element.dmdid, this);
		this._entries.push(page);
		addHash(page);
	}

	/*
	 * @description Adds to the Model a new Branch, the branch will be filled with the supplied data within element
	 * @param element Object which contains all needed informations to create a branch
	 * @return Object Branch which was created, so Subentries can be added to this branch as well
	 */
	function addBranch(element) {
		var branch = new iview.chapter.METSChapter(element.label, this);
		this._entries.push(branch);
		return branch;
	}
	
	/*
	 * @description Sets the given entry as new selected entry within the Model, if the supplied entry
	 *  isn't valid nothing happens. Else all Listeners will be notified about the changed Selection by
	 *  calling the onevent The Type of the Event is 'selected' with the previously selected entry(-id)
	 *  in old and the new Selection within new. If no entry was selected before, old holds null
	 * @param dmdid the ID/Hash of the Element which is the new selected one
	 */
	function setSelected(dmdid) {
		var newSelected = getHash(dmdid, this);
		if (newSelected != null) {
			var oldSelected = (this._selected != null)? this._selected.getID(): null;
			this._selected = newSelected;
			this.onevent.notify({"type" : 'selected', "old": oldSelected, "new": newSelected.getID()});
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
		if (getHash(entry.getID(), this) != null) {
			alert("Entry with this ID already exists. Element will not be added to List");
			return;
		}
		this._hashList[entry.getID()] = entry;
	}
	
	/*
	 * @description Proves if the supplied Hash within hash is already in use within the Model,
	 *  which is supplied through that
	 * @param hash Hash which shall be added to the hashList
	 * @param that Model where to check the Hashlist for the given hash
	 * @return null if the Element doesn't exists, else it returns the content of the given Hash Position
	 */
	function getHash(hash, that) {
		//check if Object with this hash is available
		return (typeof that._hashList[hash] === "undefined")? null:that._hashList[hash];
	}
	
	iview.chapter.Model.prototype.getEntries = getEntries;
	iview.chapter.Model.prototype.addPage = addPage;
	iview.chapter.Model.prototype.addBranch = addBranch;
	iview.chapter.Model.prototype.getContainer = getContainer;
	iview.chapter.Model.prototype.addHash = addHash;
	iview.chapter.Model.prototype.setSelected = setSelected;
})();

/*
 * @name 		METSEntry
 * @proto		Object
 * @description
 */
iview.chapter.METSEntry = function(labl) {
	this._parent = null;
};

iview.chapter.METSEntry.prototype = {
	setLabel: function(labl) {
		this._label = labl;
	},
		
	getLabel: function() {
		return this._label;
	},
	
	getParent: function() {
		return this._parent;
	}
};

/*
 * @name 		METSPage
 * @proto 		iview.chapter.METSEntry
 * @description
 */
iview.chapter.METSPage = function(labl, id, parent) {
	var caption = labl || id;
	this._label = labl || "";
	this._id = id || "";
	this._parent = parent;
}; iview.chapter.METSPage.prototype = new iview.chapter.METSEntry;

( function() {
	function setID(id) {
		this._id = id;
	}
		
	function getID() {
		return this._id;
	}

	function setLabel(labl) {
		this._label = labl;
	}
		
	function getLabel() {
		return this._label;
	}
	
	iview.chapter.METSPage.prototype.setID = setID;
	iview.chapter.METSPage.prototype.getID = getID;
	iview.chapter.METSPage.prototype.getLabel = getLabel;
	iview.chapter.METSPage.prototype.setLabel = setLabel;
})();

/*
 * @name		METSChapter
 * @proto		iview.chapter.METSEntry
 * @description	
 */
iview.chapter.METSChapter = function(entry, parent) {
	this._chapter = true;
	this._entries = [];
	this._entry = new iview.chapter.METSPage(entry, "", this);
	this._parent = parent;
	//Set the root Element so we're able to add it elements to the global Hashlist
	this._container = parent.getContainer();
}; iview.chapter.METSChapter.prototype = new iview.chapter.METSEntry;

( function() {

//	function addEntries(elements) {
//		if (!(typeof elements === undefined)) {
//			var that = this;
//			if (jQuery.isArray(elements)) {
//				jQuery.each(elements, function(index, element) { that._entries.push(element)});
//			} else {
//				addEntry(element);
//			}
//		}
//	}	

	/*
	 * @description Adds to the Model a new Page with the supplied data within element
	 * @param element Object which contains all needed informations to create a new Page within the Model
	 */
	function addPage(element) {
		var page = new iview.chapter.METSPage(element.label, element.dmdid, this);
		this._entries.push(page);
		this._container.addHash(page);
	}
	
	/*
	 * @description Adds to the Model a new Branch, the branch will be filled with the supplied data within element
	 * @param element Object which contains all needed informations to create a branch
	 * @return Object Branch which was created, so Subentries can be added to this branch as well
	 */
	function addBranch(element) {
		var branch = new iview.chapter.METSChapter(element.label, this);
		this._entries.push(branch);
		return branch;	
	}
	
	function getEntries() {
		return jQuery.extend( {}, this._entries);
	}
	
	function getInfo() {
		return jQuery.extend( {}, this._entry);
	}
	
	function setInfo(element) {
		this._entry = element;
	}
	
	function reset() {
		this._entries = [];
		this._entry = null;
	}
	
	function getLabel() {
		return this._entry.getLabel();
	}
	
	function setLabel(labl) {
		this._entry.setLabel(labl);	
	}
	
	function getContainer() {
		return this._container;
	}
	
//	iview.METSChapter.prototype.addEntries = addEntries;
	iview.chapter.METSChapter.prototype.addPage = addPage;
	iview.chapter.METSChapter.prototype.addBranch = addBranch;
	iview.chapter.METSChapter.prototype.getEntries = getEntries;
	iview.chapter.METSChapter.prototype.getInfo = getInfo;
	iview.chapter.METSChapter.prototype.reset = reset;
	iview.chapter.METSChapter.prototype.getLabel = getLabel;
	iview.chapter.METSChapter.prototype.setLabel = setLabel;
	iview.chapter.METSChapter.prototype.getContainer = getContainer;
})();

/********************************************************
 ********************************************************
 ********************************************************/
/*
 * @package iview.chapter
 * @description View to Display Data as jQuery Tree
 */
iview.chapter.View = function() {
	this._treeData;//Stores the Treedata to display(the jsTree Model)
	this._visible = false;
	this._tree = jQuery.tree.create();//The jsTree
	this._selected = null;//stores the currently selected page and enables reset if Chapters will be selected
	this.onevent = new iview.Event(this);//One Event to rule them all
	this.notifyOthers = true;//onselect Event was caused by Controller calls, so don't handle them
	this.dealEvent = true;//onselect Event was caused by Selection-changes within source code, so don't handle them as well
	this._parent = null;//where will the tree be connected to
};

( function() {
	
	/*
	 * @description creates the basic background Datastructure to Display the Data as jQuery Tree
	 */
	function initTree() {
		var that = this;
		this._treeData = {
			"data": {
				"type": 'json',
				"opts": {
					"static": []
				}				
			},
			"types": {
				"default": {
					"draggable": false
				}
			},
			"ui": {
					"selected_parent_close": false
			},
			"callback": {
				"onselect": function(node, tree) {
					//To prevent endless loop check if the event is caused by some resets(chapter Selections)
					//or as result of some external call
					if (!that.dealEvent) { that.dealEvent = true; return;}
					if (!that.notifyOthers) { that.notifyOthers = true; return;}
					
					var chapter = (jQuery(node).attr("dmdid") === undefined)? true:false;
					
					//add endless loop Prevention
					that.dealEvent = false;
					//Deselect the User clicked Element and lets see what the Model reports us(if it does)
					if (that._selected != null) {
						//if something was previously selected switch back to it
						that._tree.select_branch(that._selected);
					} else {
						//nothing was selected before, simply deselect the selection
						that._tree.deselect_branch(node);
					}
					//If that element isn't selected already and is no chapter notify all Listeners about the User interaction
					if (that._selected != node && !chapter) {
						that.onevent.notify({"type":'selected', 
							"old":jQuery(that._selected).attr("dmdid"), 
							"new":jQuery(node).attr("dmdid")});
					}
					
				}
			}
		};
	};

	/*
	 * @description adds a new branch to the supplied parentNode(mostly a branch itself), as childNode
	 * @param branch Object which will be added to the Tree at the supplied parentNode
	 * @param parentNode Object where the branch will be added as childNode
	 * @return returns the just added Branch so that the User can work further on it
	 * (to add there itself new Child Entries, or whatever)
	 */
	function addBranch(branch, parentNode) {
		var currentBranch = { "data": branch.label, "children": []};
		if (parentNode == this._treeData) {
			parentNode.data.opts["static"].push(currentBranch);
		} else {
			parentNode.children.push(currentBranch);
		}
		return currentBranch;
	}
	
	/*
	 * @description adds a simple page to the supplied parentNode as childNode
	 * @param page Object which will be added to the Tree at the supplied parentNode
	 * @param parentNode Object where the page will be added as childNode
	 */
	function addPage(page, parentNode) {
		parentNode.children.push({"data":page.label, "attributes": { "dmdid": page.dmdid}});
	}
	
	/*
	 * @description adds the View to it's parent and creates the HTML Structure to display the Tree.
	 *  Therefore makes it reachable for the user
	 */
	function addTree(parent) {
		var that = this;

		//set some Style depending Properties so that the view can work properly;		
		var chapter = jQuery('<div>').addClass("chapter").appendTo(parent);
		//Adding the Content area which holds the Tree
		var content = jQuery('<div>').addClass("content").css("overflow", "auto").appendTo(chapter);
		this._parent = jQuery(chapter)
			.mousewheel(function(event, delta) {//Add Mousescroll Capability to the View
				that._parent[0].scrollTop = that._parent[0].scrollTop - delta*4;});
		this._tree.init(content,this._treeData);
		
		//Add the collapse button and the functionality behind it
		jQuery('<div>').addClass("chapSort").appendTo(chapter).click(function(){
			that._tree.close_all();
			that.selectNode(jQuery(that._selected).attr("dmdid"))
		});
	}
	
	/*
	 * @description makes the View visible depending on the given boolean value, if no value is given
	 *  the View will switch in the opposite mode than it's currently
	 * @param bool Boolean which holds the state into which the View shall switch
	 */
	function visible(bool) {
		//if nothing is given simply switch between the states
		if (typeof bool === "undefined")
			bool = !this._visible;
		if (bool === true) {
			this._visible = true;
			this._parent.slideDown();
		} else {
			this._visible = false;
			this._parent.slideUp();
		}
	}
	
	/*
	 * @description Returns the internal TreeData representation, which is/was used to build-up the Tree
	 * @return the internal TreeData which is used to build the Tree
	 */
	function getTree() {
		return this._treeData;
	}
	
	/*
	 * @description Finds within the tree, the given Entry with matching nodeID and tells
	 *  jsTree to select it and to collapse everything except the path from the root Node
	 *  to the newly selected Entry
	 * @param nodeID the ID of the Entry which is the new selected entry, and therefore needs to be showed within the View
	 */
	function selectNode(nodeID) {
		//Avoid cycling as the Tree would notify its listeners itself which could call him again...
		this.notifyOthers = false;
		var that = this;
		//Find the node we're searching for
		var res = this._parent.find('li.leaf').filter(function(index, element) {return jQuery(element).attr("dmdid") == nodeID});
		//Select all Returned entries(should be only one, else something within the METS Document
		//or the Model is wrong..
		jQuery.each(res, function(index, element) {that._tree.select_branch(element); that._selected = element});
	}
	
	iview.chapter.View.prototype.visible = visible;
	iview.chapter.View.prototype.getTree = getTree;
	iview.chapter.View.prototype.addTree = addTree;
	iview.chapter.View.prototype.addBranch = addBranch;
	iview.chapter.View.prototype.addPage = addPage;
	iview.chapter.View.prototype.initTree = initTree;
	iview.chapter.View.prototype.selectNode = selectNode;
})();

/********************************************************
 ********************************************************
 ********************************************************/
/*
 * @name Controller
 * @package iview.chapter
 * @description Controller to Read in METS XML Documents and parsing them into internal representation
 */
iview.chapter.Controller = function(modelProvider, view, metsDoc) {
	this._model = modelProvider.createModel();
	this._view = view  || null;
	var that = this;
	
	this._model.onevent.attach(function(sender, args) { if (args.type == 'selected') that._view.selectNode(args["new"]);});
	this._view.onevent.attach(function(sender, args) { if (args.type == 'selected') that._model.setSelected(args["new"]);});
};

( function() {
	
	/*
	 * @description adds the given Element(a model Entry) to the View, if the supplied Element is a Branch,
	 *  a new Branch within the view is created for all childs of the branch, buildTree is called
	 *  recursively to add these Elements as well. If the Element is just an ordinary Page is added to
	 *  the View as Page
	 * @param element Model Object which shall be added
	 * @param parentElement View Object where element will be added to
	 * @param view View where the element will be added to parentElement  
	 */
	function buildTree(element, parentElement, view) {
		if (element instanceof iview.chapter.METSChapter) {
			parentElement = view.addBranch({"label": element._entry.getLabel()}, parentElement);
			jQuery.each(element.getEntries(), function(index, node) { buildTree(node, parentElement, view)});
		} else {
			view.addPage({"label": element.getLabel(), "dmdid": element.getID()}, parentElement);
		}
	}
	
	/*
	 * @description this function is called to create and show(depending on the View) the controller
	 *  connected Model to the User.
	 * @param parentID string of the Parent Element id or any other jQuery like construction which is able
	 *  to identify an Object
	 */
	function createView(parentID) {
		var entries = this._model.getEntries();
		var that = this;
		//initialize tree structure
		this._view.initTree();
		//start creation Process for top Level Entries within List
		jQuery.each(entries, function(index, entry){ buildTree(entry, that._view.getTree(), that._view)});
		this._view.addTree(parentID);
	}
	
	/*
	 * @description tells the View to change it's display mode to the supplied boolean value. Where true stands
	 *  for visible/show and false for invisible/hidden
	 * @param bool boolean which represents the state the view shall change it's display mode to
	 */
	function showView(bool) {
		this._view.visible(bool);
	}
	
	iview.chapter.Controller.prototype.createView = createView;
	iview.chapter.Controller.prototype.showView = showView;
})();

/*
 * @name ModelProvider
 * @package iview.chapter
 * @description reads in a default Model from the supplied METS Document
 */
iview.chapter.ModelProvider = function(metsDoc) {
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
		for (var i = 0; i < childNodes.length; i++) {
			var child = childNodes[i];
			var type = jQuery(child).attr("TYPE").toLowerCase();
			if (type != "page") {
				//If we're on a branch, move the branch down and add all Elements at the current Position within it's parent
				generateModelFromMets(child, parentEntry.addBranch({'label':jQuery(child).attr("LABEL")}));			
			} else {
				//Just an ordinary Entry so add it to the current Level
				label = (jQuery(child).attr("LABEL") === undefined)? jQuery(child).attr("DMDID"):jQuery(child).attr("LABEL");
				parentEntry.addPage({'label':label,'dmdid': jQuery(child).attr("DMDID")});
			}
		}
	}

	/*
	 *@description Starts the Reading Process of the METS file
	 */	
	function createModel() {
		if (this._model == null) {
			this._model = new iview.chapter.Model();
			//As the Mets file can contain multiple structMap Tags find the one we're using
			var structures = getNodes(this._metsDoc, "mets:structMap");
			for (var i = 0; i < structures.length; i++) {
				if (jQuery(structures[i]).attr("TYPE").toLowerCase() == "logical") {
					generateModelFromMets(structures[i], this._model);//Correct one found, start reading of Document
					break;
				}
				alert("No Matching Structure Info was delivered with the given XML Document.")
			}
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
	
	iview.chapter.ModelProvider.prototype.createModel = createModel;
	iview.chapter.ModelProvider.prototype.setDocument = setDocument;
})();
