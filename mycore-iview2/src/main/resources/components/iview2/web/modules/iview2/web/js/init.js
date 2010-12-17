/**
 * @public
 * @function
 * @name	initializeGraphic
 * @memberOf	iview.init
 * @description	here some important values and listener are set correctly, calculate simple image name hash value to spread request over different servers and initialise the viewer
 * @param 	{string} viewID ID of the derivate
 */
function initializeGraphic(viewID) {
	Iview[viewID].zoomScale = 1;//init for the Zoomscale is changed within CalculateZoomProp
	Iview[viewID].loaded = false;//indicates if the window is finally loaded
	Iview[viewID].tilesize = tilesize;
	Iview[viewID].initialModus = [false, false];
	// if the viewer started with an image with an single zoomLevel 0, because zoomMax = zoomInit & so initialZoom wont set
	Iview[viewID].initialZoom = 0;
	Iview[viewID].images = [];
	PanoJS.USE_SLIDE = false;
	PanoJS.USE_LOADER_IMAGE = false;
	PanoJS.MOVE_THROTTLE = 10;
	// press sonst nicht immer möglich, in PanoJS original merkwürdiges Verhalten
	/*PanoJS.mousePressedHandler = function(e) {
		e = e ? e : window.event;
		// only grab on left-click
		if (e.button < 2) {
			var self = this.backingBean;
			var coords = self.resolveCoordinates(e);
			self.press(coords);
		}
	
		// NOTE: MANDATORY! must return false so event does not propagate to well!
		return false;
	};*/
	
	// Listener muessen benachrichtigt werden und Richtung korrekt gesetzt
	PanoJS.keyboardMoveHandler = function(e) {
		e = e ? e : window.event;
		for (var i = 0; i < PanoJS.VIEWERS.length; i++) {
			var viewer = PanoJS.VIEWERS[i];
			if (e.keyCode == 38){
					viewer.positionTiles({'x': 0,'y': PanoJS.MOVE_THROTTLE}, true);
					viewer.notifyViewerMoved({'x': 0,'y': PanoJS.MOVE_THROTTLE});//added
					preventDefault(e);
			} else
			if (e.keyCode == 39){
					viewer.positionTiles({'x': -PanoJS.MOVE_THROTTLE,'y': 0}, true);
					viewer.notifyViewerMoved({'x': -PanoJS.MOVE_THROTTLE,'y': 0});//added
					preventDefault(e);
			} else
			if (e.keyCode == 40){
					viewer.positionTiles({'x': 0,'y': -PanoJS.MOVE_THROTTLE}, true);
					viewer.notifyViewerMoved({'x': 0,'y': -PanoJS.MOVE_THROTTLE});//added
					preventDefault(e);
			} else
			if (e.keyCode == 37){
					viewer.positionTiles({'x': PanoJS.MOVE_THROTTLE,'y': 0}, true);
					viewer.notifyViewerMoved({'x': PanoJS.MOVE_THROTTLE,'y': 0});//added
					preventDefault(e);
			}
		}
	}
	// keys are different in Browsers
	PanoJS.keyboardZoomHandler = function(e) {
		e = e ? e : window.event;
		var eventHandled=false;
		for (var i = 0; i < PanoJS.VIEWERS.length; i++) {
			var viewer = PanoJS.VIEWERS[i];
			// Opera auch bei "Einfg" --> 43
			if (e.keyCode == 109 || (e.keyCode == 45 && isBrowser("opera")) || e.keyCode== 189 || e.charCode == 45) {
				viewer.zoom(-1);
				if (Iview[viewer.viewID].useZoombar) {
					Iview[viewer.viewID].zoomBar.moveBarToLevel(viewer.zoomLevel);
				}
				eventHandled=true;
				preventDefault(e);
			} else
			if (e.keyCode == 107 || e.keyCode == 61 || (e.keyCode == 43 && isBrowser("opera")) || e.keyCode == 187 ||e.charCode == 43) {
				viewer.zoom(1);
				if (Iview[viewer.viewID].useZoombar) {
					Iview[viewer.viewID].zoomBar.moveBarToLevel(viewer.zoomLevel);
				}
				eventHandled=true;
				preventDefault(e);
			} else
			if ((e.DOM_VK_ESCAPE && e.keyCode == e.DOM_VK_ESCAPE) || e.keyCode == 27){
				//ESC key pressed, e.DOM_VK_ESCAPE is undefined in Apple Safari
				if (Iview[viewer.viewID].maximized){
					maximizeHandler(viewer.viewID);
				}
				eventHandled=true;
			}
		}
		//Safari does not support "onkeypress" for cursor keys but "onkeydown"
		if (!eventHandled){
			PanoJS.keyboardMoveHandler(e);
		}
	}
	//IsInstance doesn't recognizes the changed TileUrlProvider as the same in IE&Opera
	PanoJS.isInstance = function () { return true;};
	// opera triggers the onload twice
	var iviewTileUrlProvider = new PanoJS.TileUrlProvider(Iview[viewID].baseUri, Iview[viewID].prefix, 'jpg');
	iviewTileUrlProvider.derivate = viewID;
	iviewTileUrlProvider.imageHashes = [];
	/*
	 * calculate simple image name hash value to spread request over different servers
	 * but allow browser cache to be used by allways return the same value for a given name 
	 */
	iviewTileUrlProvider.getImageHash = function(image){
		if (iviewTileUrlProvider.imageHashes[image]){
			return iviewTileUrlProvider.imageHashes[image];
		}
		var hash=0;
		var pos=image.lastIndexOf(".");
		if (pos < 0)
			pos=image.length;
		for (var i=0;i<pos;i++){
			hash += 3 * hash + (image.charCodeAt(i)-48);
		}
		iviewTileUrlProvider.imageHashes[image]=hash;
		return hash;
	}

	/**
	 * returns the URL of all tileimages
	 */
	iviewTileUrlProvider.assembleUrl = function(xIndex, yIndex, zoom, image){
		image=(image == null)? this.prefix : image;
	    return this.baseUri[(iviewTileUrlProvider.getImageHash(image)+xIndex+yIndex) % this.baseUri.length] + '/'+ this.derivate+'/' + 
	        image + '/' + zoom + '/' + yIndex + '/' + xIndex + '.' + this.extension +
	        (PanoJS.REVISION_FLAG ? '?r=' + PanoJS.REVISION_FLAG : '');
	};

	/**
	 * initialise the viewer
	 */
	if (Iview[viewID].viewerBean == null) {
		Iview[viewID].viewerBean = new PanoJS("viewer"+viewID, {
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: Iview[viewID].tilesize,//Kachelgroesse
			tileUrlProvider: iviewTileUrlProvider,
			maxZoom: Iview[viewID].zoomMax,
			initialZoom: Iview[viewID].zoomInit,//Anfangs-Zoomlevel
			blankTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
			loadingTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif'
		});
		Iview[viewID].viewerBean.viewID = viewID;//Add Viewer ID mit übergeben damit der Viewer darauf arbeiten kann
		Iview[viewID].viewerBean.initOrig = Iview[viewID].viewerBean.init;
		Iview[viewID].viewerBean.init = function(motion, reset) {
			this.initOrig();
			// offset of viewer in the window
			this.top = 0;
			this.left = 0;
			for (var node = this.viewer; node; node = node.offsetParent) {
				this.top += node.offsetTop;
				this.left += node.offsetLeft;
			}
		}
		Iview[viewID].viewerBean.positionTilesOrig = Iview[viewID].viewerBean.positionTiles;
		Iview[viewID].viewerBean.positionTiles = function(motion, reset) {
			// default to no motion, just setup tiles
			if (typeof motion == 'undefined') {
				motion = { 'x' : 0, 'y' : 0 };
			}
			var viewID = this.viewID;
			//Changed to work for multiple Viewers
			//hinzugefuegt damit Bild nicht ueber die Raender laeuft
			if (-(this.x + motion.x) > ((Iview[viewID].bildBreite/Math.pow(2, Iview[viewID].zoomMax - this.zoomLevel))*Iview[viewID].zoomScale-this.width)) {
				motion.x = 0;
				this.x = -((Iview[viewID].bildBreite/Math.pow(2, Iview[viewID].zoomMax - this.zoomLevel))*Iview[viewID].zoomScale-this.width);
			}
			if (-(this.y + motion.y) > ((Iview[viewID].bildHoehe/Math.pow(2, Iview[viewID].zoomMax - this.zoomLevel))*Iview[viewID].zoomScale-this.height)) {
				motion.y = 0;
				this.y = -((Iview[viewID].bildHoehe/Math.pow(2, Iview[viewID].zoomMax - this.zoomLevel))*Iview[viewID].zoomScale-this.height);
			}
			if(this.x + motion.x > 0){
				this.x = 0;
				motion.x = 0;
			}		
			if(this.y + motion.y > 0){
				this.y = 0;
				motion.y = 0;
			}
			/*verschieben des Preload bildes damit man eine grobe Vorschau sieht von dem was kommt
			  wird nur ausgeführt wenn Seite geladen ist, da ansonsten die Eigenschaften noch nicht vorhanden sind*/
			if(Iview[viewID].loaded) {
				var preLoadEl=document.getElementById('preload'+viewID);
				//folgende beide IF-Anweisungen für IE
				if(isNaN(this.x)) this.x = 0; 
				if(isNaN(this.y)) this.y = 0;
				preLoadEl.style.left = (this.x + motion.x) + "px";
				preLoadEl.style.top = (this.y + motion.y) + "px";
			}
			
			this.positionTilesOrig(motion, reset);
			
			for (var c = 0; c < this.tiles.length; c++) {
				for (var r = 0; r < this.tiles[c].length; r++) {
					var tile = this.tiles[c][r];
					tile.width = this.tileSize + "px";
					tile.height = this.tileSize + "px";
				}
			}
		};
		Iview[viewID].viewerBean.createPrototype = function(src) {
			var img = document.createElement('img');
			img.src = src;
			img.relativeSrc = src;
			img.className = PanoJS.TILE_STYLE_CLASS;
			try {
				return img;
			} finally {
				img = null;
			}
		};
		//TODO gehts auch besser ohne komplettes Überschreiben
		Iview[viewID].viewerBean.assignTileImage =  function(tile, forceBlankImage) {
		var tileImgId, src;
		var useBlankImage = (forceBlankImage ? true : false);

		// check if image has been scrolled too far in any particular direction
		// and if so, use the null tile image
		if (!useBlankImage) {
			var left = tile.xIndex < 0;
			var high = tile.yIndex < 0;
			if (left || high) {
				useBlankImage=true;
			} else {
				//modification to original PanonJS code
				var iView=Iview[this.viewID];
				var currentWidth = Math.floor(iView.bildBreite / Math.pow(2, iView.zoomMax - this.zoomLevel));
				var xTileCount = Math.ceil( currentWidth / iView.tilesize);
				var currentHeight = Math.floor(iView.bildHoehe / Math.pow(2, iView.zoomMax - this.zoomLevel));
				var yTileCount = Math.ceil( currentHeight / iView.tilesize);
				var right = tile.xIndex >= xTileCount; //index starts at 0
				var low = tile.yIndex >= yTileCount;
				if (low || right) {
					useBlankImage = true;
				}
				//modification ends
			}
		}

		if (useBlankImage) {
			tileImgId = 'blank:' + tile.qx + ':' + tile.qy;
			src = this.cache['blank'].src;
		}
		else {
			tileImgId = src = this.tileUrlProvider.assembleUrl(tile.xIndex, tile.yIndex, this.zoomLevel);
		}

		// only remove tile if identity is changing
		if (tile.element != null &&
			tile.element.parentNode != null &&
			tile.element.relativeSrc != src) {
			this.well.removeChild(tile.element);
		}

		var tileImg = this.cache[tileImgId];
		// create cache if not exist
		if (tileImg == null) {
			tileImg = this.cache[tileImgId] = this.createPrototype(src);
		}

		if (useBlankImage || !PanoJS.USE_LOADER_IMAGE || tileImg.complete || (tileImg.image && tileImg.image.complete)) {
			tileImg.onload = function() {};
			if (tileImg.image) {
				tileImg.image.onload = function() {};
			}

			if (tileImg.parentNode == null) {
				tile.element = this.well.appendChild(tileImg);
			}
		}
		else {
			var loadingImgId = 'loading:' + tile.qx + ':' + tile.qy;
			var loadingImg = this.cache[loadingImgId];
			if (loadingImg == null) {
				loadingImg = this.cache[loadingImgId] = this.createPrototype(this.cache['loading'].src);
			}

			loadingImg.targetSrc = tileImgId;

			var well = this.well;
			tile.element = well.appendChild(loadingImg);
			tileImg.onload = function() {
				// make sure our destination is still present
				if (loadingImg.parentNode && loadingImg.targetSrc == tileImgId) {
					tileImg.style.top = loadingImg.style.top;
					tileImg.style.left = loadingImg.style.left;
					well.replaceChild(tileImg, loadingImg);
					tile.element = tileImg;
				}

				tileImg.onload = function() {};
				return false;
			}

			// konqueror only recognizes the onload event on an Image
			// javascript object, so we must handle that case here
			if (!PanoJS.DOM_ONLOAD) {
				tileImg.image = new Image();
				tileImg.image.onload = tileImg.onload;
				tileImg.image.src = tileImg.src;
			}
		}
//additions	
		isloaded(tileImg, this.viewID);
		//changes all not available Tiles to the blank one, so that no ugly Image not Found Pics popup.
		//tileImg.onerror = function () {this.src = Iview[this.viewID].viewerBean.cache['blank'].src; return true;};
		tileImg.onerror = function () {this.src = Iview[viewID].viewerBean.cache['blank'].src; return true;};
//endadd
	}
//		alert("ViewerInit davor");
		Iview[viewID].viewerBean.init();
		//Newer Opera/Safari Versions need somehow something from reinitGraphics to show the viewer on Startup
		if (navigator.userAgent.match(/Opera(.*)?Version\/10\.[0-9]*/i) || isBrowser(["Safari"])) {
			reinitializeGraphic(viewID);
		}
	}
	
	PanoJS.mousePressedHandler = function(e) {
			maximizeHandler(this.backingBean.viewID);
	}
}

