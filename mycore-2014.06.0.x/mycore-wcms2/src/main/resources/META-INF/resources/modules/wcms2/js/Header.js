/*
 * @package wcms
 */
var wcms = wcms || {};

/**
 * Header of MyCoRe WCMS - contains the languages and the close button.
 */
wcms.Header = function() {
	this.domNode = dojo.create("div", {id: "headerMenu"});
	this.flagsSpan = null;
	this.closeButtonSpan = null;
	this.closeButton = null;

	// event handler
	this.eventHandler = new wcms.common.EventHandler(this);
};

( function() {

	function preload() {
		// create dom
		this.flagsSpan = dojo.create("span", {id: "flags"});
		this.closeButtonSpan = dojo.create("span", {id: "closeButton"});
		this.closeButton = dojo.create("img", {
			class: "imgButton",
			src: imagePath + "/close.png"
		});

		this.domNode.appendChild(this.flagsSpan);
		this.domNode.appendChild(this.closeButtonSpan);
		this.closeButtonSpan.appendChild(this.closeButton);

		var loadFlagsFunc = dojo.hitch(this, loadFlags);
		// load flags
		loadFlagsFunc();
		dojo.connect(this.closeButton, "onclick", dojo.hitch(this, function(evt) {
			this.eventHandler.notify({
				"type" : "closeButtonClicked"
			});
		}));
	}
	function getPreloadName() {
		return "Header";
	}
	function getPreloadWeight() {
		return 1;
	}

	function loadFlags() {
		var flagPath = imagePath + "/flags/";
		var langArr = I18nManager.getInstance().getSupportedLanguages();
		for(var i in langArr) {
			var langSpan = dojo.create("span", {
				style: "padding-right: 6px"
			});
			var langButton = dojo.create("img", {
				name: langArr[i],
				src: flagPath + "lang-" + langArr[i] + ".png"
			});
			dojo.addClass(langButton, "imgButton");
			langSpan.appendChild(langButton);

			dojo.connect(langButton, "onclick", dojo.hitch(this, function(evt) {
				this.eventHandler.notify({
					"type" : "languageChanged",
					"language" : evt.currentTarget.name
				});
			}));
			this.flagsSpan.appendChild(langSpan);
		}
	}

	wcms.Header.prototype.preload = preload;
	wcms.Header.prototype.getPreloadName = getPreloadName;
	wcms.Header.prototype.getPreloadWeight = getPreloadWeight;

})();