import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp(
  {
    configUrl: import.meta.url,
    name: 'processing',
    resourcePath: 'modules/webtools/processing',
  },
  {
    build: {
      // bootstrap is provided by the surrounding MyCoRe page
      rolldownOptions: {
        external: ['bootstrap'],
      },
    },
  }
);
