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
 * @package wcms
 */
var wcms = wcms || {};

/**
 * Header of MyCoRe WCMS - contains the languages and the close button.
 */
wcms.Header = function() {
	this.domNode = dojo.create("div", {id: "headerMenu"});
	this.flagsSpan = null;
	this.closeButtonSpan = null;
	this.closeButton = null;

	// event handler
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function preload() {
		// create dom
		this.flagsSpan = dojo.create("span", {id: "flags"});
		this.closeButtonSpan = dojo.create("span", {id: "closeButton"});
		this.closeButton = dojo.create("img", {
			class: "imgButton",
			src: imagePath + "/close.png"
		});

		this.domNode.appendChild(this.flagsSpan);
		this.domNode.appendChild(this.closeButtonSpan);
		this.closeButtonSpan.appendChild(this.closeButton);

		var loadFlagsFunc = dojo.hitch(this, loadFlags);
		// load flags
		loadFlagsFunc();
		dojo.connect(this.closeButton, "onclick", dojo.hitch(this, function(evt) {
			this.eventHandler.notify({
				"type" : "closeButtonClicked"
			});
		}));
	}
	function getPreloadName() {
		return "Header";
	}
	function getPreloadWeight() {
		return 1;
	}

	function loadFlags() {
		var flagPath = imagePath + "/flags/";
		var langArr = I18nManager.getInstance().getSupportedLanguages();
		for(var i in langArr) {
			var langSpan = dojo.create("span", {
				style: "padding-right: 6px"
			});
			var langButton = dojo.create("img", {
				name: langArr[i],
				src: flagPath + "lang-" + langArr[i] + ".png"
			});
			dojo.addClass(langButton, "imgButton");
			langSpan.appendChild(langButton);

			dojo.connect(langButton, "onclick", dojo.hitch(this, function(evt) {
				this.eventHandler.notify({
					"type" : "languageChanged",
					"language" : evt.currentTarget.name
				});
			}));
			this.flagsSpan.appendChild(langSpan);
		}
	}

	wcms.Header.prototype.preload = preload;
	wcms.Header.prototype.getPreloadName = getPreloadName;
	wcms.Header.prototype.getPreloadWeight = getPreloadWeight;

})();
