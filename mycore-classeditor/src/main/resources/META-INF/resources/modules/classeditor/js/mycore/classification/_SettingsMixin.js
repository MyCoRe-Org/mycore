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
