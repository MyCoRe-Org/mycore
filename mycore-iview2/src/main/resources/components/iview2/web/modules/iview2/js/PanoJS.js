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

	if (typeof options.tileUrlProvider != 'undefined'/* &&
		PanoJS.isInstance(options.tileUrlProvider, PanoJS.TileUrlProvider)*/) {
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
//	this.slideMonitor = 0;
//	this.slideAcceleration = 0;
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

// utility functions
//PanoJS.isInstance = function(object, clazz) {
//	// FIXME: can this just be replaced with instanceof operator? It has been reported that __proto__ is specific to Netscape
//	while (object != null) {
//		if (object == clazz.prototype) {
//			return true;
//		}
//
//		object = object.__proto__;
//	}
//
//	return false;
//}
////IE and Opera doesn't accept our TileUrlProvider Instance as one of PanoJS
//PanoJS.isInstance = function () {return true;};

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

//	/**
//	 * Resize the viewer to fit snug inside the browser window (or frame),
//	 * spacing it from the edges by the specified border.
//	 *
//	 * This method should be called prior to init()
//	 * FIXME: option to hide viewer to prevent scrollbar interference
//	 */
//	fitToWindow : function(border) {
//		if (typeof border != 'number' || border < 0) {
//			border = 0;
//		}
//
//		this.border = border;
//		var calcWidth = 0;
//		var calcHeight = 0;
//		if (window.innerWidth) {
//			calcWidth = window.innerWidth;
//			calcHeight = window.innerHeight;
//		}
//		else {
//			calcWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth : document.body.clientWidth);
//			calcHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight : document.body.clientHeight);
//		}
//		
//		calcWidth = Math.max(calcWidth - 2 * border, 0);
//		calcHeight = Math.max(calcHeight - 2 * border, 0);
//		if (calcWidth % 2) {
//			calcWidth--;
//		}
//
//		if (calcHeight % 2) {
//			calcHeight--;
//		}
//
//		this.width = calcWidth;
//		this.height = calcHeight;
//		this.viewer.style.width = this.width + 'px';
//		this.viewer.style.height = this.height + 'px';
//		this.viewer.style.top = border + 'px';
//		this.viewer.style.left = border + 'px';
//	},

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
		var rows = Math.ceil(this.height / this.tileSize) + 1;
		var cols = Math.ceil(this.width / this.tileSize) + 1;
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

//		this.surface.onmousedown = PanoJS.mousePressedHandler;
//		this.surface.onmouseup = this.surface.onmouseout = PanoJS.mouseReleasedHandler;
//		this.surface.ondblclick = PanoJS.doubleClickHandler;
//		if (PanoJS.USE_KEYBOARD) {
//			document.onkeydown = PanoJS.keyboardHandler;
//		}

//		this.positionTiles();
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
		var xMove = this.x - motion.x; 
		var xViewerBorder = this.iview.currentImage.curWidth - this.width;
		var yMove = this.y - motion.y; 
		var yViewerBorder = this.iview.currentImage.curHeight - this.height;
		
		if(this.iview.currentImage.rotation == 90 || this.iview.currentImage.rotation == 270){
			xViewerBorder = this.iview.currentImage.curHeight -this.iview.viewerBean.width;
			yViewerBorder = this.iview.currentImage.curWidth - this.iview.viewerBean.height;
		}
		
		if (xViewerBorder > 0) {//testen, ob das Bild überhaupt in der aktuellen Zoomstufe in die Richtung bewegt werden kann
			if (xMove >  xViewerBorder && xViewerBorder > 0) { //max. Randbegrenzung
				this.x = xViewerBorder;
			} else if(xMove > 0) {//normal
				this.x -= motion.x;
			} else if(this.x < 0 || xMove < 0) {//min. Randbegrenzung
				this.x = 0;
			}
		} else {
			this.x = 0;
		}
		
		if (yViewerBorder > 0) {
			if (yMove > yViewerBorder && yViewerBorder > 0) {
				this.y = yViewerBorder;
			} else if (yMove > 0) {
				this.y -= motion.y;
			} else if (this.y < 0 || yMove < 0) {
				this.y = 0;
			}
		} else {
			this.y = 0;
		}
		
		/*verschieben des Preload bildes damit man eine grobe Vorschau sieht von dem was kommt
		  wird nur ausgeführt wenn Seite geladen ist, da ansonsten die Eigenschaften noch nicht vorhanden sind*/
		if (this.iview.properties.initialized) {
			//folgende beide IF-Anweisungen für IE
			if(isNaN(this.x)) this.x = 0; 
			if(isNaN(this.y)) this.y = 0;
		}
		
		this.motion = motion;
		this.updateScreen();
		jQuery(this.viewer).trigger("move.viewer", {'x': this.x, 'y': this.y});
	},
	
	updateScreen : function() {
		var that = this;

		var rect = {};
		var rotation = this.iview.currentImage.rotation;
		rect.x = this.x;
		rect.y = this.y;
		var tileSize = this.tileSize;
		
		var curWidth = this.iview.currentImage.curWidth;
		var curHeight = this.iview.currentImage.curHeight;		
					
		var cnvWidth = this.width;
		var cnvHeight = this.height;
		
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

		for (var column = 0; column < xTiles; column++) {
			for (var row = 0; row < yTiles; row++) {
				//get the associated tiles
				var tile = this.tiles[column][row];
				tile.xIndex = column + startx;
				tile.yIndex = row + starty;
				tile.width=(tile.xIndex == imgXTiles-1)? curWidth - tile.xIndex * tileSize : tileSize; 
				tile.height=(tile.yIndex == imgYTiles-1)? curHeight - tile.yIndex * tileSize: tileSize;
				
				tile.posx = column * tileSize - xoff;
				tile.posy = row * tileSize - yoff;
				
				this.assignTileImage(tile);
			}
		}
		//remove tiles which were needed earlier but for the current state are obsolete to display everything within the viewer
		if (this.tiles.length > xTiles && this.oldXTiles > xTiles) {
			for (var row = 0; row < yTiles; row++) {
				var tile = this.tiles[xTiles][row];
				try {
					jQuery(tile.element).remove();
					tile.element = null;
				} catch (e) {}
			}
		}
		
		if (this.tiles[0].length > yTiles && this.oldYTiles > yTiles) {
			for (var column = 0; column < xTiles; column++) {
				var tile = this.tiles[column][yTiles];
				try {
					jQuery(tile.element).remove();
					tile.element = null;
				} catch (e) {}
			}
		}
		
		//keep this value to only clear those tiles if there were more last run
		this.oldYTiles = yTiles;
		this.oldXTiles = xTiles;
	},

	/**
	 * Determine the source image of the specified tile based
	 * on the zoom level and position of the tile.  If forceBlankImage
	 * is specified, the source should be automatically set to the
	 * null tile image.  This method will also setup an onload
	 * routine, delaying the appearance of the tile until it is fully
	 * loaded, if configured to do so.
	 */
	assignTileImage : function(tile, forceBlankImage) {
		var tileImgId = this.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, this.zoomLevel);
		var tileImg = this.cache.getItem(tileImgId);
		// create cache if not exist
		if ((tile.element && tileImgId != tile.element.src) || tileImg == null || !tile.element) {
			if (tile.element != null && tile.element.parentNode != null) {
				jQuery(tile.element).remove();
				tile.element = null;
			}
			tileImg = this.createPrototype(tileImgId);
			this.cache.setItem(tileImgId, tileImg, {'callback': function() {
				//drop tiles if they're no longer cached
				if (tileImg.element) {
					jQuery(tile.element).remove();
					tileImg.element = null;
				}
			}});
			tile.element = this.well.appendChild(tileImg);
			this.isloaded(tileImg);
		}
		if (tile.element) {
			tile.element.style.top = tile.posy + 'px';
			tile.element.style.left = tile.posx + 'px';
		}
