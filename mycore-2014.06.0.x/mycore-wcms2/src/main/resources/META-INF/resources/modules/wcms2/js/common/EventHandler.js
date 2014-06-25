/*
 * @package wcms.navigation
 * @description 
 */
var wcms = wcms || {};
wcms.util = wcms.util || {};

wcms.common.EventHandler = function(/*Object*/ src) {
	this.source = src;
	this.listeners = [];
};

( function() {
	function attach(/*function*/ listener) {
		this.listeners.push(listener);
	}

	function detach(/*function*/ listener) {
		for (var i = 0; i < this.listeners.length; i++)
			if (this.listeners[i] == listener)
				this.listeners[i].splice(i,1);
	}

	function notify(args) {
		for (var i = 0; i < this.listeners.length; i++)
			this.listeners[i](this.source, args);
	}

	wcms.common.EventHandler.prototype.attach = attach;
	wcms.common.EventHandler.prototype.detach = detach;
	wcms.common.EventHandler.prototype.notify = notify;
})();
