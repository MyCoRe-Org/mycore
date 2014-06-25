var iview = iview || {};
iview.Scrollbar = iview.Scrollbar || {};

(function() {
	"use strict";

	/**
	 * @class
	 * @constructor
	 * @description View to display with a given template
	 */
	iview.Scrollbar.View = function(container) {
		this._container = container;
		this._container.css({
			"position" : "relative",
			"margin" : "0px",
			"padding" : "0px",
		});

		this._spacer = jQuery("<div class=\"spacer\" />");
		this._spacer.appendTo(this._container);
		
		this.enableHorizontal();
		this.enableVertical();

	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see iview.Scrollbar.Controller.getPosition
	 */
	iview.Scrollbar.View.prototype.getPosition = function sbv_getPosition() {
		var xPos = this._container.scrollLeft();
		var yPos = this._container.scrollTop();
		return {
			"x" : xPos,
			"y" : yPos
		};
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @param position
	 * @see {iview.Scrollbar.Controller.setPosition}
	 */
	iview.Scrollbar.View.prototype.setPosition = function sbv_setPosition(
			position) {
		if(typeof position != "undefined"){
			this._container.scrollTop(position.y);
			this._container.scrollLeft(position.x);
		}
		
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @returns
	 * @see {iview.Scrollbar.Controller.setPosition}
	 */
	iview.Scrollbar.View.prototype.getSize = function sbv_getSize() {
		var width = this._spacer.width();
		var height = this._spacer.height();

		return {
			"width" : width,
			"height" : height
		};
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see {iview.Scrollbar.Controller.setSize}
	 */
	iview.Scrollbar.View.prototype.setSize = function sbv_setSize(size) {
		var bak = this.getPosition();
		this._spacer.css({
			"width" : size.width + "px",
			"height" : size.height + "px",
			"padding" : "0px",
			"margin" : "0px"
		});
		this.setPosition(bak);
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see {iview.Scrollbar.Controller.enableHorizontal}
	 */
	iview.Scrollbar.View.prototype.enableHorizontal = function sbv_enableHorizontal() {
		this._container.addClass("scrollx");
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see {iview.Scrollbar.Controller.disableHorizontal}
	 */
	iview.Scrollbar.View.prototype.disableHorizontal = function sbv_disableHorizontal() {
		this._container.removeClass("scrollx");
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see {iview.Scrollbar.Controller.enableVertical}
	 */
	iview.Scrollbar.View.prototype.enableVertical = function sbv_enableVertical() {
		this._container.addClass("scrolly");
	};

	/**
	 * @memberOf {iview.Scrollbar.View}
	 * @see {iview.Scrollbar.Controller.disableVertical}
	 */
	iview.Scrollbar.View.prototype.disableVertical = function sbv_disableVertical() {
		this._container.removeClass("scrolly");
	};

})();