//		tile.width = this.tileSize + "px";
//		tile.height = this.tileSize + "px";
		// seems to need this no matter what
		//changes all not available Tiles to the blank one, so that no ugly Image not Found Pics popup.
		tileImg.onerror = function () {this.src = PanoJS.BLANK_TILE_IMAGE; return true;};
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
	
	/**
	 * @private
	 * @function
	 * @name		isloaded
	 * @memberOf	PanoJS
	 * @description	checks if the picture is loaded
	 * @param		{object} img
	 */
	isloaded : function(img) {
		/*
		NOTE tiles are not displayed correctly in Opera, because the used accuracy for pixel values only has 
		2 decimal places, however 3 are necessary for the correct representation as in FF
		*/
		if (!this.images[img.src]) {
			this.images[img.src] = new Object();
			this.images[img.src]["scaled"] = false;
			img.style.display = "none";
		}
		if (((img.naturalWidth == 0 && img.naturalHeight == 0)  && !isBrowser(["IE", "Opera"])) || (!img.complete && isBrowser(["IE", "Opera"]))) {
			if (img.src.indexOf("blank.gif") == -1) {//change
				var that = this;
				//TODO check if it can be modified to work with deferred objects
				window.setTimeout(function(image) { return function(){that.isloaded(image);} }(img), 100);
			}
		} else if (img.src.indexOf("blank.gif") == -1) {
			if (this.images[img.src]["scaled"] != true) {
				img.style.display = "inline";
				this.images[img.src]["scaled"] = true;//notice that this picture already was scaled
				//TODO math Floor rein bauen bei Höhe und Breite
			  var zoomScale=this.iview.currentImage.zoomInfo.scale;
				if (!isBrowser(["IE","Opera"])) {
					img.style.width = zoomScale * img.naturalWidth + "px";
					img.style.height = zoomScale * img.naturalHeight + "px";
				} else {
					if (!this.images[img.src]["once"]) {
						this.images[img.src]["once"] = true;
						this.images[img.src]["naturalheight"] = img.clientHeight;
						this.images[img.src]["naturalwidth"] = img.clientWidth;
					}
					img.style.width = zoomScale * this.images[img.src]["naturalwidth"] + "px";
					img.style.height = zoomScale * this.images[img.src]["naturalheight"] + "px";
				}
			}
		}
		img = null;
	},

	createPrototype : function(src) {
		var img = document.createElement('img');
		img.src = src;
		img.relativeSrc = src;
		img.className = PanoJS.TILE_STYLE_CLASS;
		//don't handle width with tiles
		try {
			return img;
		} finally {
			img = null;
		}
	},

	/**
	 * Notify listeners of a zoom event on the viewer.
	 */
	notifyViewerZoomed : function() {
		jQuery(this.viewer).trigger("zoom.viewer", {'x':this.x, 'y': this.y, 'zoomLevel': this.zoomLevel, 'percentage': (100/(this.maxZoomLevel + 1)) * (this.zoomLevel + 1)});
	},

