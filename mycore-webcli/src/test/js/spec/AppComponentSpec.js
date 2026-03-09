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

const { FakeRESTService, requireBuild } = require('../helpers/fakes');

const { AppComponent } = requireBuild('app.component.js');
const { CommunicationService } = requireBuild('service/communication.service.js');

describe('WebCLI app shell', function() {
  function createApp(restService, communicationService) {
    __webcliTestUtils.resetDom('<button class="logTab"></button>');
    return new AppComponent(restService, communicationService);
  }

  it('can stop and enable log refresh', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = createApp(restService, communicationService);

    component.setRefresh(false);
    expect(component.refreshRunning).toBe(false);
    expect(restService.stopLoggingCalls).toBe(1);

    component.setRefresh(true);
    expect(component.refreshRunning).toBe(true);
    expect(restService.startLoggingCalls).toBe(1);
  });

  it('can clear the command queue and switches back to the log tab when the queue becomes empty', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = createApp(restService, communicationService);
    let clickCount = 0;

    document.getElementsByClassName('logTab')[0].click = function() {
      clickCount++;
    };

    restService.emitQueueWithSize(['cmd1', 'cmd2'], 2);
    expect(component.currentQueueLength).toBe(2);

    component.clearCommandList();

    expect(restService.clearCommandListCalls).toBe(1);
    expect(component.currentQueueLength).toBe(0);
    expect(clickCount).toBe(1);
  });

  it('can clear the log view', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = createApp(restService, communicationService);

    component.webCliLogComponent = {
      clearLog: jasmine.createSpy('clearLog')
    };

    component.clearLog();

    expect(component.webCliLogComponent.clearLog).toHaveBeenCalled();
  });
});

