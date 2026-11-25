import type {} from 'vitest/config';

import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp(
  {
    configUrl: import.meta.url,
    name: 'texteditor',
    resourcePath: 'modules/webtools/texteditor',
  },
  {
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.ts'],
      setupFiles: ['src/test/setup.ts'],
      restoreMocks: true,
      css: false,
    },
  }
);
