/**
 * @class
 * @constructor
 * @name		ToolbarController
 * @description main Controller to control the toolbar,
 *  connects the models with their related views,
 *  models are provided by the ToolbarManager,
 *  views will be add directly here and further direct references to them are hold
 * @param		{Object} parent holds the reference to the viewer
 * @param		{AssoArray} views hold direct references to each added toolbar view
 * @param		{AssoArray} relations defines the informations about each connections between model and view
 * @requires	fg.menu 3.0 
 */
var ToolbarController =(function() {
  function constructor(parent) {
  	this.parent = parent;
  	this.views = [];
    iview.IViewObject.call(this, parent);
    this.toolbarContainer = jQuery('<div class="toolbars" onmousedown="return false;" />').appendTo(parent.viewerContainer);
  
  	// holds relation between Model and View
  	this.relations = {'mainTb' : ['mainTbView'], 'previewTb' : ['previewTbView']};
  };
  constructor.prototype = Object.create(iview.IViewObject.prototype);
  return constructor;
})();

/**
 * @public
 * @function
 * @name		addView
 * @memberOf	ToolbarController#
 * @description adds an existing view to the ToolbarController,
 *  attach to its events (press, new) and define the actions for each button
 * @param		{Object} view View which should be add to the toolbar 
 */
ToolbarController.prototype.addView = function(view) {
  var that = this;
	// helps the IE7 to maxmimize the viewer also by clicking between the preview buttons
	// TODO: maybe switch to another position	
  	if (view.id == "previewTbView") {
  		jQuery(view.toolbar).click(function() {
  		  that.getViewer().toggleViewerMode();
  		});
  	}
	
	this.views[view.id] = view;
	
	jQuery(view).bind("press", function(e, args) {
    if (args.parentName == "zoomHandles") {
      if (args.elementName == "zoomIn") {
        that.getViewer().viewerBean.zoomViewer(true);
      } else if (args.elementName == "zoomOut") {
        that.getViewer().viewerBean.zoomViewer();
      } else if (args.elementName == "fitToWidth") {
        that.getViewer().viewerBean.pictureWidth();
      } else if (args.elementName == "fitToScreen") {
        that.getViewer().viewerBean.pictureScreen();
      }
    } else if (args.parentName == "overviewHandles") {
      var button = new Object;
      button.setLoading = function(loading) {
        that.perform("setLoading", loading, args.parentName, args.elementName);
      };
      button.setSubtypeState = function(state) {
        that.perform("setSubtypeState", state, args.parentName, args.elementName);
      };
      if (args.elementName == "openThumbnailPanel" && that.getViewer().ThumbnailPanel != null) {
    	  that.getViewer().ThumbnailPanel.toggleView(button);
    	  that.getViewer().ThumbnailPanel.setSelected(that.getViewer().PhysicalModel._curPos);
      } else if (args.elementName == "openChapter") {
        iview.chapter.openChapter(that.getViewer(), button);
      }
    } else if (args.parentName == "navigateHandles" || args.parentName == "previewForward" || args.parentName == "previewBack") {
      if (args.elementName == "backward") {
        that.getViewer().PhysicalModel.setPrevious();
      } else if (args.elementName == "forward") {
        that.getViewer().PhysicalModel.setNext();
      }
    } else if (args.parentName == "permalinkHandles") {
      if (args.elementName == "permalink") {
        var button = new Object;
        button.setLoading = function(loading) {
          that.perform("setLoading", loading, args.parentName, args.elementName);
        };
        iview.Permalink.openPermalink(that.getViewer(), button);
      }
    } else if (args.parentName == "urnHandles") {
        if (args.elementName == "urn") {
          that.getViewer().urn.urnButtonClicked();
        }
    } else if (args.parentName == "pdfHandles") {
      if (args.elementName == "createPdf") {
        that.getViewer().openPdfCreator();
      }
    } else if (args.parentName == "closeHandles") {
      if (args.elementName == "close") {
      	if(that.getViewer().properties.killInstance !== "undefined" && that.getViewer().properties.killInstance == "true"){
    		that.getViewer().context.switchContext();
    		jQuery(".viewerContainer").remove();
    		iview.removeInstance(that.getViewer());
    		return;
        }
        if (URL.getParam("jumpback") == "true") {
          history.back();
          return;
        } 
        that.getViewer().toggleViewerMode();
      }
    }
  }).bind("new", function(e, args) {
    // this is more than a button
    if (args.parentName == "overviewHandles") {
      if (args.elementName == "openChapter") {
        if (that.getViewer().chapter.loaded && that.getViewer().chapter.getActive()) {
          that.perform("setSubtypeState", true, args.parentName, args.elementName);
        }
      }
    } else if (args.parentName == "navigateHandles") {
      if (args.elementName == "pageBox") {
        // TODO: maybe do this in the toolbar view?
        args.view.addClass("iview2-button-icon-dropdown");
        args.view.fgmenu({
          // content list to navigate
          content : that.getViewer().context.container.find('#pages').html(),
          /* width: 100, */
          maxHeight : 280,
          positionOpts : {
            posX : 'left',
            posY : 'bottom',
            offsetX : 0,
            offsetY : 0,
            directionH : 'right',
            directionV : 'down',
            detectH : true, // do horizontal collision detection
            detectV : false, // do vertical collision detection
            linkToFront : false
          },
          onChoose : function(item) {
            args.view.button("option", "label", jQuery(item).text());
            var content = (jQuery(item).text());
            var page = content.substring(content.lastIndexOf('[') +1, content.lastIndexOf(']'));
            that.getViewer().PhysicalModel.setPosition(page);
          }
        });
        // MainTbView erst nachträglich geladen, Mets zuvor gelesen
        if (that.getViewer().PhysicalModel) {
          var initContent = jQuery(jQuery('#pages').find("a:contains('[" + that.getViewer().PhysicalModel.getCurPos()+"]')")).html();
          that.updateDropDown(initContent);

          // chapter and ThumbnailPanel need to wait for METS informations
          that.perform("setActive", true, 'overviewHandles', 'openChapter');
          that.perform("setActive", true, 'overviewHandles', 'openThumbnailPanel');
        }
      }
    } else if (args.parentName == "permalinkHandles") {
      if (args.elementName == "permalink") {
        if (typeof that.getViewer().getPermalinkCtrl !== "undefined" && that.getViewer().getPermalinkCtrl().getActive()) {
          // TODO: Model for Permalink
          that.perform("setActive", true, 'permalinkHandles', 'permalink');
        }
      }
    }
  });
};

