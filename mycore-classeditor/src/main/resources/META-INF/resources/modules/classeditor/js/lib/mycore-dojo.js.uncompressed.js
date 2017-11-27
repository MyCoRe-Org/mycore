/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

require({cache:{
'mycore/dijit/dijit-all':function(){
define([
	"./AbstractDialog",
	"./PlainButton",
	"./ExceptionDialog",
	"./I18nRow",
	"./Preloader",
	"./Repeater",
	"./RepeaterRow",
	"./SimpleDialog",
	"./TextRow"
], function() {

	// module:
	//		mycore/dijit/dijit-all
	// summary:
	//		A rollup that includes every mycore dijit modules. You probably don't need this.

	console.warn("dijit-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");

	return {};
});

},
'mycore/dijit/PlainButton':function(){
define([
	"dojo/_base/declare", // declare
	"dijit/form/Button",
	"dojo/text!./templates/PlainButton.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dojo/dom-class", // addClass, removeClass
	"dojo/dom-style"
], function(declare, Button, template, on, lang, domConstruct, domClass, domStyle) {

/**
 * Plain dijit button. This button has no background or hover effects.
 * Can be used like a normal dojo button:
 * <button data-dojo-type="mycore.dijit.PlainButton" onClick="...">Hello world</button>
 */
return declare("mycore.dijit.PlainButton", [Button], {
	templateString: template,

	baseClass: "plainButton",

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    _setDisabledAttr: function(/*boolean*/ disabled) {
    	this.inherited(arguments);
    	if(disabled) {
    		domClass.add(this.iconNode, "plain-button-disabled");
    	} else {
    		domClass.remove(this.iconNode, "plain-button-disabled");
    	}
    }

});
});
},
'mycore/common/common-all':function(){
define([
	"./CompoundEdit",
	"./EventDelegator",
	"./I18nStore",
	"./I18nResolver",
	"./I18nManager",
	"./Preloader",
	"./UndoableEdit",
	"./UndoableMergeEdit",
	"./UndoManager"
], function() {

	// module:
	//		mycore/common/common-all
	// summary:
	//		A rollup that includes every mycore common modules. You probably don't need this.

	console.warn("common-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");

	return {};
});

},
'mycore/common/Preloader':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/Evented", // to use on.emit
	"dojo/on", // on
	"dojo/_base/lang" // hitch, clone
], function(declare, Evented, on, lang) {

/**
 * Use this class to preload javascript objects. To add an object call
 * this.preloader.list.push(myobject);. Its important that each
 * object has the following three methods:
 * -getPreloadName()   -> name which describes the object which is preloaded
 * -getPreloadWeight() -> personal guess how long the preload operation take (Integer)
 * -preload()          -> contains all preload operations
 * 
 * The preloader fire the following events:
 * -started								-> before the preload is started
 * -preloadObject {name}				-> before one object is preloaded (name of the object)
 * -preloadObjectFinished {name}{progress}	-> when an object is successfully preloaded (progress in percent)
 * -finished							-> when the whole preload process is finished
 */
return declare("mycore.common.Preloader", Evented, {

	list: null,
	_totalWeight: 0,
	_currentWeight: 0,

    constructor: function(/*Object*/ args) {
    	this.list = [];
    	declare.safeMixin(this, args);
    },

	preload: function () {
		var size = this.list.length;
		on.emit(this, "started", {size: size});
		this._currentWeight = 0;
		this._totalWeight = 0;
		// calculate weight
		for(var i = 0; i < size; i++) {
			var preloadableObject = this.list[i];
			if(!preloadableObject.getPreloadWeight) {
				console.error("No preload weight defined for:" );
				console.log(preloadableObject);
				return;
			}
			if(!preloadableObject.preload) {
				console.error("Object is not preloadable:");
				console.log(preloadableObject);
				return;
			}
			if(!preloadableObject.getPreloadName) {
				console.warn("Warning: no preload name defined for:");
				console.log(preloadableObject);
				continue;
			}
			this._totalWeight +=  preloadableObject.getPreloadWeight();
		}

		// preload
		for(var i = 0; i < size; i++) {
			var preloadableObject = this.list[i];
			on.emit(this, "preloadObject", {name: preloadableObject.getPreloadName()});
			preloadableObject.preload(lang.hitch({instance: this, object: preloadableObject}, this._onLoad));
		}
	},

	/**
	 * This method should only be called from a preloadable object via callback.
	 */
	_onLoad: function() {
		if(!this.instance || !this.object) {
			console.error("Invalid scope of _onLoad:");
			console.log(this);
			return;
		}
		this.instance._currentWeight += this.object.getPreloadWeight();
		var progress = (this.instance._currentWeight / this.instance._totalWeight) * 100;
		// remove from list
		var index = this.instance.list.indexOf(this.object);
		this.instance.list.splice(index, 1);
		// fire finished event
		on.emit(this.instance, "preloadObjectFinished", {
			name: this.object.getPreloadName(),
			progress: progress
		});
		// check is everything is done
		if(this.instance.list.length == 0) {
			on.emit(this.instance, "finished");			
		}
	}

});
});

},
'mycore/dijit/ExceptionDialog':function(){
define([
	"dojo/_base/declare", // declare
	"mycore/dijit/SimpleDialog",
	"dojo/dom-class", // addClass, removeClass
	"dojo/dom-construct", // create
	"dojo/dom-attr", // attr
	"dojo/dom-style", // style
	"dojo/on", // on
	"dojo/_base/lang" // hitch
], function(declare, simpleDialog, domClass, domConstruct, domAttr, domStyle, on, lang) {

return declare("mycore.dijit.ExceptionDialog", simpleDialog, {

	exception: null,
	exceptionI18nCache: {
		"de": {
			"mycore.dijit.exceptionDialog.title": "Fehler",
			"mycore.dijit.exceptionDialog.text": "Es ist ein Fehler aufgetreten, bitte kontaktieren Sie Ihren Administrator."
		},
		"en": {
			"mycore.dijit.exceptionDialog.title": "Exception",
			"mycore.dijit.exceptionDialog.text": "An error occur, please contact your administrator."
		}
	},

    constructor: function(/*Object*/ args) {
		if(args.exception == null) {
			console.error("No exception given in args");
			console.log(args);			
			return;
		}
		this.i18nStore.mixin(this.exceptionI18nCache, false);
		this.i18nTitle = "mycore.dijit.exceptionDialog.title";
		this.i18nText = "mycore.dijit.exceptionDialog.text";
		declare.safeMixin(this, args);
    },

	createContent: function() {
		this.inherited(arguments);
		var file = "[" + this.exception.lineNumber + "] " + this.exception.fileName;
		var excNode = domConstruct.create("div", {
			style: "color: red",
			innerHTML: "<p>"+ file + "<br />" + this.exception.message + "</p>"
		});
		this.content.appendChild(excNode);
	}

});
});


},
'mycore/dijit/Preloader':function(){
define([
	"dojo/_base/declare", // declare
	"dijit/_Widget",
	"dijit/_Templated",
	"dojo/text!./templates/Preloader.html",
	"dojo/_base/lang", // hitch
	"dojo/on", // on
	"dijit/ProgressBar"
], function(declare, _Widget, _Templated, template, lang, on) {

/**
 * Contains a progressbar and a switching text field to show a
 * user the progress of a preloading process.
 */
return declare("mycore.dijit.Preloader", [_Widget, _Templated], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "mycorePreloader",

	preloader: null,
	showText: false,
	text: '',

    constructor: function(/*Object*/ args) {
    	if(!args.preloader) {
    		console.error('No preloader defined. e.g. new mycore.dijit.Preloader({preloader: new mycore.common.Preloader()})');
    	}
    	declare.safeMixin(this, args);
    },

	create: function() {
		this.inherited(arguments);
		this.updateText();
		on(this.preloader, "preloadObjectFinished", lang.hitch(this, function(e) {
			this.progressBar.update({
	            maximum: 100,
	            progress: e.progress
	        });
			this.updateText();
		}));
	},

	updateText: function() {
		if(!this.showText) {
			return;
		}
		var text = "";
		var list = this.preloader.list;
		for(var i = 0; i < list.length; i++) {
			text += list[i].getPreloadName();
			text += i + 1 != list.length ? ", " : "";
		}
		this.text.innerHTML = text.length == 0 ? "" : "[" + text + "]";
	}

});
});

},
'mycore/common/I18nResolver':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/dom-attr", // attr
	"dojo/dom-construct",
	"mycore/util/DOMUtil",
	"mycore/util/DOJOUtil",
	"dijit/Tooltip"
], function(declare, domAttr, domConstruct, domUtil, dojoUtil) {

return declare("mycore.common.I18nResolver", null, {

	store: null, // should be I18nStore

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    /**
     * Use this method to resolve i18n keys in your DOM node or widget.
     */
    resolve: function(/*String*/ language, /*Object*/ object) {
    	if(domUtil.isNode(object)) {
    		this.resolveNode(language, object);
    		return;
    	}
    	if(dojoUtil.isWidget(object)){
    		if(dojoUtil.isWidgetClass(object, "dijit.form.Select")) {
    			this.resolveSelect(language, object);
    		} else if(dojoUtil.isWidgetClass(object, "dijit.form.ValidationTextBox")) { 
    			this.resolveValidationTextBox(language, object);
    		} else if(dojoUtil.isWidgetClass(object, "dijit.form.CheckBox")) { 
    			this.resolveCheckBox(language, object);
    		} else {
    			this.resolveWidget(language, object);
    		}
    		return;
    	}
    	console.error("Cannot resolve object:");
    	console.log(object);
    },

	/**
	 * Helper method to update a node. The node needs an i18n attribute!
	 */
	resolveNode: function(/*String*/ language, /*Node*/ node) {
		this.store.getI18nText({
			language: language,
			label: domAttr.get(node, "i18n"),
			load: function(text) {
				node.innerHTML = text;
			}
		});
	},

	/**
	 * Helper method to update an object. The object needs an i18n attribute!
	 */
	resolveWidget: function(/*String*/ language, /*Widget*/ widget) {
		var i18nKey = widget.get("i18n");
		if(i18nKey) {
			this.store.getI18nText({
				language: language,
				label: i18nKey,
				load: function(text) {
					widget.set("label", text);
				}
			});
		}
		// resolve children
		if(!widget.getChildren) {
			return;
		}
		var widgets = widget.getChildren();
		for(var i = 0; i < widgets.length; i++) {
			var childWidget = widgets[i];
			this.resolveWidget(language, childWidget);
		}
	},

	/**
	 * Helper method to update a validation text box
	 */
	resolveValidationTextBox: function(/*String*/ language, /*dijit.form.ValidationTextBox*/ textBox) {
		if(textBox.i18nPromptMessage) {
			this.store.getI18nText({
				language: language,
				label: textBox.i18nPromptMessage,
				load: function(/*String*/ text) {
					textBox.set("promptMessage", text);
				}
			});
		}
		if(textBox.i18nInvalidMessage) {
			this.store.getI18nText({
				language: language,
				label: textBox.i18nInvalidMessage,
				load: function(/*String*/ text) {
					textBox.set("invalidMessage", text);
				}
			});
		}
		if(textBox.i18nMissingMessage) {
			this.store.getI18nText({
				language: language,
				label: textBox.i18nMissingMessage,
				load: function(/*String*/ text) {
					textBox.set("missingMessage", text);
				}
			});
		}
	},

	/**
	 * Helper method to update a select box. Each option of the select box
	 * needs an i18n attribute.
	 */
	resolveSelect: function(/*String*/ language, /*dijit.form.Select*/ select) {
		var options = select.getOptions();
		for(var i = 0; i < options.length; i++) {
			var option = options[i];
			var label = option.i18n ? option.i18n : null;
			if(label == null) {
				continue;
			}
			var callbackData = {
				select: select,
				option: option
			}
			this.store.getI18nText({
				language: language,
				label: label,
				callbackData: callbackData,
				load: function(/*String*/ text, /*JSON*/ callbackData) {
					callbackData.option.label = text;
					callbackData.select.updateOption(callbackData.option);
				}
			});
		}
	},

	resolveCheckBox: function(/*String*/ language, /*dijit.form.CheckBox*/ widget) {
		this.store.getI18nText({
			language: language,
			label: widget.get("i18n"),
			load: function(text) {
				var labelFor = domConstruct.create("label", {"for": widget.get("id"), innerHTML: text});
				domConstruct.place(labelFor, widget.domNode, "after");
			}
		});
	},

	resolveTooltip: function(/*String*/ language, /*dijit.Widget*/ widget) {
		this.store.getI18nText({
			language: language,
			label: widget.i18nTooltip,
			load: function(text) {
				var tooltip = new dijit.Tooltip({
					label: text
				});
				tooltip.addTarget(widget.domNode);
			}
		})
	}

});
});
},
'dojo/NodeList-manipulate':function(){
define(["./query", "./_base/lang", "./_base/array", "./dom-construct", "./dom-attr", "./NodeList-dom"], function(dquery, lang, array, construct, attr){
	// module:
	//		dojo/NodeList-manipulate

	/*=====
	return function(){
		// summary:
		//		Adds chainable methods to dojo.query() / NodeList instances for manipulating HTML
		//		and DOM nodes and their properties.
	};
	=====*/

	var NodeList = dquery.NodeList;

	//TODO: add a way to parse for widgets in the injected markup?


	function getWrapInsertion(/*DOMNode*/node){
		// summary:
		//		finds the innermost element to use for wrap insertion.

		//Make it easy, assume single nesting, no siblings.
		while(node.childNodes[0] && node.childNodes[0].nodeType == 1){
			node = node.childNodes[0];
		}
		return node; //DOMNode
	}

	function makeWrapNode(/*DOMNode||String*/html, /*DOMNode*/refNode){
		// summary:
		//		convert HTML into nodes if it is not already a node.
		if(typeof html == "string"){
			html = construct.toDom(html, (refNode && refNode.ownerDocument));
			if(html.nodeType == 11){
				//DocumentFragment cannot handle cloneNode, so choose first child.
				html = html.childNodes[0];
			}
		}else if(html.nodeType == 1 && html.parentNode){
			//This element is already in the DOM clone it, but not its children.
			html = html.cloneNode(false);
		}
		return html; /*DOMNode*/
	}

	lang.extend(NodeList, {
		_placeMultiple: function(/*String||Node||NodeList*/query, /*String*/position){
			// summary:
			//		private method for inserting queried nodes into all nodes in this NodeList
			//		at different positions. Differs from NodeList.place because it will clone
			//		the nodes in this NodeList if the query matches more than one element.
			var nl2 = typeof query == "string" || query.nodeType ? dquery(query) : query;
			var toAdd = [];
			for(var i = 0; i < nl2.length; i++){
				//Go backwards in DOM to make dom insertions easier via insertBefore
				var refNode = nl2[i];
				var length = this.length;
				for(var j = length - 1, item; item = this[j]; j--){
					if(i > 0){
						//Need to clone the item. This also means
						//it needs to be added to the current NodeList
						//so it can also be the target of other chaining operations.
						item = this._cloneNode(item);
						toAdd.unshift(item);
					}
					if(j == length - 1){
						construct.place(item, refNode, position);
					}else{
						refNode.parentNode.insertBefore(item, refNode);
					}
					refNode = item;
				}
			}

			if(toAdd.length){
				//Add the toAdd items to the current NodeList. Build up list of args
				//to pass to splice.
				toAdd.unshift(0);
				toAdd.unshift(this.length - 1);
				Array.prototype.splice.apply(this, toAdd);
			}

			return this; // dojo/NodeList
		},

		innerHTML: function(/*String|DOMNode|NodeList?*/ value){
			// summary:
			//		allows setting the innerHTML of each node in the NodeList,
			//		if there is a value passed in, otherwise, reads the innerHTML value of the first node.
			// description:
			//		This method is simpler than the dojo/NodeList.html() method provided by
			//		`dojo/NodeList-html`. This method just does proper innerHTML insertion of HTML fragments,
			//		and it allows for the innerHTML to be read for the first node in the node list.
			//		Since dojo/NodeList-html already took the "html" name, this method is called
			//		"innerHTML". However, if dojo/NodeList-html has not been loaded yet, this
			//		module will define an "html" method that can be used instead. Be careful if you
			//		are working in an environment where it is possible that dojo/NodeList-html could
			//		have been loaded, since its definition of "html" will take precedence.
			//		The nodes represented by the value argument will be cloned if more than one
			//		node is in this NodeList. The nodes in this NodeList are returned in the "set"
			//		usage of this method, not the HTML that was inserted.
			// returns:
			//		if no value is passed, the result is String, the innerHTML of the first node.
			//		If a value is passed, the return is this dojo/NodeList
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"></div>
			//	|	<div id="bar"></div>
			//		This code inserts `<p>Hello World</p>` into both divs:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").innerHTML("<p>Hello World</p>");
			//	| 	});
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars</p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		This code returns `<p>Hello Mars</p>`:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		var message = query("div").innerHTML();
			//	| 	});
			if(arguments.length){
				return this.addContent(value, "only"); // dojo/NodeList
			}else{
				return this[0].innerHTML; //String
			}
		},

		/*=====
		html: function(value){
			// summary:
			//		see the information for "innerHTML". "html" is an alias for "innerHTML", but is
			//		only defined if dojo/NodeList-html has not been loaded.
			// description:
			//		An alias for the "innerHTML" method, but only defined if there is not an existing
			//		"html" method on dojo/NodeList. Be careful if you are working in an environment
			//		where it is possible that dojo/NodeList-html could have been loaded, since its
			//		definition of "html" will take precedence. If you are not sure if dojo/NodeList-html
			//		could be loaded, use the "innerHTML" method.
			// value: String|DOMNode|NodeList?
			//		The HTML fragment to use as innerHTML. If value is not passed, then the innerHTML
			//		of the first element in this NodeList is returned.
			// returns:
			//		if no value is passed, the result is String, the innerHTML of the first node.
			//		If a value is passed, the return is this dojo/NodeList
			return; // dojo/NodeList|String
		},
		=====*/

		text: function(/*String*/value){
			// summary:
			//		allows setting the text value of each node in the NodeList,
			//		if there is a value passed in, otherwise, returns the text value for all the
			//		nodes in the NodeList in one string.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"></div>
			//	|	<div id="bar"></div>
			//		This code inserts "Hello World" into both divs:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").text("Hello World");
			//	| 	});
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars <span>today</span></p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		This code returns "Hello Mars today":
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		var message = query("div").text();
			//	| 	});
			// returns:
			//		if no value is passed, the result is String, the text value of the first node.
			//		If a value is passed, the return is this dojo/NodeList
			if(arguments.length){
				for(var i = 0, node; node = this[i]; i++){
					if(node.nodeType == 1){
						attr.set(node, 'textContent', value);
					}
				}
				return this; // dojo/NodeList
			}else{
				var result = "";
				for(i = 0; node = this[i]; i++){
					result += attr.get(node, 'textContent');
				}
				return result; //String
			}
		},

		val: function(/*String||Array*/value){
			// summary:
			//		If a value is passed, allows seting the value property of form elements in this
			//		NodeList, or properly selecting/checking the right value for radio/checkbox/select
			//		elements. If no value is passed, the value of the first node in this NodeList
			//		is returned.
			// returns:
			//		if no value is passed, the result is String or an Array, for the value of the
			//		first node.
			//		If a value is passed, the return is this dojo/NodeList
			// example:
			//		assume a DOM created by this markup:
			//	|	<input type="text" value="foo">
			//	|	<select multiple>
			//	|		<option value="red" selected>Red</option>
			//	|		<option value="blue">Blue</option>
			//	|		<option value="yellow" selected>Yellow</option>
			//	|	</select>
			//		This code gets and sets the values for the form fields above:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query('[type="text"]').val(); //gets value foo
			//	|		query('[type="text"]').val("bar"); //sets the input's value to "bar"
			// 	|		query("select").val() //gets array value ["red", "yellow"]
			// 	|		query("select").val(["blue", "yellow"]) //Sets the blue and yellow options to selected.
			//	| 	});

			//Special work for input elements.
			if(arguments.length){
				var isArray = lang.isArray(value);
				for(var index = 0, node; node = this[index]; index++){
					var name = node.nodeName.toUpperCase();
					var type = node.type;
					var newValue = isArray ? value[index] : value;

					if(name == "SELECT"){
						var opts = node.options;
						for(var i = 0; i < opts.length; i++){
							var opt = opts[i];
							if(node.multiple){
								opt.selected = (array.indexOf(value, opt.value) != -1);
							}else{
								opt.selected = (opt.value == newValue);
							}
						}
					}else if(type == "checkbox" || type == "radio"){
						node.checked = (node.value == newValue);
					}else{
						node.value = newValue;
					}
				}
				return this; // dojo/NodeList
			}else{
				//node already declared above.
				node = this[0];
				if(!node || node.nodeType != 1){
					return undefined;
				}
				value = node.value || "";
				if(node.nodeName.toUpperCase() == "SELECT" && node.multiple){
					//A multivalued selectbox. Do the pain.
					value = [];
					//opts declared above in if block.
					opts = node.options;
					//i declared above in if block;
					for(i = 0; i < opts.length; i++){
						//opt declared above in if block
						opt = opts[i];
						if(opt.selected){
							value.push(opt.value);
						}
					}
					if(!value.length){
						value = null;
					}
				}
				return value; //String||Array
			}
		},

		append: function(/*String||DOMNode||NodeList*/content){
			// summary:
			//		appends the content to every node in the NodeList.
			// description:
			//		The content will be cloned if the length of NodeList
			//		is greater than 1. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the appended content.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars</p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").append("<span>append</span>");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div id="foo"><p>Hello Mars</p><span>append</span></div>
			//	|	<div id="bar"><p>Hello World</p><span>append</span></div>
			return this.addContent(content, "last"); // dojo/NodeList
		},

		appendTo: function(/*String*/query){
			// summary:
			//		appends nodes in this NodeList to the nodes matched by
			//		the query passed to appendTo.
			// description:
			//		The nodes in this NodeList will be cloned if the query
			//		matches more than one element. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the matched nodes from the query.
			// example:
			//		assume a DOM created by this markup:
			//	|	<span>append</span>
			//	|	<p>Hello Mars</p>
			//	|	<p>Hello World</p>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("span").appendTo("p");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<p>Hello Mars<span>append</span></p>
			//	|	<p>Hello World<span>append</span></p>
			return this._placeMultiple(query, "last"); // dojo/NodeList
		},

		prepend: function(/*String||DOMNode||NodeList*/content){
			// summary:
			//		prepends the content to every node in the NodeList.
			// description:
			//		The content will be cloned if the length of NodeList
			//		is greater than 1. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the appended content.
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars</p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").prepend("<span>prepend</span>");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div id="foo"><span>prepend</span><p>Hello Mars</p></div>
			//	|	<div id="bar"><span>prepend</span><p>Hello World</p></div>
			return this.addContent(content, "first"); // dojo/NodeList
		},

		prependTo: function(/*String*/query){
			// summary:
			//		prepends nodes in this NodeList to the nodes matched by
			//		the query passed to prependTo.
			// description:
			//		The nodes in this NodeList will be cloned if the query
			//		matches more than one element. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the matched nodes from the query.
			// example:
			//		assume a DOM created by this markup:
			//	|	<span>prepend</span>
			//	|	<p>Hello Mars</p>
			//	|	<p>Hello World</p>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("span").prependTo("p");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<p><span>prepend</span>Hello Mars</p>
			//	|	<p><span>prepend</span>Hello World</p>
			return this._placeMultiple(query, "first"); // dojo/NodeList
		},

		after: function(/*String||Element||NodeList*/content){
			// summary:
			//		Places the content after every node in the NodeList.
			// description:
			//		The content will be cloned if the length of NodeList
			//		is greater than 1. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the appended content.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars</p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").after("<span>after</span>");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div id="foo"><p>Hello Mars</p></div><span>after</span>
			//	|	<div id="bar"><p>Hello World</p></div><span>after</span>
			return this.addContent(content, "after"); // dojo/NodeList
		},

		insertAfter: function(/*String*/query){
			// summary:
			//		The nodes in this NodeList will be placed after the nodes
			//		matched by the query passed to insertAfter.
			// description:
			//		The nodes in this NodeList will be cloned if the query
			//		matches more than one element. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the matched nodes from the query.
			// example:
			//		assume a DOM created by this markup:
			//	|	<span>after</span>
			//	|	<p>Hello Mars</p>
			//	|	<p>Hello World</p>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("span").insertAfter("p");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<p>Hello Mars</p><span>after</span>
			//	|	<p>Hello World</p><span>after</span>
			return this._placeMultiple(query, "after"); // dojo/NodeList
		},

		before: function(/*String||DOMNode||NodeList*/content){
			// summary:
			//		Places the content before every node in the NodeList.
			// description:
			//		The content will be cloned if the length of NodeList
			//		is greater than 1. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the appended content.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div id="foo"><p>Hello Mars</p></div>
			//	|	<div id="bar"><p>Hello World</p></div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("div").before("<span>before</span>");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<span>before</span><div id="foo"><p>Hello Mars</p></div>
			//	|	<span>before</span><div id="bar"><p>Hello World</p></div>
			return this.addContent(content, "before"); // dojo/NodeList
		},

		insertBefore: function(/*String*/query){
			// summary:
			//		The nodes in this NodeList will be placed after the nodes
			//		matched by the query passed to insertAfter.
			// description:
			//		The nodes in this NodeList will be cloned if the query
			//		matches more than one element. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		dojo/NodeList, the nodes currently in this NodeList will be returned,
			//		not the matched nodes from the query.
			// example:
			//		assume a DOM created by this markup:
			//	|	<span>before</span>
			//	|	<p>Hello Mars</p>
			//	|	<p>Hello World</p>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("span").insertBefore("p");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<span>before</span><p>Hello Mars</p>
			//	|	<span>before</span><p>Hello World</p>
			return this._placeMultiple(query, "before"); // dojo/NodeList
		},

		/*=====
		remove: function(simpleFilter){
			// summary:
			//		alias for dojo/NodeList's orphan method. Removes elements
			//		in this list that match the simple filter from their parents
			//		and returns them as a new NodeList.
			// simpleFilter: String
			//		single-expression CSS rule. For example, ".thinger" or
			//		"#someId[attrName='value']" but not "div > span". In short,
			//		anything which does not invoke a descent to evaluate but
			//		can instead be used to test a single node is acceptable.

			return; // dojo/NodeList
		},
		=====*/
		remove: NodeList.prototype.orphan,

		wrap: function(/*String||DOMNode*/html){
			// summary:
			//		Wrap each node in the NodeList with html passed to wrap.
			// description:
			//		html will be cloned if the NodeList has more than one
			//		element. Only DOM nodes are cloned, not any attached
			//		event handlers.
			// returns:
			//		the nodes in the current NodeList will be returned,
			//		not the nodes from html argument.
			// example:
			//		assume a DOM created by this markup:
			//	|	<b>one</b>
			//	|	<b>two</b>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query("b").wrap("<div><span></span></div>");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div><span><b>one</b></span></div>
			//	|	<div><span><b>two</b></span></div>
			if(this[0]){
				html = makeWrapNode(html, this[0]);

				//Now cycle through the elements and do the insertion.
				for(var i = 0, node; node = this[i]; i++){
					//Always clone because if html is used to hold one of
					//the "this" nodes, then on the clone of html it will contain
					//that "this" node, and that would be bad.
					var clone = this._cloneNode(html);
					if(node.parentNode){
						node.parentNode.replaceChild(clone, node);
					}
					//Find deepest element and insert old node in it.
					var insertion = getWrapInsertion(clone);
					insertion.appendChild(node);
				}
			}
			return this; // dojo/NodeList
		},

		wrapAll: function(/*String||DOMNode*/html){
			// summary:
			//		Insert html where the first node in this NodeList lives, then place all
			//		nodes in this NodeList as the child of the html.
			// returns:
			//		the nodes in the current NodeList will be returned,
			//		not the nodes from html argument.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div class="container">
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query(".red").wrapAll('<div class="allRed"></div>');
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div class="container">
			// 	|		<div class="allRed">
			// 	|			<div class="red">Red One</div>
			// 	|			<div class="red">Red Two</div>
			// 	|		</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			if(this[0]){
				html = makeWrapNode(html, this[0]);

				//Place the wrap HTML in place of the first node.
				this[0].parentNode.replaceChild(html, this[0]);

				//Now cycle through the elements and move them inside
				//the wrap.
				var insertion = getWrapInsertion(html);
				for(var i = 0, node; node = this[i]; i++){
					insertion.appendChild(node);
				}
			}
			return this; // dojo/NodeList
		},

		wrapInner: function(/*String||DOMNode*/html){
			// summary:
			//		For each node in the NodeList, wrap all its children with the passed in html.
			// description:
			//		html will be cloned if the NodeList has more than one
			//		element. Only DOM nodes are cloned, not any attached
			//		event handlers.
			// returns:
			//		the nodes in the current NodeList will be returned,
			//		not the nodes from html argument.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div class="container">
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query(".red").wrapInner('<span class="special"></span>');
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div class="container">
			// 	|		<div class="red"><span class="special">Red One</span></div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red"><span class="special">Red Two</span></div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			if(this[0]){
				html = makeWrapNode(html, this[0]);
				for(var i = 0; i < this.length; i++){
					//Always clone because if html is used to hold one of
					//the "this" nodes, then on the clone of html it will contain
					//that "this" node, and that would be bad.
					var clone = this._cloneNode(html);

					//Need to convert the childNodes to an array since wrapAll modifies the
					//DOM and can change the live childNodes NodeList.
					this._wrap(lang._toArray(this[i].childNodes), null, this._NodeListCtor).wrapAll(clone);
				}
			}
			return this; // dojo/NodeList
		},

		replaceWith: function(/*String||DOMNode||NodeList*/content){
			// summary:
			//		Replaces each node in ths NodeList with the content passed to replaceWith.
			// description:
			//		The content will be cloned if the length of NodeList
			//		is greater than 1. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		The nodes currently in this NodeList will be returned, not the replacing content.
			//		Note that the returned nodes have been removed from the DOM.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div class="container">
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query(".red").replaceWith('<div class="green">Green</div>');
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div class="container">
			// 	|		<div class="green">Green</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="green">Green</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			content = this._normalize(content, this[0]);
			for(var i = 0, node; node = this[i]; i++){
				this._place(content, node, "before", i > 0);
				node.parentNode.removeChild(node);
			}
			return this; // dojo/NodeList
		},

		replaceAll: function(/*String*/query){
			// summary:
			//		replaces nodes matched by the query passed to replaceAll with the nodes
			//		in this NodeList.
			// description:
			//		The nodes in this NodeList will be cloned if the query
			//		matches more than one element. Only the DOM nodes are cloned, not
			//		any attached event handlers.
			// returns:
			//		The nodes currently in this NodeList will be returned, not the matched nodes
			//		from the query. The nodes currently in this NodeLIst could have
			//		been cloned, so the returned NodeList will include the cloned nodes.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div class="container">
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query(".red").replaceAll(".blue");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div class="container">
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="spacer">___</div>
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="red">Red Two</div>
			//	|	</div>
			var nl = dquery(query);
			var content = this._normalize(this, this[0]);
			for(var i = 0, node; node = nl[i]; i++){
				this._place(content, node, "before", i > 0);
				node.parentNode.removeChild(node);
			}
			return this; // dojo/NodeList
		},

		clone: function(){
			// summary:
			//		Clones all the nodes in this NodeList and returns them as a new NodeList.
			// description:
			//		Only the DOM nodes are cloned, not any attached event handlers.
			// returns:
			//		a cloned set of the original nodes.
			// example:
			//		assume a DOM created by this markup:
			//	|	<div class="container">
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="blue">Blue Two</div>
			//	|	</div>
			//		Running this code:
			//	|	require(["dojo/query", "dojo/NodeList-manipulate"
			//	|	], function(query){
			//	|		query(".red").clone().appendTo(".container");
			//	| 	});
			//		Results in this DOM structure:
			//	|	<div class="container">
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="blue">Blue One</div>
			// 	|		<div class="red">Red Two</div>
			// 	|		<div class="blue">Blue Two</div>
			// 	|		<div class="red">Red One</div>
			// 	|		<div class="red">Red Two</div>
			//	|	</div>

			//TODO: need option to clone events?
			var ary = [];
			for(var i = 0; i < this.length; i++){
				ary.push(this._cloneNode(this[i]));
			}
			return this._wrap(ary, this, this._NodeListCtor); // dojo/NodeList
		}
	});

	//set up html method if one does not exist
	if(!NodeList.prototype.html){
		NodeList.prototype.html = NodeList.prototype.innerHTML;
	}

	return NodeList;
});

},
'mycore/mycore-dojo-all':function(){
define([
	"./common/common-all",
	"./dijit/dijit-all",
	"./util/util-all"
], function() {

	// module:
	//		mycore/mycore-dojo-all
	// summary:
	//		A rollup that includes every mycore dojo modules. You probably don't need this.

	console.warn("mycore-dojo-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");

	return {};
});

},
'mycore/dijit/AbstractDialog':function(){
define([
	"dojo/_base/declare", // declare
	"dijit/Dialog",
	"dojo/on", // on
	"dojo/_base/lang", // hitch
	"dojo/dom-class", // addClass, removeClass
	"dojo/dom-construct", // create place
	"dojo/dom-style",
	"dijit/form/Button",
	"mycore/common/I18nStore",
	"mycore/common/I18nResolver"
], function(declare, Dialog, on, lang, domClass, domConstruct, domStyle) {

/**
 * Dialog which supports three types of options:
 * -Ok
 * -Cancel
 * -Ok, Cancel
 * -Yes, No
 * -Yes, No, Cancel
 * 
 * How to use this class @see SimpleDialog.js.
 */
return declare("mycore.dijit.AbstractDialog", Dialog, {
	Type: {
		ok: "ok",
		cancel: "cancel",
		okCancel: "okCancel",
		yesNo: "yesNo",
		yesNoCancel: "yesNoCancel"
	},
	defaultI18nCache: {
		"de": {
			"mycore.dijit.dialog.ok": "Ok",
			"mycore.dijit.dialog.cancel": "Abbruch",
			"mycore.dijit.dialog.yes": "Ja",
			"mycore.dijit.dialog.no": "Nein"
		},
		"en": {
			"mycore.dijit.dialog.ok": "Ok",
			"mycore.dijit.dialog.cancel": "Cancel",
			"mycore.dijit.dialog.yes": "Yes",
			"mycore.dijit.dialog.no": "No"
		}
	},

	i18nStore: null,
	i18nTitle: null,
	type: null,
	internalDialog: null,
	content: null,
	language: null,

	okButton: null,
	cancelButton: null,
	yesButton: null,
	noButton: null,

	additionalData: null,

	created: false,

    constructor: function(/*Object*/ args) {
    	this.language = "de";
    	this.i18nTitle = "undefined";
    	this.type = this.Type.ok;
    	if(args.i18nStore) {
    		args.i18nStore.mixin(this.defaultI18nCache);
    	} else {
    		this.i18nStore = new mycore.common.I18nStore({cache: this.defaultI18nCache});	
    	}
    	declare.safeMixin(this, args);
    },

	setTitle: function(/*String*/ i18nTitle) {
		this.i18nTitle = i18nTitle;
		this.updateTitle(this.language);
	},
	getTitle: function() {
		return this.internalDialog.get("title");
	},

	_create: function() {
		// create dijit components
		this.internalDialog = new dijit.Dialog();
		this.okButton = new dijit.form.Button({i18n: "mycore.dijit.dialog.ok"});
		this.cancelButton = new dijit.form.Button({i18n: "mycore.dijit.dialog.cancel"});
		this.yesButton = new dijit.form.Button({i18n: "mycore.dijit.dialog.yes"});
		this.noButton = new dijit.form.Button({i18n: "mycore.dijit.dialog.no"});

		// create dialog
		domClass.add(this.internalDialog.domNode, "mycoreDialog");
		this.content = domConstruct.create("div");
		domClass.add(this.content, "content");
		this.internalDialog.set("content", this.content);

		var controls = domConstruct.create("div");
		domClass.add(controls, "controls");
		domConstruct.place(controls, this.internalDialog.domNode);

		if(this.type == this.Type.ok) {
			controls.appendChild(this.okButton.domNode);
		} else if(this.type == this.Type.cancel) {
			controls.appendChild(this.cancelButton.domNode);
		} else if(this.type == this.Type.okCancel) {
			controls.appendChild(this.cancelButton.domNode);
			controls.appendChild(this.okButton.domNode);
		} else if(this.type == this.Type.yesNo) {
			controls.appendChild(this.noButton.domNode);
			controls.appendChild(this.yesButton.domNode);				
		} else if(this.type == this.Type.yesNoCancel) {
			controls.appendChild(this.cancelButton.domNode);
			controls.appendChild(this.noButton.domNode);
			controls.appendChild(this.yesButton.domNode);				
		}

		on(this.okButton, "click", lang.hitch(this, function() {
			this.onBeforeOk();
			this.internalDialog.hide();
			this.onOk();
		}));
		on(this.cancelButton, "click", lang.hitch(this, function() {
			this.onBeforeCancel();
			this.internalDialog.hide();
			this.onCancel();
		}));
		on(this.yesButton, "click", lang.hitch(this, function() {
			this.onBeforeYes();
			this.internalDialog.hide();
			this.onYes();
		}));
		on(this.noButton, "click", lang.hitch(this, function() {
			this.onBeforeNo();
			this.internalDialog.hide();
			this.onNo();
		}));
		this.created = true;
	},

	show: function() {
		if(!this.created) {
			this._create();
			if(this.createContent) {
				this.createContent();
				this.updateLang(this.language);
			} else {
				console.log("TODO: add a 'createContent' method to your dialog!");
			}
		}
		// show
		if(this.beforeShow) {
			this.beforeShow();
		}
		this.internalDialog.show();
	},

	updateLang: function(/*String*/ language) {
		this.language = language;
		if(this.created) {
			var resolver = new mycore.common.I18nResolver({store: this.i18nStore});
			resolver.resolve(language, this.okButton);
			resolver.resolve(language, this.cancelButton);
			resolver.resolve(language, this.yesButton);
			resolver.resolve(language, this.noButton);
			this.updateTitle(language);
		}
	},

	updateTitle: function(language) {
		if(this.i18nTitle) {
			this.i18nStore.getI18nText({
				language: language,
				label: this.i18nTitle,
				load: lang.hitch(this, function(/*String*/ title) {
					this.internalDialog.set("title", title);			
				})
			});
		}
	},

	setWidth: function(/*Integer*/ newWidth) {
		domStyle.set(this.content, "width", newWidth + "px");
	},

	// methods to overwrite
	onBeforeOk: function() {},
	onBeforeCancel: function() {},
	onBeforeYes: function() {},
	onBeforeNo: function() {},
	onOk: function() {},
	onCancel: function() {},
	onYes: function() {},
	onNo: function() {}

});
});

},
'mycore/common/UndoManager':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/on" // on
], function(declare, on) {

return declare("mycore.common.UndoManager", null, {
	pointer: -1,
	limit: 20,
	list: null,
	/**
	 * If a undo or redo action is currently executed.
	 */
	onExecute: false,
	/**
	 * In some cases you want to implement an undo/redo operation on a source which uses setTimeout.
	 * For example: http://dojo-toolkit.33424.n3.nabble.com/Someone-Chop-My-Head-Off-dijit-set-dijit-connect-async-setter-WAT-td1361202.html
	 * You can use blockEvent to stop adding senseless undo's.
	 */
	blockEvent: false,	
	/**
	 * Don't merge undo's. Can be set to true with forceNoMerge().
	 * Be aware that this switch is also used by undo() and redo().
	 */
	_forceNoMergeSwitch: false,

    constructor: function(/*Object*/ args) {
    	this.list = [];
    	declare.safeMixin(this, args);
    },

    /**
	 * Adds a new edit action to the list
	 */
	add: function(/*mycore.common.UndoableEdit*/ undoableEdit) {
		if(this.pointer < this.list.length - 1) {
			this.list = this.list.slice(0, this.pointer + 1);
		}
		if(this.list.length >= this.limit) {
			// to much undoable edits - remove first in row
			this.list = this.list.slice(1);
			this.pointer--;
		}
		// merge
		var lastUndoableEdit = this.list[this.pointer];
		if(	!this._forceNoMergeSwitch &&
			lastUndoableEdit != null &&
			undoableEdit.merge &&
			lastUndoableEdit.merge &&
			lastUndoableEdit.isAssociated(undoableEdit))
		{
			lastUndoableEdit.merge(undoableEdit);
			on.emit(this, "merged", {"mergedEdit" : undoableEdit, "undoableEdit" : lastUndoableEdit});
		} else {
			// default case - add to list
			this.list.push(undoableEdit);
			undoableEdit.undoManager = this;
			this.pointer++;
			on.emit(this, "add", {"undoableEdit" : undoableEdit});
		}
		this._forceNoMergeSwitch = false;
	},

	/**
	 * Checks if an undo operation is possible.
	 */
	canUndo: function() {
		return this.pointer >= 0;
	},

	/**
	 * Checks if a redo operation is possible.
	 */
	canRedo: function() {
		return this.pointer < this.list.length - 1;
	},

	undo: function() {
		if(!this.canUndo()) {
			return;
		}
		this.onExecute = true;
		this.list[this.pointer].undo();
		this.onExecute = false;
		this.pointer--;
		this.forceNoMerge();
		on.emit(this, "undo");
	},

	redo: function() {
		if(!this.canRedo()) {
			return;
		}
		this.pointer++;
		this.onExecute = true;
		this.list[this.pointer].redo();
		this.onExecute = false;
		this.forceNoMerge();
		on.emit(this, "redo");
	},

	/**
	 * This method forces the NEXT added undoable edit to
	 * be not merged.
	 */
	forceNoMerge: function() {
		this._forceNoMergeSwitch = true;
	}

});
});
},
'mycore/util/DOMUtil':function(){
define([
    "exports",
	"dojo/_base/declare", // declare
	"dojo/_base/lang", // hitch
	"dojo/Deferred",
	"dojo/dom-construct",
	"dojo/dom-attr",
	"dojo/query",
	"dojo/NodeList-manipulate"
], function(exports, declare, lang, Deferred, domConstruct, domAttr, query) {

	// http://stackoverflow.com/questions/384286/javascript-isdom-how-do-you-check-if-a-javascript-object-is-a-dom-object
	/**
	 * Returns true if it is a DOM node
	 * @param object the object to test
	 * @returns true if its a DOM node, otherwise false
	 */
	exports.isNode = function(node) {
		return (
			typeof Node === "object" ? node instanceof Node : node &&
			typeof node === "object" && typeof node.nodeType === "number" &&
			typeof node.nodeName==="string"
		);
	}

	/**
	 * Returns true if it is a DOM element
	 * @param object the object to test
	 * @returns true if its a DOM element, otherwise false
	 */
	exports.isElement = function(element) {
		return (
			typeof HTMLElement === "object" ? element instanceof HTMLElement : element &&
			typeof element === "object" && element.nodeType === 1 && typeof element.nodeName==="string"
		);
	}

	/**
	 * Loads a CSS to the head element of the HTML. Uses the promise API
	 * to handle the result.
	 */
	exports.loadCSS = function(/* String */ href) {
		var deferred = new Deferred();
		var css = domConstruct.create('link', {
			"rel": "stylesheet",
			"type": "text/css",
			"href": href,
			"onload": function() {
				deferred.resolve("success");
			},
			"onerror": function(err) {
				deferred.reject({error: err, href: href});
			}
		});
		query("head").append(css);
		return deferred.promise;
	},

	/**
	 * Set dojo theme to body for css support. This is important
	 * for Dijit Components, DnD and Tooltips.
	 */
	exports.updateBodyTheme = function(/*String*/ theme) {
		if(theme == null) {
			theme = "claro";
		}
	    query("body").forEach(function(node) {
	    	domAttr.set(node, "class", "claro");
	    });
	}

});
},
'mycore/common/UndoableEdit':function(){
define([
	"dojo/_base/declare" // declare
], function(declare) {

/**
 * An UndoableEdit represents an edit. The edit may be undone,
 * or if already undone the edit may be redone.
 */
return declare("mycore.common.UndoableEdit", null, {

	/**
	 * Should be set by the manager itself when added to the queue.
	 */
	undoManager: null,

	getLabel: function() {
		return "no label defined";
	},

	undo: function() {
		// overwrite this method!
	},

	redo: function() {
		// overwrite this method!
	}

});
});

},
'mycore/dijit/SimpleDialog':function(){
define([
	"dojo/_base/declare", // declare
	"mycore/dijit/AbstractDialog",
	"dojo/dom-class", // addClass, removeClass
	"dojo/dom-construct", // create
	"dojo/dom-attr", // attr
	"dojo/dom-style", // style
	"dojo/on", // on
	"dojo/_base/lang" // hitch
], function(declare, abstractDialog, domClass, domConstruct, domAttr, domStyle, on, lang) {

return declare("mycore.dijit.SimpleDialog", abstractDialog, {

	i18nText: "undefined",
	imageURL: null,

	textTd: null,
	imageElement: null,

	setText: function(/*String*/ i18nText) {
		this.i18nText = i18nText;
		this.updateText(this.language);
	},

	setImage: function(/*String*/ imageURL) {
		this.image = imageURL;
		this.updateImage();
	},

	createContent: function() {
		// create
		var contentTable = domConstruct.create("table");
		var tr = domConstruct.create("tr");
		var imageTd = domConstruct.create("td");
		this.textTd = domConstruct.create("td");
		this.imageElement = domConstruct.create("img", {style: "padding-right: 10px;"});
		// structure
		this.content.appendChild(contentTable);
		contentTable.appendChild(tr);
		tr.appendChild(imageTd);
		tr.appendChild(this.textTd);
		imageTd.appendChild(this.imageElement);
		// set image & text
		this.setImage(this.imageURL);
		this.setText(this.i18nText);
	},

	updateText: function(language) {
		if(this.i18nText) {
			this.i18nStore.getI18nText({
				language: language,
				label: this.i18nText,
				load: lang.hitch(this, function(/*String*/ text) {
					domAttr.set(this.textTd, {innerHTML: text});
				})
			});
		}
	},

	updateImage: function() {
		if(this.imageURL == null) {
			domStyle.set(this.imageElement, "display", "none");
		} else {
			domStyle.set(this.imageElement, "display", "block");
		}
		domAttr.set(this.imageElement, {src: this.imageURL});
	},

	updateLang: function(/*String*/ language) {
		this.inherited(arguments);
		if(this.created) {
			this.updateText(language);
		}
	}

});
});

},
'mycore/common/I18nStore':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/_base/lang", // hitch
	"dojo/_base/xhr" // xhr
], function(declare, lang, xhr) {

return declare("mycore.common.I18nStore", null, {

	cache: null,
	url: "", // "http://localhost:8080/servlets/MCRLocaleServlet/"

    constructor: function(/*Object*/ args) {
    	this.cache = [];
    	declare.safeMixin(this, args);
    },

	/**
	 * This method fetches a bunch of labels of the current
	 * language and caches them.
	 */
	fetch: function(/*String*/ language, /*String*/ prefix) {
		if(this.cache[language] == undefined) {
			this.cache[language] = {};
		}
		var xhrArgs = {
			url : this.url + "/" + language + "/" + prefix + "*",
			sync : true,
			handleAs : "json",
			load : lang.hitch(this, function(data) {
				for(var item in data) {
					this.cache[language][item] = data[item];
				}
			}),
			error : function(error) {
				console.log("Error while fetching language '" + language + "'!");
			}
		};
		xhr.get(xhrArgs);
	},

	/**
	 * This method is a simple utility function for mixing a new i18n cache
	 * to the store.
	 * 
	 * @param mixinCache the new cache to mix into the store
	 * @param overwrite if true, the mixinCache overwrites existing labels
	 */
	mixin: function(/*Object*/ mixinCache, /*boolean*/ overwrite) {
		overwrite = overwrite === undefined ? true : overwrite;
		for(var language in mixinCache) {
			if(this.cache[language]) {
				for(var label in mixinCache[language]) {
					if(overwrite || !this.cache[language][label]) {
						this.cache[language][label] = mixinCache[language][label];
					}
				}
			} else {
				this.cache[language] = mixinCache[language];
			}
		}
	},

	_getI18nTextFromCache: function(args) {
		if(!args.language) {
			console.error("Undefined language");
			console.log(args);
			throw "Undefined language";
		}
		if(!args.label) {
			console.error("Undefined label");
			console.log(args);
			throw "Undefined label";
		}
		// get from cache
		if(this.cache[args.language] == undefined) {
			var msg = "There are no i18n texts for language '" + args.language + "' defined!";
			console.error(msg);
			console.log(args);
			throw msg;
		}
		return this.cache[args.language][args.label];
	},

	getI18nTextFromCache: function(args) {
		var value = this._getI18nTextFromCache(args);
		if(value == undefined) {
			return "undefined ('" + args.label + "')";
		}
		return value;
	},

	/**
	 * Retrieves an i18n text. 
	 *
	 * @param language
	 * @param label the label of the i18n text
	 * @param callbackData some individual data you want to have in your callback functions
	 * @param load is called if the i18n label is successfully resolved
	 * @param error is called if the i18n label couldn't be resolved
	 */
	getI18nText: function(/*Object*/ args) {
		if(!args.load) {
			console.error("Undefined load method");
			console.log(args);
			throw "Undefined load method";
		}
		var value = this._getI18nTextFromCache(args);
		if(value != undefined) {
			args.load(value, args.callbackData);
		} else {
			// get from server
			this.get18nTextFromServer(args);
		}
	},

	/**
	 * You can use this method to get the text directly from the server without using 
	 * the cache.
	 */
	get18nTextFromServer: function(/*Object*/ args) {
		var xhrArgs = {
			url : this.url + "/" + args.language + "/" + args.label,
			load : lang.hitch(this, function(text) {
				this.cache[language][label] = text;
				if(args.load) {
					args.load(text, args.callbackData);
				}
			}),
			error : function(error) {
				if(args.error) {
					args.error(error, args.callbackData);
				} else {
					console.error("error while retrieving i18n text:");
					console.log(error);
					console.log(args);
				}
			}
		};
		xhr.get(xhrArgs);
	}

});
});

},
'mycore/dijit/I18nRow':function(){
define([
	"dojo/_base/declare", // declare
	"mycore/dijit/RepeaterRow",
	"dijit/_Templated",
	"dojo/text!./templates/I18nRow.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"mycore/util/DOJOUtil",
	"dijit/form/TextBox",
	"dijit/form/Select",
	"dijit/form/Textarea",
	"mycore/common/EventDelegator"
], function(declare, _RepeaterRow, _Templated, template, on, lang, domConstruct, dojoUtil) {

return declare("mycore.dijit.I18nRow", [_RepeaterRow, _Templated], {
	templateString: template,
	widgetsInTemplate: true,

	eventDelegator: null,

	baseClass: "i18nRow",

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    create: function(args) {
    	this.inherited(arguments);
    	this._setLanguages(args.languages ? args.languages : ["de"]);
    	// inital value
    	this.set("value", args.initialValue);
    	// events
    	this.eventDelegator = new mycore.common.EventDelegator({
    		source: this,
    		delegate: false,
    		getEventObject: lang.hitch(this, function(e) {
    			return {
    				row: this,
    				value: this.get("value")
    			}
    		})
    	});
    	this.eventDelegator.register("lang", this.lang);
    	this.eventDelegator.register("text", this.text);
    	this.eventDelegator.register("description", this.description);
    	setTimeout(lang.hitch(this, function() {
    		this.eventDelegator.startDelegation();
    	}), 1);
    },

    _setValueAttr: function(/*Object*/ value, /*Boolean?*/ priorityChange) {
    	if(value == null) {
    		return;
    	}
    	value = this._normalize(value);
    	if(!this.equals(value)) {
        	// block events
        	// we want to block the single change events of the widgets (lang, text, descr)
        	// the goal is to just fire one event for the whole set("value") process
        	// we have to use the eventDelegator to archive this
        	if(this.eventDelegator != null) {
		    	var toBlock = [];
		    	if(this.lang.get("value") != value.lang) {
		    		this.eventDelegator.block("lang");
		    	}
		    	if(this.text.get("value") != value.text) {
		    		this.eventDelegator.block("text");
		    	}
		    	if(this.description.get("value") != value.description) {
		    		this.eventDelegator.block("description");
		    	}
		    	if(priorityChange || priorityChange === undefined) {
		    		this.eventDelegator.fireAfterLastBlock();
		    	}
	    	}
        	if(!this.containsLanguage(value.lang)) {
        		this.lang.addOption({value: value.lang, label: value.lang});
        	}
			this.lang.set("value", value.lang);
			this.text.set("value", value.text);
			this.description.set("value", value.description);
		}
    },

    _getValueAttr: function() {
    	return {
    		lang: this.lang.get("value"),
    		text: this.text.get("value"),
    		description: this.description.get("value")
    	};
    },

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.lang.set("disabled", disabled);
		this.text.set("disabled", disabled);
		this.description.set("disabled", disabled);
		this.inherited(arguments);
	},

	_setLanguages: function(/*Array*/ languages) {
		var selectedLanguage = this.lang.get("value");
		var oldOptions = this.lang.getOptions();
		var newOptions = [];
		for(var i = 0; i < languages.length; i++) {
			newOptions.push({value: languages[i], label: languages[i]});
		}
		if(this.eventDelegator != null) {
			this.eventDelegator.block("lang");
		}
		this.lang.removeOption(oldOptions);
		this.lang.addOption(newOptions);
		if(selectedLanguage != "" && !this.containsLanguage(selectedLanguage)) {
			this.lang.addOption({value: selectedLanguage, label: selectedLanguage});
		}
		this.lang.set("value", selectedLanguage);
	},

	receive: function(/*Object*/ msg) {
		if(!msg.id) {
			return;
		}
		if(msg.id == "resetLang" && msg.languages) {
			this._setLanguages(msg.languages);
		}
	},

	_normalize: function(value) {
		return {
			lang: value.lang ? value.lang: null,
			text: value.text ? value.text: "",
			description: value.description ? value.description : ""
		};
	},

	equals: function(/*Object*/ value) {
		value = this._normalize(value);
		return this.lang.get("value") == value.lang &&
				this.text.get("value") == value.text &&
				this.description.get("value") == value.description;
	},

	containsLanguage: function(/*String*/ lang) {
		var options = this.lang.getOptions();
		for(var i = 0; i < options.length; i++) {
			if(options[i].value == lang) {
				return true;
			}
		}
		return false;
	}

});
});
},
'mycore/util/DOJOUtil':function(){
/**
 * Contains a bunch of utility functions.
 */
define([
    "exports",
	"dojo/_base/declare", // declare
	"dojo/_base/array"
], function(exports, declare, arrayUtil) {

	/**
	 * Returns true if it is a dojo widget
	 * @param widget the widget to test
	 * @returns true if its a widget, otherwise false
	 */
	exports.isWidget = function(widget) {
		return (
			typeof widget === "object" && widget.baseClass != undefined &&
			widget.declaredClass != undefined
		);
	}

	exports.isWidgetClass = function(widget, declaredClass) {
		return (
			this.isWidget(widget) && widget.declaredClass == declaredClass
		);
	}

	/**
	 * Creates a new class by className with arguments.
	 * 
	 * @param className String point separated className e.g. mycore.dijit.TextRepeatable
	 * @param args JSON arguments in the form [{paramName: value}, {paramName2: value2}]
	 */
	exports.instantiate = function(className, args) {
	    var o, f, c = window;
	    var classParts = className.split("."); // split class
	    for(var i = 0; i < classParts.length; i++) {
	    	c = c[classParts[i]];
	    	if(c == undefined) {
	    		throw new Error("Undefined class " + className);
	    	}
	    }
	    f = function() {}; // dummy function
	    f.prototype = c.prototype; // reference same prototype
	    o = new f(); // instantiate dummy function to copy prototype properties
	    c.apply(o, args); // call class constructor, supplying new object as context
	    o.constructor = c; // assign correct constructor (not f)
	    return o;
	}

	/**
	 * Mixin two arrays together. The values will be unique.
	 * dojoUtils.arrayUnique(["a", "b"], ["a", "c"]) => ["a", "b", "c"];
	 */
	exports.arrayUnique = function(a, b) {
		if(a == null || b == null) {
			return a == null && b == null ? null : a == null ? b : a;
		}
		var unique = []; 
		var conact = a.concat(b); // Merged both arrays
		arrayUtil.forEach(conact, function(item) {
			if (item == null || arrayUtil.indexOf(unique, item) > -1) return;
		    unique.push(item); 
		});
		return unique;
	}

	/**
	 * Checks if two arrays are equal. Be aware that this only works if you use
	 * single value arrays like ["a", "b", "c"]. The order is ignored.
	 * arrayEqual(["a", "b"], ["b", "a"]) == true
	 * arrayEqual(["a", "b", "c"], ["b", "a"]) == false
	 */
	exports.arrayEqual = function (a, b) {
		return !!a && !!b && !(a < b || b < a);
	}

	/**
	 * Checks if both objects are equal.
	 */
	exports.deepEqual = function(a, b) {
		var result = true;

		function lengthTest(a, b) {
			var count = 0;
			for( var p in a)
				count++;
			for( var p in b)
				count--;
			return count == 0 ? true: false;
		}

		function typeTest(a, b) {
			return (typeof a == typeof b);
		}

		function test(a, b) {
			if (!typeTest(a, b))
				return false;
			if (typeof a == 'function' || typeof a == 'object') {
				if(!lengthTest(a,b))
					return false;
				for ( var p in a) {
					result = test(a[p], b[p]);
					if (!result)
						return false;
				}
				return result;
			}
			return (a == b);
		}
		return test(a, b);
	}
});

},
'mycore/common/UndoableMergeEdit':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/_base/lang", // hitch
	"mycore/common/UndoableEdit"
], function(declare, lang, undoableEdit) {

return declare("mycore.common.UndoableMergeEdit", undoableEdit, {

	/**
	 * After this time (milliseconds) the isAssociated method always returns false,
	 * so no merge is done.
	 */
	timeout: 1000,

	_timeoutMilli: null,

    constructor: function(/*Object*/ args) {
    	this._timeoutMilli = new Date().getTime();
    	declare.safeMixin(this, args);
    },

    merge: function(/*UndoableEdit*/ mergeWith) {
		// overwrite this method
	},

	/**
	 * This method checks if the given edit is associated with
	 * the merge edit. In general this method is called by the
	 * UndoManager to check if both edits are merged.
	 */
	isAssociated: function(/*UndoableEdit*/ edit) {
		// overwrite this method and call 'this.inherited(arguments)'
		var associated = !(this._timeoutMilli + this.timeout < edit._timeoutMilli);
		this._timeoutMilli = edit._timeoutMilli;
		return associated;
	}

});
});

},
'mycore/common/EventDelegator':function(){
define([
	"dojo/_base/declare", // declare
	"dojo/Evented", // to use on.emit
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"mycore/util/DOJOUtil"
], function(declare, Evented, on, lang, dojoUtil) {

/**
 * With the EventDelegator you can delegate an event from a source
 * to another source. This is useful if you have a component containing
 * a lot of widgets and you want to delegate the events of the single
 * widgets to the component one.
 * 
 * TextBox1 -> 'change' |
 * TextBox2 -> 'change' |--> MyComponent -> 'change'
 * DropDown -> 'change' |
 * 
 * You have to register each widget of the MyComponent to the EventDelegator
 * by calling 'register'. E.g.: myEventDelegator.register('tb1', this.textBox1);
 * 
 * You can now register an event handler on MyComponent to get each change
 * of TextBox1, TextBox2 and DropDown masked as a MyComponent event.
 * 
 * It's also possible to block events. This is important if you want to update
 * the whole MyComponent with new values. For example call:
 * myComponent.update({tb1: "de", tb2: "hello world", dd: "new york"});
 * Without blocking your registered event handler will receive three events that
 * the myComponent is changed. But in general you just want a single event. To
 * archive this you can use the blocking functionality by calling:
 * myEventDelegator.block(["tb1", "tb2", "dd"]);
 * Be aware that the event delegation is blocked until each event is fired! Dojo
 * only fires an event if the value of a widget is CHANGED, so don't block widgets
 * where the values will not be changed by an update. 
 */
return declare("mycore.common.EventDelegator", Evented, {

	/**
	 * If the event delegation is running.
	 */
	delegate: true,

	event: "change",

	source: null,

	objects: null,

	_signals: null,

	_objectsToBlock: null,
	
	_fireAfterLastBlock: false,

	/**
	 * Constructs a new EventDelegator. You need to set the source and
	 * overwrite getEventObject(). E.g. using the EventDelegator in a component:
	 * this.eventDelegator = new mycore.common.EventDelegator({
	 *   source: this,
	 *   getEventObject: dojo.hitch(this, function(e) {
	 *     return { this.getValue() }
	 *   })
	 * });
	 */
    constructor: function(/*Object*/ args) {
    	if(!args.source) {
    		console.log("No source given. Create EventDelegator with new mycore.common.EventDelegator({source: mySource}).");
    		return;
    	}
    	this.objects = {};
    	this._signals = {};
    	this._objectsToBlock = [];
    	declare.safeMixin(this, args);
    },

    startDelegation: function() {
    	this.delegate = true;
    },

    stopDelegation: function() {
    	this.delegate = false;
    },

    /**
     * Register a new widget to the EventDelegator. The id is used to block
     * events.
     */
	register: function (/*String*/ id, /*Object*/ object) {
		this.objects[id] = object;
		this._signals[id] = on(object, this.event, lang.hitch({
			instance: this,
			id: id
		}, this._handleEvent));
	},

	unregister: function(/*String*/ id) {
		delete this.objects[id];
		this._signals[id].remove();
		delete this._signals[id];
		delete this._objectsToBlock[id];
	},

	/**
	 * Block event delegation until the last of the given objects have
	 * executed the event. You can pass an array or a string.
	 */
	block: function(/*Object*/ args) {
		if(typeof args === 'string') {
			this._objectsToBlock.push(args);
		} else if(Object.prototype.toString.call(args) === '[object Array]') {
			this._objectsToBlock = this._objectsToBlock.concat(args);
		} else {
			console.error("Invalid argument: call block() with json or string " + args);
		}
		this._objectsToBlock = dojoUtil.arrayUnique(this._objectsToBlock, []);
	},

	/**
	 * Overwrite this method to return an individual event object.
	 */
	getEventObject: function() {
		return {};
	},

	_handleEvent: function(e) {
		var blockIndex = this.instance._objectsToBlock.indexOf(this.id);
		if(blockIndex != -1) {
			this.instance._objectsToBlock.splice(blockIndex, 1);
			if(this.instance._fireAfterLastBlock && this.instance._objectsToBlock.length == 0) {
				this.instance._fireAfterLastBlock = false;
				lang.hitch(this.instance, this.instance.fire)(e);
			}
			return;
		}
		if(this.instance.delegate) {
			lang.hitch(this.instance, this.instance.fire)(e);
		}
	},

	fire: function(e) {
		on.emit(this.source, this.event, this.getEventObject(e));
	},

	fireAfterLastBlock: function() {
		this._fireAfterLastBlock = true;
	}

});
});

},
'dijit/_Templated':function(){
define([
	"./_WidgetBase",
	"./_TemplatedMixin",
	"./_WidgetsInTemplateMixin",
	"dojo/_base/array", // array.forEach
	"dojo/_base/declare", // declare
	"dojo/_base/lang", // lang.extend lang.isArray
	"dojo/_base/kernel" // kernel.deprecated
], function(_WidgetBase, _TemplatedMixin, _WidgetsInTemplateMixin, array, declare, lang, kernel){

	// module:
	//		dijit/_Templated

	// These arguments can be specified for widgets which are used in templates.
	// Since any widget can be specified as sub widgets in template, mix it
	// into the base widget class.  (This is a hack, but it's effective.)
	// Remove for 2.0.   Also, hide from API doc parser.
	lang.extend(_WidgetBase, /*===== {} || =====*/ {
		waiRole: "",
		waiState:""
	});

	return declare("dijit._Templated", [_TemplatedMixin, _WidgetsInTemplateMixin], {
		// summary:
		//		Deprecated mixin for widgets that are instantiated from a template.
		//		Widgets should use _TemplatedMixin plus if necessary _WidgetsInTemplateMixin instead.

		// widgetsInTemplate: [protected] Boolean
		//		Should we parse the template to find widgets that might be
		//		declared in markup inside it?  False by default.
		widgetsInTemplate: false,

		constructor: function(){
			kernel.deprecated(this.declaredClass + ": dijit._Templated deprecated, use dijit._TemplatedMixin and if necessary dijit._WidgetsInTemplateMixin", "", "2.0");
		},

		_processNode: function(baseNode, getAttrFunc){
			var ret = this.inherited(arguments);

			// Do deprecated waiRole and waiState
			var role = getAttrFunc(baseNode, "waiRole");
			if(role){
				baseNode.setAttribute("role", role);
			}
			var values = getAttrFunc(baseNode, "waiState");
			if(values){
				array.forEach(values.split(/\s*,\s*/), function(stateValue){
					if(stateValue.indexOf('-') != -1){
						var pair = stateValue.split('-');
						baseNode.setAttribute("aria-"+pair[0], pair[1]);
					}
				});
			}

			return ret;
		}
	});
});

},
'mycore/util/util-all':function(){
define([
	"./DOJOUtil",
	"./DOMUtil"
], function() {

	// module:
	//		mycore/util/util-all
	// summary:
	//		A rollup that includes every mycore util modules. You probably don't need this.

	console.warn("util-all may include much more code than your application actually requires. We strongly recommend that you investigate a custom build or the web build tool");

	return {};
});

},
'mycore/common/CompoundEdit':function(){
define([
	"dojo/_base/declare", // declare
	"mycore/common/UndoableEdit"
], function(declare, undoableEdit) {

/**
 * A collection of undoable edits.
 */
return declare("mycore.common.CompoundEdit", undoableEdit, {
	edits: null,

    constructor: function(/*Object*/ args) {
    	this.edits = [];
    	declare.safeMixin(this, args);
    },

	addEdit: function(/*UndoableEdit*/ edit) {
		this.edits.push(edit);
	},

	undo: function() {
		for(var i = this.edits.length - 1; i >= 0; i--) {
			this.edits[i].undo();
		}
	},

	redo: function() {
		for(var i = 0; i < this.edits.length; i++) {
			this.edits[i].redo();
		}
	}

});
});
},
'mycore/common/I18nManager':function(){
define([
    "dojo/_base/declare",
	"dojo/_base/lang",
    "dojo/_base/json",
	"dojo/_base/xhr",
    "mycore/common/I18nStore",
    "mycore/common/I18nResolver"
], function(declare, lang, json, xhr) {

	var I18nManager = declare("mycore.common.I18nManager", [], {

		store: null,

		resolver: null,

		language: null,

		languages: null,

	    init: function(/*URL*/ url) {
			xhr.get({
				url : url + "/language",
				sync : true,
				handleAs : "text",
				load : lang.hitch(this, function(language) {
					this.language = language;
				}),
				error : function(error) {
					this.language = "de";
					console.log("Error while fetching current language! Use 'de' as default.");
					console.log(error);
				}
			});
			xhr.get({
				url : url + "/languages",
				sync : true,
				handleAs : "json",
				load : lang.hitch(this, function(languages) {
					this.languages = languages;
				}),
				error : function(error) {
					this.languages = ["de", "en"];
					console.log("Error while fetching available languages! Use 'de' and 'en'.");
					console.log(error);
				}
			});
	    	this.store = new mycore.common.I18nStore({
				url: url + "/translate"
			});
	    	this.resolver = new mycore.common.I18nResolver({
	    		store: this.store
	    	});
	    },

	    setLanguage: function(/*String*/ language) {
	    	this.language = language;
	    },

	    setLanguages: function(/*Array*/ languages) {
	    	this.languages = languages;
	    },

	    getLanguage: function() {
	    	return this.language;
	    },

	    getLanguages: function() {
	    	return this.languages;
	    },

	    fetch: function(/*String*/ prefix) {
	    	this.store.fetch(this.language, prefix);
	    },

	    get: function(/*Object*/ args) {
	    	if(!args.language) {
	    		args.language = this.language;
	    	}
	    	this.store.getI18nText(args);
	    },

	    getFromCache: function(/*String*/ label) {
	    	return this.store.getI18nTextFromCache({
	    		language: this.language,
	    		label: label
	    	});
	    },

	    resolve: function(/*Object*/ object) {
	    	this.resolver.resolve(this.language, object);
	    },

	    resolveTooltip: function(/*Widget*/ widget) {
	    	this.resolver.resolveTooltip(this.language, widget);
	    }

    });
    if (!_instance) {
        var _instance = new I18nManager();
    }
    return _instance;

});

},
'mycore/dijit/TextRow':function(){
define([
	"dojo/_base/declare", // declare
	"mycore/dijit/RepeaterRow",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dijit/form/TextBox"
], function(declare, _RepeaterRow, on, lang, domConstruct) {

return declare("mycore.dijit.TextRow", [_RepeaterRow], {

	baseClass: "textRow",

	textBox: null,
	
	eventDelegator: null,

	constructor: function(/*Object*/ args) {
		this.inherited(arguments);
		this.textBox = new dijit.form.TextBox({
			value: this.initialValue,
    		style: "width: 100%",
    		intermediateChanges: true
    	});
    	// events
    	this.eventDelegator = new mycore.common.EventDelegator({
    		source: this,
    		delegate: false,
    		getEventObject: lang.hitch(this, function(e) {
    			return {
    				row: this,
    				value: this.get("value")
    			}
    		})
    	});
    	this.eventDelegator.register("text", this.textBox);
    	setTimeout(lang.hitch(this, function() {
    		this.eventDelegator.startDelegation();
    	}), 1);
	},

    create: function() {
    	this.inherited(arguments);
		this.addColumn(this.textBox.domNode);
    },

    _setValueAttr: function(/*String*/ value, /*Boolean?*/ priorityChange) {
    	if(!(priorityChange || priorityChange === undefined)) {
    		this.eventDelegator.block("text");
    	}
    	this.textBox.set("value", value);
    },

    _getValueAttr: function() {
    	return this.textBox.get("value");
    },

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.textBox.set("disabled", disabled);
		this.inherited(arguments);
	},

	equals: function(/*Object*/ value) {
		return this.textBox.get("value") == value;
	}

});
});

},
'mycore/dijit/RepeaterRow':function(){
define([
	"dojo/_base/declare", // declare
	"dijit/_Widget",
	"dijit/_Templated",
	"dojo/Evented", // to use on.emit
	"dojo/text!./templates/RepeaterRow.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dojo/dom-class", // addClass, removeClass
	"dojo/dom-style",
	"dojo/json",
	"dijit/form/Button"
], function(declare, _Widget, _Templated, _Evented, template, on, lang, domConstruct, domClass, domStyle, JSON) {

return declare("mycore.dijit.RepeaterRow", [_Widget, _Templated, _Evented], {
	templateString: template,
	widgetsInTemplate: true,

	disabled: false,

	baseClass: "mycoreRepeaterRow",

	_repeater: null,

	initialValue: null,

	removeable: true,

    constructor: function(/*Object*/ args) {
    	if(args._repeater == null) {
    		console.error("No repeater set for this row. You should call addRow() in your repeater to create a row.");
    		return;
    	}
    	declare.safeMixin(this, args);
    },

	create: function() {
		this.inherited(arguments);
		// add events
		on(this.removeRow, "click", lang.hitch(this, this._onRemove));
	},

    addColumn: function(/*Node*/ node) {
    	var column = domConstruct.create("td");
    	domConstruct.place(node, column);
    	domConstruct.place(column, this.control, "before");
    	return column;
    },

    getRepeater: function() {
    	return this._repeater;
    },

	_onRemove: function() {
		on.emit(this, "remove", {row: this});
	},

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.disabled = disabled;
		this.removeRow.set("disabled", disabled);
		on.emit(this, "disable", {row: this, disabled: disabled});
	},

	_setRemovableAttr: function(/*boolean*/ removable) {
		this.removable = removable;
		domStyle.set(this.removeRow.domNode, "display", (removable ? "block" : "none"));
	},

    _setValueAttr: function(/*String*/ value, /*Boolean?*/ priorityChange) {
    	// extend
    },

    _getValueAttr: function() {
    	// extend
    },

    /**
     * Checks if the given data is equal with the current row data.
     * Overwrite this method, its needed by the Repeater.
     */
    equals: function(/*Object*/ data) {
    	// extend
    	return false;
    },

//    /**
//     * This method is called when the row store of the repeater is changed. Use it for
//     * row comprehensive data.
//     */
//    rowStoreUpdated: function(id, value) {
//    	// extend
//    },

    /**
     * Receive a broadcast message from the repeater.
     */
    receive: function(/*Object*/ msg) {
    	// extend
    }

});
});
},
'mycore/dijit/Repeater':function(){
define([
	"dojo/_base/declare", // declare
	"dijit/_Widget",
	"dijit/_Templated",
	"dojo/Evented", // to use on.emit
	"dojo/text!./templates/Repeater.html",
	"dojo/on", // on
	"dojo/_base/lang", // hitch, clone
	"dojo/dom-construct", // create place
	"dojo/dom-class", // addClass, removeClass
	"mycore/util/DOJOUtil",
	"mycore/dijit/RepeaterRow",
	"mycore/dijit/PlainButton"
], function(declare, _Widget, _Templated, _Evented, template, on, lang, domConstruct, domClass, dojoUtil) {

return declare("mycore.dijit.Repeater", [_Widget, _Templated, _Evented], {
	templateString: template,
	widgetsInTemplate: true,

	baseClass: "mycoreRepeater",

	row: null,

	disabled: false,

	_rows: null,

	minOccurs: 0,

	/**
	 * The head of the repeater. Should be a row like <tr><th>...</th>...</tr>
	 */
	head: null,

    constructor: function(/*Object*/ args) {
    	if(!args.row || !args.row.className) {
    		console.error("No row class is given. Create e.g. with {row: {class: 'my.sample.className'}}");
    		console.log(args);
    		return;
    	}
    	this._rows = [];
    	declare.safeMixin(this, args);
    },

	create: function() {
		this.inherited(arguments);
		for(var i = 0; i < this.minOccurs; i++) {
			this._addRow({disabled: this.disabled});
		}
		on(this.addRowButton, "click", lang.hitch(this, this._onAdd));
	},

	/**
	 * Set the value of the repeater. The value should be an array, which contains
	 * one element for each row. e.g. [{id: "object1" ...}, {id: "object2" ...}, ...]
	 */
	_setValueAttr: function(value, /*Boolean?*/ priorityChange) {
		var fireEvent = false;
		// add and update rows
		var internalCount = 0;
		for (var i = 0; i < value.length; i++) {
			if(internalCount < this._rows.length) {
				var row = this._rows[internalCount];
				if(!row.equals(value[i])) {
					row.set("value", value[i], false);
					fireEvent = true;
				}
			} else {
				fireEvent = true;
				this._addRow({initialValue: value[i], disabled: this.disabled});
			}
			internalCount++;
		}
		// remove rows
		while(this._rows.length > internalCount) {
			fireEvent = true;
			this._removeRow(this._rows[this._rows.length - 1]);
		}
		if(fireEvent && (priorityChange || priorityChange === undefined)) {
			setTimeout(lang.hitch(this, function() {
				this._onChange();
			}), 1);
		}
	},

	_getValueAttr: function() {
		var values = [];
		for(var i = 0; i < this._rows.length; i++) {
			values.push(this._rows[i].get("value"));
		}
		return values;
	},

	/**
	 * Adds a new row at the end of the list.
	 * @param args the constructor arguments the new row is called with
	 */
	addRow: function(/*Object*/ args) {
		var row = this._addRow(args);
		// fire change event
		this._onChange();
		return row;
	},

	_addRow: function(/*Object*/ args) {
		// row arguments
		var rowArgs = this.row.args ? lang.clone(this.row.args) : {};
		lang.mixin(rowArgs, args);
		if(rowArgs.removable == null) {
			rowArgs.removable = this._rows.length >= this.minOccurs;
		}
		// set repeater
		rowArgs._repeater = this;
		// new row
		var row = dojoUtil.instantiate(this.row.className, [rowArgs]);
		// add events
		on(row, "remove", lang.hitch(this, this._onRemove));
		on(row, "change", lang.hitch(this, this._onChange));
		// dom
		domConstruct.place(row.domNode, this.addNode, "before");
		// push to list
		this._rows.push(row);
		return row;
	},

	/**
	 * Removes a row from the repeater.
	 */
	removeRow: function(row) {
		var index = this._removeRow(row);
		this._onChange();
	},

	_removeRow: function(row) {
		var index = this.indexOf(row);
		this._rows.splice(index, 1);
		// destroy row
		row.destroy();
		return index;
	},

	_setHeadAttr: function(/*Node*/ node) {
		if(this.head != null) {
			domConstruct.destroy(this.head);
		}
		this.head = node;
		domConstruct.place(this.head, this.tableBody, "first");
	},

	_onAdd: function(e) {
		this.addRow();
	},

	_onRemove: function(e) {
		this.removeRow(e.row);
	},

	_onChange: function() {
		on.emit(this, "change", {source: this});
	},

	_setDisabledAttr: function(/*boolean*/ disabled) {
		this.disabled = disabled;
		for(var i = 0; i < this._rows.length; i++) {
			this._rows[i].set("disabled", disabled);
		}
		this.addRowButton.set("disabled", disabled);
		on.emit(this, "disable", {disabled: disabled});
	},

	/**
	 * Returns the index position of the given row.
	 */
	indexOf: function(/*mycore.dijit.RepeaterRow*/ row) {
		return this._rows.indexOf(row);
	},

	/**
	 * Broadcast a message to the containing rows.
	 */
	broadcast: function(/*Object*/ args) {
		for(var i = 0; i < this._rows.length; i++) {
			this._rows[i].receive(args);
		}
	}

});
});

},
'url:mycore/dijit/templates/RepeaterRow.html':"<tr class=\"${baseClass}\">\n\t<!-- add other table columns here -->\n\t<td class=\"control remove\" data-dojo-attach-point=\"control\">\n\t\t<button data-dojo-attach-point=\"removeRow\" data-dojo-type=\"mycore.dijit.PlainButton\"\n\t\t\t\tdata-dojo-props=\"showLabel: false, iconClass:'fa fa-times'\"></button>\n\t</td>\n</tr>\n",
'url:mycore/dijit/templates/Preloader.html':"<div class=\"${baseClass}\" role=\"progressbar\" tabindex=\"-1\">\n\t<div data-dojo-attach-point=\"progressBar\" data-dojo-type=\"dijit.ProgressBar\"></div>\n\t<div data-dojo-attach-point=\"text\" class=\"text\">${text}</div>\n    <div data-dojo-attach-point=\"containerNode\"></div>\n</div>",
'url:mycore/dijit/templates/Repeater.html':"<div class=\"${baseClass}\" role=\"\" tabindex=\"-1\">\n\t<table class=\"contentNode\">\n\t\t<tbody data-dojo-attach-point=\"tableBody\">\n\t\t\t<tr data-dojo-attach-point=\"addNode\">\n\t\t\t\t<td class=\"control add\">\n\t\t\t\t\t<button\tdata-dojo-attach-point=\"addRowButton\" data-dojo-type=\"mycore.dijit.PlainButton\"\n\t\t\t\t\t\t\tdata-dojo-props=\"showLabel: false, iconClass:'fa fa-plus'\"></button>\n\t\t\t\t</td>\n\t\t\t</tr>\n\t\t</tbody>\n\t</table>\n    <div data-dojo-attach-point=\"containerNode\"></div>\n</div>\n",
'url:mycore/dijit/templates/PlainButton.html':"<a class=\"plain-button\" role=\"presentation\">\n\t<span data-dojo-attach-point=\"titleNode,focusNode\" role=\"button\" aria-labelledby=\"${id}_label\"\n\t\tdata-dojo-attach-event=\"ondijitclick:_onClick\">\n\t\t<span data-dojo-attach-point=\"iconNode\" class=\"plain-icon\"></span>\n\t\t<span data-dojo-attach-point=\"containerNode\" class=\"plain-label\">${label}</span>\n\t</span>\n\t<input ${!nameAttrSetting} type=\"${type}\" value=\"${value}\" class=\"dijitOffScreen\"\n\t\t\ttabIndex=\"-1\" role=\"presentation\" data-dojo-attach-point=\"valueNode\"/>\n</a>\n",
'url:mycore/dijit/templates/I18nRow.html':"<tr class=\"${baseClass}\">\n\t<td class=\"content lang\">\n\t\t<select data-dojo-attach-point=\"lang\" data-dojo-type=\"dijit.form.Select\"></select>\n\t</td>\n\t<td class=\"content text\">\n\t\t<input data-dojo-attach-point=\"text\" data-dojo-type=\"dijit.form.TextBox\" data-dojo-props=\"intermediateChanges: true\"/>\n\t</td>\n\t<td class=\"content description\">\n\t\t<textarea data-dojo-attach-point=\"description\" data-dojo-type=\"dijit.form.Textarea\" data-dojo-props=\"intermediateChanges: true\"></textarea>\n\t</td>\n\t<!-- add other table columns here -->\n\t<td class=\"control remove\" data-dojo-attach-point=\"control\">\n\t\t<button data-dojo-attach-point=\"removeRow\" data-dojo-type=\"mycore.dijit.PlainButton\"\n\t\t\t\tdata-dojo-props=\"showLabel: false, iconClass:'fa fa-times'\"></button>\n\t</td>\n</tr>\n"}});
define("mycore/mycore-dojo", [], 1);
