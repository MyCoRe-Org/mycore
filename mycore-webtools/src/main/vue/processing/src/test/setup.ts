import { vi } from 'vitest';

import { installBootstrapMock } from './helpers/bootstrapMock';

vi.stubEnv('DEV', false);
vi.stubEnv('PROD', true);

(globalThis as unknown as Record<string, unknown>).mycore = {
  webApplicationBaseURL: 'http://localhost:3000/mir/',
};

installBootstrapMock();