//	/**
//	 * Notify listeners of a move event on the viewer.
//	 */
//	notifyViewerMoved : function(coords) {
//		if (typeof coords == 'undefined') {
//			coords = { 'x' : 0, 'y' : 0 };
//		}
//		jQuery(this.viewer).trigger("move.viewer", {'x': this.x + (coords.x - this.mark.x),'y': this.y + (coords.y - this.mark.y)});
//	},

	zoom : function(direction) {
		// ensure we are not zooming out of range
		if (this.zoomLevel + direction < 0) {
			if (PanoJS.MSG_BEYOND_MIN_ZOOM) {
				alert(i18n.translate(PanoJS.MSG_BEYOND_MIN_ZOOM));
			}
			return;
		} else if (this.zoomLevel + direction > this.maxZoomLevel) {
			if (PanoJS.MSG_BEYOND_MAX_ZOOM) {
				alert(i18n.translate(PanoJS.MSG_BEYOND_MAX_ZOOM));
			}
			return;
		}

		this.blank();

		var oldX = this.x;
		var oldY = this.y;
		var currentImage = this.iview.currentImage;
		var dimensionsBefore = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};	
		this.zoomLevel += direction;
		var dimensionsAfter = {'width' : currentImage.curWidth, 'height' :  currentImage.curHeight};
							
		this.x = this.getNewViewerPosition(this.width, oldX, dimensionsBefore.width, dimensionsAfter.width);
		this.y = this.getNewViewerPosition(this.height, oldY, dimensionsBefore.height, dimensionsAfter.height);			

		this.prepareTiles();
		this.notifyViewerZoomed();			
		this.positionTiles();
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

		if (direction && this.zoomLevel != currentImage.zoomInfo.maxZoom) {
			dir = 1;
		}
		
		//if zoomWidth or zoomScreen was active and we're already in the max zoomlevel just reset the displayMode
		if (currentImage.zoomInfo.zoomScreen) {
			this.pictureScreen(true);
		} else if (currentImage.zoomInfo.zoomWidth) {
			this.pictureWidth(true);
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
//		this.mark = coords;
		//this.notifyViewerMoved(coords);
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
		this.x = this.getNewViewerPosition(this.width, coords.x, currentImage.curWidth, currentImage.curWidth);
		this.y = this.getNewViewerPosition(this.height, coords.y, currentImage.curHeight, currentImage.curHeight);
		this.positionTiles();
	},

	resize : function() {
		// IE fires a premature resize event
		if (!this.initialized) {
			return;
		}

        var newWidth = this.viewer.offsetWidth;
        var newHeight = this.viewer.offsetHeight;

		this.viewer.style.display = 'none';
		this.clear();

		var before = {
			'x' : Math.floor(this.width / 2),
			'y' : Math.floor(this.height / 2)
		};

//		if (this.border >= 0) {
//			this.fitToWindow(this.border);
//		}
//		else {
            this.width = newWidth;
            this.height = newHeight;
//        }

		this.prepareTiles();

		var after = {
			'x' : Math.floor(this.width / 2),
			'y' : Math.floor(this.height / 2)
		};

//		if (this.border >= 0) {
//			this.x += (after.x - before.x);
//			this.y += (after.y - before.y);
//		}
		this.positionTiles();
		this.viewer.style.display = '';
		this.initialized = true;
		//this.notifyViewerMoved();
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
			for (var i = 0; i <= this.iview.currentImage.zoomInfo.maxZoom; i++) {
				if(this.iview.currentImage.width/this.width > this.iview.currentImage.height/this.iview.context.viewer.outerHeight(true) || (stateBool && !screenZoom)){
				//Width > Height Or ZoomWidth is true
					if (this.calculateZoomProp(i, this.iview.currentImage.width, this.width, 0)) {
						break;
					}
				} else {
					if (this.calculateZoomProp(i, this.iview.currentImage.height, this.height, 0)) {
						break;
					}
				}
			}
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
	
	/**
	 * @public
	 * @function
	 * @name		calculateZoomProp
	 * @memberOf	PanoJS
	 * @description	calculates how the TileSize and the zoomvalue needs to be if the given zoomlevel fits into the viewer
	 * @param		{integer} level the zoomlevel which is used for testing
	 * @param		{integer} totalSize the total size of the Picture Dimension X or Y
	 * @param		{integer} viewerSize the Size of the Viewer Dimension X or Y
	 * @param		{integer} scrollBarSize the Height or Width of the ScrollBar which needs to be dropped from the ViewerSize
	 * @return		boolean which tells if it was successfull to scale the picture in the current zoomlevel to the viewer Size
	 */
	calculateZoomProp : function(level, totalSize, viewerSize, scrollBarSize) {
		if ((totalSize / Math.pow(2, level)) <= viewerSize) {
			if (level != 0) {
				level--;
			}
			var currentWidth = totalSize / Math.pow(2, level);
			var viewerRatio = viewerSize / currentWidth;
			var fullTileCount = Math.floor( currentWidth / this.iview.properties.tileSize);
			var lastTileWidth = currentWidth - fullTileCount * this.iview.properties.tileSize;
			this.iview.currentImage.zoomInfo.scale = viewerRatio; //determine the scaling ratio
			level = this.iview.currentImage.zoomInfo.maxZoom - level;
			this.tileSize = Math.floor((viewerSize - viewerRatio * lastTileWidth) / fullTileCount);
			this.iview.currentImage.zoomInfo.zoomBack = this.zoomLevel;
			this.zoom(level - this.zoomLevel);
			return true;
		}
		return false;
	},

	/**
	 * Resolve the coordinates from this mouse event by subtracting the
	 * offset of the viewer in the browser window (or frame).  This does
	 * take into account the scroll offset of the page.
	 */
	resolveCoordinates : function(e) {
		var currentImage = this.iview.currentImage;
		
		switch(currentImage.rotation){
			case 0:				
				return {
					'x' : this.x + e.clientX - (document.documentElement.scrollLeft || document.body.scrollLeft),//relativ: e.clientX - this.context2D.canvas.offsetLeft
					'y' : this.y + e.clientY - (document.documentElement.scrollTop || document.body.scrollTop)
				}	
			case 90:
				return {
					'x' : this.x + e.clientY + (document.documentElement.scrollTop || document.body.scrollTop),
					'y' : this.y - e.clientX - (document.documentElement.scrollLeft || document.body.scrollLeft)
				}	
			case 180:
				return {
					'x' : this.x - e.clientX + (document.documentElement.scrollLeft || document.body.scrollLeft),
					'y' : this.y - e.clientY + (document.documentElement.scrollTop || document.body.scrollTop)
				}
			case 270:
				return {
					'x' : this.x - e.clientY - (document.documentElement.scrollTop || document.body.scrollTop),
					'y' : this.y + e.clientX + (document.documentElement.scrollLeft || document.body.scrollLeft)
				}
		}
//		return {
//			'x' : (e.pageX || (e.clientX + (document.documentElement.scrollLeft || document.body.scrollLeft))) - this.left,
//			'y' : (e.pageY || (e.clientY + (document.documentElement.scrollTop || document.body.scrollTop))) - this.top
//		}
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
	
	// QUESTION: where is the best place for this method to be invoked?
//	resetSlideMotion : function() {
//		// QUESTION: should this be > 0 ?	
//		if (this.slideMonitor != 0) {
//			clearTimeout(this.slideMonitor);
//			this.slideMonitor = 0;
//		}
//
//		this.slideAcceleration = 0;
//	}
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
//		var coords = self.resolveCoordinates(e);
		if (self.zoomLevel < self.maxZoomLevel) {
			this.backingBean.zoomViewer(1);
		} else {
			//self.resetSlideMotion();
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
			//		viewer.notifyViewerMoved(motion);
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
