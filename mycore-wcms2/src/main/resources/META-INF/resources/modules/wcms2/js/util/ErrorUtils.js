/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

var wcms = wcms || {};
wcms.util = wcms.util || {};
wcms.util.ErrorUtils = wcms.util.ErrorUtils || {};

/*-------------------------------------------------------------------------
 * Util class to handle errors
 *------------------------------------------------------------------------*/

wcms.util.ErrorUtils.show = function(/* JSON */ error) {
	var caption = "";
	var label = "";
	if(error == "unauthorized") {
		caption = "component.wcms.error.unauthorizedCaption";
		label = "component.wcms.error.unauthorizedLabel";
	} else if (error == "unexpected") {
		caption = "component.wcms.error.unexpectedCaption";
		label = "component.wcms.error.unexpectedLabel";
	} else if (error == "unknownType") {
		caption = "component.wcms.error.unknownTypeCaption";
		label = "component.wcms.error.unknownTypeLabel";
	} else if (error == "notMyCoReWebPage") {
		caption = "component.wcms.navigation.error.notMyCoReWebPageCaption";
		label = "component.wcms.navigation.error.notMyCoReWebPageLabel";
	} else if (error == "invalidDirectory") {
		caption = "component.wcms.navigation.error.invalidDirectoryCaption";
		label = "component.wcms.navigation.error.invalidDirectoryLabel";
	} else if (error == "notExist") {
		caption = "component.wcms.navigation.error.notExistCaption";
		label = "component.wcms.navigation.error.notExistLabel";
	} else if (error == "invalidFile") {
		caption = "component.wcms.navigation.error.invalidFileCaption";
		label = "component.wcms.navigation.error.invalidFileLabel";
	} else if (error == "couldNotSave") {
		caption = "component.wcms.navigation.error.couldNotSaveCaption";
		label = "component.wcms.navigation.error.couldNotSaveLabel";
	} else {
		console.log("An unknown error occur");
		console.log(error);
		var unknownErrorDialog = new wcms.gui.ExceptionDialog(
				"component.wcms.error.unknownErrorCaption",
				"component.wcms.error.unknownErrorLabel", error);
		unknownErrorDialog.show();
		return;
	}
	var errorDialog = new wcms.gui.ErrorDialog(caption, label);
	errorDialog.show();

}
