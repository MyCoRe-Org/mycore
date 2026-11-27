import { flushPromises, type DOMWrapper, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import type { DuplicateEntry, NoDuplicateEntry } from '@/api/Model';
import router from '@/router';
import { ModalMock } from '@/test/helpers/bootstrapMock';
import { FetchMock, jsonResponse, translation } from '@/test/helpers/fetchMock';
import { mountAppAt, type MountedApp } from '@/test/helpers/mountApp';

const BASE = 'http://localhost:3000/mir/';

function duplicate(id1: string, id2: string, criteria: Array<[string, string]>, titles = true): DuplicateEntry {
  return {
    objectId1: id1,
    title1: titles ? `Title of ${id1}` : null,
    objectId2: id2,
    title2: titles ? `Title of ${id2}` : null,
    criteria: criteria.map(([type, value]) => ({ type, value })),
  };
}

const DUPLICATES = [
  duplicate('mir_mods_00000003', 'mir_mods_00000004', [['title', 'b']]),
  duplicate('mir_mods_00000001', 'mir_mods_00000009', [
    ['doi', '10.1000/1'],
    ['title', 'a'],
  ]),
  duplicate('mir_mods_00000005', 'mir_mods_00000002', [['isbn', '978-3']]),
];

function noDuplicate(id: number, id1: string, id2: string, creator: string, created: string): NoDuplicateEntry {
  return { id, objectId1: id1, title1: `Title of ${id1}`, objectId2: id2, title2: null, creator, created };
}

const NO_DUPLICATES = [
  noDuplicate(1, 'mir_mods_00000001', 'mir_mods_00000002', 'editor', '2026-03-01T10:00:00Z'),
  noDuplicate(2, 'mir_mods_00000003', 'mir_mods_00000004', 'admin', '2026-05-01T10:00:00Z'),
  noDuplicate(3, 'mir_mods_00000005', 'mir_mods_00000006', 'zoe', 'not a date'),
];

function columnTexts(wrapper: VueWrapper, column: number): string[] {
  return wrapper.findAll('tbody tr').map(row => row.findAll('td')[column]!.text());
}

function objectIds(wrapper: VueWrapper, column: number): string[] {
  return wrapper.findAll('tbody tr').map(row => row.findAll('td')[column]!.get('.text-muted').text());
}

function header(wrapper: VueWrapper, index: number): DOMWrapper<Element> {
  return wrapper.findAll('thead th')[index]!;
}

describe('dedup app', () => {
  let fetchMock: FetchMock;
  let app: MountedApp | null = null;

  async function mountAt(path: string): Promise<VueWrapper> {
    app = await mountAppAt(path);
    return app.wrapper;
  }

  function apiCalls(): Array<{ method: string; url: string }> {
    return fetchMock.apiRequests().map(call => ({ method: call.init?.method ?? 'GET', url: call.url }));
  }

  function modalOf(wrapper: VueWrapper): ModalMock {
    return ModalMock.forElement(wrapper.get('.modal').element);
  }

  beforeEach(async () => {
    ModalMock.instances = [];
    fetchMock = new FetchMock();
    await router.replace('/');
  });

  afterEach(() => {
    if (app !== null) {
      expect(app.warnings).toEqual([]);
      expect(app.errors).toEqual([]);
      app.wrapper.unmount();
      app = null;
    }
  });

  describe('navigation', () => {
    it('shows translated tabs and marks the current one', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse([]));

      const wrapper = await mountAt('/');

      const tabs = wrapper.findAll('.nav-tabs .nav-link');
      expect(tabs.map(tab => tab.text())).toEqual([
        translation('dedup.gui.tab.duplicates'),
        translation('dedup.gui.tab.noDuplicates'),
      ]);
      expect(tabs.map(tab => tab.attributes('href'))).toEqual(['/mir/dedup/', '/mir/dedup/no-duplicates']);
      expect(tabs.map(tab => tab.classes('active'))).toEqual([true, false]);
    });

    it('switches to the list of pairs marked as no duplicates', async () => {
      fetchMock
        .on('api/dedup/duplicates', () => jsonResponse([]))
        .on('api/dedup/no-duplicates', () => jsonResponse(NO_DUPLICATES));
      const wrapper = await mountAt('/');

      await wrapper.findAll('.nav-tabs .nav-link')[1]!.trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.name).toBe('noDuplicates');
      expect(wrapper.findAll('.nav-tabs .nav-link').map(tab => tab.classes('active'))).toEqual([false, true]);
      expect(wrapper.findAll('tbody tr')).toHaveLength(3);
    });
  });

  describe('duplicates', () => {
    it('loads the duplicates with the jwt and renders one row per pair sorted by criterion', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse(DUPLICATES));

      const wrapper = await mountAt('/');

      const [request] = fetchMock.apiRequests();
      expect(request?.url).toBe(`${BASE}api/dedup/duplicates`);
      expect(request?.init?.cache).toBe('no-store');
      expect(new Headers(request?.init?.headers).get('authorization')).toBe('Bearer test-token');
      expect(fetchMock.requests('rsc/jwt')[0]?.init?.cache).toBe('no-store');
      expect(objectIds(wrapper, 1)).toEqual(['(mir_mods_00000001)', '(mir_mods_00000005)', '(mir_mods_00000003)']);
      expect(header(wrapper, 0).find('.fa-sort-up').exists()).toBe(true);
    });

    it('renders the translated description and table headers', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse(DUPLICATES));

      const wrapper = await mountAt('/');

      expect(wrapper.get('main > p.text-muted').text()).toBe(translation('dedup.gui.duplicates.description'));
      expect(wrapper.findAll('thead th').map(th => th.text())).toEqual([
        translation('dedup.gui.duplicates.table.criterion'),
        translation('dedup.gui.duplicates.table.object1'),
        translation('dedup.gui.duplicates.table.object2'),
        '',
      ]);
    });

    it('shows title, id and metadata link of both objects', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse([DUPLICATES[0]]));

      const wrapper = await mountAt('/');

      const display = wrapper.findAll('tbody td')[1]!.get('.entry');
      expect(display.findAll('span')[0]!.text()).toBe('Title of mir_mods_00000003');
      expect(display.get('.text-muted').attributes('title')).toBe(translation('dedup.gui.objectId'));
      const link = display.get('a');
      expect(link.attributes('href')).toBe(`${BASE}receive/mir_mods_00000003`);
      expect(link.attributes('aria-label')).toBe(translation('dedup.gui.action.showMetadata'));
    });

    it('shows only the id of objects without title', async () => {
      fetchMock.on('api/dedup/duplicates', () =>
        jsonResponse([duplicate('mir_mods_00000001', 'mir_mods_00000002', [['doi', 'x']], false)])
      );

      const wrapper = await mountAt('/');

      const display = wrapper.findAll('tbody td')[1]!.get('.entry');
      expect(display.findAll('span').map(span => span.text())).toEqual(['(mir_mods_00000001)', '']);
    });

    it('shows a single criterion directly', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse([DUPLICATES[0]]));

      const wrapper = await mountAt('/');

      const cell = wrapper.get('tbody td');
      expect(cell.find('button').exists()).toBe(false);
      expect(cell.get('ul').isVisible()).toBe(true);
      expect(cell.get('li').text()).toBe('title: b');
    });

    it('collapses multiple criteria behind a toggle button', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse([DUPLICATES[1]]));

      const wrapper = await mountAt('/');

      const cell = wrapper.get('tbody td');
      const toggle = cell.get('button');
      const list = cell.get('ul');
      expect(toggle.text()).toBe(`2 ${translation('dedup.gui.duplicates.criteria')}`);
      expect(toggle.attributes('aria-controls')).toBe(list.attributes('id'));
      expect(toggle.attributes('aria-expanded')).toBe('false');
      expect(list.isVisible()).toBe(false);

      await toggle.trigger('click');
      expect(toggle.attributes('aria-expanded')).toBe('true');
      expect(toggle.find('.fa-chevron-down').exists()).toBe(true);
      expect(list.isVisible()).toBe(true);
      expect(list.findAll('li').map(li => li.text())).toEqual(['doi: 10.1000/1', 'title: a']);

      await toggle.trigger('click');
      expect(toggle.attributes('aria-expanded')).toBe('false');
      expect(list.isVisible()).toBe(false);
    });

    it('sorts by a column and reverses the order on the second click', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse(DUPLICATES));
      const wrapper = await mountAt('/');

      await header(wrapper, 2).trigger('click');
      expect(objectIds(wrapper, 2)).toEqual(['(mir_mods_00000002)', '(mir_mods_00000004)', '(mir_mods_00000009)']);
      expect(header(wrapper, 0).find('.fas').exists()).toBe(false);
      expect(header(wrapper, 2).find('.fa-sort-up').exists()).toBe(true);

      await header(wrapper, 2).trigger('click');
      expect(objectIds(wrapper, 2)).toEqual(['(mir_mods_00000009)', '(mir_mods_00000004)', '(mir_mods_00000002)']);
      expect(header(wrapper, 2).find('.fa-sort-down').exists()).toBe(true);

      await header(wrapper, 1).trigger('click');
      expect(objectIds(wrapper, 1)).toEqual(['(mir_mods_00000001)', '(mir_mods_00000003)', '(mir_mods_00000005)']);
      expect(header(wrapper, 1).find('.fa-sort-up').exists()).toBe(true);
    });

    it('shows a hint when there are no duplicates', async () => {
      fetchMock.on('api/dedup/duplicates', () => jsonResponse([]));

      const wrapper = await mountAt('/');

      expect(wrapper.get('.alert-info').text()).toBe(translation('dedup.gui.duplicates.empty'));
      expect(wrapper.find('table').exists()).toBe(false);
    });

    it('shows a spinner while loading', async () => {
      fetchMock.on('api/dedup/duplicates', () => new Promise<Response>(() => {}));

      const wrapper = await mountAt('/');

      expect(wrapper.find('.spinner-border').exists()).toBe(true);
      expect(wrapper.find('table').exists()).toBe(false);
    });

    it('explains a missing permission', async () => {
      fetchMock.on('api/dedup/duplicates', () => new Response('', { status: 403 }));

      const wrapper = await mountAt('/');

      expect(wrapper.get('.alert-danger').text()).toBe(translation('dedup.gui.notAllowed'));
    });

    it('shows the status of other server errors', async () => {
      fetchMock.on('api/dedup/duplicates', () => new Response('', { status: 500 }));

      const wrapper = await mountAt('/');

      expect(wrapper.get('.alert-danger').text()).toBe('500');
    });

    it('shows network errors', async () => {
      fetchMock.on('api/dedup/duplicates', () => Promise.reject(new TypeError('Failed to fetch')));

      const wrapper = await mountAt('/');

      expect(wrapper.get('.alert-danger').text()).toBe('TypeError: Failed to fetch');
    });

    it('shows an error when no jwt can be obtained', async () => {
      fetchMock.on('rsc/jwt', () => jsonResponse({ login_success: false }));

      const wrapper = await mountAt('/');

      expect(wrapper.get('.alert-danger').text()).toBe('Error: Could not obtain a JWT for the current session.');
      expect(fetchMock.apiRequests()).toEqual([]);
    });

    describe('filter by object', () => {
      it('loads only the duplicates of the given object', async () => {
        fetchMock.on('api/dedup/duplicates/', () => jsonResponse([DUPLICATES[0]]));

        const wrapper = await mountAt('/?id=mir_mods_00000003');

        expect(apiCalls()).toEqual([{ method: 'GET', url: `${BASE}api/dedup/duplicates/mir_mods_00000003` }]);
        const filter = wrapper.get('.alert-secondary');
        expect(filter.get('strong').text()).toBe('mir_mods_00000003');
        expect(filter.text()).toContain(translation('dedup.gui.duplicates.filter'));
        expect(filter.findAll('a').map(link => link.text())).toEqual([
          translation('dedup.gui.duplicates.filter.reset'),
        ]);
      });

      it('encodes the object id', async () => {
        fetchMock.on('api/dedup/duplicates/', () => jsonResponse([]));

        await mountAt('/?id=a%2Fb%20c');

        expect(apiCalls()).toEqual([{ method: 'GET', url: `${BASE}api/dedup/duplicates/a%2Fb%20c` }]);
      });

      it('ignores an empty id', async () => {
        fetchMock.on('api/dedup/duplicates', () => jsonResponse([]));

        const wrapper = await mountAt('/?id=');

        expect(apiCalls()).toEqual([{ method: 'GET', url: `${BASE}api/dedup/duplicates` }]);
        expect(wrapper.find('.alert-secondary').exists()).toBe(false);
      });

      it('offers a link back to the object on request', async () => {
        fetchMock.on('api/dedup/duplicates/', () => jsonResponse([]));

        const wrapper = await mountAt('/?id=mir_mods_00000003&showReturn=true');

        const links = wrapper.get('.alert-secondary').findAll('a');
        expect(links.map(link => link.text())).toEqual([
          translation('dedup.gui.duplicates.filter.return'),
          translation('dedup.gui.duplicates.filter.reset'),
        ]);
        expect(links[0]!.attributes('href')).toBe(`${BASE}receive/mir_mods_00000003`);
        expect(links[1]!.classes()).toContain('ms-2');
      });

      it('reloads all duplicates when the filter is reset', async () => {
        fetchMock
          .on('api/dedup/duplicates', () => jsonResponse(DUPLICATES))
          .on('api/dedup/duplicates/', () => jsonResponse([DUPLICATES[0]]));
        const wrapper = await mountAt('/?id=mir_mods_00000003');
        expect(wrapper.findAll('tbody tr')).toHaveLength(1);

        await wrapper.get('.alert-secondary a').trigger('click');
        await flushPromises();

        expect(router.currentRoute.value.fullPath).toBe('/');
        expect(wrapper.find('.alert-secondary').exists()).toBe(false);
        expect(wrapper.findAll('tbody tr')).toHaveLength(3);
        expect(apiCalls().map(call => call.url)).toEqual([
          `${BASE}api/dedup/duplicates/mir_mods_00000003`,
          `${BASE}api/dedup/duplicates`,
        ]);
      });
    });

    describe('mark as no duplicate', () => {
      const PAIR = DUPLICATES[0]!;

      async function openMarkDialog(): Promise<VueWrapper> {
        const wrapper = await mountAt('/');
        await wrapper.get('tbody tr button').trigger('click');
        return wrapper;
      }

      beforeEach(() => {
        fetchMock.on('api/dedup/duplicates', () => jsonResponse([PAIR]));
      });

      it('asks for confirmation and shows both objects', async () => {
        const wrapper = await openMarkDialog();

        expect(modalOf(wrapper).show).toHaveBeenCalledTimes(1);
        const modal = wrapper.get('.modal');
        expect(modal.get('.modal-title').text()).toBe(translation('dedup.gui.action.markNoDuplicate'));
        expect(modal.get('.modal-body').text()).toContain(translation('dedup.gui.action.markNoDuplicateConfirm'));
        expect(modal.findAll('.modal-body .entry .text-muted').map(id => id.text())).toEqual([
          '(mir_mods_00000003)',
          '(mir_mods_00000004)',
        ]);
      });

      it('posts the pair and reloads the duplicates when confirmed', async () => {
        fetchMock.on('api/dedup/no-duplicates', () => new Response('', { status: 201 }));
        const wrapper = await openMarkDialog();

        fetchMock.on('api/dedup/duplicates', () => jsonResponse([]));
        await wrapper.get('.modal-footer .btn-primary').trigger('click');
        await flushPromises();

        expect(modalOf(wrapper).hide).toHaveBeenCalled();
        const post = fetchMock.apiRequests().find(call => call.init?.method === 'POST');
        expect(post?.url).toBe(`${BASE}api/dedup/no-duplicates?id1=mir_mods_00000003&id2=mir_mods_00000004`);
        expect(new Headers(post?.init?.headers).get('authorization')).toBe('Bearer test-token');
        expect(apiCalls().map(call => call.method)).toEqual(['GET', 'POST', 'GET']);
        expect(wrapper.find('.alert-info').exists()).toBe(true);
        expect(wrapper.findAll('.modal-body .entry')).toHaveLength(0);
      });

      it('shows the status when marking fails', async () => {
        fetchMock.on('api/dedup/no-duplicates', () => new Response('', { status: 409 }));
        const wrapper = await openMarkDialog();

        await wrapper.get('.modal-footer .btn-primary').trigger('click');
        await flushPromises();

        expect(wrapper.get('.alert-danger').text()).toBe('409');
        expect(apiCalls().map(call => call.method)).toEqual(['GET', 'POST']);
      });

      it('does nothing when cancelled', async () => {
        const wrapper = await openMarkDialog();

        await wrapper.get('.modal-footer .btn-secondary').trigger('click');
        await flushPromises();

        expect(modalOf(wrapper).hide).toHaveBeenCalled();
        expect(apiCalls().map(call => call.method)).toEqual(['GET']);
        expect(wrapper.findAll('.modal-body .entry')).toHaveLength(0);
      });
    });
  });

  describe('no duplicates', () => {
    it('loads the pairs and sorts them by date with the newest first', async () => {
      fetchMock.on('api/dedup/no-duplicates', () => jsonResponse(NO_DUPLICATES));

      const wrapper = await mountAt('/no-duplicates');

      expect(apiCalls()).toEqual([{ method: 'GET', url: `${BASE}api/dedup/no-duplicates` }]);
      expect(columnTexts(wrapper, 2)).toEqual(['zoe', 'admin', 'editor']);
      expect(columnTexts(wrapper, 3)).toEqual([
        'not a date',
        new Date('2026-05-01T10:00:00Z').toLocaleString(),
        new Date('2026-03-01T10:00:00Z').toLocaleString(),
      ]);
      expect(header(wrapper, 3).find('.fa-sort-down').exists()).toBe(true);
    });

    it('renders the translated headers', async () => {
      fetchMock.on('api/dedup/no-duplicates', () => jsonResponse(NO_DUPLICATES));

      const wrapper = await mountAt('/no-duplicates');

      expect(wrapper.findAll('thead th').map(th => th.text())).toEqual([
        translation('dedup.gui.noDuplicates.table.object1'),
        translation('dedup.gui.noDuplicates.table.object2'),
        translation('dedup.gui.noDuplicates.table.creator'),
        translation('dedup.gui.noDuplicates.table.date'),
        '',
      ]);
    });

    it('sorts by creator and by object', async () => {
      fetchMock.on('api/dedup/no-duplicates', () => jsonResponse(NO_DUPLICATES));
      const wrapper = await mountAt('/no-duplicates');

      await header(wrapper, 2).trigger('click');
      expect(columnTexts(wrapper, 2)).toEqual(['admin', 'editor', 'zoe']);

      await header(wrapper, 2).trigger('click');
      expect(columnTexts(wrapper, 2)).toEqual(['zoe', 'editor', 'admin']);

      await header(wrapper, 1).trigger('click');
      expect(objectIds(wrapper, 1)).toEqual(['(mir_mods_00000002)', '(mir_mods_00000004)', '(mir_mods_00000006)']);
    });

    it('shows a hint when no pair is marked', async () => {
      fetchMock.on('api/dedup/no-duplicates', () => jsonResponse([]));

      const wrapper = await mountAt('/no-duplicates');

      expect(wrapper.get('.alert-info').text()).toBe(translation('dedup.gui.noDuplicates.empty'));
    });

    it('explains a missing permission', async () => {
      fetchMock.on('api/dedup/no-duplicates', () => new Response('', { status: 403 }));

      const wrapper = await mountAt('/no-duplicates');

      expect(wrapper.get('.alert-danger').text()).toBe(translation('dedup.gui.notAllowed'));
    });

    describe('delete', () => {
      async function openDeleteDialog(): Promise<VueWrapper> {
        const wrapper = await mountAt('/no-duplicates');
        await wrapper.findAll('tbody tr')[1]!.get('button').trigger('click');
        return wrapper;
      }

      beforeEach(() => {
        fetchMock.on('api/dedup/no-duplicates', () => jsonResponse(NO_DUPLICATES));
      });

      it('asks for confirmation and shows both objects', async () => {
        const wrapper = await openDeleteDialog();

        expect(modalOf(wrapper).show).toHaveBeenCalledTimes(1);
        expect(wrapper.get('.modal-title').text()).toBe(translation('dedup.gui.action.delete'));
        expect(wrapper.findAll('.modal-body .entry .text-muted').map(id => id.text())).toEqual([
          '(mir_mods_00000003)',
          '(mir_mods_00000004)',
        ]);
      });

      it.each([200, 204])('deletes the pair and reloads the list on status %i', async status => {
        fetchMock.on('api/dedup/no-duplicates/', () => new Response(null, { status }));
        const wrapper = await openDeleteDialog();

        await wrapper.get('.modal-footer .btn-danger').trigger('click');
        await flushPromises();

        expect(apiCalls()).toEqual([
          { method: 'GET', url: `${BASE}api/dedup/no-duplicates` },
          { method: 'DELETE', url: `${BASE}api/dedup/no-duplicates/2` },
          { method: 'GET', url: `${BASE}api/dedup/no-duplicates` },
        ]);
        expect(modalOf(wrapper).hide).toHaveBeenCalled();
      });

      it('shows the status when deleting fails', async () => {
        fetchMock.on('api/dedup/no-duplicates/', () => new Response('', { status: 500 }));
        const wrapper = await openDeleteDialog();

        await wrapper.get('.modal-footer .btn-danger').trigger('click');
        await flushPromises();

        expect(wrapper.get('.alert-danger').text()).toBe('500');
      });

      it('does nothing when cancelled', async () => {
        const wrapper = await openDeleteDialog();

        await wrapper.get('.modal-footer .btn-secondary').trigger('click');
        await flushPromises();

        expect(apiCalls().map(call => call.method)).toEqual(['GET']);
      });
    });
  });
});
