/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

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
