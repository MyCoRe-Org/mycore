//TODO einbauen das Scrollbar minimale Größe hat, damit man sie noch benutzen kann

var iview = iview || {};
iview.scrollbar = iview.scrollbar || {};
/*
 * @package 	iview.scrollbar
 * @description
 */
/*
 * @name 		Model
 * @proto		Object
 * @description Modeldata for internal Scrollbar Representation
 */
iview.scrollbar.Model = function() {
	this._maxVal;//Max Value the Scrollbar can reach
	this._curVal = 0;//Current Value the scrollbar is positioned
	this._size;//Size of the Scrollbar complete
	this._proportion;//holds the proportion in percent from whole scrollbar area and the slider within
	this.onevent = new iview.Event(this);//One Event to rule them all
}

iview.scrollbar.Model.prototype = {
	
	/* GETTER/SETTER
	 * @description set/get the total size of the Scrollbar
	 * @param value integer which holds the new size of the scrollbar, the value is converted to an absolute value
	 * @return integer the currently set width
	 */
	setSize: function(value) {
		value = Math.abs(toInt(value));
		//only raise the Event if a change happened
		if (this._size != value) {
			var oldVal = this._size;
			this._size = value;
			//Notify all listeners that a change happened
			this.onevent.notify({ 'type': 'size', 'old': oldVal, 'new': value });
		}
	},
	
	getSize: function() {
		return this._size;
	},
	
	/* GETTER/SETTER
	 * @description set/get the proportion between scrollbarspace and the bar within the space
	 * @param value float which holds the proportion, the value is converted to an absolute value
	 * @return float which represents the current scrollbar space/bar proportion
	 */
	setProportion: function(value) {
		value = Math.abs(toFloat(value));
		//only raise Event if a change happened
		if (this._proportion != value) {
			var oldVal = this._proportion;
			this._proportion = value;
			this.onevent.notify({ 'type': 'proportion', 'old': oldVal, 'new':value });
		}
	},
	
	getProportion: function() {
		return this._proportion;	
	},
	
	/* GETTER/SETTER
	 * @description set/get the current maxValue of the scrollbar, this means the Value the scrollbar
	 *  returns when it reached the right/bottom most position, if the value is negative zero will be used instead
	 * @param value integer which holds the new maxValue for the scrollbar
	 * @return integer the currently set maxValue
	 */
	setMaxVal: function(value) {
		value = toInt(value);
		if (value < 0) value = 0;
		//only raise the Event if a change happened
		if (this._maxVal != value) {
			var oldVal = this._maxVal;
			this._maxVal = value;
			//Notify all listeners that a change happened
			this.onevent.notify({ 'type': 'maxVal', 'old': oldVal, 'new': value });
		}
	},
	
	getMaxVal: function() {
		return this._maxVal;
	},
	
	/* GETTER/SETTER
	 * @description set/get the current Value(position) where the bar start(left/right end) is positioned
	 * @param value integer which holds the new current Value, if the value is negative zero will be used instead
	 * @return integer the currently set Value
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
			this.onevent.notify({ 'type': 'curVal', 'old':oldVal, 'new':value });
		}
	},
	
	getCurVal: function() {
		return this._curVal;
	},
	
	/*
	 * @description in contrast to setCurValue this function doesn't treat the given
	 *  value to change the curValue absolute, rather than to change it relative to the current Value
	 * @param value integer which represents the change to apply
	 */
	changeCurVal: function(value) {
		value = toInt(value);
		var oldVal = this._curVal;
		this._curVal = this._curVal + value;
		if (this._curVal < 0) this._curVal = 0;
		if (this._curVal > this._maxVal) this._curVal = this._maxVal;
		//only raise Event if a change happened
		if (oldVal != this._curVal)
			this.onevent.notify({'type': 'curVal', 'old': oldVal, 'new': this._curVal});
	}
}
/********************************************************
 ********************************************************
 ********************************************************/
/*
 * @name 		Model
 * @proto		Object
 * @description View to represent Model 
 */
iview.scrollbar.View = function(args) {
	this.my;//holds all further needed Object References
	this._direction;//Is the scrollbar Horizontal or vertical
	this._mainClass = args.mainClass || "";
	this._customClass = args.customClass || "";
	this._size = 0;
	
	this._mouseDown = false;
	this._outOfBar = false;
	this._pixelPerUnit = 1;	
	this._maxVal = 1;

	this._proportion = 1;
	this._curVal = 0;
	this._oldPos;
	this._moveTo;//If pressing into the empty area this is the var where the position is stored
	this._before;//holds if the mouse is pressed behind or before the scrollbar
	this._interval = null;//holds the currently running interval

	this.onevent = new iview.Event(this);
};

