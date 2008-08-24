var ruleBox;
var setup = false;

/************************************************************************
  Init part
 ************************************************************************/
 
/*
 * Init the rule editor
 */
function aclRuleEditorSetup(){
    if (!this.setup){
        this.onmouseover = null;
    
        initAclDetailsButtons();
        
        initAclDetailsAllButton();
        
        initAclDelAllRulesButton();
    
        this.setup = true;
        
    }
}   

/*
 * Init the "+"-details button
 */
function initAclDetailsButtons(){
    ruleBox = document.getElementById("aclEditRuleBox");
    ruleBox.detailsButtons = new Array();
    
    var ruleTables = ruleBox.getElementsByTagName("table");
    
    for (i=0; i < ruleTables.length; i++){
        var currentTable = ruleTables[i];
        var currentButton = initRuleTable(currentTable);
        
        ruleBox.detailsButtons = ruleBox.detailsButtons.concat(currentButton);
    }
}

/*
 * Init the rule table, get the "+"-button and init the checkbox
 */
function initRuleTable(table){
    var tableId = table.id;
    var buttonId = "RuleFieldButton$" + table.id;
    var targetId = "RuleField$" + table.id;
    var checkBoxId = "CheckBox$" + table.id;
    var ruleInUseId = "RuleInUse$" + table.id;
    var button = new Object();
    
    // init the checkbox
    var checkBox;
    if(checkBox = document.getElementById(checkBoxId))
        checkBox.onchange = setRuleAsDeleted;
    
    // making the hover info message for rules 
    // wich can not be set as deleted
    var ruleInUse;
    if (ruleInUse = document.getElementById(ruleInUseId)){
        ruleInUse.onmouseover = displRuleInUseMsg;
        ruleInUse.onmouseout = removeRuleInUseMsg;
    }
    
    // init the "+"-button
    var spec = {onclick: "detailsSwitch", 
                target: targetId,
                cssMouseOver: "detailsSwitch clickButtonOver", 
                cssMouseOut: "detailsSwitch clickButtonOut"};
                
    button.obj = createClickButton(buttonId, spec);
    
    var elem;
    // the if-cases check if the objects are loaded
    if (button.obj){
        if(elem = button.obj.elem){
            button.obj.elem.detailsOn = "detailsOn";
            button.obj.elem.detailsOff = "detailsOff";
            button.obj.elem.status = button.obj.elem.detailsOff;
        }
        
    }
    
    return button.obj;
}

/*
 * Init the details all button
 */
function initAclDetailsAllButton(){
    var detailsAllButton = new Object();
    var detailsAllSpec = 
                {   onclick: "detailsAll",
                    cssMouseOver: "clickButtonOver", 
                    cssMouseOut: "clickButtonOut"
                };
                
    detailsAllButton = createClickButton("detailsAllButton", detailsAllSpec);
    var elem;
    // the if-cases check if the objects are loaded
    if (detailsAllButton){
        if(elem = detailsAllButton.elem){
            detailsAllButton.elem.detailsOn = "detailsOn";
            detailsAllButton.elem.detailsOff = "detailsOff";
            detailsAllButton.elem.status = detailsAllButton.elem.detailsOff;
            detailsAllButton.elem.label = detailsAllButton.elem.innerHTML;
            if(detailsAllButton.elem.getAttribute("altLabel"))
                detailsAllButton.elem.altLabel = detailsAllButton.elem.getAttribute("altLabel");
        }
        
    }
    
    ruleBox.detailsAllButton = detailsAllButton;
}

/*
 * Init the delete all rules button
 */
function initAclDelAllRulesButton(){
    var delAllRulesButton = new Object();
    var delAllRulesSpec = 
                {   onclick: "delAllRules",
                    cssMouseOver: "clickButtonOver", 
                    cssMouseOut: "clickButtonOut"
                };
                
    delAllRulesButton = createClickButton("delAllRulesButton", delAllRulesSpec);
}

/************************************************************************
  Functional part
 ************************************************************************/
 
/*
 * What to do when detailsAllButton is pressed
 */
function delAllRules(event){
    var cmd = this.getAttribute("cmd");
    var msg = this.getAttribute("msg");
    var chk = window.confirm(msg);
    
    if (chk == true) {
        self.location.href=cmd;
    }
}

/*
 * What to do when detailsAllButton is pressed
 */
