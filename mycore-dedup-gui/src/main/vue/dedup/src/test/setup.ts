import { vi } from 'vitest';

import { installBootstrapMock } from '@/test/helpers/bootstrapMock';

export const WEB_APPLICATION_BASE_URL = 'http://localhost:3000/mir/';

vi.stubEnv('DEV', false);
vi.stubEnv('PROD', true);

const globals = window as unknown as Record<string, unknown>;
globals.mycore = { webApplicationBaseURL: WEB_APPLICATION_BASE_URL, currentLang: 'de' };

installBootstrapMock();
