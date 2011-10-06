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
				
				//this.switchActive = false;
				this.activateCanvas = false;
				var that = this;
					  
				PanoJS.prototype.assignTileImageOrig = PanoJS.prototype.assignTileImage;
				PanoJS.prototype.assignTileImage = function cv_assignTileImage() {
					that.assignTileImage(arguments[0]);
				};
				
				PanoJS.prototype.releaseOrig = PanoJS.prototype.release;
				PanoJS.prototype.release = function cv_release() {
					that.release(arguments[0]);
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
		constructor.prototype.assignTileImage = function cv_assignTileImage(tile){
			
			var tileImgId;
			var src;
			var tileImg;
			var that = this;
			
			tileImgId = src = this.getViewer().viewerBean.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, this.getViewer().viewerBean.zoomLevel);
			tileImg = this.getViewer().viewerBean.cache[tileImgId];
			
			// create cache if not exist - zoom/y/x
			if (tileImg == null) {
				tileImg = this.getViewer().viewerBean.cache[tileImgId] = this.getViewer().viewerBean.createPrototype(src);//zoom/y/x	
				tileImg.onload = function(){
					this.loaded = true;
					that.updateBackBuffer();
				};
				tileImg.src = tileImgId;			
			}
			if (tileImg.loaded){
				that.buffer2D.drawImage(tileImg, tile.posx, tile.posy, tile.width, tile.height);
			}
		}		
				
		constructor.prototype.release = function cv_release(coords){
			this.getViewer().viewerBean.activate(false);
			this.getViewer().viewerBean.mark = { 'x' : 0, 'y' : 0 };
		}
		
		constructor.prototype.zoom = function cv_zoom(direction){
			this.clearCanvas();
			this.getViewer().viewerBean.zoomOrig(direction);
		}
		
		constructor.prototype.requestAnimationFrame = (function() {
					return (
						function(callback, element) {
							return window.setTimeout(function(){callback(element);}, 1000 / 60);
						}
					);
		})();
		
		constructor.prototype.drawCanvasFromBuffer = function cv_drawCanvasFromBuffer(){
			this.context2D.drawImage(this.buffer2D.canvas, 0, 0);
			this.refreshBackBuffer = false;
			/*if (this.skipUpdate){
				delete this.skipUpdate;
				return;
			}*/
		}
		
		constructor.prototype.positionTiles = function cv_positionTiles(motion, reset){		
			
			if(this.activateCanvas)
			{					
				//calculate and add the motion difference to the absolute coordinates	
				// default to no motion
				if (typeof motion == 'undefined') {
					motion = { 'x' : 0, 'y' : 0 };
				}
				
				/*
				this.getViewer().currentImage.curWidth = (this.getViewer().currentImage.width / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale;
				this.getViewer().currentImage.curHeight = (this.getViewer().currentImage.height / Math.pow(2, this.getViewer().currentImage.zoomInfo.maxZoom - this.getViewer().viewerBean.zoomLevel))*this.getViewer().currentImage.zoomInfo.scale; 
				
				var iview = this.getViewer();

				var xEdge = Math.ceil(iview.currentImage.curWidth-this.context2D.canvas.width);
				var yEdge = Math.ceil(iview.currentImage.curHeight-this.context2D.canvas.height);				
				
				if(xEdge > 0){//check if its possible to move the image in that direction
					if(this.getViewer().viewerBean.x - motion.x < 0 || this.getViewer().viewerBean.x < 0 || !this.moved){//impossible or no movement
						this.getViewer().viewerBean.x = 0;
					}else{//valid movement
						if(this.getViewer().viewerBean.x - motion.x > xEdge || this.getViewer().viewerBean.x > xEdge){
							this.getViewer().viewerBean.x = xEdge;
						}else{
							this.getViewer().viewerBean.x -= motion.x;	
						}
					}
				}
				if(yEdge >0){
					if(this.getViewer().viewerBean.y - motion.y < 0 || this.getViewer().viewerBean.y < 0 || !this.moved){
						this.getViewer().viewerBean.y = 0;
					}else{
						if(this.getViewer().viewerBean.y - motion.y > yEdge || this.getViewer().viewerBean.y > yEdge){ 
							this.getViewer().viewerBean.y = yEdge;
						}else{
							this.getViewer().viewerBean.y -= motion.y;
						}
					}
				}
				*/
				
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
					this.getViewer().viewerBean.x = 0;
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
					this.getViewer().viewerBean.y = 0;
				}					
						
				if((xViewerBorder > 0 || yViewerBorder > 0) || (motion.x == 0 && motion.y == 0)){
					//this.context2D.drawImage(this.getViewer().context.container.find(".preload")[0].firstChild,0,0,this.getViewer().context.container.find(".preload")[0].clientWidth,this.getViewer().context.container.find(".preload")[0].clientHeight);
					this.context2D.drawImage(this.getViewer().context.container.find(".preload")[0].firstChild,0,0,this.getViewer().currentImage.curWidth,this.getViewer().currentImage.curHeight);
					this.updateBackBuffer();		
				}
				
			}
		}
		
		constructor.prototype.updateBackBuffer = function cv_updateBackBuffer(scope){
			
			if(scope === undefined || scope.refreshBackBuffer === undefined){
				scope = this;
			}			
			/*
			if(scope.refreshBackBuffer){
				this.backBufferHandle = this.backBufferHandle || scope.requestAnimationFrame(scope.updateBackBuffer, scope);
				return;
			}			
			scope.refreshBackBuffer = true;
			delete(this.backBufferHandle);*/
			
			var rect = {};						
			scope.buffer2D.canvas.width = scope.buffer2D.canvas.width;
			rect.x = scope.getViewer().viewerBean.x;
			rect.y = scope.getViewer().viewerBean.y;
			rect.width = scope.getViewer().viewerBean.width;
			rect.height = scope.getViewer().viewerBean.height;
			
			var tileSize = scope.getViewer().viewerBean.tileSize;
			var curWidth = scope.getViewer().currentImage.curWidth;
			var curHeight = scope.getViewer().currentImage.curHeight;
			var xDim = Math.min(rect.width, curWidth);
			var yDim = Math.min(rect.height,curHeight);
			
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
			for(var column = 0; column < xTiles; column++){
				for(var row = 0; row < yTiles; row++){
					
					//get the associated tiles
					var tile = scope.getViewer().viewerBean.tiles[column][row];
					tile.xIndex = column + startx;
					tile.yIndex = row + starty;
					tile.width=(tile.xIndex == imgXTiles-1)? curWidth - tile.xIndex * tileSize : tileSize; 
					tile.height=(tile.yIndex == imgYTiles-1)? curHeight - tile.yIndex * tileSize: tileSize;

					tile.posx = column * tileSize - xoff;
					tile.posy = row * tileSize - yoff;
										
					scope.assignTileImage(tile);					
				}			
			}
			
			scope.drawCanvasFromBuffer();
		}			
		
		constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping){				
				//this.switchActive = true;
				var temp = this.getViewer().viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);
				
				if(!this.getViewer().viewerContainer.isMax()){
					this.context2D.canvas.width = this.getViewer().context.container.find(".preload")[0].clientWidth;
					this.context2D.canvas.height = this.getViewer().context.container.find(".preload")[0].clientHeight;
				}else if(this.activateCanvas != true ){
					this.activateCanvas = true;
					this.appendCanvas();
					//this.getViewer().context.container.find(".well").find(".preload").remove();		
					this.buffer2D.canvas.width = this.context2D.canvas.width = this.getViewer().viewerBean.width;
					this.buffer2D.canvas.height = this.context2D.canvas.height = this.getViewer().viewerBean.height;
										
					this.positionTiles();
				}
				
				return temp;
		}
		// own
		constructor.prototype.clearCanvas = function cv_clearCanvas(){	
			this.context2D.canvas.width = this.context2D.canvas.width;
		}
		
		constructor.prototype.appendCanvas = function cv_appendCanvas(){
			this.getViewer().context.container.find(".well").prepend(this.context2D.canvas);//before,prepend,append
		}
		
		constructor.prototype.rotate = function cv_rotate(){
			/*
			this.getViewer().currentImage.rotation = (this.getViewer().currentImage.rotation + 90 >= 360) ? 0 : this.getViewer().currentImage.rotation + 90;
			var oldVal = this.getViewer().viewerBean.width;
			this.getViewer().viewerBean.width = this.getViewer().viewerBean.height;
			this.getViewer().viewerBean.height=oldVal;
		  
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
			var moveXAxis=0,moveYAxis=0;
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
		  	this.resizeRequired = false;
		  	this.getViewer().viewerBean.prepareTiles();
		  	*/
		}
		
		constructor.prototype.resizeCanvas = function cv_resizeCanvas(width, height){	
			/*
			console.warn("resizeCanvas()");
			this.skipUpdate=true;
			
			//if(this.refreshBackBuffer === undefined)
			//{
				
				var w=width, h=height;
				 if(width === undefined) {
					 w = this.getViewer().context.container.find(".preload")[0].clientWidth;
				 }
				 if(height === undefined) {
					 h = this.getViewer().context.container.find(".preload")[0].clientHeight;
				 }
				 if(!this.switchActive) {
					 switch(this.getViewer().currentImage.rotation) {
					 
					 	case 0: case 180: this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.width?this.getViewer().viewerBean.width:w);
					 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.height?this.getViewer().viewerBean.height:h);
					 					  break;
					 	case 90:case 270: this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.height?this.getViewer().viewerBean.height:w);
					 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.width?this.getViewer().viewerBean.width:h);
					 					  break;
					 }
				 }else {
					 this.switchActive = false;
					 switch(this.getViewer().currentImage.rotation) {
					 	case 0: case 180: this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.width?w:this.getViewer().viewerBean.width);
					 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.height?h:this.getViewer().viewerBean.height);
					 					  break;
					 	case 90: case 270:this.context2D.canvas.setAttribute('width',  w > this.getViewer().viewerBean.height?w:this.getViewer().viewerBean.height);
					 					  this.context2D.canvas.setAttribute('height', h > this.getViewer().viewerBean.width?h:this.getViewer().viewerBean.width);
					 					  break;
					 }
				 }
				 
				 this.buffer2D.canvas.width  = this.context2D.canvas.width;
				 this.buffer2D.canvas.height = this.context2D.canvas.height;
			//}
			this.updateBackBuffer();
			*/
		};
		
		
		return constructor;
	})();

})();
