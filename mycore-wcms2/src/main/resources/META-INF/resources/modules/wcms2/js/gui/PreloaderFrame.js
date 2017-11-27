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
 * Contains a progressbar and a switching text field to show a
 * user the progress of a preloading process.
 */
wcms.gui.PreloaderFrame = function(/*wcms.common.Preloader*/ preloader) {
	this.container = null;
	this.preloader = preloader;
	
	this.progressBar = null;
	this.textField = null;
};

( function() {

	function create() {
		this.container = new dijit.layout.BorderContainer({
			gutters: false
		});

		this.progressBar = new dijit.ProgressBar({
			region: "top"
		});
		this.textField = new dijit.layout.ContentPane({
			region: "bottom",
			content: this.textField,
			style: "padding: 1px; text-align: center;"
		});

		this.container.addChild(this.progressBar);
		this.container.addChild(this.textField);

		// handle preloader events
		this.preloader.eventHandler.attach(dojo.hitch(this, handlePreloaderEvents));
	}

	function handlePreloaderEvents(/*wcms.common.Preloader*/ source, /*Json*/ args) {
		if(args.type == "preloadObject") {
			this.textField.set("content", args.name + "...");
		} else if(args.type == "preloadObjectFinished") {
			this.progressBar.update({
	            maximum: 100,
	            progress: args.progress
	        });
		}
	}

	wcms.gui.PreloaderFrame.prototype.create = create;

})();
