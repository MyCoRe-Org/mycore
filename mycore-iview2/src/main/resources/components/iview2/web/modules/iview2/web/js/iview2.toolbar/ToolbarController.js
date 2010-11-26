/**
 * @class
 * @name ToolbarController
 * @description main Controller to control the toolbar,
 *  connects the models with their related views,
 *  models are provided by the ToolbarManager,
 *  views will be add directly here and further direct references to them are hold
 * @param {AssoArray} views hold direct references to each added toolbar view
 * @param {AssoArray} relations defines the informations about each connections between model and view
 * @requires fg.menu 3.0 
 */
var ToolbarController = function () {
	this.views = [];
	
	// holds relation between Model and View
	this.relations = {'mainTb' : ['mainTbView'], 'previewTb' : ['previewTbView']};
};

/**
 * @function
 * @name getView
 * @memberOf ToolbarController#
 * @description returns a single view out of the views array
 * @param {String} viewID name that identifies a single view
 * @return {Object} a single view, identified by its viewID
 */
ToolbarController.prototype.getView = function(viewID) {
	return this.views[viewID];
};

/**
 * @function
 * @name addView
 * @memberOf ToolbarController#
 * @description adds an existing view to the ToolbarController,
 *  attach to its events (press, new) and define the actions for each button
 * @param {Object} view View which should be add to the toolbar 
 */
ToolbarController.prototype.addView = function(view) {
	var viewerID = this.getViewer().viewID_to_remove;
	
	// helps the IE7 to maxmimize the viewer also by clicking between the preview buttons
	// TODO: maybe switch to another position	
  	if (view.id == "previewTbView") {
  		jQuery(view.toolbar).click(function() {
  			maximizeHandler(viewerID);
  		});
  	}
	
	var myself = this;
	this.views[view.id] = view;
	
	view.events.attach(function (sender, args) {
    	if (args.type == "press") {
    		if (args.parentName == "zoomHandles") {
    			if (args.elementName == "zoomIn") {
    				// FitToScreen - Button wieder reseten
    				// FitToWidth - Button wieder reseten
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToWidth").setSubtypeState(false);
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToScreen").setSubtypeState(false);
    				
    				zoomViewer(viewerID, true);
    			} else if (args.elementName == "zoomOut") {
    				// FitToScreen - Button wieder reseten
    				// FitToWidth - Button wieder reseten
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToWidth").setSubtypeState(false);
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToScreen").setSubtypeState(false);
    				
    				zoomViewer(viewerID, false);
    			} else if (args.elementName == "fitToWidth") {
    				// FitToScreen - Button wieder reseten
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToWidth").setSubtypeState(true);
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToScreen").setSubtypeState(false);
    				
    				pictureWidth(viewerID);
    			} else if (args.elementName == "fitToScreen") {    				
    				// FitToWidth - Button wieder reseten
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToWidth").setSubtypeState(false);
    				myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton("fitToScreen").setSubtypeState(true);
    				
    				pictureScreen(viewerID);
    			}
    		} else if (args.parentName == "overviewHandles") {
    			if (args.elementName == "openOverview") {
    				if (typeof myself.getViewer().overview === 'undefined') {
    					var oldUi = view.getButtonUi({'button' : args.view}).icons;
    					view.setButtonUi({'button' : args.view, 'icons' : {'primary' : 'loading'}});
    					setTimeout(function() {
    						importOverview(viewerID, function() {view.setButtonUi({'button' : args.view, 'icons' : oldUi});});
    					}, 10);
    				} else {
    					openOverview(viewerID);
    				}
    			} else if (args.elementName == "openChapter") {
					if (typeof myself.getViewer().chapter === 'undefined') {
							var oldUi = view.getButtonUi({'button' : args.view}).icons;
							view.setButtonUi({'button' : args.view, 'icons' : {'primary' : 'loading'}});
							setTimeout(function(){
								importChapter(myself.getViewer(), function() {view.setButtonUi({'button' : args.view, 'icons' : oldUi});});
							}, 10);
					} else {
						openChapter(true, myself.getViewer());
					}
    			}
    		} else if (args.parentName == "navigateHandles" || args.parentName == "previewForward" || args.parentName == "previewBack") {
    			if (args.elementName == "backward") {
    				Iview[viewerID].PhysicalModel.setPrevious();
    			} else if (args.elementName == "forward") {
    				Iview[viewerID].PhysicalModel.setNext();
    			}
    		} else if (args.parentName == "permalinkHandles") {
    			if (args.elementName == "permalink") {
    				myself.getViewer().getPermalinkCtrl().show();
    				//displayURL(this, viewerID);
    			}
    		} else if (args.parentName == "closeHandles") {
    			if (args.elementName == "close") {
    				maximizeHandler(viewerID);
    				myself.getViewer().getToolbarMgr().destroyModel('mainTb');
    			}		
    		}
	    } else if (args.type == "new") {
	    	// this is more than a button
	    	if (args.parentName == "overviewHandles") {
    			if (args.elementName == "openChapter") {
    				if (myself.getViewer().chapter && myself.getViewer().chapter.getActive()) {
    					myself.getViewer().getToolbarMgr().getModel("mainTb").getElement(args.parentName).getButton(args.elementName).setSubtypeState(true);
    				}
    			}
	    	} else if (args.parentName == "navigateHandles") {
	    		if (args.elementName == "pageBox") {
		    		// TODO: maybe do this in the toolbar view?
			    	args.view.addClass("iview2-button-icon-dropdown");
			      	args.view.menu({
			      		// content list to navigate
					    content: myself.getViewer().viewerContainer.find('#pages').html(),
					    width: 50,
					    maxHeight: 280,
					    positionOpts: {
							posX: 'left', 
							posY: 'bottom',
							offsetX: 0,
							offsetY: 0,
							directionH: 'right',
							directionV: 'down', 
							detectH: true, // do horizontal collision detection  
							detectV: false, // do vertical collision detection
							linkToFront: false
					    },
						onChoose: function(item) {
								args.view.button( "option", "label", $(item).text());
								var content = ($(item).text());
								var page = content.substring(content.lastIndexOf('[') + 1, content.lastIndexOf(']'));
								myself.getViewer().PhysicalModel.setPosition(page);
						}
					});
			      	// MainTbView erst nachträglich geladen, Mets zuvor gelesen
			      	if (myself.getViewer().PhysicalModel) {
				      	var initContent = $($('#pages').find('a')[myself.getViewer().PhysicalModel.getCurPos() - 1]).html();
				      	myself.updateDropDown(initContent);
				      	
				      	myself.setState("overviewHandles", "openChapter", true);
				      	myself.setState("overviewHandles", "openOverview", true);
			      	}
	    		}
	    	} else if (args.parentName == "permalinkHandles") {
    			if (args.elementName == "permalink") {
    				if (myself.getViewer().getPermalinkCtrl().getActive()) {
    					// TODO: Model for Permalink
			    		jQuery(view.toolbar).find("."+args.parentName+" .permalink")[0].checked = true;
			    		jQuery(view.toolbar).find("."+args.parentName+" .permalinkLabel").addClass("ui-state-active");
			    	}
    			}
	    	}
	    }
    });
};

