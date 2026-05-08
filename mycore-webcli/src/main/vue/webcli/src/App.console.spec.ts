import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { nextTick } from 'vue';
import type { VueWrapper } from '@vue/test-utils';

import type { LogEntry } from '@/types';
import { mountApp, getCurrentTransport } from '@/test/helpers/appTestHarness';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('WebCLI app console and settings', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('renders and trims logs and can clear them', async () => {
    window.localStorage.setItem('historySize', '2');
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    const emitLog = (message: string, time: number, exception: string | null = null) => {
      transport.emit({
        type: 'log',
        value: {
          logLevel: 'INFO',
          message,
          exception,
          time,
        } satisfies LogEntry,
      });
    };

    emitLog('first', 1);
    emitLog('second', 2);
    emitLog('third', 3);
    await nextTick();

    let lines = wrapper.findAll('.web-cli-log pre').map(node => node.text());
    expect(lines).toEqual(['INFO: second', 'INFO: third']);

    await wrapper.findAll('button.nav-link').find(node => node.text().includes('Clear Logs'))!.trigger('click');
    await nextTick();

    lines = wrapper.findAll('.web-cli-log pre').map(node => node.text());
    expect(lines).toEqual([]);

    transport.emit({
      type: 'log',
      value: {
        logLevel: 'ERROR',
        message: 'failed',
        exception: 'boom',
        time: 4,
      },
    });
    await nextTick();

    lines = wrapper.findAll('.web-cli-log pre').map(node => node.text());
    expect(lines).toEqual(['ERROR: failed', 'boom']);
  });

  it('renders the queue and clears it through the transport', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({
      type: 'queue',
      value: ['import object 1', 'import object 2'],
      size: 2,
    });
    await nextTick();

    const queueTab = wrapper.findAll('.queueTab').at(0);
    expect(queueTab?.text()).toContain('Command Queue (2)');
    await queueTab!.trigger('click');
    await nextTick();

    expect(wrapper.get('.web-cli-pre').text()).toContain('import object 1');
    expect(wrapper.get('.web-cli-pre').text()).toContain('import object 2');

    await wrapper.findAll('button.nav-link').find(node => node.text().includes('Clear Command Queue'))!.trigger('click');
    expect(transport.clearCommandList).toHaveBeenCalledTimes(1);

    transport.emit({ type: 'queue', value: [], size: 0 });
    await nextTick();
    expect(wrapper.findAll('.queueTab')).toHaveLength(0);
  });

  it('toggles refresh controls and calls the matching transport methods', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    const stopRefresh = wrapper.findAll('button.nav-link').find(node => node.text().includes('Stop Refresh'));
    expect(stopRefresh).toBeTruthy();
    await stopRefresh!.trigger('click');
    expect(transport.stopLog).toHaveBeenCalledTimes(1);

    await nextTick();
    const startRefresh = wrapper.findAll('button.nav-link').find(node => node.text().includes('Refresh'));
    expect(startRefresh).toBeTruthy();
    await startRefresh!.trigger('click');
    expect(transport.startLog).toHaveBeenCalledTimes(2);
  });

  it('persists the continue-if-one-fails setting and updates the transport', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    await wrapper.findAll('button.nav-link').find(node => node.text().includes('Settings'))!.trigger('click');
    await nextTick();

    expect(wrapper.get('dialog[aria-labelledby="webcli-settings-title"]').attributes('open')).toBeDefined();

    const continueIfOneFails = wrapper.find('#webcli-continue-on-fail');
    expect(continueIfOneFails).toBeTruthy();
    await continueIfOneFails.setValue(true);

    expect(window.localStorage.getItem('continueIfOneFails')).toBe('true');
    expect(transport.setContinueIfOneFails).toHaveBeenLastCalledWith(true);
  });

  it('persists the configurable suggestion limit', async () => {
    wrapper = await mountApp();

    await wrapper.findAll('button.nav-link').find(node => node.text().includes('Settings'))!.trigger('click');
    await nextTick();

    await wrapper.get('#webcli-suggestion-limit').setValue('12');

    expect(window.localStorage.getItem('suggestionLimit')).toBe('12');
  });

  it('offers queue expansion when more than ninety-nine commands are present', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({
      type: 'queue',
      value: Array.from({ length: 120 }, (_, index) => `import object ${index + 1}`),
      size: 120,
    });
    await nextTick();

    await wrapper.get('.queueTab').trigger('click');
    await nextTick();

    expect(wrapper.get('.webcli-queue-toolbar button').text()).toContain('Show all 120 commands');
    expect(wrapper.get('.small.text-secondary').text()).toContain('21 more commands hidden');
  });
});
