var iview = iview || {};

/**
 * @class
 * @constructor
 * @memberOf 	iview
 * @name		i18n
 * @param		{string} location baseURI where the translations can be loaded from, with closing /
 * @param		{string} [defLang=de] language to use if none is specified by function calls
 * @description functionality for internationalisation
 */
iview.i18n = function(location, defLanguage, prefix) {
	/**
	 * @private
	 * @name		location
	 * @memberOf	iview.i18n#
	 * @type		string
	 * @description	holds the location where translations can be loaded from
	 */
	this._location = location;
	/**
	 * @private
	 * @name		langs
	 * @memberOf	iview.i18n#
	 * @type		object
	 * @description	holds the already loaded translations
	 */
	this._langs = {};
	/**
	 * @private
	 * @name		deferedObjects
	 * @memberOf	iview.i18n#
	 * @type		object
	 * @description	holds the promise Objects, where translations jobs are enqueued
	 */
	this._deferredObjects = {};
	/**
	 * @private
	 * @name		defLang
	 * @memberOf	iview.i18n#
	 * @type		string
	 * @description	languageCode for the default Language which is returned through all functions if nothing different is specified
	 */
	this._defLang = defLanguage || "de";
	/**
	 * @private
	 * @name		prefix
	 * @memberOf	iview.i18n#
	 * @type		string
	 * @description	holds an optional prefix which will be added infront of the supplied string-id
	 */
	this._prefix = (typeof prefix !== "undefined")? prefix + "." : "";
	
	this.loadLanguage(this._defLang);
};

iview.i18n.prototype = {
	/**
	 * @public
	 * @function
	 * @name		loadLanguage
	 * @memberOf	iview.i18n#
	 * @param		{string} languageCode of the language which shall be newly loaded
	 * @param		{array|function} [callback] a single function or an array of functions which is called after the language file has been loaded
	 * @description loads the given language and makes it for further access available;
		As soon as the new language is available an event load.i18n is raised on this, if the language is already loaded the event will not be triggered again
	 * @return		this
	 */
	loadLanguage: function(languageCode, callback) {
		this._langs[languageCode] = {};
		var that = this;
		this._deferredObjects[languageCode] = new jQuery.Deferred();
		jQuery.getJSON(this._location + "servlets/MCRLocaleServlet/" + languageCode + "/component.iview2.*", function(data) {
			jQuery.each(data,function(key,val) {
        that._langs[languageCode][key] = val;
			});
			jQuery(that).trigger('load.i18n', {'language': languageCode, 'i18n':that});
		}).done(that._deferredObjects[languageCode].resolve(that)).fail(that._deferredObjects[languageCode].reject(that));
		if (typeof callback !== "undefined") {
			this.executeWhenLoaded(callback, languageCode);
		}
		return this;
	},

	/**
	 * @public
	 * @function
	 * @name		translate
	 * @memberOf	iview.i18n#
	 * @param		{string} id identifier for the translation to load
	 * @param		{string} [languageCode] language to get translation from, if none is specified the current default language will be returned
	 * @description returns for the given id and languageCode the translation, if the translation isn't available the result looks this way: languageCode::ID;
	 * note that if the language is currently not loaded an empty string will be returned, to ensure that the language is already loaded @see executeWhenLoaded
	 * @return		string translation of the given id in the requested language or languageCode::ID if no translation was found
	 */
	translate : function(id, languageCode) {
    if (typeof languageCode === "undefined") {
      return this.translate(id, this._defLang);
    }
    var translation = this._langs[languageCode][this._prefix + id];
    if (typeof translation === "undefined") {
      //try without prefix
      translation = this._langs[languageCode][id];
    }
    if (typeof translation !== "undefined") {
      return translation;
    } else {
      if (languageCode !== this._defLang) {
        //try with default language
        translation = this.translate(id, this._defLang);
        if (translation.substring(0, this._defLang.length + 2) !== this._defLang + "::") {
          return translation;
        }
      }
      return (languageCode) + "::" + id;
    }
  },
	
	/**
	 * @public
	 * @function
	 * @name		executeWhenLoaded
	 * @memberOf	iview.i18n#
	 * @param		{array|function} func single function or array of functions which are executed as soon as the given Language is available(loaded)
	 * @param		{string} [languageCode] for which the registration shall happen, if no language is specified the current default Language will be used
	 * @description attaches functions which are executed as soon as the requested language is loaded from the server;
	 *  If you're sure that the language is already used you can use @see translate.
	 *	The i18n object will be handled as param 0 to the called function.
	 * @return		this
	 */
	executeWhenLoaded: function(func, languageCode) {
		this._deferredObjects[languageCode || this._defLang].done(func);
		return this;
	},
	
	/**
	 * @public
	 * @function
	 * @name		setDefaultLanguage
	 * @memberOf	iview.i18n#
	 * @param		{string} languageCode which shall be the new default Language
	 * @description sets the new default Language, if the language is currently not loaded it will be loaded
	 * @param		{array|function} [callback] a single function or an array of functions which is called after the language file has been loaded.
	 *	After the change the event change.i18n is raised to notify all listeners about the new default language 
	 * @return		this
	 */
	setDefaultLanguage: function(languageCode, callback) {
		var that = this;
		this._defLang = languageCode;
		if (typeof this._langs[languageCode] === "undefined") {
			jQuery(this).one("load.i18n", function(e) {
				//if multiple translations are loaded avoid that one may got lost
				e.stopImmediatePropagation();
				//after the event i18n.load happened lets notify all listeners about the fact that a new defLang was set
				jQuery(that).trigger("change.i18n", {"language": languageCode, "i18n": that});
			});
			this.loadLanguage(languageCode, callback);
		} else {
			jQuery(this).trigger("change.i18n", {"language": languageCode, "i18n": this});
		}
		return this;
	}
};
