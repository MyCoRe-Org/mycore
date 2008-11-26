/* include aclClickButtons.js */

var permBox = new aclPermBox();

function aclPermBox(){
    this.delAllAclPermsButton;
    this.newRuleSelectBox;
    this.newPermObjId;
    this.newPermAcpool;
    this.delPermsCheckboxes = new Array();
}

var setup = false;

/************************************************************************
  Init part
 ************************************************************************/
 
/*
 * Init the permission editor
 */
function aclPermEditorSetup(){
    if (!this.setup){
        this.onmouseover = null;
        
        initCreateNewRuleSubmit()
        
        initDelAllAclPermsButton();
        
        initAclEditPermBox();
        
        initDelAllAclPermsCheckbox()
        
        this.setup = true;
        
    }
}


/*
 * Init the "delete all permissions" button
 */
function initDelAllAclPermsButton(){
    var spec = {onclick: "delAllAclPerms", 
                cssMouseOver: "clickButtonOver", 
                cssMouseOut: "clickButtonOut"};
                
    permBox.delAllAclPermsButton = createClickButton("delAllAclPerms", spec);
}

/*
 * Init the "delete all permissions" checkbox
 */
function initDelAllAclPermsCheckbox(){
    var delAllCheckbox = document.getElementById("delAllCheckBox");
    
    if (delAllCheckbox){
    delAllCheckbox.label = document.getElementById("checkBoxRowLabel");
    if(document.all)
        delAllCheckbox.labelChecked = delAllCheckbox.getAttributeNode("labelChecked").value;
    else
        delAllCheckbox.labelChecked = delAllCheckbox.getAttribute("labelChecked");
    delAllCheckbox.labelStd = delAllCheckbox.label.innerHTML;
    
    delAllCheckbox.onclick = function(event){
        var checkBoxes = permBox.delPermsCheckboxes;
        
        for (i=0; i < checkBoxes.length; i++){
            var currentCheckbox = checkBoxes[i];
            
            if (this.checked != currentCheckbox.checked)
                currentCheckbox.click();
        }
        
        if (this.checked)
            this.label.innerHTML = this.labelChecked;
        else
            this.label.innerHTML = this.labelStd;
    }
    }
}

/*
 * Init the create new permission submit button
 */
function initCreateNewRuleSubmit(){
    var newRuleSubmitButton = document.getElementById("newPermSubmitButton");
    
    if (newRuleSubmitButton){
    var newRuleSelectBox = document.getElementById("createNewPermFormSelBox");
    var newPermObjId = document.getElementById("newPermObjId");
    var newPermAcpool = document.getElementById("newPermAcpool");
    
    permBox.newRuleSelectBox = newRuleSelectBox;
    permBox.newPermObjId = newPermObjId;
    permBox.newPermAcpool = newPermAcpool;
    
    newRuleSubmitButton.onclick = createNewPermission;
    
    newRuleSelectBox.onchange = function(event){
        this.changed = true;
    }
    }
    
}

/*
 * Init the edit permission box
 */
function initAclEditPermBox(){
    var permBoxElem = document.getElementById("aclEditPermBox");
    
    if (permBoxElem){
    var tables = permBoxElem.getElementsByTagName("table");
    
    for (i=0; i < tables.length; i++){
        var className;
        if(document.all)
            className = tables[i].getAttributeNode("class").value;
        else
            className = tables[i].getAttribute("class");
            
        var currentTable = tables[i];
        if (className == "aclPermTable"){
            
            var currentID = currentTable.id;
            var checkBoxID = 'checkBox$' + currentID;
            var currentCheckBox = document.getElementById(checkBoxID);
            
            if (currentCheckBox){
            currentCheckBox.onclick = setPermAsDeleted;
            permBox.delPermsCheckboxes = permBox.delPermsCheckboxes.concat(currentCheckBox);
            }
            
            var selectID = 'select$' + currentID;
            var currentSelectBox = document.getElementById(selectID);
            if(currentSelectBox)
                currentSelectBox.onchange = setRuleChanged;
        }
    }
    }
}


/************************************************************************
  Functional part
 ************************************************************************/
 
/*
 * What to do when delAllAclPerms button is pressed
 */
function delAllAclPerms(event){
    var cmd = this.getAttribute("cmd");
    var msg = this.getAttribute("msg");
    var chk = window.confirm(msg);
    
    if (chk == true) {
        document.getElementById("delAllAclPermsForm").submit();
    }
}

/*
 * Set permission as deleted
 */
function setPermAsDeleted(event){
    var deleted = "deleted$";
    var node = this;
    var permBoxForm = document.getElementById("aclEditPermBoxForm");
    
    if (node.type.toLowerCase() == "checkbox"){
        if (node.checked == true){
            var newInput = document.createElement("input");
            newInput.setAttribute("type", "hidden");
            newInput.name = deleted + node.value;
            newInput.id = deleted + node.value;
    
            permBoxForm.appendChild(newInput);
        } else if (node.checked == false){
            var nodeMarkedAsDeleted = document.getElementById(deleted + node.value);
            if (nodeMarkedAsDeleted)
                permBoxForm.removeChild(nodeMarkedAsDeleted);
        }
    }
}

/*
 * When rules get changed
 */
function setRuleChanged(event){
    var changed = "changed$";
	
    var name = this.name;
    
	if (!name.match("changed")){
		this.name = changed + name;
	}
}

/*
 * Create new permission, "submit button"
 */
function createNewPermission(event){
    var msg = "";
    var msgSelBox = this.getAttribute("msgSelBox"); 
    var msgObjId = this.getAttribute("msgObjId"); 
    var msgAcPool = this.getAttribute("msgAcPool"); 
    var objIdStr = permBox.newPermObjId.value;
    var acpoolStr = permBox.newPermAcpool.value;
    var noErrors = true;
    
        
    if (!permBox.newRuleSelectBox.changed){
        noErrors = false;
        msg = msgSelBox;
	    permBox.newRuleSelectBox.className = "faultyInput";
	} else {
        permBox.newRuleSelectBox.className = "input";
    }
           
        if (objIdStr.removeSpace().length <= 0){
            noErrors = false;
            msg = msg + "\n" + msgObjId;
            permBox.newPermObjId.className = "faultyInput";
        } else {
            permBox.newPermObjId.className = "input";
        }
        
        if (acpoolStr.removeSpace().length <= 0){
            noErrors = false;
            msg = msg + "\n" + msgAcPool;
            permBox.newPermAcpool.className = "faultyInput";
        } else {
            permBox.newPermAcpool.className = "input";
        }
        
        if (noErrors)
            document.getElementById("createNewPermForm").submit();
        else
            alert(msg);
}

String.prototype.removeSpace = function() {
  return this.replace(/\s+/g, "");
}