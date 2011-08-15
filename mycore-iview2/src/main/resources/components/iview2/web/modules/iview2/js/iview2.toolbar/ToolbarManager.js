/**
 * @class
 * @constructor
 * @name		ToolbarManager
 * @description model of a toolbar buttonset with its functionalities to manage contained buttons
 * @structure	
 * 		Object {
 * 			Array:		models				//array of the contained models within the toolbar manager
 * 			Event:		events				//to trigger defined actions, while managing contained models
 * 			AssoArray:	changes				//holds all durable changes for each toolbar model, to perform them after creating new toolbar model instances
 * 		}
 */
var ToolbarManager = function () {
	this.models = [];
	this.changes = [];
};

ToolbarManager.prototype = {
	
	/**
	 * @public
	 * @function
	 * @name		getModel
	 * @memberOf	ToolbarModel#
	 * @description returns a single toolbar model out of the toolbar manager
	 * @param		{String} modelId id that identifies a single toolbar model
	 * @return		{Object} returns a single toolbar model, identified by its modelId
	 */
	getModel : function(modelId) {
	    for (var i = 0; i < this.models.length; i++) {
			if (this.models[i].id == modelId) {
				return this.models[i];
			}
		}
	},

	/**
	 * @public
	 * @function
	 * @name		addModel
	 * @memberOf	ToolbarManager#
	 * @description adds a single toolbar model to the toolbar manager,
	 *  notify the event-listener (to pass the informations)
	 * @param 		{Object} model defines the whole toolbar model
	 */
    addModel : function(model) {
    	var that = this;
    	
    	// pass events from the toolbar model
    	jQuery(model).bind("changeState changeActive changeLoading add del", function (e, val) {
	    	jQuery(that).trigger(e.type, jQuery.extend(val, {'modelId' : model.id}));
	    });
    	
    	this.models[this.models.length] = model;
	    jQuery(this).trigger('new', {'modelId' : model.id});
	    
    	// are there any durable changes to perform
    	if (this.changes[model.id]) {
    		eval(this.changes[model.id]);
    	}
    },
    
	/**
	 * @public
	 * @function
	 * @name		destroyModel
	 * @memberOf	ToolbarManager#
	 * @description removes a single toolbar model from the toolbar manager,
	 *  notify the event-listener (to pass the informations)
	 * @param		{String} id identifies the toolbar model
	 */
    destroyModel : function(id) {
    	for (var i = 0; i < this.models.length; i++) {
    		if (this.models[i].id == id) {
    			jQuery(this).trigger('destroy', {'modelId' : this.models[i].id});
    			this.models.splice(i, 1);
    			return;
    		}
    	}
    },
    
	/**
	 * @public
	 * @function
	 * @name		change
	 * @memberOf 	ToolbarManager#
	 * @description defines durable changes to a special model,
	 *  so these changes will perform after each instancing such a model
	 * @param		{String} modeId defines the id of a special model
	 * @param		{String} operation defines the change operation which should be performed after instancing
	 */   
    change : function(modelId, operation) {
    	if (!this.changes[modelId]) {
    		this.changes[modelId] = 'model.' + operation;
    	} else {
    		this.changes[modelId] = this.changes[modelId] + 'model.' + operation;
    	}
    }
};