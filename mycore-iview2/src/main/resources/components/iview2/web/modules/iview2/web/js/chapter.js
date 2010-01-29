function chapter(newId, parent) {
	var id = newId;//Chapter-ID
	var effects = false; // indicator if effects are used or not
	var pageNumber = null; // holds the current pagenumber
	var lastHighlighted = 0; // holds the number of the previous highlighted pagenumber
	var listener = []; // array who holds informations about the listeners (?)
	var pageIdCounter = 0; // counts the number of pages to give every page an ID which is the paganumber
	//var loadPageFunction = null;// string who holds the name of the function which loads the new page
	//var loadPageDataFunction = null;// string who holds the name of the function which procure book-informations
	var paddingLeft = null;// is responsible for the width of the chapter1_content-box
	var borderLeftWidth = null;// is responsible for the width of the chapter1_content-box
	var pointsWidth = null;// is responsible for the width of the chapter1_content-box
	var my = null;// chapter-object
	var bookData = null;//holds all needed Information about the book which shall be displayed
	var viewID = "";//holds the viewID the viewer is connected to
	var bgColors = [];

	chapter.PAGE_NUMBER = 0;
	chapter.ITEM_CLICK = 1;
	chapter.RESIZE = 2;

//Function declarations:
	this.init = init;
	this.useEffects = useEffects;
	this.hasEffects = hasEffects;
	this.setSize = setSize;
	this.changePage = changePage;
	this.showCurrentPageCenter = showCurrentPageCenter;
	this.displayOutAllEntries = displayOutAllEntries;
	//this.pressEachChapterEntries = pressEachChapterEntries;
	this.addListener = addListener;
	this.dropListener = dropListener;
	this.getHeight = getHeight;
	this.my = null;
	//this.setLoadPageFunction = setLoadPageFunction;
	//this.setLoadPageDataFunction = setLoadPageDataFunction;
	this.setViewID = setViewID;
	this.setBookData = setBookData;

	/*
	@description sets the ID which is needed for multiple Viewers on a page so that they different components are connected properly together
	@param id the ID of the Viewer the class is connected to
	*/
	function setViewID(id) {
		viewID = id;
	}

	/*
	 @description sets the BookData, which is a XML file Object in METS/MODS specification.
	 This is used to generate the Chapter with all it's entries, chapters and the structure 
	 @param data the BookData which is used for this chapter
	 */
	function setBookData(data) {
		bookData = data;
	}

	/*function setLoadPageFunction(func) {
		loadPageFunction = func;
	}*/

	/*function setLoadPageDataFunction(func) {
		loadPageDataFunction = func;
	}*/

	/*
	@description defines if the Chapter is used with Effects or not and if so, all needed Values are read from the CSS file
 	@param value boolean which defines if effects are used (true) or not (false)
	 */
	function useEffects(value) {
		effects = (value == true)? true: false;
		if (effects) {
			//IE&Opera return Colorvalue as Hex, so conversion is needed
			bgColors[0] = colorToRGBArray(getCssProps("", "highlight", id, ["background-color"])[0]);
			bgColors[1] = colorToRGBArray(getCssProps("", "chapter", id, ["background-color"])[0]);
			bgColors[2] = colorToRGBArray(getCssProps("", "hover", id, ["background-color"])[0]);
		}
	}

	function hasEffects() {
		return effects;
	}

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
	@description removes Listeners from the listener-array
	*/
	function dropListener(type, theListener) {
		for (var i = 0; i < listener[type].length; i++) {
			if (listener[type][i] == theListener) {
				listener[type].splice(i,1);
			}
		}
	}

	/*
	@description notifoes all Listeners within the corresponding Abot the Change
	 and give them the corresponding id
	*/
	function notifyListenerChange(value) {
		if (!listener[chapter.PAGE_NUMBER]) {
			return;
		}
		for(var i = 0; i < listener[chapter.PAGE_NUMBER].length; i++) {
			listener[chapter.PAGE_NUMBER][i].change(value);
		}
	}

	/*
	@description notifies all Listeners that a given Item was clicked so that they can react
	*/
	function notifyListenerClick(item) {
		if (!listener[chapter.ITEM_CLICK]) {
			return;
		}
		for (var i = 0; i < listener[chapter.ITEM_CLICK].length; i++) {
			listener[chapter.ITEM_CLICK][i].clicked(item);
		}
	}

	/*
	@description notifies all Listeners that a ChapterResize occurred
	*/
	function notifyListenerResize() {
		if (!listener[chapter.RESIZE]) {
			return;
		}
		for (var i = 0; i < listener[chapter.RESIZE].length; i++) {
			listener[chapter.RESIZE][i].resized();
		}
	}
	
	/*
	@description read out the logical structure of the book
	*/
	function loadContent() {
		//Auslesen der logischen Strukturierung des Buches
		var Liste = getNodes(bookData, "mets:structMap",i, true);
		var structItem = null;//hält das Objekt mit den logischen Strukturinfos
		for (var i = 0; i < Liste.length; i++) {
			if (Liste[i].attributes.getNamedItem("TYPE").value.toLowerCase() == "logical") {
				structItem = Liste[i];
			}
		}

		if (structItem == null ) {
			alert("Error 1:No Mets File found");
		} else {
			structItem = getStructure(getFirstNodeByTagName(structItem.childNodes, "mets:div"), document.getElementById(id).firstChild, true);
		}
	}

	/*
	@description Realizes to Open and Close Chapter Entries
	*/
	function clickChap(e) {
		e = getEvent(e);
		var element = (typeof(arguments[1]) != "undefined")? arguments[1]: this;

		element.expanded = !element.expanded;
		element.getElementsByTagName("div").item(0).childNodes.item(0).className =(element.expanded)? "chapterImageSymbolMinus":"chapterImageSymbolPlus";
		for (var i = 0; i < element.childNodes.length; i++) {
			if ((element.childNodes.item(i).className == "chapter") || (element.childNodes.item(i).className == "chapter highlight") || (element.childNodes.item(i).className == "chapter hover")) {
				element.childNodes.item(i).style.display = (element.expanded)? "block":"none";
			}
		}

		// check out a free place
		if (my.self.firstChild.offsetHeight < document.getElementById(id+"_content").offsetHeight) {
			my.self.firstChild.style.top = 0 + "px";
		}

		// because of additional calling without event
		if (e) {
			e.cancelBubble = true;
			notifyListenerClick(element);
			posScroll();
		}

		return false;
	}
	
	/*
	@description realizes that by clicking an Entry we're switching to the given Image
	*/
	function clickPage(e) {
		e = getEvent(e);
		moveToPageByDMDID((typeof(arguments[1]) != "undefined")? arguments[1].DMDID:this.DMDID);
		e.cancelBubble = true;
		return false;
	}
	/*
	@description creates the structure of the chapter and his functions
	*/
	//TODO TOC.id = "chapter" muss raus, die Werte Einlesen später, irgendwie anders. Aktuell mehrere Tags mit gleicher ID
	function getStructure(chapter, parent, down) {
		var curNodes = chapter.childNodes;
		var structure = new Object();
		var TOC = document.createElement("div");
		TOC.className = "chapter";
		TOC.id = "chapter";
//		TOC.onclick = tocOnclick;

		if (down == true || down == false) {
			if (down == true) {
				TOC.expanded = true;
			}
		}
		for (var i = 0; i < chapter.attributes.length; i++) {
			TOC[chapter.attributes.item(i).nodeName] = chapter.attributes.item(i).value;
		}
		parent.appendChild(TOC);

		if (curNodes.length == 0) {
			ManageEvents.addEventListener(TOC, 'click', clickPage, false);
			var values = new Object();
			var Page = document.createElement("div");

			for (var i = 0; i < chapter.attributes.length; i++) {
				Page[chapter.attributes.item(i).nodeName] = chapter.attributes.item(i).value;
				values[chapter.attributes.item(i).nodeName] = chapter.attributes.item(i).value;
			}

			// makes a nice symbol in front of every pagename
			var pageImage = document.createElement("div");
			pageImage.className = "pageImageSymbol";
			Page.appendChild(pageImage);

			if (Page.LABEL) {
				Page.innerHTML = Page.innerHTML + Page.LABEL;
				Page.title = Page.LABEL;
			} else {
				Page.innerHTML = Page.innerHTML + Page.DMDID;
				Page.title = Page.DMDID;
			}

			// to highlight the current page or their centering
			pageIdCounter = pageIdCounter + 1;
			Page.id = id+"Pagenumber"+pageIdCounter;

			TOC.appendChild(Page);

			TOC["HOVER_SESSION"] = 0;

			// damit der Click auf kompletter Breite für die einzelnen Seiten abgefangen wird
//			TOC.onclick = movetoOnclick;
			//so that the click on the full width of the individual page will be catched
			checkChapterEntry(TOC);

			ManageEvents.addEventListener(TOC, 'mouseover', function(e){hoverCurrentPage(e, true);}, false);
			ManageEvents.addEventListener(TOC, 'mouseout', function(e){hoverCurrentPage(e, false);}, false);

			return values;
		} else {
			ManageEvents.addEventListener(TOC, 'click', clickChap, false);
			var span = document.createElement("div");

			var chapterImage = document.createElement("div");
			chapterImage.className = "chapterImageSymbolPlus";
			span.appendChild(chapterImage);
			span.innerHTML = span.innerHTML + TOC.LABEL;
			span.title = TOC.LABEL;
			span.style.color ="#000000";
			TOC.appendChild(span);

			// cut the name of the Entry until it fits
			checkChapterEntry(TOC);

			try {
				for (var i = 0; i < curNodes.length; i++) {
					if (curNodes.item(i).tagName) {
						structure[i] = new Object();
						if (down) {
							structure[i] = getStructure(curNodes.item(i), TOC, false);
						} else {
							structure[i] = getStructure(curNodes.item(i), TOC);
							for (var ii = 0; ii < chapter.attributes.length; ii++) {
								structure[chapter.attributes.item(ii).nodeName] = chapter.attributes.item(ii).value;
							}
						}
					}
				}
			} catch (e) {
				alertError("Some Problem occured while loading Chapter", 113, e);
			}
		}
		return TOC;
	}

	/*
	@description cut the name of the Enty until it fits
	*/
	function checkChapterEntry(TOC) {

		// determine the "number" of the indented levels of the chapterentries
		var findMainParent = TOC;
		var counter = 0;
		while (findMainParent.id != id) {
			counter = counter + 1;
			findMainParent = findMainParent.parentNode;
		}

		if (borderLeftWidth == null) {
			borderLeftWidth = getStyle("chapter","padding-left");
		}
		if (paddingLeft == null) {
			paddingLeft = getStyle("chapter","border-left-width");
		}
		if (pointsWidth == null) {
			pointsWidth = getTextWidth("...", getStyle(id+"_content","font-family"), parseInt(getStyle(id+"_content","font-size")));
		}

		var left = getStyle('chapter','left');
		var right = getStyle('chapter','right'); //right distance from the end of the text to the Content-Div

		var distanceDifference = parseInt(borderLeftWidth) +
				    parseInt(left) +
				    parseInt(paddingLeft);

		var distanceCur = parseInt(document.getElementById(id+"_content").style.width) - ((counter - 1) * distanceDifference) - parseInt(right);
		// otherwise Opera doesn't gets the symbolwidth --> is blended out in displayOutAllEntries
		TOC.style.display = "block";
		if (TOC.TYPE != "page") {
			// must be set otherwiese you will get a contradiction
			TOC.expanded = true;
			TOC.firstChild.firstChild.className = "chapterImageSymbolMinus";
		}
		if (TOC.TYPE == "page") {
			TOC.style.width = distanceCur + "px";
			distanceCur = distanceCur - pointsWidth - TOC.firstChild.firstChild.offsetWidth - 1;
		} else {
			TOC.style.width = distanceCur + parseInt(right) + "px";
			TOC.firstChild.style.width = distanceCur + parseInt(right) + "px";
			distanceCur = distanceCur - pointsWidth - TOC.firstChild.firstChild.offsetWidth - 1;
		}

		//-1 as tolerance, otherwise you will get unsightly shifts in Opera

		// cut the caption of the entry if it's to long until it fits

		var curTxt = TOC.firstChild.innerHTML.substring(TOC.firstChild.innerHTML.lastIndexOf('>') + 1, TOC.firstChild.innerHTML.length);

		if (isBrowser("IE")) {
			distanceCur = distanceCur - parseInt(paddingLeft) - parseInt(borderLeftWidth);
		}
		
		if (getTextWidth(curTxt, getStyle(id+"_content", "font-family"), getStyle(id+"_content", "font-size")) > 
			distanceCur + pointsWidth) {
				
			// now the current Txt will cut with the help of the largest letter "A"
			curTxt = curTxt.substring(0, Math.floor(distanceCur / getTextWidth("A", getStyle(id+"_content", "font-family"), getStyle(id+"_content", "font-size"))));
			TOC.firstChild.innerHTML =  TOC.firstChild.innerHTML.substring(0, TOC.firstChild.innerHTML.lastIndexOf('>') + 1) +
				/*cutTxtToWidth(*/curTxt/*, getStyle(id+"_content", "font-family"), getStyle(id+"_content", "font-size"), distanceCur)*/ +
				"...";
		}
	}

	function moveToPageByDMDID(DMDID) {
		var nodes = getNodes(bookData, "mets:file",0,true);
		for (var i= 0; i < nodes.length; i++) {
			if (nodes[i].attributes.getNamedItem("ID").nodeValue == DMDID) {
				var tempnode = getNodes(nodes[i], "mets:FLocat", 0, true);
				if (isBrowser("Safari")) {
					//search directly in the directory so that isn't created a new object everytime
					tempnode = bookData.getElementsByTagName("file")[i].getElementsByTagName("FLocat");
				}
				for (var ii = 0; ii < tempnode.length; ii++) {
					if (tempnode[ii].attributes && tempnode[ii].attributes.getNamedItem("LOCTYPE").nodeValue =="OTHER") {
						try {
				
							//eval(loadPageFunction+"("+loadPageDataFunction+"(parseInt(tempnode[ii].attributes.getNamedItem((!window.opera ? 'xlink:href':'href')).nodeValue), true,'"+viewID+"'), '"+viewID+"');");
							// updated pagecounter in the header |||page 0 doesn't exists --> page 1
							pageNumber = parseInt(tempnode[ii].attributes.getNamedItem((!window.opera ? "xlink:href":"href")).nodeValue) + 1;
							highlightCurrentPage(pageNumber);
							notifyListenerChange(pageNumber);

							// would also center by direct selection via chapter
							//showCurrentPageCenter(pageNumber);
						} catch (e) {
							alertError("Error by Moving to Page by clicking Chapter entry", 134, e);
						}
						return;
					}
				}
			}
		}
	}

	/*
	@description opens the current "list" in the chapter (for the first page)
	*/
	function openCurPageEntry() {
		// would close everything else
		// displayOutAllEntries ($(id+"_content").firstChild, 1, 1);
		var startPoint = document.getElementById(id+"Pagenumber"+pageNumber).parentNode;
		while (true) {
			if (startPoint.parentNode.id == id+"_content") {
				break;
			} else {
				if (startPoint.style.display != "block") {
					clickChap([], startPoint.parentNode);
					/*if (window.attachEvent && !window.opera) {
						startPoint.parentNode.click();
					} else {
						startPoint.parentNode.onclick();
					}*/
				}
				startPoint = startPoint.parentNode;
			}
		}
	}

	/*
	@description highlights the current page in the chapter
	*/
	function highlightCurrentPage() {
		// stop hover by continue counting the session
		var pageEl=document.getElementById(id+"Pagenumber"+pageNumber);
		pageEl.parentNode.HOVER_SESSION = 
			parseInt(pageEl.parentNode.HOVER_SESSION) + 1;
		pageEl.parentNode.style.backgroundColor = "";

		// at start / after reload still "0"
		if (lastHighlighted > 0) {
			document.getElementById(id+"Pagenumber"+lastHighlighted).parentNode.className = "chapter";
		}
		lastHighlighted = pageNumber;
		pageEl.parentNode.className = "chapter highlight";
	}

	/*
	@description determine which field should be highlighted by which color
	*/
	function hoverCurrentPage(e, hoverNow) {
		// seems to be not available from beginning, at least there was an error
		if (e.target || e.srcElement) {
			var element = (e.srcElement)? e.srcElement:e.target;
			// Cursor over Page symbol
			if (element.parentNode.id.lastIndexOf(id+"Pagenumber") != -1) {
				var currentPageId = element.parentNode.id;
			// Cursor over field
			} else if (element.firstChild.id.lastIndexOf(id+"Pagenumber") != -1) {
				var currentPageId = element.firstChild.id;
			// Cursor over text or fieldcontent
			} else {
				var currentPageId = element.id;
			}
			// if the entry who should be hover isn't changed by highlight
			if (currentPageId.substring((id+"Pagenumber").length,currentPageId.length) != pageNumber) {
				var obj = document.getElementById(currentPageId).parentNode;
				if (hoverNow) {
					obj.style.backgroundColor = '';
					obj.className = "chapter hover";
					// start new session (continue counting)
					obj.HOVER_SESSION = parseInt(obj.HOVER_SESSION) + 1;
				} else {
					if (effects) {
						changeColor(obj, copyArray(bgColors[2]), copyArray(bgColors[1]),
								parseInt(obj.HOVER_SESSION), chapHoverStep, chapHoverDelay, "obj.style.backgroundColor = ''; obj.className = 'chapter';");
					} else {
						obj.className = "chapter";
					}
				}
			} 
		}
	}

	/*
	@description changes the backgroundcolor of an chapter-textline by hover with mouse
	*/
	function changeColor(obj, startColor, targetColor, session, step, delay, doAfter) {

		if (parseInt(obj.HOVER_SESSION) == session) {

			if (!(startColor[0] == targetColor[0] && startColor[1] == targetColor[1] && startColor[2] == targetColor[2])) {

				if (startColor[0] < targetColor[0]) {
					startColor[0] = startColor[0] + step;
					if (startColor[0] > targetColor[0]) startColor[0] = targetColor[0];
				} else if (startColor[0] > targetColor[0]) {
					startColor[0] = startColor[0] - step;
					if (startColor[0] < targetColor[0]) startColor[0] = targetColor[0];
				}

				if (startColor[1] < targetColor[1]) {
					startColor[1] = startColor[1] + step;
					if (startColor[1] > targetColor[1]) startColor[1] = targetColor[1];
				} else if (startColor[1] > targetColor[1]) {
					startColor[1] = startColor[1] - step;
					if (startColor[1] < targetColor[1]) startColor[1] = targetColor[1];
				}

				if (startColor[2] < targetColor[2]) {
					startColor[2] = startColor[2] + step;
					if (startColor[2] > targetColor[2]) startColor[2] = targetColor[2];
				} else if (startColor[2] > targetColor[2]) {
					startColor[2] = startColor[2] - step;
					if (startColor[2] < targetColor[2]) startColor[2] = targetColor[2];
				}

				obj.style.backgroundColor = "rgb("+startColor[0]+","+startColor[1]+","+startColor[2]+")";

				setTimeout(function() {changeColor(obj, startColor, targetColor, session, step, delay, doAfter);}, delay);
			} else {
				eval(doAfter);
			}
		}
	}

	/*
	@description positions all Scrollbars depending to the given value
	*/
	function posScroll() {
		if (my == null) return;
		//var scrollBarX = my.scrollBarX;
		var scrollBarY = my.scrollBarY;
		
		/*scrollBarX.setMaxValue(Math.max(0, getWidth() - my.content.offsetWidth));
		var ratio = my.content.firstChild.offsetWidth / my.content.offsetWidth;
		scrollBarX.setLength(scrollBarX.my.space.offsetWidth / ratio);
		scrollBarX.setValue(-my.firstElement.offsetLeft);*/
		
		scrollBarY.setMaxValue(Math.max(0, getHeight() - my.content.offsetHeight));
		//ratio = my.firstElement.offsetHeight / my.self.offsetHeight;
		ratio = my.firstElement.offsetHeight / my.content.offsetHeight;
		scrollBarY.setLength(scrollBarY.my.space.offsetHeight / ratio);
		scrollBarY.setValue(-my.firstElement.offsetTop);
	}

	/*
	@description initialize chapter
	*/
	function init(identer) {
		var main = document.createElement("div");
		main.id = id;
		main.className = identer;
		document.getElementById(parent).appendChild(main);

		// decide if In/Out-Place
		//chapterBool = chapterEmbedded;
		
		// are needed later, therefore they have to be read by hand
		main.style.height = getStyle(id,"height");
		main.style.top = getStyle(id,"top");
		main.style.visibility = getStyle(id,"visibility");

		var content = document.createElement("div");
		content.id = id+"_content";
		content.className = identer + "_content";
		main.appendChild(content);

		var chapSort = document.createElement("div");
		chapSort.id = id+"_chapSort";
		chapSort.className = identer + "_chapSort";
		//chapSort.onclick = function() {chapterSort();};
		ManageEvents.addEventListener(chapSort, 'click', function() {chapterSort();}, false);
		main.appendChild(chapSort);

		// to determine content-width

		// Main Chapter Div
		main.style.width = getStyle(id,"width");
		main.style.zIndex = getStyle(id,"z-index");
		
		// load intervals of chapter-content
		content.style.right = getStyle(id+"_content","right");
		content.style.left = getStyle(id+"_content","left");
//TODO IE/Opera hat hier mit Angaben derart width:100% Probleme und spinnt total rum. Berechnung klappt also irgendwie nicht richtig
		content.style.width = parseInt(main.style.width) - parseInt(content.style.left) -
						     parseInt(content.style.right) + "px";

		loadContent();

		// have to initialize so that the current page can be centered & needed to work in IE
		content.firstChild.style.top = "0px";

		// create Elements to hold chapter Class Props
		var chapIn = document.createElement("div");
		chapIn.id = id+"In";
		chapIn.className = identer +"InPlace";
		main.appendChild(chapIn);

		var chapOut = document.createElement("div");
		chapOut.id = id+"Out";
		chapOut.className = identer +"OutPlace";
		main.appendChild(chapOut);
		
		//var scrollX = new scrollBar("scrollChap"+viewID, "scrollChap");
		var scrollY = new scrollBar("scrollChap"+viewID, "scrollChap");
		
		my = {'self': main, 'content':content, 'firstElement': content.firstChild, /*'scrollBarX': scrollX,*/ 'scrollBarY':scrollY};
		this.my = my;
		//var scrollBarChapter = Iview[viewID].scrollBarChapter;
		scrollY.init(false);
		scrollY.addListener(scrollBar.LIST_MOVE, new function() { this.moved = function(vector) {my.firstElement.style.top = -my.scrollBarY.getValue()+ "px";}});
		scrollY.setParent(id);
		scrollY.setSize(main.style.height);
		//TODO immer noch die letzte Seite mit drin stehen lassen als Orientierung
		//console.log();
		scrollY.setJumpStep(parseInt(my.content.clientHeight)-my.content.firstChild.firstChild.clientHeight);

		//EventRegistrations
		ManageEvents.addEventListener(document, 'mousemove', scrollY.mouseMove, false);
		ManageEvents.addEventListener(document, 'mouseup', scrollY.mouseUp, false);
		ManageEvents.removeEventListener(getElementsByClassName("empty","scrollChap"+viewID,"div")[0], 'mouseScroll', scrollY.scroll, false);
		ManageEvents.addEventListener(main, 'mouseScroll', function(e) { e = getEvent(e); scrollY.scroll(e); e.cancelBubble = true;}, false);
	}

	/*
	@description set width and height of the chapter 
	*/
	function setSize(width, height) {
		var el=document.getElementById(id);
		if (width != null) el.style.width = width + "px";
		if (height != null) el.style.height = height + "px";
		if (pageNumber != null) showCurrentPageCenter(pageNumber);
		notifyListenerResize(this);
		//my.scrollBarX.setSize(my.content.style.width);
		my.scrollBarY.setSize(my.self.style.height);
		posScroll();
	}

	/*
	@description gets the height of the first Child from chapter-content
	*/
	function getHeight() {
		return my.content.firstChild.offsetHeight;
	}

	/*
	@description gets the height of the first Child from chapter-content
	*/
	function getWidth() {
		return my.content.firstChild.offsetWidth;
	}

	/*
	@description calls needed functions if the page is changing
	*/
	function changePage(currentPage) {
		pageNumber = currentPage;
		openCurPageEntry();
		showCurrentPageCenter(pageNumber);
		posScroll();
		highlightCurrentPage();
	}

	/*
	@description pushes the current page to the center of the chapter-content
	*/
	function showCurrentPageCenter(currentPage) {
		// the entries doesn't fit into the placeholder
		var contentEl=document.getElementById(id+"_content");
		if (contentEl.firstChild.offsetHeight > contentEl.offsetHeight) {
			// calculate distance from the current page to the top edge of the chapter
			var curPage = document.getElementById(id+"Pagenumber"+currentPage);
			var startPoint = curPage;
			var curDistance = 0;

			//enough anyway, because the heighest distance / offsetTop is "0"
			while (startPoint.parentNode.parentNode.id != id+"_content") {
				curDistance = curDistance + startPoint.parentNode.offsetTop;
				startPoint = startPoint.parentNode;
			}

			curDistance = curDistance + parseInt(contentEl.firstChild.style.top) - ((contentEl.offsetHeight - 
					curPage.offsetHeight) / 2);

			contentEl.firstChild.style.top = parseInt(contentEl.firstChild.style.top) - curDistance + "px";

			// so that not too much is moved (beyond the empty fields)

			// bottom
			if (-parseInt(contentEl.firstChild.style.top) + contentEl.offsetHeight > 
					contentEl.firstChild.offsetHeight) {
				contentEl.firstChild.style.top = - (contentEl.firstChild.offsetHeight - contentEl.offsetHeight)
									+ "px";
			}

			// top
			if (parseInt(contentEl.firstChild.style.top) > 0) {
				contentEl.firstChild.style.top = 0 + "px";
			}
		} else {
			// wenn nach sortierung alles zusammen reinpasst, oben orientieren
			contentEl.firstChild.style.top = 0 + "px";
		}
	}

	// currently not used (maybe later)
	/*function pressEachChapterEntries(startPoint, layers) {
		pressEach(startPoint, 0, layers);
	}*/

	/*
	@description 
	*/
	/*function pressEach(startPoint, index, lastClickedLayer) {
		// recursively, clicks all chapterentries from the plane firstLayer
		// edit planes (1-...) from top to bottom ||| "1" because zeroth child concerns the chapterhead
		for (window["index"+index] = 1; window["index"+index] < startPoint.childNodes.length; window["index"+index]++) {
			if (startPoint.childNodes[window["index"+index]].TYPE == "chapter") {
				pressEach(startPoint.childNodes[window["index"+index]], index+1, lastClickedLayer);
				if (index >= lastClickedLayer) {
					//startPoint.childNodes[window["index"+index]].onclick();
					clickChap([],startPoint.childNodes[window["index"+index]]);
				}
			}
		}
	}*/

	
	function displayOutAllEntries(startPoint, layers) {
		displayOut(startPoint, 0, layers);
	}

	/*
	@description recursively, closes all entries from the layer lastShownLayer
	*/
	function displayOut(startPoint, index, lastShownLayer) {
		// recursively, clicks all chapterentries from the plane lastShownLayer
		// edit planes (1-...) from top to bottom ||| "1" because zeroth child concerns the chapterhead
		for (window["index"+index] = 1; window["index"+index] < startPoint.childNodes.length; window["index"+index]++) {
			if ((startPoint.childNodes[window["index"+index]].TYPE == "chapter") && 
				(startPoint.childNodes[window["index"+index]].expanded == true)) {

				displayOut(startPoint.childNodes[window["index"+index]], index+1, lastShownLayer);
				if (index >= lastShownLayer) {
					clickChap([],startPoint.childNodes[window["index"+index]]);
				}
			}
		}
	}

	function chapterSort() {
		displayOutAllEntries(document.getElementById(id+"_content").firstChild, 0);
		changePage(pageNumber);
	}
}
