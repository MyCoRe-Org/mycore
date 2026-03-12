import { mount, type VueWrapper } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { nextTick } from 'vue';

import type { CommandGroup, LogEntry, TransportEvent } from '@/types';

type Handler = (event: TransportEvent) => void;

class MockTransport {
  handlers: Handler[] = [];

  getKnownCommands = vi.fn();

  run = vi.fn();

  startLog = vi.fn();

  stopLog = vi.fn();

  clearCommandList = vi.fn();

  setContinueIfOneFails = vi.fn();

  connect = vi.fn();

  subscribe(listener: Handler): () => void {
    this.handlers.push(listener);
    return () => {
      this.handlers = this.handlers.filter(entry => entry !== listener);
    };
  }

  emit(event: TransportEvent): void {
    this.handlers.forEach(handler => handler(event));
  }
}

let currentTransport: MockTransport;
let appWrapper: VueWrapper | null = null;
const localStorageState = new Map<string, string>();

function installLocalStorageMock(): void {
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: {
      getItem: (key: string) => localStorageState.get(key) ?? null,
      setItem: (key: string, value: string) => {
        localStorageState.set(key, String(value));
      },
      removeItem: (key: string) => {
        localStorageState.delete(key);
      },
      clear: () => {
        localStorageState.clear();
      },
    } satisfies Storage,
  });
}

function installDialogMock(): void {
  if (typeof HTMLDialogElement === 'undefined') {
    return;
  }
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value() {
      this.setAttribute('open', '');
    },
  });
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value() {
      this.removeAttribute('open');
    },
  });
}

vi.mock('@/services/webcliTransport', async () => {
  const actual = await vi.importActual<typeof import('@/services/webcliTransport')>('@/services/webcliTransport');
  return {
    ...actual,
    WebCliTransport: vi.fn(() => {
      currentTransport = new MockTransport();
      return currentTransport;
    }),
  };
});

import App from '@/App.vue';

function makeCommandGroups(): CommandGroup[] {
  return [
    {
      name: 'Basic commands',
      commands: [
        {
          command: 'process resource {0}',
          help: 'Execute the commands listed in the resource file {0}.',
        },
        {
          command: 'skip on error',
          help: 'Skip execution of failed command in case of error',
        },
      ],
    },
  ];
}

async function mountApp() {
  appWrapper = mount(App, {
    attachTo: document.body,
  });
  await nextTick();
  return appWrapper;
}

