var iview = iview || {};
iview.thumbnailPanel = iview.thumbnailPanel || {};

(function() {
	"use strict";
	iview.thumbnailPanel.View = function(container, tileUrlProvider) {
		this._thumbnails = new Array();
		this._tileUrlProvider = tileUrlProvider;
		this._container = container;
		this._visible = false;
		this._container.hide();
		this._container.css({
			"z-index" : "60"
		});
		// var viewerHeight = jQuery("div.viewerContainer >
		// div.viewer").css("height");
		this._handler = new Array();
	};
	
	iview.thumbnailPanel.View.prototype.visible = function(__visible, animation) {
		if (__visible == this._visible) {
			return;
		}
		
		if(typeof animation == "undefined" || animation == true){
			jQuery(this._container).slideToggle();
		} else {
			jQuery(this._container).toggle();
		}
		
		this._visible = __visible;
	};

	iview.thumbnailPanel.View.prototype.toggle = function() {
		jQuery(this._container).slideToggle();
		this._visible = !this._visible;
	};

	iview.thumbnailPanel.View.prototype.loadPage = function(pageName,
			pageNumber, xPosition, yPosition) {
		if (typeof this._thumbnails[pageNumber] == "undefined"
				|| this._thumbnails[pageNumber] == null) {
			var thumbnailEntry = jQuery("<div/>");
			var thumbnailImageUrl = this._tileUrlProvider.assembleUrl(0, 0, 0,
					pageName);
			var thumbnailImage = new Image();

			thumbnailImage.alt = pageName;
			thumbnailImage.src = thumbnailImageUrl;

			thumbnailImage.src = thumbnailImageUrl;
			jQuery(thumbnailImage).prependTo(thumbnailEntry);

			var thumbnailText = jQuery("<p>" + pageName + "</p>");
			thumbnailText.appendTo(thumbnailEntry);
			thumbnailEntry.attr("id", pageNumber);
			thumbnailEntry.toggleClass("thumbnail");
			thumbnailEntry.css({
				"position" : "absolute"
			});

			this._thumbnails[pageNumber] = thumbnailEntry;
			this._positionPage(pageNumber, xPosition, yPosition);
			for ( var currentHandlerIndex = 0; currentHandlerIndex < this._handler.length; currentHandlerIndex++) {
				this._thumbnails[pageNumber]
						.bind("click tapone",this._handler[currentHandlerIndex]);
			}
		}
	};

	iview.thumbnailPanel.View.prototype.reinit = function() {
		this._thumbnails = [];
		this._container.children(".thumbnail").remove();
	};

	iview.thumbnailPanel.View.prototype.deletePage = function(pagenumber) {
		this._thumbnails[pagenumber] = null;
	};

	iview.thumbnailPanel.View.prototype.attachPage = function(pageNumber) {
		this._thumbnails[pageNumber].appendTo(this._container);
	};

	iview.thumbnailPanel.View.prototype.detachPage = function(pageNumber) {
		jQuery(this._thumbnails[pageNumber]).remove();
	};

	iview.thumbnailPanel.View.prototype._positionPage = function(pageNumber, x,
			y) {
		this._thumbnails[pageNumber].css({
			"top" : y + "px",
			"left" : x + "px"
		});
	};

	iview.thumbnailPanel.View.prototype.isPageDetached = function(pageNumber) {
		return typeof this._thumbnails[pageNumber] == "undefined"
				|| this._thumbnails[pageNumber] == null;
	};

	iview.thumbnailPanel.View.prototype.registerClickEventHandler = function(
			handler) {
		
		this._container.children(".thumbnail").bind("click tapone", handler);
		this._container.children(".thumbnail img, .thumbnail p").bind("click tapone", handler);
		this._handler.push(handler);
	};
})();
