/**
 * @namespace
 * @name		iview
 */
var iview = iview || {};
/**
 * @namespace	Package for Chapter, contains Default Chapter View and Controller
 * @memberOf 	iview
 * @name		chapter
 */
iview.chapter = iview.chapter || {};

/**
 * @class
 * @constructor
 * @version	1.0
 * @memberOf	iview.chapter
 * @name 		View
 * @description View to display with a given template the underlying model. Displays a METS-Document as Chapter-Entry-Tree(using jQuery jsTree Components))
 * @requires	jsTree v0.9.6
 */
iview.chapter.View = function() {
	this._treeData;//Stores the Treedata to display(the jsTree Model)
	this._visible = false;
	this._selected = null;//stores the currently selected page and enables reset if Chapters will be selected
	this.notifyOthers = true;//onselect Event was caused by Controller calls, so don't handle them
	this._parent = null;//where will the tree be connected to
	//this._tree = jQuery.tree.create();//The jsTree creation	
};

( function() {

	/**
	 * @public
	 * @function
	 * @name		initTree
	 * @memberOf	iview.chapter.View
	 * @description	creates the basic background datastructure to display the data as jQuery Tree
	 */
	function initTree() {
		var that = this;
		var data = { data : new Array()};
		this._treeData = {
		        plugins : [ "themes", "json_data", "ui" ],
		        json_data : data,
		        "themes" : {
		        	"theme" : "classic",
		        	"url" : "../modules/iview2/gfx/default/classic/style.css"
		        },
		        "ui" : {
		        	"select_limit" : 1,
		        	"selected_parent_open" : true
		        }
		};

	};

	/**
	 * @public
	 * @function
	 * @name		addBranch
	 * @memberOf	iview.chapter.View
	 * @description	adds a new branch to the supplied parantNode(mostly a branch itself), as childNode
	 * @param		{object} branch will be added to the tree at the supplied parantNode
	 * @param		{object} parentNode object where the branch will be added as childNode
	 */
	function addBranch(branch, parentNode) {
		var currentBranch = { "data": branch.label, "children": [], "metadata": {"logid": branch.logid, "order":branch.order}};
		if (parentNode == this._treeData) {
			parentNode.json_data.data.push(currentBranch);
		} else {
			parentNode.children.push(currentBranch);
		}
		return currentBranch;
	}

	/**
	 * @public
	 * @function
	 * @name		addPage
	 * @memberOf	iview.chapter.View
	 * @description	adds a simple page to the supplied parantNode as childNode
	 * @param		{object} page will be added to the tree at the supplied parantNode
	 * @param		{object} parentNode object where the page will be added as childNode
	 */
	function addPage(page, parentNode) {
		parentNode.children.push({"data":page.label, "metadata": {"logid": page.logid, "order": page.order}});
	}

	/**
	 * @public
	 * @function
	 * @name		addTree
	 * @memberOf	iview.chapter.View
	 * @description	adds the view to it's parent an creates the HTML-structure to display the tree. Therefore makes it reachable for the user.
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the chapterview is added
	 */
	function addTree(parent) {
		var that = this;

		//set some Style depending Properties so that the view can work properly;		
		var chapter = jQuery('<div>').addClass("chapter").appendTo(parent);
		//Adding the Content area which holds the Tree
		var content = jQuery('<div>').addClass("content").css("overflow", "auto").appendTo(chapter);
		this._parent = jQuery(chapter)
			.mousewheel(function(event, delta) {//Add Mousescroll Capability to the View
				that._parent[0].scrollTop = that._parent[0].scrollTop - delta*4;
			});
		var treeContainer = jQuery(content);
		this._tree = treeContainer.jstree(this._treeData);
		jQuery(this._tree).appendTo(chapter);
		
		
		$(this._tree).bind('select_node.jstree', function(event, data) {
			var node = data.rslt.obj;
			
			if (!that.notifyOthers) { that.notifyOthers = true; return;}
			
			//If that element isn't selected already and is no chapter notify all Listeners about the User interaction
			if (that._selected != node) {
				var old = that._selected;
				//in case a branch was clicked with no files itself redirect to the first subchapter with content in it
				if (jQuery(node).data().order == Number.MAX_VALUE) {
					node = jQuery(node).find("li").first();
					that.selectNode(node.data().logid);
				}
				that._selected = node;
				jQuery(that).trigger("select.chapter",
					{"old": (jQuery(old).data() != null) ? jQuery(old).data().order : null, 
					"new":jQuery(node).data().order});
			}
		});
		
		
		//console.log(this._treeData);
		//Add the collapse button and the functionality behind it
		jQuery('<div>').addClass("chapSort").appendTo(chapter).click(function(){
			var selected = that._selected;
			var currentNode = selected;
			var last = selected;
			that.notifyOthers = false;
			jQuery(that._tree).jstree("close_all",-1,false);
			
			that.selectNode(jQuery(selected).data().logid);
			//jQuery(that._tree).jstree("select_node",selected,true);
		});
		
		
	}

	/**
	 * @public
	 * @function
	 * @name		visible
	 * @memberOf	iview.chapter.View
	 * @description	makes the View visible depending on the given boolean value. If no value is given the View will switch in the opposite mode than it's currently
	 * @param		{boolean} bool holds the state into which the View shall switch 
	 */
	function visible(bool) {
		//if nothing is given simply switch between the states
		if (typeof bool === "undefined")
			bool = !this._visible;
		if (bool === true) {
			this._visible = true;
			var that = this
			//if the node isn't within the current Viewport it's not displayed as the previous selectBranch 
			//wasn't able to position the entry within the viewport. because the viewport didn't existed at that time
			this._parent.slideDown(function() {
				jQuery(that._selected).data().logid;
			});
		} else {
			this._visible = false;
			this._parent.slideUp();
		}
	}

	/**
	 * @public
	 * @function
	 * @name		selectedNode
	 * @memberOf	iview.chapter.View
	 * @description	finds within the tree the given Entry with matching nodeID and tells jsTree to select it and to collapse everything except the path from the root Node to the newly selected Entry
	 * @param 		{string} nodeID is the id of the entry which is the new selected entry, and therefore needs to be showed within the View
	 */
	function selectNode(nodeID) {
		var that = this;
		//Find the node we're searching for
		var res = this._parent.find("li");
		//Select all Returned entries(should be only one, else something within the METS Document
		//or the Model is wrong..
		jQuery.each(res, function(index, element) {
			//Avoid cycling as the Tree would notify its listeners itself which could call him again...
			if(jQuery(element).data().logid == nodeID){
				that.notifyOthers = false;
				jQuery(that._tree).jstree("select_node", element,true);
				that._selected = element;
				return;
			}

			});
	}

	/**
	 * @public
	 * @function
	 * @name		getSelected
	 * @memberOf	iview.chapter.View
	 * @description	returns the currently selected entry within this view, if nothing is selected an empty string is returned
	 */
	function getSelected() {
		if (this._selected == null) return "";
		return jQuery(this._selected).data().logid;
	}
	
	var prototype = iview.chapter.View.prototype;
	prototype.visible = visible;
	prototype.addTree = addTree;
	prototype.addBranch = addBranch;
	prototype.addPage = addPage;
	prototype.initTree = initTree;
	prototype.selectNode = selectNode;
	prototype.getSelected = getSelected; 
})();

