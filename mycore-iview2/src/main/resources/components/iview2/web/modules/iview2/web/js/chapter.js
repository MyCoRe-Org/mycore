var iview = iview || {};
iview.chapter = iview.chapter || {};
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
	
	function addPage(element) {
		var page = new iview.chapter.METSPage(element.label, element.dmdid, this)
		this._entries.push(page);
		addHash(page);		
	}

	function addBranch(element) {
		var firstElement = new iview.chapter.METSChapter(element.label, this);
		this._entries.push(firstElement);
		return firstElement;
	}
	
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
	
	function addHash(entry) {
		if (getHash(entry.getID(), this) != null) {
			alert("Entry with this ID already exists. Element will not be added to List");
			return;
		}
		this._hashList[entry.getID()] = entry;
	}
	
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
	this._chapter = false;
	this._parent = null;
};

iview.chapter.METSEntry.prototype = {
	isChapter: function() {
		return this._chapter;
	},
	
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
	
	iview.chapter.METSPage.prototype.setID = setID;
	iview.chapter.METSPage.prototype.getID = getID;
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

	function addPage(element) {
		var page = new iview.chapter.METSPage(element.label, element.dmdid, this);
		this._entries.push(page);
		this._container.addHash(page);
	}
	
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
 * @package iview
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
		var content = jQuery('<div>').addClass("content").css("overflow", "scroll").appendTo(chapter);
		this._parent = jQuery(chapter);
		this._tree.init(content,this._treeData);
		
		//Add the collapse button and the functionality behind it
		jQuery('<div>').addClass("chapSort").appendTo(chapter).click(function(){
			that._tree.close_all();
			that.selectNode(jQuery(that._selected).attr("dmdid"))
		});

		//Add Mousescroll Capability to the View
		ManageEvents.addEventListener(this._parent[0],'mouseScroll', function(e) {
			that._parent[0].scrollTop = that._parent[0].scrollTop - returnDelta(e).y*4;
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
		var res = this._parent.find('li.leaf').filter(function(index, element) {return $(element).attr("dmdid") == nodeID});
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
 * @package iview.chapter.Controller
 * @description Controller to Read in METS XML Documents and parsing them into internal representation
 */
iview.chapter.Controller = function(model, view, metsDoc) {
	this._model = model || null;
	this._view = view  || null;
	this._metsDoc = metsDoc || null;
	var that = this;
	
	this._model.onevent.attach(function(sender, args) { that._view.selectNode(args["new"]);});
	this._view.onevent.attach(function(sender, args) { that._model.setSelected(args["new"]);});
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
		var childNodes = metsNode.childNodes;
		var label;
		for (var i = 0; i < childNodes.length; i++) {
			var child = childNodes[i];
			var type = $(child).attr("TYPE").toLowerCase();
			if (type != "page") {
				//If we're on a branch, move the branch down and add all Elements at the current Position within it's parent
				generateModelFromMets(child, parentEntry.addBranch({'label':$(child).attr("LABEL")}));			
			} else {
				//Just an ordinary Entry so add it to the current Level
				label = ($(child).attr("LABEL") === undefined)? $(child).attr("DMDID"):$(child).attr("LABEL");
				parentEntry.addPage({'label':label,'dmdid': $(child).attr("DMDID")});
			}
		}
	}

	/*
	 *@description Starts the Reading Process of the METS file
	 */	
	function createModel() {
		//As the Mets file can contain multiple structMap Tags find the one we're using
		var structures = getNodes(this._metsDoc, "mets:structMap");
		for (var i = 0; i < structures.length; i++) {
			if ($(structures[i]).attr("TYPE").toLowerCase() == "logical") {
				generateModelFromMets(structures[i], this._model);//Correct one found, start reading of Document
				return;
			}
			alert("No Matching Structure Info was delivered with the given XML Document.")
		}
	}
	
	/*
	 * @description adds the given Element(a model Entry) to the View, if the supplied Element is a Branch, a new Branch within the view is created
	 *  for all childs of the branch, buildTree is called recursively to add these Elements as well. If the Element is just an ordinary Page it' 
	 *  added to View as Page
	 * @param element Model Object which shall be added
	 * @param parentElement View Object where element will be added to
	 * @param view View where the element will be added to parentElement  
	 */
	function buildTree(element, parentElement, view) {
		if (element.isChapter()) {
			parentElement = view.addBranch({"label": element._entry.getLabel()}, parentElement);
			jQuery.each(element.getEntries(), function(index, node) { buildTree(node, parentElement, view)});
		} else {
			view.addPage({"label": element.getLabel(), "dmdid": element.getID()}, parentElement);
		}
	}
	
	/*
	 * @description this function is called to create and show(depending on the View) the controller connected Model to the User.
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
	
	function showView(bool) {
		this._view.visible(bool);
	}
	
	/*
	 * @description Starts the Reading Process of the given XML document, at the end the model is filled with all Data which the XML contained
	 * @param document the XML Document which contains the METS Data which is read  
	 */
	function setDocument(metsDoc) {
		this._metsDoc = metsDoc;	
	}
	
	iview.chapter.Controller.prototype.createModel = createModel;
	iview.chapter.Controller.prototype.setDocument = setDocument;
	iview.chapter.Controller.prototype.createView = createView;
	iview.chapter.Controller.prototype.showView = showView;
})();