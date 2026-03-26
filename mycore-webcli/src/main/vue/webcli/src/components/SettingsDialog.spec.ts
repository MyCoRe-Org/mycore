import { mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { nextTick, reactive } from 'vue';

import SettingsDialog from '@/components/SettingsDialog.vue';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';
import type { Settings } from '@/types';

function buildSettings(): Settings {
  return reactive({
    historySize: 500,
    comHistorySize: 10,
    suggestionLimit: 10,
    autoscroll: true,
    continueIfOneFails: false,
  });
}

describe('SettingsDialog', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('opens, focuses the first field, and restores previous focus when closed', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    wrapper = mount(SettingsDialog, {
      attachTo: document.body,
      props: {
        modelValue: false,
        hasCommandHistory: true,
        settings: buildSettings(),
        'onUpdate:modelValue': (value: boolean) => wrapper?.setProps({ modelValue: value }),
      },
    });

    await wrapper.setProps({ modelValue: true });
    await nextTick();
    await nextTick();

    expect(wrapper.get('dialog').attributes('open')).toBeDefined();
    expect(document.activeElement).toBe(wrapper.get('#webcli-history-size').element);

    await wrapper.get('.btn-close').trigger('click');
    await nextTick();

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);
    expect(document.activeElement).toBe(trigger);
  });

  it('disables clearing command history when no history is available', async () => {
    wrapper = mount(SettingsDialog, {
      attachTo: document.body,
      props: {
        modelValue: true,
        hasCommandHistory: false,
        settings: buildSettings(),
      },
    });
    await nextTick();
    await nextTick();

    expect(wrapper.get('.webcli-settings-history-actions button').attributes('disabled')).toBeDefined();
  });

  it('emits the clear-history action and updates bound setting values', async () => {
    const settings = buildSettings();
    wrapper = mount(SettingsDialog, {
      attachTo: document.body,
      props: {
        modelValue: true,
        hasCommandHistory: true,
        settings,
      },
    });
    await nextTick();
    await nextTick();

    await wrapper.get('#webcli-suggestion-limit').setValue('12');
    await wrapper.get('.webcli-settings-history-actions button').trigger('click');

    expect(settings.suggestionLimit).toBe(12);
    expect(wrapper.emitted('clear-command-history')).toEqual([[]]);
  });

  it('emits a reset-settings action', async () => {
    wrapper = mount(SettingsDialog, {
      attachTo: document.body,
      props: {
        modelValue: true,
        hasCommandHistory: true,
        settings: buildSettings(),
      },
    });
    await nextTick();
    await nextTick();

    await wrapper.get('.modal-footer button').trigger('click');

    expect(wrapper.emitted('reset-settings')).toEqual([[]]);
  });
});
