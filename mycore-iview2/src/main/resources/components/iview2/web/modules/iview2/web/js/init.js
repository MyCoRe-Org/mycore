loadVars("../modules/iview2/web/config.xml");
if (typeof IView2 == "undefined")
	function IView2(){};

IView2.Events = [];
	
IView2.findEvents = function(target, type, callback) {
	// to allow differences in function call
	if (type) type = type.toLowerCase();
	
	var i = IView2.Events.length;
	if (!i) return;
	
	var selection = [];
	
	// Durchlaufe alle registrierten Events
	while (i >= 1) {
		 var item = IView2.Events[i - 1];
		
		// if some argument is left out
		if (!target) {
			var usedTarget = target;
		} else {
			var usedTarget = item[0];
		}
		if (!type) {
			var usedType = type;
		} else {
			// because browser-differences
			var usedType = item[1].toLowerCase();
		}
		if (!callback) {
			var usedCallback = callback;
		} else {
			var usedCallback = item[2];
		}
		
		// Wenn gefunden, dann gib Eintragsnummer zur�ck
		if (EventUtils.objEquals(target, usedTarget) && type === usedType && callback === usedCallback)	{
			selection[selection.length] = i-1;
		}
		i--;
	}
	
	if (selection.length > 0) {
		return selection;
	} else {
		// falls nicht gefunden
		return false;
	}
}

IView2.removeEventListener = function(target, type, callback, captures) {
	// check if Event was registred in past
	var selection = IView2.findEvents(target, type, callback);
	if (selection) {
		index = 0;
		while (index < selection.length) {
			var item = IView2.Events[selection[index]];
			// item[3] ist der zugeh�rige Wrapper, den wir zum entfernen ben�tigen.
			switch (item[1].toLowerCase()) {//Browser behave on some kinds of events totally different therefore its needed to find it out and take action correctly
				case "mousescroll":
					if (isBrowser(["IE", "Opera", "Safari"])) {
						if (isBrowser("IE")) {
							item[0].detachEvent("on" + item[1], item[3]);
						} else {
							item[0].removeEventListener("mousewheel", item[3], false);
						}
					} else {
						item[0].removeEventListener("DOMMouseScroll",item[3], false);
					}
				break;
				default://all Events which are just different in the function name to apply
					if (item[0].removeEventListener) {
						// W3C standard
						item[0].removeEventListener(item[1], item[3], false);
					} else if (item[0].attachEvent) {
						// newer IE
						item[0].detachEvent("on" + item[1], item[3]);
					} else {
						// IE 5 Mac and some others
						// TODO: needs to be tested
						item[0]['on'+item[1]] = "";
					}
					result = true;
				break;
			}
			
			// Das event wird gelöscht.
			IView2.Events.splice(selection[index], 1);
			
			index++;
		}
	}
}


function initializeGraphic(viewID) {
	//Iview[viewID].baseUri = baseUri + "/" + viewID;//TODO sicherlich andere bessere Lösung
	Iview[viewID].zoomScale = 1;//init for the Zoomscale is changed within CalculateZoomProp
	Iview[viewID].loaded = false;//indicates if the window is finally loaded
	Iview[viewID].tilesize = tilesize;
	Iview[viewID].initialModus = [false, false];
	// if the viewer started with an image with an single zoomLevel 0, because zoomMax = zoomInit & so initialZoom wont set
	Iview[viewID].initialZoom = 0;
	Iview[viewID].images = [];
	PanoJS.USE_SLIDE = false;
	PanoJS.USE_LOADER_IMAGE = false;
	// opera triggers the onload twice
	var iviewTileUrlProvider = new PanoJS.TileUrlProvider(Iview[viewID].baseUri, Iview[viewID].prefix, 'jpg');
	iviewTileUrlProvider.derivate = viewID;
	iviewTileUrlProvider.assembleUrl = function(xIndex, yIndex, zoom, image){
	    return this.baseUri[(xIndex+yIndex) % this.baseUri.length] + '/'+ this.derivate+'/' + 
	        ((image == null)? this.prefix : image) + '/' + zoom + '/' + yIndex + '/' + xIndex + '.' + this.extension +
	        (PanoJS.REVISION_FLAG ? '?r=' + PanoJS.REVISION_FLAG : '');
	};
	if (Iview[viewID].viewerBean == null) {
		Iview[viewID].viewerBean = new PanoJS("viewer"+viewID, {
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: Iview[viewID].tilesize,//Kachelgroesse
			tileUrlProvider: iviewTileUrlProvider,
			maxZoom: Iview[viewID].zoomMax,
			initialZoom: Iview[viewID].zoomInit,//Anfangs-Zoomlevel
			blankTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
			loadingTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
		});
		Iview[viewID].viewerBean.viewID = viewID;//Add Viewer ID mit übergeben damit der Viewer darauf arbeiten kann
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
			this.positionTilesOrig(motion, reset);
		}
		Iview[viewID].viewerBean.init();
	}
}

