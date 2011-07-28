(function() {
  "use strict";

  /**
   * @class
   * @constructor
   * @memberOf  iview
   * @name    Properties
   * @description holds instance configuration data
   */
  iview.Properties = (function() {
    function constructor(properties) {
      this.set(properties);
    }
    /**
     * @function
     * @memberOf iview.Properties
     * @description merges all properties into the current instance
     * @param {Object} properties to be merged
     */
    constructor.prototype.set = function prop_set(properties) {
      if (typeof properties === "undefined") {
        properties = {};
      }
      for ( var prop in properties) {
        this[prop] = properties[prop];
      }
    };
    return constructor;
  })();
})();
