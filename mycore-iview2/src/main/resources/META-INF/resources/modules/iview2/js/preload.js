//TODO Preload größe anhand der von den Kacheln bestimmen
/**
 * @namespace	Package for Preload Image, contains default View and Controller
 * @memberOf 	iview
 * @name		Preload
 */
iview.Preload = iview.Preload || {};

/**
 * @class
 * @constructor
 * @memberOf	iview.Preload
 * @name 		Controller
 * @description Controller for Preload
 * @param	{IviewInst} viewer to which the Preload shall be attached to
 */
iview.Preload.Controller = function(viewer) {
	this.view = new iview.Preload.View(viewer);
	var currentImage = viewer.currentImage;
	var zoomInfo = currentImage.zoomInfo;
	var that = this;
	
	jQuery(currentImage).bind(iview.CurrentImage.CHANGE_EVENT, function() {
		that.setSrc(viewer.viewerBean.tileUrlProvider.assembleUrl(0,0,0))
	}).bind(iview.CurrentImage.DIMENSION_EVENT, function() {
		that.width(this.curWidth);
		that.height(this.curHeight);
	}).bind(iview.CurrentImage.POS_CHANGE_EVENT, function() {
		that.left(this.x);
		that.top(this.y);
	});
};

iview.Preload.Controller.prototype = {
	/**
	 * @public
	 * @function
	 * @name		width
	 * @memberOf	iview.Preload.Controller#
	 * @description gets/sets the width of the Preload Image;
	 * @param		All functionality as jQuery.width() has
	 */
	width: function() {
		return this.view.me.width.apply(this.view.me, arguments);
	},
	
	/**
	 * @public
	 * @function
	 * @name		height
	 * @memberOf	iview.Preload.Controller#
	 * @description gets/sets the height of the Preload Image;
	 * @param		All functionality as jQuery.height() has
	 */
	height: function() {
		return this.view.me.height.apply(this.view.me, arguments);
	},
	
	/**
	 * @public
	 * @function
	 * @name		position
	 * @memberOf	iview.Preload.Controller#
	 * @description gets the position of the Preload Image;
	 */
	position: function() {
		return this.view.me.position.apply(this.view.me, arguments);
	},

	/**
	 * @public
	 * @function
	 * @name		top
	 * @memberOf	iview.Preload.Controller#
	 * @description gets/sets top of Preload Image; If val isn't set the current value will be returned, else the top value will be set
	 * @param		{float} [val] to set; if not given the current value will be returned
	 */
	top: function(val) {
		if (typeof val != "undefined") {
			this.view.me.css('top', val + 'px');
		} else {
			return this.view.me.css('top');
		}
	},
	
	/**
	 * @public
	 * @function
	 * @name		left
	 * @memberOf	iview.Preload.Controller#
	 * @description gets/sets left of Preload Image; If val isn't set the current value will be returned, else the left value will be set
	 * @param		{float} [val] to set; if not given the current value will be returned
	 */
	left: function(val) {
		if (typeof val != "undefined") {
			this.view.me.css('left', val + 'px');
		} else {
			return this.view.me.css('top');
		}
	},
	
	/**
	 * @public
	 * @function
	 * @name		setSrc
	 * @memberOf	iview.Preload.Controller#
	 * @description sets the new Image for the Preload to display
	 * @param		{string} thumbSource path to image which shall be displayed
	 */
	setSrc: function(thumbSource) {
		this.view.me.empty().append(jQuery("<img>").addClass("preloadImg").css({'width': "100%", 'height': "100%"}).attr("src", thumbSource));
	}
};

/**
 * @class
 * @constructor
 * @memberOf	iview.Preload
 * @name 		View
 * @description View for Preload
 * @param	{IviewInst} viewer to which the Preload shall be attached to
 */
iview.Preload.View = function(viewer) {
	this.me = jQuery("<div>").addClass("preload").append(jQuery("<img>")).appendTo(jQuery(viewer.viewerContainer).find(".iview_well"));
};
