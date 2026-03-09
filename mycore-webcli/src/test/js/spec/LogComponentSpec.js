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

const { WebCliLogComponent } = requireBuild('log/log.component.js');
const { CommunicationService } = requireBuild('service/communication.service.js');
const { Settings } = requireBuild('settings/settings.js');

describe('WebCLI log view', function() {
  function createLogComponent(restService, communicationService) {
    __webcliTestUtils.resetDom('<div class="web-cli-log"></div>');
    const component = new WebCliLogComponent(restService, communicationService);
    component.ngAfterViewInit();
    component.ngOnInit();
    return component;
  }

  function getLogLines() {
    return Array.from(document.querySelectorAll('.web-cli-log pre')).map((node) => node.textContent);
  }

  it('displays mock log output correctly', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    createLogComponent(restService, communicationService);

    restService.emitLog({
      logLevel: 'INFO',
      message: 'Import started',
      exception: null,
      time: 1
    });
    restService.emitLog({
      logLevel: 'ERROR',
      message: 'Import failed',
      exception: 'java.lang.RuntimeException: boom',
      time: 2
    });

    expect(getLogLines()).toEqual([
      'INFO: Import started',
      'ERROR: Import failed',
      'java.lang.RuntimeException: boom'
    ]);
  });

  it('removes the oldest log messages when the history size is exceeded', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    createLogComponent(restService, communicationService);

    communicationService.setSettings(new Settings(2, 10, false, false));

    restService.emitLog({ logLevel: 'INFO', message: 'first', exception: null, time: 1 });
    restService.emitLog({ logLevel: 'INFO', message: 'second', exception: null, time: 2 });
    restService.emitLog({ logLevel: 'INFO', message: 'third', exception: null, time: 3 });

    expect(getLogLines()).toEqual([
      'INFO: second',
      'INFO: third'
    ]);
  });

  it('clears the log view so old messages are no longer displayed', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = createLogComponent(restService, communicationService);

    restService.emitLog({ logLevel: 'INFO', message: 'old entry', exception: null, time: 1 });
    restService.emitLog({ logLevel: 'INFO', message: 'older entry', exception: null, time: 2 });
    expect(getLogLines().length).toBe(2);

    component.clearLog();
    expect(getLogLines()).toEqual([]);

    restService.emitLog({ logLevel: 'INFO', message: 'new entry', exception: null, time: 3 });
    expect(getLogLines()).toEqual(['INFO: new entry']);
  });
});

