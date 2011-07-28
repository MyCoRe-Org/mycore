/**
 * @public
 * @function
 * @name openPdfCreator
 * @memberOf iview.General
 * @description opens PDF creator dialog
 * @param {button}
 *          button to which represents the PDF creator in the toolbar
 */
iview.General.prototype.openPdfCreator = function(button) {
  var that = this;
  if (typeof this.getPdfCtrl === "undefined") {
    this.getPdfCtrl = function() {
      if (!this.pdfCtrl) {
        this.pdfCtrl = new iview.Pdf.Controller(this);

        this.pdfCtrl.getViewer = function() {
          return this.parent;
        };
      }
      return this.pdfCtrl;
    };
    this.getPdfCtrl().setView(new iview.Pdf.View("pdfCreatorView",
        this.iview.viewerContainer,
        this.iview.properties.webappBaseUri + "modules/iview2",
        this.iview.properties.pdfCreatorURI,
        this.iview.properties.webappBaseUri + "servlets/MCRMETSServlet/" + this.iview.viewID + "?XSL.Style=" + this.iview.properties.pdfCreatorStyle,
        function() {
          that.getPdfCtrl().initView(i18n);
        }));
  } else {
    this.getPdfCtrl().show();
  }
};

/**
 * @namespace Package for Pdf, contains View and Controller
 * @memberOf iview
 * @name Pdf
 */
iview.Pdf = {};

/**
 * @class
 * @name View
 * @memberOf iview.Pdf
 * @proto Object
 * @description view of pdf creator within a toolbar to display a link to the current viewer content
 * @param {String}
 *          id identifies the current toolbar view
 * @param {Object}
 *          events to trigger defined actions, while managing contained elements
 * @param {Object}
 *          parent defines the parent node of the permalink view
 * @param {Object}
 *          content defines the current permalink
 * @param {Object}
 *          permalink represents the main node of the permalink view
 */
iview.Pdf.View = function(id, parent, basePath, pdfCreatorURI, pdfSourceXML, callback) {
  this.id = id;

  this.content = jQuery('<textarea class="content" readonly="readonly" wrap="off" onfocus="this.select()"/>').first();
  this.pdfCreator = jQuery('<div>').addClass(id).addClass('pdfCreator').appendTo(parent).load(basePath + '/createpdf.html .modal-content',
      function() {
        var form = jQuery("form", this)[0];
        form.action = pdfCreatorURI;
        form.mets.value = pdfSourceXML;
        callback();
      });
};

iview.Pdf.View.prototype = {

  /**
   * @function
   * @memberOf View#
   * @description displays the permalink container with slide-down effect
   */
  show : function(closeCallback) {
    var that = this;
    jQuery("div.modal-content", this.pdfCreator).modal({
      overlayId : 'createpdf-overlay',
      containerId : 'createpdf-container',
      closeHTML : null,
      minHeight : 80,
      opacity : 65,
      position : [ '0', ],
      overlayClose : true,
      onOpen : this.open,
      onClose : function(d) {
        that.close.call(this, d);
        that._initPdfCreator = true;
        if (closeCallback) {
          closeCallback();
        }
      }
    });
    this.pdfCreator = jQuery("div.modal-content").parent();
  },

  /**
   * @function
   * @memberOf View#
   * @description hides the permalink container with slide-up effect
   */
  close : function(d) {
    var self = this; // this = SimpleModal object
    d.container.animate({
      top : "-" + (d.container.height() + 20)
    }, 500, function() {
      self.close(); // or $.modal.close();
    });
  },

  /**
   * @function
   * @memberOf View#
   * @description opens pdf creator dialog
   */
  open : function(d) {
    var self = this;
    self.container = d.container[0];
    d.overlay.fadeIn('slow', function() {
      jQuery(".modal-content", self.container).show();
      var title = jQuery(".modal-title", self.container);
      title.show();
      var buttons = jQuery("input.ok, input.simplemodal-close", this);
      d.container.slideDown('slow', function() {
        setTimeout(function() {
          var h = $(".modal-data", self.container).height() + title.height() + 20; // padding
          d.container.animate({
            height : h
          }, 200, function() {
            $("div.close", self.container).show();
            $(".modal-data", self.container).show();
          });
        }, 300);
      });
      buttons.button();
    });
  },

  /**
   * @function
   * @memberOf View#
   * @returns jQuery html node of pdf creator view
   */
  getPdfCreator : function() {
    if (this._initPdfCreator) {
      this.pdfCreator = jQuery("div.modal-content").parent();
      delete this._initPdfCreator;
    }
    return this.pdfCreator;
  },

  /**
   * @function
   * @memberOf View#
   * @param text
   *          validation info
   */
  setValidationText : function(text) {
    var valEl = jQuery("div.validation", this.getPdfCreator())[0];
    valEl.innerHTML = text;
  },

  /**
   * @function
   * @memberOf View#
   * @param href
   *          URL to image preview
   */
  displayPreview : function(href) {
    var img = jQuery("img.previewImage", this.getPdfCreator())[0];
    img.src = href;
  },

  /**
   * @function
   * @memberOf View#
   * @returns jQuery html node of manual input radio box
   */
  getManualInput : function() {
    var manual = jQuery("input.manual", this.getPdfCreator());
    return manual;
  },

  setCurrentPageText : function(text, currentPage) {
    var cP = jQuery("input.currentPage", this.getPdfCreator())[0];
    cP.value = currentPage;
    cP.nextElementSibling.innerHTML = text + currentPage;
  },

  setMaxPageNumber : function(lastPage) {
    var aP = jQuery("input.allPages", this.getPdfCreator())[0];
    aP.value = "1-" + String(lastPage);
  }
};
/**
 * @class
 * @name Controller
 * @proto Object
 * @memberOf iview.Pdf
 * @description main Controller to control the PDF creator
 * @param {Object}
 *          parent holds the reference to the viewer
 */
