var wcms = wcms || {};
wcms.util = wcms.util || {};


wcms.util.Hashtable = function() {
	this.length = 0;
	this.items = new Array();
};


( function() {

	function remove(key) {
		var tmpPrevious;
		if (typeof(this.items[key]) != 'undefined') {
			this.length--;
			var tmpPrevious = this.items[key];
			delete this.items[key];
		}
		return tmpPrevious;
	}

	function get(key) {
		return this.items[key];
	}

	function put(key, value) {
		var tmpPrevious;
		if (typeof(value) != 'undefined') {
			if (typeof(this.items[key]) == 'undefined') {
				this.length++;
			} else {
				tmpPrevious = this.items[key];
			}
			this.items[key] = value;
		}
		return tmpPrevious;
	}

	function containsKey(key) {
		return typeof(this.items[key]) != 'undefined';
	}

	function clear() {
		for (var i in this.items)
			delete this.items[i];
		this.length = 0;
	}

	wcms.util.Hashtable.prototype.remove = remove;
	wcms.util.Hashtable.prototype.get = get;
	wcms.util.Hashtable.prototype.put = put;
	wcms.util.Hashtable.prototype.containsKey = containsKey;
   	wcms.util.Hashtable.prototype.clear = clear;
})();
