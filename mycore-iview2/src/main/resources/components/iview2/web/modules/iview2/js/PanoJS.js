/**
 * Panoramic JavaScript Image Viewer (PanoJS) 1.0.2
 *
 * Generates a draggable and zoomable viewer for images that would
 * be otherwise too large for a browser window.  Examples would include
 * maps or high resolution document scans.
 *
 * Images must be precut into tiles, such as by the accompanying tilemaker.py
 * python library.
 *
 * <div class="viewer">
 *   <div class="well"><!-- --></div>
 *   <div class="surface"><!-- --></div>
 *   <div class="controls">
 *     <a href="#" class="zoomIn">+</a>
 *     <a href="#" class="zoomOut">-</a>
 *   </div>
 * </div>
 * 
 * The "well" node is where generated IMG elements are appended. It
 * should have the CSS rule "overflow: hidden", to occlude image tiles
 * that have scrolled out of view.
 * 
 * The "surface" node is the transparent mouse-responsive layer of the
 * image viewer, and should match the well in size.
 *
 * var viewerBean = new PanoJS(element, 'tiles', 256, 3, 1);
 *
 * To disable the image toolbar in IE, be sure to add the following:
 * <meta http-equiv="imagetoolbar" content="no" />
 *
 * Copyright (c) 2005 Michal Migurski <mike-gsv@teczno.com>
 *                    Dan Allen <dan.allen@mojavelinux.com>
 * 
 * Redistribution and use in source form, with or without modification,
 * are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Michal Migurski <mike-gsv@teczno.com>
 * @author Dan Allen <dan.allen@mojavelinux.com>
 *
 * NOTE: if artifacts are appearing, then positions include half-pixels
 * TODO: additional jsdoc and package jsmin
 * TODO: Tile could be an object
 */
function PanoJS(viewer, options) {

	if (typeof viewer == 'string') {
		this.viewer = document.getElementById(viewer);
	}
	else {
		this.viewer = viewer;
	}

	if (typeof options == 'undefined') {
		options = {};
	}

	if (typeof options.tileUrlProvider != 'undefined') {
		this.tileUrlProvider = options.tileUrlProvider;
	}
	else {
		this.tileUrlProvider = new PanoJS.TileUrlProvider(
			options.tileBaseUri ? options.tileBaseUri : PanoJS.TILE_BASE_URI,
			options.tilePrefix ? options.tilePrefix : PanoJS.TILE_PREFIX,
			options.tileExtension ? options.tileExtension : PanoJS.TILE_EXTENSION
		);
	}

	this.tileSize = (options.tileSize ? options.tileSize : PanoJS.TILE_SIZE);

	// assign and do some validation on the zoom levels to ensure sanity
	this.zoomLevel = (typeof options.initialZoom == 'undefined' ? -1 : parseInt(options.initialZoom));
	this.maxZoomLevel = (typeof options.maxZoom == 'undefined' ? 0 : Math.abs(parseInt(options.maxZoom)));
	if (this.zoomLevel > this.maxZoomLevel) {
		this.zoomLevel = this.maxZoomLevel;
	}

	this.initialPan = (options.initialPan ? options.initialPan : PanoJS.INITIAL_PAN);

	this.initialized = false;
	this.surface = null;
	this.well = null;
	this.width = 0;
	this.height = 0;
	this.top = 0;
	this.left = 0;
	this.x = 0;
	this.y = 0;
	this.border = -1;
	this.mark = { 'x' : 0, 'y' : 0 };
	this.pressed = false;
	this.tiles = [];
	//TODO maybe its possible to store all data which is kept in images in tiles
	this.images = [];
	//create Cache Object where the size is the amount of tiles which can be displayed at once plus one row/column on each site
	this.cache = new Cache(Math.ceil(screen.availWidth / this.tileSize) * Math.ceil(screen.availHeight / this.tileSize) * 4);
	var blankTile = options.blankTile ? options.blankTile : PanoJS.BLANK_TILE_IMAGE;
	var loadingTile = options.loadingTile ? options.loadingTile : PanoJS.LOADING_TILE_IMAGE;
	this.blankImg = new Image();
	this.blankImg.src = blankTile;
	if (blankTile != loadingTile) {
		this.loadingImg = new Image();
		this.loadingImg.src = loadingTile;
	} else {
		this.loadingImg = this.blankImg;
	}

	// employed to throttle the number of redraws that
	// happen while the mouse is moving
	this.moveCount = 0;
	this.inputHandlerEnabled=true;
	
	// add to viewer registry
	PanoJS.VIEWERS[PanoJS.VIEWERS.length] = this;
}

