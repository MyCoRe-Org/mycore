import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App.vue';
import { Util } from './common/util';
import type { UpdateProcessableMessage } from './model/messages';
import { ModalMock } from './test/helpers/bootstrapMock';
import { WebSocketMock } from './test/helpers/webSocketMock';

const CREATED = Date.UTC(2026, 10, 25, 8, 30, 15);

function processable(
  id: number,
  status: UpdateProcessableMessage['status'],
  overrides: Partial<UpdateProcessableMessage> & Record<string, unknown> = {}
): UpdateProcessableMessage & Record<string, unknown> {
  return {
    type: 'UPDATE_PROCESSABLE',
    id,
    name: `job ${id}`,
    collectionId: 1,
    status,
    user: 'editor',
    createTime: CREATED,
    startTime: CREATED + 1000,
    progressText: undefined,
    properties: {},
    ...overrides,
  };
}

const COLLECTION = {
  type: 'ADD_COLLECTION',
  id: 1,
  name: 'Derivate import',
  properties: { threads: 4 },
};

describe('processing app', () => {
  let wrapper: VueWrapper | null = null;
  let warnings: string[];

  async function mountApp(): Promise<WebSocketMock> {
    warnings = [];
    wrapper = mount(App, {
      attachTo: document.body,
      global: { config: { warnHandler: message => void warnings.push(message) } },
    });
    const socket = WebSocketMock.latest();
    socket.open();
    await flushPromises();
    return socket;
  }

  async function receive(socket: WebSocketMock, ...messages: object[]): Promise<void> {
    messages.forEach(message => socket.receive(message));
    await flushPromises();
  }

  function card() {
    return wrapper!.get('.card');
  }

  function tabs(): Array<{ label: string; count: string; active: boolean }> {
    return card()
      .findAll('.nav-link')
      .map(tab => ({
        label: tab.text().replace(/\s*\d+$/, ''),
        count: tab.get('.badge').text(),
        active: tab.classes('active'),
      }));
  }

  function rowNames(): string[] {
    return card()
      .findAll('tbody .col-name')
      .map(cell => cell.text());
  }

  async function selectTab(index: number): Promise<void> {
    await card().findAll('.nav-link')[index]!.trigger('click');
  }

  function bodyModal(title: string): HTMLElement {
    const modal = [...document.body.querySelectorAll<HTMLElement>('.modal')].find(
      element => element.querySelector('.modal-title')?.textContent === title && element.isConnected
    );
    if (modal === undefined) {
      throw new Error(`no modal titled ${title}`);
    }
    return modal;
  }

  beforeEach(() => {
    WebSocketMock.install();
    ModalMock.instances = [];
    localStorage.clear();
    vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  afterEach(() => {
    expect(warnings).toEqual([]);
    wrapper?.unmount();
    wrapper = null;
    vi.useRealTimers();
  });

  describe('connection', () => {
    it('connects to the processing websocket of the application and registers', async () => {
      const socket = await mountApp();

      expect(socket.url).toBe('ws://localhost:3000/mir/ws/mycore-webtools/processing');
      expect(socket.sentMessages()).toEqual([{ type: 'connect' }]);
    });

    it('connects to the root context without double slash', async () => {
      (globalThis as unknown as { mycore: object }).mycore = { webApplicationBaseURL: 'http://localhost:3000/' };
      try {
        const socket = await mountApp();
        expect(socket.url).toBe('ws://localhost:3000/ws/mycore-webtools/processing');
      } finally {
        (globalThis as unknown as { mycore: object }).mycore = { webApplicationBaseURL: 'http://localhost:3000/mir/' };
      }
    });

    it('uses the development server in development mode', async () => {
      vi.stubEnv('DEV', true);
      try {
        const socket = await mountApp();
        expect(socket.url).toBe('ws://localhost:8291/mir/ws/mycore-webtools/processing');
      } finally {
        vi.stubEnv('DEV', false);
      }
    });

    it('waits for the socket to be open before registering', async () => {
      vi.useFakeTimers();
      wrapper = mount(App, { attachTo: document.body });
      warnings = [];
      const socket = WebSocketMock.latest();

      socket.onopen?.(new Event('open'));
      expect(socket.send).not.toHaveBeenCalled();

      socket.readyState = WebSocketMock.OPEN;
      await vi.advanceTimersByTimeAsync(500);
      expect(socket.sentMessages()).toEqual([{ type: 'connect' }]);
    });

    it('reconnects five seconds after the connection was closed', async () => {
      vi.useFakeTimers();
      const socket = await mountApp();

      socket.close();
      await vi.advanceTimersByTimeAsync(4999);
      expect(WebSocketMock.instances).toHaveLength(1);

      await vi.advanceTimersByTimeAsync(1);
      expect(WebSocketMock.instances).toHaveLength(2);
      WebSocketMock.latest().open();
      expect(WebSocketMock.latest().sentMessages()).toEqual([{ type: 'connect' }]);
    });

    it('shows websocket errors', async () => {
      const socket = await mountApp();

      socket.fail();
      await flushPromises();

      expect(wrapper!.get('.alert-danger').text()).toBe('A websocket error occurred.');
    });

    it('shows server errors until the registry is sent again', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, { type: 'ERROR', error: '500' });
      expect(wrapper!.get('.alert-danger').text()).toBe('A server error occurred: 500');

      await receive(socket, { type: 'REGISTRY' });
      expect(wrapper!.find('.alert-danger').exists()).toBe(false);
      expect(wrapper!.findAll('.card')).toHaveLength(0);
    });
  });

  describe('collections', () => {
    it('shows each collection with empty tabs', async () => {
      const socket = await mountApp();

      await receive(socket, { type: 'REGISTRY' }, COLLECTION, { ...COLLECTION, id: 2, name: 'Mail' });

      expect(wrapper!.findAll('.card h2').map(title => title.text())).toEqual(['Derivate import', 'Mail']);
      expect(tabs()).toEqual([
        { label: 'Processing', count: '0', active: true },
        { label: 'Waiting', count: '0', active: false },
        { label: 'Finished', count: '0', active: false },
      ]);
      expect(card().get('tbody').text()).toBe('no processing process');
      expect(card().findAll('thead th').map(th => th.text())).toEqual(['Name', 'User', 'Create Date', 'Progress']);
    });

    it('moves a processable through the tabs as its status changes', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, processable(1, 'CREATED'), processable(2, 'CREATED'));
      expect(tabs().map(tab => tab.count)).toEqual(['0', '2', '0']);

      await receive(socket, processable(1, 'PROCESSING', { progress: 40, progressText: '40 of 100' }));
      expect(tabs().map(tab => tab.count)).toEqual(['1', '1', '0']);
      expect(rowNames()).toEqual(['job 1']);

      await receive(socket, processable(1, 'SUCCESSFUL', { progress: 100 }));
      expect(tabs().map(tab => tab.count)).toEqual(['0', '1', '1']);
      expect(card().get('tbody').text()).toBe('no processing process');

      await selectTab(1);
      expect(rowNames()).toEqual(['job 2']);
      expect(tabs().map(tab => tab.active)).toEqual([false, true, false]);

      await selectTab(2);
      expect(rowNames()).toEqual(['job 1']);
    });

    it('renders name, user, create date and progress of a processable', async () => {
      const socket = await mountApp();
      await receive(
        socket,
        { type: 'REGISTRY' },
        COLLECTION,
        processable(1, 'PROCESSING', { progress: 40, progressText: '40 of 100' })
      );

      const row = card().get('tbody tr');
      expect(row.get('.col-user').text()).toBe('editor');
      expect(row.get('.col-create').text()).toBe(Util.formatDate(CREATED).trim());
      expect(row.get('.progress-bar').attributes('style')).toContain('width: 40%');
      expect(row.get('.progress-text').text()).toBe('40 of 100');
    });

    it('shows the status instead of a progress bar when there is no progress', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION, processable(1, 'PROCESSING'));

      const progress = card().get('tbody .col-progress');
      expect(progress.find('.progress').exists()).toBe(false);
      expect(progress.text()).toBe('PROCESSING');
    });

    it('updates the progress of a running processable in place', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION, processable(1, 'PROCESSING', { progress: 10 }));

      await receive(socket, processable(1, 'PROCESSING', { progress: 75, progressText: 'almost' }));

      expect(rowNames()).toEqual(['job 1']);
      expect(card().get('.progress-bar').attributes('style')).toContain('width: 75%');
      expect(card().get('.progress-text').text()).toBe('almost');
    });

    it('keeps only the configured number of finished processables', async () => {
      localStorage.setItem('mcr.processables.maxNumberFinished', '2');
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, processable(1, 'SUCCESSFUL'), processable(2, 'FAILED'), processable(3, 'CANCELED'));
      await selectTab(2);

      expect(rowNames()).toEqual(['job 2', 'job 3']);
    });

    it('keeps all finished processables when the limit is disabled', async () => {
      localStorage.setItem('mcr.processables.maxNumberFinished', '-1');
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, ...Array.from({ length: 60 }, (_, index) => processable(index + 1, 'SUCCESSFUL')));

      expect(tabs()[2]!.count).toBe('60');
    });

    it('keeps the 50 latest finished processables by default', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, ...Array.from({ length: 55 }, (_, index) => processable(index + 1, 'SUCCESSFUL')));
      await selectTab(2);

      expect(tabs()[2]!.count).toBe('50');
      expect(rowNames()[0]).toBe('job 6');
    });

    it('ignores processables of unknown collections', async () => {
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, processable(1, 'CREATED', { collectionId: 99 }));

      expect(tabs().map(tab => tab.count)).toEqual(['0', '0', '0']);
      expect(warn).toHaveBeenCalledWith('Unable to find collection with id 99');
    });
  });

  describe('modals', () => {
    it('shows the properties of a collection', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await card().get('.card-header a').trigger('click');

      const modal = bodyModal('Properties');
      expect(ModalMock.forElement(modal).show).toHaveBeenCalled();
      const rows = [...modal.querySelectorAll('.property-row')];
      expect(rows.map(row => row.querySelector('td')?.textContent)).toEqual(['threads']);
      expect(rows[0]!.querySelector('.json-string-primitive')?.textContent).toBe('4');
    });

    it('updates an existing collection property', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, { type: 'UPDATE_COLLECTION_PROPERTY', id: 1, propertyName: 'threads', propertyValue: 8 });

      expect(bodyModal('Properties').querySelector('.json-string-primitive')?.textContent).toBe('8');
    });

    it('ignores property updates of unknown collections', async () => {
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, COLLECTION);

      await receive(socket, { type: 'UPDATE_COLLECTION_PROPERTY', id: 7, propertyName: 'threads', propertyValue: 8 });

      expect(warn).toHaveBeenCalledWith('Unable to find collection with id 7');
    });

    it('explains when a collection has no properties', async () => {
      const socket = await mountApp();
      await receive(socket, { type: 'REGISTRY' }, { ...COLLECTION, properties: {} });

      expect(bodyModal('Properties').querySelector('tbody')?.textContent?.trim()).toBe('No properties set');
    });

    it('shows the details of a processable', async () => {
      const socket = await mountApp();
      await receive(
        socket,
        { type: 'REGISTRY' },
        COLLECTION,
        processable(1, 'PROCESSING', { properties: { object: 'mir_mods_00000001', files: ['a.pdf', 'b.pdf'] } })
      );

      await card().get('tbody .col-name a').trigger('click');

      const modals = [...document.body.querySelectorAll<HTMLElement>('.modal')].filter(element =>
        element.querySelector('.modal-dialog.modal-lg')
      );
      expect(modals).toHaveLength(1);
      const modal = modals[0]!;
      expect(ModalMock.forElement(modal).show).toHaveBeenCalled();
      const basic = [...modal.querySelectorAll('.card')[0]!.querySelectorAll('tbody tr')].map(row =>
        [...row.querySelectorAll('td')].map(cell => cell.textContent?.trim())
      );
      expect(basic).toEqual([
        ['Name', 'job 1'],
        ['Status', 'PROCESSING'],
        ['Create Time', Util.formatDate(CREATED).trim()],
        ['Start Time', Util.formatDate(CREATED + 1000).trim()],
        ['End Time', ''],
        ['Took', 'ms'],
      ]);
      const external = [...modal.querySelectorAll('.property-row')];
      expect(external.map(row => row.querySelector('td')?.textContent)).toEqual(['object', 'files']);
      expect([...external[1]!.querySelectorAll('.json-string-array-entry')].map(entry => entry.textContent)).toEqual([
        'a.pdf',
        'b.pdf',
      ]);
      expect(modal.textContent).not.toContain('Error');
    });

    it('shows the error of a failed processable', async () => {
      const socket = await mountApp();
      await receive(
        socket,
        { type: 'REGISTRY' },
        COLLECTION,
        processable(1, 'FAILED', { error: 'java.io.IOException: disk full' })
      );
      await selectTab(2);

      const modal = [...document.body.querySelectorAll<HTMLElement>('.modal')].find(element =>
        element.querySelector('.modal-lg')
      )!;
      expect(modal.querySelector('pre')?.textContent).toBe('java.io.IOException: disk full');
      expect(modal.textContent).toContain('No external properties set');
    });

    it('stores the maximum number of finished processables', async () => {
      await mountApp();

      await wrapper!.get('h1 a').trigger('click');

      const modal = bodyModal('Settings');
      expect(ModalMock.forElement(modal).show).toHaveBeenCalled();
      const input = modal.querySelector('input') as HTMLInputElement;
      expect(input.value).toBe('50');

      input.value = '20';
      input.dispatchEvent(new Event('input'));
      await flushPromises();

      expect(localStorage.getItem('mcr.processables.maxNumberFinished')).toBe('20');
    });

    it('reads the stored maximum number of finished processables', async () => {
      localStorage.setItem('mcr.processables.maxNumberFinished', '7');

      await mountApp();

      expect((bodyModal('Settings').querySelector('input') as HTMLInputElement).value).toBe('7');
    });
  });
});
