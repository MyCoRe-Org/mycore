var iview = iview || {};
/**
 * @namespace	Package for overview contains Controller, View and Model
 * @memberOf 	iview
 * @name		overview
 */
iview.overview = iview.overview || {};

/**
 * @class
 * @constructor
 * @name		View
 * @memberOf	iview.overview
 * @description	Standard View for the overview Object
 * @param		{i18n} i18n Class to allow translations
 */
iview.overview.View = function(i18n) {
	this._i18n = i18n;
	this._mousedown = false;
	this._visible = true;
	this._ratioX = 0;
	this._ratioY = 0;
	this._x = 0;
	this._y = 0;
	this._sizeX = 0;
	this._sizeY = 0;
	this._curX = 0;
	this._curY = 0;
	this._mouseX = 0;
	this._mouseY = 0;
};

(function() {
	
	/**
	 * @private
	 * @function
	 * @name		notifyOnload
	 * @memberOf	iview.overview.Model
	 * @description	as soon as the image is loaded all listeners who listen to Onload Events will be noticed that a new Image is loaded
	 * @param		{instance} that
	 */
	function notifyOnload(that) {
		if (that.my.thumbnail.complete && isBrowser(["IE","Opera"]) 
			|| that.my.thumbnail.naturalWidth > 2 && !isBrowser(["IE", "opera"])) {
			applyValues(that);
		} else {
			window.setTimeout(function() { notifyOnload(that);},100);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.overview.View
	 * @description	creates a new view based upon the values from the model and given parameters
	 * @param		{object} args Arguments to modify the view
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 */
	function createView(args) {
		var that = this;
		var complete = jQuery("<div>")
			.addClass("thumb " + args.mainClass + " " + args.customClass)
			.css("overflow","hidden")
			.dblclick(function(e) { 
				var newX = (e.layerX)? e.layerX : e.offsetX;
				var newY = (e.layerY)? e.layerY : e.offsetY;
				jQuery(that).trigger("move.overview", {"x" : {"new":scale(that, newX, true)}, "y" : {"new":scale(that, newY, false)}})
			})
			.mousewheel(function(e, delta) {
				jQuery(that).trigger("scroll.overview", {"delta": delta});
			})
			.mouseup(function(e) { mouseUp(that, e)})
			.mousemove(function(e) { mouseMove(that, e)})
			.mousedown(function() { return false;});// deactivate Browser-Drag&Drop
		
		var overview = jQuery("<div>")
			.addClass("ausschnitt " + args.mainClass + " " + args.customClass)
			.mousedown(function(e) { mouseDown(that, e)})
			.mouseup(function(e) {mouseUp(that, e)});
		var thumb = new Image();
		
		complete.append(overview)
			.append(thumb)
			.appendTo(args.thumbParent);
		
		var toggler = jQuery("<div>")
			.addClass("toggler hide")
			.click(function() {damper(that);});
		
		//set the default translation and keep upto date if it should change later
		jQuery(this._i18n.executeWhenLoaded(function(i) {toggler.attr("title", i.translate("overview.fadeOut"))}))
			.bind("change.i18n load.i18n",function(e, obj) {toggler.attr("title", obj.i18n.translate("overview.fade" + (that._visible? "Out":"In")))});
		
		
		var damp = jQuery("<div>")
			.addClass("damp " + args.mainClass + " " + args.customClass)
			.append(toggler)
			.appendTo(args.dampParent);

		this.my = {'self':complete, 'overview':overview, 'thumbnail':thumb, 'damp':damp, 'toggler': toggler};
	}
	
	/**
	 * @private
	 * @function
	 * @name		setSrc
	 * @memberOf	iview.overview.Model
	 * @description	sets for the Image a new Image Source which is then loaded
	 * @param		{instance} that
	 * @param		{string} path represents the new URL of the Image
	 */
	function setSrc(that, path) {
		var thumb = jQuery(that.my.thumbnail);
		thumb.remove();
		thumb = new Image();
		jQuery(thumb)
			.appendTo(that.my.self)
			.load(function() { notifyOnload(that);})
			.attr("src", path);
		that.my.thumbnail = thumb;
	}
	
	/**
	 * @private
	 * @function
	 * @name		adaptView
	 * @memberOf	iview.overview.View
	 * @description	takes the incoming change events and prepares them so that the view can be adapted correspondingly
	 * @param		{object} args arguments of the property change event
	 */
	function adaptView(args) {
		switch (args.type) {
		case "move":
			this._x = toFloat(args.value.x);
			this._y = toFloat(args.value.y);
			applyValues(this);
			break;
		case "ratio":
			this._ratioX = toFloat(args.value.x);
			this._ratioY = toFloat(args.value.y);
			applyValues(this);
			break;
		case "size":
			this._sizeX = toFloat(args.value.x);
			this._sizeY = toFloat(args.value.y);
			applyValues(this);
			break;
		case "path":
			setSrc(this, args.value);
			break;
		default:
			if (typeof console != "undefined") {
				console.log ("got unknown type " + args.type);
			}
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		applyValues
	 * @memberOf	iview.overview.View
	 * @description	adapt View so that it represents the latest changes within properties
	 * @param		{that} instance
	 */
	function applyValues(that) {
		var overview = that.my.overview;
		var thumbnail = jQuery(that.my.thumbnail);

		that.my.self.css({"width": thumbnail.width(), "height": thumbnail.height()});

		overview.css("width", thumbnail.width() * that._ratioX + "px");
		overview.css("height", thumbnail.height() * that._ratioY + "px");
		
		if (overview.outerWidth() > thumbnail.width()) {
			overview.css("width", thumbnail.width() - (overview.outerWidth() - overview.width()) + "px")
		}
		if (overview.outerHeight() > thumbnail.height()) {
			overview.css("height", thumbnail.height() - (overview.outerHeight() - overview.height()) + "px")
		}

		if (that._x < 0) {
			that._x = 0;
		}
		if (overview.outerWidth() + that._x > thumbnail.width()) {
			that._x = thumbnail.width() - overview.outerWidth();
		}
		
		if (that._y < 0) {
			that._y = 0;
		}
		if (overview.outerHeight() + that._y > thumbnail.height()) {
			that._y = thumbnail.height() - overview.outerHeight();
		}
		overview.css({"left":that._x + "px", "top": that._y + "px"});
	}
	

	/**
	 * @private
	 * @function
	 * @name		mouseMove
	 * @memberOf	iview.overview.Model
	 * @description	captures mouse movement and resets the Mouse overview Position when the Mouse is pressed
	 * @param		{event} e Event which occured
	 */
	function mouseMove(that, e) {
		if (that._mouseIsDown) {
			if (isBrowser(["IE"])){//IE
				that._x = that._curX + (e.clientX - that._mouseX);
				that._y = that._curY + (e.clientY - that._mouseY);
			} else {//Mozilla
				that._x = that._curX + (e.pageX - that._mouseX);
				that._y = that._curY + (e.pageY - that._mouseY);
			}
			that.my.overview.css({"left": "0", "top":0})
			applyValues(that);
		}
	}

	/**
	 * @private
	 * @function
	 * @name		mouseDown
	 * @memberOf	iview.overview.Model
	 * @description	stores the Position where the mouse was pressed so that on Mousemovement this constellation is kept, mouseIsDown is set to true	so that the other functions know the current mousestate
	 * @param		{event} e Event which occured
	 * @return		{boolean} false to prevent Browser Default Drag&Drop behave
	 */
	function mouseDown(that, e) {
		if (e.button < 2) {//nur bei linker und mittlerer Maustaste auf True setzen
			that._mouseIsDown = true;
			if(isBrowser(["IE"])){//IE
				that._mouseX = e.clientX;
				that._mouseY = e.clientY;
			} else {//Mozilla
				that._mouseX = e.pageX;
				that._mouseY = e.pageY;
			}
			//Bestimmen der aktuellen oberen Ecke, und Bewegungsvektor
			that._curX = parseInt(that.my.overview.css("left"));
			that._curY = parseInt(that.my.overview.css("top"));
		}
		return false;
	}

	/**
	 * @private
	 * @function
	 * @name		mouseUp
	 * @memberOf	iview.overview.View
	 * @description	releases the current Mousestate(mouseIsDown = false) so that no further movement of overview happen, although all MouseUp Listeners will be noticed about it
	 * @param		{event} e Event which occured
	 */
	function mouseUp(that, e) {
		if (that._mouseIsDown && e.button < 2) {
			if(isBrowser(["IE"])){//IE
				var positionX = that._curX + (e.clientX - that._mouseX);
				var positionY = that._curY + (e.clientY - that._mouseY);
			} else {//Mozilla
				var positionX = that._curX + (e.pageX - that._mouseX);
				var positionY = that._curY + (e.pageY - that._mouseY);
			}

			setPosBlock = true;
			
			if (positionX != that._curX || positionY != that._curY) {
				jQuery(that).trigger("move.overview", {x: {"new": scale(that, positionX + that.my.overview.width()/2, true), "old": scale(that, that._curX, true)}, y: {"new": scale(that, positionY + that.my.overview.height()/2, false), "old": scale(that, that._curY, false)}});
			}
			that._mouseIsDown = false;
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		scale
	 * @memberOf	iview.overview.View
	 * @description	calculates how much a pixel within the thumbnail represents pixels in the original picture
	 * @param		{instance} that
	 * @param		{float} value to transform to outside pixel count
	 * @param		{boolean} x value is a width (true) or height (false) valuetype
	 */
	function scale(that, value, x) {
		if (x) {
			return value / (jQuery(that.my.thumbnail).width() / that._sizeX);
		} else {
			return value / (jQuery(that.my.thumbnail).height() / that._sizeY);
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		damper
	 * @memberOf	iview.overview.View
	 * @description	on a mouseclick event the Thumbnail will be toggled in display state
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function damper(that) {
		if (that._visible) {
			that.my.self.fadeOut();
			that.my.toggler.removeClass("hide").addClass("show")
			that._i18n.executeWhenLoaded(function(i) {that.my.toggler.attr("title", i.translate("overview.fadeIn"))});
		} else {
			that.my.self.fadeIn();
			that.my.toggler.removeClass("show").addClass("hide")
			that._i18n.executeWhenLoaded(function(i) {that.my.toggler.attr("title", i.translate("overview.fadeOut"))});
		}
		that._visible = !that._visible;
	}
	
	var prototype = iview.overview.View.prototype
	prototype.createView = createView;
	prototype.adaptView = adaptView;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.overview
 * @name 		Model
 * @description Model for overview, Model is just a fake model and doesn't contain any Data itself. It's only a pipeline.
 */
iview.overview.Model = function() {};

(function() {
	
	/**
	 * @public
	 * @function
	 * @name		setRatio
	 * @memberOf	iview.overview.Model
	 * @description	set ratio of overview to thumbnail
	 * @param		{object} ratio x and y ratio of overview to thumbnail
	 * @param		{float} ratio.x width ratio of overview to thumbnail
	 * @param		{float} ratio.y height ratio of overview to thumbnail
	 */
	function setRatio(ratio) {
		ratio.x = toFloat(ratio.x);
		ratio.y = toFloat(ratio.y);
		jQuery(this).trigger("ratio.overview", {"value": {"x" :ratio.x,"y" : ratio.y}});
	}
	
	/**
	 * @public
	 * @function
	 * @name		setPos
	 * @memberOf	iview.overview.Model
	 * @description	set the current position of the overview within the thumbnail
	 * @param		{object} pos new x and y coords of the left upper corner of the overview
	 * @param		{float} pos.x new x coord of the upper left corner
	 * @param		{float} pos.y new y coord of the upper left corner
	 */
	function setPos(pos) {
		pos.x = toInt(pos.x);
		pos.y = toInt(pos.y);
		jQuery(this).trigger("move.overview", {"value": {"x":pos.x,"y":pos.y}});
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSrc
	 * @memberOf	iview.overview.Model
	 * @description	set the current Path of the thumbnail picture and notifies all listeners about it
	 * @param		{path} path to new thumbnail picture
	 */
	function setSrc(path) {
		jQuery(this).trigger("path.overview", {'new': path});
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSize
	 * @memberOf	iview.overview.Model
	 * @description	sets the size of the original picture to the model and notifies all listeners about a change
	 * @param		{object} size of the original picture
	 * @param		{float} size.x width of the original picture
	 * @param		{float} size.y height of the original picture
	 */
	function setSize(size) {
		jQuery(this).trigger("size.overview", {"value": {"x":size.x, "y":size.y}});
	}
	
	var prototype = iview.overview.Model.prototype
	prototype.setPos = setPos;
	prototype.setRatio = setRatio;
	prototype.setSrc = setSrc;
	prototype.setSize = setSize;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.overview
 * @name 		Controller
 * @description Controller for overview
 * @param		{ModelProvider} modelProvider to create a overview Model
 * @param		{i18n} i18n Class to allow translations
 * @param		{view} [view] where the model will be rendered to
 */
iview.overview.Controller = function(modelProvider, i18n, view) {
	this._model = modelProvider.createModel();
	this._view = new (view || iview.overview.View)(i18n);
	var that = this;
	
	jQuery(this._model).bind("ratio.overview path.overview move.overview size.overview", function(e, val) {
		 that._view.adaptView({'type':e.type,'value':val["new"] || val.value});
	});
};

(function() {
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.overview.Controller
	 * @description	this function is called to create and show (depending on the View) the controller connected Model to the user
	 * @param 		{object} args params to build view
	 * @param		{String,DOM-Object,anything jQuery supports} args.thumbParent parent Element of the thumbnail
	 * @param		{String,DOM-Object,anything jQuery supports} args.dampParent parent Element of the toggler button for the thumbnail
 	 * @param		{string} [args.mainClass]
	 * @param		{string} [args.customClass]
	 * @param		{string} [id] of the thumbnail, not required at all 
	 */
	function createView(args, id) {
		this._view.createView({'thumbParent': args.thumbParent, 'dampParent': args.dampParent, 'mainClass': (args.mainClass || ""), 'customClass': (args.customClass || "")}, id)
	}
	
	/**
	 * @public
	 * @function
	 * @name		attach
	 * @memberOf	iview.overview.Controller
	 * @description	adds the given listener to the view so the listener will be notified about changes within the view
	 * @param		{function} listener to add to the view
	 * @description	attach Eventlistener to used overview model
	 */
	function attach(event, listener) {
		jQuery(this._view).bind(event, listener);
	}
	
	/**
	 * @public
	 * @function
	 * @name		detach
	 * @memberOf	iview.overview.Controller
	 * @param		{string} event name of events to detach the listener from
	 * @param		{function} listener to add to the view
	 * @description	removes the given listener from the view so the listener will no longer receive
	 *  notifications about changes within the view
	 */
	function detach(event, listener) {
		jQuery(this._view).unbind(event, listener);
	}
	
	var prototype = iview.overview.Controller.prototype;
	prototype.createView = createView;
	prototype.attach = attach;
	prototype.detach = detach;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.overview
 * @name 		ModelProvider
 * @description ModelProvider for overview standard Model
 */
iview.overview.ModelProvider = function() {
	this._model = null;
};

(function() {
	/**
	 * @public
	 * @function
	 * @memberOf	iview.overview.ModelProvider
	 * @name 		createModel
	 * @description creates a new Model if none exists or returns the existing one
	 */
	function createModel() {
		if (this._model == null) {
			this._model = new iview.overview.Model(); 
		}
		return this._model;
	}
	
	iview.overview.ModelProvider.prototype.createModel = createModel; 
})();