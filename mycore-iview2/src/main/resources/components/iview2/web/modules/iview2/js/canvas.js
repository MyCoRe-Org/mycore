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
		
		constructor.prototype.release = function cv_release(coords){//avoid applying the movement twice
			var viewerBean = this.getViewer().viewerBean;
			viewerBean.activate(false);
			viewerBean.mark = { 'x' : 0, 'y' : 0 };	
		};
		
		constructor.prototype.resolveCoordinates = function cv_resolveCoordinates(e){	
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			
			switch(currentImage.rotation){
				case 0:				
					return {
						'x' : viewerBean.x + e.clientX - this.context2D.canvas.offsetLeft,//relativ: e.clientX - this.context2D.canvas.offsetLeft
						'y' : viewerBean.y + e.clientY - this.context2D.canvas.offsetTop
					}	
				case 90:
					return {
						'x' : viewerBean.x + e.clientY + this.context2D.canvas.offsetTop,
						'y' : viewerBean.y - e.clientX - this.context2D.canvas.offsetLeft
					}	
				case 180:
					return {
						'x' : viewerBean.x - e.clientX + this.context2D.canvas.offsetLeft,
						'y' : viewerBean.y - e.clientY + this.context2D.canvas.offsetTop
					}
				case 270:
					return {
						'x' : viewerBean.x - e.clientY - this.context2D.canvas.offsetTop,
						'y' : viewerBean.y + e.clientX + this.context2D.canvas.offsetLeft
					}
			}
		};
	
		constructor.prototype.zoom = function cv_zoom(direction){		
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			
			if (viewerBean.zoomLevel + direction < 0) {
				if (PanoJS.MSG_BEYOND_MIN_ZOOM) {
					alert(PanoJS.MSG_BEYOND_MIN_ZOOM);
				}
				return;
			}
			else if (viewerBean.zoomLevel + direction > viewerBean.maxZoomLevel) {
				if (PanoJS.MSG_BEYOND_MAX_ZOOM) {
					alert(PanoJS.MSG_BEYOND_MAX_ZOOM);
				}
				return;
			}

			viewerBean.blank();
			
			var oldX = viewerBean.x;
			var oldY = viewerBean.y;
			
			var dimensionsBefore = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};	
			viewerBean.zoomLevel += direction;
			viewerBean.notifyViewerZoomed();			
			var dimensionsAfter = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};
								
			viewerBean.x = this.getNewViewerPosition(viewerBean.width, oldX, dimensionsBefore.width, dimensionsAfter.width);
			viewerBean.y = this.getNewViewerPosition(viewerBean.height, oldY, dimensionsBefore.height, dimensionsAfter.height);			

			this.prepareTiles();
			this.positionTiles();
		};
		
		constructor.prototype.getNewViewerPosition = function(dimensionMax, position, valueBefore, valueAfter){
			//case 1: image fits into screen (before and after), no scrolling - x and y remain 0
			//case 2: image gets bigger (x and y) than viewerBean, scrolling possible, try to center the picture (depends on viewerBeanSize and movement)
			//case 3: image gets bigger in just one direction, scrolling only this axis, - what should happen?Center visible area or center the picture?				
			var newPos = 0;
			if(valueBefore > dimensionMax){
				var dimensionCenter = Math.floor(dimensionMax / 2); 
				var dimensionViewPortCenter = position + dimensionCenter;//current viewport center					
				var dimensionDifference = Math.floor(100.0 / valueBefore * dimensionViewPortCenter);//percentage of current viewport			
				newPos = Math.floor(dimensionDifference * (valueAfter/100.0)) - dimensionCenter;
			}
			else if(valueAfter > dimensionMax){
				//user didn't moved the viewer, center the picture
				if(position == 0){
					newPos = Math.floor(((valueAfter/100.0)*50.0) - (dimensionMax / 2.0));
				}				
			}			
			if(newPos < 0)
				newPos = 0;
			return newPos;			
		};
		
		constructor.prototype.zoomViewer = function cv_zoomViewer(direction){
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			var dir = -1;
			
			if(direction && viewerBean.zoomLevel != currentImage.zoomInfo.maxZoom) {
				dir = 1;
			}
			
			//if zoomWidth or zoomScreen was active and we're already in the max zoomlevel just reset the displayMode
			if (currentImage.zoomInfo.zoomScreen) {
				viewerBean.pictureScreen(true);
			}
			else if (currentImage.zoomInfo.zoomWidth) {
				viewerBean.pictureWidth(true);
			}

			viewerBean.zoom(dir);
		};
		
		constructor.prototype.resize = function cv_resize(){	
			var viewerBean = this.getViewer().viewerBean;
			
			// IE fires a premature resize event
			if (!viewerBean.initialized) {
				return;
			}

	        var newWidth = viewerBean.offsetWidth;
	        var newHeight = viewerBean.offsetHeight;

			viewerBean.clear();

			var before = {
				'x' : Math.floor(viewerBean.width / 2),
				'y' : Math.floor(viewerBean.height / 2)
			};

			this.context2D.canvas.width = viewerBean.width;
			this.context2D.canvas.height = viewerBean.height;	

			viewerBean.prepareTiles();

			var after = {
				'x' : Math.floor(viewerBean.width / 2),
				'y' : Math.floor(viewerBean.height / 2)
			};

			viewerBean.x += (after.x - before.x);
			viewerBean.y += (after.y - before.y);
			
			viewerBean.positionTiles();
			viewerBean.initialized = true;
			viewerBean.notifyViewerMoved();

		};

		constructor.prototype._drawFpsBox = function cv_drawDebugBox(fps){
			this.context2D.fillStyle = "Rgb(204,204,0)";
			this.context2D.fillRect(0, this.context2D.canvas.height - 15, 20, 15);
			this.context2D.fillStyle = "Rgb(0,0,0)";
			this.context2D.fillText(Math.round(fps), 0, this.context2D.canvas.height-3);
		};
				
		constructor.prototype.positionTiles = function cv_positionTiles(motion, reset){	
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			
			if(this.activateCanvas)
			{					
				//calculate and add the motion difference to the absolute coordinates	
				// default to no motion
				if (typeof motion == 'undefined') {
					motion = { 'x' : 0, 'y' : 0 };
				}
								
				var xMove = viewerBean.x - motion.x; 
				var xViewerBorder = currentImage.curWidth - viewerBean.width;
				var yMove = viewerBean.y - motion.y; 
				var yViewerBorder = currentImage.curHeight - viewerBean.height;		
				
				if(xViewerBorder > 0){//testen, ob das Bild überhaupt in der aktuellen Zoomstufe in die Richtung bewegt werden kann
					if (xMove >  xViewerBorder && xViewerBorder > 0) { //max. Randbegrenzung
						viewerBean.x = xViewerBorder;
					}
					else if(xMove > 0){//normal
						viewerBean.x -= motion.x;
					}		
					else if(viewerBean.x < 0 || xMove < 0){//min. Randbegrenzung
						viewerBean.x = 0;
					}
				}else{
					viewerBean.x = 0;
				}
				
				if(yViewerBorder > 0){
					if (yMove > yViewerBorder && yViewerBorder > 0) {
						viewerBean.y = yViewerBorder;
					}	
					else if(yMove > 0){
						viewerBean.y -= motion.y;
					}
					else if(viewerBean.y < 0 || yMove < 0){
						viewerBean.y = 0;
					}
				}else{
					viewerBean.y = 0;
				}
				//console.log(viewerBean.x + " " + viewerBean.y);
				this.updateScreen();		
			}
		};
		
		constructor.prototype.drawPreview = function cv_drawPreview(){
			//uses negative values because canvas starts at 0,0
			var viewerBean = this.getViewer().viewerBean;
			var currentImage = this.getViewer().currentImage;
			var x = -viewerBean.x, y = -viewerBean.y;			
			/*if(!this.bothAxisBigger()){
				x = 0;
				y = 0;		
			}*/
			var w = Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curWidth :  currentImage.curHeight);
			var h = Math.floor((currentImage.rotation == 0 || currentImage.rotation == 180) ? currentImage.curHeight :  currentImage.curWidth);			
			this.context2D.drawImage(this.preView, x, y, w, h);
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
 
			//number of visible tiles
			var xTiles = Math.ceil((xDim+xoff) / tileSize);
			var yTiles = Math.ceil((yDim+yoff) / tileSize);
			
			//border tiles
			var imgXTiles = Math.ceil(curWidth/tileSize);
			var imgYTiles = Math.ceil(curHeight/tileSize);
						
			//xstart, ystart 
			var startx = Math.floor(rect.x/tileSize);
			var starty = Math.floor(rect.y/tileSize);
			
			for(var row = 0; row < yTiles; row++){
				for(var column = 0; column < xTiles; column++){
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
		};			
		
		constructor.prototype.switchDisplayMode = function cv_switchDisplayMode(screenZoom, stateBool, preventLooping){		
			var viewerBean = this.getViewer().viewerBean;
			var temp = viewerBean.switchDisplayModeOrig(screenZoom, stateBool, preventLooping);	
			
			if(this.getViewer().viewerContainer.isMax() && this.activateCanvas != true ){
				this.activateCanvas = true;
				this.context2D.canvas.y = this.context2D.canvas.x = 0;
				this.appendCanvas();
				
				//make sure that the preview image is already loaded before drawing on canvas
				var that = this;	
				that.preView.onload = function(){
					that.positionTiles();
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
		
		constructor.prototype.prepareTiles = function cv_prepareTiles(){	
			var viewerBean = this.getViewer().viewerBean;
			
			var rows = Math.ceil(viewerBean.height / viewerBean.tileSize) + 1;
			var cols = Math.ceil(viewerBean.width / viewerBean.tileSize) + 1;	
			
			viewerBean.tiles = [];
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
				viewerBean.tiles.push(tileCol);
			}			
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
			
			//((CASE ZERO)) rotate full pic and move it so the user didn't notice it	
			if(degree){
					var moveXAxis = 0, moveYAxis = 0;						
					if(!this.bothAxisBigger())//kleiner, Zwischenstufe, fit-to...
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
						
						this.context2D.rotate(degree * (Math.PI / 180));	
					  	this.context2D.translate(moveXAxis, moveYAxis);
					}
					else
					{//größer
						this.context2D.translate(Math.ceil(this.context2D.canvas.width/2), Math.ceil(this.context2D.canvas.height/2));				
						this.context2D.rotate(this.getViewer().currentImage.rotation * (Math.PI / 180));
						
						switch(degree){
						case 90:	//var diff = currentImage.curWidth - (viewerBean.width + viewerBean.y);//check whether there is any place below 									
									//moveYAxis = -viewerBean.width;//-Math.min(currentImage.curWidth, viewerBean.width) - diff;//without diff drawing error (start-> zoom in-> rotate)				
									
									//this.context2D.translate( -this.context2D.canvas.height/2, -this.context2D.canvas.width/2);
									this.context2D.translate( -this.context2D.canvas.width/2, -this.context2D.canvas.height/2);
									break;
				  		case 180:	//moveYAxis = -viewerBean.height;//- viewerBean.y ?
				  					//moveXAxis = -viewerBean.width;//- viewerBean.x ?
				  					//this.context2D.translate( -this.context2D.canvas.width/2, -this.context2D.canvas.height/2);
				  					break;
				  		case 270:	/*this.context2D.translate( -this.context2D.canvas.height/2, -this.context2D.canvas.width/2);
				  					moveXAxis = -Math.min(currentImage.curWidth, viewerBean.width);*/
				  					break;
						}
					}			
			}
		};
		
		//rotate90degree does everything which belongs to an image-roation
		constructor.prototype.rotate90degree = function cv_rotate90degree(){	
			var currentImage = this.getViewer().currentImage;
			currentImage.rotation = (currentImage.rotation + 90 >= 360) ? 0 : currentImage.rotation + 90;	
			this.flipImageDimensions();
			this.flipViewerDimensions();
			this.prepareTiles();
			this.positionTiles();
		};
		
		return constructor;
	})();

})();
