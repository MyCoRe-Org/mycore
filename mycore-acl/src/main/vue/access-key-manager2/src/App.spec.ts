import { afterEach, describe, expect, it, vi } from 'vitest';

import { WEB_APPLICATION_BASE_URL } from '@/test/setup';
import { jsonResponse, type FetchMock } from '@/test/helpers/fetchMock';
import {
  accessKey,
  accessKeyResponse,
  click,
  field,
  input,
  installBackend,
  modalEvent,
  query,
  queryAll,
  startApp,
  texts,
  settle,
  type StartedApp,
} from '@/test/helpers/appHarness';

const KEYS = [
  accessKey('secret-hash-1'),
  accessKey('secret-hash-2', {
    type: 'writedb',
    isActive: false,
    expiration: Date.UTC(2027, 0, 1),
  }),
];

const API = `${WEB_APPLICATION_BASE_URL}api/v2/access-keys`;

function listRequests(backend: FetchMock): string[] {
  return backend
    .requests('api/v2/access-keys/?')
    .filter(call => (call.init?.method ?? 'GET') === 'GET')
    .map(call => call.url);
}

function rows(): string[][] {
  return queryAll('tbody tr').map(row =>
    [...row.querySelectorAll('td')]
      .slice(0, 5)
      .map(cell => cell.textContent?.trim() ?? '')
  );
}

function modalButtons(id: string): HTMLButtonElement[] {
  return queryAll<HTMLButtonElement>(`#${id} .modal-footer button`);
}

