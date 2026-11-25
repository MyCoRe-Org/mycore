import { afterEach, describe, expect, it, vi } from 'vitest';

import { FetchMock, jsonResponse } from '@/test/helpers/fetchMock';

const globals = window as unknown as Record<string, unknown>;

async function importInMode(mode: string) {
  vi.resetModules();
  vi.stubEnv('MODE', mode);
  return {
    auth: await import('@/api/Auth'),
    baseUrl: await import('@/api/BaseURL'),
    i18n: await import('@/api/I18N'),
  };
}

describe('api helpers', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    globals.currentLang = 'de';
  });

  it('uses the web application base url of the page', async () => {
    const { baseUrl } = await importInMode('production');

    expect(baseUrl.getBaseURL()).toBe('http://localhost:3000/mir/');
  });

  it('uses the local development server in development mode', async () => {
    const { baseUrl, auth, i18n } = await importInMode('development');

    expect(baseUrl.getBaseURL()).toBe('http://localhost:8291/mir/');
    expect(await auth.getAuthorizationHeader()).toBe(`Basic ${btoa('administrator:alleswirdgut')}`);
    expect(i18n.getLanguage()).toBe('de');
  });

  it('builds the authorization header from the jwt of the session', async () => {
    const fetchMock = new FetchMock();
    const { auth } = await importInMode('production');

    expect(await auth.getAuthorizationHeader()).toBe('Bearer test-token');
    expect(fetchMock.calls.map(call => call.url)).toEqual(['http://localhost:3000/mir/rsc/jwt']);
  });

  it('fails when the jwt login is not successful', async () => {
    new FetchMock().on('rsc/jwt', () => jsonResponse({ login_success: false }));
    const { auth } = await importInMode('production');

    await expect(auth.getAuthorizationHeader()).rejects.toThrow('Login failed');
  });

  it('translates keys in the current language of the page', async () => {
    const fetchMock = new FetchMock().on('rsc/locale/translate/en/', () => new Response('Job queue'));
    globals.currentLang = 'en';
    const { i18n } = await importInMode('production');

    expect(i18n.getLanguage()).toBe('en');
    expect(await i18n.i18n('jobqueue.title')).toBe('Job queue');
    expect(fetchMock.calls.map(call => call.url)).toEqual([
      'http://localhost:3000/mir/rsc/locale/translate/en/jobqueue.title',
    ]);
  });

  it('falls back to german when the page has no language', async () => {
    globals.currentLang = undefined;
    const { i18n } = await importInMode('production');

    expect(i18n.getLanguage()).toBe('de');
  });
});
