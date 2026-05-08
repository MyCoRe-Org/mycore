import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick, ref } from 'vue';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { useDialogLifecycle } from '@/composables/useDialogLifecycle';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

const Harness = defineComponent({
  setup() {
    const modelValue = ref(false);
    const dialogRef = ref<HTMLDialogElement | null>(null);
    const focusCount = ref(0);

    useDialogLifecycle(modelValue, dialogRef, () => {
      focusCount.value += 1;
    });

    function setOpen(value: boolean): void {
      modelValue.value = value;
    }

    return {
      dialogRef,
      focusCount,
      modelValue,
      setOpen,
    };
  },
  template: '<dialog ref="dialogRef"></dialog>',
});

describe('useDialogLifecycle', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('opens the dialog, runs the open callback, and restores focus on close', async () => {
    const trigger = document.createElement('button');
    document.body.appendChild(trigger);
    trigger.focus();

    wrapper = mount(Harness, { attachTo: document.body });
    const vm = (wrapper.vm as unknown) as { focusCount: number; setOpen: (value: boolean) => void };

    vm.setOpen(true);
    await nextTick();
    await nextTick();

    expect(wrapper.get('dialog').attributes('open')).toBeDefined();
    expect(vm.focusCount).toBe(1);

    vm.setOpen(false);
    await nextTick();

    expect(wrapper.get('dialog').attributes('open')).toBeUndefined();
    expect(document.activeElement).toBe(trigger);
  });

  it('closes an open dialog during unmount', async () => {
    wrapper = mount(Harness, { attachTo: document.body });
    const vm = (wrapper.vm as unknown) as { setOpen: (value: boolean) => void };
    const dialogElement = wrapper.get('dialog').element as HTMLDialogElement;

    vm.setOpen(true);
    await nextTick();
    await nextTick();
    expect(dialogElement.hasAttribute('open')).toBe(true);

    wrapper.unmount();
    wrapper = null;

    expect(dialogElement.hasAttribute('open')).toBe(false);
  });
});
