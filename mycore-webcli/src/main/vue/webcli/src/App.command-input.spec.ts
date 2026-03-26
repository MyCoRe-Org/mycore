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

  it('focuses the input and places the caret at the end after selecting a command without placeholders', async () => {
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
    await nextTick();

    const input = wrapper.get('#command-input input').element as HTMLInputElement;
    expect(document.activeElement).toBe(input);
    expect(input.selectionStart).toBe('skip on error'.length);
    expect(input.selectionEnd).toBe('skip on error'.length);
    expect(wrapper.find('.webcli-suggestion').exists()).toBe(false);
  });

  it('shows substring suggestions with help text and still executes the typed command on Enter', async () => {
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
    expect(suggestions[0].classes()).not.toContain('is-highlighted');
    expect(input.attributes('aria-activedescendant')).toBeUndefined();

    await input.trigger('keydown', { key: 'Enter', preventDefault: vi.fn() });
    await nextTick();

    expect(transport.run).toHaveBeenCalledWith('xslt');
  });

  it('shows when suggestions are capped by the configured limit', async () => {
    window.localStorage.setItem('suggestionLimit', '2');
    wrapper = await mountApp();
    const transport = getCurrentTransport();

    transport.emit({
      type: 'commandList',
      value: [
        {
          name: 'Transformations',
          commands: Array.from({ length: 4 }, (_, index) => ({
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

    expect(wrapper.findAll('.webcli-suggestion')).toHaveLength(2);
    expect(wrapper.get('.webcli-suggestions-meta').text()).toContain('Showing top 2 of 4 matches');
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

  it('accepts a highlighted suggestion with Enter after arrow-key navigation starts', async () => {
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

    await input.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    expect(wrapper.findAll('.webcli-suggestion')[0].classes()).toContain('is-highlighted');
    await input.trigger('keydown', { key: 'Enter', preventDefault: vi.fn() });
    await nextTick();
    await nextTick();

    expect((input.element as HTMLInputElement).value).toBe('xslt transform {0}');
    expect(document.activeElement).toBe(input.element);
    expect(wrapper.find('.webcli-suggestion').exists()).toBe(false);
  });

  it('scrolls the highlighted suggestion into view during arrow-key navigation', async () => {
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
    await input.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    expect(scrollIntoView).toHaveBeenCalled();
  });

  it('uses Arrow keys for suggestion navigation while the popup is open', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a', 'process b']));
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

    await input.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await input.trigger('keydown', { key: 'Enter', preventDefault: vi.fn() });
    await nextTick();

    expect((input.element as HTMLInputElement).value).toBe('xslt transform {0}');
  });

  it('uses history browsing again after Escape closes the suggestions', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a', 'process b']));
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

    await input.trigger('keydown', { key: 'Escape', preventDefault: vi.fn() });
    await nextTick();
    await input.trigger('keydown', { key: 'ArrowUp' });
    await nextTick();

    expect((input.element as HTMLInputElement).value).toBe('process b');
  });

  it('keeps focus in the input when Escape closes the suggestions', async () => {
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
    (input.element as HTMLInputElement).focus();
    (input.element as HTMLInputElement).setSelectionRange(4, 4);

    await input.trigger('keydown', { key: 'Escape', preventDefault: vi.fn() });
    await nextTick();
    await nextTick();

    expect(document.activeElement).toBe(input.element);
    expect((input.element as HTMLInputElement).selectionStart).toBe(4);
    expect((input.element as HTMLInputElement).selectionEnd).toBe(4);
  });

  it('applies a suggestion reliably on pointer interaction before blur closes the popup', async () => {
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

    await wrapper.get('.webcli-suggestion').trigger('pointerdown');
    await nextTick();
    await nextTick();

    expect((input.element as HTMLInputElement).value).toBe('xslt transform {0}');
    expect(document.activeElement).toBe(input.element);
    expect((input.element as HTMLInputElement).selectionStart).toBe('xslt transform {0}'.indexOf('{0}'));
    expect(wrapper.find('.webcli-suggestion').exists()).toBe(false);
  });

  it('keeps suggestions hidden after selecting a command from the menu until the input changes', async () => {
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
    await nextTick();

    const input = wrapper.get('#webcli-command-input');
    await input.trigger('focus');
    await nextTick();

    expect(wrapper.find('.webcli-suggestion').exists()).toBe(false);

    await input.setValue('error');
    await nextTick();

    expect(wrapper.find('.webcli-suggestion').exists()).toBe(true);
  });

  it('keeps suggestions hidden after selecting a suggestion until the input changes', async () => {
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

    await wrapper.get('.webcli-suggestion').trigger('pointerdown');
    await nextTick();
    await nextTick();

    await input.trigger('focus');
    await nextTick();

    expect(wrapper.find('.webcli-suggestion').exists()).toBe(false);

    await input.setValue('xslt');
    await nextTick();

    expect(wrapper.find('.webcli-suggestion').exists()).toBe(true);
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
    expect(wrapper.get('button[aria-haspopup="dialog"]').attributes('aria-expanded')).toBe('false');
    expect(wrapper.get('input#webcli-history-size').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-history-size').attributes('min')).toBe('1');
    expect(wrapper.get('input#webcli-command-history-size').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-command-history-size').attributes('min')).toBe('0');
    expect(wrapper.get('input#webcli-suggestion-limit').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-suggestion-limit').attributes('min')).toBe('1');
    expect(wrapper.get('input#webcli-command-input').attributes('role')).toBe('combobox');
  });
});
