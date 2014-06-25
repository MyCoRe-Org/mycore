var storageAcces;
var getStorageAcces = function() {
	if (typeof storageAcces == "undefined" || storageAcces == null) {
		return storageAcces = new StorageAcces();
	} else {
		return storageAcces;
	}
};

/**
 * Used to store key/value pairs in the Users Browser. Its global (for all iview
 * instances)
 */
StorageAcces = (function() {
	
	function constructor() {
		if (!this.isStoreAvailable()) {
			this.isStoreAvailable = this.addStoragePair = this.getStoragePair = this.removeStoragePair = this.clearStorage = doNothing;
		}

	}
	
	/**
	 * Used as a dummy function for not supporting Browsers.
	 * 
	 * @returns always true
	 */
	function doNothing() {
		return true
	}
	
	/**
	 * Detects the browser supports the html5 storage element
	 * 
	 * @returns true if storage is available
	 */
	constructor.prototype.isStoreAvailable = function isStoreAvailable() {
		if (typeof (Storage) !== "undefined") {
			return true;
		}
		return false;
	};

	constructor.prototype.addStoragePair = function(key, value) {
		localStorage.setItem(key, value);
	};

	constructor.prototype.getStoragePair = function(key) {
		return localStorage.getItem(key);
	};

	constructor.prototype.removeStoragePair = function(key) {
		localStorage.removeItem(key);
	};

	constructor.prototype.clearStorage = function() {
		localStorage.clear();
	};
	
	return constructor;
})();
