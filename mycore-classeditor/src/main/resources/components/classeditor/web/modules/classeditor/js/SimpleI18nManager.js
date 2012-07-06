/**
 * Localisation Manager
 */
var SimpleI18nManager = (function() {

	var instance = null;
	var supportedLanguages = undefined;
	var currentLanguage = "de";
	var i18nMap = [];
	var settings;

	function CreateI18nManager() {

		this.initialize = function(newSettings) {
			settings = newSettings;
			supportedLanguages = dojo.mixin(["de", "en"], settings.supportedLanguages);
			supportedLanguages = dojo.mixin(supportedLanguages, dojo.fromJson(dojo.cookie("classeditor.languages")));
			dojo.cookie("classeditor.languages", dojo.toJson(supportedLanguages), {expires: 365});
			if(settings.language != null) {
				this.setCurrentLanguage(settings.language);
			}
			this.prefetchLang("component.classeditor");
		}

		/**
		 * This method fetches a bunch of labels of the current
		 * language and caches them.
		 */
		this.prefetchLang = function(/*String*/ prefix) {
			var xhrArgs = {
				url : settings.webAppBaseURL + "servlets/MCRLocaleServlet/"+currentLanguage+"/" + prefix + "*",
				sync : true,
				handleAs : "json",
				load : function(data) {
					for(var item in data) {
						i18nMap[item] = data[item];
					}
				},
				error : function(error) {
					console.log("Error while fetching language!");
				}
			};
			dojo.xhrGet(xhrArgs);
		}
		
		this.setSupportedLanguages = function(/*Array*/ langArr) {
			supportedLanguages = langArr;
			dojo.cookie("classeditor.languages", dojo.toJson(supportedLanguages), {expires: 365});
		}

		this.getSupportedLanguages = function() {
			return supportedLanguages;
		}

		this.setCurrentLanguage = function(/*String*/ newLang) {
			if(!this.isSupportedLanguage(newLang)) {
				console.log("'" + newLang + "' is not supported! Valid languages are: " + supportedLanguages);
				return;
			}
			currentLanguage = newLang;
		}

		this.getCurrentLanguage = function() {
			return currentLanguage;
		}

		this.addSupportedLanguage = function(/*String*/ newLang) {
			supportedLanguages.push(newLang);
			dojo.cookie("classeditor.languages", dojo.toJson(supportedLanguages), {expires: 365});
		}

		this.isSupportedLanguage = function(/*String*/ lang) {
			return dojo.indexOf(supportedLanguages, lang) >= 0;
		}

		this.get = function(/*String*/ label) {
			return i18nMap[label];
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
