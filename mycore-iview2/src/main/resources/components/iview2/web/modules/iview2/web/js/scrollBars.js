//TODO einbauen das Scrollbar minimale Größe hat, damit man sie noch benutzen kann
//TODO einbauen das Stepper wie normale ScrollBar sich bewegt und dann einrastet beim loslassen
function scrollBar(newId) {
//Constants
	scrollBar.STARTCL = "start";
	scrollBar.ENDCL = "end";
	scrollBar.EMPTYCL = "empty";
	scrollBar.UNIT = "px";
	scrollBar.LIST_MOVE = 1;
	scrollBar.LIST_STEP = 2;
	scrollBar.JUMPSTEP = 30;
	scrollBar.STEP =3;
	scrollBar.INTERVAL = 100;

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
	this.my = null;//holds the Elements itself in a named kind;
//Event registration features;
	this.mouseUp = null;
	this.mouseMove = null;
	this.scroll = null;

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
				newStep = Math.ceil((parseInt(bar.style.left) /*- offsetLeft*/) / bar.offsetWidth);
			} else {
				newStep = Math.ceil((parseInt(bar.style.top) /*- offsetTop*/) / bar.offsetHeight);
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
				move = Math.round(move / areaRatio);
				curValue -= move;
				if (horz) {
					if (curValue <= 0) {
						move = Math.abs(move + curValue);
						curValue = 0;
						bar.style.left = margin + scrollBar.UNIT;
					} else {
						bar.style.left= Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': -move, 'y': 0});
				} else {
					if (curValue <= 0) {
						move = Math.abs(move + curValue);
						curValue = 0;
						bar.style.top = margin + scrollBar.UNIT;
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
				move = Math.round(move / areaRatio);
				curValue += move;
				if (!horz) {
					if (curValue > maxValue) {
						move = curValue - maxValue;
						curValue = maxValue;
						bar.style.top = bar.parentNode.offsetHeight - bar.offsetHeight - margin + scrollBar.UNIT;
					} else {
						bar.style.top = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': 0, 'y': move});
				} else {
					if (curValue > maxValue) {
						move = curValue - maxValue;
						curValue = maxValue;
						bar.style.left = bar.parentNode.offsetWidth - bar.offsetWidth - margin + scrollBar.UNIT;
					} else {
						bar.style.left = Math.round(curValue * areaRatio) + margin + scrollBar.UNIT;
					}
					notifyListenerMove({'x': move, 'y': 0});
				}
			} else {
				var value = 0;
				if (!horz) {
					value = parseInt(bar.style.top) + bar.offsetHeight /*- offsetTop*/;
					if (value + bar.offsetHeight >= bar.parentNode.offsetHeight)
						value = bar.parentNode.offsetHeight - bar.offsetHeight;
					bar.style.top = value /*+ offsetTop*/ + scrollBar.UNIT;
				} else {
					value = parseInt(bar.style.left) + bar.offsetWidth /*- offsetTop*/;
					if (value + bar.offsetWidth >= bar.parentNode.offsetWidth)
						value = bar.parentNode.offsetWidth - bar.offsetWidth;
					bar.style.left = value /*+ offsetTop*/ + scrollBar.UNIT;
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
			
			if (before) {
				if (((parseInt(bar.style.left) - jumpStep) <= layerX) && horz) {
					clearInterval(interval);
					moveUp(parseInt(bar.style.left)  - layerX);
				} else if ((parseInt(bar.style.top) - jumpStep <= layerY) && !horz) {
					clearInterval(interval);
					moveUp(parseInt(bar.style.top) - layerY);
				} else {
					moveUp(jumpStep);
				}
			} else {
				if (((parseInt(bar.style.left) + bar.offsetWidth + jumpStep) >= layerX) && horz) {
					clearInterval(interval);
					moveDown(layerX - parseInt(bar.style.left) - bar.offsetWidth);
				} else if (((parseInt(bar.style.top) + bar.offsetHeight + jumpStep) >= layerY) && !horz) {
					clearInterval(interval);
					moveDown(layerY - parseInt(bar.style.top) - bar.offsetHeight);
				} else {
					moveDown(jumpStep);
				}
			}
		}
		//Event Listener
		/*
		@description realizes while mouse is pressed that the bar moves in a given Timeframe downwards
		@param e Event which holds the current Event
		*/
		function upLeftMouseDown(e) {
			moveUp();
			interval = setInterval(function() { moveUp();}, timeInt);
			return false;
		}
		/*
		@description stops the continous Movement after the mouse is released
		@param e event which holds the current MouseEvent
		*/
		function upLeftMouseUp(e) {
			mouseDown = false;
			clearInterval(interval);
		}
		/*
		@description Starts the Drag&Drop of the Button by saving the initial Position of the Bar related to the mouse Position
		@param e event which holds the Event happening Position
		*/
		function divMouseDown(e) {
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
			e = getEvent(e);
			mouseDown = false;
			outOfBar = false;
			e.cancelBubble = true;
			clearInterval(interval);
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
			e = getEvent(e);
			mouseDown = true;
			outOfBar = true;
			var layerX = (e.layerX)? e.layerX:e.offsetX;
			var layerY = (e.layerY)? e.layerY:e.offsetY;
			if ((((parseInt(bar.style.top) + bar.offsetHeight) < layerY) && !horz)
				|| (((parseInt(bar.style.left) + bar.offsetWidth) < layerX) && horz)) {
				stopUnderMouse(e, false);
				var ev = {'layerX': layerX, 'layerY': layerY};
				interval = setInterval(function() { stopUnderMouse(ev, false);}, timeInt);
			} else if (((layerY < parseInt(bar.style.top)) && !horz) 
				|| ((layerX < parseInt(bar.style.left)) && horz)) {
				stopUnderMouse(e, true);
				var ev = {'layerX': layerX, 'layerY': layerY};
				interval = setInterval(function() { stopUnderMouse(ev, true);}, timeInt);
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
			clearInterval(interval);
		}

		/*
		@description if a MouseScroll is done it calls once a Bar moveup or movedown and cancels any further bubbling
		@param e event which holds the mousedelta which allows it to decide if scrolling down or up happened
		*/
		function dbgMouseWheel(e) {
			e = getEvent(e);
			if (returnDelta(e, true) > 0) {
				moveUp();
			} else {
				moveDown();
			}
			e.cancelBubble = true;
		}
		/*
		@description starts by a mousedown the continous move down/right of the Bar as long as the Button is pressed
		@param e event which happened
		*/
		function downRightMouseDown(e) {
			moveDown();
			interval = setInterval(function() { moveDown();}, timeInt);
			return false;
		}
		/*
		@description stops the move down/right of the Bar as soon as the Button is released
		@param e event which happened
		*/
		function downRightMouseUp(e) {
			clearInterval(interval);
		}
	//Start Init Function creation of the Div Structure and registering all needed Events so that the Scrollbar is fully functional
		horz = horizontal;
		var complete = document.createElement("div");
		complete.id = id;
		complete.className = identer+((horz)? "H":"V");//new
		if (parent != "")
			$(parent).appendChild(complete);

	//Start Up/Left
		var buttonUL = document.createElement("div");
		buttonUL.onmousedown = function() {return false;};
		buttonUL.className = scrollBar.STARTCL;// + identer;
		complete.appendChild(buttonUL);
		EventUtils.addEventListener(buttonUL, "mousedown", upLeftMouseDown, false);
		EventUtils.addEventListener(buttonUL, "mouseup", upLeftMouseUp, false);
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
		EventUtils.addEventListener(wholeSpace, "mousescroll", dbgMouseWheel, false);
		EventUtils.addEventListener(wholeSpace, "mousedown", dbgMouseDown, false);
		EventUtils.addEventListener(wholeSpace, "mouseup", dbgMouseUp, false);
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
		EventUtils.addEventListener(barScroll, "mousedown", divMouseDown, false);
		EventUtils.addEventListener(barScroll, "mouseup", divMouseUp, false);
		EventUtils.addEventListener(barScroll, "mousemove", divMouseMove, false);
		EventUtils.addEventListener(barScroll, "mouseover", divMouseOver, false);
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
		EventUtils.addEventListener(buttonDR, "mousedown", downRightMouseDown, false);
		EventUtils.addEventListener(buttonDR, "mouseup", downRightMouseUp, false);

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
		$(name).appendChild(this.my.self);
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
