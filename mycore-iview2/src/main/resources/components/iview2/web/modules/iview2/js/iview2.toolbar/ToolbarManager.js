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

/**
 * @description	creates the Toolbars and stores them within the supplied structure
 * @param viewer {Iview} instance for which it shall create the toolbars
 * @return
 */
var createToolbars = function(viewer) {
	jQuery(viewer.viewerContainer)
	//exploit that the init.viewer event bubbles up the DOM hierarchy
	.bind("init.viewer", function() {
		viewer.toolbar = {};
		viewer.toolbar.mgr = new ToolbarManager();
		viewer.toolbar.ctrl = new ToolbarController(viewer);
		// entweder Mgr macht alles und Übergabe des related... (Modelprovider) oder Models kümmern sich untereinander und schöne Form
		// (siehe unten)
		// vom Drop Down Menu nur die View oder auch ein Model im ToolbarManager?
		
		// Toolbar Manager
		viewer.toolbar.mgr.addModel(new PreviewToolbarModelProvider("previewTb").getModel());
		// Toolbar Controller
		viewer.toolbar.ctrl.addView(new ToolbarView("previewTbView", viewer.toolbar.ctrl.toolbarContainer, i18n));
		
		// holt alle bisherigen Models in den Controller und setzt diese entsprechend um
		viewer.toolbar.ctrl.catchModels();
		viewer.properties.initialized = true;
	})
	.bind("maximize.viewerContainer", function() {
  		viewer.toolbar.ctrl.addView(new ToolbarView("mainTbView", viewer.toolbar.ctrl.toolbarContainer, i18n));
		viewer.toolbar.mgr.addModel(new StandardToolbarModelProvider("mainTb", viewer).getModel());
		if (viewer.PhysicalModel) {
			viewer.toolbar.ctrl.checkNavigation(viewer.PhysicalModel.getCurPos());
		}
		viewer.toolbar.ctrl.paint("mainTb");
		if (viewer.currentImage.zoomInfo.zoomWidth) {
			/*TODO rebuild so that setActive of the corresponding Buttons is called, so the view can take care of the display part
		needs rewriting of some parts within ToolbarController and View
			 */
			jQuery(".mainTbView .zoomHandles .fitToWidth")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToWidthLabel").addClass("ui-state-active");
		} else if (viewer.currentImage.zoomInfo.zoomScreen) {
			jQuery(".mainTbView .zoomHandles .fitToScreen")[0].checked = true;
			jQuery(".mainTbView .zoomHandles .fitToScreenLabel").addClass("ui-state-active");
		}
  })
  	.bind("minimize.viewerContainer", function() {
  		viewer.toolbar.mgr.destroyModel('mainTb');
  })
  	.bind("reinit.viewer", function() {
  		viewer.toolbar.ctrl.paint("mainTb");
  })
  	.bind("zoom.viewer", function() {
  		viewer.toolbar.ctrl.checkZoom(viewer.viewerBean.zoomLevel);
  })
}