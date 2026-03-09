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

const path = require('path');
const Subject = require('rxjs/Subject').Subject;

const BUILD_ROOT = path.join(process.cwd(), 'target/classes/META-INF/resources/modules/webcli/build');

function requireBuild(relativePath) {
  return require(path.join(BUILD_ROOT, relativePath));
}

class FakeRESTService {
  constructor() {
    this._currentCommandList = new Subject();
    this._currentLog = new Subject();
    this._currentQueue = new Subject();
    this._currentCommand = new Subject();
    this._currentQueueLength = new Subject();
    this._continueIfOneFails = new Subject();

    this.currentCommandList = this._currentCommandList.asObservable();
    this.currentLog = this._currentLog.asObservable();
    this.currentQueue = this._currentQueue.asObservable();
    this.currentCommand = this._currentCommand.asObservable();
    this.currentQueueLength = this._currentQueueLength.asObservable();
    this.continueIfOneFails = this._continueIfOneFails.asObservable();

    this.getCommandsCalls = 0;
    this.executedCommands = [];
    this.startLoggingCalls = 0;
    this.stopLoggingCalls = 0;
    this.clearCommandListCalls = 0;
    this.continueIfOneFailsCalls = [];
  }

  getCommands() {
    this.getCommandsCalls++;
  }

  executeCommand(command) {
    if (command !== undefined && command !== '') {
      this.executedCommands.push(command);
    }
  }

  startLogging() {
    this.startLoggingCalls++;
  }

  stopLogging() {
    this.stopLoggingCalls++;
  }

  clearCommandList() {
    this.clearCommandListCalls++;
    this.emitQueueWithSize([], 0);
    this.emitCurrentCommand('');
  }

  setContinueIfOneFails(value) {
    this.continueIfOneFailsCalls.push(value);
  }

  emitCommandList(commandList) {
    this._currentCommandList.next(commandList);
  }

  emitLog(log) {
    this._currentLog.next(log);
  }

  emitQueue(queue) {
    this.emitQueueWithSize(queue, queue.length);
  }

  emitQueueWithSize(queue, size) {
    this._currentQueue.next(queue);
    this._currentQueueLength.next(size);
  }

  emitCurrentCommand(command) {
    this._currentCommand.next(command);
  }

  emitContinueIfOneFails(value) {
    this._continueIfOneFails.next(value);
  }
}

class FakeWebSocket {
  constructor(url) {
    this.url = url;
    this.readyState = 1;
    this.sent = [];
    this.onmessage = null;
    this.onclose = null;
    this.onerror = null;
    this.onopen = null;
    FakeWebSocket.instances.push(this);
  }

  send(data) {
    this.sent.push(data);
  }

  emitMessage(data) {
    if (this.onmessage) {
      this.onmessage({ data });
    }
  }

  close() {
    this.readyState = 3;
    if (this.onclose) {
      this.onclose({});
    }
  }

  static reset() {
    FakeWebSocket.instances = [];
  }
}

FakeWebSocket.instances = [];

class FakeInputElement {
  constructor() {
    this.focused = false;
    this.selectionStart = -1;
    this.selectionEnd = -1;
  }

  focus() {
    this.focused = true;
  }

  setSelectionRange(start, end) {
    this.selectionStart = start;
    this.selectionEnd = end;
  }
}

module.exports = {
  requireBuild,
  FakeRESTService,
  FakeWebSocket,
  FakeInputElement
};

