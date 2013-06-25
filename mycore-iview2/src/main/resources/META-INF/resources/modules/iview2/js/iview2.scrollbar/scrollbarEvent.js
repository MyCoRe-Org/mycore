var iview = iview || {};
iview.Scrollbar = iview.Scrollbar || {};

(function() {
	"use strict";

	/**
	 * Creates a new ScrollbarEvent
	 * 
	 * @class
	 * @constructor
	 * @param {String}
	 *            EventType positionChanged || sizeChanged
	 */
	iview.Scrollbar.ScrollbarEvent = function(eventType, newValueX, newValueY) {
		if(typeof eventType == "undefined" ){
			throw new iview.IviewInstanceError("Scrollbar eventType ==  \"undefined\"");
		}
		
		/** @private */
		this._type = eventType;

		/** @private */
		this._newVal = {
			"xVal" : newValueX,
			"yVal" : newValueY
		};
	};
	
	/**
	 * @memberOf {iview.Scrollbar.ScrollbarEvent}
	 * @this {iview.Scrollbar.ScrollbarEvent}
	 * @returns positionChanged || sizeChanged
	 */
	iview.Scrollbar.ScrollbarEvent.prototype.getEventType = function(){
		return this._type;
	};
	
	/**
	 * @memberOf {iview.Scrollbar.ScrollbarEvent}
	 * @this {iview.Scrollbar.ScrollbarEvent}
	 * @returns the events x Value (can be the x value or the width)
	 */
	iview.Scrollbar.ScrollbarEvent.prototype.getXValue = function(){
		return this._newVal.xVal;
	};
	
	/**
	 * @memberOf {iview.Scrollbar.ScrollbarEvent}
	 * @this {iview.Scrollbar.ScrollbarEvent}
	 * @returns the events y Value (can be the y value or the height)
	 */
	iview.Scrollbar.ScrollbarEvent.prototype.getYValue = function(){
		return this._newVal.yVal;
	};

})();