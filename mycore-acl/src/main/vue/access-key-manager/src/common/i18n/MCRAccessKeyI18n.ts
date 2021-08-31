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
  "mcr.accessKey.button.new": "Add",
  "mcr.accessKey.button.no": "No",
  "mcr.accessKey.button.remove": "Remove",
  "mcr.accessKey.button.update": "Update",
  "mcr.accessKey.button.yes": "Yes",
  "mcr.accessKey.error.collision": "The value collides with another key.",
  "mcr.accessKey.error.disabled": "The access key is disabled.",
  "mcr.accessKey.error.expired": "The access key is expired.",
  "mcr.accessKey.error.fatal": "An unknown error has occurred. Please update the application or contact an administrator",
  "mcr.accessKey.error.invalidType": "The type is invalid.",
  "mcr.accessKey.error.invalidValue": "The value is invalid.",
  "mcr.accessKey.error.noPermission": "Please make sure that you have the necessary access rights.",
  "mcr.accessKey.error.request": "A connection to the server could not be established. Please check your internet connection or contact an administrator.",
  "mcr.accessKey.error.server": "The server could not handle the request. Please contact an administrator.",
  "mcr.accessKey.error.transformation": "An error occurred while processing the request.",
  "mcr.accessKey.error.unknownKey": "The access key is unknown.",
  "mcr.accessKey.label.comment": "Comment",
  "mcr.accessKey.label.creation": "Creation",
  "mcr.accessKey.label.creator": "Creator",
  "mcr.accessKey.label.enabled": "Activated",
  "mcr.accessKey.label.expiration": "Expiration",
  "mcr.accessKey.label.id": "ID",
  "mcr.accessKey.label.lastChange": "Last change",
  "mcr.accessKey.label.lastChanger": "Last changer",
  "mcr.accessKey.label.state": "State",
  "mcr.accessKey.label.state.enabled": "Enabled",
  "mcr.accessKey.label.state.disabled": "Disabled",
  "mcr.accessKey.label.type": "Type",
  "mcr.accessKey.label.type.read": "Read",
  "mcr.accessKey.label.type.writedb": "Write",
  "mcr.accessKey.label.value": "Value",
  "mcr.accessKey.popover.enabled": "This can be used to activate or deactivate the access key.",
  "mcr.accessKey.popover.expiration": "The expiration date can be used to optionally define the lifetime of an access key. The access key is then valid until that day and cannot be used afterwards.",
  "mcr.accessKey.popover.type": "A <em>read key</em> authorizes the user to read all information including derivates.<br><br>A <em>write key</em> authorizes the user to edit the object. This also includes the management of derivates or access keys.",
  "mcr.accessKey.popover.value": "The <em>value</em> is used as a token to enable the access key for an user. <br><br><b>Attention</b>: After adding the key the value is no longer visible and cannot be changed afterwards.",
  "mcr.accessKey.success.add": "An access key <b>{0}</b> with value <b>{1}</b> was successfully added.",
  "mcr.accessKey.success.add.url": "Alternatively, the access key can be activated and used without logging in via:",
  "mcr.accessKey.success.add.url.format": "<a href={0}receive/{1}?accesskey={2} disabled>{0}receive/{1}?accesskey={2}</a>",
  "mcr.accessKey.success.delete": "The access key <b>{0}</b> was successfully removed.",
  "mcr.accessKey.success.update": "The access key was successfully updated.",
  "mcr.accessKey.text.remove": "Are they sure they want to delete the access key?",
  "mcr.accessKey.title.add": "Add access key",
  "mcr.accessKey.title.alert": "An error has occurred!",
  "mcr.accessKey.title.edit": "Edit access key",
  "mcr.accessKey.title.main": "Access Key Manager",
  "mcr.accessKey.title.modal": "Confirmation",
  "mcr.accessKey.title.popover": "Information",
  "mcr.accessKey.validation.type": "The type is not valid",
  "mcr.accessKey.validation.value": "The value is mandatory",
};

export default dict
