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

const { JSDOM } = require('jsdom');

const DEFAULT_URL = 'http://localhost/myapp/rsc/WebCLI/gui/index.html';
let dom;

global.__modalCalls = [];
global.alert = function() {};
global.$ = function() {
  return {
    modal(action) {
      global.__modalCalls.push(action);
    }
  };
};

function applyGlobals(currentDom) {
  global.window = currentDom.window;
  global.document = currentDom.window.document;
  global.navigator = currentDom.window.navigator;
  global.location = currentDom.window.location;
  global.localStorage = currentDom.window.localStorage;
  global.HTMLElement = currentDom.window.HTMLElement;
  global.Node = currentDom.window.Node;
  global.Event = currentDom.window.Event;
  global.window.$ = global.$;
  global.window.alert = global.alert;
}

function resetDom(html, url) {
  if (!dom) {
    dom = new JSDOM('<!doctype html><html><body></body></html>', {
      url: url || DEFAULT_URL
    });
  } else {
    dom.reconfigure({
      url: url || DEFAULT_URL
    });
  }

  applyGlobals(dom);
  document.body.innerHTML = html || '';
  localStorage.clear();
  global.__modalCalls = [];
  delete global.WebSocket;
  delete global.window.WebSocket;
}

global.__webcliTestUtils = {
  resetDom,
  modalCalls() {
    return global.__modalCalls.slice();
  }
};

resetDom('', DEFAULT_URL);

beforeEach(function() {
  resetDom('', DEFAULT_URL);
});

afterAll(function() {
  if (dom) {
    dom.window.close();
  }
});

