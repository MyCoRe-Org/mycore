import { flushPromises, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import router from '@/router';
import { EXPANDED_VIEW_PREFERENCE_KEY } from '@/views/TextEditorViewState';
import { WEB_APPLICATION_BASE_URL } from '@/test/setup';
import { FetchMock, jsonResponse } from '@/test/helpers/fetchMock';
import { mountAppAt, type MountedApp } from '@/test/helpers/mountApp';

const OBJECT_ID = 'mir_mods_00000001';
const DERIVATE_ID = 'mir_derivate_00000001';
const API = `${WEB_APPLICATION_BASE_URL}api/v2/`;

const OBJECT_XML = `<mycoreobject ID="${OBJECT_ID}"><metadata><related>mir_mods_00000002</related></metadata></mycoreobject>`;
const PRETTY_OBJECT_XML = [
  `<mycoreobject ID="${OBJECT_ID}">`,
  '  <metadata>',
  '    <related>mir_mods_00000002</related>',
  '  </metadata>',
  '</mycoreobject>',
].join('\n');

function xml(body: string, init: ResponseInit = {}): Response {
  return new Response(body, { ...init, headers: { 'Content-Type': 'application/xml', ...(init.headers ?? {}) } });
}

describe('texteditor app', () => {
  let backend: FetchMock;
  let app: MountedApp | null = null;

  function installBackend(): FetchMock {
    return new FetchMock()
      .on('rsc/jwt', () => jsonResponse({ login_success: true, access_token: 'test-token', token_type: 'Bearer' }))
      .on(/\/api\/v2\/objects\/.*\/try$/, () => new Response(null, { status: 200 }))
      .on(/\/api\/v2\/objects\/[^/]+\/lock$/, () => new Response(null, { status: 200 }))
      .on(`api/v2/objects/${OBJECT_ID}`, (_url, init) =>
        init?.method === 'PUT' ? new Response(null, { status: 204 }) : xml(OBJECT_XML)
      );
  }

  async function mountAt(path: string): Promise<VueWrapper> {
    app = await mountAppAt(path);
    return app.wrapper;
  }

  function calls(filter: (method: string, url: string) => boolean = () => true): string[] {
    return backend.apiRequests()
      .map(call => `${call.init?.method ?? 'GET'} ${call.url}`)
      .filter(entry => filter(entry.split(' ')[0]!, entry.split(' ')[1]!));
  }

  function textarea(wrapper: VueWrapper): HTMLTextAreaElement {
    return wrapper.get('textarea').element as HTMLTextAreaElement;
  }

  function button(wrapper: VueWrapper, label: string) {
    const found = wrapper.findAll('button').find(candidate => candidate.text() === label);
    if (found === undefined) {
      throw new Error(`no button labeled ${label}`);
    }
    return found;
  }

  function hasButton(wrapper: VueWrapper, label: string): boolean {
    return wrapper.findAll('button').some(candidate => candidate.text() === label);
  }

  async function edit(wrapper: VueWrapper, text: string): Promise<void> {
    await wrapper.get('textarea').setValue(text);
    await flushPromises();
  }

  function breadcrumbs(wrapper: VueWrapper): string[] {
    return wrapper.findAll('.breadcrumb > span:not(.breadcrumb-sep)').map(crumb =>
      crumb.classes('breadcrumb-link') ? `[${crumb.text()}]` : crumb.text()
    );
  }

  beforeEach(async () => {
    localStorage.clear();
    vi.spyOn(console, 'log').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
    backend = installBackend();
    await router.replace('/');
  });

  afterEach(async () => {
    if (app !== null) {
      expect(app.warnings).toEqual([]);
      expect(app.errors).toEqual([]);
      app.wrapper.unmount();
      app = null;
      await flushPromises();
    }
    vi.useRealTimers();
  });

  it('shows the not found page for paths without type', async () => {
    const wrapper = await mountAt('/');

    expect(wrapper.get('h2').text()).toBe('404');
    expect(wrapper.get('h3').text()).toBe('Page not found');
  });

  describe('object', () => {
    it('loads the prettified object with the jwt and locks it', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(calls()).toEqual([
        `GET ${API}objects/${OBJECT_ID}?expanded=false`,
        `PUT ${API}objects/${OBJECT_ID}/try`,
        `PUT ${API}objects/${OBJECT_ID}/lock`,
      ]);
      const load = backend.apiRequests()[0]!;
      expect(load.init?.cache).toBe('no-store');
      expect(load.init?.headers).toEqual({ Authorization: 'Bearer test-token' });
      expect(textarea(wrapper).value).toBe(PRETTY_OBJECT_XML);
      expect(textarea(wrapper).readOnly).toBe(false);
      expect(wrapper.find('.spinner').exists()).toBe(false);
    });

    it('offers object specific actions', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(breadcrumbs(wrapper)).toEqual([OBJECT_ID]);
      expect(hasButton(wrapper, 'View Object')).toBe(true);
      expect(hasButton(wrapper, 'View Contents')).toBe(false);
      expect(button(wrapper, 'Original').attributes('aria-pressed')).toBe('true');
      expect(button(wrapper, 'Original').classes()).toContain('active');
      expect(button(wrapper, 'Expanded').attributes('aria-pressed')).toBe('false');
      expect(button(wrapper, 'Update').attributes('disabled')).toBeDefined();
    });

    it('highlights the xml and turns mycore ids into links', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      const preview = wrapper.get('pre.prism-editor__editor');
      expect(preview.find('.token.tag').exists()).toBe(true);
      expect(preview.findAll('.mcr-link').map(link => link.attributes('data-mcr-id'))).toEqual([
        OBJECT_ID,
        'mir_mods_00000002',
      ]);
    });

    it('enables the update button when the content is changed', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      await edit(wrapper, `${PRETTY_OBJECT_XML}\n`);

      expect(button(wrapper, 'Update').attributes('disabled')).toBeUndefined();
    });

    it('saves the object and reloads it', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);
      const changed = PRETTY_OBJECT_XML.replace('mir_mods_00000002', 'mir_mods_00000003');

      await edit(wrapper, changed);
      await button(wrapper, 'Update').trigger('click');
      await flushPromises();

      const save = backend.apiRequests().find(call => call.init?.method === 'PUT' && call.url.endsWith(OBJECT_ID))!;
      expect(save.init?.headers).toEqual({
        'Content-Type': 'application/xml; charset=utf-8',
        Authorization: 'Bearer test-token',
      });
      expect(save.init?.body).toBe(changed);
      expect(calls(method => method === 'GET')).toHaveLength(2);
      expect(wrapper.get('.alert-success').text()).toBe('Successfully saved!');
      expect(button(wrapper, 'Update').attributes('disabled')).toBeDefined();
    });

    it('shows the server error when saving fails and allows to retry', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);
      backend.on(`api/v2/objects/${OBJECT_ID}`, (_url, init) =>
        init?.method === 'PUT'
          ? new Response(null, {
              status: 400,
              headers: { 'X-Error-Message': 'Invalid', 'X-Error-Detail': 'line 3', 'X-Error-Uuid': 'abc-123' },
            })
          : xml(OBJECT_XML)
      );

      await edit(wrapper, 'broken');
      await button(wrapper, 'Update').trigger('click');
      await flushPromises();

      const message = wrapper.get('.alert-danger').text();
      expect(message).toContain(`Unable to save ${OBJECT_ID}. Request failed with status code 400. Invalid - line 3`);
      expect(message).toContain('Error Log Code: abc-123');
      expect(wrapper.find('.alert-success').exists()).toBe(false);
      expect(button(wrapper, 'Update').attributes('disabled')).toBeUndefined();
      expect(textarea(wrapper).value).toBe('broken');
    });

    it('shows an error when the object cannot be loaded', async () => {
      backend.on(`api/v2/objects/${OBJECT_ID}?`, () => new Response(null, { status: 404 }));

      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(wrapper.get('.alert-danger').text()).toContain(
        `Unable to load ${OBJECT_ID}. Request failed with status code 404.`
      );
      expect(wrapper.find('.spinner').exists()).toBe(false);
      expect(calls(method => method === 'PUT').some(call => call.endsWith('/lock'))).toBe(false);
    });

    it('fails to load content without content type', async () => {
      backend.on(`api/v2/objects/${OBJECT_ID}?`, () => new Response(new TextEncoder().encode('<x/>')));

      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(wrapper.get('.alert-danger').text()).toBe(
        `Unable to load ${OBJECT_ID}. Request was successful but the 'Content-Type' is empty.`
      );
    });

    it('opens the object readonly without write access', async () => {
      backend.on(/\/api\/v2\/objects\/.*\/try$/, () => new Response(null, { status: 403 }));

      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(hasButton(wrapper, 'Update')).toBe(false);
      expect(textarea(wrapper).readOnly).toBe(true);
      expect(calls().some(call => call.endsWith('/lock'))).toBe(false);
    });

    it('warns when another user holds the lock', async () => {
      backend.on(/\/api\/v2\/objects\/[^/]+\/lock$/, () => new Response(null, { status: 409 }));

      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(wrapper.get('.alert-warning').text()).toBe('This object is currently being edited by another user!');
    });

    it('shows an error when locking fails', async () => {
      backend.on(/\/api\/v2\/objects\/[^/]+\/lock$/, (_url, init) =>
        new Response(null, { status: init?.method === 'PUT' ? 500 : 200 })
      );

      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      expect(wrapper.get('.alert-danger').text()).toContain(`Unable to lock ${OBJECT_ID}.`);
    });

    it('refreshes the lock every 30 seconds', async () => {
      vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
      await mountAt(`/objects/${OBJECT_ID}`);
      const lockCount = () => calls(method => method === 'PUT').filter(call => call.endsWith('/lock')).length;
      expect(lockCount()).toBe(1);

      vi.advanceTimersByTime(30000);
      await flushPromises();
      expect(lockCount()).toBe(2);

      vi.advanceTimersByTime(60000);
      await flushPromises();
      expect(lockCount()).toBe(4);
    });

    it('releases the lock when the editor is closed', async () => {
      await mountAt(`/objects/${OBJECT_ID}`);

      app!.wrapper.unmount();
      app = null;
      await flushPromises();

      const unlock = backend.apiRequests().find(call => call.init?.method === 'DELETE');
      expect(unlock?.url).toBe(`${API}objects/${OBJECT_ID}/lock`);
      expect(unlock?.init?.keepalive).toBe(true);
    });

    it('releases the lock when the page is left', async () => {
      await mountAt(`/objects/${OBJECT_ID}`);

      window.dispatchEvent(new Event('beforeunload'));
      await flushPromises();

      expect(calls(method => method === 'DELETE')).toEqual([`DELETE ${API}objects/${OBJECT_ID}/lock`]);
    });

    it('restores the loaded content when undo would clear the editor', async () => {
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      await wrapper.get('textarea').trigger('keydown', { key: 'z', ctrlKey: true });
      await edit(wrapper, '');

      expect(textarea(wrapper).value).toBe(PRETTY_OBJECT_XML);
      expect(button(wrapper, 'Update').attributes('disabled')).toBeDefined();
    });

    describe('expanded view', () => {
      beforeEach(() => {
        backend.on(`api/v2/objects/${OBJECT_ID}?expanded=true`, () =>
          xml(`<mycoreobject ID="${OBJECT_ID}"><structure/></mycoreobject>`)
        );
      });

      it('loads the expanded object readonly and releases the lock', async () => {
        const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

        await button(wrapper, 'Expanded').trigger('click');
        await flushPromises();

        expect(calls().slice(3)).toEqual([
          `DELETE ${API}objects/${OBJECT_ID}/lock`,
          `GET ${API}objects/${OBJECT_ID}?expanded=true`,
        ]);
        expect(textarea(wrapper).value).toContain('<structure/>');
        expect(textarea(wrapper).readOnly).toBe(true);
        expect(button(wrapper, 'Expanded').attributes('aria-pressed')).toBe('true');
        expect(button(wrapper, 'Update').attributes('disabled')).toBeDefined();
        expect(localStorage.getItem(EXPANDED_VIEW_PREFERENCE_KEY)).toBe('true');
      });

      it('locks the object again when switching back', async () => {
        const wrapper = await mountAt(`/objects/${OBJECT_ID}`);
        await button(wrapper, 'Expanded').trigger('click');
        await flushPromises();

        await button(wrapper, 'Original').trigger('click');
        await flushPromises();

        expect(calls().slice(-2)).toEqual([
          `GET ${API}objects/${OBJECT_ID}?expanded=false`,
          `PUT ${API}objects/${OBJECT_ID}/lock`,
        ]);
        expect(textarea(wrapper).readOnly).toBe(false);
        expect(localStorage.getItem(EXPANDED_VIEW_PREFERENCE_KEY)).toBe('false');
      });

      it('opens objects in the expanded view when it was chosen before', async () => {
        localStorage.setItem(EXPANDED_VIEW_PREFERENCE_KEY, 'true');

        const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

        expect(calls()).toEqual([`GET ${API}objects/${OBJECT_ID}?expanded=true`, `PUT ${API}objects/${OBJECT_ID}/try`]);
        expect(textarea(wrapper).readOnly).toBe(true);
      });

      it('asks before discarding unsaved changes', async () => {
        const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false);
        const wrapper = await mountAt(`/objects/${OBJECT_ID}`);
        await edit(wrapper, 'changed');

        await button(wrapper, 'Expanded').trigger('click');
        await flushPromises();

        expect(confirm).toHaveBeenCalledWith('Discard unsaved changes and switch view?');
        expect(textarea(wrapper).value).toBe('changed');
        expect(calls().some(call => call.includes('expanded=true'))).toBe(false);

        confirm.mockReturnValue(true);
        await button(wrapper, 'Expanded').trigger('click');
        await flushPromises();
        expect(textarea(wrapper).value).toContain('<structure/>');
      });
    });
  });

  describe('derivate', () => {
    const PATH = `${OBJECT_ID}/derivates/${DERIVATE_ID}`;

    beforeEach(() => {
      backend.on(`api/v2/objects/${PATH}?`, () => xml(`<mycorederivate ID="${DERIVATE_ID}"/>`));
    });

    it('loads the derivate without locking and expanded view', async () => {
      const wrapper = await mountAt(`/objects/${PATH}`);

      expect(calls()).toEqual([`GET ${API}objects/${PATH}?expanded=false`, `PUT ${API}objects/${PATH}/try`]);
      expect(breadcrumbs(wrapper)).toEqual([`[${OBJECT_ID}]`, DERIVATE_ID]);
      expect(hasButton(wrapper, 'View Contents')).toBe(true);
      expect(hasButton(wrapper, 'View Object')).toBe(false);
      expect(hasButton(wrapper, 'Expanded')).toBe(false);
      expect(hasButton(wrapper, 'Update')).toBe(true);
    });

    it('navigates to the object via the breadcrumb', async () => {
      const wrapper = await mountAt(`/objects/${PATH}`);

      await wrapper.get('.breadcrumb-link').trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.path).toBe(`/objects/${OBJECT_ID}`);
      expect(textarea(wrapper).value).toBe(PRETTY_OBJECT_XML);
    });

    it('navigates to its contents', async () => {
      backend.on(`api/v2/objects/${PATH}/contents?`, () => xml('<contents><file>mets.xml</file></contents>'));
      const wrapper = await mountAt(`/objects/${PATH}`);

      await button(wrapper, 'View Contents').trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.path).toBe(`/objects/${PATH}/contents`);
      expect(hasButton(wrapper, 'Update')).toBe(false);
      expect(calls().at(-1)).toBe(`PUT ${API}objects/${PATH}/try`);
    });
  });

  describe('contents', () => {
    const CONTENTS = `${OBJECT_ID}/derivates/${DERIVATE_ID}/contents`;

    it('links the text files of the listing', async () => {
      backend
        .on(`api/v2/objects/${CONTENTS}?`, () =>
          xml('<contents><file>mets.xml</file><file>cover.jpg</file><file>readme.md</file></contents>')
        )
        .on(`api/v2/objects/${CONTENTS}/readme.md?`, () =>
          new Response('# Readme', { headers: { 'Content-Type': 'text/markdown' } })
        );
      const wrapper = await mountAt(`/objects/${CONTENTS}`);

      const links = wrapper.findAll('.file-link');
      expect(links.map(link => link.attributes('data-file-name'))).toEqual(['mets.xml', 'readme.md']);

      await links[1]!.trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.path).toBe(`/objects/${CONTENTS}/readme.md`);
      expect(textarea(wrapper).value).toBe('# Readme');
      expect(wrapper.get('pre.prism-editor__editor').find('.token.title').exists()).toBe(true);
    });

    it('saves a content file without reloading it', async () => {
      const FILE = `${CONTENTS}/readme.md`;
      backend.on(`api/v2/objects/${FILE}`, (_url, init) =>
        init?.method === 'PUT'
          ? new Response(null, { status: 204 })
          : new Response('# Readme', { headers: { 'Content-Type': 'text/markdown' } })
      );
      const wrapper = await mountAt(`/objects/${FILE}`);

      expect(breadcrumbs(wrapper)).toEqual([`[${OBJECT_ID}]`, `[${DERIVATE_ID}]`, '[contents]', 'readme.md']);
      await edit(wrapper, '# Readme\n\nchanged');
      await button(wrapper, 'Update').trigger('click');
      await flushPromises();

      expect(calls(method => method !== 'GET')).toEqual([
        `PUT ${API}objects/${OBJECT_ID}/derivates/${DERIVATE_ID}/try`,
        `PUT ${API}objects/${FILE}`,
      ]);
      const save = backend.apiRequests().find(call => call.url === `${API}objects/${FILE}` && call.init?.method === 'PUT');
      expect((save?.init?.headers as Record<string, string>)['Content-Type']).toBe('text/markdown; charset=utf-8');
      expect(calls(method => method === 'GET')).toHaveLength(1);
      expect(button(wrapper, 'Update').attributes('disabled')).toBeDefined();
    });
  });

  describe('links', () => {
    it.each([
      ['mir_mods_00000002', '/objects/mir_mods_00000002'],
      ['mir_class_00000001', '/classifications/mir_class_00000001'],
      ['mir_derivate_00000009', `/objects/${OBJECT_ID}/derivates/mir_derivate_00000009`],
    ])('opens %s', async (id, path) => {
      backend
        .on(`api/v2/objects/${OBJECT_ID}?`, () => xml(`<mycoreobject><ref>${id}</ref></mycoreobject>`))
        .on(/\/api\/v2\/(objects|classifications)\/.*(mir_mods_00000002|mir_class|mir_derivate_00000009)/, () =>
          xml('<target/>')
        );
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);

      await wrapper.get(`.mcr-link[data-mcr-id="${id}"]`).trigger('click');
      await flushPromises();

      expect(router.currentRoute.value.path).toBe(path);
    });

    it('follows links in the editable textarea only with the ctrl key', async () => {
      backend.on(/mir_mods_00000002/, () => xml('<target/>'));
      const wrapper = await mountAt(`/objects/${OBJECT_ID}`);
      const link = wrapper.get('.mcr-link[data-mcr-id="mir_mods_00000002"]').element;
      vi.spyOn(link, 'getBoundingClientRect').mockReturnValue(new DOMRect(10, 10, 100, 20));
      const at = { clientX: 20, clientY: 15 };

      await wrapper.get('textarea').trigger('mousemove', { ...at, ctrlKey: true });
      expect(textarea(wrapper).style.cursor).toBe('pointer');
      await wrapper.get('textarea').trigger('mouseleave');
      expect(textarea(wrapper).style.cursor).toBe('');

      await wrapper.get('textarea').trigger('click', at);
      await flushPromises();
      expect(router.currentRoute.value.path).toBe(`/objects/${OBJECT_ID}`);

      await wrapper.get('textarea').trigger('click', { ...at, ctrlKey: true });
      await flushPromises();
      expect(router.currentRoute.value.path).toBe('/objects/mir_mods_00000002');
    });
  });

  describe('classification', () => {
    const CLASS_ID = 'mir_institutes';

    beforeEach(() => {
      backend.on(`api/v2/classifications/${CLASS_ID}`, (_url, init) =>
        init?.method === 'PUT'
          ? new Response(null, { status: 204 })
          : xml(`<mycoreclass ID="${CLASS_ID}"><categories/></mycoreclass>`)
      );
    });

    it('loads the classification without authorization, lock and expanded view', async () => {
      const wrapper = await mountAt(`/classifications/${CLASS_ID}`);

      expect(calls()).toEqual([`GET ${API}classifications/${CLASS_ID}`]);
      expect(backend.apiRequests()[0]!.init).toEqual({ cache: 'no-store' });
      expect(textarea(wrapper).value).toBe(`<mycoreclass ID="${CLASS_ID}">\n  <categories/>\n</mycoreclass>`);
      expect(hasButton(wrapper, 'Expanded')).toBe(false);
      expect(hasButton(wrapper, 'View Object')).toBe(false);
      expect(textarea(wrapper).readOnly).toBe(false);
    });

    it('saves the classification as xml', async () => {
      const wrapper = await mountAt(`/classifications/${CLASS_ID}`);

      await edit(wrapper, '<mycoreclass/>');
      await button(wrapper, 'Update').trigger('click');
      await flushPromises();

      const save = backend.apiRequests().find(call => call.init?.method === 'PUT');
      expect(save?.url).toBe(`${API}classifications/${CLASS_ID}`);
      expect(save?.init?.headers).toEqual({
        'Content-Type': 'application/xml; charset=utf-8',
        Authorization: 'Bearer test-token',
      });
      expect(save?.init?.body).toBe('<mycoreclass/>');
      expect(calls(method => method === 'GET')).toHaveLength(1);
      expect(wrapper.get('.alert-success').text()).toBe('Successfully saved!');
    });

    it('shows an error when the classification cannot be loaded', async () => {
      backend.on(`api/v2/classifications/${CLASS_ID}`, () => new Response('', { status: 404 }));

      const wrapper = await mountAt(`/classifications/${CLASS_ID}`);

      expect(wrapper.get('.alert-danger').text()).toContain(
        `Unable to load ${CLASS_ID}. Request failed with status code 404.`
      );
    });
  });
});
