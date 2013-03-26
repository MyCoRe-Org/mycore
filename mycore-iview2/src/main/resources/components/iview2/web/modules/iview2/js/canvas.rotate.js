(function() {
    "use strict";
    // TODO : fix rotation position bug 
    function cv_getRotateCurrentWidth() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curWidth : currentImage.curHeight);
    }

    function cv_getRotateCurrentHeight() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curHeight : currentImage.curWidth);
    }
    
    function cv_getOrigWidth() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curHeight : currentImage.curWidth);
    }

    function cv_getOrigHeight() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curWidth : currentImage.curHeight);
    }

    function cv_getCurrentRotatedCanvasWidth() {
        return Math
                .floor((this.getViewer().currentImage.rotation == 0 || this.getViewer().currentImage.rotation == 180) ? this.context2D.canvas.width
                        : this.context2D.canvas.height);
    }

    function cv_getCurrentRotatedCanvasHeight() {
        return Math
                .floor((this.getViewer().currentImage.rotation == 0 || this.getViewer().currentImage.rotation == 180) ? this.context2D.canvas.height
                        : this.context2D.canvas.width);
    }
    
    function cv_getMaxDimViewer (screen){
    	if( this.iview.currentImage.rotation == 0 ||  this.iview.currentImage.rotation == 180){
    		return (screen) ? this.width : this.height ;
    	} else {
    		return (screen) ?  this.width : this.height;
    	}
	 }
    

	function cv_getTileSizeMinZoomLevel(screen) {
		if ( this.iview.currentImage.rotation == 0 ||  this.iview.currentImage.rotation == 180) {
			return (screen) ? this.iview.currentImage.zoomInfo.dimensions[0].width
					: this.iview.currentImage.zoomInfo.dimensions[0].height;
		} else {
			return (!screen) ? this.iview.currentImage.zoomInfo.dimensions[0].width
					: this.iview.currentImage.zoomInfo.dimensions[0].height;
		}
	}
	
	
  

	function cv_getMaxDimCurZoomLevel(screen, calculatedMinFitZoomLevel) {
		if (this.iview.currentImage.rotation == 0
				|| this.iview.currentImage.rotation == 180) {
			return (screen) ? this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].width
					: this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].height;
		} else {
			return (!screen) ? this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].width
					: this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].height;
		}
	}
    
    function cv_isXBorderTile(tileX) {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var tileSize = viewerBean.tileSize;
        var rotation = currentImage.rotation;
        if(rotation==90 || rotation==270)
            return Math.floor(currentImage.curHeight / tileSize) == tileX;
        return Math.floor(currentImage.curWidth / tileSize) == tileX;
    };

    function cv_isYBorderTile(tileY) {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var tileSize = viewerBean.tileSize;
        var rotation = currentImage.rotation;
        if(rotation==90 || rotation==270)
            return Math.floor(currentImage.curWidth / tileSize) == tileY;
        return Math.floor(currentImage.curHeight / tileSize) == tileY;     
    };
    
    function cv_calculateMoveVector() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;
        var posX = viewerBean.x;
        var posY = viewerBean.y;

        if(rotation == 90 || rotation == 270){
            return {
                "xOff" : posY - this.lastPosY,
                "yOff" : posX - this.lastPosX
            };
        }
        
        return {
            "xOff" : posX - this.lastPosX,
            "yOff" : posY - this.lastPosY
        };
    }
    
    function cv_bothAxisBigger() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        return !!((currentImage.curWidth > viewerBean.width && currentImage.curHeight > viewerBean.height) || (currentImage.curHeight > viewerBean.width && currentImage.curWidth > viewerBean.height));
    }


    function cv_getPreviewX() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;

        switch (rotation) {
        case 0:
            return -viewerBean.x;
        case 90:
            return -viewerBean.y;
        case 180:
            return (currentImage.curWidth > viewerBean.width) ? -(currentImage.curWidth + (-viewerBean.width - viewerBean.x)) : 0;
        case 270:
            return (currentImage.curHeight > viewerBean.height) ? -(currentImage.curHeight + (-viewerBean.height - viewerBean.y)) : 0;
        }
    }

    function cv_getPreviewY() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;

        switch (rotation) {
        case 0:
            return -viewerBean.y;
        case 90:
            return (currentImage.curWidth > viewerBean.width) ? -(currentImage.curWidth + (-viewerBean.width - viewerBean.x)) : 0;
        case 180:
            return (currentImage.curHeight > viewerBean.height) ? -(currentImage.curHeight + (-viewerBean.height - viewerBean.y)) : 0;
        case 270:
            return -viewerBean.x;// + areaEndY;
        }
    }
    
       function cv_calculateMoveVector() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;
        var posX = viewerBean.x;
        var posY = viewerBean.y;

        if(rotation == 90 || rotation == 270){
            return {
                "xOff" : posY - this.lastPosY,
                "yOff" : posX - this.lastPosX
            };
        }
        
        return {
            "xOff" : posX - this.lastPosX,
            "yOff" : posY - this.lastPosY
        };
    }
    
    function cv_getMoveAxis(degree) {
        var currentImage = this.getViewer().currentImage;
        var viewerBean = this.getViewer().viewerBean;
        var moveXAxis = 0, moveYAxis = 0;
        if (!this.bothAxisBigger())//kleiner, Zwischenstufe, fit-to-width
        { //!!!prevent that the viewer moves to much (e.g. y is much bigger)
            switch (degree) {
            case 90:
                moveYAxis = -Math.min(currentImage.curWidth, viewerBean.width);//Höhe des Bildes oder Breite des Viewers
                break;
            case 180:
                moveYAxis = -Math.min(currentImage.curHeight, viewerBean.height);
                moveXAxis = -Math.min(currentImage.curWidth, viewerBean.width);
                break;
            case 270:
                moveXAxis = -Math.min(currentImage.curHeight, viewerBean.height);//Breite des Bildes oder Höhe des Viewers                                   
                break;
            }

        } else {//größer                       
            switch (degree) {
            case 90:
                moveYAxis = -Math.min(viewerBean.width, currentImage.curWidth);//maximale Verschiebung um Breite, auch wenn mehr möglich wäre
                break;
            case 180:
                moveYAxis = -viewerBean.height;
                moveXAxis = -viewerBean.width;
                break;
            case 270:
                moveXAxis = -viewerBean.height;
                break;
            }
        }
        return {
            "x" : moveXAxis,
            "y" : moveYAxis
        };
    }

    function cv_rotateCanvas(degree) {
        var currentImage = this.getViewer().currentImage;
        var viewerBean = this.getViewer().viewerBean;
        if (degree) {

            var axis = this.getMoveAxis(degree);
            var moveXAxis = axis.x, moveYAxis = axis.y;
            this.context2D.save();
            this.context2D.rotate(degree * (Math.PI / 180));
            this.context2D.translate(moveXAxis, moveYAxis);
        }
        jQuery(this.getViewer()).trigger("move.viewer", {'x': viewerBean.x, 'y': viewerBean.y});
    }

    function cv_flipImageDimensions() {
        var currentImage = this.getViewer().currentImage;
        var old = currentImage.curHeight;
        currentImage.curHeight = currentImage.curWidth;
        currentImage.curWidth = old;
        old = currentImage.height;
        currentImage.height = currentImage.width;
        currentImage.width = old;

    }

    //rotate90degree does everything which belongs to an image-roation
    function cv_rotate90degree() {
        var currentImage = this.getViewer().currentImage;
        var viewerBean = this.getViewer().viewerBean;

        currentImage.rotation = (currentImage.rotation + 90 >= 360) ? 0 : currentImage.rotation + 90;
        this.flipImageDimensions();
        this.rotateOverview90degree();
        
        this.lastZoomLevel = -1;
        var x = viewerBean.x;
        var y = viewerBean.y;
        
        jQuery(currentImage).trigger(iview.CurrentImage.DIMENSION_EVENT);
        
        var motion = this.setNewViewport(x,y);
        jQuery(currentImage).trigger(iview.CurrentImage.POS_CHANGE_EVENT);
        
        viewerBean.positionTiles(motion);
    }

    /**
     * Rotates the Overview 90 degree, if overview not loaded it will be rotated before first draw
     */
    function cv_rotateOverview90Degree() {
        var that = this;
        
        if(typeof this.getViewer().overview.ov == "undefined" || 
           typeof this.getViewer().overview.ov._view.my.thumbnail == "undefined" ||
           (this.getViewer().overview.ov._view.my.thumbnail.height == 0 ||
                   this.getViewer().overview.ov._view.my.thumbnail.width == 0)){    

            if(that.rotationPreview==0){
                that.rotationPreview+=90;
            }
            
            jQuery(this).one(iview.Canvas.AFTER_DRAW_EVENT, that.rotateOverview90degree);
            return;
        }
        
        if (typeof this.getViewer().canvas.ov_canvas == "undefined") {
            this.ov_canvas = document.createElement('canvas');
        } 

        var backCanvas = this.getViewer().canvas.ov_canvas;
        var img = this.getViewer().overview.ov._view.my.thumbnail;
        var cw = img.width, ch = img.height, cx = 0, cy = 0;
        var cContext = backCanvas.getContext('2d');

        if(that.rotationPreview==0){
            that.rotationPreview=90;
        }
        
        
        cw = (that.rotationPreview == 90 || that.rotationPreview == 270) ? img.height : img.width;
        ch = (that.rotationPreview == 90 || that.rotationPreview == 270) ? img.width : img.height;
        backCanvas.setAttribute('width', cw);
        backCanvas.setAttribute('height', ch);
        
        switch(that.rotationPreview){
        case 90:
            cContext.translate(img.height,0);
            break;
        case 180:
            cContext.translate(img.width,img.height);
            break;
        case 270:
            cContext.translate(0,img.width);
            break;
        }
        
        cContext.rotate(that.rotationPreview * Math.PI / 180);
        cContext.drawImage(img, cx, cy);

        that.rotationPreview=0;
        img.src = backCanvas.toDataURL();
    }


    function cv_calculateDamagedAreaRotated(moveVec) {
        var viewerBean = this.getViewer().viewerBean;
        var cvn = this.context2D.canvas;
        var damagedAreas = new Array();
        var rotation = this.getViewer().currentImage.rotation;
        
        var damagedAreaY = {
            "x" : 0,
            "y" : 0,
            "w" : 0,
            "h" : 0
        };
        var damagedAreaX = {
            "x" : 0,
            "y" : 0,
            "w" : 0,
            "h" : 0
        };
        
        
        var height = cvn.height;
        var width = cvn.width;
        
        if(rotation == 90 || rotation == 270){
            width = cvn.height;
            height = cvn.width;
        }
        
        
        switch(rotation){
        case 0:
            if (moveVec.xOff < 0) {
                damagedAreaX.w = moveVec.xOff * -1;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
            } else if (moveVec.xOff > 0) {
                damagedAreaX.x = width - moveVec.xOff;
                damagedAreaX.w = moveVec.xOff;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
            }

            if (moveVec.yOff < 0) {
                damagedAreaY.h = moveVec.yOff * -1;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           } else if (moveVec.yOff > 0) {
                damagedAreaY.y = height - moveVec.yOff;
                damagedAreaY.h = moveVec.yOff;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           }
           break;
        case 90:
           if (moveVec.xOff < 0) {
               damagedAreaX.w = moveVec.xOff * -1;
               damagedAreaX.h = height;
               damagedAreas.push(damagedAreaX);
           } else if (moveVec.xOff > 0) {
               damagedAreaX.x = width - moveVec.xOff;
               damagedAreaX.w = moveVec.xOff;
               damagedAreaX.h = height;
               damagedAreas.push(damagedAreaX);
           }
           if (moveVec.yOff < 0) {
               damagedAreaY.y = height + moveVec.yOff;
               damagedAreaY.h = moveVec.yOff * -1;
               damagedAreaY.w = width;
               damagedAreas.push(damagedAreaY);
          } else if (moveVec.yOff > 0) {
               damagedAreaY.h = moveVec.yOff;
               damagedAreaY.w = width;
               damagedAreas.push(damagedAreaY);
          }
          break;
        case 180:
            if (moveVec.xOff < 0) {
                damagedAreaX.x = width + moveVec.xOff;
                damagedAreaX.w = moveVec.xOff * -1;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
  
            } else if (moveVec.xOff > 0) {
                damagedAreaX.w = moveVec.xOff;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
            }
            if (moveVec.yOff < 0) {
                damagedAreaY.y = height + moveVec.yOff;
                damagedAreaY.h = moveVec.yOff * -1;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           } else if (moveVec.yOff > 0) {
                damagedAreaY.h = moveVec.yOff;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           }
           break;
        case 270:
            if (moveVec.xOff < 0) {
                damagedAreaX.x = width + moveVec.xOff;
                damagedAreaX.w = moveVec.xOff * -1;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
  
            } else if (moveVec.xOff > 0) {
                damagedAreaX.w = moveVec.xOff;
                damagedAreaX.h = height;
                damagedAreas.push(damagedAreaX);
            }

            if (moveVec.yOff < 0) {
                damagedAreaY.h = moveVec.yOff * -1;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           } else if (moveVec.yOff > 0) {
                damagedAreaY.y = height - moveVec.yOff;
                damagedAreaY.h = moveVec.yOff;
                damagedAreaY.w = width;
                damagedAreas.push(damagedAreaY);
           }
           break;
        }

        for ( var i in damagedAreas) {
            damagedAreas[i].x += this.getBeanX();
            damagedAreas[i].y += this.getBeanY();
        }
               
        return damagedAreas;
    }
    
    function cv_addRotateButton() {
        jQuery(document).bind("toolbarloaded", function(e) { 
            if (e.model.id != "mainTb") {
                return;
            }

            var toolbarModel = e.model;
            var i = toolbarModel.getElementIndex('spring');

            var buttonSet = new ToolbarButtonsetModel("rotation");
            var button = new ToolbarButtonModel("rotateRight", {
                'type' : 'buttonDefault'
            }, {
                'label' : "rotate",
                'text' : false,
                'icons' : {
                    primary : 'iview2-icon iview2-icon-rotate'
                }
            }, "toolbar.rotate", true, false);
            toolbarModel.addElement(buttonSet, i);
            buttonSet.addButton(button); // attach to events of view
            jQuery.each(e.getViews(), function(index, view) {
                jQuery(view).bind("press", function(event, args) {
                    if (args.parentName == buttonSet.elementName) {
                        if (args.elementName == button.elementName) {
                            e.viewer.canvas.rotate90degree();
                        }
                    }
                });
            });
        });
    }
    
    
    function cv_setNewViewport(x,y){
        
    
       var viewerBean = this.getViewer().viewerBean;
       var currentImage = this.getViewer().currentImage;
       var rotation = currentImage.rotation;               
       var beanMiddleX = (viewerBean.width / 2);
       var beanMiddleY = (viewerBean.height / 2);

       var newX = currentImage.curWidth  - (y + viewerBean.height) + (beanMiddleY - (viewerBean.width / 2));
       var newY = x + (beanMiddleX - (viewerBean.height/ 2));
       

		return {
			x : viewerBean.x - newX,
			y : viewerBean.y - newY
		};
    } 
    
    function cv_getRotatedBeanX(){
        var viewerBean = this.getViewer().viewerBean;
        var rotation = this.getViewer().currentImage.rotation;
        var currentImage = this.getViewer().currentImage;
        
        switch(rotation){
        case 0:
            return viewerBean.x;
        case 90:
            return viewerBean.y;
        case 180:
            return (currentImage.curWidth - viewerBean.x) - Math.min(currentImage.curWidth, viewerBean.width);
        case 270:
            return (currentImage.curHeight - viewerBean.y) -  Math.min(currentImage.curHeight, viewerBean.height);//- viewerBean.width;
        }
    }
    
    function cv_getRotatedBeanY(){
        var viewerBean = this.getViewer().viewerBean;
        var rotation = this.getViewer().currentImage.rotation;
        var currentImage = this.getViewer().currentImage;
        
        switch(rotation){
        case 0:
            return viewerBean.y;
        case 90:
            return (currentImage.curWidth - viewerBean.x) - Math.min(currentImage.curWidth, viewerBean.width) ;
        case 180:
            return (currentImage.curHeight - viewerBean.y) - Math.min(currentImage.curHeight, viewerBean.height);
        case 270:
            return (currentImage.curWidth - (currentImage.curWidth - viewerBean.x));
        }
    }

    function cv_getFullScreenAreaRotated(){
        var viewerBean = this.getViewer().viewerBean;
        var rotation = this.getViewer().currentImage.rotation;
        
        var fsa = {
                "x" : this.getBeanX(),
                "y" : this.getBeanY(),
                "w" : Math.min(viewerBean.width, this.getCurrentImageWidth()),
                "h" : Math.min(viewerBean.height, this.getCurrentImageHeight()) 
            };
        
        if(rotation == 90 || rotation == 270){
            fsa.w = Math.min(viewerBean.height, this.getCurrentImageWidth());
            fsa.h = Math.min(viewerBean.width, this.getCurrentImageHeight());
        }
        return fsa;
    }
    
    function cv_moveCanvas(moveVec) {
        var ctx = this.context2D;
        var rotation = this.getViewer().currentImage.rotation;
        ctx.save();
        ctx.beginPath();
        ctx.rect(0,0,this.context2D.canvas.width,this.context2D.canvas.height);
        ctx.clip();
        if(rotation == 90 || rotation == 270){
            ctx.drawImage(ctx.canvas,-(moveVec.yOff),-(moveVec.xOff));
        } else {
            ctx.drawImage(ctx.canvas, -(moveVec.xOff), -(moveVec.yOff));
        }
        ctx.restore();
    }
    
    if (iview.isCanvasAvailable) {
        cv_addRotateButton();
        iview.Canvas.Rotate = (function() {

            function constructor(iviewInst) {
                iview.IViewObject.call(this, iviewInst);
            }
            return constructor;

        });

        jQuery(document).bind(iview.IViewInstance.INIT_EVENT, function(event, iViewInst) {
            iViewInst.rotate = new iview.Canvas.Rotate(iViewInst);
            //PanoJS.prototype.getMaxDimViewer = cv_getMaxDimViewer;
            PanoJS.prototype.getMaxDimCurZoomLevel = cv_getMaxDimCurZoomLevel;
            PanoJS.prototype.getTileSizeMinZoomLevel = cv_getTileSizeMinZoomLevel;
            
            iViewInst.canvas.flipImageDimensions = cv_flipImageDimensions;
            iViewInst.canvas.rotateCanvas = cv_rotateCanvas;
            iViewInst.canvas.rotate90degree = cv_rotate90degree;
            iViewInst.canvas.bothAxisBigger = cv_bothAxisBigger;
            iViewInst.canvas.rotateOverview90degree = cv_rotateOverview90Degree;
            iViewInst.canvas.getCurrentImageWidth = cv_getRotateCurrentWidth;
            iViewInst.canvas.getCurrentImageHeight = cv_getRotateCurrentHeight;
            iViewInst.canvas.getCurrentCanvasWidth = cv_getCurrentRotatedCanvasWidth;
            iViewInst.canvas.getCurrentCanvasHeight = cv_getCurrentRotatedCanvasHeight;
            iViewInst.canvas.getPreviewX = cv_getPreviewX;
            iViewInst.canvas.getPreviewY = cv_getPreviewY;
            iViewInst.canvas.getMoveAxis = cv_getMoveAxis;
            iViewInst.canvas.calculateDamagedArea = cv_calculateDamagedAreaRotated;
            iViewInst.canvas.getFullScreenArea = cv_getFullScreenAreaRotated;
            iViewInst.canvas.getOriginalImageWidth = cv_getOrigWidth;
            iViewInst.canvas.getOriginalImageHeight = cv_getOrigHeight;
            iViewInst.canvas.isYBorderTile = cv_isYBorderTile;
            iViewInst.canvas.isXBorderTile = cv_isXBorderTile;
            iViewInst.canvas.getBeanY = cv_getRotatedBeanY;
            iViewInst.canvas.getBeanX = cv_getRotatedBeanX;
            iViewInst.canvas.calculateMoveVector = cv_calculateMoveVector;
            iViewInst.canvas.moveCanvas = cv_moveCanvas;
            iViewInst.canvas.setNewViewport = cv_setNewViewport;
            iViewInst.canvas.rotationPreview = 0;
            
            jQuery(iViewInst.canvas).bind(iview.Canvas.BEFORE_DRAW_EVENT, function(event) {
                iViewInst.canvas.rotateCanvas(iViewInst.currentImage.rotation);
                event.stopPropagation();
            });
            
            jQuery(iViewInst.canvas).bind(iview.Canvas.AFTER_DRAW_EVENT, function(event) {
                this.context2D.restore();
            });
            
            var rotParName="rotation";

            iViewInst.permalink.observer.push({
                getParameter : function(){ 
                            return { "name": rotParName, "value": iViewInst.currentImage.rotation
                        };}
            });
            var rotPar=URL.getParam(rotParName);
            if (isNaN(rotPar)||rotPar % 90 != 0){
                log('the rotation parameter is invalid');
                return;
            }
            
            jQuery(iViewInst.currentImage).bind(iview.CurrentImage.CHANGE_EVENT,function(event) {
                iViewInst.currentImage.rotation=0;
            });
            
            jQuery(iViewInst.currentImage).one(iview.CurrentImage.CHANGE_EVENT,function(event) {
                while (rotPar%360!=0 || rotPar != 0){
                    iViewInst.canvas.rotate90degree();
                    rotPar-=90;
                }
            });
        });
    }
})();