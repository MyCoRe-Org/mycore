var wcms = wcms || {};
wcms.gui = wcms.gui || {};

/**
 * 
 */
wcms.gui.LoadingDialog = function(/*Preloader*/ preloader) {
	this.constructor();

	this.preloader = preloader;
	this.type = this.Type.cancel;

	this.i18nTitle = "component.wcms.load";

	this.preloaderFrame = null;
};

( function() {

	function createContent() {
		// preloading frame
		this.preloaderFrame = new wcms.gui.PreloaderFrame(this.preloader);
		this.preloaderFrame.create();

		// add to dom
		this.content.appendChild(this.preloaderFrame.container.domNode);
	}

	function beforeShow() {
		this.preloader.preload();
	}

	// inheritance
	wcms.gui.LoadingDialog.prototype = new wcms.gui.AbstractDialog;
	
	wcms.gui.LoadingDialog.prototype.createContent = createContent;
	wcms.gui.LoadingDialog.prototype.beforeShow = beforeShow;

})();