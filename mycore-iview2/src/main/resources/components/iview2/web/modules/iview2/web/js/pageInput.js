// TODO: wird viewId überhaupt benötigt?

function pageInput(newId, parent, identer) {
	// self calculated
	var id = newId;
	var identer = identer;
	var viewID = "";
	var amountInputs = 1;
	
	// Listener
	var listener = [];
	pageInput.PAGE_NUMBER = 0;
	
	var my = [];

	//Function declarations:	
	this.init = init;
	this.initNext = initNext;
	this.addListener = addListener;
	this.actualize = actualize;
	this.setViewID = setViewID;
	this.setNumberOfPages = setNumberOfPages;
	
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
		if (!listener[pageInput.PAGE_NUMBER]) {
			return;
		}
		for(var i = 0; i < listener[pageInput.PAGE_NUMBER].length; i++) {
			listener[pageInput.PAGE_NUMBER][i].change(value);
		}
	}

	function inputKey(e){
		e = getEvent(e);
		var inputElement = (e.srcElement)? e.srcElement:e.target;
		
		// wenn keine Zahl kommt, nichts machen
		//my.input.value = my.input.value.replace(/[^0-9]/g,'');
		inputElement.value = inputElement.value.replace(/[^0-9]/g,'');
		e.preventDefault();

		if(e.keyCode == 13) {//treatment, if "Enter"-Button was pressed
			/*if(!isNaN(parseInt(my.input.value))){ 
				notifyListenerChange(parseInt(my.input.value));
			}*/
			if(!isNaN(parseInt(inputElement.value))){ 
				notifyListenerChange(parseInt(inputElement.value));
			}
		}
	}
	
	function init() {
		if (arguments[0]) amountInputs = arguments[0];
		
		var main = document.createElement("form");
		main.id = id + amountInputs;
		main.className = identer + amountInputs;
		$(parent).appendChild(main);
		
		var input = document.createElement("textarea");
		input.className = "input";
		input.name = "input";
		main.appendChild(input);

		EventUtils.addEventListener(input, 'keypress', function(e){e.cancelBubble = true;}, false);
		EventUtils.addEventListener(input, 'keyup', function(e){inputKey(e);}, false);
		
		var slash = document.createElement("textarea");
		slash.className = "slash";
		slash.name = "slash";
		slash.innerHTML = "/";
		slash.readOnly = true;
		main.appendChild(slash);
		
		var amount = document.createElement("textarea");
		amount.className = "amount";
		amount.name = "amount";
		amount.readOnly = true;
		main.appendChild(amount);
		
		my[amountInputs] = {'main':main, 'input':input, 'slash':slash, 'amount':amount};
	}
	
	function initNext() {
		init(amountInputs + 1);
	}
	
	function actualize(pNum) {
		for (i = 1; i <= amountInputs; i++) {
			my[i].input.value = pNum;
		}
	}
	
	function setViewID(id) {
		viewID = id;
	}
	
	function setNumberOfPages(value) {
		for (i = 1; i <= amountInputs; i++) {
			my[i].amount.value = value;
		}
	}
}
