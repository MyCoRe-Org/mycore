(function() {
  "use strict";

  iview.IViewInstance = (function() {
    "use strict";
    function constructor(derivateId, container, options) {
      this.derivateId = derivateId;
      this.viewerContainer = container;
      this.ausschnittParent = container; // TODO: get rid of this
      this.chapterParent = container; // TODO: get rid of this
      this.gen = new iview.General(this);
      this.ToolbarImporter = new ToolbarImporter(this, i18n);
    }
    return constructor;
  })();
})();
