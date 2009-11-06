/** 
 * A static utility class for accessing browser-neutral event properties and
 * methods.
 */
 
// holds each registred Event, so now it's possible to remove them
var ManageEvents_Events = [];

function ManageEvents() {
	throw 'RuntimeException: ManageEvents is a static utility class and may not be instantiated';
}

ManageEvents.addEventListener = function(target, type, callback, captures) {
	// Eine Wrapper Funktion, die unseren Event-Handler aufruft. Und zwar erstens im richtigen Scope,
	// n�mlich den des Elements, und zweitens mit unserem vereinheitlichten Event-Objekt.
	var wrapper = function(ev) {
		callback.call(target, ev);
	};
		
	// True, wenn das Event erfolgreich registriert wurde.
	var result = false;

	switch (type.toLowerCase()) {//Browser behave on some kinds of events totally different therefore its needed to find it out and take action correctly
		case "mousescroll":
			if (isBrowser(["IE", "Opera", "Safari"])) {
				//window.onmousewheel = target.onmousewheel = callback;
				if (window.attachEvent) {
					target.attachEvent("onmousewheel", wrapper, captures);
				} else {
					target.addEventListener("mousewheel", wrapper, captures);
				}
			} else {
				target.addEventListener("DOMMouseScroll", wrapper, captures);
			}
			result = true;
		break;
		default://all Events which are just different in the function name to apply
			if (target.addEventListener) {
				// W3C standard
				target.addEventListener(type, wrapper, false);
			} else if (target.attachEvent) {
				// newer IE
				target.attachEvent("on" + (type == "DOMMouseScroll" ? "mousewheel" : type), wrapper);
			} else {
				// IE 5 Mac and some others
				target['on'+type] = callback;
			}
			result = true;
		break;
	}
	
	// Falls das Event erfolgreicht registriert wurde, m�ssen wir es noch hier registrieren,
	// um das Event auch wieder an Hand seiner Original Funktion entfernen zu k�nnen.
	if (result)	{
		ManageEvents_Events[ManageEvents_Events.length] = [target, type, callback, wrapper];
	}
	return result;
}

ManageEvents.removeEventListener = function(target, type, callback, captures) {
	// check if Event was registred in past
	var selection = ManageEvents.findEvents(target, type, callback);
	if (selection) {
		index = 0;
		while (index < selection.length) {
			var item = ManageEvents_Events[selection[index]];
			// item[3] ist der zugeh�rige Wrapper, den wir zum entfernen ben�tigen.
			switch (item[1].toLowerCase()) {//Browser behave on some kinds of events totally different therefore its needed to find it out and take action correctly
				case "mousescroll":
					if (isBrowser(["IE", "Opera", "Safari"])) {
						if (isBrowser("IE")) {
							item[0].detachEvent("on" + item[1], item[3]);
						} else {
							item[0].removeEventListener("mousewheel", item[3], false);
						}
					} else {
						item[0].removeEventListener("DOMMouseScroll",item[3], false);
					}
				break;
				default://all Events which are just different in the function name to apply
					if (item[0].removeEventListener) {
						// W3C standard
						item[0].removeEventListener(item[1], item[3], false);
					} else if (item[0].attachEvent) {
						// newer IE
						item[0].detachEvent("on" + item[1], item[3]);
					} else {
						// IE 5 Mac and some others
						// TODO: needs to be tested
						item[0]['on'+item[1]] = "";
					}
					result = true;
				break;
			}
			
			// Das event wird gel�scht.
			ManageEvents_Events.splice(selection[index], 1);
			
			index++;
		}
	}
}

ManageEvents.findEvents = function(target, type, callback) {
	// to allow differences in function call
	if (type) type = type.toLowerCase();
	
	var i = ManageEvents_Events.length;
	if (!i) return;
	
	var selection = [];
	
	// Durchlaufe alle registrierten Events
	while (i >= 1) {
		 var item = ManageEvents_Events[i - 1];
		
		// if some argument is left out
		if (!target) {
			var usedTarget = target;
		} else {
			var usedTarget = item[0];
		}
		if (!type) {
			var usedType = type;
		} else {
			// because browser-differences
			var usedType = item[1].toLowerCase();
		}
		if (!callback) {
			var usedCallback = callback;
		} else {
			var usedCallback = item[2];
		}
		
		// Wenn gefunden, dann gib Eintragsnummer zur�ck
		if (ManageEvents.objEquals(target, usedTarget) && type === usedType && callback === usedCallback)	{
			selection[selection.length] = i-1;
		}
		i--;
	}
	
	if (selection.length > 0) {
		return selection;
	} else {
		// falls nicht gefunden
		return false;
	}
}

ManageEvents.objEquals = function(obj1, obj2) {
	if (obj1 == obj2) {
		return true;
	} else if (!obj1.attributes || !obj2.attributes) {
		return false;
	} else if (obj1.attributes.length != obj2.attributes.length) {
		return false;
	} else {
		for (i = 0; i < obj1.attributes.length; i++) {
		 	if (!(obj1.attributes[i].nodeValue == obj2.attributes[i].nodeValue)) {
		 		return false;
		 	}
		 }
		 return true;
	}
	return false;
}

/*
ManageEvents.findEvent = function(target, type, callback) {
	// to allow differences in function call
	type = type.toLowerCase();
	
	var i = ManageEvents_Events.length;
	if (!i) return;
	
	// Durchlaufe alle registrierten Events
	while (i >= 1) {
		 var item = ManageEvents_Events[i - 1];
		
		// Wenn gefunden, dann gib Eintragsnummer zur�ck
		if (ManageEvents.objEquals(target, usedTarget) && type === usedType && callback === usedCallback)	{
			return i - 1;
		}
		i--;
	}
	
	// falls nicht gefunden
	return false;
}*/
/*
ManageEvents.findTarget = function(e, allowTextNodes) {
    var target;
	if (window.event) {
		target = window.event.srcElement;
	} else if (e) {
		target = e.target;
	} else { 
		// we can't find it, just use window
		target = window;
	}

	if (!allowTextNodes && target.nodeType == 3) {
		target = target.parentNode;
	}

	return target;
}*/

/**
 * @return {x, y}
 */
ManageEvents.getMousePosition = function(e) {
	var posx = 0;
	var posy = 0;
	if (!e)
	{
		e = window.event;
	}

	if (e.pageX || e.pageY)
	{
		posx = e.pageX;
		posy = e.pageY;
	}
	else if (e.clientX || e.clientY)
	{
		posx = e.clientX + document.body.scrollLeft;
		posy = e.clientY + document.body.scrollTop;
	}

	return { x : posx, y : posy };
}

function TextSelectionEvent(selectedText, mousePosition) {
	this.selectedText = selectedText;
	this.x = mousePosition.x;
	this.y = mousePosition.y;
}
