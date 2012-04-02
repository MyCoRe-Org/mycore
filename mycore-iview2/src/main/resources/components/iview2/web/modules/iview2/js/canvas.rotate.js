(function() {
    "use strict";

    function cv_getRotateCurrentWidth() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curWidth : currentImage.curHeight);
    }

    function cv_getRotateCurrentHeight() {
        var currentImage = this.getViewer().currentImage;
        return Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curHeight : currentImage.curWidth);
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

    function cv_bothAxisBigger() {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        return !!((currentImage.curWidth > viewerBean.width && currentImage.curHeight > viewerBean.height) || (currentImage.curHeight > viewerBean.width && currentImage.curWidth > viewerBean.height));
    }

    function cv_getXOffset(areaStartX, areaEndX) {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;
        switch (rotation) {
        case 0:
            return viewerBean.x + areaStartX;
        case 90:
            return viewerBean.y + areaStartX;
        case 180:
            return (currentImage.curWidth - viewerBean.x) - areaEndX;
        case 270:
            return (currentImage.curHeight - viewerBean.y) - areaEndX;
        }
    }

    function cv_getYOffset(areaStartY, areaEndY) {
        var viewerBean = this.getViewer().viewerBean;
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation;
        switch (rotation) {
        case 0:
            return viewerBean.y + areaStartY;
        case 90:
            return (currentImage.curWidth - viewerBean.x) - areaEndY;
        case 180:
            return (currentImage.curHeight - viewerBean.y) - areaEndY;
        case 270:
            return currentImage.curWidth - (currentImage.curWidth - viewerBean.x);// + areaEndY;
        }
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

        //degree = 0 -> nothing to translate and viewport doesn't change too
        if (degree) {

            var axis = this.getMoveAxis(degree);
            var moveXAxis = axis.x, moveYAxis = axis.y;
            this.context2D.save();
            this.context2D.rotate(degree * (Math.PI / 180));
            this.context2D.translate(moveXAxis, moveYAxis);
        }
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

    function cv_flipViewerDimensions() {
        var viewerBean = this.getViewer().viewerBean;
        var old = viewerBean.width;
        viewerBean.width = viewerBean.height;
        viewerBean.height = old;
    }

    //rotate90degree does everything which belongs to an image-roation
    function cv_rotate90degree() {
        var currentImage = this.getViewer().currentImage;
        var viewerBean = this.getViewer().viewerBean;

        currentImage.rotation = (currentImage.rotation + 90 >= 360) ? 0 : currentImage.rotation + 90;
        this.flipImageDimensions();
        this.rotateOverview90degree();
        viewerBean.positionTiles();

        jQuery(currentImage).trigger(iview.CurrentImage.DIMENSION_EVENT);
    }

    function cv_rotateOverview90Degree() {
        if (typeof this.ov_canvas == "undefined") {
            this.ov_canvas = document.createElement('canvas');
        }

        var backCanvas = this.ov_canvas;
        var img = this.getViewer().overview.ov._view.my.thumbnail;
        var cw = img.width, ch = img.height, cx = 0, cy = 0;

        var cContext = backCanvas.getContext('2d');

        cw = img.height;
        ch = img.width;
        cy = img.height * (-1);

        backCanvas.setAttribute('width', cw);
        backCanvas.setAttribute('height', ch);
        cContext.rotate(90 * Math.PI / 180);
        cContext.drawImage(img, cx, cy);
        img.src = backCanvas.toDataURL();

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
                'label' : "Rotate right",
                'text' : false,
                'icons' : {
                    primary : 'paperClip-icon'
                }
            }, "Rotate right", true, false);
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

    /**
     * Calculates the damaged area for a move vector if canvas rotated
     * @param moveVec the move vector {x,y}
     * @returns {Array} the calculated damaged Areas
     */
    function cv_calculateDamagedAreaRotated(moveVec){
        var viewerBean = this.getViewer().viewerBean;
        var cvn = this.context2D.canvas;
        var damagedAreas = new Array();
        var currentImage = this.getViewer().currentImage;
        var rotation = currentImage.rotation
        var damagedAreaY = {"x" : 0, "y" : 0, "w" : 0, "h" : 0 };
        var damagedAreaX = {"x" : 0, "y" : 0, "w" : 0, "h" : 0 };
        
        switch(rotation){
            case 0:
                if(moveVec.xOff < 0){
                    damagedAreaX.x = viewerBean.x;
                    damagedAreaX.y = viewerBean.y;
                    damagedAreaX.w = moveVec.xOff * -1;
                    damagedAreaX.h = viewerBean.height;
                    damagedAreas.push(damagedAreaX);
                }else if(moveVec.xOff > 0){
                    damagedAreaX.y = viewerBean.y;
                    damagedAreaX.x = viewerBean.x + (viewerBean.width - moveVec.xOff);
                    damagedAreaX.w = moveVec.xOff;
                    damagedAreaX.h = viewerBean.height;
                    damagedAreas.push(damagedAreaX);
                }
                if(moveVec.yOff < 0){
                    damagedAreaY.y = viewerBean.y;
                    damagedAreaY.x = viewerBean.x;
                    damagedAreaY.h = moveVec.yOff * -1;
                    damagedAreaY.w = viewerBean.width;
                    damagedAreas.push(damagedAreaY);
                }else if(moveVec.yOff > 0){
                    damagedAreaY.x = viewerBean.x;
                    damagedAreaY.y = viewerBean.y + (viewerBean.height - moveVec.yOff);
                    damagedAreaY.h = moveVec.yOff;
                    damagedAreaY.w = viewerBean.width;
                    damagedAreas.push(damagedAreaY);
                }
             break;
            case 90:

            break;
            case 180:

            break;
            case 270:
            
            break;        
        } 
        
        
        return damagedAreas;
    };
    
    if (iview.isCanvasAvailable) {
        cv_addRotateButton();
        iview.Canvas.Rotate = (function() {

            function constructor(iviewInst) {
                iview.IViewObject.call(this, iviewInst);
            }
            return constructor;

        });

        jQuery(document).bind(iview.IViewInstance.INIT_EVENT, function(event, iViewInst) {

            // alter iViewInst
            iViewInst.rotate = new iview.Canvas.Rotate(iViewInst);
            // define necessary functions
            iViewInst.canvas.flipImageDimensions = cv_flipImageDimensions;
            //iViewInst.canvas.flipViewerDimensions = cv_flipViewerDimensions;
            iViewInst.canvas.rotateCanvas = cv_rotateCanvas;
            iViewInst.canvas.rotate90degree = cv_rotate90degree;
            iViewInst.canvas.bothAxisBigger = cv_bothAxisBigger;
            iViewInst.canvas.rotateOverview90degree = cv_rotateOverview90Degree;
            // override necessary functions 

            iViewInst.canvas.getCurrentWidth = cv_getRotateCurrentWidth;
            iViewInst.canvas.getCurrentHeight = cv_getRotateCurrentHeight;
            iViewInst.canvas.getCurrentCanvasWidth = cv_getCurrentRotatedCanvasWidth;
            iViewInst.canvas.getCurrentCanvasHeight = cv_getCurrentRotatedCanvasHeight;
            iViewInst.canvas.getXOffset = cv_getXOffset;
            iViewInst.canvas.getYOffset = cv_getYOffset;
            iViewInst.canvas.getPreviewX = cv_getPreviewX;
            iViewInst.canvas.getPreviewY = cv_getPreviewY;
            iViewInst.canvas.getMoveAxis = cv_getMoveAxis;
            iViewInst.canvas.calculateDamagedArea = cv_calculateDamagedAreaRotated;
            // put in the pre draw operations
            
            jQuery(iViewInst.canvas).bind(iview.Canvas.BEFORE_DRAW_EVENT, function(event) {
                iViewInst.canvas.rotateCanvas(iViewInst.currentImage.rotation);
                event.stopPropagation();
            });

            jQuery(iViewInst.canvas).bind(iview.Canvas.AFTER_DRAW_EVENT, function(event) {
                this.context2D.restore();
            });

        });
    }
})();