(function() {
  "use strict";

  iview.CurrentImage = (function() {

    function constructor() {
      this.name = null;
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
    };

    return constructor;

  })();
})();
