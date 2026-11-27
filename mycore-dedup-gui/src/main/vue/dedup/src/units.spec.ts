import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { nextTick, reactive } from 'vue';

import Modal from '@/components/Modal.vue';
import SortArrow from '@/components/SortArrow.vue';
import { ModalMock } from '@/test/helpers/bootstrapMock';
import { FetchMock, jsonResponse } from '@/test/helpers/fetchMock';

const globals = window as unknown as Record<string, unknown>;

async function importFresh() {
  vi.resetModules();
  return {
    auth: await import('@/api/Auth'),
    baseUrl: await import('@/api/BaseURL'),
    i18n: await import('@/api/I18N'),
    utils: await import('@/api/Utils'),
  };
}

describe('SortArrow', () => {
  it.each([
    ['asc', 'fa-sort-up'],
    ['desc', 'fa-sort-down'],
    ['none', 'fa-sort'],
  ] as const)('shows the %s icon', (direction, icon) => {
    const wrapper = mount(SortArrow, { props: { direction } });

    expect(wrapper.get('span').classes()).toEqual(['fas', icon]);
  });
});

describe('Modal', () => {
  it('wraps a bootstrap modal and forwards its events', async () => {
    const wrapper = mount(Modal, {
      slots: { title: 'The title', default: 'The body', footer: '<button>ok</button>' },
    });
    const modal = ModalMock.forElement(wrapper.get('.modal').element);

    expect(wrapper.get('.modal-title').text()).toBe('The title');
    expect(wrapper.get('.modal-body').text()).toBe('The body');
    expect(wrapper.get('.modal-footer button').text()).toBe('ok');

    (wrapper.vm as unknown as { show: () => void }).show();
    expect(modal.show).toHaveBeenCalled();
    expect(wrapper.emitted('show')).toHaveLength(1);

    (wrapper.vm as unknown as { hide: () => void }).hide();
    expect(modal.hide).toHaveBeenCalled();
    expect(wrapper.emitted('hide')).toHaveLength(1);
  });
});

describe('api', () => {
  afterEach(() => {
    vi.stubEnv('DEV', false);
    globals.mycore = { webApplicationBaseURL: 'http://localhost:3000/mir/', currentLang: 'de' };
  });

  it('reads base url and language from the page', async () => {
    globals.mycore = { webApplicationBaseURL: 'https://example.org/repo/', currentLang: 'en' };
    const { baseUrl, utils } = await importFresh();

    expect(baseUrl.getWebApplicationBaseURL()).toBe('https://example.org/repo/');
    expect(baseUrl.getCurrentLanguage()).toBe('en');
    expect(utils.getObjectURL('mir_mods_00000001')).toBe('https://example.org/repo/receive/mir_mods_00000001');
  });

  it('fails without base url and falls back to german without language', async () => {
    globals.mycore = undefined;
    const { baseUrl } = await importFresh();

    expect(() => baseUrl.getWebApplicationBaseURL()).toThrow("'mycore.webApplicationBaseURL' is not set");
    expect(baseUrl.getCurrentLanguage()).toBe('de');
  });

  it('uses the development server and basic authentication in development mode', async () => {
    vi.stubEnv('DEV', true);
    const { baseUrl, auth } = await importFresh();

    expect(baseUrl.getWebApplicationBaseURL()).toBe('http://localhost:8291/mir/');
    expect(baseUrl.getCurrentLanguage()).toBe('de');
    expect(await auth.getAuthorizationHeader()).toBe(`Basic ${btoa('administrator:alleswirdgut')}`);
  });

  it('fails when no jwt can be obtained', async () => {
    new FetchMock().on('rsc/jwt', () => jsonResponse({ login_success: false }));
    const { auth } = await importFresh();

    await expect(auth.getAuthorizationHeader()).rejects.toThrow('Could not obtain a JWT');
  });

  describe('resolveI18N', () => {
    it('fills the object with translations of the current language', async () => {
      const fetchMock = new FetchMock().on('rsc/locale/translate/de/a', () => new Response('A'));
      const { i18n } = await importFresh();
      const texts = reactive<Record<string, string>>({ a: 'a' });

      i18n.resolveI18N(texts);
      expect(texts.a).toBe('a');
      await flushPromises();

      expect(texts.a).toBe('A');
      expect(fetchMock.translatedKeys()).toEqual(['a']);
    });

    it('keeps the key when the translation cannot be loaded', async () => {
      new FetchMock().on('rsc/locale/translate/', () => new Response('', { status: 404 }));
      const { i18n } = await importFresh();
      const texts = reactive<Record<string, string>>({ missing: 'x' });

      i18n.resolveI18N(texts);
      await flushPromises();

      expect(texts.missing).toBe('missing');
    });

    it('requests each key only once for all components', async () => {
      let resolveTranslation: (response: Response) => void = () => {};
      const fetchMock = new FetchMock().on(
        'rsc/locale/translate/',
        () => new Promise<Response>(resolve => (resolveTranslation = resolve))
      );
      const { i18n } = await importFresh();
      const first = reactive<Record<string, string>>({ key: 'key' });
      const pending = reactive<Record<string, string>>({ key: 'key' });

      i18n.resolveI18N(first);
      i18n.resolveI18N(pending);
      resolveTranslation(new Response('Translated'));
      await flushPromises();
      const later = reactive<Record<string, string>>({ key: 'key' });
      i18n.resolveI18N(later);
      await nextTick();

      expect([first.key, pending.key, later.key]).toEqual(['Translated', 'Translated', 'Translated']);
      expect(fetchMock.translatedKeys()).toEqual(['key']);
    });
  });
});