// project specific variables
PanoJS.PROJECT_NAME = 'PanoJS';
PanoJS.PROJECT_VERSION = '1.0.0';
PanoJS.REVISION_FLAG = '';

// CSS definition settings
PanoJS.SURFACE_STYLE_CLASS = 'surface';
PanoJS.WELL_STYLE_CLASS = 'well';
PanoJS.CONTROLS_STYLE_CLASS = 'controls'
PanoJS.TILE_STYLE_CLASS = 'tile';

// language settings
PanoJS.MSG_BEYOND_MIN_ZOOM = 'component.iview2.panojs.minzoom';
PanoJS.MSG_BEYOND_MAX_ZOOM = 'component.iview2.panojs.maxzoom';

// defaults if not provided as constructor options
PanoJS.TILE_BASE_URI = 'tiles';
PanoJS.TILE_PREFIX = 'tile-';
PanoJS.TILE_EXTENSION = 'jpg';
PanoJS.TILE_SIZE = 256;
PanoJS.BLANK_TILE_IMAGE = 'blank.gif';
PanoJS.LOADING_TILE_IMAGE = 'blank.gif';
PanoJS.INITIAL_PAN = { 'x' : .5, 'y' : .5 };
PanoJS.USE_LOADER_IMAGE = true;
//PanoJS.USE_SLIDE = true;
PanoJS.USE_KEYBOARD = true;

// performance tuning variables
PanoJS.MOVE_THROTTLE = 3;
//PanoJS.SLIDE_DELAY = 40;
//PanoJS.SLIDE_ACCELERATION_FACTOR = 5;

// the following are calculated settings
PanoJS.DOM_ONLOAD = (navigator.userAgent.indexOf('KHTML') >= 0 ? false : true);
PanoJS.GRAB_MOUSE_CURSOR = (navigator.userAgent.search(/KHTML|Opera/i) >= 0 ? 'pointer' : (document.attachEvent ? 'url(grab.cur)' : '-moz-grab'));
PanoJS.GRABBING_MOUSE_CURSOR = (navigator.userAgent.search(/KHTML|Opera/i) >= 0 ? 'move' : (document.attachEvent ? 'url(grabbing.cur)' : '-moz-grabbing'));

// registry of all known viewers
PanoJS.VIEWERS = [];

