var redirectURL = "";

function reloadTree(){
    log("reloadTree()");
    redirectURL = reloadTreeURL;
    displayConfirmReloadDialog('Zuletzt gespeicherte Version laden');
}

function resetTree(){
    log("resetTree()");
    redirectURL = resetTreeURL;
    displayConfirmReloadDialog('Strukturansicht zurücksetzen');
}

function displayConfirmReloadDialog(titleToDisplay){
    log("displayConfirmReloadDialog()");
    var dialog = dijit.byId('confirmReloadDialog');
    dialog.set('title', titleToDisplay);
    dialog.show();
}

function redirect(){
    log("redirect()");
    log("Redirecting to " + redirectURL);
    window.location = redirectURL;
}
