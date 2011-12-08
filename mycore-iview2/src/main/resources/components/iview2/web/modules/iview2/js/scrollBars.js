//TODO einbauen das Scrollbar minimale Größe hat, damit man sie noch benutzen kann
/**
 * @namespace	Package for Scrollbar, contains Default Scrollbar Model, View and Controller
 * @memberOf 	iview
 * @name		scrollbar
 */
iview.scrollbar = iview.scrollbar || {};
/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.scrollbar
 * @name		Model
 * @description Modeldata for internal Scrollbar Representation
 */
iview.scrollbar.Model = function() {
	/**
	 * @private
	 * @name		maxVal
	 * @memberOf	iview.scrollbar.Model#
	 * @type		integer
	 * @description	max Value the Scrollbar can reach
	 */
	this._maxVal;
	/**
	 * @private
	 * @name		curVal
 	 * @memberOf	iview.scrollbar.Model#
 	 * @type		integer
	 * @description	Current Value the scrollbar is positioned
	 */
	this._curVal = 0;
	/**
	 * @private
	 * @name		size
 	 * @memberOf	iview.scrollbar.Model#
 	 * @type		number
	 * @description	Size of the Scrollbar complete
	 */
	this._size;
	/**
	 * @private
	 * @name		proportion
 	 * @memberOf	iview.scrollbar.Model#
 	 * @type		float
	 * @description	holds the proportion in percent from whole scrollbar area and the slider within,
	 */
	this._proportion;
};

iview.scrollbar.Model.prototype = {
	/**
	 * @public
	 * @function
	 * @name		setSize
	 * @memberOf	iview.scrollbar.Model#
	 * @description sets the total size of the Scrollbar
	 * @param		{integer} value which holds the new size of the scrollbar, the value is converted to an absolute value
	 */
	setSize: function(value) {
		value = Math.abs(toInt(value));
		//only raise the Event if a change happened
		if (this._size != value) {
			var oldVal = this._size;
			this._size = value;
			//Notify all listeners that a change happened
			jQuery(this).trigger("size.scrollbar", {'old': oldVal, 'new': value });
		}
	},
	
	/**
	 * @public
	 * @function
	 * @name		setProportion
	 * @memberOf	iview.scrollbar.Model#
	 * @description	set the proportion between scrollbarspace and the bar within the space
	 * @param		{float} value which holds the proportion, the value is converted to an absolute value
	 */
	setProportion: function(value) {
		value = Math.abs(toFloat(value));
		//only raise Event if a change happened
		if (this._proportion != value) {
			var oldVal = this._proportion;
			this._proportion = value;
			jQuery(this).trigger("proportion.scrollbar", {'old': oldVal, 'new':value });
		}
	},

	/**
	 * @public
	 * @function
	 * @name		setMaxVal
	 * @memberOf	iview.scrollbar.Model#
	 * @description	set the current maxValue of the scrollbar, this means the Value the scrollbar
	 *  returns when it reached the right/bottom most position, if the value is negative, zero will be used instead
	 * @param		{integer} value which holds the new maxValue for the scrollbar
	 */
	setMaxVal: function(value) {
		value = toInt(value);
		if (value < 0) value = 0;
		//only raise the Event if a change happened
		if (this._maxVal != value) {
			var oldVal = this._maxVal;
			this._maxVal = value;
			//Notify all listeners that a change happened
			jQuery(this).trigger("maxVal.scrollbar", { 'old': oldVal, 'new': value });
		}
	},
	
	/**
	 * @public
	 * @function
	 * @name		setCurVal
	 * @memberOf	iview.scrollbar.Model#
	 * @description	set the current Value(position) where the bar start(left/right end) is positioned
	 * @param		{integer} value which holds the new current Value, if the value is negative zero will be used instead
	 */
	setCurVal: function(value) {
		value = toInt(value);
		if (value < 0) value = 0;
		if (value > this._maxVal) value = this._maxVal;
		//only raise Event if a change happened
		if (this._curVal != value) {
			var oldVal = this._curVal;
			this._curVal = value;
			//Notify all listeners that a change happened
			jQuery(this).trigger("curVal.scrollbar", { 'old':oldVal, 'new':value });
		}
	},
	
	
	/**
	 * @public
	 * @function
	 * @name		changeCurVal
	 * @memberOf	iview.scrollbar.Model#
	 * @description	in contrast to setCurValue this function doesn't treat the given
	 *  value to change the curValue absolute, rather than to change it relative to the current Value
	 * @param		{integer} value integer which represents the change to apply
	 */
	changeCurVal: function(value) {
		value = toInt(value);
		var oldVal = this._curVal;
		this._curVal = this._curVal + value;
		if (this._curVal < 0) this._curVal = 0;
		if (this._curVal > this._maxVal) this._curVal = this._maxVal;
		//only raise Event if a change happened
		if (oldVal != this._curVal)
			jQuery(this).trigger("curVal.scrollbar", { 'old': oldVal, 'new': this._curVal});
	}
}

