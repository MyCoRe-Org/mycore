import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp({
  configUrl: import.meta.url,
  name: 'texteditor',
  resourcePath: 'modules/webtools/texteditor',
});
