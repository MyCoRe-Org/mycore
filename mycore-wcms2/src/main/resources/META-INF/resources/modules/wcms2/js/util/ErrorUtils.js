var wcms = wcms || {};
wcms.util = wcms.util || {};
wcms.util.ErrorUtils = wcms.util.ErrorUtils || {};

/*-------------------------------------------------------------------------
 * Util class to handle errors
 *------------------------------------------------------------------------*/

wcms.util.ErrorUtils.show = function(/* JSON */error, /*JSON*/ args) {

	if (error.errorType == "unexpected") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.error.unexpectedCaption",
				"component.mt-wcms.error.unexpectedLabel");
		errorDialog.show();
	} else if (error.errorType == "unknownType") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.error.unknownTypeCaption",
				"component.mt-wcms.error.unknownTypeLabel");
		errorDialog.show();
	} else if (error.errorType == "notMyCoReWebPage") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.navigation.error.notMyCoReWebPageCaption",
				"component.mt-wcms.navigation.error.notMyCoReWebPageLabel");
		errorDialog.show();
	} else if (error.errorType == "invalidDirectory") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.navigation.error.invalidDirectoryCaption",
				"component.mt-wcms.navigation.error.invalidDirectoryLabel");
		errorDialog.show();
	} else if (error.errorType == "notExist") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.navigation.error.notExistCaption",
				"component.mt-wcms.navigation.error.notExistLabel");
		errorDialog.show();
	} else if (error.errorType == "invalidFile") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.navigation.error.invalidFileCaption",
				"component.mt-wcms.navigation.error.invalidFileLabel");
		errorDialog.show();
	} else if (error.errorType == "couldNotSave") {
		var errorDialog = new wcms.gui.ErrorDialog(
				"component.mt-wcms.navigation.error.couldNotSaveCaption",
				"component.mt-wcms.navigation.error.couldNotSaveLabel");
		errorDialog.show();
	} else {
		console.log("An unknown error occur");
		console.log(error);
		var unknownErrorDialog = new wcms.gui.ExceptionDialog(
				"component.mt-wcms.error.unknownErrorCaption",
				"component.mt-wcms.error.unknownErrorLabel", error);
		unknownErrorDialog.show();
	}

}