/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.scrollbar
 * @name 		View
 * @description View to display with a given template the underlying model
 */
iview.scrollbar.View = function() {
	/**
	 * @private
	 * @name		my
	 * @memberOf	iview.scrollbar.View#
	 * @type		Object
	 * @description	holds all further needed Object References
	 */
	this.my;
	/**
	 * @private
	 * @name		direction
	 * @memberOf	iview.scrollbar.View#
	 * @type		boolean
	 * @description	scrollbar direction which is either horizontal (true) or vertical (false)
	 */
	this._direction;
	/**
	 * @private
	 * @name		size
	 * @memberOf	iview.scrollbar.View#
	 * @type		integer
	 * @description	stores the totalsize of the scrollbar
	 */
	this._size = 0;
	/**
	 * @private
	 * @name		mouseDown
	 * @memberOf	iview.scrollbar.View#
	 * @type		boolean
	 * @description	was the mouse pressed over scrollbar (and is still pressed)
	 */
	this._mouseDown = false;
	/**
	 * @private
	 * @name		outOfBar
	 * @memberOf	iview.scrollbar.View#
	 * @type		boolean
	 * @description	stores if mouse was pressed while beeing over the scrollbar or within
	 *  the space, this causes different behavior of given components
	 */
	this._outOfBar = false;
	/**
	 * @private
	 * @name		pixelPerUnit
	 * @memberOf	iview.scrollbar.View#
	 * @type		float
	 * @description	translates from the value area of the model to the available pixel count the
	 *  scrollbar uses for display. So its possible to display the correct location of the scrollbar
	 *  and to map a given move of the scrollbar to the model
	 */
	this._pixelPerUnit = 1;
	/**
	 * @private
	 * @name		maxVal
	 * @memberOf	iview.scrollbar.View#
	 * @type		float
	 * @description	maximal value the scrollbar can reach
	 */
	this._maxVal = 1;

	/**
	 * @private
	 * @name		proportion
	 * @memberOf	iview.scrollbar.View#
	 * @type		float
	 * @description	proportion of scrollbar to total scrollbar area
	 */
	this._proportion = 1;
	/**
	 * @private
	 * @name		curVal
	 * @memberOf	iview.scrollbar.View#
	 * @type		integer
	 * @description	current Value the scrollbar holds
	 */
	this._curVal = 0;
	/**
	 * @private
	 * @name		oldPos
	 * @memberOf	iview.scrollbar.View#
	 * @type		Object .x && .y
	 * @description	contains the previously x && y position of the mouse
	 */
	this._oldPos;
	/**
	 * @private
	 * @name		moveTo
	 * @memberOf	iview.scrollbar.View#
	 * @type		float
	 * @description	If pressing into the empty area this is the var where the position is stored
	 */
	this._moveTo;
	/**
	 * @private
	 * @name		before
	 * @memberOf	iview.scrollbar.View#
	 * @type		boolean
	 * @description	holds if the mouse is pressed behind or before the scrollbar
	 */
	this._before;
	/**
	 * @private
	 * @name		interval
	 * @memberOf	iview.scrollbar.View#
	 * @type		function
	 * @description	holds the currently running interval, null if none is running
	 */
	this._interval = null;//holds the currently running interval
	/**
	 * @private
	 * @name		type
	 * @memberOf	iview.scrollbar.View#
	 * @type		function
	 * @description	holds the currently running interval, null if none is running
	 */
	this._type = "scroll";
};

