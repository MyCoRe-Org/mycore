import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';
import type { VueWrapper } from '@vue/test-utils';

import { mountApp, getCurrentTransport } from '@/test/helpers/appTestHarness';
import { makeCommandGroups } from '@/test/helpers/commandFixtures';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('WebCLI app command input', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('loads commands on startup and can select and execute a command', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    expect(transport.connect).toHaveBeenCalledTimes(1);
    expect(transport.getKnownCommands).toHaveBeenCalledTimes(1);
    expect(transport.startLog).toHaveBeenCalledTimes(1);
    expect(transport.setContinueIfOneFails).toHaveBeenCalledWith(false);

    transport.emit({ type: 'commandList', value: makeCommandGroups() });
    await nextTick();

    await wrapper.get('.webcli-command-menu .dropdown-toggle').trigger('click');
    await nextTick();
    const commandLink = wrapper.findAll('.dropdown-item').find(item => item.text() === 'process resource {0}');
    expect(commandLink).toBeTruthy();

    await commandLink!.trigger('click');
    await nextTick();
    await nextTick();

    const input = wrapper.get('#command-input input').element as HTMLInputElement;
    expect(input.value).toBe('process resource {0}');
    expect(input.selectionStart).toBe('process resource {0}'.indexOf('{0}'));

    await wrapper.get('#command-input button').trigger('click');

    expect(transport.run).toHaveBeenCalledWith('process resource {0}');
    expect(window.localStorage.getItem('commandHistory')).toBe(JSON.stringify(['process resource {0}']));
  });

  it('focuses the execute button after selecting a command without placeholders', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({ type: 'commandList', value: makeCommandGroups() });
    await nextTick();

    await wrapper.get('.webcli-command-menu .dropdown-toggle').trigger('click');
    await nextTick();

    const commandLink = wrapper.findAll('.dropdown-item').find(item => item.text() === 'skip on error');
    expect(commandLink).toBeTruthy();

    await commandLink!.trigger('click');
    await nextTick();

    const executeButton = wrapper.get('#command-input button').element as HTMLButtonElement;
    expect(document.activeElement).toBe(executeButton);
  });

  it('shows substring suggestions with help text and selects one with Enter', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({
      type: 'commandList',
      value: [
        {
          name: 'Transformations',
          commands: [
            { command: 'xslt transform {0}', help: 'Run an XSLT transformation.' },
            { command: 'import object', help: 'Import a resource with optional xslt mapping.' },
          ],
        },
      ],
    });
    await nextTick();

    const input = wrapper.get('#webcli-command-input');
    await input.setValue('xslt');
    await nextTick();

    const suggestions = wrapper.findAll('.webcli-suggestion');
    expect(suggestions).toHaveLength(2);
    expect(suggestions[0].text()).toContain('xslt transform {0}');
    expect(suggestions[0].text()).toContain('Run an XSLT transformation.');
    expect(suggestions[0].text()).toContain('Transformations');

    await input.trigger('keydown', { key: 'Enter', preventDefault: vi.fn() });
    await nextTick();

    expect((input.element as HTMLInputElement).value).toBe('xslt transform {0}');
  });

  it('accepts the highlighted suggestion with Tab before placeholder jumping starts', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({
      type: 'commandList',
      value: [
        {
          name: 'Transformations',
          commands: [{ command: 'xslt transform {0}', help: 'Run an XSLT transformation.' }],
        },
      ],
    });
    await nextTick();

    const input = wrapper.get('#webcli-command-input');
    await input.setValue('xslt');
    await nextTick();

    const preventDefault = vi.fn();
    await input.trigger('keydown', { key: 'Tab', preventDefault });
    await nextTick();

    expect(preventDefault).toHaveBeenCalledTimes(1);
    expect((input.element as HTMLInputElement).value).toBe('xslt transform {0}');
    expect((input.element as HTMLInputElement).selectionStart).toBe('xslt transform {0}'.indexOf('{0}'));
  });

  it('scrolls the highlighted suggestion into view during keyboard navigation', async () => {
    wrapper = await mountApp();
    const transport = getCurrentTransport();
    const scrollIntoView = vi.fn();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
      configurable: true,
      value: scrollIntoView,
    });

    transport.emit({
      type: 'commandList',
      value: [
        {
          name: 'Transformations',
          commands: Array.from({ length: 10 }, (_, index) => ({
            command: `xslt transform ${index}`,
            help: `Run XSLT transformation ${index}.`,
          })),
        },
      ],
    });
    await nextTick();

    const input = wrapper.get('#webcli-command-input');
    await input.setValue('xslt');
    await nextTick();

    await input.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    expect(scrollIntoView).toHaveBeenCalled();
  });

  it('moves through placeholders with Tab and then lets focus continue normally', async () => {
    wrapper = await mountApp();
    const input = wrapper.get('#command-input input');
    await input.setValue('command {0} and {1}');

    const element = input.element as HTMLInputElement;
    element.selectionEnd = 0;

    const firstPreventDefault = vi.fn();
    await input.trigger('keydown', { key: 'Tab', preventDefault: firstPreventDefault });
    expect(element.selectionStart).toBe(8);
    expect(element.selectionEnd).toBe(11);
    expect(firstPreventDefault).toHaveBeenCalledTimes(1);

    element.selectionEnd = 11;
    const secondPreventDefault = vi.fn();
    await input.trigger('keydown', { key: 'Tab', preventDefault: secondPreventDefault });
    expect(element.selectionStart).toBe(16);
    expect(element.selectionEnd).toBe(19);
    expect(secondPreventDefault).toHaveBeenCalledTimes(1);

    element.selectionEnd = 19;
    const thirdPreventDefault = vi.fn();
    await input.trigger('keydown', { key: 'Tab', preventDefault: thirdPreventDefault });
    expect(thirdPreventDefault).not.toHaveBeenCalled();
    expect(element.selectionStart).toBe(16);
    expect(element.selectionEnd).toBe(19);
  });

  it('uses semantic buttons and labels for the primary controls', async () => {
    wrapper = await mountApp();

    expect(wrapper.get('label[for="webcli-command-input"]').text()).toContain('Command input');
    expect(wrapper.get('button[aria-controls="webcli-settings-panel"]').attributes('aria-expanded')).toBe('false');
    expect(wrapper.get('input#webcli-history-size').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-history-size').attributes('min')).toBe('1');
    expect(wrapper.get('input#webcli-command-history-size').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-command-history-size').attributes('min')).toBe('0');
    expect(wrapper.get('input#webcli-command-input').attributes('role')).toBe('combobox');
  });
});
