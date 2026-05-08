import { mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { nextTick } from 'vue';

import CommandHistoryDialog from '@/components/CommandHistoryDialog.vue';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('CommandHistoryDialog', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('keeps the dialog closed until modelValue is true', () => {
    wrapper = mount(CommandHistoryDialog, {
      attachTo: document.body,
      props: {
        modelValue: false,
        entries: ['process a'],
      },
    });

    expect(wrapper.get('dialog').attributes('open')).toBeUndefined();
  });

  it('opens, focuses the close button, and emits the selected command', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    wrapper = mount(CommandHistoryDialog, {
      attachTo: document.body,
      props: {
        modelValue: false,
        entries: ['process a', 'process b'],
      },
    });
    await wrapper.setProps({ modelValue: true });
    await nextTick();
    await nextTick();

    expect(wrapper.get('dialog').attributes('open')).toBeDefined();
    const historyEntries = wrapper.findAll('.list-group-item');
    expect(historyEntries.map(entry => entry.text())).toEqual(['process b', 'process a']);
    expect(document.activeElement).toBe(wrapper.get('.btn-close').element);

    await historyEntries[1].trigger('click');
    expect(wrapper.emitted('select-command')).toEqual([['process a']]);
  });

  it('emits model updates when closing and restores previous focus', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    wrapper = mount(CommandHistoryDialog, {
      attachTo: document.body,
      props: {
        modelValue: false,
        entries: ['process a'],
        'onUpdate:modelValue': (value: boolean) => wrapper?.setProps({ modelValue: value }),
      },
    });
    await wrapper.setProps({ modelValue: true });
    await nextTick();
    await nextTick();

    await wrapper.get('.btn-close').trigger('click');
    await nextTick();

    expect(wrapper.emitted('update:modelValue')).toEqual([[false]]);
    expect(document.activeElement).toBe(trigger);
  });
});
