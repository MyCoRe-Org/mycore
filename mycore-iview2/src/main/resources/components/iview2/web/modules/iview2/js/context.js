(function() {
  "use strict";

  iview.Context = (function() {

    function constructor(container) {
      this.doc = null;
      this.container=container;
      this.viewer=container.find(".viewer");
      this.preload=container.find(".preload");
      this.viewerSibling = container[0].previousSibling;
    }

    constructor.prototype = {
      switchContext : function ctx_switchContext() {
        if (this.doc == null) {
          // store document and replace by viewer
          this.doc = new Array();
          var index = 0;
          while (document.body.firstChild) {
            this.doc[index] = document.body.firstChild;
            document.body.removeChild(document.body.firstChild);
            index++;
          }
          // add Viewer
          document.body.appendChild(this.container[0]);
          // because of IE7 in
          document.documentElement.style.overflow = "hidden";
          document.body.style.overflow = "hidden";
          // class-change causes in IE resize
          this.container.removeClass("min").addClass("max");
        } else {
          // restore document
          while (document.body.firstChild) {
            document.body.removeChild(document.body.firstChild);
          }
          // restore current document content
          var index = 0;
          while (index < this.doc.length) {
            document.body.appendChild(this.doc[index]);
            index++;
          }
          this.viewerSibling.parentNode.insertBefore(this.container[0], this.viewerSibling.nextSibling);
          // because of IE7 in
          document.documentElement.style.overflow = "";
          document.body.style.overflow = "";
          this.doc = null;
          // class-change causes in IE resize
          this.container.removeClass("max").addClass("min");
        }
      },

    };

    return constructor;

  })();
})();
