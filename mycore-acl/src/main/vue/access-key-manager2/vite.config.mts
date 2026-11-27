import type {} from 'vitest/config';

import {
  bootstrapFromWindow,
  defineMCRVueApp,
} from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp(
  {
    configUrl: import.meta.url,
    name: 'access-key-manager',
    resourcePath: 'access-key-manager',
  },
  {
    // @mycore-org/vue-components imports bootstrap at runtime, the surrounding MyCoRe page provides it
    plugins: [bootstrapFromWindow()],
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.ts'],
      setupFiles: ['src/test/setup.ts'],
      restoreMocks: true,
      server: {
        deps: {
          inline: [
            '@mycore-org/vue-access-key-manager',
            '@mycore-org/vue-components',
          ],
        },
      },
    },
  }
);
