import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { useSettingsState } from '@/composables/useSettingsState';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';
import type { Settings } from '@/types';

const Harness = defineComponent({
  setup() {
    return useSettingsState();
  },
  template: '<div />',
});

describe('useSettingsState', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('loads settings from local storage on initialization', () => {
    window.localStorage.setItem('historySize', '42');
    window.localStorage.setItem('comHistorySize', '7');
    window.localStorage.setItem('suggestionLimit', '12');
    window.localStorage.setItem('autoScroll', 'false');
    window.localStorage.setItem('continueIfOneFails', 'true');

    wrapper = mount(Harness);

    expect(((wrapper.vm as unknown) as { settings: Settings }).settings).toEqual({
      historySize: 42,
      comHistorySize: 7,
      suggestionLimit: 12,
      autoscroll: false,
      continueIfOneFails: true,
    });
  });

  it('normalizes invalid settings updates and persists the normalized values', async () => {
    wrapper = mount(Harness);
    const vm = (wrapper.vm as unknown) as { settings: Settings };

    vm.settings.historySize = Number.NaN;
    vm.settings.comHistorySize = -2;
    vm.settings.suggestionLimit = 0;
    vm.settings.autoscroll = false;
    await nextTick();
    await nextTick();

    expect(vm.settings.historySize).toBe(500);
    expect(vm.settings.comHistorySize).toBe(0);
    expect(vm.settings.suggestionLimit).toBe(1);
    expect(window.localStorage.getItem('historySize')).toBe('500');
    expect(window.localStorage.getItem('comHistorySize')).toBe('0');
    expect(window.localStorage.getItem('suggestionLimit')).toBe('1');
    expect(window.localStorage.getItem('autoScroll')).toBe('false');
  });

  it('resets settings to defaults and clears persisted storage until the next edit', async () => {
    window.localStorage.setItem('historySize', '42');
    window.localStorage.setItem('comHistorySize', '7');
    window.localStorage.setItem('suggestionLimit', '12');
    window.localStorage.setItem('autoScroll', 'false');
    window.localStorage.setItem('continueIfOneFails', 'true');

    wrapper = mount(Harness);
    const vm = (wrapper.vm as unknown) as { resetSettings: () => void; settings: Settings };

    vm.resetSettings();
    await nextTick();
    await nextTick();

    expect(vm.settings).toEqual({
      historySize: 500,
      comHistorySize: 10,
      suggestionLimit: 10,
      autoscroll: true,
      continueIfOneFails: false,
    });
    expect(window.localStorage.getItem('historySize')).toBeNull();
    expect(window.localStorage.getItem('comHistorySize')).toBeNull();
    expect(window.localStorage.getItem('suggestionLimit')).toBeNull();
    expect(window.localStorage.getItem('autoScroll')).toBeNull();
    expect(window.localStorage.getItem('continueIfOneFails')).toBeNull();

    vm.settings.historySize = 250;
    await nextTick();
    await nextTick();

    expect(window.localStorage.getItem('historySize')).toBe('250');
    expect(window.localStorage.getItem('comHistorySize')).toBe('10');
    expect(window.localStorage.getItem('suggestionLimit')).toBe('10');
    expect(window.localStorage.getItem('autoScroll')).toBe('true');
    expect(window.localStorage.getItem('continueIfOneFails')).toBe('false');
  });
});