/**
 * @public
 * @function
 * @name		catchModelsView
 * @memberOf	ToolbarController#
 * @description	catches each current toolbar model and
 *  adds listeners for rendering this models to their current defined views,
 *  calls for each toolbar model the function checkNewModel,
 *  should call only ones after instancing the ToolbarController
 * @throws		{toolbarloaded} event on document Node after new Model is added to Controller
 */
ToolbarController.prototype.catchModels = function() {
	var that = this;
	var toolbarMgr = this.getViewer().toolbar.mgr;
	var models = toolbarMgr.models;
	
	// attach listeners to ToolbarManager
    jQuery(toolbarMgr).bind("changeState changeLoading changeActive add del destroy new", function (e, args) {
    	if (e.type == "new") {
	    	that._checkNewModel(toolbarMgr.getModel(args.modelId));
	    } else {
	    	// use the right view for the current model
			var curViewIds = that.relations[args.modelId];
			
			// process for each view
			for (var i = 0; i < curViewIds.length; i++) {
				var curView = that.views[curViewIds[i]];
				switch (e.type) {
				case "destroy":
		    		curView.destroy();
		    		delete(that.views[that.relations[args.modelId]]);
		    	break;
				case "add"://TODO new elements have to be add able without any special treating here
		    		if (args.element) {
				    	var element = args.element;
				    	switch (element.type) {
				    	case "buttonset":
				   			curView.addButtonset({'elementName' : element.elementName, 'index' : element.index});
				   		break;
				    	case "divider":
				    		curView.addDivider({'elementName' : element.elementName, 'index' : element.index});
				    	break;
				    	case "text":
				    		curView.addText({'elementName' : element.elementName, 'text' : element.text, 'index' : element.index});
				    	break;
				    	case "spring":
				    		curView.addSpring({'elementName' : element.elementName, 'weight' : element.weight, 'index' : element.index});
				    	break;
				    	case "image":
				    		curView.addImage({'elementName' : element.elementName, 'src' : element.src, 'index' : element.index});
			    		break;
				    	}
			    	} else if (args.button) {
			    		var button = args.button;
			    		if (button.type == 'button') {
			    			curView.addButton({'elementName' : button.elementName, 'ui' : button.ui, 'index' : button.index, 'captionId' : button.captionId, 'subtype' : button.subtype, 'parentName' : button.relatedButtonset.elementName, 'active' : button.active});
			    		}
			    	}
		    	break;
				case "del":
		    		if (args.element) {
				    	var element = args.element;
				    	if (element.type == "buttonset" || element.type == "divider") {
				    		curView.removeToolbarElement({'elementName' : element.elementName});
				    	}
		    		} else if (args.button) {
		    			var button = args.button;
				    	curView.removeButton({'elementName' : button.elementName, 'parentName' : button.relatedButtonset.elementName});
				    }
		    	break;
				case "changeState":
		    		// TODO: maybe it will be fine to move this to the view
					if ((args.elementName == "zoomHandles" && (args.buttonName == "fitToWidth" || args.buttonName == "fitToScreen"))
					|| (args.elementName == "overviewHandles" && (args.buttonName == "openChapter" || args.buttonName == "openThumbnailPanel"))) {
						// the view provides the TRUE functionality on its own (by click)
						// the other case won't be used
						if (typeof curView != "undefined") {
							jQuery(curView.toolbar).find("."+args.elementName+" ." + args.buttonName)[0].checked = args.state;
							if (args.state) {
								jQuery(curView.toolbar).find("."+args.elementName+" ." + args.buttonName + "Label").addClass("ui-state-active");
							} else {
								jQuery(curView.toolbar).find("."+args.elementName+" ." + args.buttonName + "Label").removeClass("ui-state-active");
							}
						}
					}
		    	break;
				case "changeActive":
    				curView.setButtonUi({'button' : '.'+args.elementName+' .'+args.buttonName, 'disabled' : !args.active});
    			break;
				case "changeLoading":
    				if (args.loading) {
    					curView.setButtonUi({'button' : '.'+args.elementName+' .'+args.buttonName, 'icons' : {'primary' : 'loading'}});
    				} else {
    					var ui = toolbarMgr.getModel(args.modelId).getElement(args.elementName).getButton(args.buttonName).ui;
    					curView.setButtonUi({'button' : '.'+args.elementName+' .'+args.buttonName, 'icons' : ui.icons});
    				}
    			break;
    			}
			}
	    }
    });
    
    // notify listeners for each predefined Model-Element
    for (var i = 0; i < models.length; i++) {
    	this._checkNewModel(models[i]);
	}
};

