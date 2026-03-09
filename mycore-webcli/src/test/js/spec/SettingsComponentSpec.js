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
const { WebCliSettingsComponent } = requireBuild('settings/settings.component.js');

describe('WebCLI settings', function() {
  it('persists the continue-if-one-fails setting and sends it to the backend', function() {
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const component = new WebCliSettingsComponent(communicationService, restService);

    component.ngOnInit();
    expect(restService.continueIfOneFailsCalls).toEqual([false]);

    component.onContinueIfOneFailsChange({
      srcElement: {
        checked: true
      }
    });

    expect(localStorage.getItem('continueIfOneFails')).toBe('true');
    expect(restService.continueIfOneFailsCalls).toEqual([false, true]);
  });

  it('applies changed log history settings to the log view', function() {
    __webcliTestUtils.resetDom('<div class="web-cli-log"></div>');
    const restService = new FakeRESTService();
    const communicationService = new CommunicationService();
    const logComponent = new WebCliLogComponent(restService, communicationService);
    logComponent.ngAfterViewInit();
    logComponent.ngOnInit();

    const settingsComponent = new WebCliSettingsComponent(communicationService, restService);
    settingsComponent.ngOnInit();
    settingsComponent.settings.historySize = 1;
    settingsComponent.onHistoryChange();
    communicationService.setSettings(settingsComponent.settings);

    restService.emitLog({ logLevel: 'INFO', message: 'old log', exception: null, time: 1 });
    restService.emitLog({ logLevel: 'INFO', message: 'new log', exception: null, time: 2 });

    const lines = Array.from(document.querySelectorAll('.web-cli-log pre')).map((node) => node.textContent);
    expect(localStorage.getItem('historySize')).toBe('1');
    expect(lines).toEqual(['INFO: new log']);
  });
});

