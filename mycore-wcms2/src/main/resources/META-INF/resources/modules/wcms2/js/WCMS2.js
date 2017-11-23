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
 * Start class to show WMCS 2.
 * 
 * @param langArr - array of supported languages 
 */
wcms.WCMS2 = function() {
    // divs
	this.mainContainer = null;
	this.content = null;

	// header
	this.header = null;

	// tabs
	this.tabContainer = null;
    this.navigationTab = null;
    this.accessTab = null;

    this.activeTab = "navigation";
    
    // dialogs
    this.closeDialog = null;
    
    //NavigationContent
    this.navContent = null;
};

( function() {

	function start() {
		// get divs
		this.mainContainer = dojo.byId("mainContainer");
		this.content = dojo.byId("content");

		// create header
		this.header = new wcms.Header();

		// create tabs
		this.tabContainer = new dijit.layout.TabContainer({
			id: "tabContainer",
			tabStrip: true,
			splitter: false,
			gutters: false
		});
		this.navContent = new wcms.navigation.NavigationContent();
		this.navigationTab = new wcms.navigation.NavigationTab(this.navContent);
		this.accessTab = new wcms.access.AccessTab(this.navContent.eventHandler);

		// preloader
		var preloader = new wcms.common.Preloader();
		preloader.preloadList.push(this);
		preloader.preloadList.push(this.navContent);
		preloader.preloadList.push(I18nManager.getInstance());
		preloader.preloadList.push(this.header);
		preloader.preloadList.push(this.navigationTab);
		preloader.preloadList.push(this.accessTab);

		var contentLoader = new wcms.gui.ContentLoader(preloader, this.content, this.tabContainer.domNode);
		contentLoader.create();
		contentLoader.eventHandler.attach(dojo.hitch(this, handlePreloaderEvents));
		contentLoader.preload();
	}

	function preload() {
		// initialize i18n manager
		I18nManager.getInstance().initialize(langArr);

		// create close dialog
		this.closeDialog = new wcms.gui.SimpleDialog("yesNoCancel", "component.wcms.closeHeader", "component.wcms.closeText");
		this.closeDialog.eventHandler.attach(dojo.hitch(this, function(/*wcms.gui.SimpleDialog*/ source, /*Json*/ args) {
			if(args.type == "yesButtonClicked") {
				this.navigationTab.saveChanges();
				this.close();
			} else if(args.type == "noButtonClicked") {
				this.close();
			}
		}));
	}
	this.getPreloadName = function() {
		return "Main Frame";
	}
	this.getPreloadWeight = function() {
		return 1;
	}

	function handlePreloaderEvents(/*wcms.common.Preloader*/ source, /*Json*/ args) {
		if(args.type == "beforePreloadingFinished") {
			// add to dom
			// -> header
			this.content.appendChild(this.header.domNode);
			this.header.eventHandler.attach(dojo.hitch(this, handleHeaderEvents));
			// -> content
			this.tabContainer.addChild(this.navigationTab.domNode);
			this.tabContainer.addChild(this.accessTab.domNode);
		} else if(args.type == "afterPreloadingFinished") {
			this.tabContainer.startup();
			this.tabContainer.layout();
		}
	}

	function handleHeaderEvents(/*wcms.Header*/ source, /*Object*/ args) {
		if(args.type == "closeButtonClicked") {
			var dirty = this.navigationTab.isDirty();
			if(dirty) {
				this.closeDialog.show();
			} else {
				this.close();
			}
		} else if(args.type == "languageChanged") {
			this.setLang(args.language);
		}
	}

	function close() {
		console.log("close wcms");
		// check if everything is saved
		// return to previous website
		window.location = returnUrl;
	}

	function setLang(/*String*/ newLang) {
		I18nManager.getInstance().setLang(newLang);
		this.closeDialog.updateLang();
		this.navigationTab.updateLang();
		this.accessTab.updateLang();
	}

	wcms.WCMS2.prototype.start = start;
	wcms.WCMS2.prototype.setLang = setLang;
	wcms.WCMS2.prototype.close = close;

	wcms.WCMS2.prototype.preload = preload;
	wcms.WCMS2.prototype.getPreloadName = getPreloadName;
	wcms.WCMS2.prototype.getPreloadWeight = getPreloadWeight;
})();
