import { flushPromises, mount, type VueWrapper } from '@vue/test-utils';

import App from '@/App.vue';
import router from '@/router';

export interface MountedApp {
  wrapper: VueWrapper;
  warnings: string[];
  errors: unknown[];
}

export async function mountAppAt(path: string): Promise<MountedApp> {
  const warnings: string[] = [];
  const errors: unknown[] = [];
  await router.push(path);
  await router.isReady();
  const wrapper = mount(App, {
    attachTo: document.body,
    global: {
      plugins: [router],
      config: {
        warnHandler: message => {
          warnings.push(message);
        },
        errorHandler: error => {
          errors.push(error);
        },
      },
    },
  });
  await flushPromises();
  return { wrapper, warnings, errors };
}
