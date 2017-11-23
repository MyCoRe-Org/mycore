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

/*
 * @package wcms.navigation
 * @description editor for a tree item
 */
var wcms = wcms || {};
wcms.navigation = wcms.navigation || {};

wcms.navigation.InsertItemEditor = function() {
	this.constructor();

	// uri
	this.uriComboBox = null;
};

( function() {

	// i18n text
	// ie-bug: use var instead of const
	var mainHeaderText = "component.wcms.navigation.insertItemEditor.mainHeader";
	var uriText = "component.wcms.navigation.insertItemEditor.uri";

	function create() {
		// create dijit components
		this.refRadio = new dijit.form.RadioButton();
		var refStore = new dojo.data.ItemFileReadStore({
			data: {"identifier": "id", "items": [
					{"id":0,"text":"component:search"},
				 	{"id":1,"text":"component:editor"},
			        {"id":2,"text":"component:workflow"}
				  ]}
	    });
		this.uriComboBox = new dijit.form.ComboBox({
	        store: refStore,
	        searchAttr: 'text',
	        intermediateChanges: true,
	    });
		dojo.addClass(this.uriComboBox.domNode, "largeComponent");

		// caption
		this.setHeader(mainHeaderText);
		// add uri combobox
		this.addElement(uriText, this.uriComboBox.domNode);
		// update i18n texts
		this.updateLang();

		// -uri
		dojo.connect(this.uriComboBox, "onChange", this, function(/*String*/ value) {
			if(this.currentItem == null)
				return;
			if(!equal(this.currentItem.uri, value)) {
				this.currentItem.uri = value;
				this.eventHandler.notify({"type" : "itemUpdated", "item": this.currentItem});
			}
		});

	}

	function updateEditor(/*JSON*/ item) {
		this.currentItem = item;
		// general
		this.setValue(this.uriComboBox, item.uri);
	}

	function reset() {
		if(this.currentItem != null)
			this.currentItem = null;
		this.uriComboBox.set("value", "");
	}

	function setDisabled(/*boolean*/ value) {
		this.disabled = value;
		// general
		this.uriComboBox.set("disabled", this.disabled);
	}

	// inheritance
	wcms.navigation.InsertItemEditor.prototype = new wcms.gui.ContentEditor;

	// own methods
	wcms.navigation.InsertItemEditor.prototype.create = create;
	wcms.navigation.InsertItemEditor.prototype.updateEditor = updateEditor;
	wcms.navigation.InsertItemEditor.prototype.reset = reset;
	wcms.navigation.InsertItemEditor.prototype.setDisabled = setDisabled;
})();
