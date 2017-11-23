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
wcms.gui = wcms.gui || {};

/**
 * 
 */
wcms.gui.LoadingDialog = function(/*Preloader*/ preloader) {
	this.constructor();

	this.preloader = preloader;
	this.type = this.Type.cancel;

	this.i18nTitle = "component.wcms.load";

	this.preloaderFrame = null;
};

( function() {

	function createContent() {
		// preloading frame
		this.preloaderFrame = new wcms.gui.PreloaderFrame(this.preloader);
		this.preloaderFrame.create();

		// add to dom
		this.content.appendChild(this.preloaderFrame.container.domNode);
	}

	function beforeShow() {
		this.preloader.preload();
	}

	// inheritance
	wcms.gui.LoadingDialog.prototype = new wcms.gui.AbstractDialog;
	
	wcms.gui.LoadingDialog.prototype.createContent = createContent;
	wcms.gui.LoadingDialog.prototype.beforeShow = beforeShow;

})();
