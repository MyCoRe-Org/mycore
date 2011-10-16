var iview = iview || {}; // holds API
var Iview = Iview || {}; // holds instances
//TODO is that object really needed?
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
