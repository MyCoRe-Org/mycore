import { flushPromises } from '@vue/test-utils';
import { vi } from 'vitest';
import type { Router } from 'vue-router';

import type { AccessKey } from '@jsr/mycore__js-common/access-key';

import { FetchMock, jsonResponse } from '@/test/helpers/fetchMock';

export const APP_PATH = '/mir/access-key-manager';

const FRONTEND_KEYS = [
  'button.createAccessKey',
  'button.no',
  'button.showCreateAccessKeyModal',
  'button.updateAccessKey',
  'button.yes',
  'description.createAccesskey',
  'error.fatal',
  'label.actions',
  'label.active',
  'label.comment',
  'label.expiration',
  'label.id',
  'label.permission',
  'label.reference',
  'label.secret',
  'select',
  'success.add.title',
  'success.add.url',
  'title.createAccessKey',
  'title.main',
  'title.viewAccessKey',
];

export const ACCESS_KEY_TRANSLATIONS: Record<string, string> = {
  ...Object.fromEntries(
    FRONTEND_KEYS.map(key => [
      `component.acl.accesskey.frontend.${key}`,
      `t(${key})`,
    ])
  ),
  'component.acl.accesskey.frontend.confirmRemove.title':
    't(confirmRemove.title)',
  'component.acl.accesskey.frontend.confirmRemove.text':
    't(confirmRemove.text {secret})',
  'component.acl.accesskey.frontend.success.add': 't(success.add {secret})',
  'component.acl.accesskey.frontend.success.add.url.format':
    '{baseUrl}receive/{reference}?accesskey={secret}',
};

export const VUE_TRANSLATIONS: Record<string, string> = Object.fromEntries(
  ['general', '401', '403', '404'].flatMap(type => [
    [`component.vue.error.${type}.message`, `t(error.${type}.message)`],
    [`component.vue.error.${type}.description`, `t(error.${type}.description)`],
  ])
);

export function accessKey(
  id: string,
  overrides: Partial<AccessKey> = {}
): AccessKey {
  return {
    id,
    reference: 'mir_mods_00000001',
    type: 'read',
    isActive: true,
    ...overrides,
  } as AccessKey;
}

export function accessKeyResponse(
  keys: AccessKey[],
  total = keys.length
): Response {
  return jsonResponse(keys, { headers: { 'X-Total-Count': String(total) } });
}

export function installBackend(
  keys: AccessKey[] = [],
  total = keys.length
): FetchMock {
  return new FetchMock()
    .on('rsc/locale/translate/de/component.vue.', () =>
      jsonResponse(VUE_TRANSLATIONS)
    )
    .on('rsc/locale/translate/de/component.acl.accesskey.', () =>
      jsonResponse(ACCESS_KEY_TRANSLATIONS)
    )
    .on('rsc/jwt', () =>
      jsonResponse({ login_success: true, access_token: 'test-token' })
    )
    .on('api/v2/access-keys', () => accessKeyResponse(keys, total));
}

export interface StartedApp {
  router: Router;
  container: HTMLElement;
  vueWarnings: () => string[];
}

export async function startApp(path: string): Promise<StartedApp> {
  window.history.replaceState(null, '', APP_PATH + path);
  document.body.innerHTML = '<div id="app"></div>';
  const warn = vi.spyOn(console, 'warn');
  vi.resetModules();
  await import('@/main');
  const { default: router } = await import('@/router');
  await settle();
  if (document.querySelector('#app[data-v-app]') !== null) {
    await router.isReady();
    await settle();
  }
  return {
    router,
    container: document.getElementById('app') as HTMLElement,
    vueWarnings: () =>
      warn.mock.calls
        .map(args => args.map(String).join(' '))
        .filter(message => /\[(Vue|intlify)/.test(message)),
  };
}

export async function settle(): Promise<void> {
  for (let i = 0; i < 5; i++) {
    await flushPromises();
  }
}

export function query<T extends Element = HTMLElement>(selector: string): T {
  const element = document.querySelector<T>(selector);
  if (element === null) {
    throw new Error(`no element matches ${selector}`);
  }
  return element;
}

export function queryAll<T extends Element = HTMLElement>(
  selector: string
): T[] {
  return [...document.querySelectorAll<T>(selector)];
}

export function texts(selector: string): string[] {
  return queryAll(selector).map(element => element.textContent?.trim() ?? '');
}

export async function click(selector: string | Element): Promise<void> {
  const element = typeof selector === 'string' ? query(selector) : selector;
  (element as HTMLElement).click();
  await settle();
}

export async function wait(milliseconds = 20): Promise<void> {
  await new Promise(resolve => setTimeout(resolve, milliseconds));
  await settle();
}

export function field<T extends HTMLElement = HTMLInputElement>(
  modalId: string,
  labelKey: string
): T {
  const label = queryAll(`#${modalId} label`).find(
    candidate => candidate.textContent?.trim() === `t(${labelKey})`
  );
  const id = label?.getAttribute('for');
  if (!id) {
    throw new Error(`no field labeled ${labelKey} in #${modalId}`);
  }
  return document.getElementById(id) as T;
}

export async function input(
  target: string | HTMLElement,
  value: string
): Promise<void> {
  const element = (
    typeof target === 'string' ? query(target) : target
  ) as HTMLInputElement;
  element.value = value;
  element.dispatchEvent(new Event('input'));
  await settle();
}

export function modalEvent(
  id: string,
  type: 'shown' | 'hidden'
): Promise<void> {
  return new Promise(resolve => {
    const listener = (event: Event): void => {
      if ((event.target as Element | null)?.id === id) {
        document.removeEventListener(`${type}.bs.modal`, listener);
        resolve();
      }
    };
    document.addEventListener(`${type}.bs.modal`, listener);
  });
}