( function() {
	
	/*
	 * @description sets the currently active interval, if an old interval is found it's replaced with the new one
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param intervalFunc function which is set to be called by the Intervaltime time
	 * @param time integer time in miliseconds which lays between two calls to the function call 
	 */
	function addInterval(that, intervalFunc, time) {
		if (that._interval != null) {
			dropInterval(that);
		}
		that._interval = setInterval(intervalFunc,time);
	}

	/*
	 * @description drops (if one is set) the currently executed interval and the connected function
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */	
	function dropInterval(that) {
		clearInterval(that._interval);
		that._interval = null;
	}

	/*
	 * @description after the initial mouseClick some time lays between the starting of the periodic
	 *  "recall" of the mouseClick on the button while the mouse is still pressed over the button
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param isIncrease boolean which holds if the current value shall be increased or decreased
	 */
	function delayMouseClick(that, isIncrease) {
		if (that._mouseDown) {
			addInterval(that, function() { notifyClick(that, isIncrease);}, that._intervalTime);
		}
	}
	
	/*
	 * @description after the initial mouseClick some time lays between the starting of the periodic
	 *  "recall" of the mouseClick on the space while the mouse is still pressed
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param isIncrease boolean which holds if the current value shall be increased or decreased
	 */
	function delaySpaceClick(that, before) {
		if (that._mouseDown) {
			addInterval(that, function() { stopUnderMouse(that);}, that._intervalTime);
		}
	}
	
	/*
	 * @description set the value by which the curValue is changed as soon as the Mouse is pressed on a button
	 * @param value integer defines the change which is applied as soon as a button is pressed,
	 *  value is converted to absolute value
	 */
	function setStepByClick(value) {
		this._stepByClick = Math.abs(toInt(value));
	}
	
	/*
	 * @description set the value by which the curValue is changed as soon as the mouse is pressed
	 *  within the space of the scrollbar
	 * @param value integer defines the change which is applied as soon as the mouse is pressed
	 *  within the space of the scrollbar
	 */
	function setJumpStep(value) {
		this._jumpStep = Math.abs(toInt(value));
	}

	/*
	 * @description set the time which lays between the initial click of on a button and the
	 *  periodic recall of the movement while the mouse is kept pressed
	 * @param value integer defines the time in miliseconds which lays between the initial click and
	 *  the periodic recall
	 */	
	function setScrollDelay(value) {
		this._scrollDelay = Math.abs(toInt(value));
	}
	
	/*
	 * @description set the time which lays between the initial click within space and the
	 *  periodic recall of the movement while the mouse is kept pressed
	 * @param value integer defines the time in miliseconds which lays between the initial click and
	 *  the periodic recall
	 */	
	function setSpaceDelay(value) {
		this._spaceDelay = Math.abs(toInt(value));
	}
	
	/*
	 * @description set the time in which all intervals are runned
	 * @param value integer the interval time which is used for all intervals which are set within the scrollbar
	 */
	function setIntervalTime(value) {
		this._intervalTime = Math.abs(toInt(value));
	}

	/*
	 * @description handles the initial Click when the mouse is pressed within the space. After calling the first
	 *  time stopUnderMouse it calls the delay Function so that an automatic movement can be applied
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param e event of the mousepress which raised this event
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
	
	/*
	 * @description handles the MouseUp within the space area and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function dbgMouseUp(that) {
		that._mouseDown = false;
		that._outOfBar = false;
	}
	
	/*
	 * @description removes all previously registered intervals and stores the position of the mousepress to handle
	 *  the mouse movement in other events correctly
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param e event of the mousepress which raised this event
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
	
	/*
	 * @description handles the MouseUp within the bar and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function divMouseUp(that) {
		dropInterval(that);
		that._mouseDown = false;
		that._outOfBar = false;
	}

	/*
	* @description if Drag&Drop is initialized it continously moves the bar so under the mouse that
	*  the MouseDown Position of Bar relative to Mouse is restored, similiar it places the Stepper
	* @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	* @param e event of the Current MouseMove with the Position of the mouse
	*/
	function divMouseMove(that, e) {
		if (!that._mouseDown || that._outOfBar) return;
		var value = null;
		var vector = null;
		//store the movement vector
		if (that._direction) {
			vector = e.clientX - that._oldPos.x;
		} else {
			vector = e.clientY - that._oldPos.y;
		}
		//notify all listeners of the event
		that.onevent.notify({'type':'mouseMove','change':vector/that._pixelPerUnit});
		//and store as new old position the current one
		that._oldPos = {'x': e.clientX, 'y': e.clientY};
		e.cancelBubble = true;
	}
	
	/*
	 * @description handles the MouseDown event on the scrollbar buttons, this includes removing all previously
	 *  run intervals as well as raising a first value change and adding an reoccuring event which is called
	 *  after an initial delay regulary as long as the mouse is kept pressed
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param downRight boolean which tells if the button which caused the event was the downRight one which is
	 *  represented by true, if it was the upLeft one the value is false
	 */
	function buttonMouseDown(that, downRight) {
		downRight = (downRight == true)? true:false;
		if (that._mouseDown) return;
		that._mouseDown = true;
		that._outOfBar = true;
		dropInterval(that);
		notifyClick(that, downRight);
		window.setTimeout(function() { delayMouseClick(that, downRight);}, that._scrollDelay);
		return false;
	}
	
	/*
	 * @description handles the MouseUp within the button and sets needed variables to the corresponding
	 *  values to set the scrollbar in the correct new state
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function buttonMouseUp(that) {
		dropInterval(that);
		that._mouseDown = false;
		that._outOfBar = false;
	}
	
	/*
	 * @description adds the scrollbar to the given parent
	 * @param parent DOM element to which the scrollbar is added
	 */
	function addTo(parent) {
		jQuery(this.my.self).appendTo(parent);
	}
	
	/*
	 * @description adapts the scrollbar to the new full size, so the space and bar sizes are adapted to fulfill
	 *  the new size and to look nicely
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 */
	function applySize(that) {
		var my = that.my;
		var size = that._size;
		if (that._direction) {
			my.self.css("width", size + "px");
			my.space.css("width", Math.abs(size - toInt(my.bUL.css("width")) - toInt(my.bDR.css("width"))) + "px");
			my.space.children("[class='space']").css("width", Math.abs(toInt(my.space.css("width")) - toInt(my.space.children('[class="start"]').css("width")) - toInt(my.space.children("[class='end']").css("width"))) + "px");
			if (toInt(my.bar.css("width")) > toInt(my.space.css("width")) || that._maxVal == 0) {
				my.bar.css("width", my.space.css("width"));
			}
			my.bar.children("[class='scroll']").css("width", Math.abs(toInt(my.bar.css("width")) - toInt(my.bar.children("[class='start']").css("width")) - toInt(my.bar.children("[class='end']").css("width"))) + "px");
		} else {
			my.self.css("height", size + "px");
			my.space.css("height", Math.abs(size - toInt(my.bUL.css("height")) - toInt(my.bDR.css("height"))) + "px");
			my.space.children("[class='space']").css("height", Math.abs(toInt(my.space.css("height")) - toInt(my.space.children('[class="start"]').css("height")) - toInt(my.space.children("[class='end']").css("height"))) + "px");
			if (toInt(my.bar.css("height")) > toInt(my.space.css("height")) || that._maxVal == 0) {
				my.bar.css("height", my.space.css("height"));
			}
			my.bar.children("[class='scroll']").css("height", Math.abs(toInt(my.bar.css("height")) - toInt(my.bar.children("[class='start']").css("height")) - toInt(my.bar.children("[class='end']").css("height"))) + "px");
		}
	}
	
	/*
	 * @description creates the scrollbar by creating all needed DOM Elements and setting some important
	 *  css values so the scrollbar can work correctly; connects all view functions to the related events
	 *  and objects
	 * @param direction (horizontal|vertical) tells if the bar will be vertical or horizontal
	 */
	function createView(direction) {
		var that = this;
		this._direction = (direction == "horizontal")? true:false;
		var complete = jQuery("<div>")
			.addClass(this._mainClass +((this._direction)? "H":"V"));
			
		//Add additional Class to override the overall scrollbar setting
		if (this._customClass != "") {
			complete.addClass(this._customClass +((this._direction)? "H":"V"));
		}
			
	//Start Up/Left
		var buttonUL = jQuery("<div>")
			.addClass("start")
			.mousedown(function() { return false;})
			.mousedown(function() { buttonMouseDown(that, false);})
			.appendTo(complete);
			
		var wholeSpace = jQuery("<div>")
			.addClass("empty")
			.mousedown(function(e) { dbgMouseDown(that, e); return false;})
			.mouseup(function() { dbgMouseUp(that); return false;})
			.mousewheel(function(e, delta) {
				that.onevent.notify({'type':'mouseWheel', 'change':2*-delta/that._pixelPerUnit}); 
				return false;})
			.appendTo(complete);

		var spaceStart = jQuery("<div>")
			.addClass("emptyStart")
			.appendTo(wholeSpace);
			
		var space = jQuery("<div>")
			.addClass("space")
			.appendTo(wholeSpace);
		
		var spaceEnd = jQuery("<div>")
			.addClass("emptyEnd")
			.appendTo(wholeSpace);
		
		var barScroll = jQuery("<div>")
			.addClass("bar")
			.mousedown(function(e) { divMouseDown(that, e); return false;})
			.mouseup(divMouseUp)
			.mousemove(function(e) { divMouseMove(that, e); return false;})
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
			.mousedown(function() { return false;})
			.mousedown(function() { buttonMouseDown(that, true);})
			.appendTo(complete);
		jQuery(document)
			.mouseup(function() { buttonMouseUp(that);})
			.mousemove(function(e) { divMouseMove(that, e);});
			
		if (this._direction) {
			wholeSpace.css("cssFloat", "left");
			space.css("cssFloat", "left");
			buttonUL.css("cssFloat", "left");
			buttonDR.css("cssFloat", "left");
			scroll.css("left", "0px");
		} else {
			scroll.css("top", "0px");
		}
		
		this.my = { 'self': complete, 'bUL': buttonUL, 'bDR': buttonDR, 'bar': barScroll, 'space': wholeSpace};
		applySize(this);
	}
	
	/*
	 * @description is called to adapt the view/scrollbar to display the changed values correctly to the user
	 * @param args object which holds the type and value of the property which changed so that the view
	 *  can be adapted to represent the new values correctly
	 */
	function adaptView(args/*, that*/) {
		var my = this.my;
		var direction = (this._direction)? "width":"height";
		switch (args.type) {
			case "curVal":
				this._curVal = args.value;
				break;
			case "proportion":
				//Set the new bar width
				this._proportion = args.value;
				break;
			case "maxVal":
				this._maxVal = args.value;
				break;
			case "size":
				this._size = args.value;
				applySize(this);
				break;
			default:
				if (console) {
					console.log("Got:"+args.type+" as argument type, don't know what to do with it");
				}
		}
		//calculate the bar size
		my.bar.css(direction ,this._proportion*toFloat(my.space.css(direction)) + "px");
		//calculate the pixelPerUnit rate to set the positioning of the bar correctly
		this._pixelPerUnit = Math.abs((toInt(my.space.css(direction))-toInt(my.bar.css(direction)))/this._maxVal);
		//IE throws as usual Errors if some value isn't in the expected range, so a 0 within maxVal gets him mad
		if (this._maxVal != 0) {
			my.bar.css((direction == "width")? "left":"top",this._curVal*this._pixelPerUnit + "px");
		}
		applySize(this);
	}

	/*
	 * @description is called to notify all listeners about an occured Value change caused by an click, depending
	 *  on the isIncrease boolean you can tell if the current value is increased or decreased
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
	 *  over the instance from which is called to work properly
	 * @param isIncrease boolean which tells if the curValue will be increased or decreased
	 */
	function notifyClick(that, isIncrease) {
		that.onevent.notify({'type':'mouseClick', 'change':(isIncrease === true)? that._stepByClick: -that._stepByClick});
	}

	/*
	 * @description when the mouse is pressed within the space of the bar this function is called, which checks if
	 *  the bar is already under the mouse and stops the reoccuring process if that is the case. Else it will
	 *  notify all listeners about the happened changes
	 * @param that Object as the function is just an "Class" one and not connected to an instance we need to handle
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
		var barLength = (that._direction)? toInt(that.my.bar.css("width")): toInt(that.my.bar.css("height"));
		if ((that._before && (that._moveTo >= curVal)) || (!that._before && (that._moveTo <= curVal + barLength))) {
			dropInterval(that)
			that._mouseDown = false;
			return;
		}
		//Notify all Listeners about the occured change, prepare the Event Object depending on the direction
		if (that._direction) {
			that.onevent.notify({'type': 'mouseClick', 'change':((that._before)?-that._jumpStep:that._jumpStep)});
		} else {
			that.onevent.notify({'type': 'mouseClick', 'change':((that._before)?-that._jumpStep:that._jumpStep)});
		}
	}
	
	iview.scrollbar.View.prototype.createView = createView;
	iview.scrollbar.View.prototype.addTo = addTo;
	iview.scrollbar.View.prototype.adaptView = adaptView;
	iview.scrollbar.View.prototype.setStepByClick = setStepByClick;
	iview.scrollbar.View.prototype.setJumpStep = setJumpStep;
	iview.scrollbar.View.prototype.setScrollDelay = setScrollDelay; 
	iview.scrollbar.View.prototype.setSpaceDelay = setSpaceDelay;
	iview.scrollbar.View.prototype.setIntervalTime = setIntervalTime;
})();

/********************************************************
 ********************************************************
 ********************************************************/
/*
 * @name 		Model
 * @proto		Object
 * @description Controller for Scrollbars
 */
iview.scrollbar.Controller = function() {
	this._model = new iview.scrollbar.Model();
	this._view;
}

iview.scrollbar.Controller.prototype = {
	
	setSize: function(value) {
		this._model.setSize(value);
	},
	
	setMaxValue: function(value) {
		this._model.setMaxVal(value);
	},
	
	getMaxValue: function() {
		return this._model.getMaxVal();
	},
	
	setCurValue: function(value) {
		this._model.setCurVal(value);
	},
	
	getCurValue: function() {
		return this._model.getCurVal();
	},
	
	setProportion: function(value) {
		this._model.setProportion(value);
	},
	
	getProportion: function() {
		return this._model.getProportion();
	},
	
	/*
	 * @description creates the scrollbar with the given Settings and some predefined settings as
	 *  scrollamount and initial delays
	 * @param args Object which contains for example the main and custom CSS Class for the scrollbar,
	 *  as well as the direction and id of the scrollbar
	 */
	createView: function(args) {
		var that = this;
		this._view = new iview.scrollbar.View({'mainClass': args.mainClass || "", 'customClass': args.customClass || ""});
		this._model.onevent.attach(function(sender, args) {
			 that._view.adaptView({'type':args.type,'value':args["new"]});
		});
		this._view.onevent.attach(function(sender, args) {
			if (args.type == "mouseClick" || args.type == "mouseMove" || args.type == "mouseWheel") {
				that._model.changeCurVal(args.change);
			}
		});
		this._view.createView(args.direction, args.id);
		this._view.addTo(args.parent);
		this._view.setStepByClick(5);
		this._view.setJumpStep(20);
		this._view.setScrollDelay(150);//initial delay after which the interval is started which automatically resumes scrolling unless a mouseup event is registered 
		this._view.setSpaceDelay(150);
		this._view.setIntervalTime(100);//period with which the scroll is automaticall resumed
		this.my = this._view.my;
	}
}

function scrollBar(newId) {
//Constants
	scrollBar.STARTCL = "start";
	scrollBar.ENDCL = "end";
	scrollBar.EMPTYCL = "empty";
	scrollBar.UNIT = "px";
	scrollBar.LIST_MOVE = 1;
	scrollBar.LIST_STEP = 2;
	scrollBar.JUMPSTEP = 30;
	scrollBar.STEP =10;
	scrollBar.INTERVAL = 100;//How often is the scrolling done
	scrollBar.SCROLL_DELAY = 300;//How big is the initial scrolling Delay
	scrollBar.NO_REPEAT = false;//On the Empty space by current mouse press scroll repeatedly or not

	var id = newId;
	var identer = "";
	if (typeof arguments[1] != "undefined" && arguments[1] != "")
		identer = arguments[1];

	var parent = "";
	if (typeof arguments[2] != "undefined" && arguments[2] != "")
		parent = arguments[2];

	var bar = null;
	var step = scrollBar.STEP;
	var jumpStep = scrollBar.JUMPSTEP;
	var scrollDelay = scrollBar.SCROLL_DELAY;
	var horz = null;//horz true: horizontal, false: vertical
	var pictures = null;//holds a array of pictures which are displayed
	var timeInt = scrollBar.INTERVAL;//Intervall between single clicks while mouse is pressed over Move Buttons
	var mouseDown = false;//to get if the Mouse is pressed or not
	var outOfBar = false;//stores if the mouse was pressed outside the moving Barpart and blocks the normal mousedown Move behaviour of the bar
	var oldPos = { 'x': null, 'y': null };//Holds the old MousePosition
	var oldStep = 0;//Holds the last Step position
	var listener = {};// holds Arrays of all Listener
	var interval = null;//holds the currently active interval, needed to drop it later
	var stepper = false;//holds if the ScrollBar is used as Stepper or not
	var steps = 0;
	var jumpStepOld = 0;
	var noRepeat = scrollBar.NO_REPEAT;//holds if by pressed on the Empty space the Mouse moves while the mouse keeps pressed 
	var curValue = 0;//holds the value which the Bar currently represents
	var maxValue = 0;//holds the maximum Value which can be set
	var margin = 0;
	var areaRatio = 1;
	var size = 0;
	var my = null;

//Function declarations:	
	this.init = init;
	this.addListener = addListener;
	this.dropListener = dropListener;
	this.setTimeInt = setTimeInt;
	this.getTimeInt = getTimeInt;
	this.setValue = setValue;
	this.getValue = getValue;
	this.setSize = setSize;
	this.getSize = getSize;
	this.setLength = setLength;
	this.getLength = getLength;
	this.setJumpStep = setJumpStep;
	this.getJumpStep = getJumpStep;
	this.setStepper = setStepper;
	this.getStepper = getStepper;
	this.setSteps = setSteps;
	this.getSteps = getSteps;
	this.setMaxValue = setMaxValue;
	this.getMaxValue = getMaxValue;
	this.setMargin = setMargin;
	this.getMargin = getMargin;
	this.setParent = setParent;
	this.setNoRepeat = setNoRepeat;
	this.getNoRepeat = getNoRepeat;
	this.setScrollAmount = setScrollAmount;
	this.getScrollAmount = getScrollAmount;
	this.setScrollDelay = setScrollDelay;
	this.getScrollDelay = getScrollDelay;

	this.my = null;//holds the Elements itself in a named kind;
//Event registration features;
	this.mouseUp = null;
	this.mouseMove = null;
	this.scroll = null;

	function addInterval(intervalFunc, time) {
		if (interval != null) {
			dropInterval();
		}
		interval = setInterval(intervalFunc,time);
	}
	
	function dropInterval() {
		clearInterval(interval);
		interval = null;
	}
	/*
	@description creates a new fully featured ScrollBar
	@param horizontal boolean which defines if the scrollbar is used as horizontal one or vertical
	*/
	function init(horizontal) {
		this.mouseUp = divMouseUp;
		this.mouseMove = divMouseMove;
		this.scroll = dbgMouseWheel;

		/*
		@description notifies all Listener when a move of the Scrollbar happened, occurs by MouseScroll, Drag&Drop and Button Clicking
		@param vector vector which holds the new X & Y Position/Value of the Scrollbar
		*/
		function notifyListenerMove(vector) {
			if (!listener[scrollBar.LIST_MOVE]) {
				return;
			}
			for(var i = 0; i < listener[scrollBar.LIST_MOVE].length; i++) {
				listener[scrollBar.LIST_MOVE][i].moved(vector);
			}
		}

		/*
		@description Notifies all Listeners if a Stepping taked place, it will return the current step
		*/
		function notifyListenerStep() {
			if (!listener[scrollBar.LIST_STEP]) {
				return;
			}
			var newStep = 0;
			var move = 0;
			if (horz) {
				newStep = Math.ceil((parseInt(bar.style.left)) / bar.offsetWidth);
			} else {
				newStep = Math.ceil((parseInt(bar.style.top)) / bar.offsetHeight);
			}
			move = oldStep - newStep;
			oldStep = newStep;
			if (move == 0) return;
			for (var i = 0; i < listener[scrollBar.LIST_STEP].length; i++) {
				listener[scrollBar.LIST_STEP][i].steped(-move);
			}
		}

		/*
		@description realizes that the Scrollbar can be moved upwards/leftwards
		*/
		function moveUp() {
			if (stepper) {
				var value = 0;
				if (horz) {
					value = parseInt(bar.style.left) - bar.offsetWidth;
					if (value < 0)
						value = 0;
					bar.style.left = value + scrollBar.UNIT;
				} else {
					value = parseInt(bar.style.top) - bar.offsetHeight;
					if (value < 0)
						value = 0;
					bar.style.top = value + scrollBar.UNIT;
				}
				notifyListenerStep();
			} else {
				var move = step;
				if ((typeof arguments[0] != "undefined") && !isNaN(parseInt(arguments[0]))) {
					move = parseInt(arguments[0]);
				}
				//move = Math.round(move / areaRatio);
				curValue -= move;
				if (horz) {
					if (curValue <= 0) {
						move = Math.abs(move + curValue);
						curValue = 0;
						bar.style.left = margin + scrollBar.UNIT;
						dropInterval();
					} else {
						bar.style.left= Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': -move, 'y': 0});
				} else {
					if (curValue <= 0) {
						move = Math.abs(move + curValue);
						curValue = 0;
						bar.style.top = margin + scrollBar.UNIT;
						dropInterval();
					} else {
						bar.style.top = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': 0, 'y': -move});
				}
			}
		}

		/*
		@description realizes that the Scrollbar can be moved downwards/rightwards
		*/
		function moveDown() {
			if (!stepper) {
				var move = step;
				if ((typeof arguments[0] != "undefined") && !isNaN(parseInt(arguments[0]))) {
					move = parseInt(arguments[0]);
				}
				//move = Math.round(move / areaRatio);
				curValue += move;
				if (!horz) {
					if (curValue >= maxValue) {
						move = curValue - maxValue;
						curValue = maxValue;
						bar.style.top = bar.parentNode.offsetHeight - bar.offsetHeight - margin + scrollBar.UNIT;
						dropInterval();
					} else {
						bar.style.top = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': 0, 'y': move});
				} else {
					if (curValue >= maxValue) {
						move = curValue - maxValue;
						curValue = maxValue;
						bar.style.left = bar.parentNode.offsetWidth - bar.offsetWidth - margin + scrollBar.UNIT;
						dropInterval();
					} else {
						bar.style.left = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': move, 'y': 0});
				}
			} else {
				var value = 0;
				if (!horz) {
					value = parseInt(bar.style.top) + bar.offsetHeight;
					if (value + bar.offsetHeight >= bar.parentNode.offsetHeight)
						value = bar.parentNode.offsetHeight - bar.offsetHeight;
					bar.style.top = value + scrollBar.UNIT;
				} else {
					value = parseInt(bar.style.left) + bar.offsetWidth;
					if (value + bar.offsetWidth >= bar.parentNode.offsetWidth)
						value = bar.parentNode.offsetWidth - bar.offsetWidth;
					bar.style.left = value + scrollBar.UNIT;
				}
				notifyListenerStep();
			}
		}

		/*
		@description checks each time before a movement is raised if the next Scrollbar Position is already under the mouse or not, if so it stops further movement after a final move which brings it under the mouse
		@param e event of the Mousedown/Click Event which holds the e.clientY value which holds the initial mouse Click Position
		@param before boolean which defines if the mouseclick was in front of the bar or behind the bar (left/top or right/bottom of the bar)
		*/
		function stopUnderMouse(e, before) {
			var layerX = (e.layerX)? e.layerX:e.offsetX;
			var layerY = (e.layerY)? e.layerY:e.ofsetY;
			var jump = jumpStep*areaRatio;

			if (before) {
				if (((parseInt(bar.style.left) - jump) <= layerX) && horz) {
					dropInterval();
					moveUp(parseInt(bar.style.left)  - layerX);
				} else if ((parseInt(bar.style.top) - jump <= layerY) && !horz) {
					dropInterval();
					moveUp(parseInt(bar.style.top) - layerY);
				} else {
					moveUp(jumpStep);
				}
			} else {
				if (((parseInt(bar.style.left) + bar.offsetWidth + jump) >= layerX) && horz) {
					dropInterval();
					moveDown(layerX - parseInt(bar.style.left) - bar.offsetWidth);
				} else if (((parseInt(bar.style.top) + bar.offsetHeight + jump) >= layerY) && !horz) {
					dropInterval();
					moveDown(layerY - parseInt(bar.style.top) - bar.offsetHeight);
				} else {
					moveDown(jumpStep);
				}
			}
		}
		//Event Listener
		/*
		@description starts the autoscrolling Process by calling (delayed) the function which sets the Autoscroll period
		@param e Event which holds the current Event
		*/
		function upLeftMouseDown(e) {
			if (mouseDown) return;
			mouseDown = true;
			outOfBar = true;
			dropInterval();
			moveUp();
			window.setTimeout(function() { delayUpLeft();},scrollDelay);
			return false;
		}
		
		/*
		 @description starts the autoscrolling when the mouse is kept pressed
		 */
		function delayUpLeft() {
			if (mouseDown) {
				addInterval(function() { moveUp();}, timeInt);
			}
		}

		/*
		@description stops the continous Movement after the mouse is released
		@param e event which holds the current MouseEvent
		*/
		function upLeftMouseUp(e) {
			dropInterval();
			mouseDown = false;
			outOfBar = false;
		}
		/*
		@description Starts the Drag&Drop of the Button by saving the initial Position of the Bar related to the mouse Position
		@param e event which holds the Event happening Position
		*/
		function divMouseDown(e) {
			if (mouseDown) return;
			dropInterval();
			e = getEvent(e);
			mouseDown = true;
			oldPos = {'x': e.clientX, 'y': e.clientY};
			e.cancelBubble = true;
/*			if (stepper) 
				bar.parentNode.onmousemove = divMouseMove;*/
			return false;
		}
		/*
		@description Stops the Drag&Drop of the Bar
		@param e event which holds the mouseUp Event
		*/
		function divMouseUp(e) {
			dropInterval();
			e = getEvent(e);
			mouseDown = false;
			outOfBar = false;
		}

		/*
		@description if Drag&Drop is initialized it continously moves the bar so under the mouse that the MouseDown Position of Bar relative to Mouse is restored, similiar it places the Stepper
		@param e event of the Current MouseMove with the Position of the mouse
		*/
		function divMouseMove(e) {
			e = getEvent(e);
			if (!mouseDown || outOfBar) {
				return;
			}
			var value = null;
			var vector = null;
			if (stepper) {
				if (horz) {
					//TODO: horz Stepper not tested yet
					
					value = parseInt(bar.style.left);
					vector = parseInt((e.clientX - oldPos.x) / bar.offsetWidth) * bar.offsetWidth;
					//TODO: uses margin for stepper ?!
					if (vector != 0) {
						if ((value + vector) < margin) {
							vector = margin - value;
							value = margin;
						} else if (value + bar.offsetWidth + vector >= bar.parentNode.offsetWidth - margin) {
							value = bar.parentNode.offsetWidth - bar.offsetWidth - margin;
							vector = value - parseInt(bar.style.left);
						} else {
							value = value + vector;
						}
						bar.style.left = value + scrollBar.UNIT;
						oldPos.x = oldPos.x + vector;
					}
				} else {
					value = parseInt(bar.style.top);
					vector = parseInt((e.clientY - oldPos.y) / bar.offsetHeight) * bar.offsetHeight;
					// TODO: uses margin for stepper ?!
					if (vector != 0) {
						if ((value + vector) < margin) {
							vector = margin - value;
							value = margin;
						} else if (value + bar.offsetHeight + vector >= bar.parentNode.offsetHeight - margin) {
							value = bar.parentNode.offsetHeight - bar.offsetHeight - margin;
							vector = value - parseInt(bar.style.top);
						} else {
							value = value + vector;
						}
						bar.style.top = value + scrollBar.UNIT;
						oldPos.y = oldPos.y + vector;
					}
				}
				notifyListenerStep();
			} else {
				if (horz) {
					value = parseInt(bar.style.left);
					vector = e.clientX - oldPos.x;
					if ((value + vector) < margin ) {
						vector = margin - value;
						value = margin;
					} else if ((value + bar.offsetWidth + vector) >= bar.parentNode.offsetWidth - margin) {
						value = bar.parentNode.offseWidth - bar.offsetWidth - margin;
						vector = (bar.parentNode.offsetHeight - bar.offsetHeight - margin) - bar.offsetTop;
					} else {
						value = value + vector;
					}
					bar.style.left = value + scrollBar.UNIT;
					notifyListenerMove({'x': vector / areaRatio, 'y': 0});
				} else {
					value = parseInt(bar.style.top);
					vector = e.clientY - oldPos.y;
					if ((value + vector) < margin) {
						vector = margin - value;
						value = margin;
					} else if ((value + bar.offsetHeight + vector) >= bar.parentNode.offsetHeight - margin) {
						value = bar.parentNode.offsetHeight - bar.offsetHeight - margin;
						vector = (bar.parentNode.offsetHeight - bar.offsetHeight - margin) - bar.offsetTop;
					} else {
						value = value + vector;
					}
					bar.style.top = value + scrollBar.UNIT;
					notifyListenerMove({'x': 0, 'y': vector/areaRatio});
				}
				curValue = Math.round(toFloat(value / areaRatio));
				oldPos = {'x': e.clientX, 'y': e.clientY};
			}
			e.cancelBubble = true;
			return false;
		}

		function divMouseOver(e) {
			//e = getEvent(e);
		}
		/*
		@description initializes the Bar movement so that it later can stop under the mouse
		@param e event which holds the MouseClick Position
		*/
		function dbgMouseDown(e) {
			if (mouseDown) return;
			dropInterval();
			e = getEvent(e);
			mouseDown = true;
			outOfBar = true;
			var layerX = (e.layerX)? e.layerX:e.offsetX;
			var layerY = (e.layerY)? e.layerY:e.offsetY;
			if ((((parseInt(bar.style.top) + bar.offsetHeight) < layerY) && !horz)
				|| (((parseInt(bar.style.left) + bar.offsetWidth) < layerX) && horz)) {
				stopUnderMouse(e, false);
				var ev = {'layerX': layerX, 'layerY': layerY};
				if (!noRepeat && mouseDown) {
					addInterval(function() { stopUnderMouse(ev, false);}, timeInt);
				}
			} else if (((layerY < parseInt(bar.style.top)) && !horz) 
				|| ((layerX < parseInt(bar.style.left)) && horz)) {
				stopUnderMouse(e, true);
				var ev = {'layerX': layerX, 'layerY': layerY};
				if (!noRepeat && mouseDown) {
					addInterval(function() { stopUnderMouse(ev, true);}, timeInt);
				}
			}
			e.cancelBubble = true;
			return false;
		}

		/*
		@description Stops the Bar Movement as soon as the MouseButton isn't longer pressed, so it could be that the Bar didn't stoped under the mouse
		@param e event which raises this event
		*/
		function dbgMouseUp(e) {
			mouseDown = false;
			outOfBar = false;
			dropInterval();
		}

		/*
		@description if a MouseScroll is done it calls once a Bar moveup or movedown and cancels any further bubbling
		@param e event which holds the mousedelta which allows it to decide if scrolling down or up happened
		*/
		function dbgMouseWheel(e) {
			e = getEvent(e);
			if (returnDelta(e, true).y > 0) {
				moveUp();
			} else {
				moveDown();
			}
			e.cancelBubble = true;
		}

		/*
		@description starts the autoscrolling Process by calling (delayed) the function which sets the Autoscroll period
		@param e Event which holds the current Event
		*/
		function downRightMouseDown(e) {
			if (mouseDown) return;
			mouseDown = true;
			outOfBar = true;
			dropInterval();
			moveDown();
			window.setTimeout(function() {delayDownRight()},scrollDelay);
			return false;
		}
		
		/*
		 @description starts the autoscrolling when the mouse is kept pressed
		 */
		function delayDownRight() {
			if (mouseDown) {
				addInterval(function() { moveDown();}, timeInt);
			}
		}
		
		/*
		@description stops the move down/right of the Bar as soon as the Button is released
		@param e event which happened
		*/
		function downRightMouseUp(e) {
			dropInterval();
			mouseDown = false;
			outOfBar = false;
		}
	//Start Init Function creation of the Div Structure and registering all needed Events so that the Scrollbar is fully functional
		horz = horizontal;
		var complete = document.createElement("div");
		complete.id = id;
		complete.className = identer+((horz)? "H":"V");//new
		if (parent != "")
			document.getElementById(parent).appendChild(complete);

	//Start Up/Left
		var buttonUL = document.createElement("div");
		buttonUL.onmousedown = function() {return false;};
		buttonUL.className = scrollBar.STARTCL;// + identer;
		complete.appendChild(buttonUL);
		ManageEvents.addEventListener(buttonUL, "mousedown", upLeftMouseDown, false);
		ManageEvents.addEventListener(buttonUL, "mouseup", upLeftMouseUp, false);
	//END
		var wholeSpace = document.createElement("div");
		wholeSpace.onmousedown = function() {return false;};
		var space = document.createElement("div");
		wholeSpace.className = scrollBar.EMPTYCL;// + identer;
		var spaceStart = document.createElement("div");
		spaceStart.className = "spaceStart";// + identer;
		wholeSpace.appendChild(spaceStart);
		space.className = (horz)? "eHorizontal"/* + identer*/: "eVertical" /*+ identer*/;
		complete.appendChild(wholeSpace);
		wholeSpace.appendChild(space);
		var spaceEnd = document.createElement("div");
		spaceEnd.className = "spaceEnd";// + identer;
		wholeSpace.appendChild(spaceEnd);
		ManageEvents.addEventListener(wholeSpace, "mousescroll", dbgMouseWheel, false);
		ManageEvents.addEventListener(wholeSpace, "mousedown", dbgMouseDown, false);
		ManageEvents.addEventListener(wholeSpace, "mouseup", dbgMouseUp, false);
	//ScrollBar
		var barScroll = document.createElement("div");
		barScroll.onmousedown = function() {return false;};
		var barStart = document.createElement("div");
		barStart.className = "barStart";
		
		barScroll.appendChild(barStart);
		var scroll = document.createElement("div");
		scroll.className = (horz)? "horizontal" /*+ identer*/ : "vertical" /*+ identer*/;
		barScroll.id = id + "Bar";
		barScroll.className = complete.className + "Bar";//new
		wholeSpace.appendChild(barScroll);
		ManageEvents.addEventListener(barScroll, "mousedown", divMouseDown, false);
		ManageEvents.addEventListener(barScroll, "mouseup", divMouseUp, false);
		ManageEvents.addEventListener(barScroll, "mousemove", divMouseMove, false);
		ManageEvents.addEventListener(barScroll, "mouseover", divMouseOver, false);
		bar = barScroll;
		barScroll.appendChild(scroll);
		var barEnd = document.createElement("div");
		barEnd.className = "barEnd";

		barScroll.appendChild(barEnd);
	//END
	//Ending Down/Right
		var buttonDR = document.createElement("div");
		buttonDR.onmousedown = function() {return false;};
		buttonDR.className = scrollBar.ENDCL;// + identer;
		ManageEvents.addEventListener(buttonDR, "mousedown", downRightMouseDown, false);
		ManageEvents.addEventListener(buttonDR, "mouseup", downRightMouseUp, false);

		if (horz) {
			wholeSpace.style.cssFloat = "left";
			wholeSpace.style.styleFloat = "left";
			space.style.styleFloat = "left";
			space.style.cssFloat = "left";
			buttonUL.style.cssFloat = "left";
			buttonUL.style.styleFloat = "left";
			buttonDR.style.cssFloat = "left";
			buttonDR.style.styleFloat = "left";
			scroll.style.left = 0 + scrollBar.UNIT;//initialize
		} else {
			scroll.style.top = 0 + scrollBar.UNIT;//initialize
		}
		complete.appendChild(buttonDR);
		//Defining some Shortcuts for internal and external easier usage of the Object
		this.my = {'self': complete, 'bUL': buttonUL, 'bDR': buttonDR, 'bar': bar, 'space': wholeSpace};
		my = this.my;
	}

//GET/SET Property
	/*
	@SET/GET TimeInt defines how many time between two Move Events takes place, eg when the Down/Right or Top/Button is pressed
	@param value Non negative Integer which holds the timespan between two such events, in ms
	@return integer the time in ms which takes place between two Move Events
	*/
	function setTimeInt(value) {
		timeInt = parseInt(value);
		if (timeInt < 0) {
			timeInt = -timeInt;
		}
	}

	function getTimeInt() {
		return timeInt;
	}
	/*
	@SET/GET Value Sets the current Position of the Scrollbar
	@param value Non negative Integer which holds the new Position/Value of the Scrollbar
	@return integer the current Position/Value the Scrollbar holds
	*/
	function setValue(value) {
		value = parseInt(value);
		if (stepper) {
			if (value < 0) return;
			if (horz) {
				value = value * bar.offsetWidth;
				if (value + bar.offsetWidth > bar.parentNode.offsetWidth)
					value = bar.parentNode.offsetWidth - bar.offsetWidth;
				bar.style.left = value + scrollBar.UNIT;
				oldStep = Math.ceil(value / bar.offsetWidth);
			} else {
				value = value * bar.offsetHeight;
				if (value + bar.offsetHeight > bar.parentNode.offsetHeight)
					value = bar.parentNode.offsetHeight - bar.offsetHeight;
				bar.style.top = value + scrollBar.UNIT;			
				oldStep = Math.ceil(value / bar.offsetHeight);
			}
		} else {
			if (value < 0) {
				value = -value;
			}
			curValue = value;
			value = Math.round(value * areaRatio);//Converting to Pixel equivalent
			if (horz) {
				if (bar.offsetWidth + value > bar.parentNode.offsetWidth - margin) {
					bar.style.left = bar.parentNode.offsetWidth - bar.offsetWidth - margin + scrollBar.UNIT;
				} else {
					bar.style.left = value + margin + scrollBar.UNIT;
				}
			} else {
				if (bar.offsetHeight + value > bar.parentNode.offsetHeight - margin) {
					bar.style.top = bar.parentNode.offsetHeight - bar.offsetHeight  - margin + scrollBar.UNIT;
				} else {
					bar.style.top = value + margin + scrollBar.UNIT;
				}
			}
		}
	}

	function getValue() {
		if (stepper) {
			if (horz) {
				return Math.ceil((parseInt(bar.style.left)) / bar.offsetWidth);
			} else {
				return Math.ceil((parseInt(bar.style.top))/ bar.offsetHeight);
			}
		} else {
			return curValue;
		}
	}
	/*
	@SET/GET SetLength defines the Length of the Scrollbar
	@param value Non negative Integer which holds the new Size of the Scrollbar
	@return integer the size of the Scrollbar
	*/
	function setLength(value) {
		if (stepper) return false;//When Stepper active no Sizing
		value = parseInt(value);
		if (value < 0) {
			value = -value;
		}
		if (horz) {
			if (value > my.space.offsetWidth && my.space.offsetWidth > 0) {
				bar.style.width = my.space.offsetWidth + scrollBar.UNIT;	
			} else {
				bar.style.width = value + scrollBar.UNIT;
			}
		} else {
			if (value > my.space.offsetHeight && my.space.offsetHeight > 0) {
				bar.style.height = my.space.offsetHeight + scrollBar.UNIT;
			} else {
				bar.style.height = value + scrollBar.UNIT;
			}
		}
		configure();
		applySize();
	}

	function getLength() {
		if (horz) {
			return parseInt(bar.offsetWidth);
		} else {
			return parseInt(bar.offsetHeight);
		}
	}

	/*
	@SET/GET NoRepeat defines if the bars keeps moving after pressing within the empty space or moves just once
	@param bool Boolean which tells if Repeating is disabled or not
	@return boolean which holds the currently applied Repeating mode
	*/
	function setNoRepeat(bool) {
		noRepeat = (bool == true)? true:false;	
	}
	
	function getNoRepeat() {
		return noRepeat;
	}
	
	/*
	@SET/GET ScrollDelay defines the initial delay when the mouse is kept pressed over the upLeft or downRight Scroll Buttons
	@param value Non negative Integer which holds the inital delay before the period starts
	@return integer which holds the currently applied initial delay
	*/
	function setScrollDelay(value) {
		scrollDelay = toInt(value);
		if (scrollDelay < 0) scrollDelay = -scrollDelay;	
	}
	
	function getScrollDelay() {
		return scrollDelay;
	}
	
	/*
	@SET/GET ScrollAmount defines the amount of change(within 0 and the MaxValue) which is done by each click on the Mouse Up/Down Buttons and Scrollevents
	@param value non negative Float which holds the amount of change applied by each Move Event 
	@return float which holds the amount of change currently used
	*/
	function setScrollAmount(value) {
		step = toFloat(value);
		if (step < 0) step = -step;
	}
	
	function getScrollAmount() {
		return step;
	}

	/*
	@SET/GET Size defines the complete Size of the Scrollbar
	@param value Non negative Integer which holds the Size of the complete Scrollbar
	@return integer the size of the complete Scrollbar
	*/
	function setSize(value) {
		value = parseInt(value);
		if (value < 0) {
			value = -value;
		}
		size = value;
		applySize();
		configure();
		if (stepper)
			prepareStepper();
	}

	function getSize() {
		return size;
	}
	/*
	@SET/GET JumpStep defines the value difference between two Positions which are caused by The Mousedown over the Scrollarea
	@param value Non negative Integer which defines the difference between two positions
	@return integer the Value difference between two Positions
	*/
	function setJumpStep(value) {
		value = parseInt(value);
		if (value < 0) {
			value = -value;
		}
		jumpStep = value;
	}
	
	function getJumpStep() {
		return jumpStep;
	}
	/*
	@SET/GET Stepper defines if the Bar will behave as a Stepper or normal Scrollbar
	@param value Boolean which defines if it shall behave like a stepper or not
	@return boolean which holds if the Scrollbar behaves like a Scrollbar or Stepper
	*/
	function setStepper(value) {
		if (value) {
			stepper = true;
			jumpStepOld = jumpStep;
		} else {
			stepper = false;
			jumpStep = jumpStepOld;
		}
	}

	function getStepper() {
		return stepper;
	}
	/*
	@SET/GET Steps sets the number of Steps the Stepper has
	@param value Boolean which defines if it shall behave like a stepper or not
	@return boolean which holds if the Scrollbar behaves like a Scrollbar or Stepper
	*/
	function setSteps(value) {
		value = parseInt(value);
		if (value < 0) {
			value = -value;
		} else if (value == 0) {
			value = 1;
		}
		steps = value;
		if (stepper) {
			prepareStepper();
			applySize();
		}
	}

	function getSteps() {
		return steps;
	}
	/*
	@SET/GET MaxValue defines the max possible Value the Scrollbar can have
	@param value non negative Integer which holds the max possible Value the Scrollbar shall can get
	@return integer  value which is max possible 
	*/
	function setMaxValue(value) {
		value = parseInt(value);
		if (value < 0) 
			value = -value;
		maxValue = value;
		configure();
	}

	function getMaxValue() {
		return maxValue;
	}
	/*
	@SET/GET  Margin sets the Offset of the scroll area where the Bar can be moved, so it's possible that the bar doesn't move to the top
	@param value non negative Integer which holds the Margin for the Scrollarea
	@return integer Scrollarea margin
	*/
	function setMargin(value) {
		value = parseInt(value);
		if (value < 0)
			value = -value;
		margin = value;
		configure();
	}

	// TODO: margin-Funktionalität noch nicht fixiert, fehlerhaltig --> TEST
	function getMargin() {
		return margin;
	}
	/*
	@description if the Scrollbar was created in the first place without defining a Parent Element it's possible to add the Scrollbar with this Function to a Parent
	@param name string which holds the ID of the Object which shall become the Parent of this Scrollbar
	*/
	function setParent(name) {
		parent = name;
		document.getElementById(name).appendChild(this.my.self);
		//getOffset();
	}
//EVENT LISTENER Functions
	/*
	@description Adds/Drops a Listener for the given Event,so the Listener will be noticed if any changes will happen for this Event
	@param type constant which defines to which type of listener it shall be added/dropped
	@param theListener Object which holds a special function which is called if a Event happens
	*/
	function addListener(type, theListener) {
		if (!listener[type]) {
			listener[type] = [];
		}
		listener[type].push(theListener);
	}

	function dropListener(type, theListener) {
		for (var i = 0; i < listener[type].length; i++) {
			if (listener[type][i] == theListener) {
				listener[type].splice(i,1);
			}
		}
	}
//UTIL Functions
	/*
	@description Sets all Parts of the Scrollbar to the correct size depending on the given Values like total Size, Size of the Buttons and so on
	*/
	function applySize() {
		if (horz) {
			my.self.style.width = size + scrollBar.UNIT;
			my.space.style.width = Math.abs(size - my.bUL.offsetWidth - my.bDR.offsetWidth) + scrollBar.UNIT;
			my.space.childNodes[1].style.width = Math.abs(my.space.offsetWidth - my.space.childNodes[0].offsetWidth - my.space.childNodes[2].offsetWidth) + scrollBar.UNIT;
			if (my.bar.offsetWidth > my.space.offsetWidth) 
				my.bar.style.width = my.space.style.width;
			my.bar.childNodes[1].style.width = Math.abs(my.bar.offsetWidth - my.bar.childNodes[0].offsetWidth - my.bar.childNodes[2].offsetWidth) + scrollBar.UNIT;
		} else {
			my.self.style.height = size + scrollBar.UNIT;
			my.space.style.height = Math.abs(size - my.bUL.offsetHeight - my.bDR.offsetHeight) + scrollBar.UNIT;
			my.space.childNodes[1].style.height = Math.abs(my.space.offsetHeight - my.space.childNodes[0].offsetHeight - my.space.childNodes[2].offsetHeight) + scrollBar.UNIT;
			if (my.bar.offsetHeight > my.space.offsetHeight) 
				my.bar.style.height = my.space.style.height;
			my.bar.childNodes[1].style.height = Math.abs(my.bar.offsetHeight - my.bar.childNodes[0].offsetHeight - my.bar.childNodes[2].offsetHeight) + scrollBar.UNIT;
		}
	}
	/*
	@description Calculates the Ratio between available Space and maxValue, so that the Values which are returned for the Events hold the correct Value rather than the Pixel Pos of the Bar and the same vice verca
		that the Value will be correctly transformed to a Pixel amount. Positions as well the Scrollbar to the Depending values
	*/
	function configure() {
		var areaSize = 0;
		if (horz) {
			areaSize = bar.parentNode.offsetWidth - bar.offsetWidth - (2 * margin);
		} else {
			areaSize = bar.parentNode.offsetHeight - bar.offsetHeight - (2 * margin);
		}
		areaRatio = areaSize / maxValue;
		areaRatio = toFloat(areaRatio);
//		if (isNaN(parseInt(areaRatio))) 
	//		areaRatio = 0;
		if (horz) {
			bar.style.left = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
		} else {
			bar.style.top = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
		}
	}
	/*
	@description calculates how big the Bar has to be to be able to display all steps and how big the JumpStep has to be so that Stepping happens correctly
	*/
	function prepareStepper() {
		if (horz) {
			bar.style.width = bar.parentNode.offsetWidth / steps + scrollBar.UNIT;
			jumpStep = bar.offsetWidth;
		} else {
			bar.style.height = bar.parentNode.offsetHeight / steps + scrollBar.UNIT;
			jumpStep = bar.offsetHeight;
		}
	}
}