PanoJS.prototype = {
	/**
	 * @function
	 * @memberOf iview.General
	 * @name isInputHandlerEnabled
	 * @returns true if input events (keyboard, mouse) are captured
	 */
	isInputHandlerEnabled : function() {
	  return this.inputHandlerEnabled;
	},
	 
	/**
	 * @function
	 * @memberOf iview.General
	 * @name disableInputHandler
	 * @description disable input events
	 */
	disableInputHandler : function() {
	  this.inputHandlerEnabled = false;
	},
	 
	 /**
	 * @function
	 * @memberOf iview.General
	 * @name enableInputHandler
	 * @description enable input events
	 */
	enableInputHandler : function() {
	  this.inputHandlerEnabled = true;
	},


	init : function() {
		if (document.attachEvent) {
			document.body.ondragstart = function() { return false; }
		}
		
		if (this.width == 0 && this.height == 0) {
			this.width = this.viewer.offsetWidth;
			this.height = this.viewer.offsetHeight;
		}

		var fullSize = this.tileSize;
		// explicit set of zoom level
		if (this.zoomLevel >= 0 && this.zoomLevel <= this.maxZoomLevel) {
			fullSize = this.tileSize * Math.pow(2, this.zoomLevel);
		}
		// calculate the zoom level based on what fits best in window
		else {
			this.zoomLevel = -1;
			fullSize = this.tileSize / 2;
			do {
				this.zoomLevel += 1;
				fullSize *= 2;
			} while (fullSize < Math.max(this.width, this.height));
			// take into account picture smaller than window size
			if (this.zoomLevel > this.maxZoomLevel) {
				var diff = this.zoomLevel - this.maxZoomLevel;
				this.zoomLevel = this.maxZoomLevel;
				fullSize /= Math.pow(2, diff);
			}
		}

		// move top level up and to the left so that the image is centered
		this.x = Math.floor((fullSize - this.width) * -this.initialPan.x);
		this.y = Math.floor((fullSize - this.height) * -this.initialPan.y);

		// offset of viewer in the window
		for (var node = this.viewer; node; node = node.offsetParent) {
			this.top += node.offsetTop;
			this.left += node.offsetLeft;
		}

		for (var child = this.viewer.firstChild; child; child = child.nextSibling) {
			if (child.className == PanoJS.SURFACE_STYLE_CLASS) {
				this.surface = child;
				child.backingBean = this;
			}
			else if (child.className == PanoJS.WELL_STYLE_CLASS) {
				this.well = child;
				child.backingBean = this;
			}
			else if (child.className == PanoJS.CONTROLS_STYLE_CLASS) {
				for (var control = child.firstChild; control; control = control.nextSibling) {
					if (control.className) {
						control.onclick = PanoJS[control.className + 'Handler'];
					}
				}
			}
		}
		
		//moved from prepareTiles
		this.surface.onmousedown = PanoJS.mousePressedHandler;
		this.surface.onmouseup = this.surface.onmouseout = PanoJS.mouseReleasedHandler;
		this.surface.ondblclick = PanoJS.doubleClickHandler;
		if (PanoJS.USE_KEYBOARD) {
			document.onkeydown = PanoJS.keyboardHandler;
		}
		//end move
		this.viewer.backingBean = this;
		this.surface.style.cursor = PanoJS.GRAB_MOUSE_CURSOR;
		this.prepareTiles();
		this.initialized = true;
	},

	prepareTiles : function() {
		if(!(this.tileSize > 0) || typeof(input)=='number'){
			return; // dont throw exception
			throw new iview.IviewInstanceError("Invalid Tilesize :" + this.tileSize, this);
		}
		var rows = Math.ceil(this.height / this.tileSize) + 2;
		var cols = Math.ceil(this.width / this.tileSize) + 2;
		//if there's nothing to change don't drop anything as we may loose important references
		if (this.tiles.length == cols && this.tiles[0].length == rows) {
			return;
		}
		this.tiles = [];
		for (var c = 0; c < cols; c++) {
			var tileCol = [];

			for (var r = 0; r < rows; r++) {
				/**
				 * element is the DOM element associated with this tile
				 * posx/posy are the pixel offsets of the tile
				 * xIndex/yIndex are the index numbers of the tile segment
				 * qx/qy represents the quadrant location of the tile
				 */
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
		
			this.tiles.push(tileCol);
		}
	},

	/**
	 * Position the tiles based on the x, y coordinates of the
	 * viewer, taking into account the motion offsets, which
	 * are calculated by a motion event handler.
	 */
	positionTiles : function(motion, reset) {
		//calculate and add the motion difference to the absolute coordinates	
		// default to no motion
		if (typeof motion == 'undefined') {
			motion = { 'x' : 0, 'y' : 0 };
		}
		
		var xMove = this.iview.viewerBean.x - motion.x; 
		var yMove = this.iview.viewerBean.y - motion.y; 
		
		var viewerWidth   = this.iview.viewerBean.width;
		var viewerHeight  = this.iview.viewerBean.height;
		
		var viewerPosX = this.iview.viewerBean.x;
		var viewerPosY = this.iview.viewerBean.y;
		
		var imageWidth = this.iview.currentImage.curWidth;
		var imageHeight = this.iview.currentImage.curHeight;
		
		xMove = Math.min(xMove, imageWidth - viewerWidth);
		this.iview.viewerBean.x = Math.max(xMove, 0);
       
		yMove = Math.min(yMove, imageHeight - viewerHeight);
		this.iview.viewerBean.y = Math.max(yMove, 0);
		
		
		/*verschieben des Preload bildes damit man eine grobe Vorschau sieht von dem was kommt
		  wird nur ausgeführt wenn Seite geladen ist, da ansonsten die Eigenschaften noch nicht vorhanden sind*/
		if (this.iview.properties.initialized) {
			//folgende beide IF-Anweisungen für IE
			if(isNaN(this.x)) this.x = 0; 
			if(isNaN(this.y)) this.y = 0;
		}
		
		this.motion = motion;
		this.updateScreen();
		
		if(viewerPosX != this.iview.viewerBean.x || viewerPosY != this.iview.viewerBean.y){
			jQuery(this.viewer).trigger("move.viewer", {'x': this.iview.viewerBean.x, 'y': this.iview.viewerBean.y});
		}
		
	},
	
	updateScreen : function() {
		var that = this;
		
		var rect = {};
		var rotation = this.iview.currentImage.rotation;
		var zoomInfo = this.iview.currentImage.zoomInfo;
		rect.x = this.x;
		rect.y = this.y;
		var tileSize = this.tileSize;
		
		var curWidth = this.iview.currentImage.curWidth;
		var curHeight = this.iview.currentImage.curHeight;
		
		if (curWidth == 0 || curHeight == 0){
		  //image is not ready
		  return;
		}
					
		var bWidth = this.iview.viewerBean.width;
		var bHeight = this.iview.viewerBean.height;
		
		//determine how much space is needed to draw all necessary tiles
		var xDim = Math.min(bWidth, curWidth);
		var yDim = Math.min(bHeight, curHeight);
			
		//gap between first tile and border
		var xoff = rect.x%tileSize;
		var yoff = rect.y%tileSize;
		//border tiles
		var imgXTiles = Math.ceil(zoomInfo.dimensions[zoomInfo.curZoom].width/256);
		var imgYTiles = Math.ceil(zoomInfo.dimensions[zoomInfo.curZoom].height/256);
		//number of visible tiles
		var xTiles = Math.ceil((xDim+xoff) / tileSize);
		var yTiles = Math.ceil((yDim+yoff) / tileSize);
		if (zoomInfo.scale!=1){
		  //fitToWidth and fitToScreen
	      xTiles = Math.min(xTiles, imgXTiles);
	      yTiles = Math.min(yTiles, imgYTiles);
		}
		
		//xstart, ystart
		var startx = Math.floor(rect.x/tileSize);
		var starty = Math.floor(rect.y/tileSize);
		
		var dataName="inserted";
		var currentTime=new Date().getTime();

		for (var column = 0; column < xTiles; column++) {
			for (var row = 0; row < yTiles; row++) {
				var tile = this.tiles[column][row];
				//get the associated tiles
				tile.xIndex = column + startx;
				tile.yIndex = row + starty;
				tile.width=(tile.xIndex == imgXTiles-1)? curWidth - tile.xIndex * tileSize : tileSize; 
				tile.height=(tile.yIndex == imgYTiles-1)? curHeight - tile.yIndex * tileSize: tileSize;
				
				tile.posx = column * tileSize - xoff;
				tile.posy = row * tileSize - yoff;
				jQuery(this.assignTileImage(tile)).data(dataName,currentTime);
			}
		}
		//remove tiles which were needed earlier but for the current state are obsolete to display everything within the viewer
		jQuery("img."+PanoJS.TILE_STYLE_CLASS, this.well).each(function removeEdgeTiles(){
			if (jQuery(this).data(dataName)!=currentTime){
				//was not inserted or updated in the current run of updateScreen
				try {
					this.parentNode.removeChild(this);
				} catch (e) { log("Error while tile remove : " + e); }
			}
		});
	},
	
	/**
	 * Determine the source image of the specified tile based
	 * on the zoom level and position of the tile.  If forceBlankImage
	 * is specified, the source should be automatically set to the
	 * null tile image.  This method will also setup an onload
	 * routine, delaying the appearance of the tile until it is fully
	 * loaded, if configured to do so.
	 */
	assignTileImage : function(tile) {
		var tileURL = this.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, this.zoomLevel);
		var tileImg = this.cache.getItem(tileURL);
		// create cache if not exist
		if (tileImg == null){
			//create tileImg and store in cache
			tileImg = this.createPrototype(tileURL);
			this.cache.setItem(tileURL, tileImg, {'callback': function() {
				//drop tiles if they're no longer cached
				log("removing tileImg from cache"+tileImg.src);
				if (tileImg.parentNode!=null) {
					if (tileImg.parentNode.removeChild(tileImg)!=null){
						log("removed "+tileImg.src+" from cache");
					} else {
						log("error while removing "+tileImg.src+" from cache");
					};
				}
			}});
		}

		tileImg.style.top = tile.posy + 'px';
		tileImg.style.left = tile.posx + 'px';
		tileImg.style.width = tile.width + "px";
		tileImg.style.height = tile.height + "px";
		
		if (tileImg.parentNode==null){
			tile.element=this.well.appendChild(tileImg);
		}
		return tileImg;
	},
	
	/**
	 * @public
	 * @function
	 * @name		removeScaling
	 * @memberOf	PanoJS
	 * @description	saves the scaling of loaded tiles if picture fits to height or to width (for IE)
	 */
	removeScaling : function() {
		for (var img in this.images) {
			this.images[img]["scaled"] = false;
		}
	},
	
	createPrototype : function(src) {
		var img = document.createElement('img');
		img.style.display = "none";
		img.className = PanoJS.TILE_STYLE_CLASS;
		// seems to need this no matter what
		//changes all not available Tiles to the blank one, so that no ugly Image not Found Pics popup.
		img.onload = function () {
			this.style.display = "block";
		};
		img.onerror = function () {
			this.src = PanoJS.BLANK_TILE_IMAGE;
			return true;
		};
		//important for events to set "src" as late a possible
		img.src = src;
		img.relativeSrc = src;
		//don't handle width with tiles
		return img;
	},

	/**
	 * Notify listeners of a zoom event on the viewer.
	 */
	notifyViewerZoomed : function() {
		jQuery(this.viewer).trigger("zoom.viewer", {'x':this.x, 'y': this.y, 'zoomLevel': this.zoomLevel, 'percentage': (100/(this.maxZoomLevel + 1)) * (this.zoomLevel + 1)});
	},

	zoom : function(direction) {
		// ensure we are not zooming out of range
		if (this.zoomLevel + direction < 0) {
			if (PanoJS.MSG_BEYOND_MIN_ZOOM) {
				showMessage(PanoJS.MSG_BEYOND_MIN_ZOOM);
			}
			return;
		} else if (this.zoomLevel + direction > this.maxZoomLevel) {
			if (PanoJS.MSG_BEYOND_MAX_ZOOM) {
				showMessage(PanoJS.MSG_BEYOND_MAX_ZOOM);
			}
			return;
		}

		this.blank();

		var oldX = this.x;
		var oldY = this.y;
		var currentImage = this.iview.currentImage;
		var dimensionsBefore = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};	
		this.zoomLevel += direction;
		this.prepareTiles();
		this.notifyViewerZoomed();	
		//TODO Viewer depends on currentImage to correctly position the image after zoom
		var dimensionsAfter = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};
							
		var newx = this.getNewViewerPosition(this.width, oldX, dimensionsBefore.width, dimensionsAfter.width);
		var newy = this.getNewViewerPosition(this.height, oldY, dimensionsBefore.height, dimensionsAfter.height);			

		this.positionTiles({'x' : this.x - newx, 'y' : this.y - newy});
	},
	
	/**
	 * @public
	 * @function
	 * @name		zoomViewer
	 * @memberOf	PanoJS
	 * @description	handles the direction of zooming in the viewer
	 * @param 		{boolean} direction: true = zoom in, false = zoom out
	 */
	zoomViewer : function(direction) {
		var currentImage = this.iview.currentImage;
		var dir = -1;
		
		if (direction){
		  //zoom in
		  if (this.zoomLevel != currentImage.zoomInfo.maxZoom) {
		    dir = 1;
		  } else {
		    //if zoomWidth or zoomScreen was active and we're already in the max zoomlevel just reset the displayMode
		    dir = 0;
		    if (currentImage.zoomInfo.zoomScreen) {
		      this.pictureScreen(true);
		    } else if (currentImage.zoomInfo.zoomWidth) {
		      this.pictureWidth(true);
		    }
		  }
		}
		
		this.zoom(dir);
	},

	/** 
	 * Clear all the tiles from the well for a complete reinitialization of the
	 * viewer. At this point the viewer is not considered to be initialized.
	 */
	clear : function() {
		this.blank();
		this.initialized = false;
		this.tiles = [];
	},

	/**
	 * Remove all tiles from the well, which effectively "hides"
	 * them for a repaint.
	 */
	blank : function() {
		var it = this.cache.iterator();
		var entry;
		while (it.hasNext()) {
			entry = it.next().value;
			jQuery(entry).remove();
			entry.element = null;
		}
	},

	/**
	 * Method specifically for handling a mouse move event.  A direct
	 * movement of the viewer can be achieved by calling positionTiles() directly.
	 */
	moveViewer : function(coords) {
		this.positionTiles({ 'x' : (coords.x - this.mark.x), 'y' : (coords.y - this.mark.y) });
	},

	/**
	 * Make the specified coords the new center of the image placement.
	 * This method is typically triggered as the result of a double-click
	 * event.  The calculation considers the distance between the center
	 * of the viewable area and the specified (viewer-relative) coordinates.
	 * If absolute is specified, treat the point as relative to the entire
	 * image, rather than only the viewable portion.
	 */
	recenter : function(coords, absolute) {
		var currentImage = this.iview.currentImage;
		var x = this.x - this.getNewViewerPosition(this.width, coords.x, currentImage.curWidth, currentImage.curWidth);
		var y = this.y - this.getNewViewerPosition(this.height, coords.y, currentImage.curHeight, currentImage.curHeight);
		this.positionTiles( { 'x' : x, 'y' : y});
	},

	resize : function() {
		// IE fires a premature resize event
		if (!this.initialized) {
			return;
		}

		this.clear();
        this.width = this.viewer.offsetWidth;
        this.height = this.viewer.offsetHeight;
		this.prepareTiles();
		this.initialized = true;
	},
	
	/**
	 * @public
	 * @function
	 * @name		pictureWidth
	 * @memberOf	PanoJS
	 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerwidth which is smaller than the viewerwidth
	 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
	 */
	pictureWidth : function(preventLooping){
		var bool = (typeof (preventLooping) != undefined)? preventLooping:false;
		this.iview.currentImage.zoomInfo.zoomWidth = this.switchDisplayMode(false, this.iview.currentImage.zoomInfo.zoomWidth, bool);
	},

	/**
	 * @public
	 * @function
	 * @name		pictureScreen
	 * @memberOf	PanoJS
	 * @description	calculates how the tilesize has to be so that the picture fully fits into the viewer Area, tiles used are the nearest zoomlevel to the available viewerspace which is smaller than the viewerspace
	 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
	 */
	pictureScreen : function(preventLooping){
		var bool = (typeof (preventLooping) != undefined)? preventLooping:false;
		this.iview.currentImage.zoomInfo.zoomScreen = this.switchDisplayMode(true, this.iview.currentImage.zoomInfo.zoomScreen, bool);
	},
	
	/**
	 * @public
	 * @function
	 * @name		switchDisplayMode
	 * @memberOf	PanoJS
	 * @description	calculates how the picture needs to be scaled so that it can be displayed within the display-area as the mode requires it
	 * @param		{boolean} screenZoom defines which displaymode will be calculated
	 * @param		{boolean} statebool holds the value which defines if the current mode is set or needs to be set
	 * @param 		{boolean} [preventLooping] optional tells if the function is called from the Zoombar or any Function which is connected to it or not and prevents infite loop
	 * @return		boolean which holds the new StateBool value, so it can be saved back into the correct variable
	 */
	switchDisplayMode : function(screenZoom, stateBool, preventLooping) {
		//this is viewerBean
		if (screenZoom) {
			this.iview.currentImage.zoomInfo.zoomWidth = false;
		} else {
			this.iview.currentImage.zoomInfo.zoomScreen = false;
		}
		stateBool = (stateBool)? false: true;
		this.clear();
		this.removeScaling();
		if (stateBool) {
			var maxDimViewer;
			var maxDimImg;
			var tileSizeMinZoomLevel;
			var calculatedMinFitZoomLevel;
			var maxDimCurZoomLevel;
			
			var imageWidth = this.iview.currentImage.width;
			var imageHeight = this.iview.currentImage.height;
			
			var screen = imageWidth/this.width > imageHeight/this.iview.context.viewer.outerHeight(true) || (stateBool && !screenZoom);
			
			maxDimViewer = this.getMaxDimViewer(screen);
			maxDimImg = imageHeight
			tileSizeMinZoomLevel = this.getTileSizeMinZoomLevel(screen);
			calculatedMinFitZoomLevel = Math.min(Math.max(Math.ceil(Math.log( maxDimViewer / tileSizeMinZoomLevel)/Math.LN2),0),this.iview.currentImage.zoomInfo.maxZoom);
			maxDimCurZoomLevel = this.getMaxDimCurZoomLevel(screen,calculatedMinFitZoomLevel);

			var viewerRatio = maxDimViewer / maxDimCurZoomLevel;
			this.tileSize = Math.floor(this.iview.properties.tileSize * viewerRatio) ;
			this.iview.currentImage.zoomInfo.scale = this.tileSize  / this.iview.properties.tileSize;
			this.zoom(calculatedMinFitZoomLevel - this.zoomLevel);
			
			this.init();
		} else {
			this.iview.currentImage.zoomInfo.scale = 1;
			this.tileSize = this.iview.properties.tileSize;
			this.init();
			
			//an infinite loop would arise if the repeal of the zoombar comes
			if (typeof (preventLooping) == "undefined" || preventLooping == false) {
				this.zoom(this.iview.currentImage.zoomInfo.zoomBack - this.zoomLevel);
			}
		}

		return stateBool;
	},
	 getMaxDimViewer : function(screen){
		 return (screen) ?  this.width : this.height;
	 },
	 getTileSizeMinZoomLevel : function(screen){
		 return (screen) ? this.iview.currentImage.zoomInfo.dimensions[0].width : this.iview.currentImage.zoomInfo.dimensions[0].height;
	 }
	,
	getMaxDimCurZoomLevel : function(screen, calculatedMinFitZoomLevel){
		return (screen) ? this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].width : this.iview.currentImage.zoomInfo.dimensions[calculatedMinFitZoomLevel].height;
	}
	,

	/**
	 * Resolve the coordinates from this mouse event by subtracting the
	 * offset of the viewer in the browser window (or frame).  This does
	 * take into account the scroll offset of the page.
	 */
	resolveCoordinates : function(e) {
		var currentImage = this.iview.currentImage;
		return {
			'x' : this.x + e.clientX - (document.documentElement.scrollLeft || document.body.scrollLeft),//relativ: e.clientX - this.context2D.canvas.offsetLeft
			'y' : this.y + e.clientY - (document.documentElement.scrollTop || document.body.scrollTop)
		}	
	},

	press : function(coords) {
		this.activate(true);
		this.mark = coords;
	},

	release : function(coords) {
		this.activate(false);
		this.mark = { 'x' : 0, 'y' : 0 };
	},

	/**
	 * @description	Activate the viewer into motion depending on whether the mouse is pressed or
	 * not pressed. This method localizes the changes that must be made to the
	 * layers.
	 */
	activate : function(pressed) {
		this.pressed = pressed;
		this.surface.style.cursor = (pressed ? PanoJS.GRABBING_MOUSE_CURSOR : PanoJS.GRAB_MOUSE_CURSOR);
		this.surface.onmousemove = (pressed ? PanoJS.mouseMovedHandler : function() {});
	},

	/**
	 * @description	Check whether the specified point exceeds the boundaries of
	 * the viewer's primary image.
	 */
	pointExceedsBoundaries : function(coords) {
		return (coords.x < this.x ||
			coords.y < this.y ||
			coords.x > (this.tileSize * Math.pow(2, this.zoomLevel) + this.x) ||
			coords.y > (this.tileSize * Math.pow(2, this.zoomLevel) + this.y));
	},

	getNewViewerPosition : function(dimensionMax, position, valueBefore, valueAfter){
		//case 1: image fits into screen (before and after), no scrolling - x and y remain 0
		//case 2: image gets bigger (x and y) than viewerBean, scrolling possible, try to center the picture (depends on viewerBeanSize and movement)
		//case 3: image gets bigger in just one direction, scrolling only this axis, - what should happen?Center visible area or center the picture?				
		var newPos = 0;
		if (valueBefore > dimensionMax) {
			var dimensionCenter = Math.floor(dimensionMax / 2); 
			var dimensionViewPortCenter = position + dimensionCenter;//current viewport center					
			var dimensionDifference = Math.floor(100.0 / valueBefore * dimensionViewPortCenter);//percentage of current viewport			
			newPos = Math.floor(dimensionDifference * (valueAfter/100.0)) - dimensionCenter;
		} else if(valueAfter > dimensionMax) {
			//user didn't moved the viewer, center the picture
			if(position == 0) {
				newPos = Math.floor(((valueAfter/100.0)*50.0) - (dimensionMax / 2.0));
			}				
		}			
		if(newPos < 0) {
			newPos = 0;
		}
		return newPos;			
	}
};