( function() {
	/**
	 * @private
	 * @function
	 * @name		addInterval
	 * @memberOf	iview.scrollbar.View
	 * @description	sets the currently active interval, if an old interval is found it's replaced with the new one
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{function} intervalFunc which is set to be called by the Intervaltime time
	 * @param		{integer} time in miliseconds which lays between two calls to the function call
	 */
	function addInterval(that, intervalFunc, time) {
		if (that._interval !== null) {
			dropInterval(that);
		}
		that._interval = setInterval(intervalFunc,time);
	}

	/**
	 * @private
	 * @function
	 * @name		dropInterval
	 * @memberOf	iview.scrollbar.View
	 * @description drops (if one is set) the currently executed interval and the connected function
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */	
	function dropInterval(that) {
		clearInterval(that._interval);
		that._interval = null;
	}

	/**
	 * @private
	 * @function
	 * @name		delayMouseClick
	 * @memberOf	iview.scrollbar.View
	 * @description	after the initial mouseClick some time lays between the starting of the periodic
	 *  "recall" of the mouseClick on the button while the mouse is still pressed over the button
	 * @param		{instance} Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{boolean} isIncrease which holds if the current value shall be increased or decreased
	 */
	function delayMouseClick(that, isIncrease) {
		if (that._mouseDown) {
			addInterval(that, function() { notifyClick(that, isIncrease);}, that._intervalTime);
		}
	}
	
	/**
	 * @private
	 * @function
	 * @name		delaySpaceClick
	 * @memberOf	iview.scrollbar.View
	 * @description	after the initial mouseClick some time lays between the starting of the periodic
	 *  "recall" of the mouseClick on the space while the mouse is still pressed
	 * @param		{instance} Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{boolean} isIncrease which holds if the current value shall be increased or decreased
	 */
	function delaySpaceClick(that, before) {
		if (that._mouseDown) {
			addInterval(that, function() { stopUnderMouse(that);}, that._intervalTime);
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		setStepByClick
	 * @memberOf	iview.scrollbar.View#
	 * @description set the value by which the curValue is changed as soon as the Mouse is pressed on a button
	 * @param value integer defines the change which is applied as soon as a button is pressed,
	 *  value is converted to absolute value
	 */
	function setStepByClick(value) {
		this._stepByClick = Math.abs(toInt(value));
	}
	
	/**
	 * @public
	 * @function
	 * @name		setJumpStep
	 * @memberOf	iview.scrollbar.View#
	 * @description	set the value by which the curValue is changed as soon as the mouse is pressed
	 *  within the space of the scrollbar
	 * @param		{integer} value defines the change which is applied as soon as the mouse is pressed
	 *  within the space of the scrollbar
	 */
	function setJumpStep(value) {
		this._jumpStep = Math.abs(toInt(value));
	}

	/**
	 * @public
	 * @function
	 * @name		setScrollDelay
	 * @memberOf	iview.scrollbar.View#
	 * @description	set the time which lays between the initial click of on a button and the
	 *  periodic recall of the movement while the mouse is kept pressed
	 * @param		{integer} value defines the time in miliseconds which lays between the initial click and
	 *  the periodic recall
	 */	
	function setScrollDelay(value) {
		this._scrollDelay = Math.abs(toInt(value));
	}
	
	/**
	 * @public
	 * @function
	 * @name		setSpaceDelay
	 * @memberOf	iview.scrollbar.View#
	 * @description	set the time which lays between the initial click within space and the
	 *  periodic recall of the movement while the mouse is kept pressed
	 * @param		{integer} value defines the time in miliseconds which lays between the initial click and
	 *  the periodic recall
	 */	
	function setSpaceDelay(value) {
		this._spaceDelay = Math.abs(toInt(value));
	}
	
	/**
	 * @public
	 * @function
	 * @name		setIntervalTime
	 * @memberOf	iview.scrollbar.View#
	 * @description	set the time in which all intervals are runned
	 * @param		{integer} value the interval time which is used for all intervals which are set within the scrollbar
	 */
	function setIntervalTime(value) {
		this._intervalTime = Math.abs(toInt(value));
	}

	/**
	 * @private
	 * @function
	 * @name		dbgMouseDown
	 * @memberOf	iview.scrollbar.View
	 * @description	handles the initial Click when the mouse is pressed within the space. After calling the first
	 *  time stopUnderMouse it calls the delay Function so that an automatic movement can be applied
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param 		{event} e of the mousepress which raised this event
	 */
	function dbgMouseDown(that, e) {
		if (that._mouseDown) return;
		that._mouseDown = true;
		that._outOfBar = true;
		//find out if the mouse is pressed before or after the bar so that the movement can be adjusted on it
		if (that._direction) {
			that._moveTo = e.layerX || e.offsetX;
			that._before = (toInt(that.my.bar.css("left")) > (e.layerX || e.offsetX))? true:false;
		} else {
			that._moveTo = e.layerY || e.offsetY;
			that._before = (toInt(that.my.bar.css("top")) > (e.layerY || e.offsetY))? true:false;
		}
		//set the initial movement
		stopUnderMouse(that);
		//add an reoccuring event after an inital delay
		window.setTimeout(function() { delaySpaceClick(that);}, that._spaceDelay);
	}
	
	/**
	 * @private
	 * @function
	 * @name		dbgMouseUp
	 * @memberOf	iview.scrollbar.View
	 * @description	handles the MouseUp within the space area and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function dbgMouseUp(that) {
		that._mouseDown = false;
		that._outOfBar = false;
	}
	
	/**
	 * @private
	 * @function
	 * @name		divMouseDown
	 * @memberOf	iview.scrollbar.View
	 * @description	removes all previously registered intervals and stores the position of the mousepress to handle
	 *  the mouse movement in other events correctly
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{event} e of the mousepress which raised this event
	 */
	function divMouseDown(that, e) {
		if (that._mouseDown) return;
		dropInterval(that);
		that._mouseDown = true;
		//Store the position where the event happened to be able to position the scrollbar later on the occured position changes
		that._oldPos = { 'x': e.clientX, 'y': e.clientY};
		e.cancelBubble = true;
		return false;
	}
	
	/**
	 * @private
	 * @function
	 * @name		divMouseUp
	 * @memberOf	iview.scrollbar.View
	 * @description	handles the MouseUp within the bar and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function divMouseUp(that) {
		dropInterval(that);
		that._mouseDown = false;
		that._outOfBar = false;
	}

	/**
	 * @private
	 * @function
	 * @name		divMouseMove
	 * @memberOf	iview.scrollbar.View
	 * @description	if Drag&Drop is initialized it continously moves the bar so under the mouse that
	 *  the MouseDown Position of Bar relative to Mouse is restored, similiar it places the Stepper
	 * @param		{instance}that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param 		{event} e of the Current MouseMove with the Position of the mouse
	 */
	function divMouseMove(that, e) {
		if (!that._mouseDown || that._outOfBar) return;
		var vector;
		if (that._type == "scroll") {
			//store the movement vector
			if (that._direction) {
				vector = e.clientX - that._oldPos.x;
			} else {
				vector = e.clientY - that._oldPos.y;
			}
			//notify all listeners of the event
			jQuery(that).trigger("mouseMove.scrollbar", {'change':vector/that._pixelPerUnit});
		} else {
			var barStart;
			var barSize;
			var pos;
			if (that._direction) {
				vector = e.clientX - that._oldPos.x;
				pos = e.clientX;
				barStart = that.my.bar.offset().left;
				barSize = that.my.bar.width();
			} else {
				vector = e.clientY - that._oldPos.y;
				pos = e.clientY;
				barStart = that.my.bar.offset().top;
				barSize = that.my.bar.height();
			}
			if (vector < 0 && pos < barStart) {
				jQuery(that).trigger("mouseMove.scrollbar", {'change':-1});
			} else if (vector > 0 && pos > barStart + barSize) {
				jQuery(that).trigger("mouseMove.scrollbar", {'change':1});
			}
			
		}
		//and store as new old position the current one
		that._oldPos = {'x': e.clientX, 'y': e.clientY};
		e.cancelBubble = true;
	}
	
	/**
	 * @private
	 * @function
	 * @name		buttonMouseDown
	 * @memberOf	iview.scrollbar.View
	 * @description	handles the MouseDown event on the scrollbar buttons, this includes removing all previously
	 *  run intervals as well as raising a first value change and adding an reoccuring event which is called
	 *  after an initial delay regulary as long as the mouse is kept pressed
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{boolean} downRight which tells if the button which caused the event was the downRight one which is
	 *  represented by true, if it was the upLeft one the value is false
	 */
	function buttonMouseDown(that, downRight) {
		downRight = (downRight === true)? true:false;
		if (that._mouseDown) return;
		that._mouseDown = true;
		that._outOfBar = true;
		dropInterval(that);
		notifyClick(that, downRight);
		window.setTimeout(function() { delayMouseClick(that, downRight);}, that._scrollDelay);
		jQuery(document).bind("mouseup" ,function() { buttonMouseUp(that);});
		return false;
	}
	
	/**
	 * @private
	 * @function
	 * @name		buttonMouseUp
	 * @memberOf	iview.scrollbar.View
	 * @description	handles the MouseUp within the button and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function buttonMouseUp(that) {
		dropInterval(that);
		that._mouseDown = false;
		that._outOfBar = false;
		jQuery(document).unbind("mouseup" ,function() { buttonMouseUp(that);});
	}
	
	/**
	 * @private
	 * @function
	 * @name		mouseScroll
	 * @memberOf	iview.scrollbar.View
	 * @description	notifies all listeners about a recent mouse scroll event and performs if needed special event
	 *  behavior depending on the bar mode
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 * @param 		{integer} delta of the mousescroll event which occurred. Note that the delta needs to be already normalized as jQuery does
	 */
	function mouseScroll(that, delta) {
		jQuery(that).trigger("mouseWheel.scrollbar", {'change': ((that._type != "stepper")? 2*-delta/that._pixelPerUnit: -delta)});
	}
	
	/**
	 * @public
	 * @function
	 * @name		addTo
	 * @memberOf	iview.scrollbar.View#
	 * @description	adds the scrollbar to the given parent
	 * @param		{String,DOM-Object,anything jQuery supports} parent DOM element to which the scrollbar is added
	 */
	function addTo(parent) {
		this.my.self.appendTo(parent);
	}
	
	/**
	 * @private
	 * @function
	 * @name		applySize
	 * @memberOf	iview.scrollbar.View
	 * @description	adapts the scrollbar to the new full size, so the space and bar sizes are adapted to fulfill
	 *  the new size and to look nicely
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function applySize(that) {
		var my = that.my;
		var size = that._size;
		if (that._direction) {
			my.self.width(size + "px");
			my.space.width(my.self.width() - my.bUL.width() - my.bDR.width() + "px");
			my.space.children("[class='space']").width(my.space.width() - my.space.children('[class="start"]').width() - my.space.children("[class='end']").width() + "px");
			if (my.bar.width() > my.space.width() || that._maxVal === 0) {
				my.bar.width(my.space.width() + "px");
			}
			my.bar.children("[class='scroll']").width(my.bar.width() - my.bar.children("[class='start']").width() - my.bar.children("[class='end']").width() + "px");
		} else {
			my.self.height(size + "px");
			my.space.height(size - my.bUL.height() - my.bDR.height() + "px");
			my.space.children("[class='space']").height(my.space.height() - my.space.children('[class="start"]').height() - my.space.children("[class='end']").height() + "px");
			if (my.bar.height() > my.space.height() || that._maxVal === 0) {
				my.bar.height(my.space.height() + "px");
			}
			my.bar.children("[class='scroll']").height(my.bar.height() - my.bar.children("[class='start']").height() - my.bar.children("[class='end']").height() + "px");
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.scrollbar.View#
	 * @description creates the scrollbar by creating all needed DOM Elements and setting some important
	 *  css values so the scrollbar can work correctly; connects all view functions to the related events
	 *  and objects
	 * @param 		{object} args
	 * @param		{string} args.direction=(horizontal|vertical) tells if the bar will be vertical or horizontal
	 * @param		{string} args.mainClass tells what the main Class for the scrollbar shall be
	 * @param		{string} args.customClass allows it to modify the Scrollbar in parts to differ from others
	 * @param 		{string} [id] tells the id of the scrollbar. This property isn't needed as the
	 *  scrollbar works just fine without ids. The id maybe only needed if you plan to perform custom
	 *  transformations on the scrollbar DOM
	 */
	function createView(args, id) {
		var that = this;
		this._direction = (args.direction == "horizontal")? true:false;
		var mainClass = args.mainClass || "";
		var customClass = args.customClass || "";
		
		var complete = jQuery("<div>")
			.addClass(mainClass +((this._direction)? "H":"V"))
		
		//Add additional Class to override the overall scrollbar setting
		if (customClass != "") {
			complete.addClass(customClass +((this._direction)? "H":"V"));
		}
		if (typeof id !== "undefined") {
			complete.attr("id", id);
		}
		
	//Start Up/Left
		var buttonUL = jQuery("<div>")
			.addClass("start")
			.mousedown(function() { buttonMouseDown(that, false); return false;})
			.appendTo(complete);
			
		var wholeSpace = jQuery("<div>")
			.addClass("empty")
			.mousedown(function(e) { dbgMouseDown(that, e); return false;})
			.mouseup(function() { dbgMouseUp(that);})
			.mousewheel(function(e, delta) {
				mouseScroll(that, delta);
				return false;})
			.appendTo(complete);

		//spaceStart
		jQuery("<div>")
			.addClass("emptyStart")
			.appendTo(wholeSpace);
			
		var space = jQuery("<div>")
			.addClass("space")
			.appendTo(wholeSpace);
		
		//spaceEnd
		jQuery("<div>")
			.addClass("emptyEnd")
			.appendTo(wholeSpace);
		
		var fnMouseMove = function(e) {
			divMouseMove(that, e);
		};
		var fnMouseUp = function() {
			buttonMouseUp(that);
			jQuery(document)
			.unbind('mouseup', this)
			.unbind('mousemove', fnMouseMove);
			return false;
		};
		
		var barScroll = jQuery("<div>")
			.addClass("bar")
			.mousedown(function(e) {
				divMouseDown(that, e);
				jQuery(document)
				.mouseup(fnMouseUp)
				.mousemove(fnMouseMove);
				return false;
			})
			.mouseup(divMouseUp)
			.mousemove(fnMouseMove)
			.css("position", "absolute")
			.appendTo(wholeSpace);
		
		var barStart = jQuery("<div>")
			.addClass("start")
			.appendTo(barScroll);
		
		var scroll = jQuery("<div>")
			.addClass("scroll")
			.appendTo(barScroll);
		
		var barEnd = jQuery("<div>")
			.addClass("end")
			.appendTo(barScroll);
		
		var buttonDR = jQuery("<div>")
			.addClass("end")
			.mousedown(function() { buttonMouseDown(that, true);return false;})
			.appendTo(complete);
		
		if (this._direction) {
			jQuery(complete).find("*").css("float", "left");
			scroll.css("left", "0px");
		} else {
			scroll.css("top", "0px");
		}
		
		this.my = { 'self': complete, 'bUL': buttonUL, 'bDR': buttonDR, 'bar': barScroll, 'space': wholeSpace};
		applySize(this);
	}
	
	/**
	 * @private
	 * @function
	 * @name		setButtonVisibility
	 * @memberOf	iview.scrollbar.View
	 * @description	based upon the current Scrollbar-Value both buttons are toggled to display the state
	 *  of the scrollbar
	 * @param		{instance} that
	 */
	function setButtonVisibility(that) {
		if (that._curVal <= 0) {
			that.my.bUL.addClass("disabled");
			that.my.bDR.removeClass("disabled");
		} else if (that._curVal >= that._maxVal) {
			that.my.bUL.removeClass("disabled");
			that.my.bDR.addClass("disabled");
		} else {
			that.my.bUL.removeClass("disabled");
			that.my.bDR.removeClass("disabled");
		}
	}
	
	/**
	 * @public
	 * @function
	 * @name		adaptView
	 * @memberOf	iview.scrollbar.View#
	 * @description	is called to adapt the view/scrollbar to display the changed values correctly to the user
	 * @param		{object} args which holds the type and value of the property which changed so that the view
	 *  can be adapted to represent the new values correctly
	 * @param		{string} type
	 * @param		{integer} value 
	 */
	function adaptView(args) {
		var my = this.my;
		switch (args.type) {
			case "curVal":
				this._curVal = args.value;
				setButtonVisibility(this);
				break;
			case "proportion":
				//Set the new bar width/height
				this._proportion = args.value;
				break;
			case "maxVal":
				this._maxVal = args.value;
				if (this._maxVal <= 0) {
					this.my.bUL.addClass("disabled");
					this.my.bDR.addClass("disabled");
				} else {
					setButtonVisibility(this);
				}
				break;
			case "size":
				this._size = args.value;
				applySize(this);
				break;
			default:
				log("Got:"+args.type+" as argument type, don't know what to do with it");
		}
		//calculate the bar size
		//calculate the pixelPerUnit rate to set the positioning of the bar correctly
		if (this._direction) {
			my.bar.width(this._proportion*my.space.width());
			this._pixelPerUnit = Math.abs(my.space.width()-my.bar.width())/this._maxVal;
		} else {
			my.bar.height(this._proportion*my.space.height());
			this._pixelPerUnit = Math.abs(my.space.height()-my.bar.height())/this._maxVal;
		}

		//IE throws as usual Errors if some value isn't in the expected range, so a 0 within maxVal gets him mad
		if (this._maxVal != 0) {
			my.bar.css((this._direction)? "left":"top",this._curVal*this._pixelPerUnit + "px");
		} else {
			my.bar.css((this._direction)? "left": "top", "0px");
		}
		applySize(this);
	}

	/**
	 * @private
	 * @function
	 * @name		notifyClick
	 * @memberOf	iview.scrollbar.View
	 * @description	is called to notify all listeners about an occured Value change caused by an click, depending
	 *  on the isIncrease boolean you can tell if the current value is increased or decreased
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param		{boolean} isIncrease which tells if the curValue will be increased or decreased
	 */
	function notifyClick(that, isIncrease) {
		if (that._curVal < 0 || that._curVal >= that._maxValue) return;
		jQuery(that).trigger("mouseClick.scrollbar", {'change':(isIncrease === true)? that._stepByClick: -that._stepByClick});
	}

	/**
	 * @private
	 * @function
	 * @name		stopUnderMouse
	 * @memberOf	iview.scrollbar.View
	 * @description	when the mouse is pressed within the space of the bar this function is called, which checks if
	 *  the bar is already under the mouse and stops the reoccuring process if that is the case. Else it will
	 *  notify all listeners about the happened changes
	 * @param		{instance} that as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function stopUnderMouse(that) {
		//if the mouse is released while this function is called remove all set Intervals to represent this fact
		if (!that._mouseDown) {
			dropInterval(that);
			return;
		}
		//calculate the movement which has to take place and if the scrollbar is already under the mouse stop the interval		
		var curVal = (that._direction)? toInt(that.my.bar.css("left")): toInt(that.my.bar.css("top"));
		var barLength = (that._direction)? that.my.bar.width(): that.my.bar.height();
		if ((that._before && (that._moveTo >= curVal)) || (!that._before && (that._moveTo <= curVal + barLength))) {
			dropInterval(that)
			that._mouseDown = false;
			return;
		}
		//Notify all Listeners about the occured change, prepare the Event Object depending on the clicked positon
		jQuery(that).trigger("mouseClick.scrollbar", {'change':((that._before)?-that._jumpStep:that._jumpStep)});
	}
	
	/**
	 * @public
	 * @function
	 * @name		addEventFrom
	 * @memberOf	iview.scrollbar.View#
	 * @description	allows it that some functions of the scrollbar are triggered from events outside the scrollbar
	 * @param		{string} type which tells what event shall be added
	 * @param		{string,DOM-Object, anything JQuery can handle} element where the event type shall be added to
	 */
	function addEventFrom(type, from) {
		var that = this;
		switch (type) {
		case "mousescroll":
			jQuery(from).mousewheel(function(e, delta) {
				mouseScroll(that, delta);
				return false;
			});
			break;
		case "mouseup":
			jQuery(from).mouseup(function(event) {
				divMouseUp(that)
			});
			break;
		case "mousemove":
			jQuery(from).mousemove(function(event) {
				divMouseMove(that, event);
			});
			break;
		default:
			break;
		}
	}
	
	var prototype = iview.scrollbar.View.prototype;
	prototype.createView = createView;
	prototype.addTo = addTo;
	prototype.adaptView = adaptView;
	prototype.setStepByClick = setStepByClick;
	prototype.setJumpStep = setJumpStep;
	prototype.setScrollDelay = setScrollDelay; 
	prototype.setSpaceDelay = setSpaceDelay;
	prototype.setIntervalTime = setIntervalTime;
	prototype.addEventFrom = addEventFrom;
})();

/**
 * @class
 * @constructor
 * @version		1.0
 * @memberOf	iview.scrollbar
 * @name 		Controller
 * @description Controller for Scrollbars
 * @param		{iview.scrollbar.Model, API-equal Object} [model=iview.scrollbar.Model] Modeltype to use
 *  for this Scrollbar, if not the package Type is used be sure to use a compatible one
 * @param		{iview.scrollbar.View, API-equal Object} [view=iview.scrollbar.View] Viewtype to use for
 *  this Scrollbar, if not the package Type is used be sure to use a compatible one
 */
iview.scrollbar.Controller = function(model, view) {
	this._model = new (model || iview.scrollbar.Model)();
	this._view = new (view || iview.scrollbar.View)();
	var that = this;
	
	jQuery(this._model).bind("size.scrollbar proportion.scrollbar curVal.scrollbar maxVal.scrollbar", function(e, val) {
		 that._view.adaptView({'type':e.type, 'value': val["new"]});
	});
	
	jQuery(this._view).bind("mouseClick.scrollbar mouseMove.scrollbar mouseWheel.scrollbar", function(e, val) {
		that._model.changeCurVal(val.change);
	});
}

iview.scrollbar.Controller.prototype = {
	
	/**
	 * @function
	 * @name		setSize
	 * @see			iview.scrollbar.Model#setSize
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setSize: function(value) {
		this._model.setSize(value);
	},
	
	/**
	 * @function
	 * @name		setMaxVal
	 * @see			iview.scrollbar.Model#setMaxVal
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setMaxValue: function(value) {
		this._model.setMaxVal(value);
	},
	
	/**
	 * @function
	 * @name		getMaxVal
	 * @see			iview.scrollbar.Model#getMaxVal
	 * @memberOf	iview.scrollbar.Controller#
	 */
	getMaxValue: function() {
		return this._model._maxVal;
	},
	
	/**
	 * @function
	 * @name		setCurVal
	 * @see			iview.scrollbar.Model#setCurVal
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setCurValue: function(value) {
		this._model.setCurVal(value);
	},
	
	/**
	 * @function
	 * @name		getCurVal
	 * @see			iview.scrollbar.Model#getCurVal
	 * @memberOf	iview.scrollbar.Controller#
	 */
	getCurValue: function() {
		return this._model._curVal;
	},
	
	/**
	 * @function
	 * @name		setProportion
	 * @see			iview.scrollbar.Model#setProportion
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setProportion: function(value) {
		this._model.setProportion(value);
	},
	
	/**
	 * @function
	 * @name		getProportion
	 * @see			iview.scrollbar.Model#getProportion
	 * @memberOf	iview.scrollbar.Controller#
	 */
	getProportion: function() {
		return this._model._proportion;
	},
	
	/**
	 * @function
	 * @name		setStepByClick
	 * @see			iview.scrollbar.View#setStepByClick
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setStepByClick: function(value) {
		this._view.setStepByClick(value);
	},
	
	/**
	 * @function
	 * @name		setJumpStep
	 * @see			iview.scrollbar.View#setJumpStep
	 * @memberOf	iview.scrollbar.Controller#
	 */
	setJumpStep: function(value) {
		this._view.setJumpStep(value);
	},
	
	/**
	 * @function
	 * @name		addEventFrom
	 * @see			iview.scrollbar.View#addEventFrom
	 * @memberOf	iview.scrollbar.Controller#
	 */
	addEventFrom: function(type, from) {
		this._view.addEventFrom(type, from);
	},
	
	/**
	 * @public
	 * @function
	 * @name		createView
	 * @memberOf	iview.scrollbar.Controller#
	 * @description	creates the scrollbar with the given Settings and some predefined settings as
	 *  scrollamount and initial delays
	 * @param		{Object} args which contains for example the main and custom CSS Class for the scrollbar,
	 *  as well as the direction and id of the scrollbar
	 * @param		{string} [args.mainClass]
	 * @param		{string} [args.customClass]
	 * @param		{string} [args.direction] will the scrollbar be horizontal or vertical
	 * @param		{string} [args.id] id the scrollbar shall get
	 */
	createView: function(args) {
		var that = this;
		this._view._type = args.type || "scroll";
		this._view.createView({'mainClass': args.mainClass, 'customClass': args.customClass, 'direction':args.direction}, args.id);
		this._view.addTo(args.parent);
		this._view.setStepByClick(5);
		this._view.setJumpStep(20);
		this._view.setScrollDelay(150);//initial delay after which the interval is started which automatically resumes scrolling unless a mouseup event is registered 
		this._view.setSpaceDelay(150);
		this._view.setIntervalTime(100);//period with which the scroll is automaticall resumed
		this.my = this._view.my;
	},
	
	/**
	 * @public
	 * @function
	 * @name		attach
	 * @memberOf	iview.scrollbar.Controller#
	 * @description	adds the given listener to the model so the listener will be notified about changes within the model
	 * @param		{string} event name of events to register the listener to
	 * @param		{function} listener to add to the model
	 */
	attach: function(event, listener) {
		jQuery(this._model).bind(event, listener);
	},
	
	/**
	 * @public
	 * @function
	 * @name		detach
	 * @memberOf	iview.scrollbar.Controller#
	 * @description	removes the given listener from the model so the listener will no longer receive
	 *  notifications about changes within the model
	 * @param		{string} event name of events to detach the listener from
	 * @param		{function} listener to add to the model
	 */
	detach: function(event, listener) {
		jQuery(this._model).unbind(event, listener);
	}
}

/**
 * @public
 * @function
 * @name		importScrollbars
 * @memberOf	iview.scrollbar
 * @description	creates scrollbars for x and y direction
 * @param		{iviewInst} viewer in which the bars shall be created in
 */
iview.scrollbar.importScrollbars = function(viewer) {
	/**
	 * @private
	 * @name		viewerPosUpdate
	 * @memberOf	iview.scrollbar.createScrollbars#
	 * @description	notifies the viewer about a change in the view port
	 * @param x		{integer} valueX number of pixels how far the bar has been moved horizontal
	 * @param y		{integer} valueY number of pixels how far the bar has been moved vertical
	 */
	var viewerPosUpdate = function(x, y) {
		var pos = {'x': x, 'y': y};
		viewer.viewerBean.positionTiles (pos);
	}

	// ScrollBars
	// horizontal
	viewer.scrollbars={};//TODO: make real Object
	var barX = viewer.scrollbars.x = new iview.scrollbar.Controller();
	barX.createView({ 'direction':'horizontal', 'parent':viewer.context.container, 'mainClass':'scroll'});
	barX.attach("curVal.scrollbar", function(e, val) {
		if (!viewer.roller) {
			viewerPosUpdate(-(val["new"]-val["old"]), 0);
		}
	});
	// vertical
	var barY = viewer.scrollbars.y = new iview.scrollbar.Controller();
	barY.createView({ 'direction':'vertical', 'parent':viewer.context.container, 'mainClass':'scroll'});
	barY.attach("curVal.scrollbar", function(e, val) {
		if (!viewer.roller) {
			viewerPosUpdate(0, -(val["new"]-val["old"]));
		}
	});
	var fnAdaptBars = function() {
		var ymaxVal = currentImage.curHeight - viewer.viewerBean.height;
		barY.setMaxValue((ymaxVal < 0)? 0:ymaxVal);
		barY.setProportion(viewer.viewerBean.height/currentImage.curHeight);
		
		var xmaxVal = currentImage.curWidth - viewer.viewerBean.width;
		barX.setMaxValue(xmaxVal);
		barX.setProportion(viewer.viewerBean.width/currentImage.curWidth);
		
		// correctly represent the new view position
		barX.setCurValue(-this.x);
		barY.setCurValue(-this.y);
		// set the new size of the scrollbar
		barY.setSize(viewer.viewerBean.height);
		barY.my.self[0].style.top = jQuery(viewer.viewerBean.viewer).offset().top + "px";
		barX.setSize(viewer.viewerBean.width);
	};
	
	var currentImage = viewer.currentImage;
	jQuery(currentImage).bind(iview.CurrentImage.POS_CHANGE_EVENT, function() {
		viewer.roller = true;
		barX.setCurValue(-this.x);
		barY.setCurValue(-this.y);
		viewer.roller = false;
	}).bind(iview.CurrentImage.DIMENSION_EVENT, function() {
		fnAdaptBars();
	});
	
	jQuery(viewer.viewerContainer).bind("reinit.viewer", function() {
		fnAdaptBars();
	});
	// Additional Events
	// register to scroll into the viewer
	viewer.context.viewer.mousewheel(function(e, delta, deltaX, deltaY) {
		e.preventDefault();
		viewerPosUpdate(deltaX*PanoJS.MOVE_THROTTLE, deltaY*PanoJS.MOVE_THROTTLE);
	});
	jQuery(viewer.viewerContainer).bind("maximize.viewerContainer minimize.viewerContainer", function() {
		if (viewer.viewerContainer.isMax()) {
			viewer.addDimensionSubstract(true, 'scrollbar', barX.my.self.outerHeight());
			viewer.addDimensionSubstract(false, 'scrollbar', barY.my.self.outerWidth());
		} else {
			viewer.removeDimensionSubstract(true, 'scrollbar');
			viewer.removeDimensionSubstract(false, 'scrollbar');
		}
	})
	viewer.addDimensionSubstract(true, 'scrollbar', barX.my.self.outerHeight());
	viewer.addDimensionSubstract(false, 'scrollbar', barY.my.self.outerWidth());
};