/**
 * @private
 * @function
 * @name		cheackNewModel
 * @memberOf	ToolbarController#
 * @description	checks each element of the given toolbar model and
 *  notify its corresponding add listener
 * @param		{Object} model defines the toolbar model which will be check
 * @param		useIndexes describes whether defined element and button indexes will be used or not
 * @throws		{toolbarloaded} event on document Node after new Model is added to Controller
 */
ToolbarController.prototype._checkNewModel = function(model) {
	
	// checks predefined models
	for (var i = 0; i < model.elements.length; i++) {
		
		// notify views for ModelProvider elements
		jQuery(model).trigger("add", {'element' : model.elements[i]});
		if (model.elements[i].type == "buttonset") {

			// notify views for buttons
			for (var k = 0; k < model.elements[i].buttons.length; k++) {
				jQuery(model.elements[i]).trigger("add", {'button' :model.elements[i].buttons[k]});
			}
		}
	}
	
	//Send notification that new Model was added
	var loadEvent = jQuery.Event("toolbarloaded");
	loadEvent.viewer = this.getViewer();
	loadEvent.model = model;
	//TODO: optimize this method invocation 
	loadEvent.getViews = function(){
		var ctrl = this.viewer.toolbar.ctrl;
		var views = [];
		var relView = ctrl.relations[this.model.id];
		for (var i = 0; i < relView.length; i++){
			views.push(ctrl.views[relView[i]]);
		}
		return views;
	};
	jQuery(document).trigger(loadEvent);
};

