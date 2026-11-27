import { afterEach, describe, expect, it, vi } from 'vitest';

async function importProvider() {
  vi.resetModules();
  return await import('@/config/provider');
}

describe('config provider', () => {
  afterEach(() => {
    vi.stubEnv('DEV', false);
  });

  it('reads the configuration of the page', async () => {
    const { appConfig, accessKeyConfig, I18N_PREFIX } = await importProvider();

    expect(appConfig).toEqual({
      baseUrl: 'http://localhost:3000/mir/',
      currentLang: 'de',
    });
    expect(accessKeyConfig).toEqual({
      allowedAccessKeySessionPermissions: ['read', 'writedb'],
    });
    expect(I18N_PREFIX).toBe('component.acl.accesskey.*');
  });

  it('reads the configuration from the environment in development mode', async () => {
    vi.stubEnv('DEV', true);
    vi.stubEnv(
      'VITE_APP_CONFIG_webApplicationBaseURL',
      'http://localhost:8291/mir/'
    );
    vi.stubEnv('VITE_APP_CONFIG_currentLang', 'en');
    vi.stubEnv(
      'VITE_APP_CONFIG_MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes',
      'read'
    );

    const { appConfig, accessKeyConfig } = await importProvider();

    expect(appConfig).toEqual({
      baseUrl: 'http://localhost:8291/mir/',
      currentLang: 'en',
    });
    expect(accessKeyConfig).toEqual({
      allowedAccessKeySessionPermissions: ['read'],
    });
  });
});
