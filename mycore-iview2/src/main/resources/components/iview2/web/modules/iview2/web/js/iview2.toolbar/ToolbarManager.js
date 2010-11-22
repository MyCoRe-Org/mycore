/**
 * @class
 * @name ToolbarManager
 * @description model of a toolbar buttonset with its functionalities to manage contained buttons
 * @strcuture	
 * 		Object {
 * 			Array:		models				//array of the contained models within the toolbar manager
 * 			Event:		events				//to trigger defined actions, while managing contained models
 *  		AssoArray:	titles				//titles of each button within the whole models of the toolbar manager
 * 		}
 */
var ToolbarManager = function () {
	this.models = [];
	this.events = new iview.Event(this);
	
	this.titles = null;
};

ToolbarManager.prototype = {
	
	/**
	 * @function
	 * @name getModel
	 * @memberOf ToolbarModel#
	 * @description returns a single toolbar model out of the toolbar manager
	 * @param {String} modelId id that identifies a single toolbar model
	 * @return {Object} returns a single toolbar model, identified by its modelId
	 */
	getModel : function(modelId) {
	    for (var i = 0; i < this.models.length; i++) {
			if (this.models[i].id == modelId) {
				return this.models[i];
			}
		}
	},

	/**
	 * @function
	 * @name getModels
	 * @memberOf ToolbarModel#
	 * @description returns an array with each toolbar model out of the toolbar manager
	 * @return {Array} returns an array of all toolbar models, which are registred in the toolbar manager
	 */
	getModels : function() {
		return this.models;
	},

	/**
	 * @function
	 * @name addModel
	 * @memberOf ToolbarModel#
	 * @description adds a single toolbar model to the toolbar manager,
	 *  notify the event-listener (to pass the informations)
	 * @param {Object} model defines the whole toolbar model
	 */
    addModel : function(model) {
    	var myself = this;
    	
    	// pass events from the toolbar model
    	model.events.attach(function (sender, args) {
	    	myself.events.notify(jQuery.extend(args, {'modelId' : model.id}));
	    });
    	
    	this.models[this.models.length] = model;
	    this.events.notify({'type' : 'new', 'modelId' : model.id});
    },
    
	/**
	 * @function
	 * @name destroyModel
	 * @memberOf ToolbarModel#
	 * @description removes a single toolbar model from the toolbar manager,
	 *  notify the event-listener (to pass the informations)
	 * @param {String} id identifies the toolbar model
	 */
    destroyModel : function(id) {
    	for (var i = 0; i < this.models.length; i++) {
    		if (this.models[i].id == id) {
    			this.events.notify({'type' : 'destroy', 'modelId' : this.models[i].id});
    			this.models.splice(i, 1);
    			return;
    		}
    	}
    },
    
	/**
	 * @function
	 * @name setTitles
	 * @memberOf ToolbarModel#
	 * @description sets the titles (after parsing out of the iview2 xsl)
	 * @param {Array} titles titles of each button within the whole models of the toolbar manager
	 */    
    setTitles : function(titles) {
    	this.titles = titles;
    }
};