/*********************************************
 * 			Toolbar Permalink				 *
 *********************************************/
 var iview = iview || {};
 
/**
 * @namespace	Package for Permalink, contains View and Controller
 * @memberOf 	iview
 * @name		Permalink
 */
iview.Permalink = {};

/**
 * @class
 * @name		View
 * @memberOf	iview.Permalink
 * @proto		Object
 * @description view of permalink within a toolbar to display a link to the current viewer content
 * @param {String} id identifies the current toolbar view
 * @param {Object} events to trigger defined actions, while managing contained elements
 * @param {Object} parent defines the parent node of the permalink view
 * @param {Object} content defines the current permalink
 * @param {Object} permalink represents the main node of the permalink view
 */
iview.Permalink.View = function (id, parent) {
    this.id = id;
    
    this.events = new iview.Event(this);
    
    var newPermalink = jQuery('<div>').addClass(id).addClass('permalinkArea').appendTo(parent);
    var content = jQuery('<textarea class="content" readonly="readonly" wrap="off" onfocus="this.select()"/>').appendTo(newPermalink);
    
    this.content = content[0];
  	this.permalink = newPermalink[0];
};
 
iview.Permalink.View.prototype = {
	
 	/**
 	 * @function
 	 * @memberOf View#
	 * @description removes the permalink view and its content
	 */
    destroy : function () {
		jQuery(this.permalink).remove();
    },
    
	/**
	 * @function
	 * @memberOf View#
	 * @description displays the permalink container with slide-down effect
	 */    
    show : function () {
    	jQuery(this.permalink).show("blind");
    },
  
	/**
	 * @function
	 * @memberOf View#
	 * @description hides the permalink container with slide-up effect
	 */  
    hide : function () {
    	jQuery(this.permalink).hide("blind");
    },
 
	/**
	 * @function
	 * @memberOf View#
	 * @description sets the displayed link to the given url argument
	 * @param {String} url defines the target url, which should be shown in the permalink container
	 */    
    setURL : function (url) {
    	jQuery(this.content).text(url);
    }
};

/********************************************************
 ********************************************************
 ********************************************************/
/**
 * @class
 * @name		Controller
 * @proto		Object
 * @memberOf	iview.Permalink
 * @description main Controller to control the permalink,
 *  functionalities to display, update registred permalink (views)
 *  views will be add directly here and further direct references to them are hold
 * @param {AssoArray} views hold direct references to each added toolbar view
 * @param {boolean} active describes the state of the permalink: displayed (true) or hidden (false)
 */
iview.Permalink.Controller = function () {
	this.views = [];

	this.active = false;
};


iview.Permalink.Controller.prototype = {

	/**
	 * @function
	 * @memberOf View#
	 * @description adds an existing view to the PermalinkController,
	 *  attach to its events (press) and define the actions for each button
	 * @param {Object} view View which should be add
	 */
	addView: function(view) {

		var viewerID = this.getViewer().viewID_to_remove;
		
		var myself = this;
		this.views[view.id] = view;
		
		view.events.attach(function (sender, args) {
	    	if (args.type == "press") {
		    }
	    });
	},
	
   /**
    * @function
    * @memberOf View#
    * @description returns the current state of the permalink (active or not)
    * @return {boolean} returns the current active state
    */
	getActive: function() {
		return this.active;
	},
	
	/**
	 * @function
	 * @memberOf View#
	 * @description displays or hides the permalink view,
	 *  the current state, given in this.active, defines the following action
	 */
	show: function() {
		for (var view in this.views) {
			if (this.active) {
				this.views[view].hide();	
			} else {
				var url = this._update();
				this.views[view].setURL(url);
				this.views[view].show();
			}
		}
		this.active = !this.active;
	},

	/**
	 * @function
	 * @memberOf View#
	 * @description returns the updated link to the current viewer content,
	 * @return {String} returns the generated URL
	 */
	_update: function() {
		var viewerID = this.getViewer().viewID_to_remove;
		// TODO sollte mit in die PermaLink Klasse
		return generateURL(viewerID);
	}
};