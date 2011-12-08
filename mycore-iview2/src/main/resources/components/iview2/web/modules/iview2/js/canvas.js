(function() {
	"use strict";
	
	iview.isCanvasAvailable = (function(){
		return !!document.createElement("canvas").getContext;
	})();
	

	iview.Canvas = (function() {

		function constructor(iviewInst) {				
			iview.IViewObject.call(this, iviewInst);	
			
			if (iview.isCanvasAvailable) {
				
				this.context2D = document.createElement('canvas').getContext('2d');
				
		        this.activateCanvas = false;               
		        this.lastFrame = new Date();
		        this.updateCanvasCount = 0;
		        this.preView = new Image();
		        this.pixelModifier = {'red' : 0, 'green' : 0, 'blue' : 0};
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
					return that.switchDisplayMode(arguments[0],arguments[1],arguments[2],arguments[3]);
				};	
				
				PanoJS.prototype.updateScreenOrig = PanoJS.prototype.updateScreen;
				PanoJS.prototype.updateScreen = function cv_updateScreen(){
					that.updateScreen();
				}

				jQuery(document).bind("toolbarloaded", {canvasInstance: this}, function(e) {
					if (e.model.id != "mainTb") { 
						  return;
					}
					
					var toolbarModel = e.model;
					var i = toolbarModel.getElementIndex('spring');
						  
					var buttonSet = new ToolbarButtonsetModel("rotation");
					var button = new ToolbarButtonModel("rotateRight", { 'type' : 'buttonDefault' }, { 'label' : "Rotate right", 'text' : false, 'icons' : { primary : 'paperClip-icon' } },
														"Rotate right", true, false); 
					toolbarModel.addElement(buttonSet, i);
					buttonSet.addButton(button); // attach to events of view
					jQuery.each(e.getViews(), function(index, view) {
						jQuery(view).bind("press", {canvasInstance: e.data.canvasInstance}, function(e, args) { 
							if (args.parentName == buttonSet.elementName)
							{
								if(args.elementName == button.elementName)
								{
									e.data.canvasInstance.rotate90degree();
								}
							}
						});
					});
				});
			}
		}		
		
		// inheritance(copy) from prototype iview
		constructor.prototype = Object.create(iview.IViewObject.prototype);
		
		constructor.prototype.assignTileImage = function cv_assignTileImage(tile){			
			var viewerBean = this.getViewer().viewerBean;			
			var tileImgId = viewerBean.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, viewerBean.zoomLevel);
			var tileImg = viewerBean.cache.getItem(tileImgId);			
			// create cache if not exist - zoom/y/x
			if (tileImg == null) {
				tileImg = this.createImageTile(tileImgId, tile, viewerBean);
			}
			if (tileImg.loaded){
				this.context2D.drawImage(tileImg, tile.posx, tile.posy, tile.width, tile.height);
			}
		};
		
		constructor.prototype.createImageTile = function cv_createImageTile(tileImgId, tile, viewerBean){
			var tileImg = new Image();
			viewerBean.cache.setItem(tileImgId, tileImg);
			var that = this;
			tileImg.onload = function cv_tileLoaded(){
				this.loaded = true;
				that.updateScreen();
			};
			tileImg.src = tileImgId
			return tileImg;
		};
		
		constructor.prototype._drawFpsBox = function cv_drawDebugBox(fps){
			this.context2D.fillStyle = "Rgb(204,204,0)";
			this.context2D.fillRect(0, this.context2D.canvas.height - 15, 20, 15);
			this.context2D.fillStyle = "Rgb(0,0,0)";
			this.context2D.fillText(Math.round(fps), 0, this.context2D.canvas.height-3);
		};
		
		constructor.prototype.drawPreview = function cv_drawPreview(){
			//uses negative values because canvas starts at 0,0
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			var x = -viewerBean.x, y = -viewerBean.y;			
			var w = Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curWidth :  currentImage.curHeight);
			var h = Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curHeight :  currentImage.curWidth);			
			this.context2D.drawImage(this.preView, x, y, w, h);//das Bild wird komplett gezeichnet, dafür wird der Startpunkt ab die virtuelle viewerPosition angepasst
		};
		
		constructor.prototype._updateCanvas = function cv__updateCanvas(){			
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			
			var rect = {};
			var old = 0;
			var rotation = currentImage.rotation;
			rect.x = viewerBean.x;
			rect.y = viewerBean.y;
			var tileSize = viewerBean.tileSize;
			
			var curWidth = currentImage.curWidth;
			var curHeight = currentImage.curHeight;		
									
			var cnvWidth = this.context2D.canvas.width;
			var cnvHeight = this.context2D.canvas.height;
			if(rotation == 90 || rotation == 270){
				cnvWidth = this.context2D.canvas.height;
				cnvHeight = this.context2D.canvas.width;
				old = curWidth;
				curWidth = curHeight;
				curHeight = old;			
			}
			
			//determine how much space is needed to draw all necessary tiles
			var xDim = Math.min(cnvWidth, curWidth);
			var yDim = Math.min(cnvHeight, curHeight);
				
			//gap between first tile and border
			var xoff = rect.x%tileSize;
			var yoff = rect.y%tileSize;
 
			//number of visible tiles per axis
			var xTiles = Math.ceil((xDim+xoff) / tileSize);
			var yTiles = Math.ceil((yDim+yoff) / tileSize);
			
			//whole image-tile-set
			var imgXTiles = Math.ceil(curWidth/tileSize);
			var imgYTiles = Math.ceil(curHeight/tileSize);
						
			//xstart, ystart 
			var startx = Math.floor(rect.x/tileSize);
			var starty = Math.floor(rect.y/tileSize);
			
			for(var column = 0; column < xTiles; column++){
				for(var row = 0; row < yTiles; row++){
					//get the associated tiles					
					var tile = viewerBean.tiles[column][row];
					tile.xIndex = column + startx;
					tile.yIndex = row + starty;
					tile.width=(tile.xIndex == imgXTiles-1)? curWidth - tile.xIndex * tileSize : tileSize; 
					tile.height=(tile.yIndex == imgYTiles-1)? curHeight - tile.yIndex * tileSize: tileSize;

					tile.posx = column * tileSize - xoff;//first value could be < 0 because tile is not fully drawn (from origin at 0,0)
					tile.posy = row * tileSize - yoff;
			
					this.assignTileImage(tile);					
				}			
			}
		};
		
		constructor.prototype.updateScreen = function cv_updateScreen(render){
			if(this.activateCanvas){
				var scope = this;
				var currentImage = this.getViewer().currentImage;
				
				if(!render){
					if (this.updateCanvasCount++ == 0){
						//mozRequestAnimationFrame has no return value as of FF7, tracking first call to method via updateCanvasCount
						requestAnimationFrame(function(){scope.updateScreen(true);}, this.context2D.canvas);
					}
					return;
				}
				
				this.clearCanvas();
				this.refreshImageDimensions();
				this.rotateCanvas(currentImage.rotation);
				this.updateCanvasCount = 0;
				this.drawPreview();
				this._updateCanvas();
	
				if (false){
					var curTime = new Date();
					var difference = curTime.getTime() - this.lastFrame.getTime();
					this._drawFpsBox(1000/difference);
					this.lastFrame = curTime;
				}
			}
		};			
		
		constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping){		
			var viewerBean = this.getViewer().viewerBean;
			var temp = viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);	
			
			if(this.getViewer().viewerContainer.isMax() && !this.activateCanvas){
				this.activateCanvas = true;
				this.context2D.canvas.y = this.context2D.canvas.x = 0;
				this.appendCanvas();
				
				//make sure that the preview image is already loaded before drawing on canvas
				var that = this;	
				that.preView.onload = function(){
					that.getViewer().viewerBean.positionTiles();
				};
				that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;					
				this.getViewer().context.container.find(".well").find(".preload").remove();		
				
				this.context2D.canvas.width = viewerBean.width;
				this.context2D.canvas.height = viewerBean.height;		
				
			}
			
			return temp;
		};

		constructor.prototype.clearCanvas = function cv_clearCanvas(){
			this.context2D.canvas.width = this.context2D.canvas.width;
		};
		
		constructor.prototype.appendCanvas = function cv_appendCanvas(){
			this.getViewer().context.container.find(".well").prepend(this.context2D.canvas);//before,prepend,append
		};
		
		constructor.prototype.smallerThanCanvas = function cv_smallerThanCanvas(){
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			return !!((currentImage.curWidth <= viewerBean.width && currentImage.curHeight <= viewerBean.height) && (currentImage.curHeight <= viewerBean.width && currentImage.curWidth <= viewerBean.height));
		};
		
		constructor.prototype.refreshImageDimensions = function cv_refreshImageDimensions(){
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			currentImage.curWidth = Math.ceil((currentImage.width / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*currentImage.zoomInfo.scale);
			currentImage.curHeight = Math.ceil((currentImage.height / Math.pow(2, currentImage.zoomInfo.maxZoom - viewerBean.zoomLevel))*currentImage.zoomInfo.scale); 
		};
		
		constructor.prototype.flipImageDimensions = function cv_flipImageDimensions(){
			var currentImage = this.getViewer().currentImage;
			var old = currentImage.curHeight;
			currentImage.curHeight = currentImage.curWidth;
			currentImage.curWidth = old;
			old = currentImage.height;
			currentImage.height = currentImage.width;
			currentImage.width = old;
		};
		
		constructor.prototype.flipViewerDimensions = function cv_flipViewerDimensions(){
			var viewerBean = this.getViewer().viewerBean;
			var old = viewerBean.width;
			viewerBean.width = viewerBean.height;
			viewerBean.height = old;
		};
		
		constructor.prototype.getPosFromLowerLeft = function cv_getPosFromLowerLeft(axis, imageDimension, viewerDimension){
			var diff = imageDimension - axis;//gap between maxImageDimension and y 
			if(diff >= viewerDimension){//image bigger than viewer
				return Math.floor(imageDimension - (axis + viewerDimension));
			}
			return 0;			
		};
		
		constructor.prototype.setNewViewport = function cv_setNewViewport(){
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			var rotation = currentImage.rotation;			
			
			viewerBean.x = this.getPosFromLowerLeft(viewerBean.y, currentImage.curWidth, viewerBean.width);
			viewerBean.y = this.getPosFromLowerLeft(viewerBean.x, currentImage.curHeight, viewerBean.height);
			
			if(rotation == 0 || rotation == 180){
				var old = viewerBean.x;
				viewerBean.x = viewerBean.y;
				viewerBean.y = old;					
			}		
		};
		
		constructor.prototype.bothAxisBigger = function cv_bothAxisBigger(){
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			return !!((currentImage.curWidth > viewerBean.width && currentImage.curHeight > viewerBean.height)||
					(currentImage.curHeight > viewerBean.width && currentImage.curWidth > viewerBean.height));			
		};
		
		constructor.prototype.rotateCanvas = function cv_rotateCanvas(degree){		
			var currentImage = this.getViewer().currentImage;
			var viewerBean = this.getViewer().viewerBean;
			
			//degree = 0 -> nothing to translate and viewport doesn't change too
			if(degree){
					var moveXAxis = 0, moveYAxis = 0;						
					if(!this.bothAxisBigger())//kleiner, Zwischenstufe, fit-to-width
					{	//!!!prevent that the viewer moves to much (e.g. y is much bigger)
						switch (degree){
				  		case 90:	moveYAxis = -Math.min(currentImage.curWidth, viewerBean.height);//Höhe des Bildes oder Breite des Viewers
				  					break;
				  		case 180:	
				  					moveYAxis = -Math.min(currentImage.curHeight, viewerBean.height);
				  					moveXAxis = -Math.min(currentImage.curWidth, viewerBean.width);
				  					break;	  				
				  		case 270:	moveXAxis = -Math.min(currentImage.curHeight, viewerBean.width);//Breite des Bildes oder Höhe des Viewers				  					
				  					break;
						}
						viewerBean.x = viewerBean.y = 0;
					}
					else
					{//größer						
						switch(degree){
						case 90:	var diff = currentImage.curWidth - (viewerBean.y + viewerBean.width);//restlicher Höhenabstand
									moveYAxis = -viewerBean.width;			
									if(diff >= viewerBean.height - viewerBean.width){
										moveYAxis = -viewerBean.height;//maximale Verschiebung um Breite, auch wenn mehr möglich wäre
									}
									else{
										moveYAxis -= diff;//falls der Abstand kleiner ist, um soviel verschieben, wie möglich
									}
									break;
				  		case 180:	moveYAxis = -viewerBean.height;
				  					moveXAxis = -viewerBean.width;
				  					break;
				  		case 270:	moveXAxis = -viewerBean.width;
				  					break;
						}
					}			
					
					this.context2D.rotate(degree * (Math.PI / 180));	
				  	this.context2D.translate(moveXAxis, moveYAxis);
			}
		};
		
		//rotate90degree does everything which belongs to an image-roation
		constructor.prototype.rotate90degree = function cv_rotate90degree(){	
			var currentImage = this.getViewer().currentImage;
			var viewerBean = this.getViewer().viewerBean;
			
			currentImage.rotation = (currentImage.rotation + 90 >= 360) ? 0 : currentImage.rotation + 90;	
			this.flipImageDimensions();
			this.flipViewerDimensions();
			
			viewerBean.tiles = [];
			
			viewerBean.prepareTiles();
			viewerBean.positionTiles();
		};
		
		constructor.prototype.getPixelModifier = function cv_getPixelModifier(){
            return this.pixelModifier;           
        };
		       
        //value between 0 and 255
        constructor.prototype.setPixelModifier = function cv_setPixelModifier(red, green, blue){
            red = (red < 0) ? 0 : red;
            green = (green < 0) ? 0 : green;
            blue = (blue < 0) ? 0 : blue;
           
            red = (red > 255) ? 255 : red;
            green = (green > 255) ? 255 : green;
            blue = (blue > 255) ? 255 : blue;
           
            this.pixelModifier.red = red;
            this.pixelModifier.green = green;
            this.pixelModifier.blue = blue;
        };
		       
        constructor.prototype.changeCanvasColorIntensity = function	cv_changeCanvasColorIntensity(){
            var rotation = this.getViewer().currentImage.rotation;
            var cnvWidth = this.context2D.canvas.width;
            var cnvHeight = this.context2D.canvas.height;
                       
            if(rotation == 90 || rotation == 270){
                cnvHeight  = cnvWidth;
            }
           
            var canvasArea = this.context2D.getImageData(0, 0, cnvWidth, cnvHeight);
            var numberOfPixel = cnvWidth * cnvHeight * 4;
           
            for(var channel = 0; channel < numberOfPixel; channel += 4){
                canvasArea.data[channel]  += this.pixelModifier.red;
                canvasArea.data[channel+1] += this.pixelModifier.green;
                canvasArea.data[channel+2] += this.pixelModifier.blue;
            }
		           
            this.context2D.putImageData(canvasArea, 0, 0);
        };
        
        /*NOTE: to change color it's need to call these functions:
         * this.setPixelModifier(50, 0, 0);
         * this.changeCanvasColorIntensity();
         */
		
		return constructor;
	})();

})();
