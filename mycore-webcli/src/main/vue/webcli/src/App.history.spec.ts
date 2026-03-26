import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { nextTick } from 'vue';
import type { VueWrapper } from '@vue/test-utils';

import { mountApp } from '@/test/helpers/appTestHarness';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('WebCLI app history integration', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('shows command history and lets the user restore a previous command', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a', 'process b']));
    wrapper = await mountApp();
    await nextTick();

    const historyTrigger = wrapper.findAll('button.nav-link').find(node => node.text().includes('Command History'));
    expect(historyTrigger).toBeTruthy();
    await historyTrigger!.trigger('click');
    await nextTick();

    expect(wrapper.get('dialog').attributes('open')).toBeDefined();
    const historyEntries = wrapper.findAll('.list-group-item');
    expect(historyEntries.map(entry => entry.text())).toEqual(['process b', 'process a']);

    await historyEntries[1].trigger('click');
    await nextTick();

    expect((wrapper.get('#command-input input').element as HTMLInputElement).value).toBe('process a');
  });

  it('keeps the history dialog hidden until it is opened', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a']));
    wrapper = await mountApp();

    expect(wrapper.get('dialog').attributes('open')).toBeUndefined();
  });
});
