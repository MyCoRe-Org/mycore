import type {} from 'vitest/config';

import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp(
  {
    configUrl: import.meta.url,
    name: 'jobqueue',
    resourcePath: 'jobqueue',
  },
  {
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.ts'],
      setupFiles: ['src/test/setup.ts'],
      restoreMocks: true,
      unstubGlobals: true,
    },
  }
);
