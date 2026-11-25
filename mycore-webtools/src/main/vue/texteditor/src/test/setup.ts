import { vi } from 'vitest';

export const WEB_APPLICATION_BASE_URL = 'http://localhost:3000/mir/';

vi.stubEnv('DEV', false);
vi.stubEnv('PROD', true);

(window as unknown as Record<string, unknown>).mycore = { webApplicationBaseURL: WEB_APPLICATION_BASE_URL };