/**
 * @public
 * @function
 * @name	reinitializeGraphic
 * @memberOf	iview.init
 * @description	is called if the viewer size is resized and calculates/set therefore all values for the current zoomlevel and viewModus (i.e. scrrenWidth)
 * @param 	{string} viewID ID of the derivate
 */
function reinitializeGraphic(viewID) {
	// TODO: attention on the runtime, if to slow, then the viewer will be shown shortly
	// --> eventuell sogar rausschieben falls sinnvoll - moeglich
	var viewerBean = Iview[viewID].viewerBean;
	if (viewerBean == null) return;
		
	var curHeight = 0;
	var curWidth = 0;
	if (window.innerWidth) {
		curWidth = window.innerWidth;
		curHeight = window.innerHeight;
	}
	else {
		curWidth = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientWidth : document.body.clientWidth);
		curHeight = (document.compatMode == 'CSS1Compat' ? document.documentElement.clientHeight : document.body.clientHeight);
	}

	// damit volle Höhe gewährleistet werden kann, height: 100% nicht verwendbar
	var viewerContainer=document.getElementById("viewerContainer"+viewID);
	var viewer=document.getElementById("viewer"+viewID);
	if (Iview[viewID].maximized == true) {
		viewerContainer.style.height = curHeight - viewerContainer.offsetTop + "px";
		viewer.style.height = curHeight - viewer.parentNode.offsetTop - Iview[viewID].barX.my.self[0].offsetHeight  + "px";
		viewerContainer.style.width = curWidth + "px";
		viewer.style.width = curWidth - Iview[viewID].barY.my.self[0].offsetWidth  + "px";
	} else {
		// Wert wieder herstellen
		viewerContainer.style.height = Iview[viewID].startHeight + "px";
		viewer.style.height = Iview[viewID].startHeight - ((Iview[viewID].barY.my.self.css("visibility") == "visible")? Iview[viewID].barY.my.self[0].offsetHeight : 0)  + "px";
		viewerContainer.style.width = Iview[viewID].startHeight + "px";
		viewer.style.width = Iview[viewID].startWidth - ((Iview[viewID].barX.my.self.css("visibility") == "visible")? Iview[viewID].scrollBarX.my.self[0].offsetWidth : 0)  + "px";
	}
	
	viewerBean.width = viewer.offsetWidth;
	viewerBean.height = viewer.offsetHeight;
	viewerBean.resize();
	
	// den Modus beibehalten & aktualisieren
	if(Iview[viewID].zoomScreen){
		Iview[viewID].zoomScreen = !Iview[viewID].zoomScreen;	
		pictureScreen(viewID);
	} else if(Iview[viewID].zoomWidth){
		Iview[viewID].zoomWidth = !Iview[viewID].zoomWidth;
		pictureWidth(viewID);
	}
	
	if (Iview[viewID].useOverview && Iview[viewID].overview && Iview[viewID].overview.getActive()) {
		// actualize Overview only if visible else delay it upto the reopening
		Iview[viewID].overview.setSelected(Iview[viewID].PhysicalModel.getCurPos());
	}
	
	handleResizeScrollbars(viewID);
	
	if (Iview[viewID].useCutOut) {
		Iview[viewID].cutOutModel.setRatio({
			'x': viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale),
			'y': viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)});
		Iview[viewID].cutOutModel.setPos({
			'x': - (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale,
			'y': - (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale});
	}
	
	// Actualize forward & backward Buttons
	var previewTbView = jQuery(Iview[viewID].getToolbarCtrl().getView("previewTbView").toolbar);
	var newTop = ((((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - 1)) * Iview[viewID].zoomScale) - (toInt(previewTbView.css("height")) + toInt(previewTbView.css("padding-top")) + toInt(previewTbView.css("padding-bottom")))) / 2) + "px";
	if (Iview[viewID].viewerContainer.hasClass("viewerContainer min")) {
		Iview[viewID].viewerContainer.find(".toolbars .toolbar").css("top", newTop);
	}
}

