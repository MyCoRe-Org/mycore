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
wcms.common = wcms.common || {};

/**
 * This class uses a recursive approach to create a dojo menu from a json object
 * and bind it to a domNode.
 * 
 * @param store
 *            the json store of the menu
 * @param bindToNode
 *            the dom node where to bind the menu - this could be a div for
 *            example
 */
wcms.common.MenuBuilder = function(/*ItemFileRead*/ store, /*domNode*/ bindToNode, /*Object*/ actionManager) {
	this.store = store;
	this.bindToNode = bindToNode;
	this.actionManager = actionManager;
	this.menu = undefined;
	this.menuItem = undefined;

	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function build() {
		var instance = this;
		this.store.fetch({
			query: {type: new RegExp("ActionManager|Menu")},
			onItem: function(item, i) {
				var initParemeterFunc = dojo.hitch(instance, initParameter);
				initParemeterFunc(item, i);
			},
			onComplete: function(item, i) {
				var createMenuFunc = dojo.hitch(instance, createMenu);
				createMenuFunc();
				instance.eventHandler.notify({"type" : "complete"});
			}
		});
	}

	/**
	 * Initializes the action manager and the menuItem variable.
	 */
	function initParameter(item, i) {
		var typeValue = this.store.getValue(item, "type");
		if(typeValue == "ActionManager") {
			this.actionManager = this.store.getValue(item, "name");
		} else if(typeValue == "Menu") {
			this.menuItem = item;
		}
	}

	/**
	 * Creates the whole menu.
	 */
	function createMenu() {
		console.log("MenuBuilder: create menu");
		if (this.menuItem.type == "Menu") {
			var instance = this;
			this.menu = new dijit.Menu(this.menuItem.params);
			dojo.forEach(this.menuItem.children, function(child, index, array) {
				var addMenuItemFunc = dojo.hitch(instance, addMenuItem);
				addMenuItemFunc(child, instance.menu);
			});

			this.menu.startup();
			this.menu.bindDomNode(this.bindToNode);
		} else {
			console.log("MenuBuilder: Invalid type: " + menuItem.type);
		}
		console.log("MenuBuilder: menu successfully created");
	}

   	/**
	 * Adds a menu item to a parent.
	 * 
	 * @param item
	 *            the menu item to add
	 * @param parent
	 *            the parent
	 * @return the added menu item
	 */
   	function addMenuItem(/* json */ item, /* Menu | PopupMenuItem */ parent) {
   		var instance = this;
		var widget = null;
		var params = this.store.getValue(item, "params");
		
		if (item.type == "MenuItem") {
			widget = new dijit.MenuItem(params);
		} else if (item.type == "CheckedMenuItem") {
			widget = new dijit.CheckedMenuItem(params);
		} else if (item.type == "MenuSeparator") {
			widget = new dijit.MenuSeparator(params);
		} else if (item.type == "PopupMenuItem") {
			widget = new dijit.Menu();
			dojo.forEach(item.children, function(child, index, array) {
				var addMenuItemFunc = dojo.hitch(instance, addMenuItem);
				addMenuItemFunc(child, widget);
			});
		} else {
			console.log("MenuBuilder (Error): invalid type: " + item.type);
			return;
		}

		// connect item to the action method
		try {
			if (params.action != null) {
				widget.set("method", params.action);
				dojo.connect(widget, "onClick", this, callActionMethod);
			}
		} catch (err) {}

		if (item.type == "PopupMenuItem") {
			params.popup = widget;
			var popupMenu = new dijit.PopupMenuItem(params);
			parent.addChild(popupMenu);
		} else {
			parent.addChild(widget);
		}
		return widget;
	}

   	function callActionMethod(event) {
   		var menuDijit = dijit.byId(event.currentTarget.id);
   		var methodName = menuDijit.get("method");
		var parameter = "(dijit.byId(\"" + this.bindToNode.id + "\"))";
		var actionMethod = null;
   		if(typeof this.actionManager == 'string') {
   			actionMethod = this.actionManager + "." + methodName + parameter;
   		} else if(typeof this.actionManager == 'object') {
   			actionMethod = "this.actionManager." + methodName + parameter;
   		}
   		console.log("MenuBuilder: " + actionMethod);
   		eval(actionMethod);
	}

   	wcms.common.MenuBuilder.prototype.build = build;
})();