describe('WebCLI app', () => {
  beforeEach(() => {
    installLocalStorageMock();
    installDialogMock();
    vi.restoreAllMocks();
    vi.clearAllMocks();
    window.localStorage.clear();
    document.body.innerHTML = '';
    vi.spyOn(window, 'fetch').mockResolvedValue({ ok: true } as Response);
    vi.spyOn(window, 'alert').mockImplementation(() => undefined);
  });

  afterEach(() => {
    appWrapper?.unmount();
    appWrapper = null;
    document.body.innerHTML = '';
  });

  it('loads commands on startup and can select and execute a command', async () => {
    const wrapper = await mountApp();

    expect(currentTransport.connect).toHaveBeenCalledTimes(1);
    expect(currentTransport.getKnownCommands).toHaveBeenCalledTimes(1);
    expect(currentTransport.startLog).toHaveBeenCalledTimes(1);
    expect(currentTransport.setContinueIfOneFails).toHaveBeenCalledWith(false);

    currentTransport.emit({ type: 'commandList', value: makeCommandGroups() });
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

    expect(currentTransport.run).toHaveBeenCalledWith('process resource {0}');
    expect(window.localStorage.getItem('commandHistory')).toBe(JSON.stringify(['process resource {0}']));
  });

  it('focuses the execute button after selecting a command without placeholders', async () => {
    const wrapper = await mountApp();

    currentTransport.emit({ type: 'commandList', value: makeCommandGroups() });
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

  it('supports keyboard navigation in the command menu', async () => {
    const wrapper = await mountApp();

    currentTransport.emit({ type: 'commandList', value: makeCommandGroups() });
    await nextTick();

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 100,
      right: 200,
      bottom: 130,
      left: 0,
      width: 200,
      height: 30,
      x: 0,
      y: 100,
      toJSON: () => undefined,
    });

    const toggle = wrapper.get('.webcli-command-menu .dropdown-toggle');
    await toggle.trigger('keydown', { key: 'ArrowDown', preventDefault: vi.fn() });
    await nextTick();

    const groupButton = wrapper.find('[data-group-button]');
    expect(document.activeElement).toBe(groupButton.element);

    await groupButton.trigger('keydown', { key: 'ArrowRight', preventDefault: vi.fn() });
    await nextTick();

    const commandButton = wrapper.find('[data-command-button="0"]');
    expect(document.activeElement).toBe(commandButton.element);
    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(flyout.style.left).toBe('200px');

    await commandButton.trigger('keydown', { key: 'Escape', preventDefault: vi.fn() });
    await nextTick();

    expect(document.activeElement).toBe(toggle.element);
  });

  it('positions the flyout submenu on hover so it is visible in the browser', async () => {
    const wrapper = await mountApp();

    currentTransport.emit({ type: 'commandList', value: makeCommandGroups() });
    await nextTick();

    const submenuGroup = wrapper.find('.dropdown-submenu');
    expect(submenuGroup.exists()).toBe(true);

    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 100,
      right: 200,
      bottom: 130,
      left: 0,
      width: 200,
      height: 30,
      x: 0,
      y: 100,
      toJSON: () => undefined,
    });

    await submenuGroup.trigger('mouseenter');

    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(flyout.style.top).toBe('100px');
    expect(flyout.style.left).toBe('200px');
    expect(flyout.style.maxHeight).toContain('px');
  });

  it('shifts the flyout submenu up when there is not enough space below', async () => {
    const wrapper = await mountApp();

    currentTransport.emit({
      type: 'commandList',
      value: [
        {
          name: 'Large group',
          commands: [
            { command: 'one', help: '1' },
            { command: 'two', help: '2' },
            { command: 'three', help: '3' },
          ],
        },
      ],
    });
    await nextTick();

    const originalInnerHeight = window.innerHeight;
    Object.defineProperty(window, 'innerHeight', {
      configurable: true,
      value: 500,
      writable: true,
    });

    const submenuGroup = wrapper.find('.dropdown-submenu');
    const groupElement = submenuGroup.element as HTMLElement;
    groupElement.getBoundingClientRect = () => ({
      top: 480,
      right: 200,
      bottom: 530,
      left: 0,
      width: 200,
      height: 50,
      x: 0,
      y: 480,
      toJSON: () => undefined,
    });

    await submenuGroup.trigger('mouseenter');

    const flyout = submenuGroup.find('.dropdown-menu').element as HTMLElement;
    expect(Number.parseInt(flyout.style.top, 10)).toBe(384);
    expect(flyout.style.left).toBe('200px');
    expect(Number.parseInt(flyout.style.maxHeight, 10)).toBe(106);

    Object.defineProperty(window, 'innerHeight', {
      configurable: true,
      value: originalInnerHeight,
      writable: true,
    });
  });

  it('moves through placeholders with Tab and then lets focus continue normally', async () => {
    const wrapper = await mountApp();
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

  it('renders and trims logs and can clear them', async () => {
    window.localStorage.setItem('historySize', '2');
    const wrapper = await mountApp();

    const emitLog = (message: string, time: number, exception: string | null = null) => {
      currentTransport.emit({
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

    currentTransport.emit({
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
    const wrapper = await mountApp();

    currentTransport.emit({
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
    expect(currentTransport.clearCommandList).toHaveBeenCalledTimes(1);

    currentTransport.emit({ type: 'queue', value: [], size: 0 });
    await nextTick();
    expect(wrapper.findAll('.queueTab')).toHaveLength(0);
  });

  it('toggles refresh controls and calls the matching transport methods', async () => {
    const wrapper = await mountApp();

    const stopRefresh = wrapper.findAll('button.nav-link').find(node => node.text().includes('Stop Refresh'));
    expect(stopRefresh).toBeTruthy();
    await stopRefresh!.trigger('click');
    expect(currentTransport.stopLog).toHaveBeenCalledTimes(1);

    await nextTick();
    const startRefresh = wrapper.findAll('button.nav-link').find(node => node.text().includes('Refresh'));
    expect(startRefresh).toBeTruthy();
    await startRefresh!.trigger('click');
    expect(currentTransport.startLog).toHaveBeenCalledTimes(2);
  });

  it('persists the continue-if-one-fails setting and updates the transport', async () => {
    const wrapper = await mountApp();

    await wrapper.findAll('button.nav-link').find(node => node.text().includes('Settings'))!.trigger('click');
    await nextTick();

    const continueIfOneFails = wrapper.find('#webcli-continue-on-fail');
    expect(continueIfOneFails).toBeTruthy();
    await continueIfOneFails.setValue(true);

    expect(window.localStorage.getItem('continueIfOneFails')).toBe('true');
    expect(currentTransport.setContinueIfOneFails).toHaveBeenLastCalledWith(true);
  });

  it('shows command history and lets the user restore a previous command', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a', 'process b']));
    const wrapper = await mountApp();
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
    const wrapper = await mountApp();

    expect(wrapper.get('dialog').attributes('open')).toBeUndefined();
  });

  it('uses semantic buttons and labels for the primary controls', async () => {
    const wrapper = await mountApp();

    expect(wrapper.get('label[for="webcli-command-input"]').text()).toContain('Command input');
    expect(wrapper.get('button[aria-controls="webcli-settings-panel"]').attributes('aria-expanded')).toBe('false');
    expect(wrapper.get('input#webcli-history-size').attributes('type')).toBe('number');
    expect(wrapper.get('input#webcli-command-history-size').attributes('type')).toBe('number');
  });
});