/**
 * @public
 * @function
 * @name		checkNavigation
 * @memberOf	ToolbarController#
 * @description checks the navigation buttons (forward and backward) and
 *  deactivate them if there isn't a page in direction
 * @param		{iview.METS.PhysicalModel} which allows it to check if the buttons are clickable or not
 */
ToolbarController.prototype.checkNavigation = function(model) {
	var tooHigh =!(model.hasNext());
	var tooLow = !(model.hasPrevious());
	
	this.perform("setActive", !tooLow, 'previewBack', 'backward');
	this.perform("setActive", !tooHigh, 'previewForward', 'forward');
	this.perform("setActive", !tooLow, 'navigateHandles', 'backward');
	this.perform("setActive", !tooHigh, 'navigateHandles', 'forward');
};

/**
 * @public
 * @function
 * @name		checkZoom
 * @memberOf	ToolbarController#
 * @description checks the zoom buttons (zoomIn and zoomOut) and
 *  deactivate them if there isn't a possibility to zoom again in their direction,
 *  zoom is only possible between level 0 and zoomMax
 * @param		{integer} zoom defines the current zoom Level of the shown content
 */
ToolbarController.prototype.checkZoom = function(zoom) {
	var viewer = this.getViewer();
	var preload = viewer.preload;
	var zoomIn = (viewer.currentImage.width <= preload.width() && viewer.currentImage.height <= preload.height())? false : true;
	var zoomOut = (zoom == 0)? false : true;
	
	this.perform("setActive", zoomIn, 'zoomHandles', 'zoomIn');
	this.perform("setActive", zoomOut, 'zoomHandles', 'zoomOut');
};

/**
 * @public
 * @function
 * @name		updateDropDown
 * @memberOf	ToolbarController#
 * @description updates the current content of the drop-down-menue button
 * @param		{String} content defines the current shown content which is choose in the drop-down-box
 */
ToolbarController.prototype.updateDropDown = function(content) {
	// TODO: sollte eventuell nochmal überdacht werden (vieleicht direkter Wechsel auf Seite)
	jQuery('.navigateHandles .pageBox').button('option', 'label', content);
};

/**
 * @public
 * @function
 * @name		perform
 * @memberOf	ToolbarController#
 * @description performs an special action for a single button wihtin a buttonset,
 *  setActive : activate or deactivate Buttons (for e.g. forward or backward buttons),
 *              it has nothing to do with activate and deactivate of checkbuttons
 *  setSubtypeState : check or uncheck checkButtons,
 *  setLoading : activate or deactivate loading icon of Buttons (for e.g. spinning arrows)
 * @param		{String} action defines the action (setActive, setSubtypeState, setLoading)
 * @param		{String} argument defines the argumentfor the special action of the button
 * @param		{String} buttonset defines the buttonset of the button
 * @param		{String} button defines the name of the button
 */
ToolbarController.prototype.perform = function(action, argument, buttonset, button) {
	var models = this.getViewer().toolbar.mgr.models;

	for (var i = 0; i < models.length; i++) {
		if (models[i].getElement(buttonset) && models[i].getElement(buttonset).getButton(button)) {
			eval("models[i].getElement('" + buttonset + "').getButton('" + button + "')."+action+"("+argument+")");
		}
	}
};

/**
 * @public
 * @function
 * @name		paint
 * @memberOf	ToolbarController#
 * @description	repaint all Views of the given Model, as some Components are not able to
 *  be browser automatic refreshed
 * @param		{string} name under which the Model is registered at this controller
 */
ToolbarController.prototype.paint = function(model) {
	try {
		for (var view in this.relations[model]) {
		  if (typeof this.views[this.relations[model][view]]!= "undefined")
		    this.views[this.relations[model][view]].paint();
		}
	} catch (e) {
		log(e);
	}
};