/**
 * @class
 * @constructor
 * @version	1.0
 * @memberOf	iview.chapter
 * @name 		Controller
 * @description Controller for Chapter
 */
iview.chapter.Controller = function(modelProvider, physicalModelProvider, view) {
	this._model = modelProvider.createModel();
	this._view = new (view  || iview.chapter.View)();
	var that = this;
	this._physicalModel = physicalModelProvider.createModel();
	
	jQuery(this._physicalModel).bind("select.METS", function(e, val) {
		if (that._view.getSelected() != val["new"]) {
			that._view.selectNode(that._model._containedIn[val["new"]].getID());
		}
	});
	jQuery(this._view).bind("select.chapter", function(e, val) {
		that._model.setSelected(val["new"]);
		that._physicalModel.setPosition(val["new"]);
	});
};

( function() {

	/**
	 * @public
	 * @function
	 * @name		buildTree
	 * @memberOf	iview.chapter.Controller
	 * @description	Adds the given element(a model entry) to the View, if the supplied element is a branch, a new branch within the view is created for all childs of the branch, buildTree is called recursivly to add these elements as well. If the element is just an ordinary Page is added to the View as Page
	 * @param 		{object} element Model-object which shall be added
	 * @param		{object} parentElement View-object where element will be added to
	 * @param		{object} view View where the element will be added to parentElement
	 */
	function buildTree(element, parentElement, view) {
		if (element instanceof iview.METS.ChapterBranch) {
			parentElement = view.addBranch({"label": element.getLabel(), "logid": element.getID(), "order":element.getOrder()}, parentElement);
			jQuery.each(element.getEntries(), function(index, node) { buildTree(node, parentElement, view)});
		} else {
			view.addPage({"label": element.getLabel(), "logid": element.getID(), "order":element.getOrder()}, parentElement);
		}
	}

	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.chapter.Controller
	 * @description	this function is called to create and show (depending on the View) the controller connected Model to the user
	 * @param 		{string} parentID string of the paranet element id or any other jQuery like construction which is able to identify an object
	 */
	function createView(parentID) {
		var entries = this._model.getEntries();
		var that = this;
		//initialize tree structure
		this._view.initTree();
		//start creation Process for top Level Entries within List
		jQuery.each(entries, function(index, entry){
			buildTree(entry, that._view._treeData, that._view)
		});
		this._view.addTree(parentID);
		jQuery(this._view._tree).bind("loaded.jstree", function (event, data) {
				that._view.selectNode(that._model._containedIn[that._physicalModel.getCurPos()].getID())
		});
	}

	/**
	 * @public
	 * @function
	 * @name		showView
	 * @memberOf	iview.chapter.Controller
	 * @description	tells the view to show up
	 */
	function showView() {
		this._view.visible(true);
	}

	/**
	 * @public
	 * @function
	 * @name		hideView
	 * @memberOf	iview.chapter.Controller
	 * @description	tells the view to hide itself
	 */
	function hideView() {
		this._view.visible(false);
	}	

	/**
	 * @public
	 * @function
	 * @name		toggleView
	 * @memberOf	iview.chapter.Controller
	 * @description	tells the view to change it's display mode to the currently opposite mode
	 */
	function toggleView() {
		this._view.visible();
	}
	
	/**
	 * @public
	 * @function
	 * @name		getActive
	 * @memberOf	iview.chapter.Controller
	 * @description	returns the current state of the ChapterView (if its visible or not)
	 */
	function getActive() {
		return this._view._visible;
	}
	
	var prototype = iview.chapter.Controller.prototype;
	prototype.createView = createView;
	prototype.showView = showView;
	prototype.hideView = hideView;
	prototype.toggleView = toggleView;
	prototype.getActive = getActive;
})();

