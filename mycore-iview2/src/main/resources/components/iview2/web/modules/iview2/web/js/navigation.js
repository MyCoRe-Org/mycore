/*
@description checks if the requested page is in Bounds, if so all needed steps to show the controls for this page correctly will be applied(forward/backward buttons, correct textbox value aso.)
@param page the new Page which shall be displayed, will be checked if it's in bounds
@param viewID
@param arguments[2] if this one is set the loadPage Function will not be executed so no needless PageChange is called
*/

function navigatePage(pNum, viewID) {
	notifyListenerNavigate(pNum, viewID);
	Iview[viewID].pagenumber = checkLastNextControls(pNum, viewID);
	if (classIsUsed("BSE_pageInput1")) {
		Iview[viewID].pageInputObj.actualize(Iview[viewID].pagenumber);
		//doForEachInClass("input",".value = '"+arabToRoem(Iview[viewID].pagenumber)+"';", viewID);
	}
	if (classIsUsed("BSE_pageForm1")) {
		Iview[viewID].pageFormObj.actualize(Iview[viewID].pagenumber);
	}
	if (arguments.length == 2) {
		loadPage(viewID);
	} else
		updateModuls(viewID);	

}

/*
@description checks if the given page-Number is out of bounds or not, corrects it if needed and sets the buttons to the related values
@param pNum which is proofed if it's in bounds or not
@return pNum which is in bounds
*/
function checkLastNextControls(pNum, viewID) {
	if (classIsUsed("BSE_forward")) doForEachInClass("BSE_forward",".style.display = 'block';", viewID);
	if (classIsUsed("BSE_backward")) doForEachInClass("BSE_backward",".style.display = 'block';", viewID);

	if(pNum >= Iview[viewID].amountPages) {
		pNum = Iview[viewID].amountPages;
		if (classIsUsed("BSE_forward")) doForEachInClass("BSE_forward",".style.display = 'none';", viewID);
	}
	if(pNum <= 1) {
		pNum = 1;
		if (classIsUsed("BSE_backward")) doForEachInClass("BSE_backward",".style.display = 'none';", viewID);
	}
	return pNum;
}