PanoJS.TileUrlProvider = function(baseUri, prefix, extension) {
	this.baseUri = baseUri;
	this.prefix = prefix;
	this.extension = extension;
	this.imageHashes = [];
}

PanoJS.TileUrlProvider.prototype = {
	/**
	 * @description	calculate simple image name hash value to spread request over different servers
	 * but allow browser cache to be used by allways return the same value for a given name 
	 */
	getImageHash: function(image) {
		if (this.imageHashes[image]) {
			return this.imageHashes[image];
		}
		var hash=0;
		var pos=image.lastIndexOf(".");
		if (pos < 0)
			pos = image.length;
		for (var i=0;i<pos;i++) {
			hash += 3 * hash + (image.charCodeAt(i)-48);
		}
		this.imageHashes[image] = hash;
		return hash;
	},
	
	/**
	 * @description	returns the URL of all tileimages
	 */
	assembleUrl: function(xIndex, yIndex, zoom, image) {
		image = (image == null)? this.getCurrentImage().name : image;
	    return this.baseUri[(this.getImageHash(image)+xIndex+yIndex) % this.baseUri.length] + '/'+ this.derivate+'/' + 
	        image + '/' + zoom + '/' + yIndex + '/' + xIndex + '.' + this.extension +
	        (PanoJS.REVISION_FLAG ? '?r=' + PanoJS.REVISION_FLAG : '');
	}
}