iview.Pdf.Controller = function(parent) {
  this.parent = parent;
  this.view = null;
  this.order = parent.iview.PhysicalModel._order;
  this.maxPages = 0;
  var that = this;
  jQuery.ajax({
    url : this.parent.iview.properties.pdfCreatorURI + "?getRestrictions",
    success : function(data) {
      data = jQuery.parseJSON(data);
      that.maxPages = data.maxPages;
    }
  });
};

iview.Pdf.Controller.prototype = {

  /**
   * @public
   * @function
   * @memberOf iview.Pdf.Controller#
   * @description initializes pdf creator view
   * @param view
   *          {Object} View which should be initialized
   */
  initView : function(i18n) {
    this.i18n = i18n;
    var pdfEl = this.view.getPdfCreator();
    var that = this;
    jQuery("input[type=radio]", pdfEl).focus(function() {
      that.validateRangeInput(this);
    });
    jQuery("input.manualInput", pdfEl).change(function() {
      that.changeManualInput(this);
    });
    jQuery("form", pdfEl).submit(function(event) {
      return iview.Pdf.Controller.validateForm(event.target, that.maxPages, that.i18n);
    });
    jQuery(".mcri18n").mcrI18N(i18n);
    this.show();
  },

  changeManualInput : function(el) {
    var m = this.view.getManualInput()[0];
    m.value = el.value;
    this.validateRangeInput(m);
  },

  /**
   * @public
   * @function
   * @memberOf iview.Pdf.Controller#
   * @description initializes pdf creator view
   * @param el
   *          {Object} input element
   */
  validateRangeInput : function(el) {
    this.view.setValidationText("");
    try {
      iview.Pdf.Controller.validateRange(el.value, this.maxPages, this.i18n);
      this.displayPreview(el.value);
    } catch (e) {
      this.view.setValidationText(e.message);
      console.log({
        msg : e.message,
        stack : e.stack
      });
      el.focus();
    }
  },

  displayPreview : function(ranges) {
    var first = ranges.split(",")[0].split("-")[0];
    if (first.length == 0) {
      return;
    }
    var imgPath = this.order[first]._href;
    this.view.displayPreview(this.parent.iview.properties.webappBaseUri + "servlets/MCRTileServlet/" + this.parent.iview.viewID + "/" + imgPath
        + "/0/0/0.jpg");
  },

  /**
   * @public
   * @function
   * @memberOf iview.Pdf.Controller#
   * @description adds an existing view to the PdfController, attach to its events (press) and define the actions for each button
   * @param {Object}
   *          view View which should be add
   */
  setView : function(view) {
    this.view = view;
  },

  /**
   * @public
   * @function
   * @memberOf iview.Pdf.Controller#
   * @description displays or hides the permalink view, the current state, given in this.active, defines the following action
   */
  show : function() {
    this.parent.disableInputHandler();
    var that = this;
    var cP = this.parent.iview.PhysicalModel._curPos;
    this.i18n.executeWhenLoaded(function(i) {
      that.view.setCurrentPageText(i.translate("component.iview2.createPdf.range.currentPage") + ":", cP);
    });
    this.view.setMaxPageNumber(this.parent.iview.PhysicalModel._pageCount);
    this.displayPreview(String(cP));
    this.view.show(function() {
      // on close:
      that.parent.enableInputHandler();
    });
  }

};

/**
 * @function
 * @memberOf iview.Pdf.Controller#
 * @param range
 *          string of form "a-b,c,d-e"
 * @param maxPages
 *          integer of max allowed pages in range
 * @description throw a validation error if {range} is not valid
 */
iview.Pdf.Controller.validateRange = function(range, maxPages, i18n) {
  var pages = iview.Pdf.Controller.amountPages(range, i18n);
  if (pages == 0) {
    throw new Error(i18n.translate("component.iview2.createPdf.errors.noPages"));
  }
  if (pages > maxPages) {
    throw new Error(i18n.translate("component.iview2.createPdf.errors.tooManyPages") + ": " + pages);
  }
};

iview.Pdf.Controller.amountPages = function(range, i18n) {
  var pages = 0;
  var ranges = range.split(",");
  if (ranges[0].length == 0) {
    return 0;
  }
  for ( var i = 0; i < ranges.length; i++) {
    var r = ranges[i];
    if (r.indexOf("-") > 0) {
      var ft = r.split("-");
      var from = parseInt(ft[0]);
      var to = parseInt(ft[1]);
      if (from > to) {
        throw new Error(i18n.translate("component.iview2.createPdf.errors.rangeInvalid") + ": " + r);
      }
      pages += to - from + 1;
    } else {
      pages++;
    }
  }
  return pages;
};

iview.Pdf.Controller.validateForm = function(form, maxPages, i18n) {
  for ( var i = 0; i < form.pages.length; i++) {
    if (form.pages[i].checked) {
      try {
        iview.Pdf.Controller.validateRange(form.pages[i].value, maxPages, i18n);
      } catch (e) {
        alert(e.message);
        return false;
      }
    }
  }
  return true;
};
