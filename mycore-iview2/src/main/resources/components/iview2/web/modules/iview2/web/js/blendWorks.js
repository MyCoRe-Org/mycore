//TODO funzt noch nicht alles f�r Effects disabled
function blendWorks() {
	var effects = true;
	var fadeList = new Array();
	this.useEffects = useEffects;
	this.hasEffects = hasEffects;
	this.setOpacity = setOpacity;
	this.getOpacity = getOpacity;
	this.fadeOut = fadeOut;
	this.fadeIn = fadeIn;
	this.slide = slide;
	this.blend = blend;

	/*
	@description sets if (Fade) effects will be applied or not
	@param value boolean which defines if fade effects are allowed or not
	*/
	function useEffects(value) {
		effects = (value == true)? true: false;
	}

	/*
	@description returns if fade effects are enabled or not
	@return boolean which tells if effects are on or off
	*/
	function hasEffects() {
		return effects;
	}

	/*
	@description moves a given List of Objects within a given Number of Steps around the vector
	@param objList List with Objects and/or IDs of Objects which will be moved
	@param values, 0 (left+top); 1(right+top); 2(left+bottom);3(right+bottom)
	*/
	function slide(objList, vector, steps, time) {
		var values = toInt(arguments[4]);
		var two = Math.floor(values/2);
		var one = values - 2*two;
		var doAfter = null;
		var fadings = null;
		if (typeof arguments[5] != "undefined") {
			fadings = arguments[5];
		}
		if (typeof arguments[6] != "undefined") {
					eval (arguments[6]);
		}
		if (typeof arguments[7] != "undefined") {
			doAfter = arguments[7];
		}
		if (typeof (objList) == "string") {
			var totalX = toFloat(vector[2]) - toFloat(vector[0]);
			var totalY = toFloat(vector[3]) - toFloat(vector[1]);
			if (effects) {
				var stepX = totalX / steps;
				var stepY = totalY / steps;
				var interval = null;

				if (one==0) {
					if ($(objList).style.left == "")
						$(objList).style.left = toFloat(vector[0]) + "px";
				} else {
					if ($(objList).style.right == "")
						$(objList).style.right = toFloat(vector[0]) + "px";
				}
				if (two==0) {
					if ($(objList).style.top == "")
						$(objList).style.top = toFloat(vector[1]) + "px";
				} else {
					if ($(objList).style.bottom == "")
						$(objList).style.bottom = toFloat(vector[1]) + "px";
				}
				
				var slider = function() {
					if (one==0) {
						$(objList).style.left = toFloat($(objList).style.left) + stepX + "px";
					} else {
						$(objList).style.right = toFloat($(objList).style.right) + stepX + "px";
					}
					if (two==0) {
						$(objList).style.top = toFloat($(objList).style.top) + stepY + "px";
					} else {
						$(objList).style.bottom = toFloat($(objList).style.bottom) + stepY + "px";
					}
					for (var i = 0; i < fadings.length; i++) {
						setOpacity($(fadings[i]), getOpacity($(fadings[i])) + fadeList[i]);
					}
					steps--;
					if (steps == 0) {
						clearInterval(interval);
						eval(doAfter);
						fadeList = new Array();
					}
				}
				makeFadeList(fadings, steps);
				interval = setInterval(function() { slider();}, time);
			} else {
				if (one==0) {
					$(objList).style.left = toFloat(vector[2]) + "px";
				} else {
					$(objList).style.right = toFloat(vector[2]) + "px";
				}
				if (two==0) {
					$(objList).style.top = toFloat(vector[3]) + "px";
				} else {
					$(objList).style.bottom = toFloat(vector[3]) + "px";
				}
				if (fadings[0].substring(fadings[0].indexOf(":")+1, fadings[0].length) == "in") {
					setOpacity($(objList), 100);
					//$(objList).style.display = "block";
					$(objList).style.visibility = "visible";
				} else {
					setOpacity($(objList), 0);
//					$(objList).style.display = "none";
					$(objList).style.visibility = "hidden";
				}
			}
		}
	}

	/*
	@description Calculates for every Item how much it's faded per step and in which direction it needs to be faded
	@param fadings array with names(ids) of objects which shall be faded and in which direction
	@param steps the number of steps which shall happen between opacity = 0|1 is reached
	*/
	function makeFadeList(fadings, steps) {
		if (fadeList.length == 0) {
			for (var i = 0; i < fadings.length; i++) {
				var opacity = getOpacity($(fadings[i].substring(0,fadings[i].indexOf(":"))));
				if (parseInt(opacity) == 0) {
					opacity = 100;
				}
				fadeList.push(opacity / steps);
				if (fadings[i].indexOf(":out") != -1) {
					fadeList[i] = - fadeList[i];
				}
				fadings[i] = fadings[i].substring(0, fadings[i].indexOf(":"));
			}
		}
	}

	/*
	@description depending on the fact that the ObjList shall be blended in or out it will blend each object one step more in or out
	@param inFade boolean defines if the ObjectList will be blended in or out
	@param step the factor it will be blended in or out, needs to be negative if fade out will be done
	@param objList Object or List of Objects which shall be faded out/in a bit more
	*/
	function fade(inFade, step, objList) {
		var allDone = true;
		var opacity = null;
		if (typeof (objList) == "string" || objList.nodeType) {
			opacity = getOpacity($(objList));
			if ((opacity + step > 0 && inFade == false) || (opacity + step < 100 && inFade == true)) {
				allDone = false;
			}
			setOpacity($(objList), opacity + step);
		} else {
			for (var i = 0; i < objList.length; i++) {
				opacity = getOpacity($(objList[i]));
				if ((opacity + step > 0 && inFade == false) || (opacity + step < 100 && inFade == true)) {
					allDone = false;
				}
				setOpacity($(objList[i]), opacity + step);
			}
		}
		return allDone;
	}
	
	/*
	@description fades out a list of Objects or Ids of Objects, if effects are disabled the elements wll simply be hided and all supplied code be executed right in time
	@param objList Lit of Objects and/or IDs of Objects which shall be faded out, can although be just a single Object or ID
	@param step Number which tells the difference between two opacity values for the Objects
	@param time time between two fading events
	@param arguments[3] Additional parameter which will be executed after all Objects where faded out completely
	@param arguments[4] Additional parameter which will be executed just before any fading happens
	*/
	function fadeOut(objList, step, time) {
		if (typeof arguments[4] != "undefined") {
			eval(arguments[4]);
		}
		if (effects) {
			step = toFloat(step);
			//Fading all Elements a bit more out.
			if (!fade(false, - step, objList)) {
				if (typeof arguments[3] != "undefined") {
					var doAfter = arguments[3];
					setTimeout(function() { fadeOut(objList, step, time, doAfter);}, time);
				} else {
					setTimeout(function() { fadeOut(objList, step, time);}, time);
				}
			} else {
				if (typeof arguments[3] != "undefined") {
					eval(arguments[3]);
				}
			}
		} else {
			//If Fade Effects are disabled just set Opacity to zero and execute the attached do After code
			if (typeof (objList) == "string" || objList.nodeType) {
				setOpacity($(objList), 0);
			} else {
				for (var i = 0; i < objList.length; i++) {
					setOpacity($(objList[i]), 0);
				}
			}
			if (typeof arguments[3] != "undefined") {
				eval(arguments[3]);
			}
		}
	}

	/*
	@description Fades in a list of Objects, if effects are turned off it will simply set the opacity to max and executes any after Code
	@param objList List of Objects and/or IDs of Objects which shall be faded in, can be although just one Object/ID
	@param step Number which defines the difference between two opacity values which will be displayed
	@param time Number which defines the time between two fading steps
	@param arguments[3] Additional parameter which will be evaluted after all objects are faded fully
	@param arguments[4] Additional parameter which will be executed before the fading starts
	*/
	function fadeIn(objList, step, time) {
	//DoBefore
		if (typeof arguments[4] != "undefined") {
			eval(arguments[4]);
		}
		if (effects) {
			step = toFloat(step);
			if (!fade(true, step, objList)) {

				if (typeof arguments[3] != "undefined") {
					var doAfter = arguments[3];
					setTimeout(function() { fadeIn(objList, step, time, doAfter);}, time);
				} else {
					setTimeout(function() { fadeIn(objList, step, time);}, time);
				}
			} else {
				if (typeof arguments[3] != "undefined") {
					eval(arguments[3]);
				}
			}
		} else {
			if (typeof (objList) == "string" || objList.nodeType) {
				setOpacity($(objList), 100);
			} else {
				for (var i = 0; i < objList.length; i++) {
					setOpacity($(objList[i]), 100);
				}
			}
			if (typeof arguments[3] != "undefined") {
				eval(arguments[3]);
			}
		}
	}

	/*
	@description fades Objects out, does some addition stuff between and then fades in some other objects, if effects are disabled it will simply hide the given Objects,	execute the given between code and then show the given Objects
	@param objListOut List with Objects or Names of Objects which shall be faded out, cab although just be some single String/Object
	@param objListIn List with Objects or Names of Objects which shall be faded in, can although just be some single String/Object
	@param steps Number of difference between two opacity Values, which will be set
	@param time Time between a fade step.
	@param arguments[4] additional one, is threated as DoAfter Code.
	@param arguments[5] additional one, is threated as DoBetween Code
	@param arguments[6] additional one, is threated as DoBefore Code
	*/
	function blend(objListOut, objListIn, step, time) {
		if (typeof arguments[6] != "undefined") {
			eval(arguments[6]);
		}
		var doBetween = "";
		if (typeof arguments[5] != "undefined") {
			doBetween = arguments[5];
			if (doBetween.charAt(doBetween.length-1) != ";") {
				doBetween = doBetween + ";";
			}
		}
		var doAfter = "";
		if (typeof arguments[4] != "undefined") {
			doAfter = ", \""+arguments[4]+"\"";
		}
		fadeIn(objListIn, step, time, doBetween+" fadeOut('"+objListOut+"', "+step+", "+time+doAfter+");");
	}

	/*
	@description sets the Opacity for a given Object to the supplied value
	@param obj Object which will get the supplied Opacity
	@param opacity Number which tells the new opacity for the Object
	*/
	function setOpacity(obj, opacity) {
		if (typeof obj != "object") {
			throw ("Requires a Object, given was:" + typeof obj);
		}
		opacity = (opacity == 100)?99.999:opacity;
		opacity = (opacity < 0)? 0:opacity;
		// IE/Win
		obj.style.filter = "alpha(opacity="+opacity+")";

		// Safari<1.2, Konqueror
		obj.style.KHTMLOpacity = opacity/100;

		// Older Mozilla and Firefox
		obj.style.MozOpacity = opacity/100;

		// Safari 1.2, newer Firefox and Mozilla, CSS3
		obj.style.opacity = opacity/100;
	}

	/*
	@description return the Opacity of given Object
	@param obj Object which Opacity shall be retrieved
	@return float of the opacity of obj
	//TODO: Korrektes Auslesen f�r IE, pr�fen
	*/
	function getOpacity(obj) {
		if (typeof obj != "object") {
			throw ("Requires a Object, given was:" + typeof obj);
		}
		//var temp_style=document.defaultView.getComputedStyle(obj,null);
		var temp_style=null;
		if (obj.currentStyle) {
			temp_style=obj.currentStyle;
		} else if (window.getComputedStyle) {
			temp_style=window.getComputedStyle(obj, null);
		}
		if (!isNaN(temp_style.opacity))	{
			opacityVal = temp_style.opacity*100;
		} else if (!isNaN(temp_style.MozOpacity)) {
			opacityVal = temp_style.MozOpacity*100;
		} else {
			opacityVal = getStyle(obj, "opacity");
		}
		return parseFloat(opacityVal);
	}

}
