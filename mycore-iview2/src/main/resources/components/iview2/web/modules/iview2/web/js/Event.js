var iview = iview || {};

iview.Event = function(sender) {
	this._sender = sender;
	this._listeners = [];
};

iview.Event.prototype = {
	attach : function(listener) {
		this._listeners.push(listener);
	},
	detach: function(listener) {
		for (var i = 0; i < this._listeners.length; i++) {
			if (this._listeners[i] == listener) {
				this._listeners[i].splice(i,1);
			}
		}
	},
	notify : function(args) {
		for (var i = 0; i < this._listeners.length; i++) {
			this._listeners[i](this._sender, args);
		}
	}
};