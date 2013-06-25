define([
	"dojo/_base/declare", // declare
	"dojo/_base/array"
], function(declare, arrayUtil) {
return declare("mycore.classification._SettingsMixin", null, {

	settings: null,

    constructor: function(/*Object*/ args) {
    	declare.safeMixin(this, args);
    },

    create: function(args) {
    	this.inherited(arguments);
    },

    _setSettingsAttr: function(settings) {
    	this.settings = settings;
    	this._updateSettings(settings);
    },

    _updateSettings: function(settings) {
    	for(var item in this) {
    		if(this[item] != null && typeof this[item] === 'object' && this[item].isInstanceOf != null && this[item].isInstanceOf(mycore.classification._SettingsMixin)) {
    			this[item].set("settings", settings);
    			this[item].onSettingsReady();
    		}
    	}
    },

    onSettingsReady: function() {
    	// overwrite this
    }

});
});