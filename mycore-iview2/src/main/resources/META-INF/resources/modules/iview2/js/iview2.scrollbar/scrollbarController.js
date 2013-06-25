var iview = iview || {};
iview.Scrollbar = iview.Scrollbar || {};

(function() {
	"use strict";

	/**
	 * Creates a Scrollbar.
	 * 
	 * @class
	 * @constructor
	 * @this {iview.Scrollbar.Controller}
	 * @memberOf iview.Scrollbar.Controller
	 * @param {jQuery
	 *            Selector} container The container wich should contain the
	 *            scrollbars
	 */
	iview.Scrollbar.Controller = function(container) {
		var that = this;

		/** @private */
		this._view = new iview.Scrollbar.View(container);
		this._container = container;

		/*
		 * Updates the model Position if the view changes
		 */
		jQuery(container).scroll(function() {
			that._fireEvent("positionChanged");
		});

	};

	iview.Scrollbar.Controller.SCROLLBAR_WIDTH = 13;
	iview.Scrollbar.Controller.SCROLLBAR_HEIGHT = 13;
	
	/**
	 * Set the position of the scrollbar. Updates the view.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 * @param {x,
	 *            y} newPosition The new Position that should be set.
	 * @param {bool} notify Should the event fired ?
	 */
	iview.Scrollbar.Controller.prototype.setPosition = function(newPosition) {
		var oldPosition = this.getPosition();
		if(typeof newPosition != "undefined" && newPosition.x == oldPosition.x && newPosition.y == oldPosition.y){
			return;
		}
		
		this._view.setPosition(newPosition);
	};

	/**
	 * Set the size of the scrollbar container. Updates the View.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 * @param {width,
	 *            height} newSize The new Size that should be set.
	 */
	iview.Scrollbar.Controller.prototype.setSize = function(newSize) {
		this._view.setSize(newSize);
		this._fireEvent("sizeChanged");
	};

	/**
	 * Makes the container horizontal scrollable.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 */
	iview.Scrollbar.Controller.prototype.enableHorizontal = function() {
		this._view.enableHorizontal();
	};

	/**
	 * Makes the container not horizontal scrollable.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 */
	iview.Scrollbar.Controller.prototype.disableHorizontal = function() {
		this._view.disableHorizontal();
	};

	/**
	 * Makes the container vertical scrollable.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 */
	iview.Scrollbar.Controller.prototype.enableVertical = function() {
		this._view.enableVertical();
	};

	/**
	 * Makes the container not vertical scrollable.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 */
	iview.Scrollbar.Controller.prototype.disableVertical = function() {
		this._view.disableVertical();
	};

	/**
	 * Adds a jQuery bind for scrollbarEvent.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 * @param {function
	 *            ({iview.Scrollbar.ScrollbarEvent})} e A function wich can
	 *            handle iview.Scrollbar.ScrollbarEvent.
	 */
	iview.Scrollbar.Controller.prototype.registerEventHandler = function(e) {
		jQuery(this).bind("scrollbarEvent", e);
	};

	/**
	 * remove a jQuery bind for ScrollbarEvent.
	 * 
	 * @this {iview.Scrollbar.Controller}
	 * @param {function
	 *            ({iview.Scrollbar.ScrollbarEvent})} e A function wich can
	 *            handle iview.Scrollbar.ScrollbarEvent.
	 */
	iview.Scrollbar.Controller.prototype.unRegisterEventHandler = function(e) {
		jQuery(this).unbind("scrollbarEvent", e);
	};
	
	/**
	 * @this {iview.Scrollbar.Controller}
	 * @returns {x, y} The position of the Scrollbar.
	 */
	iview.Scrollbar.Controller.prototype.getPosition = function() {
		return this._view.getPosition();
	};

	/**
	 * @this {iview.Scrollbar.Controller}
	 * @returns {width, height} The size of the Scrollbar.
	 */
	iview.Scrollbar.Controller.prototype.getSize = function() {
		return this._view.getSize();
	};

	/**
	 * Fires a jQuery Event with parameters of size or position
	 * 
	 * @private
	 * @this {iview.Scrollbar.Controller}
	 * @param {String}
	 *            type "positionChanged" || "sizeChanged"
	 */
	iview.Scrollbar.Controller.prototype._fireEvent = function(type) {
		var xValue = null, yValue = null, pos, size;
		if (type === "positionChanged") {
		  pos = this._view.getPosition();
			xValue = pos.x;
			yValue = pos.y;
		} else if (type === "sizeChanged") {
		  size = this._view.getSize();
			xValue = size.width;
			yValue = size.height;
		} else {
			return;
		}

		var event = new iview.Scrollbar.ScrollbarEvent(type, xValue, yValue);
		jQuery(this).trigger("scrollbarEvent", event);
	};
	
	iview.Scrollbar.Controller.prototype.getContainer = function() {
		return this._container;
	};

})();
