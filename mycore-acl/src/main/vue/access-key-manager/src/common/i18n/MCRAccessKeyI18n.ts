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

const dict: Record<string, string> = {
  "mcr.accessKey.button.add": "Access Key",
  "mcr.accessKey.button.yes": "Yes",
  "mcr.accessKey.button.no": "No",
  "mcr.accessKey.button.remove": "Remove",
  "mcr.accessKey.button.update": "Update",
  "mcr.accessKey.error.collision": "The value collides with another key.",
  "mcr.accessKey.error.invalidType": "The type is invalid.",
  "mcr.accessKey.error.invalidValue": "The value is invalid.",
  "mcr.accessKey.error.unknownKey": "The access key is unknown.",
  "mcr.accessKey.error.transformation": "An error occurred while processing the request.",
  "mcr.accessKey.error.noPermission": "Please make sure that you have the necessary access rights.",
  "mcr.accessKey.error.server": "The server could not handle the request. Please contact an administrator.",
  "mcr.accessKey.error.request": "A connection to the server could not be established. Please check your internet connection or contact an administrator.",
  "mcr.accessKey.error.fatal": "An unknown error has occurred. Please update the application or contact an administrator",
  "mcr.accessKey.success.add": "An access key <b>{0}</b> with value <b>{1}</b> was successfully added.<br>Users can activate the access key in the action menu.",
  "mcr.accessKey.success.update": "The access key <b>{0}</b> was successfully updated.",
  "mcr.accessKey.success.delete": "The access key <b>{0}</b> was successfully removed.",
  "mcr.accessKey.label.comment": "Comment",
  "mcr.accessKey.label.creation": "Creation",
  "mcr.accessKey.label.creator": "Creator",
  "mcr.accessKey.label.id": "ID",
  "mcr.accessKey.label.lastChange": "Last change",
  "mcr.accessKey.label.lastChanger": "Last changer",
  "mcr.accessKey.title.modal": "Confirmation",
  "mcr.accessKey.label.type": "Type",
  "mcr.accessKey.label.type.read": "Read",
  "mcr.accessKey.label.type.writedb": "Write",
  "mcr.accessKey.label.value": "Value",
  "mcr.accessKey.popover.value": "The <em>value</em> is used as a token to enable the access key for an user. <br><br><b>Attention</b>: After adding the key the value is no longer visible and cannot be changed afterwards.",
  "mcr.accessKey.popover.type": "A <em>read key</em> authorizes the user to read all information including derivates.<br><br>A <em>write key</em> authorizes the user to edit the object. This also includes the management of derivates or access keys.",
  "mcr.accessKey.validation.type": "The type is not valid",
  "mcr.accessKey.validation.value": "The value is mandatory",
  "mcr.accessKey.title.popover": "Information",
  "mcr.accessKey.title.add": "Add access key",
  "mcr.accessKey.title.edit": "Edit access key",
  "mcr.accessKey.title.alert": "An error has occurred!",
  "mcr.accessKey.text.remove": "Are they sure they want to delete the access key?",
  "mcr.accessKey.title.main": "Access Key Manager",
};

export default dict
