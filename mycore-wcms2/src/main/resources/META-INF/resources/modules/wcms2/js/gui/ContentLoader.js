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
 * @package wcms.gui
 */
var wcms = wcms || {};
wcms.gui = wcms.gui || {};

/**
 * A div which contains a progressbar which is added to bindToNode. If
 * the progress is successfully finished, the progressbar is automatically
 * replaced with the endNode.
 */
wcms.gui.ContentLoader = function(/*Preloader*/ preloader, /*Node*/ bindToNode, /*Node*/ endNode) {
	this.preloader = preloader;
	this.bindToNode = bindToNode;
	this.endNode = endNode;
	
	this.preloaderFrame = null;

	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function create() {
		// create preloader frame
		this.preloaderFrame = new wcms.gui.PreloaderFrame(this.preloader);
		this.preloaderFrame.create();

		// add to dom
		var preloaderNode = dojo.create("div");
		dojo.addClass(preloaderNode, "center preload");
		preloaderNode.appendChild(this.preloaderFrame.container.domNode);
		this.bindToNode.appendChild(preloaderNode);

		// events
		this.preloader.eventHandler.attach(dojo.hitch(this, handlePreloaderEvents));
	}

	function preload() {
		this.preloader.preload();
	}

	function handlePreloaderEvents(/*wcms.common.Preloader*/ source, /*JSON*/ args) {
		if(args.type == "started") {
			console.log("start preloading...");
		} else if(args.type == "finished") {
			// remove old loading div
			dojo.empty(this.bindToNode);
			// fire event
			this.eventHandler.notify({"type" : "beforePreloadingFinished"});
			// fade in
			dojo.style(this.bindToNode, "opacity", "0");
			this.bindToNode.appendChild(this.endNode);
			console.log("preloading finished...");
			this.eventHandler.notify({"type" : "afterPreloadingFinished"});
            var fadeArgs = {
                node: this.bindToNode,
                duration: 1500,
            };
            dojo.fadeIn(fadeArgs).play();
		}
	}

	wcms.gui.ContentLoader.prototype.create = create;
	wcms.gui.ContentLoader.prototype.preload = preload;

})();
