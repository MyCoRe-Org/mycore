import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick, ref, type Ref } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useCommandInputController } from '@/composables/useCommandInputController';
import { cleanupDomTestEnvironment, setupDomTestEnvironment } from '@/test/helpers/domMocks';
import type { CommandGroup } from '@/types';

function buildCommandGroups(): CommandGroup[] {
  return [
    {
      name: 'Transformations',
      commands: [
        { command: 'xslt transform {0}', help: 'Run an XSLT transformation.' },
        { command: 'import object', help: 'Import a resource with optional xslt mapping.' },
      ],
    },
  ];
}

function createHarness(
  executedCommands: string[],
  commandGroups: Ref<CommandGroup[]>,
  commandHistorySize: Ref<number>,
  suggestionLimit: Ref<number>,
) {
  return defineComponent({
    setup() {
      return useCommandInputController({
        commandGroups,
        commandHistorySize,
        suggestionLimit,
        onExecuteCommand: command => {
          executedCommands.push(command);
        },
      });
    },
    template: `
      <div>
        <input ref="inputElement" />
        <button ref="executeButtonElement" type="button">Execute</button>
      </div>
    `,
  });
}

describe('useCommandInputController', () => {
  let wrapper: VueWrapper | null = null;

  beforeEach(() => {
    setupDomTestEnvironment();
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    cleanupDomTestEnvironment();
  });

  it('executes commands, clears the input, and stores command history', async () => {
    const executedCommands: string[] = [];
    const commandGroups = ref(buildCommandGroups());
    const commandHistorySize = ref(10);
    const suggestionLimit = ref(10);

    wrapper = mount(createHarness(executedCommands, commandGroups, commandHistorySize, suggestionLimit), {
      attachTo: document.body,
    });
    const vm = (wrapper.vm as unknown) as {
      command: string;
      commandHistoryEntries: string[];
      executeCommand: () => void;
    };

    vm.command = 'process object';
    vm.executeCommand();
    await nextTick();

    expect(executedCommands).toEqual(['process object']);
    expect(vm.command).toBe('');
    expect(vm.commandHistoryEntries).toEqual(['process object']);
  });

  it('uses arrow keys for suggestions while open and switches back to history after Escape', async () => {
    window.localStorage.setItem('commandHistory', JSON.stringify(['process a', 'process b']));
    const executedCommands: string[] = [];
    const commandGroups = ref(buildCommandGroups());
    const commandHistorySize = ref(10);
    const suggestionLimit = ref(10);

    wrapper = mount(createHarness(executedCommands, commandGroups, commandHistorySize, suggestionLimit), {
      attachTo: document.body,
    });
    const vm = (wrapper.vm as unknown) as {
      command: string;
      highlightedSuggestion: { command: string } | null;
      isSuggestionMenuVisible: boolean;
      onCommandInput: () => void;
      onCommandKeydown: (event: KeyboardEvent) => void;
    };

    vm.command = 'xslt';
    vm.onCommandInput();
    await nextTick();

    vm.onCommandKeydown({ key: 'ArrowDown', preventDefault: vi.fn() } as unknown as KeyboardEvent);
    expect(vm.isSuggestionMenuVisible).toBe(true);
    expect(vm.highlightedSuggestion?.command).toBe('import object');

    vm.onCommandKeydown({ key: 'Escape', preventDefault: vi.fn() } as unknown as KeyboardEvent);
    await nextTick();
    vm.onCommandKeydown({ key: 'ArrowUp' } as KeyboardEvent);

    expect(vm.command).toBe('process b');
  });

  it('accepts a highlighted suggestion on Tab before placeholder navigation starts', async () => {
    const executedCommands: string[] = [];
    const commandGroups = ref(buildCommandGroups());
    const commandHistorySize = ref(10);
    const suggestionLimit = ref(10);

    wrapper = mount(createHarness(executedCommands, commandGroups, commandHistorySize, suggestionLimit), {
      attachTo: document.body,
    });
    const vm = (wrapper.vm as unknown) as {
      command: string;
      onCommandInput: () => void;
      onCommandKeydown: (event: KeyboardEvent) => void;
    };

    vm.command = 'xslt';
    vm.onCommandInput();
    await nextTick();

    const preventDefault = vi.fn();
    vm.onCommandKeydown({ key: 'Tab', preventDefault } as unknown as KeyboardEvent);
    await nextTick();

    expect(preventDefault).toHaveBeenCalledTimes(1);
    expect(vm.command).toBe('xslt transform {0}');
  });

  it('trims stored history when the configured history size shrinks', async () => {
    const executedCommands: string[] = [];
    const commandGroups = ref(buildCommandGroups());
    const commandHistorySize = ref(3);
    const suggestionLimit = ref(10);

    wrapper = mount(createHarness(executedCommands, commandGroups, commandHistorySize, suggestionLimit), {
      attachTo: document.body,
    });
    const vm = (wrapper.vm as unknown) as {
      command: string;
      commandHistoryEntries: string[];
      executeCommand: () => void;
    };

    vm.command = 'process a';
    vm.executeCommand();
    vm.command = 'process b';
    vm.executeCommand();
    vm.command = 'process c';
    vm.executeCommand();
    await nextTick();

    commandHistorySize.value = 1;
    await nextTick();

    expect(vm.commandHistoryEntries).toEqual(['process c']);
  });
});
