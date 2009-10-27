loadVars("../modules/iview2/web/config.xml");
function initializeGraphic(viewID) {
	//Iview[viewID].baseUri = baseUri + "/" + viewID;//TODO sicherlich andere bessere Lösung
	Iview[viewID].zoomScale = 1;//init for the Zoomscale is changed within CalculateZoomProp
	Iview[viewID].loaded = false;//indicates if the window is finally loaded
	Iview[viewID].tilesize = tilesize;
	Iview[viewID].initialModus = [false, false];
	// if the viewer started with an image with an single zoomLevel 0, because zoomMax = zoomInit & so initialZoom wont set
	Iview[viewID].initialZoom = 0;
	Iview[viewID].images = [];
	// opera triggers the onload twice
	if (Iview[viewID].viewerBean == null) {
		Iview[viewID].viewerBean = new GSIV("viewer"+viewID, {
			tileBaseUri: Iview[viewID].baseUri,
			tilePrefix: Iview[viewID].prefix,
			initialPan: {'x' : 0, 'y' : 0 },//Koordianten der oberen linken Ecke
			tileSize: Iview[viewID].tilesize,//Kachelgr��e
			tileExtension: "jpg",
			maxZoom: Iview[viewID].zoomMax,
			initialZoom: Iview[viewID].zoomInit,//Anfangs-Zoomlevel
			blankTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
			loadingTile: "../modules/iview2/web/" + styleFolderUri + 'blank.gif',
			viewID: viewID//Add Viewer ID mit übergeben damit der Viewer darauf arbeiten kann					
		});
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