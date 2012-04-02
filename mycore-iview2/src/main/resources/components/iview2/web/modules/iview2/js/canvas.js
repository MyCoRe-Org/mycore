(function() {
    "use strict";

    iview.isCanvasAvailable = (function() {
        return !!document.createElement("canvas").getContext && URL.getParam("iview2.canvas") == "true";
    })();

    iview.Canvas = (function() {

        function constructor(iviewInst) {
            iview.IViewObject.call(this, iviewInst);

            if (iview.isCanvasAvailable) {
                
                this.context2D = document.createElement('canvas').getContext('2d');
                this.redrawPreview = document.createElement('canvas');
                jQuery(this.redrawPreview).css("position" ,"fixed");
                this.activateCanvas = false;
                this.lastFrame = new Date();
                this.updateCanvasCount = 0;
                this.preView = new Image();
                this.lastPosX = 0;
                this.lastPosY = 0;
                this.lastZoomLevel = -1;
                this.damagedArea = new Array();
                this.notLoadedTile = new Array();
                var that = this;

                PanoJS.prototype.assignTileImageOrig = PanoJS.prototype.assignTileImage;
                PanoJS.prototype.assignTileImage = function cv_assignTileImage() {
                    that.assignTileImage(arguments[0]);
                };

                PanoJS.prototype.resizeOrig = PanoJS.prototype.resize;
                PanoJS.prototype.resize = function cv_resize() {
                    that.context2D.canvas.width = that.getViewer().viewerBean.width;
                    that.context2D.canvas.height = that.getViewer().viewerBean.height;
                    that.getViewer().viewerBean.resizeOrig();
                };

                PanoJS.prototype.switchDisplayModeOrig = PanoJS.prototype.switchDisplayMode;
                PanoJS.prototype.switchDisplayMode = function cv_switchDisplayMode() {
                    return that.switchDisplayMode(arguments[0], arguments[1], arguments[2], arguments[3]);
                };

                PanoJS.prototype.updateScreenOrig = PanoJS.prototype.updateScreen;
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
            var scope = this;
            var viewerBean = this.getViewer().viewerBean;
            var zoomLevel = viewerBean.zoomLevel;
            var moveVector = this.calculateMoveVector();
            var moveOutOfScreen = this.isMoveOutOfScreen(moveVector);
            var zoomLevelChanged = this.zoomLevelChanged(this.lastZoomLevel);
            this.updateCanvasCount = 0;
            this.lastPosX = viewerBean.x;
            this.lastPosY = viewerBean.y;
            this.refreshImageDimensions();
            this.redrawPreview.width = this.context2D.canvas.width;
            this.redrawPreview.height = this.context2D.canvas.height;
            if (zoomLevelChanged || moveOutOfScreen) {
                this.clearCanvas();
                jQuery(scope).trigger(iview.Canvas.BEFORE_DRAW_EVENT);
                this.drawPreview();
                this.clearDamagedArea();
                this.drawArea({
                    "x" : viewerBean.x,
                    "y" : viewerBean.y,
                    "w" : viewerBean.width,
                    "h" : viewerBean.height
                });
            } else {
                this.moveCanvas(moveVector);
                jQuery(scope).trigger(iview.Canvas.BEFORE_DRAW_EVENT);
                var moveDamagedArea = this.calculateDamagedArea(moveVector);
                var damagedAreas = this.damagedArea.concat(moveDamagedArea);
                this.clearDamagedArea();

                var currentArea;

                while (currentArea = damagedAreas.pop()) {
                    this.drawArea(currentArea);
                    var ctxx = this.redrawPreview.getContext('2d');
                    ctxx.lineWidth = 3;
                    ctxx.fillStyle = "black";
                    ctxx.strokeRect(currentArea.x - viewerBean.x, currentArea.y - viewerBean.y, currentArea.w, currentArea.h);
                }
            }
            this.lastZoomLevel = zoomLevel;
            jQuery(scope).trigger(iview.Canvas.AFTER_DRAW_EVENT);
            
        };

        constructor.prototype.zoomLevelChanged= function cv_zoomLevelChanged(lastZoomLevel) {
            var zoomLevel =  this.getViewer().viewerBean.zoomLevel;
            var hasChanged = lastZoomLevel != zoomLevel;
            return hasChanged;
        }
        

        //                    ;

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
            while (this.damagedArea.pop())
                ;
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
         *            the area that should be drawn
         */
        constructor.prototype.drawArea = function cv_drawArea(area) {
            if(!this.isAreaInBean(area)) return;

            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var old = 0;
            var tileSize = viewerBean.tileSize;// currentImage.zoomInfo.scale;
            var originTileSize = this.getViewer().properties.tileSize;
            var scale = currentImage.zoomInfo.scale;
            var curWidth = this.getCurrentWidth();
            var curHeight = this.getCurrentHeight();

            var sizeX = curWidth;
            var sizeY = curHeight;

            var areaStartX, areaStartY, areaEndX, areaEndY;

            areaStartX = Math.max(area.x - viewerBean.x, 0);
            areaStartY = Math.max(area.y - viewerBean.y, 0);
            areaEndX = Math.min((areaStartX + area.w), sizeX);
            areaEndY = Math.min((areaStartY + area.h), sizeY);

            var xOffset = area.x;
            var yOffset = area.y;

            var xTileOffset = Math.floor(xOffset / tileSize);
            var yTileOffset = Math.floor(yOffset / tileSize);

            // calculate border tiles
            var startBorderTileSizeX, startBorderTileSizeY, endBorderTileSizeX, endBorderTileSizeY;

            startBorderTileSizeX = Math.min(Math.min(tileSize - (xOffset % tileSize), areaEndX),sizeX);
            startBorderTileSizeY = Math.min(Math.min(tileSize - (yOffset % tileSize), areaEndY),sizeY);

            endBorderTileSizeX = (areaEndX - areaStartX - startBorderTileSizeX) % tileSize;
            endBorderTileSizeY = (areaEndY - areaStartY - startBorderTileSizeY) % tileSize;

            var columnCount = Math.floor((areaEndX - areaStartX - startBorderTileSizeX - endBorderTileSizeX) / tileSize);
            var rowCount = Math.floor((areaEndY - areaStartY - startBorderTileSizeY - endBorderTileSizeY) / tileSize);

            if (startBorderTileSizeX > 0)
                columnCount++;
            if (endBorderTileSizeX > 0)
                columnCount++;
            if (startBorderTileSizeY > 0)
                rowCount++;
            if (endBorderTileSizeY > 0)
                rowCount++;

            for ( var currentX = 0; currentX < columnCount; currentX++) {
                var dx, dw, sx, sw;
                sx = 0;
                if (currentX == 0) {
                    dx = areaStartX;
                    dw = startBorderTileSizeX;
                    sw = Math.min(Math.floor(dw / scale), originTileSize);
                    sx = originTileSize - sw;

                    if (currentX == columnCount - 1) {
                        sx = Math.floor((xOffset % tileSize));
                        if (xOffset + tileSize > curWidth) {
                            sw = curWidth - xOffset;
                        }
                    }
                } else if (currentX == columnCount - 1) {
                    dx = areaStartX + startBorderTileSizeX + (tileSize * (currentX - 1))
                    dw = endBorderTileSizeX;
                    sw = Math.floor(dw / scale);
                } else {
                    dx = areaStartX + startBorderTileSizeX + (tileSize * (currentX - 1));
                    dw = tileSize;
                    sw = originTileSize;
                }

                for ( var currentY = 0; currentY < rowCount; currentY++) {
                    var dy, dh, sy, sh;
                    sy = 0;
                    if (currentY == 0) {
                        dy = areaStartY;
                        dh = startBorderTileSizeY;
                        sh = Math.min(Math.floor(dh / scale), originTileSize);
                        sy = originTileSize - sh;

                        if (currentY == rowCount - 1) {
                            sy = Math.floor((yOffset % tileSize) / scale);
                            if (yOffset + tileSize > curHeight) {
                                sh = curHeight - yOffset;
                            }
                        }
                    } else if (currentY == rowCount - 1) {
                        dy = areaStartY + startBorderTileSizeY + (tileSize * (currentY - 1));
                        dh = endBorderTileSizeY;
                        sh = Math.min(Math.floor(dh / scale), originTileSize);
                    } else {
                        dy = areaStartY + startBorderTileSizeY + (tileSize * (currentY - 1));
                        dh = tileSize;
                        sh = originTileSize;
                    }
                    try {
                        this.assignTileImage(xTileOffset + currentX, yTileOffset + currentY, sx, sy, sw, sh, dx, dy, dw, dh);
                        //this.context2D.strokeRect(areaStartX, areaStartY, area.w, area.h);
                    } catch (err) {
                        console.log({
                            "Error while drawing Tiles " : err,
                            "Params" : {
                                "col" : (xTileOffset + currentX),
                                "row" : (yTileOffset + currentY),
                                "sx" : sx,
                                "sy" : sy,
                                "sw" : sw,
                                "sh" : sh,
                                "dx" : dx,
                                "dy" : dy,
                                "dw" : dw,
                                "dh" : dh,
                                "curWidth" : curWidth,
                                "curHeight" : curHeight,
                                "cvnWidth" : this.getCurrentCanvasWidth(),
                                "cvnHeight" : this.getCurrentCanvasHeight(),
                                "area" : area
                            }
                        });
                    }

                }
            }
            
            if(this.notLoadedTile.lenght != 0){
                var ctx = this.context2D;
                ctx.save();
                ctx.beginPath();
                ctx.strokeStyle = "#a00";
                var current;
                while(current = this.notLoadedTile.pop()){
                    ctx.rect(current.x, current.y, current.w, current.h);
                }
                //ctx.stroke();
                ctx.clip();
                this.drawPreview();
                ctx.restore();
                
            }
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

        constructor.prototype.assignTileImage = function cv_assignTileImage(tileX, tileY, sx, sy, sw, sh, dx, dy, dw, dh) {
            var viewerBean = this.getViewer().viewerBean;
            var tileImgId = viewerBean.tileUrlProvider.assembleUrl(tileX, tileY, viewerBean.zoomLevel);
            var tileImg = viewerBean.cache.getItem(tileImgId);
            
            
            if (tileImg == null ) {
                tileImg = this.createImageTile(tileImgId, true ,viewerBean, tileX, tileY);
            }
            
            if(typeof tileImg.loaded == "undefined"){
                this.notLoadedTile.push({"x" : dx, "y" : dy, "w" : dw, "h" : dh});
            }

            
            
            if (tileImg.loaded) {
                // make sure there no floating point coords(performance)
                sx = this.floatToInt(sx);
                sy = this.floatToInt(sy);
                sw = this.floatToInt(sw);
                sh = this.floatToInt(sh);
                dx = this.floatToInt(dx);
                dy = this.floatToInt(dy);
                dw = this.floatToInt(dw);
                dh = this.floatToInt(dh);

                // draws the Tile only direct if it is already loaded
                this.context2D.drawImage(tileImg, sx, sy, sw, sh, dx, dy, dw, dh);
                
                var ctxx = this.redrawPreview.getContext('2d');
                ctxx.lineWidth = 1;
                ctxx.strokeStyle = "blue";
                ctxx.strokeRect(dx, dy, dw, dh);
            }
        };
        constructor.prototype.createImageTile = function cv_createImageTile(tileImgId, createCache,viewerBean, tx, ty) {
            var tileImg = new Image();
            var viewer = this.getViewer();
            var viewerBean = viewer.viewerBean;
            viewerBean.cache.setItem(tileImgId, tileImg);
            var that = this;
            var zoomLevelOnLoad = this.getViewer().viewerBean.zoomLevel;
            tileImg.onload = function cv_tileOnLoad(){
                var imgScope = this;
                //setTimeout(function() { // to simulate bad connection
                    imgScope.loaded = true;    
                    
                    if(that.zoomLevelChanged(zoomLevelOnLoad)){
                        console.log("das zoomLevel hat sich geändert!");
                        return;
                    }
                    
                    var tileSize = viewerBean.tileSize;
                    var scale = that.getViewer().currentImage.zoomInfo.scale;
                    var bx = viewerBean.x, by = viewerBean.y;
                    var dw = imgScope.width * scale;
                    var dh = imgScope.height * scale;
                    var dx = (Math.floor(tx * tileSize) )- bx ;
                    var dy = (Math.floor(ty * tileSize) )- by ;
                    
                    if(!that.isAreaInBean({"x" : dx + bx, "y" : dy + by, "w" : dw, "h" : dh})){
                        console.log("das tile ist ausserhalb des viewerbean!");
                        //return;
                    }

                    
                    
                    console.log("Zeichne tile dx: " + dx + " dy:" + dy + " dw:" + dw + " dh:" + dh);
                    that.context2D.drawImage(tileImg, dx, dy, dw, dh);    
                    var ctxx = that.redrawPreview.getContext('2d');
                    ctxx.lineWidth = 1;
                    ctxx.strokeStyle = "red";
                    ctxx.strokeRect(dx, dy, dw, dh);
                //}, 2000);
            };

            tileImg.src = tileImgId;
            return tileImg;
        };
        
        constructor.prototype.createCoordObject = function cv_createCoordObject(sx, sy, sw, sh, dx, dy, dw, dh){
           return {"sx" : sx, "sy" : sy, "sw" : sw, "sh" : sh,"dx" : dx,"dy" : dy,"dw" : dw,"dh" : dh};
        }

        constructor.prototype._drawFpsBox = function cv_drawDebugBox(fps) {
            this.context2D.fillStyle = "Rgb(204,204,0)";
            this.context2D.fillRect(0, this.context2D.canvas.height - 15, 20, 15);
            this.context2D.fillStyle = "Rgb(0,0,0)";
            this.context2D.fillText(Math.round(fps), 0, this.context2D.canvas.height - 3);
        };

        constructor.prototype.getCurrentWidth = function cv_getCurrentWidth() {
            return this.getViewer().currentImage.curWidth;
        };

        constructor.prototype.getCurrentHeight = function cv_getCurrentHeight() {
            return this.getViewer().currentImage.curHeight;
        };

        constructor.prototype.getCurrentCanvasWidth = function cv_getCurrentCanvasWidth() {
            return this.context2D.canvas.width;
        };

        constructor.prototype.getCurrentCanvasHeight = function cv_getCurrentCanvasHeight() {
            return this.context2D.canvas.height;
        };

        constructor.prototype.getXOffset = function cv_getXOffset(areaStartX, areaEndX) {
            return this.getViewer().viewerBean.x + areaStartX;
        };
        constructor.prototype.getYOffset = function cv_getYOffset(areaStartY, areaEndY) {
            return this.getViewer().viewerBean.y + areaStartY;
        };
        constructor.prototype.getPreviewX = function cv_getPreviewX() {
            return -this.getViewer().viewerBean.x;
        };
        constructor.prototype.getPreviewY = function cv_getPreviewY() {
            return -this.getViewer().viewerBean.y;
        };

        /**
         * Draws the Preview Image to the right Position.
         */
        constructor.prototype.drawPreview = function cv_drawPreview() {
            if(!this.preView.loaded) return;
            var viewerBean = this.getViewer().viewerBean;
            var currentImage = this.getViewer().currentImage;
            var x = this.getPreviewX(), y = this.getPreviewY();
            var w = this.getCurrentWidth();
            var h = this.getCurrentHeight();
            this.context2D.drawImage(this.preView, x, y, w, h);
        };

        constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping) {
            var viewerBean = this.getViewer().viewerBean; 
            var temp = viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);

            if (this.getViewer().viewerContainer.isMax() && !this.activateCanvas) {
                this.activateCanvas = true;
                this.context2D.canvas.y = this.context2D.canvas.x = 0;
                this.appendCanvas();

                //make sure that the preview image is already loaded before drawing on canvas
                var that = this;
                that.preView.onload = function() {
                    that.preView.loaded=true;
                    that.getViewer().viewerBean.positionTiles();
                };
                that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;
                this.getViewer().context.container.find(".well").find(".preload")[0].style.display = "none";

                jQuery(this.getViewer().currentImage).bind(iview.CurrentImage.CHANGE_EVENT, function() {
                    that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;
                    that.lastZoomLevel = -1;
                });

                this.context2D.canvas.width = viewerBean.width;
                this.context2D.canvas.height = viewerBean.height;

            }

            return temp;
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
            this.getViewer().context.container.find(".well").prepend(this.context2D.canvas);//before,prepend,append
            this.getViewer().context.container.find(".well").prepend(this.redrawPreview);
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
})();
