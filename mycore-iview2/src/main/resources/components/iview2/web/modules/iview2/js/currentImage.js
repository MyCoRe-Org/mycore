(function() {
  "use strict";

  iview.CurrentImage = (function() {

    function constructor() {
      this.name = null;
      this.height = 0;
      this.width = 0;
    }

    constructor.prototype = {
      getName : function ci_getName() {
        return this.name;
      },
      setName : function ci_setName(name) {
        var oldValue = this.name;
        this.name = name;
        return oldValue;
      },
      getWidth : function ci_getWidth() {
        return this.width;
      },
      getHeight : function ci_getHeight() {
        return this.height;
      },
      setWidth : function ci_setWidth(width) {
        this.width = width;
      },
      setHeight : function ci_setHeight(height) {
        this.height = height;
      },
    };

    return constructor;

  })();
})();
