/*
 * @package classeditor
 * @description 
 */
var classeditor = classeditor || {};

classeditor.EventHandler = function(/*Object*/ src) {
	this.source = src;
	this.listeners = [];
};

( function() {

	function attach(/*function*/ listener) {
		this.listeners.push(listener);
	}

	function detach(/*function*/ listener) {
		for (var i = 0; i < this.listeners.length; i++) {
			if (this.listeners[i] == listener) {
				this.listeners[i].splice(i,1);
			}
		}
	}

	function notify(args, scope) {
		for (var i = 0; i < this.listeners.length; i++) {
			if(scope) {
				dojo.hitch(scope, this.listeners[i](this.source, args));				
			} else {
				this.listeners[i](this.source, args)
			}
		}
	}

	classeditor.EventHandler.prototype.attach = attach;
	classeditor.EventHandler.prototype.detach = detach;
	classeditor.EventHandler.prototype.notify = notify;
})();
