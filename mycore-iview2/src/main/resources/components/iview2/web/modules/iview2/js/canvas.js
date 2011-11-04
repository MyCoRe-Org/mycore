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
				
				this.activateCanvas = false;				
				this.lastFrame = new Date();
				this.updateCanvasCount = 0;
				this.preView = new Image();
				var that = this;				
					  
				PanoJS.prototype.assignTileImageOrig = PanoJS.prototype.assignTileImage;
				PanoJS.prototype.assignTileImage = function cv_assignTileImage() {
					that.assignTileImage(arguments[0]);
				};
				
				PanoJS.prototype.positionTilesOrig = PanoJS.prototype.positionTiles;
				PanoJS.prototype.positionTiles = function cv_positionTiles() {
					that.positionTiles(arguments[0],arguments[1]);
				};
				
				PanoJS.prototype.releaseOrig = PanoJS.prototype.release;
				PanoJS.prototype.release = function cv_release() {
					that.release(arguments[0]);
				};
				
				PanoJS.prototype.resizeOrig = PanoJS.prototype.resize;
				PanoJS.prototype.resize = function cv_resize() {
					that.resize();
				};
				
				PanoJS.prototype.resolveCoordinatesOrig = PanoJS.prototype.resolveCoordinates;
				PanoJS.prototype.resolveCoordinates = function cv_resolveCoordinates() {
					return that.resolveCoordinates(arguments[0]);
				};
				
				PanoJS.prototype.switchDisplayModeOrig = PanoJS.prototype.switchDisplayMode;
				PanoJS.prototype.switchDisplayMode = function cv_switchDisplayMode() {
					return that.switchDisplayMode(arguments[0],arguments[1],arguments[2],arguments[3]);
				};	
				
				PanoJS.prototype.zoomOrig = PanoJS.prototype.zoom;
				PanoJS.prototype.zoom = function cv_zoom() {
					that.zoom(arguments[0]);
				};
				
				PanoJS.prototype.zoomViewerOrig = PanoJS.prototype.zoomViewer;
				PanoJS.prototype.zoomViewer = function cv_zoomViewer() {
					that.zoomViewer(arguments[0]);
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
		
		constructor.prototype.release = function cv_release(coords){
			this.getViewer().viewerBean.activate(false);
			var motion = {
				'x' : (coords.x - this.getViewer().viewerBean.mark.x),
				'y' : (coords.y - this.getViewer().viewerBean.mark.y)
			};
			this.getViewer().viewerBean.mark = { 'x' : 0, 'y' : 0 };			
		};
		
		constructor.prototype.resolveCoordinates = function cv_resolveCoordinates(e){	
			if(this.getViewer().currentImage.rotation == 0){
				return {
					'x' : this.getViewer().viewerBean.x + e.clientX - this.context2D.canvas.offsetLeft,//relativ: e.clientX - this.context2D.canvas.offsetLeft
					'y' : this.getViewer().viewerBean.y + e.clientY - this.context2D.canvas.offsetTop
				}	
			}
			else if(this.getViewer().currentImage.rotation == 90){
				return {
					'x' : (this.getViewer().viewerBean.x + e.clientY + this.context2D.canvas.offsetTop),
					'y' : (this.getViewer().viewerBean.y - e.clientX - this.context2D.canvas.offsetLeft)
				}	
			}
			else if(this.getViewer().currentImage.rotation == 180){
				return {
					'x' : this.getViewer().viewerBean.x - e.clientX + this.context2D.canvas.offsetLeft,
					'y' : this.getViewer().viewerBean.y - e.clientY + this.context2D.canvas.offsetTop
				}
			}
			else if(this.getViewer().currentImage.rotation == 270){
				return {
					'x' : (this.getViewer().viewerBean.x - e.clientY - this.context2D.canvas.offsetTop),
					'y' : (this.getViewer().viewerBean.y + e.clientX + this.context2D.canvas.offsetLeft)
				}
			}
		};
	
		constructor.prototype.zoom = function cv_zoom(direction){	
			
			if (this.getViewer().viewerBean.zoomLevel + direction < 0) {
				if (PanoJS.MSG_BEYOND_MIN_ZOOM) {
					alert(PanoJS.MSG_BEYOND_MIN_ZOOM);
				}
				return;
			}
			else if (this.getViewer().viewerBean.zoomLevel + direction > this.getViewer().viewerBean.maxZoomLevel) {
				if (PanoJS.MSG_BEYOND_MAX_ZOOM) {
					alert(PanoJS.MSG_BEYOND_MAX_ZOOM);
				}
				return;
			}

			this.getViewer().viewerBean.blank();
			this.clearCanvas();
				
			var coordsBefore = {'x' : this.getViewer().viewerBean.x, 'y' :  this.getViewer().viewerBean.y,
								'width' : this.getViewer().currentImage.curWidth, 'height' :  this.getViewer().currentImage.curHeight
			};
			
			//increase/decrease zoomlevel
			this.getViewer().viewerBean.zoomLevel += direction;

			//calculate new dimensions
			if(this.getViewer().currentImage.rotation == 0 || this.getViewer().currentImage.rotation == 180){
				this.getViewer().currentImage.curWidth = Math.floor((this.getViewer().currentImage.width / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale);			
				this.getViewer().currentImage.curHeight = Math.floor((this.getViewer().currentImage.height / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale); 
			}
	
			var coordsAfter = {'width' : this.getViewer().currentImage.curWidth, 'height' :  this.getViewer().currentImage.curHeight};
			
			//case 1: image fits into screen (before and after), no scrolling - x and y remain 0
			//case 2: image gets bigger (x and y) than viewerBean, scrolling possible, try to center the picture (depends on viewerBeanSize and movement)
			//!!!case 3: image gets bigger in just one direction, scrolling only this axis, - what should happen?Center visible area or center the picture?
			//this.getViewer().viewerBean.x = (100.0 / coordsBefore.width * coordsBefore.x) * (coordsAfter.width/100.0);
							
			if(coordsBefore.width > this.getViewer().viewerBean.width){
				var viewerXCenter = Math.floor(this.getViewer().viewerBean.width / 2); 
				var xCenter = coordsBefore.x + viewerXCenter;				
				var diffX = Math.floor(100.0 / coordsBefore.width * xCenter);			
				this.getViewer().viewerBean.x = Math.floor(diffX * (coordsAfter.width/100.0)) - viewerXCenter;
				//this.getViewer().viewerBean.x = Math.ceil((100.0 / coordsBefore.width * coordsBefore.x) * (coordsAfter.width/100.0));
			}
			else if(coordsAfter.width > this.getViewer().viewerBean.width){
				//user didn't moved the viewer, center the picture
				if(this.getViewer().viewerBean.x == 0){
					this.getViewer().viewerBean.x = Math.floor(((coordsAfter.width/100.0)*50.0) - (this.getViewer().viewerBean.width/2.0));
				}
				
			}else{
				this.getViewer().viewerBean.x = 0;
			}
			
			
			if(coordsBefore.height > this.getViewer().viewerBean.height){
				var viewerYCenter = Math.floor(this.getViewer().viewerBean.height / 2); 
				var yCenter = coordsBefore.y + viewerYCenter;//current viewport center			
				var diffY = Math.floor(100.0 / coordsBefore.height * yCenter);//percentage of current viewport				
				this.getViewer().viewerBean.y = Math.floor(diffY * (coordsAfter.height/100.0)) - viewerYCenter;				
			}
			else if(coordsAfter.height > this.getViewer().viewerBean.height){
				//user didn't moved the viewer, center the picture
				if(this.getViewer().viewerBean.y == 0){
					this.getViewer().viewerBean.y = Math.floor(((coordsAfter.height/100.0)*50.0) - (this.getViewer().viewerBean.height/2.0));
				}
				
			}else{
				this.getViewer().viewerBean.y = 0;
			}
			
			if(this.activateCanvas){
				this.rotate(true);
			}
			else{
				this.getViewer().viewerBean.prepareTiles();
			}
			
			this.getViewer().viewerBean.notifyViewerZoomed();

		};
		
		constructor.prototype.zoomViewer = function cv_zoomViewer(direction){
			var dir = -1;
			if(direction && this.getViewer().viewerBean.zoomLevel != this.getViewer().currentImage.zoomInfo.maxZoom) {
				dir = 1;
			}
			
			//if zoomWidth or zoomScreen was active and we're already in the max zoomlevel just reset the displayMode
			if (this.getViewer().currentImage.zoomInfo.zoomScreen) {
				this.getViewer().viewerBean.pictureScreen(true);
			}
			else if (this.getViewer().currentImage.zoomInfo.zoomWidth) {
				this.getViewer().viewerBean.pictureWidth(true);
			}

			this.getViewer().viewerBean.zoom(dir);
		};
		
		constructor.prototype.resize = function cv_resize(){			
			// IE fires a premature resize event
			if (!this.getViewer().viewerBean.initialized) {
				return;
			}

	        var newWidth = this.getViewer().viewerBean.offsetWidth;
	        var newHeight = this.getViewer().viewerBean.offsetHeight;

			this.getViewer().viewerBean.clear();

			var before = {
				'x' : Math.floor(this.getViewer().viewerBean.width / 2),
				'y' : Math.floor(this.getViewer().viewerBean.height / 2)
			};

			this.context2D.canvas.width = this.getViewer().viewerBean.width;
			this.context2D.canvas.height = this.getViewer().viewerBean.height;	

			this.getViewer().viewerBean.prepareTiles();

			var after = {
				'x' : Math.floor(this.getViewer().viewerBean.width / 2),
				'y' : Math.floor(this.getViewer().viewerBean.height / 2)
			};

			this.getViewer().viewerBean.x += (after.x - before.x);
			this.getViewer().viewerBean.y += (after.y - before.y);
			
			this.getViewer().viewerBean.positionTiles();
			this.getViewer().viewerBean.initialized = true;
			this.getViewer().viewerBean.notifyViewerMoved();

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

				var iview = this.getViewer();
				
				//Plus <-> Minus				
				var xMove = this.getViewer().viewerBean.x - motion.x; 
				var xViewerBorder = this.getViewer().currentImage.curWidth - this.getViewer().viewerBean.width;
				var yMove = this.getViewer().viewerBean.y - motion.y; 
				var yViewerBorder = this.getViewer().currentImage.curHeight - this.getViewer().viewerBean.height;
				
				if(this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270){
					xViewerBorder = this.getViewer().currentImage.curWidth - this.getViewer().viewerBean.height;
					yViewerBorder = this.getViewer().currentImage.curHeight - this.getViewer().viewerBean.width;
				}				
				
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
			//uses negative values because canvas starts at 0,0 and drawing starts there
			var viewerBean = this.getViewer().viewerBean;
			var x = -viewerBean.x;
			var y = -viewerBean.y;			
			this.context2D.drawImage(this.preView, x, y, this.getViewer().currentImage.curWidth, this.getViewer().currentImage.curHeight);
		};
		
		constructor.prototype._updateCanvas = function cv__updateCanvas(){
			var rect = {};

			rect.x = this.getViewer().viewerBean.x;
			rect.y = this.getViewer().viewerBean.y;
		
			var tileSize = this.getViewer().viewerBean.tileSize;
			var curWidth = this.getViewer().currentImage.curWidth;
			var curHeight = this.getViewer().currentImage.curHeight;			
			
			//check if rotated and axis are inverted
			if(this.smallerThanCanvas() == false && (this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270)){
				var old = curWidth;
				curWidth = curHeight;
				curHeight = old;
			}
			
			var xDim = Math.min(this.context2D.canvas.width, curWidth);
			var yDim = Math.min(this.context2D.canvas.height,curHeight);
			
			//DON'T swap canvas dimensions directly because it would erase the preview
			if(this.smallerThanCanvas() == false && (this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270)){
				if(xDim == this.context2D.canvas.width){
					var old = xDim;
					xDim = yDim;
					yDim = old;
				}
			}			
			
			var xoff = rect.x%tileSize;
			var yoff = rect.y%tileSize;
 
			var xTiles = Math.ceil((xDim+xoff) / tileSize);
			var yTiles = Math.ceil((yDim+yoff) / tileSize);
			
			if(this.smallerThanCanvas() == false && (this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270)){
				var old = curWidth;
				curWidth = curHeight;
				curHeight = old;
			}
			
			var imgXTiles = Math.ceil(curWidth/tileSize);
			var imgYTiles = Math.ceil(curHeight/tileSize);
						
			//xstart, ystart Kacheln
			var startx = Math.floor(rect.x/tileSize);
			var starty = Math.floor(rect.y/tileSize);
			
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
										
					this.assignTileImage(tile);					
				}			
			}
		};
		
		constructor.prototype.updateScreen = function cv_updateScreen(render){
			var scope = this;
			if(!render){
				if (this.updateCanvasCount++ == 0){
					//mozRequestAnimationFrame has no return value as of FF7, tracking first call to method via updateCanvasCount
					requestAnimationFrame(function(){scope.updateScreen(true);}, this.context2D.canvas);
				}
				return;
			}

			this.updateCanvasCount = 0;
			this.drawPreview();
			this._updateCanvas();

			if (false){
				var curTime = new Date();
				var difference = curTime.getTime() - this.lastFrame.getTime();
				this._drawFpsBox(1000/difference);
				this.lastFrame = curTime;
			}
		};			
		
		constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping){				
				var temp = this.getViewer().viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);				
				if(this.getViewer().viewerContainer.isMax() && this.activateCanvas != true ){
					this.activateCanvas = true;
					this.appendCanvas();
					
					//make sure that the preview image is already loaded before drawing on canvas
					var that = this;	
					that.preView.onload = function(){
						that.positionTiles();
					};
					that.preView.src = this.getViewer().context.container.find(".preload")[0].firstChild.src;					
					this.getViewer().context.container.find(".well").find(".preload").remove();		
					
					this.context2D.canvas.width = this.getViewer().viewerBean.width;
					this.context2D.canvas.height = this.getViewer().viewerBean.height;		
					
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
			return !!((this.getViewer().currentImage.curWidth <= this.getViewer().viewerBean.width && this.getViewer().currentImage.curHeight <= this.getViewer().viewerBean.height) && (this.getViewer().currentImage.curHeight <= this.getViewer().viewerBean.width && this.getViewer().currentImage.curWidth <= this.getViewer().viewerBean.height));
		};
		
		constructor.prototype.rotate = function cv_rotate(zoomCaller){			
			
			this.clearCanvas();  				
			this.getViewer().currentImage.curWidth = Math.ceil((this.getViewer().currentImage.width / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale);
			this.getViewer().currentImage.curHeight = Math.ceil((this.getViewer().currentImage.height / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale); 

			
			var rows = Math.ceil(this.getViewer().viewerBean.height / this.getViewer().viewerBean.tileSize) + 1;
			var cols = Math.ceil(this.getViewer().viewerBean.width / this.getViewer().viewerBean.tileSize) + 1;
			
			if(zoomCaller === undefined){//rotate if caller isn't zoom
				this.getViewer().currentImage.rotation = (this.getViewer().currentImage.rotation + 90 >= 360) ? 0 : this.getViewer().currentImage.rotation + 90;
			}
			
			if(this.getViewer().currentImage.rotation != 0){		
				
				//picture is small enough to fit in the canvas area
				if(this.smallerThanCanvas()){					
					var moveXAxis = 0, moveYAxis = 0;					
					switch (this.getViewer().currentImage.rotation){
				  		case 90:  	moveYAxis = -this.getViewer().currentImage.curHeight;
				  					break;
				  		case 180:	moveYAxis = -this.getViewer().currentImage.curHeight;
				  					moveXAxis = -this.getViewer().currentImage.curWidth;
				  					break;	  				
				  		case 270:	moveXAxis = -this.getViewer().currentImage.curWidth;
				  					break;
					}
					this.context2D.rotate(this.getViewer().currentImage.rotation * (Math.PI / 180));	
				  	this.context2D.translate(moveXAxis,moveYAxis);
					
				}else{
					//swap dimensions						
					this.context2D.translate(this.context2D.canvas.width/2, this.context2D.canvas.height/2);				
					this.context2D.rotate(this.getViewer().currentImage.rotation * (Math.PI / 180));

					if(this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270){
						this.context2D.translate( -this.context2D.canvas.height/2, -this.context2D.canvas.width/2);
					}else{
						this.context2D.translate( -this.context2D.canvas.width/2, -this.context2D.canvas.height/2);
					}
					
					if(this.getViewer().currentImage.rotation == 90 || this.getViewer().currentImage.rotation == 270){
						var old = rows;
						rows = cols;
						cols = rows;
					}
				}
			}
			//reset\refill tile-array
			this.getViewer().viewerBean.tiles = [];
			for (var c = 0; c < cols; c++) {
				var tileCol = [];
				for (var r = 0; r < rows; r++) {
					var tile = {
						'element' : null,
						'posx' : 0,
						'posy' : 0,
						'xIndex' : c,
						'yIndex' : r,
						'qx' : c,
						'qy' : r
					};

					tileCol.push(tile);
				}
			
				this.getViewer().viewerBean.tiles.push(tileCol);
			}			
			this.positionTiles();
		};
		 
		return constructor;
	})();

})();
