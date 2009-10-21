function pageForm(newId, parent, identer) {
	// self calculated
	var id = newId;
	var identer = identer;
	var viewID = "";
	// counter for all Page-Forms
	var amountForms = 1;
	
	// Listener
	var listener = [];
	pageForm.PAGE_NUMBER = 0;
	
	var my = [];

	//Function declarations:	
	this.init = init;
	this.initNext = initNext;
	this.fill = fill;
	this.addListener = addListener;
	this.actualize = actualize;
	this.setViewID = setViewID;
	
	function addListener(type, theListener) {
		if (!listener[type]) {
			listener[type] = [];
		}
		listener[type].push(theListener);
	}

	function dropListener(type, theListener) {
		for (var i = 0; i < listener[type].length; i++) {
			if (listener[type][i] == theListener) {
				listener[type].splice(i,1);
			}
		}
	}

	function notifyListenerChange(value) {
		if (!listener[pageForm.PAGE_NUMBER]) {
			return;
		}
		for(var i = 0; i < listener[pageForm.PAGE_NUMBER].length; i++) {
			listener[pageForm.PAGE_NUMBER][i].change(value);
		}
	}
	
	function init() {
		if (arguments[0]) amountForms = arguments[0];
	
		main = document.createElement("form");
		main.id = id + amountForms;
		main.className = identer + amountForms;
		$(parent).appendChild(main);
		
		pEl = document.createElement("p");
		main.appendChild(pEl);
		
		selection = document.createElement("select");
		selection.className = identer + "Select" + amountForms;;
		selection.size = "1";
		selection.style.fontFamily = "arial";
		selection.style.fontSize = "14px";
		// TODO: EventListener???
		//selection.onchange = function() {navigatePage(this.value, viewID)};
		selection.onchange = function() {notifyListenerChange(this.value)};
		
		pEl.appendChild(selection);
		
		my[amountForms] = {'main' : main, 'selection' : selection};
	}
	
	function initNext() {
		init(amountForms + 1);
	}
	
	function fill(numberOfPages) {
		for (cur = 1; cur <= amountForms; cur++) {
			for (i = 1; i <= numberOfPages; i++) {
				var option = document.createElement("option");
				//if (i/2 - parseInt(i/2) == 0) {
					option.className = identer + "Option" + cur;
				/*} else {
					option.className = identer + "Option2";
				}*/
				option.value = i;
				// TODO: eine Variable in XML die römische Ziffern ein bzw ausschaltet
				//if (cur == 1) {
					option.innerHTML = arabToRoem(i) + "  /  " + arabToRoem(numberOfPages);
				/*} else {
					option.innerHTML = i + "  /  " + numberOfPages;
				}*/
				my[cur].selection.appendChild(option);
			}
		}
	}
	
	function actualize(pNum) {
		for (i = 1; i <= amountForms; i++) {
			my[i].selection.selectedIndex = pNum - 1;
		}
	}
	
	function setViewID(id) {
		viewID = id;
	}
}