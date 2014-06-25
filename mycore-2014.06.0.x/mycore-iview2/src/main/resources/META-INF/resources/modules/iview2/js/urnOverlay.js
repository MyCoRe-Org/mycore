(function() {
	"use strict";

	jQuery(document).bind(iview.IViewInstance.INIT_EVENT,
			function(event, iViewInst) {
				// iViewInst.urnView =
			});

	iview.urn = iview.urn || {};
	iview.urn.Controller = iview.urn.Controller || {};
	iview.urn.View = iview.urn.View || {};

	iview.urn.View = (function() {

		function constructor(iViewInst) {
			this._toolBarVisible = true;
			this._iViewInstance = iViewInst;
			this._urnCloseButton = null;
			this._urnText = null;
			this._urnToolBar = null;
			this.EVENT_ClosePressed = "iview.urn.closeClicked";
		}
		;

		/**
		 * @description Creates the urntoolbar in the viewer Triggers
		 *              iview.urn.view.closed on the view.
		 */
		constructor.prototype._createToolbar = function urn_createToolbar() {
			var that = this;

			jQuery('.toolbars').before("<div class=\"urnBox\"></div>");
			this._urnToolBar = jQuery(".urnBox");

			jQuery(this._urnToolBar).ready().append(
					"<div class=\"urnText\"></div>");
			this._urnText = jQuery(".urnText");

			jQuery(this._urnToolBar).ready().append(
					"<div class=\"urnClose\">x</div>");
			this._urnCloseButton = jQuery(".urnClose");

			var that = this;
			jQuery(this._urnCloseButton).click(function() {
				jQuery(that).trigger(that.EVENT_ClosePressed);
			});
			this.setUrnToolbarVisible(true);
		};

		/**
		 * @description Changes the Text of the URN Toolbar
		 * @param {Object}
		 *            newText
		 */
		constructor.prototype.updateToolbarText = function urn_updateToolbarText(
				newText) {
			jQuery(this._urnText).text(newText);
		};

		/**
		 * @description makes the urn button (in)visible
		 * @param {Boolean}
		 *            visible
		 * @param {Integer}
		 *            animate
		 */
		constructor.prototype.setUrnButtonVisible = function urn_setUrnButtonVisible(
				visible) {
			this._iViewInstance.toolbar.ctrl.perform("setActive", visible,
					'urnHandles', 'urn');
		};

		constructor.prototype.removeUrnButton = function urn_removeButton() {
			this._iViewInstance.toolbar.ctrl.perform("remove", "",
					'urnHandles', 'urn');
		};

		constructor.prototype.setUrnToolbarVisible = function urn_setUrnToolbarVisible(
				visible) {
			if (visible) {
				if (this._iViewInstance.addDimensionSubstract(true,
						'UrnToolbar', 27)) {
					this._toolBarVisible = true;
					this._iViewInstance.reinitializeGraphic();

					var container = jQuery(this._iViewInstance.viewerContainer.context);

					jQuery(container).find(".toolbars").css({
						top : "27px",
					});

					jQuery(this._urnToolBar).animate({
						top : "0px",
						opacity : "show"
					}, 100);

					jQuery(container).find("div.viewer").css({
						"top" : "73px"
					});


				}
				this._toolBarVisible = true;

				jQuery(window).trigger("resize");
			} else {
				var container = jQuery(this._iViewInstance.viewerContainer.context);

				if (this._iViewInstance.viewerContainer.isMax()) {
					jQuery(container).find("div.viewer").css({
						"top" : "44px"
					});
				} else {
					jQuery(container).find("div.viewer").css({
						"top" : "0px"
					});
				}

				if (this._iViewInstance.removeDimensionSubstract(true,
						'UrnToolbar')) {
					jQuery(container).find(".toolbars").css({
						top : "-=27px",
					});

					jQuery(this._urnToolBar).css({
						top : "-=50px",
						display : "none"
					});
					if (this._iViewInstance.viewerContainer.isMax()) {
						jQuery(container).find("div.viewer").css({
							"top" : "44px"
						});
					}

				}
				this._toolBarVisible = false;
				jQuery(window).trigger("resize");
			}
		};

		return constructor;
	})();

	iview.urn.Controller = (function() {
		function constructor(iViewInst) {
			var that = this;
			this._view = new iview.urn.View(iViewInst);
			this._iViewInstance = iViewInst;
			this.hasUrn = false;
			this._URN_TOOLBAR_STORAGE_KEY = "urnBarVisible";

			if (typeof that._iViewInstance.PhysicalModel.getCurrent() !== "undefined"
					&& that._iViewInstance.PhysicalModel.getCurrent() != null) {
				var currentUrn = iViewInst.PhysicalModel.getCurrent()
						.getContentId();
				that.hasUrn = !(typeof currentUrn == "undefined"
						|| currentUrn == null || currentUrn == "");
				that.updateUrn();
			}

			jQuery(iViewInst.currentImage).bind(
					iview.CurrentImage.CHANGE_EVENT, function() {
						that.updateUrn();
					});

			jQuery(iViewInst.viewerContainer).bind(
					"minimize.viewerContainer",
					function(e) {
						var store = getStorageAcces();
						store.addStoragePair(that._URN_TOOLBAR_STORAGE_KEY,
								that._view._toolBarVisible);
						that._view.setUrnToolbarVisible(false);
					});

			jQuery(iViewInst.viewerContainer).one("maximize.viewerContainer",
					function(e) {
						that._view._createToolbar();

					});

			var initalize = function (e) {
				var store = getStorageAcces();
				var toolBarVisible = store
						.getStoragePair(that._URN_TOOLBAR_STORAGE_KEY);
				if (that.hasUrn && toolBarVisible != "false") {
					that._view.setUrnToolbarVisible(true);
					that.updateUrn();
				} else {
					if (that.hasUrn) {
						that._view.setUrnButtonVisible(true, false);
						that.updateUrn();
					} else {
						that._view.setUrnButtonVisible(false, false);
						that._view.removeUrnButton();
					}
					that._view.setUrnToolbarVisible(false);
				}
			};
			
			if(!iViewInst.viewerContainer.isMax()) {
				jQuery(iViewInst.viewerContainer).bind("maximize.viewerContainer",initalize);
			} else {
				initalize();
			}
			

			jQuery(this._view).bind(this._view.EVENT_ClosePressed, function() {
				if (that.hasUrn) {
					that._view.setUrnToolbarVisible(false);
					that._view.setUrnButtonVisible(true, true);
				}
			});
		}

		constructor.prototype.updateUrn = function urn_updateUrn() {
			if (this.hasUrn) {
				var currentUrn = this._iViewInstance.PhysicalModel.getCurrent()
						.getContentId();
				this._view.updateToolbarText(currentUrn);
			}
		}

		constructor.prototype.urnButtonClicked = function urn_urnButtonClicked(
				button) {
			
			this._view.setUrnToolbarVisible(!this._view._toolBarVisible);
		}

		return constructor;
	})();

})();