// uses the callback format GSIV.{className}Handler
/**
 * @public
 * @function
 * @name	maximizeHandler
 * @memberOf	iview.init
 * @description	maximize and show the viewer with the related image or minimize and close the viewer
 * @param 	{string} viewID ID of the derivate
 */
function maximizeHandler(viewID) {
	if (Iview[viewID].maximized) {
		if (window.location.search.get("jumpback") == "true"){
			history.back();
			return;
		}
		Iview[viewID].maximized = false;
		
		// viewer wieder einhängen
		Iview[viewID].VIEWER = document.body.firstChild;
		
		// Dokumenteninhalt loeschen
		while (document.body.firstChild) {
			document.body.removeChild(document.body.firstChild);
		}
		
		// alten Dokumenteninhalt wieder herstellen
		var index = 0;
		while (index < Iview[viewID].DOCUMENT.length) {
			document.body.appendChild(Iview[viewID].DOCUMENT[index]);
			index++;
		}
		
		// aktuellen Viewer hinzufuegen
		document.getElementById("viewerParent").insertBefore(Iview[viewID].VIEWER, currentPos);
				
		/*if (document.compatMode == "CSS1Compat") {
			document.documentElement.style.overflow="auto";
		} else {
			document.body.style.overflow="auto";
		}*/
		//close Overview when going to minimized mode 
		if (Iview[viewID].useOverview && Iview[viewID].overviewActive) {
			openOverview(viewID);
		}

		// wegen IE7 zusätzlich
		document.documentElement.style.overflow="";
		
		document.body.style.overflow="";

		// class-Wechsel löst im IE resize aus
		document.getElementById("viewerContainer"+viewID).className = "viewerContainer min";
		
		PanoJS.mousePressedHandler = function(e) {
			maximizeHandler(this.backingBean.viewID);
		};
		PanoJS.doubleClickHandler = function(e) {
		};

		if (!Iview[viewID].zoomScreen) {
			pictureScreen(viewID);
		}
	} else {
		Iview[viewID].maximized = true;
		
		Iview[viewID].getToolbarCtrl().addView(new ToolbarView("mainTbView", Iview[viewID].viewerContainer.find(".toolbars")));
		Iview[viewID].getToolbarMgr().addModel(new StandardToolbarModelProvider("mainTb", Iview[viewID].getToolbarMgr().titles).getModel());
		if (Iview[viewID].PhysicalModel) {
			Iview[viewID].getToolbarCtrl().checkNavigation(Iview[viewID].PhysicalModel.getCurPos());
		}
		
		if (Iview[viewID].zoomWidth) {
			$(".mainTbView .zoomHandles .fitToWidth")[0].checked = true;
			$(".mainTbView .zoomHandles .fitToWidthLabel").addClass("ui-state-active");
		} else if (Iview[viewID].zoomScreen) {
			$(".mainTbView .zoomHandles .fitToScreen")[0].checked = true;
			$(".mainTbView .zoomHandles .fitToScreenLabel").addClass("ui-state-active");
		}
		
		// Dokumenteninhalt sichern
		Iview[viewID].DOCUMENT = new Array();
		Iview[viewID].VIEWER = document.getElementById("viewerContainer"+viewID).parentNode.parentNode.parentNode.parentNode;
		currentPos = Iview[viewID].VIEWER.nextSibling;
		document.getElementById("viewerContainer"+viewID).parentNode.parentNode.parentNode.parentNode.parentNode.id = "viewerParent";
		
		// Dokumenteninhalt loeschen
		var index = 0;
		while (document.body.firstChild) {
			Iview[viewID].DOCUMENT[index] = document.body.firstChild;
			document.body.removeChild(document.body.firstChild);
			index++;
		}

		// Viewer hinzufuegen
		document.body.appendChild(Iview[viewID].VIEWER);
		
		// wegen IE7 zusätzlich
		document.documentElement.style.overflow="hidden";
		
		document.body.style.overflow="hidden";

		// class-Wechsel loesst im IE resize aus
		document.getElementById("viewerContainer"+viewID).className = "viewerContainer max";
		
		PanoJS.mousePressedHandler = function(e) {
			e = e ? e : window.event;
			// only grab on left-click
			if (e.button < 2) {
				var self = this.backingBean;
				var coords = self.resolveCoordinates(e);
				self.press(coords);
			};
			// NOTE: MANDATORY! must return false so event does not propagate to well!
			return false;
		}		
		// dblclick only if maximize and additional zoomInEvent
		PanoJS.doubleClickHandler = function(e) {
			e = e ? e : window.event;
			var self = this.backingBean;
			coords = self.resolveCoordinates(e);
			if (!self.pointExceedsBoundaries(coords)) {
				self.resetSlideMotion();
				self.recenter(coords);
			}
			if (self.zoomLevel < self.maxZoomLevel) {
				self.zoom(1);
				if (Iview[viewID].useZoomBar) Iview[viewID].zoomBar.moveBarToLevel(Iview[viewID].viewerBean.zoomLevel);
			}
		};
	}

// IE löst resize bereits bei den Class-Wechsel (sicherlich wegen position rel <-> fix)
	//IE führt die zwar so irgendwie mehrfach aus... aber ohne die auch nicht...muss man wohl mit leben
//	if (!(isBrowser("IE"))) {
		reinitializeGraphic(viewID);
//	}

}