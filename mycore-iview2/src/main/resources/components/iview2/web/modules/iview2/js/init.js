var iview = iview || {}; // holds API
var Iview = Iview || {}; // holds instances

iview.IViewObject = (function(){
  "use strict";
  function constructor(iviewInst){
    this._iview=iviewInst;
    hideProperty(this,"_iview", false);
  }
  constructor.prototype = {
      getViewer: function iv_getViewer(){
        return this._iview;
      }
  };
  return constructor;
})();

/**
 * @class
 * @constructor
 * @memberOf	iview.General
 * @name		General
 * @description All Viewer data and functions which don't fit in other packages
 */
iview.General = function(iviewInst) {
	//TODO later it should be possible to remove all this.iview with just this
	this.iview = iviewInst;
	//structure for all Viewer DOM-Objects
	this.iview.context = new iview.Context(iviewInst.viewerContainer, iviewInst);
	this.iview.currentImage = new iview.CurrentImage(iviewInst);
	var that = this;
};

var genProto = iview.General.prototype;
