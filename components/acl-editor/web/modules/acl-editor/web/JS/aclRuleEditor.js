/* include aclClickButtons.js */

var ruleBox = new aclRuleBox();

function aclRuleBox(){
    this.detailsButtons = new Array();
    this.delCheckboxes = new Array();
}

aclRuleBox.prototype.addButton = function(button){
    this.detailsButtons = this.detailsButtons.concat(button)
}

aclRuleBox.prototype.addCheckbox = function(checkbox){
    this.delCheckboxes = this.delCheckboxes.concat(checkbox)
}

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
    
        initAclEditRuleBox();
        
        initCreateNewRuleButton()
        
        this.setup = true;
        
    }
}   

/*
 * Init the create new rule submit button
 */
function initCreateNewRuleButton(){
    var submitButton = document.getElementById("createNewRuleButon");
    
    submitButton.onclick = createNewRule;
}

/*
 * Init the edit rule box
 */
function initAclEditRuleBox(){
    var ruleBoxElem = document.getElementById("aclEditRuleBox");
    //ruleBox.detailsButtons = new Array();
    //ruleBox.delCheckboxes = new Array();
    
    // rule tables
    var ruleTables = ruleBoxElem.getElementsByTagName("table");
    
    for (i=0; i < ruleTables.length; i++){
        var currentTable = ruleTables[i];
        var ruleTableObj = initRuleTable(currentTable);
        var currentButton = ruleTableObj.detailsButton;
        var currentCheckbox = ruleTableObj.delCheckbox;
        
        if (ruleBox){
            //ruleBox.detailsButtons = ruleBox.detailsButtons.concat(currentButton);
            //ruleBox.delCheckboxes = ruleBox.delCheckboxes.concat(currentCheckbox);
            ruleBox.addButton(currentButton)
            ruleBox.addCheckbox(currentCheckbox)
            ruleBox.elem = ruleBoxElem;
        }
    }
    
    initAclDetailsAllButton();
        
    initAclDelAllRulesButton();

    initAclDelAllRulesCheckBox();
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
    var ruleDescId = "RuleDesc$" + table.id;
    var ruleStringId = "RuleString$" + table.id;
    var button = new Object();
    
    // init the description field
    var ruleDesc;
    if(ruleDesc = document.getElementById(ruleDescId))
        ruleDesc.onchange = setChanged;
    
    // init the description field
    var ruleString;
    if(ruleString = document.getElementById(ruleStringId))
        ruleString.onchange = setChanged;
    
    // init the delete checkbox
    var checkBox;
    if(checkBox = document.getElementById(checkBoxId))
        checkBox.onclick = setRuleAsDeleted;
    
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
    
    var ruleTable = new Object();
    ruleTable.detailsButton = button.obj;
    ruleTable.delCheckbox = checkBox;
    return ruleTable;
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

/*
 * Init the delete all rules checkbox
 */
function initAclDelAllRulesCheckBox(){
    var delAllRulesCheckBox = document.getElementById("delAllRulesCheckBox");
    
    if (delAllRulesCheckBox)
        delAllRulesCheckBox.onclick = setAllRulesAsDeleted;
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
        document.getElementById("delAllRulesForm").submit();
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
    var ruleBoxForm = document.getElementById("aclEditRuleBoxForm");
    
    if (node.type.toLowerCase() == "checkbox"){
        if (node.checked == true){
            var newInput = document.createElement("input");
            newInput.setAttribute("type", "hidden");
            newInput.name = deleted + node.value;
            newInput.id = deleted + node.value;
    
            ruleBoxForm.appendChild(newInput);
        } else if (node.checked == false){
            var nodeMarkedAsDeleted = document.getElementById(deleted + node.value);
            if (nodeMarkedAsDeleted)
                ruleBoxForm.removeChild(nodeMarkedAsDeleted);
        }
    }
    
}

/*
 * Set all rules as deleted when checkbox is pressed
 */
function setAllRulesAsDeleted(event){
    var delRuleCheckboxes = ruleBox.delCheckboxes;
    
    for (i=0; i < delRuleCheckboxes.length; i++){
        var currentCheckbox = delRuleCheckboxes[i];
        
        if (currentCheckbox)
        currentCheckbox.click();
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
    //this.msgWindow.removeChild(this.msgWindow.debug);
}

function createNewRule(event){
    var msg = "";
    var msgDesc = this.getAttribute("msgDesc"); 
    var msgRule = this.getAttribute("msgRule");
    var descElem = document.getElementById("aclNewRuleDesc");
    var ruleElem = document.getElementById("aclNewRuleStr");
    var descStr = descElem.value;
    var ruleStr = ruleElem.value;
    var noErrors = true;
    
    if (descStr.removeSpace().length <= 0){
        noErrors = false;
        msg = msg + "\n" + msgDesc;
        descElem.className = "faultyInput";
    } else {
        descElem.className = "input";
    }
    
    if (ruleStr.removeSpace().length <= 0){
        noErrors = false;
        msg = msg + "\n" + msgRule;
        ruleElem.className = "faultyTextarea";
    } else {
        ruleElem.className = "textarea";
    }
    
    if (noErrors)
        document.getElementById("aclCreateNewRuleForm").submit();
    else
        alert(msg)
    
    
}

String.prototype.removeSpace = function() {
  return this.replace(/\s+/g, "");
}

/*
 * When rules get changed
 */
function setChanged(event){
    var changed = "changed$";
    
    var name = this.name;
    
    if (!name.match("changed")){
        this.name = changed + name;
    }
}