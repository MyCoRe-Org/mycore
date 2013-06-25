;
(function($) {
  // attach plugin to jQuery
  $.fn.extend({
    mcrI18N : function(i18n) {
      var that = this;
      i18n.executeWhenLoaded(function(i) {
        return that.each(function() {
          // here is my code
          if (this.type == "submit" || this.type == "button") {
            // input fields
            this.value = i.translate(this.value);
          } else {
            $(this.childNodes).each(function() {
              if (this.nodeType == 3) {
                // text node
                var key = $.trim(this.data);
                if (key.length > 0) {
                  this.data = this.data.replace(key, i.translate(key));
                }
              }
            });
          }
        });
      });
    }
  });
})(jQuery);