function reinitializeGraphic(viewID) {
	// TODO: attention on the runtime, if to slow, then the viewer will be shown shortly
	// --> eventuell sogar rausschieben falls sinnvoll - moeglich
	viewerBean = Iview[viewID].viewerBean;
	if (viewerBean == null) return;
	if (Iview[viewID].useOverview) {
		Iview[viewID].overview1.resize();
		// Aktualisierung nur wenn Overview geoeffnet ist, sonst erst beim Oeffnen
		if (Iview[viewID].overviewActive) {
			Iview[viewID].overview1.actualize(pagenumber);
		}
		// Anpassung des Black-Blank (zum Faden)
		$("blackBlank"+viewID).style.height = $("viewerContainer"+viewID).offsetHeight + "px";
	}
	
	// damit volle Höhe gewährleistet werden kann, height: 100% nicht verwendbar
	if (Iview[viewID].maximized == true) {
		$("viewerContainer"+viewID).style.height = document.body.clientHeight - $("viewerContainer"+viewID).offsetTop + "px";
		//$("viewer"+viewID).style.height = document.body.clientHeight - $("viewer"+viewID).parentNode.offsetTop - Iview[viewID].scrollBarX.my.self.offsetHeight  + "px";
		
	} else {
		// Wert wieder aus CSS entnehmen
		$("viewerContainer"+viewID).style.height = "";
		//$("viewer"+viewID).style.height = "";
	}
	$("viewer"+viewID).style.height = $("viewerContainer"+viewID).offsetHeight - Iview[viewID].scrollBarX.my.self.offsetHeight  + "px";
	$("viewer"+viewID).style.width = $("viewerContainer"+viewID).offsetWidth - Iview[viewID].scrollBarY.my.self.offsetWidth  + "px";
	
	viewerBean.width = $("viewer"+viewID).offsetWidth;
	viewerBean.height = $("viewer"+viewID).offsetHeight;
	viewerBean.resize();

	handleResizeScrollbars(viewID);
	
	if (Iview[viewID].useCutOut) {
		Iview[viewID].ausschnitt.updateSize((viewerBean.width / ((Iview[viewID].bildBreite / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)), (viewerBean.height / ((Iview[viewID].bildHoehe / Math.pow(2, Iview[viewID].zoomMax - viewerBean.zoomLevel))*Iview[viewID].zoomScale)));
		Iview[viewID].ausschnitt.updatePos((- (viewerBean.x / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale), (- (viewerBean.y / Math.pow(2, viewerBean.zoomLevel))*Iview[viewID].zoomScale));
	}
	
	if (Iview[viewID].useChapter && chapDynResize) {
		var newHeight = Iview[viewID].chapter1.my.self.parentNode.offsetHeight * chapResizeMul + chapResizeAdd;
		var newWidth = Iview[viewID].chapter1.my.self.parentNode.offsetWidth * chapResizeMul + chapResizeAdd;
		if (0 > newHeight) newHeight = 0;
		if (0 > newWidth) newWidth = 0;
		Iview[viewID].chapter1.setSize(newWidth, newHeight);
	}
}

// uses the callback format GSIV.{className}Handler
function maximizeHandler(viewID) {
	if (Iview[viewID].maximized) {
		Iview[viewID].maximized = false;
		/*if (document.compatMode == "CSS1Compat") {
			document.documentElement.style.overflow="auto";
		} else {
			document.body.style.overflow="auto";
		}*/
		if (classIsUsed("BSE_fullView")) doForEachInClass("BSE_fullView", ".style.display = 'block';", viewID);
		if (classIsUsed("BSE_normalView")) doForEachInClass("BSE_normalView", ".style.display = 'none';", viewID);
		
		document.body.style.overflow="";
		
		// class-Wechsel löst im IE resize aus
		$("viewerContainer"+viewID).className = "viewerContainer min";
		$("buttonSurface"+viewID).className = "buttonSurface min";
		
		if (Iview[viewID].useChapter) {
			openChapter(false, viewID);
		}
		
	} else {
		Iview[viewID].maximized = true;
		/*if (document.compatMode == "CSS1Compat") {
			document.documentElement.style.overflow="hidden";
		} else {
			document.body.style.overflow="hidden";
		}*/
		if (classIsUsed("BSE_fullView")) doForEachInClass("BSE_fullView", ".style.display = 'none';", viewID);
		if (classIsUsed("BSE_normalView")) doForEachInClass("BSE_normalView", ".style.display = 'block';", viewID);
		
		document.body.style.overflow="hidden";
		
		// class-Wechsel löst im IE resize aus
		$("viewerContainer"+viewID).className = "viewerContainer max";
		$("buttonSurface"+viewID).className ="buttonSurface max";
		
		if (Iview[viewID].useChapter) {
			openChapter(false, viewID);
		}
	}

	// IE löst resize bereits bei bei den Class-Wechsel (sicherlich wegen position rel <-> fix)
	if (!(isBrowser("IE"))) {
		reinitializeGraphic(viewID);
	}

	// beim Wechsel zw. Vollbild und Normal aktuelle ZoomMethode beibehalten
	if(Iview[viewID].zoomScreen){
		Iview[viewID].zoomScreen = !Iview[viewID].zoomScreen;	
		pictureScreen(viewID);
	} else if(Iview[viewID].zoomWidth){
		Iview[viewID].zoomWidth = !Iview[viewID].zoomWidth;
		pictureWidth(viewID);
	}
	//TODO maximized noch nötig, wegen viewerBean.maximized?
	//Iview[viewID].maximized = !Iview[viewID].maximized;
}