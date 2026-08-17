import { defineMCRVueApp } from '../../../../../mycore-vue/vite.shared.ts';

export default defineMCRVueApp({
  configUrl: import.meta.url,
  name: 'jobqueue',
  resourcePath: 'jobqueue',
});
