/**
 * @class
 * @constructor
 * @name		ToolbarView
 * @description view of a toolbar to present the model informations
 * @param		{String} id identifies the current toolbar view
 * @param		{Object} events to trigger defined actions, while managing contained elements
 * @param		{Object} parent the parent node of the toolbar view
 * @param		{Object} toolbar represents the main node of the toolbar view
 * @param		{i18n} i18n Class to allow translations
 */
var ToolbarView = function (id, parent, i18n) {
    this.id = id;
    this.i18n = i18n;
    
    var newToolbar = jQuery('<div>').addClass(id).addClass('toolbar ui-widget-header ui-corner-all ui-helper-clearfix').appendTo(parent);
  	this.toolbar = newToolbar[0]; //TODO keep jQuery object
};

ToolbarView.prototype = {
	/**
	 * @public
	 * @function
	 * @name		destroy
	 * @memberOf	ToolbarView#
	 * @description	remove the complete toolbar view from the DOM
	 */
    destroy : function () {
		jQuery(this.toolbar).remove();
    },
    
    /**
     * @public
     * @function
	 * @name		addButtonset
	 * @memberOf	ToolbarView#
	 * @description add a new buttonset to the toolbar view,
	 *  either on the next position or on a special position defined by args.index
	 * @param		{String} args.elementName the name of the buttonset
	 * @param		{integer} args.index the position between the other predefined elements where the new buttonset should be added
	 * @return		{Object} the buttonset which was just added
	 */
    addButtonset : function (args) {
    	return this._addElement(jQuery('<span>').buttonset().addClass(args.elementName), args.index);
    },

    /**
     * @public
     * @function
	 * @name		addDivider
	 * @memberOf	ToolbarView#
	 * @description add a new divider to the toolbar view,
	 *  a divider is only a vertical line for a better visual seperation of two buttonsets
	 * @param		{String} args.elementName the name of the divider
	 * @param		{integer} args.index the position between the other predefined elements where the new divider should be added
	 * @return		{jQuery-Object} the divider element which was just added
	 */        
    addDivider : function (args) {
    	return this._addElement(jQuery('<span>').addClass(args.elementName + ' ui-divider'), args.index);
    },
    
    /**
     * @public
     * @function
	 * @name		addSpring
	 * @memberOf	ToolbarView#
	 * @description add a new spring to the toolbar view, a spring creates space between its surrounding elements
	 * @param		{String} args.elementName the name of the spring
	 * @param		{integer} args.index the position between the other predefined elements where the new spring should be added
	 * @return		{jQuery-Object} the spring element which was just added
	 */ 
    addSpring : function (args) {
    	return this._addElement(jQuery('<span>').addClass(args.elementName + ' ui-spring').attr("weight",args.weight), args.index);
    },

    /**
     * @public
     * @function
	 * @name		addText
	 * @memberOf	ToolbarView#
	 * @description adds a new text element to the toolbar view,
	 *  a text element is simple place to show plain some information within the toolbar
	 * @param		{String} args.elementName the name of the text element
	 * @param		{String} args.text the content of the text element
	 * @param		{integer} args.index the position between the other predefined elements where the new divider should be added
	 * @return		{jQuery-Object} the text-element which was just added
	 */        
    addText : function (args) {
    	return this._addElement(jQuery('<span>').addClass(args.elementName + ' ui-text').html(args.text),args.index);
    },
    
    /**
     * @public
     * @function
	 * @name		addImage
	 * @memberOf	ToolbarView#
	 * @description adds a new image element to the toolbar view,
	 *  which displays the supplied image
	 * @param		{String} args.elementName the name of the image (at the same time the class name)
	 * @param		{String} args.src source of the image to display
	 * @param		{integer} args.index the position between the other predefined elements where the new divider should be added
	 * @return		{jQuery-Object} the image-element which was just added
	 */
    addImage : function (args) {
    	return this._addElement(jQuery('<span>').addClass(args.elementName + ' ui-image').append(jQuery('<img>').attr("src", args.src)));
    },
    
    /**
     * @private
     * @function
	 * @name		addElement
	 * @memberOf	ToolbarView#
	 * @description adds a new element to the toolbar view,
	 * @param		{jQuery-Object} element to add to the view
	 * @param		{integer} index the position between the other predefined elements where the new element should be added
	 * @return		{jQuery-Object} the element which was previously added
	 */ 
    _addElement : function (element, index) {
    	if (!isNaN(index) && index < this.toolbar.childNodes.length) {
    		jQuery(this.toolbar.childNodes[index]).before(element);
    	} else {
    		jQuery(this.toolbar).append(element);
    	}
    	return element
    },
    
    /**
     * @public
     * @function
	 * @name		getToolbarElement
	 * @memberOf	ToolbarView#
	 * @description returns the parent tag of a direct toolbar view element (e.g. buttonset or divider)
	 *  (buttons aren't direct elements, they are within a special buttonset
	 * @param		{String} elementName the name of a direct toolbar view element
	 * @return		{Object} returns a single view element
	 */        
    getToolbarElement : function (elementName) {
    	for (var i = 0; i < this.toolbar.childNodes.length; i++) {
    		if (jQuery(this.toolbar.childNodes[i]).hasClass(elementName)) return this.toolbar.childNodes[i];
    	}
    },
    
    /**
     * @public
     * @function
	 * @name		removeToolbarElement
	 * @memberOf	ToolbarView#
	 * @description removes a direct toolbar view element (e.g. buttonset or divider)
	 *  (buttons aren't direct elements, they are within a special buttonset
	 * @param		{String} args.elementName the name of a direct toolbar view element
	 */  
    removeToolbarElement : function (args) {
    	this.toolbar.removeChild(this.getToolbarElement(args.elementName));
    },
     
    /**
     * @public
     * @function
	 * @name		getButtonUi
	 * @memberOf	ToolbarView#
	 * @description returns the button ui properties of a single jQuery Ui Button
	 * @param		{!Object} args.button a special button
	 * @return		{Object} returns the button ui properties <br>
	 * 	{ <br>
	 *   icons: the button icons (primary, second), <br>
	 *   text: if the label will shown within the button <br>
	 *   label: the label and if enabled the text of the button, <br>
	 *   disabled: if the button is enables (false) or disabled (true) <br>
	 *  }
	 */
    getButtonUi : function (args) {
    	var button = this._getButton(args.button);
    	return {
    		'icons' : button.button( "option", "icons" ),
    		'text' : button.button( "option", "text" ),
    		'label' : button.button( "option", "label" ),
    		'disabled' : button.button( "option", "disabled" )
    	};
    },
    
    /**
     * @public
     * @function
	 * @name		setButtonUi
	 * @memberOf	ToolbarView#
	 * @description sets the given button ui properties to a single jQuery Ui Button
	 * @param		{Object} args.button a special button
	 * @param		{Object} args.icons the button icons (primary, second)
	 * @param		{String} args.text if the label will shown within the button
	 * @param		{boolean} args.label the label and if enabled the text of the button,
	 * @param		{boolean} args.disabled if the button is enables (false) or disabled (true)
	 */
    setButtonUi : function (args) {
    	var button = this._getButton(args.button);
    	if (args.icons) button.button( "option", "icons", args.icons );
    	if (args.text != undefined) button.button( "option", "text", args.text );
    	if (args.label) button.button( "option", "label", args.label );
    	if (args.disabled != undefined) {
    		button.button( "option", "disabled", args.disabled );
    		// buttons won't be un-hovered by simply disable them
    		if (args.disabled) button.removeClass("ui-state-hover");
    	}
    	
    },

    /** 
     * @public
     * @function
	 * @name		addButton
	 * @memberOf	ToolbarView#   
	 * @description adds a given button with its given properties to the toolbar view
	 * @param		{String} args.parentName the buttonset name in which the button should insert
	 * @param		{String} args.elementName the name of the button
	 * @param		{String} args.captionId the title-Id text of the button,
	 * @param		{integer} args.index a special position between the other buttons where the current button should be inserted
	 */
    addButton : function (args) {
    	var myButtonset = this.getToolbarElement(args.parentName);
    	var that = this;
    	
		var newButton = null;

		var onClick = function(event) {
				if (jQuery(this).attr("aria-disabled") == "true") return false;
				getEvent(event).cancelBubble = true;
    			jQuery(that).trigger("press", {'elementName' : args.elementName, 'parentName' : args.parentName, 'view' : newButton});
    			return false;
    		};

		if (args.subtype.type == "buttonDefault") {
    		newButton = jQuery('<button>').addClass(args.elementName).click(onClick);
    		jQuery(this.i18n.executeWhenLoaded(function(i) {newButton.attr("title", i.translate(args.captionId))}))
    			.bind("change.i18n load.i18n",function(e, obj) {newButton.attr("title", obj.i18n.translate(args.captionId))});
		} else if (args.subtype.type == "buttonCheck") {
			newButton = jQuery('<input type="checkbox" id="'+args.elementName+'" class='+args.elementName+' /><label for='+args.elementName+' class='+args.elementName+"Label"+'></label>');
			jQuery(this.i18n.executeWhenLoaded(function(i) {newButton.attr("title", i.translate(args.captionId)).find("label").html(i.translate(args.captionId))}))
				.bind("change.i18n load.i18n",function(e, obj) {newButton.attr("title", obj.i18n.translate(args.captionId)).find("label").html(obj.i18n.translate(args.captionId))});
			newButton[1].onclick = onClick;
		}
		
		var curIndex = 0;
    	if (!isNaN(args.index) && myButtonset.childNodes.length != 0) {
    		// needs to differ between checkButtons (Label + Input = 2 Elements) and simple Buttons (Button = 1 Element)
        	var buttonCount = 0;
        	var i = 0;
        	while (buttonCount < args.index && i <= myButtonset.childNodes.length) {
    			if (myButtonset.childNodes[i].nodeName == "LABEL" ||
    				myButtonset.childNodes[i].nodeName == "BUTTON") {
    				buttonCount++;
    			}
    			i++;
        	}
        	
    		newButton.insertBefore(myButtonset.childNodes[i]);
    		
    		curIndex = i;
    		// its an checkButton
    		if (newButton[1]) curIndex++;
    	} else {
    		jQuery(myButtonset).append(newButton);
    		curIndex =  myButtonset.childNodes.length - 1;
    	}

    	// must appear after append (for checkbox-function)
    	newButton = jQuery("."+this.id+" ."+args.parentName+" ."+args.elementName).button(args.ui);

    	if (!args.active) {
    		newButton.button( "option", "disabled", true )
    	}

    	this._checkButtonStyle({'buttonset' : myButtonset, 'buttonIndex' : curIndex, 'reason' : "add"});
    	jQuery(this).trigger("new", {'elementName' : args.elementName, 'parentName' : args.parentName, 'view' : newButton});
    },
    
    /**
     * @public
     * @function
	 * @name		removeButton
	 * @memberOf	ToolbarView#  
	 * @description removes a given button from its buttonset
	 * @param		{String} args.parentName the buttonset name which contains the button
	 * @param		{String} args.elementName the name of the button
	 */
    removeButton : function (args) {
    	var myButtonset = this.getToolbarElement(args.parentName);
    	for (var i = 0; i < myButtonset.childNodes.length; i++) {
			if (myButtonset.childNodes[i].className.indexOf(args.elementName) > -1) {
				this._checkButtonStyle({'buttonset' : myButtonset, 'buttonIndex' : i, 'reason' : "del"});
				myButtonset.removeChild(myButtonset.childNodes[i]);
			}
		}
    },
    
    /**
     * @private
     * @function
     * @name 		_getButton
     * @description returns a button view instance
     * @param 		{Object|String} either a view instance or a jQuery selector
     */
    _getButton : function (button) {
    	if (typeof button === "string"){
    		return jQuery(this.toolbar).find(button);
    	}
    	return button;
    },
    
    /**
     * @private
     * @function
	 * @name		checkButtonStyle
	 * @memberOf	ToolbarView#
	 * @description checks a single button and its neighbours to correct their corners within their buttonset
	 * @param		{Object} args.buttonset the buttonset which contains the button(s)
	 * @param		{integer} args.buttonIndex the index of the current changed button
	 * @param		{String} args.reason the reason of changing (add, del)
	 */
    // checks for round button edges
    _checkButtonStyle : function (args) {
    	var buttonset = args.buttonset;
    	var buttonIndex = args.buttonIndex;
    	
    	
    	// needs to differ between checkButtons (Label + Input = 2 Elements) and simple Buttons (Button = 1 Element)
    	var buttonCount = 0;
    	for (var i = 0; i < buttonset.childNodes.length; i++) {
			if (buttonset.childNodes[i].nodeName == "LABEL" ||
				buttonset.childNodes[i].nodeName == "BUTTON") {
				buttonCount++;
			}
    	}
    	
    	var isFirstButton = ((buttonIndex == 0 && buttonset.childNodes[buttonIndex].nodeName == "BUTTON") ||
    			(buttonIndex == 1 && buttonset.childNodes[buttonIndex].nodeName == "LABEL"));

    	var isLastButton = (buttonIndex == buttonset.childNodes.length - 1);
    	
    	var ancestorIndex;
    	// either its a simple button
    	if (buttonset.childNodes[buttonIndex].nodeName == "BUTTON") {
    		ancestorIndex = buttonIndex - 1;
    	// or its a Label Tag of a check Button
    	} else {
    		ancestorIndex = buttonIndex - 2;
    	}
    	
    	var successorIndex;
    	if (buttonset.childNodes[buttonIndex + 1]) {
			if (buttonset.childNodes[buttonIndex + 1].nodeName == "BUTTON") {
				successorIndex = buttonIndex + 1;
	    	// or its a Input Tag of a check Button
	    	} else {
	    		successorIndex = buttonIndex + 2;
	    	}
    	}
    	
    	
    	if (args.reason == "add") {
    		if (buttonCount == 2) {
    			if (isFirstButton) {
    				jQuery(buttonset.childNodes[successorIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-right');
    				jQuery(buttonset.childNodes[buttonIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-left');
    			} else {
    				jQuery(buttonset.childNodes[ancestorIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-left');
    				jQuery(buttonset.childNodes[buttonIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-right');
    			}	
    		} else if (buttonCount > 2) {
    			if (isFirstButton) {
    				jQuery(buttonset.childNodes[successorIndex])
    					.removeClass('ui-corner-left').addClass('ui-corner-none');
    				jQuery(buttonset.childNodes[buttonIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-left');
    			} else if (isLastButton) {
    				jQuery(buttonset.childNodes[ancestorIndex])
    					.removeClass('ui-corner-right').addClass('ui-corner-none');
    				jQuery(buttonset.childNodes[buttonIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-right');
    			} else {
    				jQuery(buttonset.childNodes[buttonIndex])
    					.removeClass('ui-corner-all').addClass('ui-corner-none');
    			}
    		}
		} else if (args.reason == "del") {
			if (jQuery(buttonset.childNodes[buttonIndex]).hasClass('ui-corner-left')) {
				if (buttonCount == 2) {
					jQuery(buttonset.childNodes[successorIndex])
						.removeClass('ui-corner-right').addClass('ui-corner-all');
			    } else {
			    	jQuery(buttonset.childNodes[successorIndex])
			    		.removeClass('ui-corner-none').addClass('ui-corner-left');
			    }
			} else if (jQuery(buttonset.childNodes[buttonIndex]).hasClass('ui-corner-right')) {
				if (buttonCount == 2) {
					jQuery(buttonset.childNodes[ancestorIndex])
						.removeClass('ui-corner-left').addClass('ui-corner-all');
			    } else {
			    	jQuery(buttonset.childNodes[ancestorIndex])
			    		.removeClass('ui-corner-none').addClass('ui-corner-right');
			    }
			}
		}
    },
    
    /**
     * @public
     * @function
	 * @name		paint
	 * @memberOf	ToolbarView#
	 * @description repaints this view or atleast those components who are not browser automatic refreshed
	 */
    paint: function() {
    	var toolbar = this.toolbar;
    	var width = 0;
    	jQuery(toolbar.childNodes).not(".ui-spring").each(function() {
    		width += Math.ceil(jQuery(this).outerWidth(true));
    	});
    	
    	width = Math.floor(jQuery(toolbar).width() - width);
    	jQuery(toolbar).children(".ui-spring").each(function() {
    		var spring = jQuery(this);
    		spring.css("width", toFloat(spring.attr("weight")) * width);
    	});
    }
};