/**
 * @function
 * @name catchModelsView
 * @memberOf ToolbarController#
 * @description catches each current toolbar model and
 *  adds listeners for rendering this models to their current defined views,
 *  calls for each toolbar model the function checkNewModel,
 *  should call only ones after instancing the ToolbarController
 */
ToolbarController.prototype.catchModels = function() {
	var myself = this;

	var toolbarMgr = this.getViewer().getToolbarMgr();
	var models = toolbarMgr.getModels();
	
	// attach listeners to ToolbarManager
    toolbarMgr.events.attach(function (sender, args) {
    	
    	if (args.type == "new") {
	    	myself._checkNewModel(toolbarMgr.getModel(args.modelId));
	    } else {
	    	// use the right view for the current model
			var curViewIds = myself.relations[args.modelId];
			
			// process for each view
			for (var i = 0; i < curViewIds.length; i++) {
				var curView = myself.views[curViewIds[i]];
		    	if (args.type == "destroy") {
		    		curView.destroy();
		    		delete(myself.views[myself.relations[args.modelId]]);
		    	} else if (args.type == "add") {
		    		if (args.element) {
				    	var element = args.element;
				    	if (element.type == "buttonset") {
				   			curView.addButtonset({'elementName' : element.elementName, 'index' : element.index});
				    	} else if (element.type == "divider") {
				    		curView.addDivider({'elementName' : element.elementName, 'index' : element.index});
				    	} else if (element.type == "text") {
				    		curView.addText({'elementName' : element.elementName, 'text' : element.text, 'index' : element.index});
				    	}
			    	} else if (args.button) {
			    		var button = args.button;
			    		if (button.type == 'button') {
			    			curView.addButton({'elementName' : button.elementName, 'ui' : button.ui, 'index' : button.index, 'title' : button.title, 'subtype' : button.subtype, 'parentName' : button.relatedButtonset.elementName, 'active' : button.active});
			    		}
			    	}
		    	} else if (args.type == "del") {
		    		if (args.element) {
				    	var element = args.element;
				    	if (element.type == "buttonset" || element.type == "divider") {
				    		curView.removeToolbarElement({'elementName' : element.elementName});
				    	}
		    		} else if (args.button) {
		    			var button = args.button;
				    	curView.removeButton({'elementName' : button.elementName, 'parentName' : button.relatedButtonset.elementName});
				    }
		    	} else if (args.type == "changeState") {
		    		// TODO: maybe it will be fine to move this to the view
		    		if (args.elementName == "zoomHandles") {
			    		if (args.buttonName == "fitToWidth") {
			    			if (args.state == false) {
			    				jQuery(curView.toolbar).find("."+args.elementName+" .fitToWidth")[0].checked = false;
			    				jQuery(curView.toolbar).find("."+args.elementName+" .fitToWidthLabel").removeClass("ui-state-active");
			    			}
			    			// the view provides the TRUE functionality on its own (by click)
			    		} else if (args.buttonName == "fitToScreen") {
			    			if (args.state == false) {
			    				jQuery(curView.toolbar).find("."+args.elementName+" .fitToScreen")[0].checked = false;
			    				jQuery(curView.toolbar).find("."+args.elementName+" .fitToScreenLabel").removeClass("ui-state-active");
			    			}
			    			// the view provides the TRUE functionality on its own (by click)
			    		}
		    		} else if (args.elementName == "overviewHandles") {
		    			if (args.buttonName == "openChapter") {
		    				if (args.state == true) {
		    					jQuery(curView.toolbar).find("."+args.elementName+" .openChapter")[0].checked = true;
		    					jQuery(curView.toolbar).find("."+args.elementName+" .openChapterLabel").addClass("ui-state-active");
				    		}
		    				// the other case won't be used
		    			}
		    		}
    			} else if (args.type == "changeActive") {
    				curView.setButtonUi({'button' : '.'+args.elementName+' .'+args.buttonName, 'disabled' : !args.active});
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
 * @name cheackNewModel
 * @memberOf ToolbarController#
 * @description checks each element of the given toolbar model and
 *  notify its corresponding add listener
 * @param {Object} model defines the toolbar model which will be check
 */
ToolbarController.prototype._checkNewModel = function(model) {
	
	// doesn't make sense
	/*
	// use the right view for the current model
	var curViewIds = this.relations[model.id];
	var curViews = [];
	
	for (var j = 0; j < curViewIds.length; j++) {
		curViews.push(this.views[curViewIds[j]]);
	}
	*/
	
	// checks predefined models
	for (var i = 0; i < model.elements.length; i++) {
		
		// notify views for ModelProvider elements
		model.events.notify({'type' : "add", 'element' : model.elements[i]});
		if (model.elements[i].type == "buttonset") {

			// notify views for buttons
			for (var k = 0; k < model.elements[i].buttons.length; k++) {
				model.elements[i].events.notify({'type' : "add", 'button' :model.elements[i].buttons[k]});
			}
		}
	}
};

/**
 * @function
 * @name checkNavigation
 * @memberOf ToolbarController#
 * @description checks the navigation buttons (forward and backward) and
 *  deactivate them if there isn't a page in direction
 * @param {integer} pNum defines the page number of the current shown page
 */
ToolbarController.prototype.checkNavigation = function(pNum) {
	var tooHigh = (pNum >= this.getViewer().amountPages)? true : false;
	var tooLow = (pNum <= 1)? true : false;
	
	var models = this.getViewer().getToolbarMgr().getModels();

	//for (var i = 0; i < models.length; i++) {
	//	if (models[i].id == "previewTb") {
			this.setState('previewBack', 'backward', !tooLow);
			this.setState('previewForward', 'forward', !tooHigh);
	//	} else if (models[i].id == "mainTb") {
			this.setState('navigateHandles', 'backward', !tooLow);
			this.setState('navigateHandles', 'forward', !tooHigh);
	//	}
	//}
};

/**
 * @function
 * @name checkZoom
 * @memberOf ToolbarController#
 * @description checks the zoom buttons (zoomIn and zoomOut) and
 *  deactivate them if there isn't a possibility to zoom again in their direction,
 *  zoom is only possible between level 0 and zoomMax
 * @param {integer} zoom defines the current zoom Level of the shown content
 */
ToolbarController.prototype.checkZoom = function(zoom) {
	var zoomIn = (zoom == this.getViewer().zoomMax)? false : true;
	var zoomOut = (zoom == 0)? false : true;
	
	var models = this.getViewer().getToolbarMgr().getModels();

	this.setState('zoomHandles', 'zoomIn', zoomIn);
	this.setState('zoomHandles', 'zoomOut', zoomOut);
};

/**
 * @function
 * @name updateDropDown
 * @memberOf ToolbarController#
 * @description updates the current content of the drop-down-menue button
 * @param {String} content defines the current shown content which is choose in the drop-down-box
 */
ToolbarController.prototype.updateDropDown = function(content) {
	// TODO: sollte eventuell nochmal überdacht werden (vieleicht direkter Wechsel auf Seite)
	$('.navigateHandles .pageBox').button('option', 'label', content);
};

/**
 * @function
 * @name setState
 * @memberOf ToolbarController#
 * @description activate or deactivate Buttons,
 *  for e.g. forward or backward buttons,
 *  it has nothing to do with activate and deactivate of checkbuttons
 * @param {String} handle defines the buttonset of the button
 * @param {String} button defines the name of the button
 * @param {boolean} state defines the target state of the button
 */
ToolbarController.prototype.setState = function(handle, button, state) {
	var models = this.getViewer().getToolbarMgr().getModels();
	
	for (var i = 0; i < models.length; i++) {
		if (models[i].getElement(handle) && models[i].getElement(handle).getButton(button)) {
			models[i].getElement(handle).getButton(button).setActive(state);
		}
	}
};