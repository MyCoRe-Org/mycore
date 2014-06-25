var wcms = wcms || {};
wcms.gui = wcms.gui || {};

/**
 * Contains a progressbar and a switching text field to show a
 * user the progress of a preloading process.
 */
wcms.gui.PreloaderFrame = function(/*wcms.common.Preloader*/ preloader) {
	this.container = null;
	this.preloader = preloader;
	
	this.progressBar = null;
	this.textField = null;
};

( function() {

	function create() {
		this.container = new dijit.layout.BorderContainer({
			gutters: false
		});

		this.progressBar = new dijit.ProgressBar({
			region: "top"
		});
		this.textField = new dijit.layout.ContentPane({
			region: "bottom",
			content: this.textField,
			style: "padding: 1px; text-align: center;"
		});

		this.container.addChild(this.progressBar);
		this.container.addChild(this.textField);

		// handle preloader events
		this.preloader.eventHandler.attach(dojo.hitch(this, handlePreloaderEvents));
	}

	function handlePreloaderEvents(/*wcms.common.Preloader*/ source, /*Json*/ args) {
		if(args.type == "preloadObject") {
			this.textField.set("content", args.name + "...");
		} else if(args.type == "preloadObjectFinished") {
			this.progressBar.update({
	            maximum: 100,
	            progress: args.progress
	        });
		}
	}

	wcms.gui.PreloaderFrame.prototype.create = create;

})();
