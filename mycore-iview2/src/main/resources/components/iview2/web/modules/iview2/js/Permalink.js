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
    
    this.content = jQuery('<textarea class="content" readonly="readonly" wrap="off" onfocus="this.select()"/>').first();
  	this.permalink = jQuery('<div>').addClass(id).addClass('permalinkArea').appendTo(parent).append(this.content).first();
};
 
iview.Permalink.View.prototype = {
	
 	/**
 	 * @function
 	 * @memberOf View#
	 * @description removes the permalink view and its content
	 */
    destroy : function () {
		this.permalink.remove();
    },
    
	/**
	 * @function
	 * @memberOf View#
	 * @description displays the permalink container with slide-down effect
	 */    
    show : function () {
    	this.permalink.show("blind");
    },
  
	/**
	 * @function
	 * @memberOf View#
	 * @description hides the permalink container with slide-up effect
	 */  
    hide : function () {
    	this.permalink.hide("blind");
    },
 
	/**
	 * @function
	 * @memberOf View#
	 * @description sets the displayed link to the given url argument
	 * @param {String} url defines the target url, which should be shown in the permalink container
	 */    
    setURL : function (url) {
    	this.content.text(url);
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
 * @param		{iviewInst} viewer in which the function shall operate
 */
iview.Permalink.Controller = function (viewer) {
	/**
	 * @private
	 * @name		views
	 * @type		Array
	 * @description	holds all views connected to this controller
	 */
	this.views = [];
	this.viewer = viewer;
	/**
	 * @private
	 * @name		active
	 * @type		boolean
	 * @description describes the state of the permalink: displayed (true) or hidden (false)
	 */
	this.active = false;
    
};


iview.Permalink.Controller.prototype = {

	/**
	 * @public
	 * @function
	 * @memberOf	iview.Permalink.Controller#
	 * @description adds an existing view to the PermalinkController,
	 *  attach to its events (press) and define the actions for each button
	 * @param {Object} view View which should be add
	 */
	addView: function(view) {
		this.views[view.id] = view;
	},
	
   /**
    * @public
    * @function
    * @memberOf		iview.Permalink.Controller#
    * @description	returns the current state of the permalink (active or not)
    * @return		{boolean} returns the current active state
    */
	getActive: function() {
		return this.active;
	},
	
	/**
	 * @public
	 * @function
	 * @memberOf	iview.Permalink.Controller#
	 * @description displays or hides the permalink view,
	 *  the current state, given in this.active, defines the following action
	 */
	toggleView: function() {
		for (var view in this.views) {
			if (this.active) {
				this.views[view].hide();	
			} else {
				this.views[view].setURL(this._update());
				this.views[view].show();
			}
		}
		this.active = !this.active;
	},
	
	/**
	 * @private
	 * @function
	 * @memberOf	iview.Permalink.Controller#
	 * @description	generates a permalink which contains all needed informations to display the same Picture&Position and other things
	 * @return 		{String} string which contains the generated URL
	 */
	_update: function() {
		var viewer = this.viewer;
		var url = "http://" + window.location.host + window.location.pathname + "?" + window.location.search.replace(/[?|&](x|y|page|zoom|tosize|maximized|css)=([^&]*)/g,"").replace(/\?/,"");
	    url += "&page="		+ viewer.currentImage.name;
	    url += "&derivate="	+ viewer.properties.derivateId;
		url += "&zoom="		+ viewer.viewerBean.zoomLevel;
		url += "&x="		+ viewer.viewerBean.x;
		url += "&y="		+ viewer.viewerBean.y;
		
		var size = "none";
		if (viewer.currentImage.zoomInfo.zoomWidth)
			size = "width";
		if (viewer.currentImage.zoomInfo.zoomScreen)
			size = "screen";
		
		url += "&tosize="	+ size;
		url += "&maximized="+ viewer.viewerContainer.isMax();
		
		for(var currentIndex = 0; currentIndex < this.observer.length; currentIndex++){
		    var parameter = this.observer[currentIndex].getParameter();
		    url += "&" + parameter.name + "=" + parameter.value;
		}
		
		return url;
	}
};

/**
 * @public
 * @function
 * @name		openPermalink
 * @memberOf	iview.Permalink
 * @description	switch between visibility of Permalink element, if needed it's created at first run
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{button} button to which represents the Permalink in the toolbar
 */
iview.Permalink.openPermalink = function(viewer, button) {
	var that = this;
	if (viewer.permalink.loaded) {
		viewer.permalink.toggleView();
	} else {
		button.setLoading(true);
		setTimeout(function() {
			that.importPermalink(viewer, jQuery.Deferred().done(function() {
				button.setLoading(false);
				viewer.permalink.toggleView();
		}))}, 10);
	}
}

/**
 * @public
 * @function
 * @name		importPermalink
 * @memberOf	iview.Permalink
 * @description	calls the corresponding functions to create the Permalink
 * @param		{iviewInst} viewer in which the function shall operate
 * @param		{Deferred} def to set as resolved after the ThumbnailPanel was imported
 */
iview.Permalink.importPermalink = function(viewer, def) {
	viewer.permalink = jQuery.extend(viewer.permalink, new iview.Permalink.Controller(viewer));
	viewer.permalink.addView(new iview.Permalink.View("permalinkView", viewer.viewerContainer));
	viewer.permalink.loaded = true;
	def.resolve();
};


iview.Permalink.parameterObservable = { 
        getParameter : function(){
            return {
                    "name": null,
                    "value": null
                    };
        }
};