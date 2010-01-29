function overview(newId, parent, identer) {
	var id = newId;//Overview-ID
	var identer = identer; //Allows different Styles for given Groups
	var divSizeHeight = null;//height if the single divBoxes
	var divSizeWidth = null;//width of the single divBoxes
	var previewSizeHeight = null;//height of the previewImages
	var previewSizeWidth = null;//width of the previewImages
	var scrollBarWidth = null;//width of the scrollbar
	var amountHeight = null;//horizontal number of previewImages
	var amountWidth = null;//vertical number of previewImages
	var currentFirstRow = null;//linenumber  of the first line in the overview
	var listener = [];//array who contains the individual listeners
	var useScrollBar = true; // 'true' requires class ScrollBars.js
	var scrollOverview = null;
	var viewID = "";
	var book = null;
	var baseUri = "";
	var numberOfPages = 0;
	overview.PAGE_NUMBER = 0;

//Function declarations:	
	this.init = init;
	this.actualize = actualize;
	this.addListener = addListener;
	this.getAmountHeight = getAmountHeight;
	this.getAmountWidth = getAmountWidth;
	//this.loadImagesFromLine = loadImagesFromLine;
	this.resize = resize;
	this.setViewID = setViewID;
	this.setNumberOfPages = setNumberOfPages;
	this.setCurrentFirstRow = setCurrentFirstRow;
	this.setBook = setBook;
	this.setBaseUri = setBaseUri;
	/*
	@description add Listeners to the listener-array
	*/
	function addListener(type, theListener) {
		if (!listener[type]) {
			listener[type] = [];
		}
		listener[type].push(theListener);
	}

	/*
	@description adds the individual overview-blocks into the listener-array (?)
	*/
	function notifyListenerClick(value) {
		if (!listener[overview.PAGE_NUMBER]) {
			return;
		}
		for(var i = 0; i < listener[overview.PAGE_NUMBER].length; i++) {
			listener[overview.PAGE_NUMBER][i].click(value);
		}
	}

	/*
	@description creates container for the div-boxes
	*/
	function createContainers() {
		//calculate the number of horizontal and vertical div-boxes
		var el=document.getElementById(id);
		amountWidth = Math.floor((parseInt(el.offsetWidth) - scrollBarWidth) / divSizeWidth);
		amountHeight = Math.floor(parseInt(el.offsetHeight) / divSizeHeight);

		// create target Div's
		for (var i = 0; i < amountHeight; i++) {
			for (var j = 0; j < amountWidth; j++) {
				createAbsoluteObject("div", "divBox", id);
				// container is later brought into the picture
				//$("divBox").style.left = "-10000px";
				prepareContainer(i,j);
				preparePreview("divBox" + viewID +((i * amountWidth) + j), i, j);
			}
		}
	}

	/*
	@description set styles for the containers
	*/
	function prepareContainer(i,j) {
		// CSS
		var divBox=jQuery("#divBox");
		divBox.css("border-top-style", divBox.css("border-top-style"));
		divBox.css("border-bottom-style", divBox.css("border-bottom-style"));
		divBox.css("borderLeftStyle", divBox.css("border-left-style"));
		divBox.css("borderRightStyle", divBox.css("border-right-style"));

		divBox.css("borderTopWidth", divBox.css("border-top-width"));
		divBox.css("borderBottomWidth", divBox.css("border-bottom-width"));
		divBox.css("borderLeftWidth", divBox.css("border-left-width"));
		divBox.css("borderRightWidth", divBox.css("border-right-width"));

		divBox.css("borderTopColor", divBox.css("border-top-color"));
		divBox.css("borderBottomColor", divBox.css("border-bottom-color"));
		divBox.css("borderLeftColor", divBox.css("border-left-color"));
		divBox.css("borderRightColor", divBox.css("border-right-color"));

		divBox.css("backgroundColor", divBox.css("background-color"));

		divBox.attr("id", "divBox" + viewID +((i * amountWidth) + j));
	}

	/*
	@description set styles for the preview-pictures
	*/
	function preparePreview (targetDiv, i, j) {

		//prepare info-div
		createAbsoluteObject("div", "infoDiv", targetDiv);
		var infoDiv=document.getElementById("infoDiv");
		infoDiv.style.left = ((divSizeWidth - parseInt(document.getElementById("infoDiv").offsetWidth)) / 2) + "px";
		infoDiv.id="infoDiv" + viewID +((i * amountWidth) + j);

		//prepare preview-image
		createAbsoluteObject("img", "previewDiv" + viewID +((i * amountWidth) + j), targetDiv);
		document.getElementById("previewDiv" + viewID +((i * amountWidth) + j)).style.cursor = 'pointer';

	}

	/*
	@description positioned the container and the div-boxes to the correct position
	*/
	function posOverviewContainers() {
		var el=document.getElementById(id);
		var distanceLeft = (((parseInt(el.offsetWidth)- scrollBarWidth) - (amountWidth * divSizeWidth)) / 2);
		var distanceTop = ((parseInt(el.offsetHeight) - (amountHeight * divSizeHeight)) / 2);
		var divBox;
		// position target Div's
		for (var i = 0; i < amountHeight; i++) {
			for (var j = 0; j < amountWidth; j++) {
				divBox=document.getElementById("divBox" + viewID +((i * amountWidth) + j));
				divBox.style.height = divSizeHeight + "px";
				divBox.style.width = divSizeWidth + "px";
				divBox.style.left = (distanceLeft + (j * divSizeWidth)) + "px";
				divBox.style.top = (distanceTop + (i * divSizeHeight)) + "px";
			}
		}
	}

	/*
	@description create Scrollbar in the overview
	*/
	function prepareScrollBar() {
		// embed/configure scrollbar
		scrollOverview = new scrollBar("scrollOV"+viewID, "scrollOV" + identer, "");
		scrollOverview.init(false,50);
		scrollOverview.addListener(scrollBar.LIST_STEP, new function() {
			this.steped = function(value) {
				// first condition: there are more images that should be shown
				// second condition: all images fits in the current screen
				if ((currentFirstRow + value > 0 && (numberOfPages > (((amountHeight - 1) + currentFirstRow + value) * amountWidth))) ||
		  			(currentFirstRow + value == 0 /*&& (numberOfPages <= (amountHeight * amountWidth))*/)) {
						setCurrentFirstRow(currentFirstRow + value);
						loadImages();
				}
			};
		});
		var el=document.getElementById(id);
		scrollOverview.setParent(id);
		scrollOverview.setSize(el.offsetHeight);
		scrollOverview.setStepper(true);
		// IE specific, remove absolute Value
		if(window.attachEvent && !window.opera) {
			scrollOverview.my.bar.parentNode.style.width = '27px'; scrollOverview.my.bar.style.left = '0px';
		}

		// register additional Events
		ManageEvents.addEventListener(document, 'mousemove', scrollOverview.mouseMove, false);
		ManageEvents.addEventListener(document, 'mouseup', scrollOverview.mouseUp, false);
		// default mouseScroll event von Scrollbar entfernen, da sonst doppelt registriert
		ManageEvents.removeEventListener(getElementsByClassName("empty","scrollOV"+viewID,"div")[0], 'mouseScroll', scrollOverview.scroll, false);
		ManageEvents.addEventListener(el, 'mouseScroll', scrollOverview.scroll, false);
	}

	/*
	@description initialize-function for the overview
	*/
	function init() {
		// Main
		var main = document.createElement("div");
		main.id = id;
		main.className = "overview" + identer;
		// Browser-Drag&Drop deaktiveren
		main.onmousedown = function(){return false;}; //so that browser-drag-&-drop disabled
		document.getElementById(parent).appendChild(main);
		
		// define divSize	
		tmp = getCssProps("divBox", "", id, ["width", "border-left-width", "border-right-width", "height", "border-top-width", "border-bottom-width"], "parseInt")
		divSizeWidth = tmp[0] + tmp[1] + tmp[2];
		divSizeHeight = tmp[3] + tmp[4] + tmp[5];

		// define previewSize
		tmp = getCssProps("previewImage", "", id, ["width", "height"], "parseInt");
		previewSizeWidth = tmp[0];
		previewSizeHeight = tmp[1];

		createContainers();
		posOverviewContainers();
		
		if (useScrollBar) {
			//define scrollBarWidth	
			scrollBarWidth = getCssProps("scrollOVV" + identer, "scrollOVV" + identer, id, ["width"], "parseInt")[0];
			
			prepareScrollBar();
		}
	}

	/*
	@description if overview is already created and is called so load loadImagesFromLine() and the scrollbar 
	*/
	function actualize(pageNumber) {

		currentFirstRow = Math.floor((parseInt(pageNumber) - 1) / amountWidth) ;
		// if overview is to big for remaining pages
		if (currentFirstRow + amountHeight - 1 > Math.ceil(numberOfPages / amountWidth) - 1) {
			currentFirstRow = Math.ceil(numberOfPages / amountWidth) - amountHeight;
		}
		// if all pages fit in overview
		if (currentFirstRow < 0) {
			currentFirstRow = 0;
		}
		
		// TODO: Können eigentlich nicht (mehr) eintreten die Fälle
		/*
		//load preview-images in the overview if they exists
		if (currentFirstRow >= 0 && (((amountHeight + currentFirstRow - 1) * amountWidth) <= numberOfPages - 1) ||
		   (numberOfPages <= (amountHeight * amountWidth) && currentFirstRow == 0) ) {*/
			loadImages(currentFirstRow);
		//}				
		// shift scrollbar to the actually start-line
		if (useScrollBar) {
			scrollOverview.setValue(currentFirstRow);
		}
	}

	/*
	@description load the overview so, that the actually picture is in first line
	*/
	function loadImages() {
		// für spätere Prüfung "initialisiert"
		var delFrom = amountHeight;
		
		//currentcurrentFirstRow = currentFirstRow;
		
		var divBox;
		// proceed line wise
		for (var i = 0; i < amountHeight; i++) {
			for (var j = 0; j < amountWidth; j++) {
				divBox=document.getElementById("divBox" + viewID +((i * (amountWidth)) + j));
				//get back previously hidden div's
				divBox.style.display = "block";
				
				//load needed Previews
				if ((((i + currentFirstRow) * amountWidth) + j) < numberOfPages) {
					loadSingleImage(i, j);
				}
				// last line who contains pages
				if ((i + currentFirstRow) >= (Math.floor((numberOfPages) / amountWidth))) {
					// page not existing???
					if ((((currentFirstRow + i) * amountWidth)+j) > (numberOfPages - 1)) {
						divBox.style.display = "none";
						if (i <= amountHeight) {
							delFrom = i + 1;
						}
					}
				}
			}
		}
		// to remove redundant divs when the pagenumbers are small
		if (delFrom < amountHeight) {
			for (var i = delFrom; i < amountHeight; i++) {
				for (var j = 0; j < amountWidth; j++) {
					document.getElementById("divBox" + viewID +((i * (amountWidth)) + j)).style.display = "none";
				}
			}
		}
	}

	/*
	@description preloads the preview-image
	*/
	function loadSingleImage(i, j) {
		var pageName = findInArrayElement(nodeProps(book, "mets:file", (((i + currentFirstRow) * amountWidth) + j), true), "LOCTYPE", "URL").href;

		var source = Iview[viewID].viewerBean.tileUrlProvider.assembleUrl(0, 0, 0, pageName);
		var preview = document.getElementById("previewDiv" + viewID +((i * amountWidth) + j));
		
		// nicht sicher ob das so in allen Browsern funktioniert
		// linking the previews with the viewer, only if this wasen't done in past
		//if (!(ManageEvents.findEvents($("previewDiv" + viewID +((i * amountWidth) + j)),"click",""))) {
			//ManageEvents.addEventListener($("previewDiv" + viewID +((i * amountWidth) + j)), 'click', function(e) {notifyListenerClick((((i + currentFirstRow) * amountWidth) + j) + 1);}, true);
			preview.onclick =  function(e) {notifyListenerClick((((i + currentFirstRow) * amountWidth) + j) + 1);};
		//}
		
		// original Values needs, because Img will scale automatic in each Props
		var origImage = new Image;
		origImage.onload = function() {trimImage(preview, source, origImage.height, origImage.width);};
		origImage.src = source;
		
		// fill Info div
		var infoDiv=document.getElementById("infoDiv" + viewID +((i * amountWidth) + j));
		infoDiv.innerHTML = pageName;
		infoDiv.title = pageName;
		// page 0 doesn't exist
		
		// nett anzuschaun ist es auch, wenn source bereits vor onload des origImage gesetzt wird, da die Bilder dann sichtbar zusammenschrumpfen
		// Muss man gegebenenfalls zuerst das Bild unsichtbar machen und dann wieder einblenden sobald es fertig geladen & geschrumpft ist.
	}


	/*
	@desciption fits picture to the correct size within the divBox
	*/
	function trimImage(preview, source, origHeight, origWidth) {
		preview.src = source;
	
		// scale preview-images
		var scaleFactorH = (previewSizeHeight / origHeight);
		var scaleFactorW = (previewSizeWidth / origWidth);
		
		if (scaleFactorH <= 1) {
			// image is higher then div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				if (scaleFactorW < scaleFactorH) {
					preview.style.width = previewSizeWidth + "px";
					preview.style.height = (origHeight * scaleFactorW) + "px";
				} else {
					preview.style.width = (origWidth * scaleFactorH) + "px";
					preview.style.height = previewSizeHeight + "px";
				}
			} else {
				// image is smaller than the div
				preview.style.width = (origWidth * scaleFactorH) + "px";
				preview.style.height = previewSizeHeight + "px";
			}

		} else {
			// image is lower than the div
			if (scaleFactorW <= 1) {
				// image is wider than the div
				preview.style.width = previewSizeWidth + "px";
				preview.style.height = (origHeight * scaleFactorW) + "px";
			} else {
				// image is smaller than the div
				if (scaleFactorW < scaleFactorH) {
					preview.style.width = previewSizeWidth + "px";
					preview.style.height = (origHeight * scaleFactorW) + "px";
				} else {
					preview.style.width = (origWidth * scaleFactorH) + "px";
					preview.style.height = previewSizeHeight + "px";
				}
			}
		}
		// center previews horz & vert
		// (infoDivs sind alle gleich groß)
		preview.style.top = ((parseInt(document.getElementById("infoDiv" + viewID + "0").offsetTop) - 
						parseInt(preview.style.height)) / 2) + "px";
		preview.style.left = ((divSizeWidth - preview.offsetWidth) / 2) + "px";
	}

	function resize() {
		clearOverview();
		createContainers();
		posOverviewContainers();
		if (useScrollBar) {
			prepareScrollBar();
			// because no change in NumberofPages while resizing
			scrollOverview.setSteps(Math.ceil(numberOfPages / amountWidth) - amountHeight + 1);
		}
	}

	/*
	@description clears the overview
	*/
	function clearOverview() {
		var curOverview=document.getElementById(id);
		var isVisible = curOverview.style.visibility;
		var myParent = curOverview.parentNode;
		myParent.removeChild(curOverview);
		
		var resetOverview = document.createElement("div");
		resetOverview.id = id;
		resetOverview.className = "overview" + identer;
		resetOverview.onmousedown = function(){return false;}; //so that browser-drag-&-drop remains disabled after resize
		myParent.appendChild(resetOverview);
		resetOverview.style.visibility = isVisible;
	}

	function getAmountHeight() {
		return amountHeight;
	}

	function getAmountWidth() {
		return amountWidth;
	}
	
	/*
	@description sets the ID which is needed for multiple Viewers on a page so that they different components are connected properly together
	@param id the ID of the Viewer the class is connected to
	*/
	function setViewID(id) {
		viewID = id;
	}

	function setBook(value) {
		book = value;
	}

	function setNumberOfPages(value) {
		numberOfPages = value;
		// set step-number for the scrollbar
		if (useScrollBar) {
			scrollOverview.setSteps(Math.ceil(numberOfPages / amountWidth) - amountHeight + 1);
		}
	}
	
	function setCurrentFirstRow(value) {
		currentFirstRow = value;
	}
	
	function setBaseUri(uri) {
		baseUri = uri;
	}
}