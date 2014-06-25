/**
 * Localisation Manager
 */
var I18nManager = (function() {
	var default_lang_package = "component.wcms";
	
	var instance = null;
	var supportedLanguages = undefined;
	var cachedLanguages = [];
	var currentLang = undefined;
	var cache = new wcms.util.Hashtable();

	function CreateI18nManager() {

		this.initialize = function(/*Array*/ langArr) {
			if(langArr.length <= 0) {
				console.log("Empty language array. Couldn't initialize I18nManager!");
				return;
			}
			supportedLanguages = langArr;
		}

		this.preload = function() {
			currentLang = wcms.startLang;
			// create cache array
			var validLang = false;
			for(var i = 0; i < supportedLanguages.length; i++) {
				cache.put(supportedLanguages[i], new wcms.util.Hashtable());
				if(currentLang == supportedLanguages[i])
					validLang = true;
			}
			if(!validLang) {
				console.log("The current language '" + currentLang + "' is not defined in supportedLanguages[" + supportedLanguages + "]!");
				return;
			}
			// prefetch current lang
			this.prefetchLang(default_lang_package);			
		}
		this.getPreloadName = function() {
			return "I18nManager";
		}
		this.getPreloadWeight = function() {
			return 5;
		}

		/**
		 * This method fetches a bunch of labels of the current
		 * language and caches them.
		 */
		this.prefetchLang = function(/*String*/ prefix) {
			var langMap = cache.get(currentLang);
			if(langMap == undefined) {
				console.log("Undefined language " + currentLang + "!");
				return;
			}
			var xhrArgs = {
				url : webApplicationBaseURL + "servlets/MCRLocaleServlet/"+currentLang+"/" + prefix + "*",
				sync : true,
				handleAs : "json",
				load : function(data) {
					for(var item in data) {
						langMap.put(item, data[item]);
					}
					cachedLanguages[currentLang] = true;
				},
				error : function(error) {
					console.log("Error while fetching language!");
				}
			};
			dojo.xhrGet(xhrArgs);
		}

		/**
		 * Getting the current language.
		 * 
		 * @return language (de, en ...)
		 */
		this.getLang = function() {
			return currentLang;
		}

		/**
		 * Sets the new language.
		 * 
		 * @param newLang language to set
		 */
		this.setLang = function(/* String */newLang) {
			currentLang = newLang;
			if(!cachedLanguages[currentLang]) {
				this.prefetchLang(default_lang_package);
			}
		}

		/**
		 * Returns an array of all languages which are supported by
		 * the manager.
		 */
		this.getSupportedLanguages = function() {
			return supportedLanguages;
		}

		/**
		 * Retrieves an i18n text. 
		 * 
		 * @param label the label of the i18n text
		 * @param callbackData some individual data you want to have in your callback functions
		 * @param onSuccess is called if the i18n label is successfully resolved
		 * @param onError is called if the i18n label coudnt be resolved
		 */
		this.getI18nText = function(/*String*/ label, /*Object*/ callbackData, /*function*/ onSuccess, /*function*/ onError) {
			// get from cache
			var langMap = cache.get(currentLang);
			if(langMap == undefined) {
				console.log("Undefined language " + currentLang + "!");
				return;
			}
			var value = langMap.get(label);
			if(value != undefined) {
				onSuccess(value, callbackData);
			} else {
				// get from server
				this.get18nTextFromServer(currentLang, label, callbackData, onSuccess, onError);
			}
		}
		
		this.getI18nTextAsString = function(label) {
			var t = undefined;
			this.getI18nText(label,null,dojo.hitch(this, function(text) {
				t = text;
			}),null);
			return t;
		}

		this.get18nTextFromServer = function(/*String*/ lang, /*String*/ label, /*Object*/ callbackData, /*function*/ onSuccess, /*function*/ onError) {
			var xhrArgs = {
				url : webApplicationBaseURL + "servlets/MCRLocaleServlet/"+lang+"/" + label,
				load : function(data) {
					// add to cache
					var langMap = cache.get(lang);
					langMap.put(label, data);			
					// callback
					onSuccess(data, callbackData);
				},
				error : function(error) {
					onError(error, callbackData);
				}
			};
			dojo.xhrGet(xhrArgs);
		}

		/**
		 * Helper method to update a node. The node needs an i18n attribute!
		 */
		this.updateI18nNode = function(/*Node*/ node) {
			this.getI18nText(
				dojo.attr(node, "i18n"),
				node,
				function(/*String*/ text, /*Node*/ node) {
					node.innerHTML = text;
				},
				function(error) {
					console.log("error while retrieving i18n text for node '" + dojo.attr(node, "i18n") + "'!\n" + error);
				}
			);
		}

		/**
		 * Helper method to update an object. The object needs an i18n attribute!
		 */
		this.updateI18nObject = function(/*Object*/ obj) {
			this.getI18nText(
				obj.get("i18n"),
				obj,
				function(/*String*/ text, /*Object*/ obj) {
					obj.set("label", text);
				},
				function(error) {
					console.log("error while retrieving i18n text for node '" + obj.get("i18n") + "'!\n" + error);
				}
			);
		}

		/**
		 * Helper method to update a button. The button needs an i18n attribute!
		 */
		this.updateI18nButton = function(/*dijit.form.Button*/ button) {
			this.getI18nText(
				button.i18n,
				button,
				function(/*String*/ text, /*dijit.form.Button*/ button) {
					button.set("label", text);
				},
				function(error) {
					console.log("error while retrieving i18n text '" + label + "'!\n" + error);
				}
			);
		}

		/**
		 * Helper method to update a validation text box
		 */
		this.updateI18nValidationTextBox = function(/*dijit.form.ValidationTextBox*/ textBox) {
			if(textBox.i18nMessage) {
				this.getI18nText(textBox.i18nMessage, textBox,
					function(/*String*/ text, /*dijit.form.ValidationTextBox*/ textBox) {
						textBox.set("message", text);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
			if(textBox.i18nInvalidMessage) {
				this.getI18nText(textBox.i18nInvalidMessage,	textBox,
					function(/*String*/ text, /*dijit.form.ValidationTextBox*/ textBox) {
						textBox.set("invalidMessage", text);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
			if(textBox.i18nPromptMessage) {
				this.getI18nText(textBox.i18nPromptMessage, textBox,
					function(/*String*/ text, /*dijit.form.ValidationTextBox*/ textBox) {
						textBox.set("promptMessage", text);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
			if(textBox.i18nMissingMessage) {
				this.getI18nText(textBox.i18nMissingMessage,	textBox,
					function(/*String*/ text, /*dijit.form.ValidationTextBox*/ textBox) {
						textBox.set("missingMessage", text);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
		}

		/**
		 * Helper method to update a select box. Each option of the select box
		 * needs an i18n attribute.
		 */
		this.updateI18nSelect = function(/*dijit.form.Select*/ select) {
			var options = select.getOptions();
			for(var i = 0; i < options.length; i++) {
				var option = options[i];
				var label = option.i18n;
				if(label == null)
					continue;
				var callbackData = {
					select: select,
					option: option
				}
				this.getI18nText(
					label,
					callbackData,
					function(/*String*/ text, /*JSON*/ callbackData) {
						callbackData.option.label = text;
						callbackData.select.updateOption(callbackData.option);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
		}

		/**
		 * Helper method to update a menu. Each menu entry needs an i18n attribute.
		 */
		this.updateI18nMenu = function(/*Widget*/ parentWidget) {
			if(!parentWidget.getChildren)
				return;
			var widgets = parentWidget.getChildren();
			if(widgets.length <= 0)
				return;

			for(var i = 0; i < widgets.length; i++) {
				var widget = widgets[i];
				if(!widget.i18n)
					continue;
				if(widget.declaredClass == "dijit.PopupMenuItem")
					this.updateI18nMenu(widget.popup);
				this.getI18nText(
					widget.i18n,
					widget,
					function(/*String*/ text, /*Widget*/ widget) {
						widget.set("label", text);
					},
					function(error) {
						console.log("error while retrieving i18n text '" + label + "'!\n" + error);
					}
				);
			}
		}
	}

	return new function() {
		this.getInstance = function() {
			if(instance == null) {
				instance = new CreateI18nManager();
				instance.constructor = null;
			}
			return instance;
		}
	}
})();
