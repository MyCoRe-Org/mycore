import { vi } from 'vitest';

export const WEB_APPLICATION_BASE_URL = 'http://localhost:3000/mir/';

export const SESSION_PERMISSION_TYPES = 'read,writedb';

vi.stubEnv('DEV', false);
vi.stubEnv('PROD', true);

window.mycore = {
  webApplicationBaseURL: WEB_APPLICATION_BASE_URL,
  currentLang: 'de',
  'MCR.ACL.AccessKey.Strategy.AllowedSessionPermissionTypes':
    SESSION_PERMISSION_TYPES,
};
