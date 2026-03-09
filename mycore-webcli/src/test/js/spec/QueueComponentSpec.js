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

const { WebCliQueueComponent } = requireBuild('queue/queue.component.js');

describe('WebCLI queue view', function() {
  function createQueueComponent(restService) {
    __webcliTestUtils.resetDom('<pre class="web-cli-pre"></pre>');
    return new WebCliQueueComponent(restService);
  }

  function queueElement() {
    return document.getElementsByClassName('web-cli-pre')[0];
  }

  it('shows multiple queued commands', function() {
    const restService = new FakeRESTService();
    createQueueComponent(restService);

    restService.emitQueueWithSize(['import object 1', 'import object 2'], 2);

    expect(queueElement().innerHTML).toContain('import object 1');
    expect(queueElement().innerHTML).toContain('import object 2');
  });

  it('clears the queue view when the queue is cleared', function() {
    const restService = new FakeRESTService();
    createQueueComponent(restService);

    restService.emitQueueWithSize(['import object 1', 'import object 2'], 2);
    expect(queueElement().textContent).toContain('import object 1');

    restService.clearCommandList();

    expect(queueElement().innerHTML).toBe('');
  });
});