PanoJS.mousePressedHandler = function(e) {
	var that = this.backingBean.iview;
	if (this.backingBean.isInputHandlerEnabled()) {
		e = getEvent(e);
		if (that.viewerContainer.isMax()) {
			// only grab on left-click
			if (e.button < 2) {
				var self = this.backingBean;
				var coords = self.resolveCoordinates(e);
				self.press(coords);
			}
		} else {
			that.toggleViewerMode();
		}
		// NOTE: MANDATORY! must return false so event does not propagate to well!
		return false;
	}
};

PanoJS.mouseReleasedHandler = function(e) {
	e = e ? e : window.event;
	var self = this.backingBean;
	if (self.pressed) {
		// OPTION: could decide to move viewer only on release, right here
		self.release(self.resolveCoordinates(e));
	}
};

PanoJS.mouseMovedHandler = function(e) {
	e = e ? e : window.event;
	var self = this.backingBean;
	self.moveCount++;
	if (self.moveCount % PanoJS.MOVE_THROTTLE == 0) {
		self.moveViewer(self.resolveCoordinates(e));
	}
};

PanoJS.zoomInHandler = function(e) {
	e = e ? e : window.event;
	var self = this.parentNode.parentNode.backingBean;
	self.zoom(1);
	return false;
};

PanoJS.zoomOutHandler = function(e) {
	e = e ? e : window.event;
	var self = this.parentNode.parentNode.backingBean;
	self.zoom(-1);
	return false;
};

