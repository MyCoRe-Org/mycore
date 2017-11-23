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
