/**
 * @class
 * @name ToolbarView
 * @description view of a toolbar to present the model informations
 * @param {String} id identifies the current toolbar view
 * @param {Object} events to trigger defined actions, while managing contained elements
 * @param {Object} parent defines the parent node of the toolbar view
 * @param {Object} toolbar represents the main node of the toolbar view
 */
var ToolbarView = function (id, parent) {
    this.id = id;
    this.events = new iview.Event(this);
    
    var newToolbar = jQuery('<div>').addClass(id).addClass('toolbar ui-widget-header ui-corner-all ui-helper-clearfix').appendTo(parent);
  	this.toolbar = newToolbar[0];
};

ToolbarView.prototype = {
	/**
	 * @function
	 * @name destroy
	 * @memberOf ToolbarView#
	 * @description remove the complete toolbar view from the DOM
	 */
    destroy : function () {
		jQuery(this.toolbar).remove();
    },
    
    /**
     * @function
	 * @name addButtonset
	 * @memberOf ToolbarView#
	 * @description add a new buttonset to the toolbar view,
	 *  either on the next position or on a special position defined by args.index
	 * @param {String} args.elementName defines the name of the buttonset
	 * @param {integer} args.index defines the special position between the other predefined elements where the new buttonset should be added
	 * @return {Object} returns the parent tag of the added buttonset
	 */
    addButtonset : function (args) {
    	var newButtonset = $('<span>').buttonset().addClass(args.elementName);
    	if (!isNaN(args.index) && args.index != this.toolbar.childNodes.length) {
    		newButtonset.insertBefore(this.toolbar.childNodes[args.index]);
    	} else {
    		jQuery(this.toolbar).append(newButtonset);
    	}
    	
		return newButtonset;
    },

    /**
     * @function
	 * @name addDivider
	 * @memberOf ToolbarView#
	 * @description add a new divider to the toolbar view,
	 *  a divider is only a vertical line for a better visual seperation of two buttonsets
	 * @param {String} args.elementName defines the name of the divider
	 * @param {integer} args.index defines the special position between the other predefined elements where the new divider should be at
	 * @return {Object} returns the parent tag of the added divider
	 */        
    addDivider : function (args) {
    	var newDivider = $('<span>').addClass(args.elementName).addClass('ui-divider')/*.addClass('ui-button-size-normal')*/;
		if (!isNaN(args.index) && args.index != this.toolbar.childNodes.length) {
    		newDivider.insertBefore(this.toolbar.childNodes[args.index]);
    	} else {
    		jQuery(this.toolbar).append(newDivider);
    	}	
		return newDivider;
    },

    /**
     * @function
	 * @name getToolbarElement
	 * @memberOf ToolbarView#
	 * @description returns the parent tag of a direct toolbar view element (e.g. buttonset or divider)
	 *  (buttons aren't direct elements, they are within a special buttonset
	 * @param {String} elementName defines the name of a direct toolbar view element
	 * @return {Object} returns a single view element
	 */        
    getToolbarElement : function (elementName) {
    	for (var i = 0; i < this.toolbar.childNodes.length; i++) {
    		if (jQuery(this.toolbar.childNodes[i]).hasClass(elementName)) return this.toolbar.childNodes[i];
    	}
    },
    
    /**
     * @function
	 * @name removeToolbarElement
	 * @memberOf ToolbarView#
	 * @description removes a direct toolbar view element (e.g. buttonset or divider)
	 *  (buttons aren't direct elements, they are within a special buttonset
	 * @param {String} args.elementName defines the name of a direct toolbar view element
	 */  
    removeToolbarElement : function (args) {
    	this.toolbar.removeChild(this.getToolbarElement(args.elementName));
    },
     
    /**
     * @function
	 * @name getButtonUi
	 * @memberOf ToolbarView#
	 * @description returns the button ui properties of a single jQuery Ui Button
	 * @param {Object} args.button defines a special button
	 * @return {AssoArray} returns the button ui properties <br>
	 * 	{ <br>
	 *   icons: defines the button icons (primary, second), <br>
	 *   text: defines if the label will shown within the button <br>
	 *   label: defines the label and if enabled the text of the button, <br>
	 *   disabled: defines if the button is enables (false) or disabled (true) <br>
	 *  }
	 */
    getButtonUi : function (args) {
    	var button = this._getButton(args.button);
    	return ({
    		'icons' : button.button( "option", "icons" ),
    		'text' : button.button( "option", "text" ),
    		'label' : button.button( "option", "label" ),
    		'disabled' : button.button( "option", "disabled" )
    	});
    },
    
    /**
     * @function
	 * @name setButtonUi
	 * @memberOf ToolbarView#
	 * @description sets the given button ui properties to a single jQuery Ui Button
	 * @param {Object} args.button defines a special button
	 * @param {AssoArray} args.icons defines the button icons (primary, second)
	 * @param {String} args.text defines if the label will shown within the button
	 * @param {boolean} args.label defines the label and if enabled the text of the button,
	 * @param {boolean} args.disabled defines if the button is enables (false) or disabled (true)
	 */
    setButtonUi : function (args) {
    	var button = this._getButton(args.button);
    	if (args.icons) button.button( "option", "icons", args.icons );
    	if (args.text != undefined) button.button( "option", "text", args.text );
    	if (args.label) button.button( "option", "label", args.label );
    	if (args.disabled != undefined) button.button( "option", "disabled", args.disabled );
    },

    /** 
     * @function
	 * @name addButton
	 * @memberOf ToolbarView#   
	 * @description adds a given button with its given properties to the toolbar view
	 * @param {String} args.parentName defines the buttonset name in which the button should insert
	 * @param {String} args.elementName defines the name of the button
	 * @param {String} args.title defines the title text of the button,
	 * @param {integer} args.index defines a special position between the other buttons where the current button should be inserted
	 */
    addButton : function (args) {

    	var myButtonset = this.getToolbarElement(args.parentName);
    	var myView = this;

		var newButton = null;

		var onClick = function(event) {
				if (jQuery(this).attr("aria-disabled") == "true") return false;
				getEvent(event).cancelBubble = true;
    			myView.events.notify({'type' : "press", 'elementName' : args.elementName, 'parentName' : args.parentName, 'view' : newButton});
    			return false;
    		};

		if (args.subtype.type == "buttonDefault") {
    		newButton = $('<button>').attr('title', args.title).addClass(args.elementName);
    		newButton.click(onClick);
		} else if (args.subtype.type == "buttonCheck") {
			newButton = $('<input type="checkbox" id="'+args.elementName+'" class='+args.elementName+' /><label for='+args.elementName+' class='+args.elementName+"Label"+'>'+args.title+'</label>')
				.attr('title', args.title);
			newButton[1].onclick = onClick;
		}

    	if (!isNaN(args.index) && args.index != myButtonset.childNodes.length) {
    		newButton.insertBefore(myButtonset.childNodes[args.index]);
    	} else {
    		jQuery(myButtonset).append(newButton);
    	}

    	// must appear after append (for checkbox-function)
    	newButton = $("."+this.id+" ."+args.parentName+" ."+args.elementName).button(args.ui);

    	if (!args.active) {
    		newButton.button( "option", "disabled", true )
    	}
    	
    	this._checkButtonStyle({'buttonset' : myButtonset, 'buttonIndex' :  (!isNaN(args.index))? args.index : myButtonset.childNodes.length - 1, 'reason' : "add"});
    	this.events.notify({'type' : "new", 'elementName' : args.elementName, 'parentName' : args.parentName, 'view' : newButton});
    },
    
    /** 
     * @function
	 * @name removeButton
	 * @memberOf ToolbarView#  
	 * @description removes a given button from its buttonset
	 * @param {String} args.parentName defines the buttonset name which contains the button
	 * @param {String} args.elementName defines the name of the button
	 */
    removeButton : function (args) {
    	var myButtonset = this.getToolbarElement(args.parentName);
    	for (var i = 0; i < myButtonset.childNodes.length; i++) {
			if (myButtonset.childNodes[i].className.indexOf(args.elementName) > -1) {
				this.checkButtonStyle({'buttonset' : myButtonset, 'buttonIndex' : i, 'reason' : "del"});
				myButtonset.removeChild(myButtonset.childNodes[i]);
			}
		}
    },
    
    /**
     * @function
     * @name _getButton
     * @description returns a button view instance
     * @param {Object}|{String} either a view instance or a jQuery selector
     */
    _getButton : function (button) {
    	if (typeof button === "string"){
    		return jQuery(this.toolbar).find(button);
    	}
    	return button;
    },
    
    /** 
     * @function
	 * @name checkButtonStyle
	 * @memberOf ToolbarView#
	 * @description checks a single button and its neighbours to correct their corners within their buttonset
	 * @param {Object} args.buttonset defines the buttonset which contains the button(s)
	 * @param {integer} args.buttonIndex defines the index of the current changed button
	 * @param {String} args.reason defines the reason of changing (add, del)
	 */
    // checks for round button edges
    _checkButtonStyle : function (args) {
    	var buttonset = args.buttonset;
    	var buttonIndex = args.buttonIndex;
    	if (args.reason == "add") {
    		if (buttonset.childNodes.length == 2) {
    			if (buttonIndex == 0) {
    				$(buttonset.childNodes[buttonIndex + 1]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex + 1]).addClass('ui-corner-right');
    				$(buttonset.childNodes[buttonIndex]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex]).addClass('ui-corner-left');
    			} else {
    				$(buttonset.childNodes[buttonIndex - 1]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex - 1]).addClass('ui-corner-left');
    				$(buttonset.childNodes[buttonIndex]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex]).addClass('ui-corner-right');
    			}	
    		} else if (buttonset.childNodes.length > 2) {
    			if (buttonIndex == 0) {
    				$(buttonset.childNodes[buttonIndex + 1]).removeClass('ui-corner-left');
    				$(buttonset.childNodes[buttonIndex + 1]).addClass('ui-corner-none');
    				$(buttonset.childNodes[buttonIndex]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex]).addClass('ui-corner-left');
    			} else if (buttonIndex == buttonset.childNodes.length - 1) {
    				$(buttonset.childNodes[buttonIndex - 1]).removeClass('ui-corner-right');
    				$(buttonset.childNodes[buttonIndex - 1]).addClass('ui-corner-none');
    				$(buttonset.childNodes[buttonIndex]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex]).addClass('ui-corner-right');
    			} else {
    				$(buttonset.childNodes[buttonIndex]).removeClass('ui-corner-all');
    				$(buttonset.childNodes[buttonIndex]).addClass('ui-corner-none');
    			}
    		}
		} else if (args.reason == "del") {
			if ($(buttonset.childNodes[buttonIndex]).hasClass('ui-corner-left')) {
				if (buttonset.childNodes.length == 2) {
					$(buttonset.childNodes[buttonIndex + 1]).removeClass('ui-corner-right');
			    	$(buttonset.childNodes[buttonIndex + 1]).addClass('ui-corner-all');
			    } else {
			    	$(buttonset.childNodes[buttonIndex + 1]).removeClass('ui-corner-none');
			    	$(buttonset.childNodes[buttonIndex + 1]).addClass('ui-corner-left');
			    }
			} else if ($(buttonset.childNodes[buttonIndex]).hasClass('ui-corner-right')) {
				if (buttonset.childNodes.length == 2) {
					$(buttonset.childNodes[buttonIndex - 1]).removeClass('ui-corner-left');
			    	$(buttonset.childNodes[buttonIndex - 1]).addClass('ui-corner-all');
			    } else {
					$(buttonset.childNodes[buttonIndex - 1]).removeClass('ui-corner-none');
		    		$(buttonset.childNodes[buttonIndex - 1]).addClass('ui-corner-right');
			    }
			}
		}
    }
};
