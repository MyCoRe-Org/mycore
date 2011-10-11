(function() {
	"use strict";
	
	iview.isCanvasAvailable = (function(){
		return !!document.createElement("canvas").getContext;
	})();
	

	iview.Canvas = (function() {
		//just an info:
		//this.getViewer() = ViewerInstance
		//this.getViewer().viewerBean = PanoJS

		function constructor(iviewInst) {				
			iview.IViewObject.call(this, iviewInst);	
			
			if (iview.isCanvasAvailable) {
				
				this.context2D = document.createElement('canvas').getContext('2d');
				this.buffer2D = document.createElement('canvas').getContext('2d');
				
				this.activateCanvas = false;				
				this.lastFrame = new Date();
				this.updateCanvasCount = 0;
				this.preView = new Image();
				var that = this;				
					  
				PanoJS.prototype.assignTileImageOrig = PanoJS.prototype.assignTileImage;
				PanoJS.prototype.assignTileImage = function cv_assignTileImage() {
					that.assignTileImage(arguments[0]);
				};
				
				PanoJS.prototype.zoomOrig = PanoJS.prototype.zoom;
				PanoJS.prototype.zoom = function cv_zoom() {
					that.zoom(arguments[0]);
				};
				  
				PanoJS.prototype.positionTilesOrig = PanoJS.prototype.positionTiles;
				PanoJS.prototype.positionTiles = function cv_positionTiles() {
					that.positionTiles(arguments[0],arguments[1]);
				};
				
				PanoJS.prototype.switchDisplayModeOrig = PanoJS.prototype.switchDisplayMode;
				PanoJS.prototype.switchDisplayMode = function cv_switchDisplayMode() {
					return that.switchDisplayMode(arguments[0],arguments[1],arguments[2],arguments[3]);
				};			
									  
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
									e.data.canvasInstance.rotate();
								}
							}
						});
					});
				});
			}
		}		
		
		// inheritance(copy) from prototype iview
		constructor.prototype = Object.create(iview.IViewObject.prototype);
		
		// overwritten
		constructor.prototype.assignTileImage = function cv_assignTileImage(tile, canvas){
			
			var viewerBean=this.getViewer().viewerBean;
			
			var tileImgId = viewerBean.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, viewerBean.zoomLevel);
			var tileImg = viewerBean.cache[tileImgId];
			
			// create cache if not exist - zoom/y/x
			if (tileImg == null) {
				tileImg=this.createImageTile(tileImgId, tile, viewerBean);
			}
			if (tileImg.loaded){
				canvas.drawImage(tileImg, tile.posx, tile.posy, tile.width, tile.height);
				//console.log("img drawed "+tileImgId);
			}
		};
		
		constructor.prototype.createImageTile=function cv_createImageTile(tileImgId, tile, viewerBean){
			var tileImg = new Image();
			viewerBean.cache[tileImgId] = tileImg;
			var that = this;
			tileImg.onload = function cv_tileLoaded(){
//				console.log("img loaded "+tileImgId);
				this.loaded = true;
				that.updateScreen();
			};
			tileImg.src = tileImgId
			return tileImg;
		};
		
		constructor.prototype.zoom = function cv_zoom(direction){
			this.getViewer().viewerBean.prepareTiles();
			this.clearCanvas();
			this.getViewer().viewerBean.zoomOrig(direction);
		};

		constructor.prototype._drawFpsBox = function cv_drawDebugBox(fps){
			this.context2D.fillStyle = "Rgb(204,204,0)";
			this.context2D.fillRect(0, this.context2D.canvas.height - 15, 20, 15);
			this.context2D.fillStyle = "Rgb(0,0,0)";
			this.context2D.fillText(Math.round(fps), 0, this.context2D.canvas.height-3);
		};
		
		constructor.prototype.positionTiles = function cv_positionTiles(motion, reset){		
			
			if(this.activateCanvas)
			{					
				//calculate and add the motion difference to the absolute coordinates	
				// default to no motion
				if (typeof motion == 'undefined') {
					motion = { 'x' : 0, 'y' : 0 };
				}

				//fixed movement while mouse is still pressed
				this.getViewer().viewerBean.mark.x += motion.x; 
				this.getViewer().viewerBean.mark.y += motion.y;

				var iview = this.getViewer();
				this.getViewer().currentImage.curWidth = Math.ceil((this.getViewer().currentImage.width / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale);
				this.getViewer().currentImage.curHeight = Math.ceil((this.getViewer().currentImage.height / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale); 

				//Plus <-> Minus
				var xMove = this.getViewer().viewerBean.x - motion.x; 
				var xViewerBorder = this.getViewer().currentImage.curWidth - this.getViewer().viewerBean.width;
				var yMove = this.getViewer().viewerBean.y - motion.y; 
				var yViewerBorder = this.getViewer().currentImage.curHeight - this.getViewer().viewerBean.height;
				
				if(xViewerBorder > 0){//testen, ob das Bild überhaupt in der aktuellen Zoomstufe in die Richtung bewegt werden kann
					if (xMove >  xViewerBorder && xViewerBorder > 0) { //max. Randbegrenzung
						this.getViewer().viewerBean.x = xViewerBorder;
						motion.x = 0;
					}
					else if(xMove > 0){//normal
						this.getViewer().viewerBean.x -= motion.x;
					}		
					else if(this.getViewer().viewerBean.x < 0 || xMove < 0){//min. Randbegrenzung
						this.getViewer().viewerBean.x = motion.x = 0;
					}
				}else{
					this.getViewer().viewerBean.x = motion.x = 0;
				}
				
				if(yViewerBorder > 0){
					if (yMove > yViewerBorder && yViewerBorder > 0) {
						this.getViewer().viewerBean.y = yViewerBorder;
						motion.y = 0;
					}	
					else if(yMove > 0){
						this.getViewer().viewerBean.y -= motion.y;
					}
					else if(this.getViewer().viewerBean.y < 0 || yMove < 0){
						this.getViewer().viewerBean.y = motion.y = 0;
					}
				}else{
					this.getViewer().viewerBean.y = motion.y = 0;
				}		

						
				if((xViewerBorder > 0 || yViewerBorder > 0) || (motion.x == 0 && motion.y == 0)){
					this.updateScreen();		
				}
				
			}
		};
		
		constructor.prototype.drawPreview = function cv_drawPreview(){
			var viewerBean = this.getViewer().viewerBean;
			var x = -viewerBean.x;
			var y = -viewerBean.y;
			this.context2D.drawImage(this.preView, x, y, this.getViewer().currentImage.curWidth, this.getViewer().currentImage.curHeight);
		};
		
		constructor.prototype._updateCanvas = function cv__updateCanvas(){
			var rect = {};
			var bufferCanvas=this.buffer2D;
			rect.x = this.getViewer().viewerBean.x;
			rect.y = this.getViewer().viewerBean.y;
			
			var tileSize = this.getViewer().viewerBean.tileSize;
			var curWidth = this.getViewer().currentImage.curWidth;
			var curHeight = this.getViewer().currentImage.curHeight;
			var xDim = Math.min(bufferCanvas.canvas.width, curWidth);
			var yDim = Math.min(bufferCanvas.canvas.height,curHeight);
			
			var xoff = rect.x%tileSize;
			var yoff = rect.y%tileSize;
 
			var xTiles = Math.ceil((xDim+xoff) / tileSize);
			var yTiles = Math.ceil((yDim+yoff) / tileSize);
			
			var imgXTiles = Math.ceil(curWidth/tileSize);
			var imgYTiles = Math.ceil(curHeight/tileSize);
						
			//xstart, ystart Kacheln
			var startx = Math.floor(rect.x/tileSize);
			var starty = Math.floor(rect.y/tileSize);
			
			//iterate through these tiles and assign them to the backbuffer
			for(var row = 0; row < yTiles; row++){
				for(var column = 0; column < xTiles; column++){
					
					//get the associated tiles
					var tile = this.getViewer().viewerBean.tiles[column][row];
					tile.xIndex = column + startx;
					tile.yIndex = row + starty;
					tile.width=(tile.xIndex == imgXTiles-1)? curWidth - tile.xIndex * tileSize : tileSize; 
					tile.height=(tile.yIndex == imgYTiles-1)? curHeight - tile.yIndex * tileSize: tileSize;

					tile.posx = column * tileSize - xoff;
					tile.posy = row * tileSize - yoff;
										
					this.assignTileImage(tile, this.context2D);					
				}			
			}
		};
		
		constructor.prototype.updateScreen = function cv_updateScreen(render){
			var scope=this;
			if(!render){
				//console.log("skipped");
				if (this.updateCanvasCount++==0){
					//mozRequestAnimationFrame has no return value as of FF7, tracking first call to method via updateCanvasCount
					//console.log("scheduled");
					requestAnimFrame(function(){scope.updateScreen(true);}, this.context2D.canvas);
				}
				return;
			}
			//console.log("draw");
			this.updateCanvasCount=0;
			this.drawPreview();
			this._updateCanvas();
			//console.log(this.context2D.canvas);
			if (false){
				var curTime = new Date();
				var difference = curTime.getTime() - this.lastFrame.getTime();
				this._drawFpsBox(1000/difference);
				this.lastFrame=curTime;
			}
		};			
		
		constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping){				
				var temp = this.getViewer().viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);				
				if(!this.getViewer().viewerContainer.isMax()){
					this.context2D.canvas.width = this.getViewer().context.container.find(".preload")[0].clientWidth;
					this.context2D.canvas.height = this.getViewer().context.container.find(".preload")[0].clientHeight;
				}else if(this.activateCanvas != true ){
					this.activateCanvas = true;
					this.appendCanvas();
					
					//sichergehen,dass das Previewbild bereits geladen ist, bevor die Tiles auf das Canvas kommen
					var that = this;	
					that.preView.onload = function(){
						that.positionTiles();
					};
					that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;
					
					this.getViewer().context.container.find(".well").find(".preload").remove();		
					this.buffer2D.canvas.width = this.context2D.canvas.width = this.getViewer().viewerBean.width;
					this.buffer2D.canvas.height = this.context2D.canvas.height = this.getViewer().viewerBean.height;		
				}
				
				return temp;
		};
		// own
		constructor.prototype.clearCanvas = function cv_clearCanvas(){
			this.buffer2D.canvas.width = this.buffer2D.canvas.width;
			this.context2D.canvas.width = this.context2D.canvas.width;
		};
		
		constructor.prototype.appendCanvas = function cv_appendCanvas(){
			this.getViewer().context.container.find(".well").prepend(this.context2D.canvas);//before,prepend,append
		};
		
		constructor.prototype.rotate = function cv_rotate(){
			
			//this.getViewer().context.container.find(".preload")[0].hidden = true;
			/*this.clearCanvas();
			
			this.getViewer().currentImage.rotation = (this.getViewer().currentImage.rotation + 90 >= 360) ? 0 : this.getViewer().currentImage.rotation + 90;
			var oldVal = this.getViewer().viewerBean.width;
			this.getViewer().viewerBean.width = this.getViewer().viewerBean.height;
			this.getViewer().viewerBean.height = oldVal;

			if (this.getViewer().currentImage.rotation == 0 || this.getViewer().currentImage.rotation == 180)
			{				  
				this.resizeCanvas(this.getViewer().context.container.find(".preload")[0].clientWidth, this.getViewer().context.container.find(".preload")[0].clientHeight);
			}
			else
			{				  
				this.resizeCanvas(this.getViewer().context.container.find(".preload")[0].clientHeight, this.getViewer().context.container.find(".preload")[0].clientWidth);
			}
		  
			this.context2D.rotate(this.getViewer().currentImage.rotation * (Math.PI / 180));
		  
			//x,y axis rotates with canvas 
			var moveXAxis = 0, moveYAxis = 0;
			switch (this.getViewer().currentImage.rotation){
		  		case 90:  	moveYAxis =- this.context2D.canvas.width;
		  					break;
		  				
		  		case 180:	moveYAxis =- this.context2D.canvas.height;
		  					moveXAxis =- this.context2D.canvas.width;
		  					break;
		  				
		  		case 270:	moveXAxis =- this.context2D.canvas.height;
		  					break;
		  	}
		  
		  	//move zero point
		  	this.context2D.translate(moveXAxis,moveYAxis);
		  	
		  	//repaint 
		  	this.getViewer().viewerBean.clear();
		  	this.getViewer().viewerBean.prepareTiles();*/
		};
		
		constructor.prototype.resizeCanvas = function cv_resizeCanvas(width, height){			
			/*
			var w = width, h = height;
			if(width === undefined) {
				w = this.getViewer().context.container.find(".preload")[0].clientWidth;
			}
			if(height === undefined) {
				h = this.getViewer().context.container.find(".preload")[0].clientHeight;
			}

			switch(this.getViewer().currentImage.rotation) {
				case 0: case 180: this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.width?this.getViewer().viewerBean.width:w);
			 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.height?this.getViewer().viewerBean.height:h);
			 					  break;
			 	case 90:case 270: this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.height?this.getViewer().viewerBean.height:w);
			 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.width?this.getViewer().viewerBean.width:h);
			 					  break;
			}
			
			this.buffer2D.canvas.width  = this.context2D.canvas.width;
			this.buffer2D.canvas.height = this.context2D.canvas.height;
			*/
		};
		
		
		return constructor;
	})();

})();
