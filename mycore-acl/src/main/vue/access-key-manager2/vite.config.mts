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
  }
);
