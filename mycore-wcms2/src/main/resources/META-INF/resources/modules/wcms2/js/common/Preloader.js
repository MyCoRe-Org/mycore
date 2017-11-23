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

/*
 * @package wcms.common
 */
var wcms = wcms || {};
wcms.common = wcms.common || {};

/**
 * Use this class to preload javascript objects. To add an object call
 * this.preloader.preloadList.push(myobject);. Its important that each
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
wcms.common.Preloader = function() {
	this.preloadList = [];
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function preload() {
		var size = this.preloadList.length;
		this.eventHandler.notify({"type" : "started", "size" : size});

		// calculate weight
		var totalWeight = 0;
		for(var i = 0; i < size; i++) {
			var preloadableObject = this.preloadList[i];
			if(!preloadableObject.getPreloadWeight) {
				console.log("Warning: no preload weight defined for:" );
				console.log(preloadableObject);
				continue;
			}
			totalWeight +=  preloadableObject.getPreloadWeight();
		}

		// preload
		var currentWeight = 0;
		for(var i = 0; i < size; i++) {
			var preloadableObject = this.preloadList[i];
			var loadingName = undefined;
			if(preloadableObject.getPreloadName) {
				loadingName = preloadableObject.getPreloadName();
			} else {
				loadingName = "undefined";
				console.log("Warning: no preload name defined for:");
				console.log(preloadableObject);
			}
			this.eventHandler.notify({
				"type" : "preloadObject",
				"name" : loadingName
			});
			if(preloadableObject.preload) {
				preloadableObject.preload();
			} else {
				console.log("Error: Object is not preloadable:");
				console.log(preloadableObject);
				continue;
			}
			if(preloadableObject.getPreloadWeight)
				currentWeight += preloadableObject.getPreloadWeight();
			this.eventHandler.notify({
				"type" : "preloadObjectFinished",
				"name" : loadingName,
				"progress" : ((currentWeight / totalWeight) * 100)
			});
		}
		this.eventHandler.notify({"type" : "finished"});
	}

	wcms.common.Preloader.prototype.preload = preload;
	
})();