describe('access key manager app', () => {
  let app: StartedApp | null = null;

  afterEach(() => {
    if (app !== null) {
      expect(app.vueWarnings()).toEqual([]);
      app = null;
    }
    document.body.innerHTML = '';
  });

  describe('startup', () => {
    it('loads the vue and access key translations in the current language', async () => {
      const backend = installBackend(KEYS);

      app = await startApp('/admin');

      expect(backend.requests('rsc/locale/').map(call => call.url)).toEqual([
        `${WEB_APPLICATION_BASE_URL}rsc/locale/translate/de/component.vue.*`,
        `${WEB_APPLICATION_BASE_URL}rsc/locale/translate/de/component.acl.accesskey.*`,
      ]);
      expect(query('h3').textContent?.trim()).toBe('t(title.main)');
      expect(texts('thead th')).toEqual([
        't(label.id)',
        't(label.reference)',
        't(label.permission)',
        't(label.active)',
        't(label.expiration)',
        't(label.actions)',
      ]);
    });

    it('redirects the root path to the admin view', async () => {
      installBackend(KEYS);

      app = await startApp('/');

      expect(app.router.currentRoute.value.fullPath).toBe('/admin');
      expect(window.location.pathname).toBe('/mir/access-key-manager/admin');
    });

    it('shows a message when the translations cannot be loaded', async () => {
      const consoleError = vi
        .spyOn(console, 'error')
        .mockImplementation(() => {});
      installBackend(KEYS).on(
        'rsc/locale/translate/de/component.acl.accesskey.',
        () => new Response('', { status: 500 })
      );

      app = await startApp('/admin');

      expect(query('#app').innerHTML).toBe('<p>Failed to initialize app.</p>');
      expect(consoleError).toHaveBeenCalledWith(
        'App initialization failed:',
        expect.any(Error)
      );
    });
  });

  describe('admin view', () => {
    it('lists the access keys with the jwt of the session', async () => {
      const backend = installBackend(KEYS);

      app = await startApp('/admin');

      expect(backend.requests('rsc/jwt').map(call => call.url)).toEqual([
        `${WEB_APPLICATION_BASE_URL}rsc/jwt`,
      ]);
      expect(listRequests(backend)).toEqual([`${API}/?limit=8`]);
      const [request] = backend.requests('api/v2/access-keys/?');
      expect(request?.init?.headers).toEqual({
        Authorization: 'Bearer test-token',
        Accept: 'application/json',
      });
      expect(rows()).toEqual([
        ['secret-hash-1', 'mir_mods_00000001', 'read', 't(button.yes)', '-'],
        [
          'secret-hash-2',
          'mir_mods_00000001',
          'writedb',
          't(button.no)',
          '2027-01-01',
        ],
      ]);
      expect(query('#loading-overlay').style.visibility).toBe('hidden');
    });

    it('restricts the list to the reference of the path', async () => {
      const backend = installBackend(KEYS);

      app = await startApp('/admin/mir_mods_00000002');

      expect(query('h3 span').textContent).toBe('(mir_mods_00000002)');
      expect(backend.requests('rsc/jwt').map(call => call.url)).toEqual([
        `${WEB_APPLICATION_BASE_URL}rsc/jwt?ua=acckey_mir_mods_00000002&sa=acckey_mir_mods_00000002`,
      ]);
      expect(listRequests(backend)).toEqual([
        `${API}/?reference=mir_mods_00000002&limit=8`,
      ]);
    });

    it('passes permissions, page and page size of the query to the api', async () => {
      const backend = installBackend(KEYS, 12);

      app = await startApp(
        '/admin?availablePermissions=read,writedb&page=2&pageSize=5'
      );

      expect(listRequests(backend)).toEqual([
        `${API}/?permissions=read%2Cwritedb&offset=5&limit=5`,
      ]);
    });

    it('pages through the access keys and keeps the page in the url', async () => {
      const backend = installBackend(KEYS, 20);
      app = await startApp('/admin');

      expect(texts('.pagination .page-link')).toEqual([
        '‹',
        '1',
        '2',
        '3',
        '›',
      ]);
      await click(queryAll('.pagination .page-link')[2]!);

      expect(listRequests(backend)).toEqual([
        `${API}/?limit=8`,
        `${API}/?offset=8&limit=8`,
      ]);
      expect(app.router.currentRoute.value.query).toEqual({ page: '2' });
      expect(query('.pagination .active').textContent?.trim()).toBe('2');
    });
  });

  describe('reference view', () => {
    it('requires the available permissions', async () => {
      const backend = installBackend(KEYS);

      app = await startApp('/mir_mods_00000002');

      expect(app.router.currentRoute.value.name).toBe('403');
      expect(query('.alert-danger').textContent).toContain(
        't(error.403.message)'
      );
      expect(listRequests(backend)).toEqual([]);
    });

    it('lists the access keys of the reference with the available permissions', async () => {
      const backend = installBackend(KEYS);

      app = await startApp('/mir_mods_00000002?availablePermissions=read');

      expect(query('h3 span').textContent).toBe('(mir_mods_00000002)');
      expect(listRequests(backend)).toEqual([
        `${API}/?reference=mir_mods_00000002&permissions=read&limit=8`,
      ]);
      expect(rows()).toHaveLength(2);
    });
  });

  describe('errors', () => {
    it.each([
      [401, '401', 't(error.401.message)'],
      [403, '403', 't(error.403.message)'],
      [500, 'error', 't(error.general.message)'],
    ])('shows the error view for status %i', async (status, route, message) => {
      installBackend().on(
        'api/v2/access-keys',
        () => new Response('', { status })
      );

      app = await startApp('/admin');

      expect(app.router.currentRoute.value.name).toBe(route);
      expect(query('.alert-danger').textContent).toContain(message);
    });

    it('shows the general error view when no jwt can be obtained', async () => {
      const backend = installBackend(KEYS).on('rsc/jwt', () =>
        jsonResponse({ login_success: false })
      );

      app = await startApp('/admin');

      expect(app.router.currentRoute.value.name).toBe('error');
      expect(listRequests(backend)).toEqual([]);
    });
  });

  describe('delete', () => {
    async function requestDelete(backend: FetchMock): Promise<void> {
      app = await startApp('/admin');
      backend.on(/api\/v2\/access-keys\/secret-hash-2$/, () => {
        backend.on('api/v2/access-keys/?', () => accessKeyResponse([KEYS[0]!]));
        return new Response(null, { status: 204 });
      });
      const shown = modalEvent('confirmModal', 'shown');
      await click(
        queryAll('tbody tr')[1]!.querySelector('.fa-trash')!.closest('button')!
      );
      await shown;
    }

    it('asks for confirmation', async () => {
      const backend = installBackend(KEYS);

      await requestDelete(backend);

      const modal = query('#confirmModal');
      expect(modal.classList).toContain('show');
      expect(modal.querySelector('.modal-title')?.textContent).toBe(
        't(confirmRemove.title)'
      );
      expect(modal.querySelector('.modal-body')?.textContent?.trim()).toBe(
        't(confirmRemove.text secret-hash-2)'
      );
      expect(modalButtons('confirmModal')).toHaveLength(2);
    });

    it('deletes the key when confirmed', async () => {
      const backend = installBackend(KEYS);
      await requestDelete(backend);

      const hidden = modalEvent('confirmModal', 'hidden');
      await click(modalButtons('confirmModal').at(-1)!);
      await hidden;
      await settle();

      const deletion = backend.calls.find(
        call => call.init?.method === 'DELETE'
      );
      expect(deletion?.url).toBe(`${API}/secret-hash-2`);
      expect(deletion?.init?.headers).toEqual({
        Authorization: 'Bearer test-token',
      });
      expect(rows().map(row => row[0])).toEqual(['secret-hash-1']);
      expect(query('#confirmModal').classList).not.toContain('show');
    });

    it('keeps the key when cancelled', async () => {
      const backend = installBackend(KEYS);
      await requestDelete(backend);

      const hidden = modalEvent('confirmModal', 'hidden');
      await click(modalButtons('confirmModal')[0]!);
      await hidden;
      await settle();

      expect(backend.calls.some(call => call.init?.method === 'DELETE')).toBe(
        false
      );
      expect(rows()).toHaveLength(2);
    });

    it('shows the error view when deleting fails', async () => {
      const backend = installBackend(KEYS);
      await requestDelete(backend);
      backend.on(
        /api\/v2\/access-keys\/secret-hash-2$/,
        () => new Response('', { status: 403 })
      );

      await click(modalButtons('confirmModal').at(-1)!);
      await settle();

      expect(app?.router.currentRoute.value.name).toBe('403');
    });
  });

  describe('create', () => {
    const CREATED = accessKey('new-hash', {
      reference: 'mir_mods_00000007',
      type: 'read',
      comment: 'for review',
    });

    async function openCreateDialog(backend: FetchMock): Promise<void> {
      app = await startApp('/admin');
      backend
        .on('api/v2/access-keys', (_url, init) =>
          init?.method === 'POST'
            ? new Response(null, {
                status: 201,
                headers: { Location: `${API}/new-hash` },
              })
            : accessKeyResponse([...KEYS, CREATED])
        )
        .on(/api\/v2\/access-keys\/new-hash$/, () => jsonResponse(CREATED));
      const shown = modalEvent('createAccessKeyModal', 'shown');
      await click('.btn-primary .fa-plus');
      await shown;
    }

    it('opens the create dialog', async () => {
      const backend = installBackend(KEYS);

      await openCreateDialog(backend);

      const modal = query('#createAccessKeyModal');
      expect(modal.classList).toContain('show');
      expect(modal.querySelector('.modal-body p')?.textContent).toBe(
        't(description.createAccesskey)'
      );
      expect(texts('#createAccessKeyModal label')).toEqual([
        't(label.reference)',
        't(label.secret)',
        't(label.permission)',
        't(label.expiration)',
        't(label.active)',
        't(label.comment)',
      ]);
    });

    it('validates the required fields before sending', async () => {
      const backend = installBackend(KEYS);
      await openCreateDialog(backend);

      await click(modalButtons('createAccessKeyModal')[0]!);
      await settle();

      expect(backend.calls.some(call => call.init?.method === 'POST')).toBe(
        false
      );
      expect(
        queryAll('#createForm .is-invalid').map(element =>
          document
            .querySelector(`label[for="${element.id}"]`)
            ?.textContent?.trim()
        )
      ).toEqual([
        't(label.reference)',
        't(label.secret)',
        't(label.permission)',
      ]);
    });

    it('creates the key, shows the activation link and reloads the list', async () => {
      const backend = installBackend(KEYS);
      await openCreateDialog(backend);

      await input(
        field('createAccessKeyModal', 'label.reference'),
        'mir_mods_00000007'
      );
      await input(field('createAccessKeyModal', 'label.secret'), 'my secret');
      await input(field('createAccessKeyModal', 'label.permission'), 'read');
      await input(
        field('createAccessKeyModal', 'label.expiration'),
        '2099-12-31'
      );
      await input(field('createAccessKeyModal', 'label.comment'), 'for review');
      const created = modalEvent('createAccessKeyModal', 'hidden');
      const confirmationShown = modalEvent('confirmModal', 'shown');
      await click(modalButtons('createAccessKeyModal')[0]!);
      await created;
      await confirmationShown;
      await settle();

      const creation = backend.calls.find(call => call.init?.method === 'POST');
      expect(creation?.url).toBe(API);
      expect(JSON.parse(String(creation?.init?.body))).toEqual({
        reference: 'mir_mods_00000007',
        secret: 'my secret',
        isActive: true,
        type: 'read',
        expiration: Date.UTC(2099, 11, 31),
        comment: 'for review',
      });
      expect(backend.requests('api/v2/access-keys/new-hash')).toHaveLength(1);
      expect(query('#createAccessKeyModal').classList).not.toContain('show');

      const confirmation = query('#confirmModal');
      expect(confirmation.classList).toContain('show');
      expect(confirmation.querySelector('.modal-title')?.textContent).toBe(
        't(success.add.title)'
      );
      expect(
        confirmation.querySelector('.modal-body')?.textContent?.trim()
      ).toBe(
        `t(success.add my secret) t(success.add.url) ${WEB_APPLICATION_BASE_URL}receive/mir_mods_00000007?accesskey=my%20secret`
      );
      expect(modalButtons('confirmModal')).toHaveLength(1);
      expect(rows().map(row => row[0])).toEqual([
        'secret-hash-1',
        'secret-hash-2',
        'new-hash',
      ]);
    });

    it('omits the activation link for permissions that cannot be activated in the session', async () => {
      const backend = installBackend(KEYS);
      await openCreateDialog(backend);
      backend.on(/api\/v2\/access-keys\/new-hash$/, () =>
        jsonResponse({ ...CREATED, type: 'delete' })
      );

      await input(
        field('createAccessKeyModal', 'label.reference'),
        'mir_mods_00000007'
      );
      await input(field('createAccessKeyModal', 'label.secret'), 'my secret');
      await input(field('createAccessKeyModal', 'label.permission'), 'delete');
      const confirmationShown = modalEvent('confirmModal', 'shown');
      await click(modalButtons('createAccessKeyModal')[0]!);
      await confirmationShown;

      expect(query('#confirmModal .modal-body').textContent?.trim()).toBe(
        't(success.add my secret)'
      );
    });

    it('shows the error in the dialog when creating fails', async () => {
      const backend = installBackend(KEYS);
      await openCreateDialog(backend);
      backend.on('api/v2/access-keys', (_url, init) =>
        init?.method === 'POST'
          ? new Response('', { status: 409, statusText: 'Conflict' })
          : accessKeyResponse(KEYS)
      );

      await input(
        field('createAccessKeyModal', 'label.reference'),
        'mir_mods_00000007'
      );
      await input(field('createAccessKeyModal', 'label.secret'), 'my secret');
      await input(field('createAccessKeyModal', 'label.permission'), 'read');
      await click(modalButtons('createAccessKeyModal')[0]!);
      await settle();

      expect(
        query('#createAccessKeyModal .alert-danger').textContent
      ).toContain('409 Conflict');
      expect(query('#createAccessKeyModal').classList).toContain('show');
    });
  });

  describe('edit', () => {
    async function openEditDialog(backend: FetchMock): Promise<void> {
      app = await startApp('/admin');
      const shown = modalEvent('infoAccessKeyModal', 'shown');
      await click(
        queryAll('tbody tr')[1]!.querySelector('.fa-edit')!.closest('button')!
      );
      await shown;
      backend.on(/api\/v2\/access-keys\/secret-hash-2$/, (_url, init) =>
        init?.method === 'PATCH'
          ? new Response(null, { status: 204 })
          : jsonResponse({ ...KEYS[1], isActive: true, comment: 'checked' })
      );
    }

    it('shows the values of the selected key', async () => {
      const backend = installBackend(KEYS);

      await openEditDialog(backend);

      expect(query('#infoAccessKeyModal').classList).toContain('show');
      expect(field('infoAccessKeyModal', 'label.reference').value).toBe(
        'mir_mods_00000001'
      );
      expect(field('infoAccessKeyModal', 'label.permission').value).toBe(
        'writedb'
      );
      expect(field('infoAccessKeyModal', 'label.expiration').value).toBe(
        '2027-01-01'
      );
      expect(field('infoAccessKeyModal', 'label.active').checked).toBe(false);
    });

    it('sends only the changed values and updates the row', async () => {
      const backend = installBackend(KEYS);
      await openEditDialog(backend);

      await click(field('infoAccessKeyModal', 'label.active'));
      await input(field('infoAccessKeyModal', 'label.comment'), 'checked');
      const hidden = modalEvent('infoAccessKeyModal', 'hidden');
      await click(modalButtons('infoAccessKeyModal')[0]!);
      await hidden;
      await settle();

      const update = backend.calls.find(call => call.init?.method === 'PATCH');
      expect(update?.url).toBe(`${API}/secret-hash-2`);
      expect(JSON.parse(String(update?.init?.body))).toEqual({
        comment: 'checked',
        isActive: true,
      });
      expect(rows()[1]).toEqual([
        'secret-hash-2',
        'mir_mods_00000001',
        'writedb',
        't(button.yes)',
        '2027-01-01',
      ]);
      expect(query('#infoAccessKeyModal').classList).not.toContain('show');
    });
  });
});