PanoJS.doubleClickHandler = function(e) {
	var iview = this.backingBean.iview;
	if (iview.viewerContainer.isMax() && this.backingBean.isInputHandlerEnabled()) {
		e = getEvent(e);
		var self = this.backingBean;
		if (self.zoomLevel < self.maxZoomLevel) {
			this.backingBean.zoomViewer(1);
		} else {
			self.recenter(self.resolveCoordinates(e));
		}
	}
};

PanoJS.keyboardHandler = function(e) {
	e = getEvent(e);
	if (iview.credits)
		iview.credits(e);
	for (var i in PanoJS.VIEWERS){
		var viewer = PanoJS.VIEWERS[i];
		if (viewer.isInputHandlerEnabled()) {
			if (e.keyCode >= 37 && e.keyCode <=40) {
				//cursorkey movement
				var motion = {
				'x': PanoJS.MOVE_THROTTLE * (e.keyCode % 2) * (38 - e.keyCode),
				'y': PanoJS.MOVE_THROTTLE * ((39 - e.keyCode) % 2)};
			  	if (viewer.iview.viewerContainer.isMax()){
					viewer.positionTiles(motion, true);
			  	}
				preventDefault(e);
				return false;
			} else if (jQuery.inArray(e.keyCode, [109,45,189,107,61,187,144,27])>=0) {
				var dir = 0;
				//+/- Buttons for Zooming
				//107 and 109 NumPad +/- supported by all, other keys are standard keypad codes of the given Browser
				if (e.keyCode == 109 || (e.keyCode == 45 && isBrowser("opera")) || e.keyCode == 189) {
					dir = -1;
				} else if (e.keyCode == 107 || e.keyCode == 61 || (isBrowser(["Chrome", "IE"]) && e.keyCode == 187) || (isBrowser("Safari") && e.keyCode == 144)) {
					dir = 1;
				} else if (e.keyCode == 27) {
					if (viewer.iview.viewerContainer.isMax()) {
						viewer.iview.maximizeHandler();
					//TODO wrong call
					}
				}
				
				if (dir != 0 && viewer.iview.viewerContainer.isMax()) {
					viewer.iview.viewerBean.zoomViewer(dir); 
					preventDefault(e);
					e.cancelBubble = true;
					return false;
				}
			}
		}
	}
};
