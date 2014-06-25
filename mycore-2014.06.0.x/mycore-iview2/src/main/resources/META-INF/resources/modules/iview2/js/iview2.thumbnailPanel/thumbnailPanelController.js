var iview = iview || {};
iview.thumbnailPanel = iview.thumbnailPanel || {};

(function() {
	"use strict";
	
	
	iview.thumbnailPanel.Controller = function(viewer, model, tileUrlProvider) {
		var that = this;
		this._model = model;
		this._container = jQuery("<div />");
		this._container.toggleClass("thumbnailOverview");
		this._container.insertAfter("div.toolbars");
		this._view = new iview.thumbnailPanel.View(this._container,
				tileUrlProvider);
		this._scrollBar = new iview.Scrollbar.Controller(this._container);
		this._scrollBar.disableHorizontal();
		this._positionModifier = -1;
		this._scrollBar.registerEventHandler(function(jq, event) {
			if (event.getEventType() == "positionChanged") {
				that._updateThumbnails(event.getYValue());
			}
		});
		this._pages = new Array();

		this._thumbHeight = 252;
		this._thumbWidth = 252;
		this._preLoadedLines = 2;
		this._postLoadedLines = 5;
		this._thumbnailSpace = 20;
		this._lastSelected = -1;
		this._lastMin = -1;
		this._lastMax = -1;
		this._lastButton = null;
		this.readModel();
		this._calculateScrollbarSize();
		this._updateThumbnails(0);

		this._container.resize(function() {
			this.reinit();
		});

		this._view.registerClickEventHandler(function(e) {
			
			var name = jQuery(e.currentTarget).children("img").attr("alt");
			var number = parseInt(jQuery(e.currentTarget).attr("id"))
					- that._positionModifier;

			that._model.setPosition(number);
			
			that.toggleView();
		});
	};

	iview.thumbnailPanel.Controller.prototype.showView = function(animation) {
		this._view.visible(true, animation);
		this._view.reinit();
	};

	iview.thumbnailPanel.Controller.prototype.hideView = function(animation) {
		this._view.visible(false, animation);
	};

	iview.thumbnailPanel.Controller.prototype.toggleView = function(button) {
		if(typeof button != "undefined"){
			//button.setSubtypeState(false);
			this._lastButton = button;
		} else if(this._lastButton != null){
			this._lastButton.setSubtypeState(false);
		}
		this._view.toggle();
		this.reinit();
	};

	iview.thumbnailPanel.Controller.prototype.setSelected = function(pageNumber) {
		if (this._lastSelected != -1) {
			jQuery("div").removeClass("selected");
		}

		pageNumber = pageNumber + this._positionModifier;
		var containerWidth = this._getContainerSize().width;

		var pagesPerLine = Math.floor(containerWidth
				/ (this._thumbWidth + this._thumbnailSpace));
		var pixelPosition = Math.floor(pageNumber / pagesPerLine)
				* (this._thumbHeight + this._thumbnailSpace);

		this._scrollBar.setPosition({
			x : 1,
			y : pixelPosition
		});
		jQuery("div#" + pageNumber + ".thumbnail").addClass("selected");
		this._lastSelected = pageNumber;
	};

	iview.thumbnailPanel.Controller.prototype.getActive = function() {
		return this._view._visible;
	};

	iview.thumbnailPanel.Controller.prototype._getContainerSize = function() {
		return {
			"width" : this._container.width(),
			"height" : this._container.height()
		};
	};

	iview.thumbnailPanel.Controller.prototype._calculateScrollbarSize = function() {
		var containerSize = this._getContainerSize().width;
		var pagesPerLine = Math.floor(containerSize / (this._thumbWidth + this._thumbnailSpace));
		var lines = Math.ceil(this._pages.length / pagesPerLine);

		this._scrollBar.setSize({
			"width" : 1,
			"height" : lines * (this._thumbHeight + this._thumbnailSpace)
		});

	};

	iview.thumbnailPanel.Controller.prototype._checkInRange = function(
			currentPage, minLine, maxLine, pagesPerLine) {
		var _currentPage = Math.floor(currentPage / pagesPerLine);
		return (_currentPage <= maxLine && _currentPage >= minLine) ? true
				: false;
	};

	iview.thumbnailPanel.Controller.prototype._updateThumbnails = function(
			currentPosition) {
		var containerWidth = this._getContainerSize().width;
		var containerHeight = this._getContainerSize().height;

		var pagesPerLine = Math.floor(containerWidth
				/ (this._thumbWidth + this._thumbnailSpace));
		var linesPerContainer = Math.ceil(containerHeight
				/ (this._thumbHeight + this._thumbnailSpace));

		var lines = Math.ceil(this._pages.length / pagesPerLine);

		var minLine = Math.floor(currentPosition
				/ (this._thumbHeight + this._thumbnailSpace));
		var maxLine = minLine + linesPerContainer;

		var space = (containerWidth % (this._thumbWidth + this._thumbnailSpace)) / 2;

		// minLine with postLoaded
		minLine = Math.max(minLine - this._postLoadedLines, 0);

		// maxLine with preLoaded
		maxLine = Math.min(maxLine + this._preLoadedLines, lines);

		if (minLine != this._lastMin || maxLine != this._lastMax) {
			for ( var currentLine = minLine; currentLine < maxLine; currentLine++) {
				for ( var currentInLine = 0; currentInLine < pagesPerLine; currentInLine++) {
					var currentPage = currentLine * pagesPerLine
							+ currentInLine;
					if (this._view.isPageDetached(currentPage)
							&& typeof this._pages[currentPage] != "undefined") {
						this._view
								.loadPage(
										this._pages[currentPage].name,
										currentPage,
										space
												+ currentInLine
												* (this._thumbWidth + this._thumbnailSpace),
										currentLine
												* (this._thumbHeight + this._thumbnailSpace));
						this._view.attachPage(currentPage);
					}
				}

			}

			if (this._lastMin != -1 && this._lastMax != -1) {

				for ( var currentLine = this._lastMin; currentLine < this._lastMax; currentLine++) {
					for ( var currentInLine = 0; currentInLine < pagesPerLine; currentInLine++) {
						var currentPage = currentLine * pagesPerLine
								+ currentInLine;
						if (!this._checkInRange(currentPage, minLine, maxLine,
								pagesPerLine)) {
							this._view.detachPage(currentPage);
							this._view.deletePage(currentPage);
						}
					}
				}
			}

			this._lastMin = minLine;
			this._lastMax = maxLine;
		}
	};

	iview.thumbnailPanel.Controller.prototype.reinit = function() {

		this._lastMin = -1;
		this._lastMax = -1;
		this._calculateScrollbarSize();
		this._view.reinit();
		this._scrollBar.setPosition({
			"x" : 0,
			"y" : 0
		});
		this._updateThumbnails(1);
	};

	iview.thumbnailPanel.Controller.prototype.readModel = function() {
		var iter = this._model.iterator();
		var modifier = this._positionModifier;
		var temp;
		while (iter.hasNext()) {
			temp = iter.next();
			this._pages[temp.getOrder() + modifier] = {
				"name" : temp.getHref()
			};
		}
	};
})();