function detailsAll(event){
    var detailsButtons = ruleBox.detailsButtons;
    var status = this.status;
    
    for (i=0; i < detailsButtons.length; i++){
        var currentButton;
        var elem;
        
        /* the if case is to prevent the "no properties" error */
        if(currentButton = detailsButtons[i]){
            
            if(elem = currentButton.elem){
                
                if (elem.status == status)
                    elem.onclick(event);
            }
        }
    }
    
    if (status == this.detailsOn){
        this.status = this.detailsOff;
        this.innerHTML = this.label
    } else{
        this.status = this.detailsOn;
        this.innerHTML = this.altLabel
    }
}

/*
 * What to do when "+"-button is pressed
 */
function detailsSwitch(event){
    var _self = this;
    var targetNode = document.getElementById(this.target)
    
    var status = null;
    
    try {
        status = targetNode.currentStyle.display;
        
        if (status == "none"){
            targetNode.style.display = "block";
            _self.firstChild.nodeValue="-";
            this.status = this.detailsOn;
        } else{
            targetNode.style.display = "none";
            _self.firstChild.nodeValue="+";
            this.status = this.detailsOff;
        }
    }
    catch(error){
        targetNode.style.display="table-row";
        status = document.defaultView.getComputedStyle(targetNode, null).visibility;
        
        if (status == "collapse"){
            targetNode.style.visibility = "visible";
            _self.firstChild.nodeValue="-";
            this.status = this.detailsOn;
        } else{
            targetNode.style.visibility="collapse";
            _self.firstChild.nodeValue="+";
            this.status = this.detailsOff;
        }
    }
}

/*
 * Set the rule as deleted when checkbox is pressed
 */
function setRuleAsDeleted(event){
    var deleted = "deleted$";
    var node = this;
    
    if (node.type.toLowerCase() == "checkbox"){
        if (node.checked == true){
            var newInput = document.createElement("input");
            newInput.name = deleted + node.value;
            newInput.id = deleted + node.value;
    
            node.appendChild(newInput);
        } else if (node.checked == false){
            var nodeMarkedAsDeleted = document.getElementById(deleted + node.value);
            if (nodeMarkedAsDeleted)
                node.removeChild(nodeMarkedAsDeleted);
        }
    }
}

/*
 * Display info message for rules which can not be set as deleted
 */
function displRuleInUseMsg(event){
    var msgWindowId = this.getAttribute("msgID");
    var msgWindow = document.getElementById(msgWindowId);
    
    this.style.cursor = "help";
    
    var windowWidth;
    var windowHeight;
    
    if (!this.windowWidth){
    if(msgWindow.currentStyle){
        // IE
        windowWidth = msgWindow.currentStyle.width;
        windowHeight = msgWindow.currentStyle.height;
    } else{
        windowWidth = document.defaultView.getComputedStyle(msgWindow, null).width;
        windowHeight = document.defaultView.getComputedStyle(msgWindow, null).height;
    }
    
    this.windowWidth = parseInt(windowWidth);
    this.windowHeight = parseInt(windowHeight);
    } 
    
    windowWidth = this.windowWidth;
    windowHeight = this.windowHeight;
    
    // get mouse position
    var mouseX;
    var mouseY;
    if (window.event){
        mouseX = window.event.clientX;
        mouseY = window.event.clientY;
    } else{
        mouseX = event.pageX;
        mouseY = event.pageY;
    }
    
    
    
    var left = mouseX -10 - windowWidth;
    var top = mouseY -10 - windowHeight;
    
    // dirty method to check for IE
    if (document.all){
    	// need to add scroll distance in IE
    	var scrollTop = document.documentElement.scrollTop;
    	var scrollLeft = document.documentElement.scrollLeft;
    	
		top = top + scrollTop;
		left = left + scrollLeft;
   	}
    
    msgWindow.style.left = left+"px";
    msgWindow.style.top = top+"px";
    msgWindow.style.display = "block";
    
    var debug = document.createElement("p");
    debug.innerHTML = " mouse: " + mouseX + " # " + mouseY + " parent: " + document.documentElement.scrollTop;
    
    
    
    //msgWindow.appendChild(debug);
    //msgWindow.debug = debug;
    	
    if(msgWindow)
        this.msgWindow = msgWindow;
}

function removeRuleInUseMsg(event){
    this.msgWindow.style.display = "none";
    this.msgWindow.removeChild(this.msgWindow.debug);
}