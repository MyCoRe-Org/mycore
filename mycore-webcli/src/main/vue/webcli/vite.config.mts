import type {} from 'vitest/config';

import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp(
  {
    configUrl: import.meta.url,
    name: 'webcli',
    resourcePath: 'modules/webcli/gui',
  },
  {
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.ts'],
      exclude: ['tests/a11y/**'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        reportsDirectory: '../../../../target/vitest-coverage',
        exclude: ['src/test/helpers/**'],
        thresholds: {
          lines: 83,
          statements: 83,
          functions: 85,
          branches: 75,
        },
      },
    },
  }
);
