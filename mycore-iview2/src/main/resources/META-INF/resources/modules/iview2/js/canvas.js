(function() {
    "use strict";

    iview.isCanvasAvailable = (function() {
        return !!document.createElement("canvas").getContext && URL.getParam("iview2.canvas") != "false";
    })();

    iview.Canvas = (function() {

        function constructor(iviewInst) {
            iview.IViewObject.call(this, iviewInst);
           
            if (iview.isCanvasAvailable) {

                this.context2D = document.createElement('canvas').getContext('2d');
                this.redrawPreview = document.createElement('canvas');
                jQuery(this.redrawPreview).css("position", "fixed");
                this.activateCanvas = false;
                this.lastFrame = new Date();
                this.updateCanvasCount = 0;
                this.preView = new Image();
                this.lastPosX = 0;
                this.lastPosY = 0;
                this.lastZoomLevel = -1;
                this.lastScale = -1;
                this.damagedArea = new Array();
                this.notLoadedTile = new Array();
                this.loadingTile = new Array();
                var that = this;

                if(!PanoJS.prototype.getMaxDimViewerOrig) {
                	PanoJS.prototype.getMaxDimViewerOrig = PanoJS.prototype.getMaxDimViewer;
                }
                PanoJS.prototype.getMaxDimViewer = function cv_getMaxDimViewerOrig(screen) {
                	 return that.getViewer().viewerBean.getMaxDimViewerOrig(screen);
				};

				if(!PanoJS.prototype.getTileSizeMinZoomLevelOrig) {
					PanoJS.prototype.getTileSizeMinZoomLevelOrig = PanoJS.prototype.getTileSizeMinZoomLevel;
				}
                PanoJS.prototype.getTileSizeMinZoomLevel = function cv_getTileSizeMinZoomLevelOrig(screen) {
                	 return that.getViewer().viewerBean.getTileSizeMinZoomLevelOrig(screen);
				};
				
				if(!PanoJS.prototype.getMaxDimCurZoomLevelOrig) {
					PanoJS.prototype.getMaxDimCurZoomLevelOrig = PanoJS.prototype.getMaxDimCurZoomLevel;
				}
                PanoJS.prototype.getMaxDimCurZoomLevel = function cv_getMaxDimCurZoomLevelOrig(screen, calculatedMinFitZoomLevel) {
                	 return that.getViewer().viewerBean.getMaxDimCurZoomLevelOrig(screen , calculatedMinFitZoomLevel);
				};
                
				if(!PanoJS.prototype.assignTileImageOrig) {
					PanoJS.prototype.assignTileImageOrig = PanoJS.prototype.assignTileImage;
				}
                PanoJS.prototype.assignTileImage = function cv_assignTileImage() {
                    that.assignTileImage(arguments[0]);
                };

                if(!PanoJS.prototype.resizeOrig) {
                	PanoJS.prototype.resizeOrig = PanoJS.prototype.resize;
                }
                PanoJS.prototype.resize = function cv_resize() {
                    that.context2D.canvas.width = that.getViewer().viewerBean.width;
                    that.context2D.canvas.height = that.getViewer().viewerBean.height;
                    that.getViewer().viewerBean.resizeOrig();
                };

                if(!PanoJS.prototype.switchDisplayModeOrig) {
                	PanoJS.prototype.switchDisplayModeOrig = PanoJS.prototype.switchDisplayMode;
                }
                PanoJS.prototype.switchDisplayMode = function cv_switchDisplayMode() {
                    return that.switchDisplayMode(arguments[0], arguments[1], arguments[2], arguments[3]);
                };

                if(!PanoJS.prototype.updateScreenOrig) {
                	PanoJS.prototype.updateScreenOrig = PanoJS.prototype.updateScreen;
                }
                PanoJS.prototype.updateScreen = function cv_updateScreen() {
                    that.updateScreen();
                }
                if (false) {
                    jQuery(this).bind(iview.Canvas.AFTER_DRAW_EVENT, function(event) {
                        var curTime = new Date();
                        var difference = curTime.getTime() - this.lastFrame.getTime();
                        this._drawFpsBox(1000 / difference);
                        this.lastFrame = curTime;
                    });
                }
                jQuery(this).trigger(iview.Canvas.AFTER_INIT_EVENT);
            }
        }

        // inheritance(copy) from prototype iview
        constructor.prototype = Object.create(iview.IViewObject.prototype);

        /**
         * prepare tiles is no needed anymore
         */
        constructor.prototype.prepareTiles = function cv_prepareTiles() {
        };

        /**
         * Requests a Screen Update and calls _updateScreen
         */
        constructor.prototype.updateScreen = function cv_updateScreen() {
            var scope = this;
            if (this.updateCanvasCount++ == 0) {
                requestAnimationFrame(function() {
                    scope._updateScreen();
                }, this.context2D.canvas);
            }
        };

        /**
         * Updates the Canvas.
         */
        constructor.prototype._updateScreen = function cv__updateScreen() {
            if(!this.activateCanvas){
                this._activateCanvas();
                //return;
            }
            var scope = this;
            var viewerBean = this.getViewer().viewerBean;
            var zoomLevel = viewerBean.zoomLevel;
            var moveVector = this.calculateMoveVector();
            var moveOutOfScreen = this.isMoveOutOfScreen(moveVector);
            var zoomLevelChanged = this.zoomLevelChanged(this.lastZoomLevel);
            var currentImage = this.getViewer().currentImage;
            var scale = currentImage.zoomInfo.scale;
            var scaleChanged = this.scaleChanged(this.lastScale);
            this.updateCanvasCount = 0;
            this.lastPosX = viewerBean.x;
            this.lastPosY = viewerBean.y;
            this.refreshImageDimensions();
            this.redrawPreview.width = this.context2D.canvas.width;
            this.redrawPreview.height = this.context2D.canvas.height;
            if (zoomLevelChanged || moveOutOfScreen || scaleChanged || (moveVector.xOff == 0 && moveVector.yOff==0)) {
                // something happened that needs to redraw all tiles
                this.clearCanvas();
                jQuery(scope).trigger(iview.Canvas.BEFORE_DRAW_EVENT);
                this.drawPreview();
                this.clearDamagedArea();
                this.drawArea(this.getFullScreenArea());
            } else {
                // the viewer only moves, so we need only to draw the border Tiles 
                this.moveCanvas(moveVector);
                jQuery(scope).trigger(iview.Canvas.BEFORE_DRAW_EVENT);
                var moveDamagedArea = this.calculateDamagedArea(moveVector);
                var damagedAreas = this.damagedArea.concat(moveDamagedArea);
                this.clearDamagedArea();

                var currentArea;

                while (currentArea = damagedAreas.pop()) {
                    this.drawArea(currentArea);
                }
            }
            this.lastZoomLevel = zoomLevel;
            this.lastScale = scale;
            jQuery(scope).trigger(iview.Canvas.AFTER_DRAW_EVENT);
        };

        /**
         * Calculates a Damaged Area for the whole viewer Bean
         */
        constructor.prototype.getFullScreenArea = function cv_getFullScreenArea() {
            var viewerBean = this.getViewer().viewerBean;
            return {
                "x" : viewerBean.x,
                "y" : viewerBean.y,
                "w" : Math.min(viewerBean.width, this.getCurrentImageWidth()),
                "h" : Math.min(viewerBean.height, this.getCurrentImageHeight())
            };
        };

        /**
         * Checks the zoom-level of the picture has changed
         * @param lastZoomLevel 
         * The last zoom-level to compare with 
         */
        constructor.prototype.zoomLevelChanged = function cv_zoomLevelChanged(lastZoomLevel) {
            var zoomLevel = this.getViewer().viewerBean.zoomLevel;
            var hasChanged = lastZoomLevel != zoomLevel;
            return hasChanged;
        };

        /**
         * Checks the scale of the picture has changed
         * @param lastScale 
         * The last Scale to compare with 
         */
        constructor.prototype.scaleChanged = function cv_scaleChanged(lastScale) {
            var scale = this.getViewer().currentImage.zoomInfo.scale;
            var hasChanged = lastScale != scale;
            return hasChanged;
        };

        /**
         * Checks a move vector move complete out of the current screen
         * 
         * @param moveVector
         *            the vector to check
         * @returns {Boolean} moves out of screen
         */
        constructor.prototype.isMoveOutOfScreen = function cv_isMoveOutOfScreen(moveVector) {
            var viewerBean = this.getViewer().viewerBean;
            return (moveVector.xOff > 0 && moveVector.xOff > viewerBean.width)
                    || (moveVector.xOff < 0 && moveVector.xOff < -viewerBean.width)
                    || (moveVector.yOff > 0 && moveVector.yOff > viewerBean.height)
                    || (moveVector.yOff < 0 && moveVector.yOff < -viewerBean.height);
        }

        /**
         * @returns the difference between this and last position of ViewerBean {xOff, yOff}
         */
        constructor.prototype.calculateMoveVector = function cv_calculateMoveVector() {
            var viewerBean = this.getViewer().viewerBean;
            var posX = viewerBean.x;
            var posY = viewerBean.y;

            return {
                "xOff" : posX - this.lastPosX,
                "yOff" : posY - this.lastPosY
            };
        };

        /**
         * Moves content of Canvas by {moveVec}
         * 
         * @param moveVec
         */
        constructor.prototype.moveCanvas = function cv_moveCanvas(moveVec) {
            var ctx = this.context2D;
            ctx.drawImage(ctx.canvas, -(moveVec.xOff), -(moveVec.yOff));
        };

        /**
         * Remove all Childs from DamagedArea
         */
        constructor.prototype.clearDamagedArea = function cv_clearDamagedArea() {
            while (this.damagedArea.pop());
                
        };

        /**
         * Calculates the damaged area for a move vector
         * 
         * @param moveVec
         *            the move vector {x,y}
         * @returns {Array} the calculated damaged Areas
         */
        constructor.prototype.calculateDamagedArea = function cv_calculateDamagedArea(moveVec) {
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
            
            

            if (moveVec.xOff < 0) {
                damagedAreaX.w = moveVec.xOff * -1;
                damagedAreaX.h = cvn.height;
                damagedAreas.push(damagedAreaX);
            } else if (moveVec.xOff > 0) {
                damagedAreaX.x = this.getCurrentCanvasWidth() - moveVec.xOff;
                damagedAreaX.w = moveVec.xOff;
                damagedAreaX.h = cvn.height;
                damagedAreas.push(damagedAreaX);
            }

            if (moveVec.yOff < 0) {
                damagedAreaY.h = moveVec.yOff * -1;
                damagedAreaY.w = cvn.width;
                damagedAreas.push(damagedAreaY);
            } else if (moveVec.yOff > 0) {
                damagedAreaY.y = this.getCurrentCanvasHeight() - moveVec.yOff;
                damagedAreaY.h = moveVec.yOff;
                damagedAreaY.w = cvn.width;
                damagedAreas.push(damagedAreaY);
            }

            for ( var i in damagedAreas) {
                damagedAreas[i].x += viewerBean.x;
                damagedAreas[i].y += viewerBean.y;
            }

            return damagedAreas;
        };

        /**
         * Draws a specific area to the Canvas
         * 
         * @param area
         *            {x,y,w,h} the area that should be drawn. X and y are global(whole picture, not only bean)
         */
        constructor.prototype.drawArea = function cv_drawArea(area) {
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var old = 0;
            var tileSize = viewerBean.tileSize;
            var originTileSize = this.getViewer().properties.tileSize;
            var scale = currentImage.zoomInfo.scale;
            var curWidth = this.getCurrentImageWidth();
            var curHeight = this.getCurrentImageHeight();

            var sizeX = curWidth;
            var sizeY = curHeight;

            var tilesXStart = Math.floor(area.x / tileSize);
            var tilesYStart = Math.floor(area.y / tileSize);
            var tilesXEnd = Math.ceil((area.x + area.w) / tileSize);
            var tilesYEnd = Math.ceil((area.y + area.h) / tileSize);

            var ctx = this.context2D;
            ctx.save();
            ctx.beginPath();
            ctx.rect(area.x - this.getBeanX(), area.y - this.getBeanY(), area.w, area.h);
            ctx.clip();
            // Draw only in damaged Area. The "rest" of the tiles will clipped.
            for (tilesXStart = tilesXStart; tilesXStart < tilesXEnd; tilesXStart++) {
                for ( var yCount = tilesYStart; yCount < tilesYEnd; yCount++) {
                    this.assignTileImage(tilesXStart, yCount);
                }
            }
            ctx.restore();
            
          
            // draw the preview picture to the not loaded tiles
            if (this.notLoadedTile.length != 0) {
                ctx.save();
                ctx.beginPath();
                ctx.strokeStyle = "#a00";
                var current;
                while (current = this.notLoadedTile.pop()) {
                    ctx.rect(current.x, current.y, current.w, current.h);
                }
                ctx.clip();
                this.drawPreview(); 
                ctx.restore();
            } 
            
            this.tilesComplete = true;
            
        };

        constructor.prototype.isAreaInBean = function isAreaInBean(area) {
            var viewerBean = this.getViewer().viewerBean;
            var x1 = area.x, x2 = viewerBean.x, y1 = area.y, y2 = viewerBean.y;
            var w1 = area.w, w2 = viewerBean.width, h1 = area.h, h2 = viewerBean.height;
            if (x1 + w1 < x2)
                return false;
            if (x1 > x2 + w2)
                return false;
            if (y1 + h1 < y2)
                return false;
            if (y1 > y2 + h2)
                return false;
            return true;
        };

        constructor.prototype.assignTileImage = function cv_assignTileImage(tileX, tileY) {
            var viewerBean = this.getViewer().viewerBean;
            var tileImgId = viewerBean.tileUrlProvider.assembleUrl(tileX, tileY, viewerBean.zoomLevel);
            var tileImg = viewerBean.cache.getItem(tileImgId);

            if (tileImg == null) {
                tileImg = this.createImageTile(tileImgId, true, viewerBean, tileX, tileY);
            }
            
            this.drawTileImage(tileX, tileY, tileImg);
        };

        constructor.prototype.isXBorderTile = function cv_isXBorderTile(tileX) {
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var tileSize = viewerBean.tileSize;
            return Math.floor(currentImage.curWidth / tileSize) == tileX;
        };

        constructor.prototype.isYBorderTile = function cv_isYBorderTile(tileY) {
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var tileSize = viewerBean.tileSize;
            return Math.floor(currentImage.curHeight / tileSize) == tileY;
        };


        constructor.prototype.drawTileImage = function cv_drawTileImage(tileX, tileY, tileImg) {
        	var dx, dy, dw, dh, sx, sy, sw, sh;
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var tileSize = viewerBean.tileSize;// currentImage.zoomInfo.scale;
            var originTileSize = this.getViewer().properties.tileSize;
            var scale = currentImage.zoomInfo.scale;

            dx = (tileX * tileSize) - this.getBeanX();
            dy = (tileY * tileSize) - this.getBeanY();
            dw = tileSize;
            dh = tileSize;
            sx = 0;
            sy = 0;
            sw = originTileSize;
            sh = originTileSize;

            if (this.isXBorderTile(tileX)) {
                dw = this.getCurrentImageWidth() % tileSize;
                sw = tileImg.naturalWidth;
            }
            if (this.isYBorderTile(tileY)) {
                dh = this.getCurrentImageHeight() % tileSize;
                sh = tileImg.naturalHeight;
            }

            if (tileImg.loaded) {
                try {
                    this.context2D.drawImage(tileImg, sx, sy, sw, sh, dx, dy, dw, dh);
                } catch (err) {
                    console.log(err);
                }
            } else {
                this.notLoadedTile.push({
                    "x" : dx,
                    "y" : dy,
                    "w" : dw,
                    "h" : dh
                });
                this.tilesComplete = false;
            }

        };

        constructor.prototype.createImageTile = function cv_createImageTile(tileImgId, createCache, viewerBean, tx, ty) {
            var tileImg = new Image();
            var viewer = this.getViewer();
            var viewerBean = viewer.viewerBean;
            viewerBean.cache.setItem(tileImgId, tileImg);
            var that = this;
            var zoomLevelOnLoad = this.getViewer().viewerBean.zoomLevel;
            tileImg.onload = function cv_tileOnLoad() {
                var imgScope = this;
                imgScope.loaded = true;
                
                if (that.zoomLevelChanged(zoomLevelOnLoad)) {
                    return;
                }
                
                jQuery(that).trigger(iview.Canvas.BEFORE_DRAW_EVENT);
                that.drawTileImage(tx, ty, tileImg);
                jQuery(that).trigger(iview.Canvas.AFTER_DRAW_EVENT);
            };
            this.loadingTile.push(tileImg);
            tileImg.src = tileImgId;
            
            return tileImg;
        };

        constructor.prototype.createCoordObject = function cv_createCoordObject(sx, sy, sw, sh, dx, dy, dw, dh) {
            return {
                "sx" : sx,
                "sy" : sy,
                "sw" : sw,
                "sh" : sh,
                "dx" : dx,
                "dy" : dy,
                "dw" : dw,
                "dh" : dh
            };
        }

        constructor.prototype._drawFpsBox = function cv_drawDebugBox(fps) {
            this.context2D.fillStyle = "Rgb(204,204,0)";
            this.context2D.fillRect(0, this.context2D.canvas.height - 15, 20, 15);
            this.context2D.fillStyle = "Rgb(0,0,0)";
            this.context2D.fillText(Math.round(fps), 0, this.context2D.canvas.height - 3);
        };

        constructor.prototype.getBeanX = function cv_getBeanX() {
            var viewer = this.getViewer();
            var viewerBean = viewer.viewerBean;
            return viewerBean.x;
        };

        constructor.prototype.getBeanY = function cv_getBeanY() {
            var viewer = this.getViewer();
            var viewerBean = viewer.viewerBean;
            return viewerBean.y;
        };


        constructor.prototype.getCurrentCanvasWidth = function cv_getCurrentCanvasWidth() {
            return this.context2D.canvas.width;
        };

        constructor.prototype.getCurrentCanvasHeight = function cv_getCurrentCanvasHeight() {
            return this.context2D.canvas.height;
            j
        };

        /**
         * Gets the right x coordinate for the preview picture
         */
        constructor.prototype.getPreviewX = function cv_getPreviewX() {
            return -this.getViewer().viewerBean.x;
        };

        /**
         * Gets the right y coordinate for the preview picture
         */
        constructor.prototype.getPreviewY = function cv_getPreviewY() {
            return -this.getViewer().viewerBean.y;
        };

        /**
         * Draws the Preview Image to the right Position.
         */
        constructor.prototype.drawPreview = function cv_drawPreview() {
            if (!this.preView.loaded)
                return;
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var x = this.getPreviewX(), y = this.getPreviewY();
            var w = this.getCurrentImageWidth();
            var h = this.getCurrentImageHeight();
            this.context2D.drawImage(this.preView, x, y, w, h);
        };

        constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping) {
            var viewerBean = this.getViewer().viewerBean;
            var temp = viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);
            
            this._activateCanvas();

            return temp;
        };

        constructor.prototype._activateCanvas = function cv_activateCanvas(){
            var viewerBean = this.getViewer().viewerBean;
            if (!this.activateCanvas) {
                this.activateCanvas = true;
                this.context2D.canvas.y = this.context2D.canvas.x = 0;
                this.appendCanvas();

                //make sure that the preview image is already loaded before drawing on canvas
                var that = this;
                that.preView.onload = function() {
                    that.preView.loaded = true;
                    that.getViewer().viewerBean.positionTiles();
                };
                that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;
                this.getViewer().context.container.find(".iview_well").find(".preload")[0].style.display = "none";

                jQuery(this.getViewer().currentImage).on(iview.CurrentImage.CHANGE_EVENT, function(){
                	that.clearCanvas();
                	var current;
                	while(current = that.loadingTile.pop()){
                		// the image changes and the tiles shouldnt be drawn.
                		// replace the onload with dummy function.
                		current.onload=function(){}; 
                	}
                });
                jQuery(this.getViewer().currentImage).bind(iview.CurrentImage.CHANGE_EVENT, function() {
                	that.preView.loaded = false;
                    that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;
                    that.lastZoomLevel = -1;
                });

                this.context2D.canvas.width = viewerBean.width;
                this.context2D.canvas.height = viewerBean.height;

            }
        };
        
        /**
         * Resets the canvas
         */
        constructor.prototype.clearCanvas = function cv_clearCanvas() {
            this.context2D.canvas.width = this.context2D.canvas.width;
        };

        /**
         * Ads the canvas to the viewer Container
         */
        constructor.prototype.appendCanvas = function cv_appendCanvas() {
            this.getViewer().context.container.find(".iview_well").prepend(this.context2D.canvas);//before,prepend,append
            this.getViewer().context.container.find(".iview_well").prepend(this.redrawPreview);
        };

        /**
         * Updates the width and height of the CurrentImage
         */
        constructor.prototype.refreshImageDimensions = function cv_refreshImageDimensions() {
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            currentImage.curWidth = Math.ceil((currentImage.width / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))
                    * currentImage.zoomInfo.scale);
            currentImage.curHeight = Math.ceil((currentImage.height / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))
                    * currentImage.zoomInfo.scale);
        };

        constructor.prototype.floatToInt = function cv_floatToInt(somenum) {
            return (0.5 + somenum) << 0;
        };

        return constructor;

    })();

    iview.Canvas.BEFORE_DRAW_EVENT = "beforeDraw.canvas.iview";
    iview.Canvas.AFTER_DRAW_EVENT = "afterDraw.canvas.iview";
    iview.Canvas.AFTER_INIT_EVENT = "afterInit.canvas.iview";
})();
