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

const { FakeWebSocket, requireBuild } = require('../helpers/fakes');

const { RESTService } = requireBuild('service/rest.service.js');

describe('RESTService websocket protocol', function() {
  function createService(url) {
    __webcliTestUtils.resetDom('', url || 'http://localhost/myapp/rsc/WebCLI/gui/index.html');
    FakeWebSocket.reset();
    global.WebSocket = FakeWebSocket;
    global.window.WebSocket = FakeWebSocket;
    return new RESTService(null);
  }

  it('builds the websocket URL from the current browser location', function() {
    createService('http://localhost/myapp/rsc/WebCLI/gui/index.html');

    expect(FakeWebSocket.instances.length).toBe(1);
    expect(FakeWebSocket.instances[0].url).toBe('ws://localhost/myapp/rsc/ws/mycore-webcli/socket');
  });

  it('sends command, refresh, queue and settings messages to the server', function() {
    const service = createService();
    const socket = FakeWebSocket.instances[0];

    service.executeCommand('process resource demo.txt');
    service.startLogging();
    service.stopLogging();
    service.clearCommandList();
    service.setContinueIfOneFails(true);

    expect(socket.sent).toEqual([
      JSON.stringify({ type: 'run', command: 'process resource demo.txt' }),
      JSON.stringify({ type: 'startLog' }),
      JSON.stringify({ type: 'stopLog' }),
      JSON.stringify({ type: 'clearCommandList' }),
      JSON.stringify({ type: 'continueIfOneFails', value: true })
    ]);
  });

  it('routes incoming queue and log messages to subscribers', function() {
    const service = createService();
    const socket = FakeWebSocket.instances[0];
    let seenQueue = null;
    let seenQueueLength = null;
    let seenLog = null;

    service.currentQueue.subscribe((queue) => {
      seenQueue = queue;
    });
    service.currentQueueLength.subscribe((queueLength) => {
      seenQueueLength = queueLength;
    });
    service.currentLog.subscribe((log) => {
      seenLog = log;
    });

    socket.emitMessage(JSON.stringify({
      type: 'commandQueue',
      return: ['cmd1', 'cmd2'],
      size: 2
    }));
    socket.emitMessage(JSON.stringify({
      type: 'log',
      return: {
        logLevel: 'INFO',
        message: 'started',
        exception: null,
        time: 1
      }
    }));

    expect(seenQueue).toEqual(['cmd1', 'cmd2']);
    expect(seenQueueLength).toBe(2);
    expect(seenLog.message).toBe('started');
  });

  it('handles permission denial without JSON parsing', function() {
    const service = createService();
    const socket = FakeWebSocket.instances[0];
    spyOn(global, 'alert');

    socket.emitMessage('noPermission');

    expect(global.alert).toHaveBeenCalled();
    expect(service.retryCounter).toBe(5);
  });
});