/**
 * @public
 * @function
 * @name		openChapter
 * @memberOf	iview.chapter
 * @description	open and close the chapterview
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{button} button which represents the chapter in the toolbar
 */
iview.chapter.openChapter = function(viewer, button){
	if (chapterEmbedded) {
		//alert(warnings[0])
		return;
	}
	var that = this;
	// chapter isn't created
	if (viewer.chapter.loaded) {
		viewer.chapter.toggleView();
	} else {
		button.setLoading(true);
		setTimeout(function() {
			that.importChapter(viewer, new jQuery.Deferred().done(function() {
			viewer.chapter.toggleView();
			button.setLoading(false);
		}));
		}, 10);
	}
}

/**
 * @public
 * @function
 * @name		importChapter
 * @memberOf	iview.chapter
 * @description	calls the corresponding functions to create the chapter
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{Deferred} def to set as resolved after the ThumbnailPanel was imported
 */
iview.chapter.importChapter = function(viewer, def) {
	viewer.ChapterModelProvider = new iview.METS.ChapterModelProvider(viewer.metsDoc);
	
	viewer.chapter = jQuery.extend(viewer.chapter, new iview.chapter.Controller(viewer.ChapterModelProvider, viewer.PhysicalModelProvider));

	viewer.chapter.createView(viewer.chapter.parent);
	viewer.chapter.loaded = true;//signal that the chapter was loaded successfully
	def.resolve();
};
