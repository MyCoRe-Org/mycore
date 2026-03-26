import { computed, nextTick, ref, watch, type Ref } from 'vue';

import { useCommandHistory } from '@/composables/useCommandHistory';
import { useCommandSearch } from '@/composables/useCommandSearch';
import type { CommandGroup } from '@/types';

interface CommandInputControllerOptions {
  commandGroups: Ref<CommandGroup[]>;
  commandHistorySize: Ref<number>;
  suggestionLimit: Ref<number>;
  onExecuteCommand: (command: string) => void;
}

export function useCommandInputController(options: CommandInputControllerOptions) {
  const command = ref('');
  const isSuggestionMenuVisible = ref(false);
  const isSuggestionNavigationActive = ref(false);
  const suppressSuggestionMenuOnFocus = ref(false);
  const suppressSuggestionsUntilInput = ref(false);
  const inputElement = ref<HTMLInputElement | null>(null);
  const executeButtonElement = ref<HTMLButtonElement | null>(null);
  const suggestionListId = 'webcli-command-suggestions';
  const activeHighlightedIndex = computed(() => {
    return isSuggestionNavigationActive.value ? highlightedIndex.value : null;
  });

  const {
    addEntry,
    browseDown,
    browseUp,
    clear: clearCommandHistory,
    entries: commandHistoryEntries,
    finalizeExecution,
    trimToSize,
  } = useCommandHistory(() => Math.max(0, options.commandHistorySize.value));

  const {
    hasSuggestions,
    highlightedIndex,
    highlightedSuggestion,
    highlightSuggestion,
    moveHighlight,
    resetHighlight,
    totalSuggestionCount,
    suggestions,
  } = useCommandSearch(options.commandGroups, command, options.suggestionLimit);

  function closeSuggestionMenu(restoreFocus = false): void {
    isSuggestionMenuVisible.value = false;
    isSuggestionNavigationActive.value = false;
    resetHighlight();
    if (restoreFocus) {
      suppressSuggestionMenuOnFocus.value = true;
      nextTick(() => {
        if (inputElement.value && document.activeElement !== inputElement.value) {
          inputElement.value.focus();
        }
      });
    }
  }

  function activateSuggestionNavigation(index = 0): void {
    if (!hasSuggestions.value) {
      return;
    }
    isSuggestionMenuVisible.value = true;
    isSuggestionNavigationActive.value = true;
    highlightSuggestion(index);
  }

  function selectNextPlaceholder(startFrom = 0): boolean {
    const input = inputElement.value;
    if (!input || !command.value) {
      return false;
    }
    const regex = /\{[0-9]+\}/g;
    regex.lastIndex = startFrom;
    const match = regex.exec(command.value);
    if (match) {
      input.focus();
      input.setSelectionRange(match.index, match.index + match[0].length);
      return true;
    }
    return false;
  }

  function focusInput(selectionStart = command.value.length, selectionEnd = selectionStart): void {
    nextTick(() => {
      const input = inputElement.value;
      if (!input) {
        return;
      }
      if (document.activeElement !== input) {
        suppressSuggestionMenuOnFocus.value = true;
      }
      input.focus();
      input.setSelectionRange(selectionStart, selectionEnd);
    });
  }

  function focusSelectedCommand(): void {
    nextTick(() => {
      const focusedPlaceholder = selectNextPlaceholder(0);
      if (!focusedPlaceholder) {
        focusInput();
      }
    });
  }

  function executeCommand(): void {
    const value = command.value.trim();
    if (!value) {
      return;
    }
    finalizeExecution();
    options.onExecuteCommand(value);
    addEntry(value);
    command.value = '';
    closeSuggestionMenu();
  }

  function selectCommand(value: string): void {
    command.value = value;
    suppressSuggestionsUntilInput.value = true;
    closeSuggestionMenu();
    focusSelectedCommand();
  }

  function onCommandKeydown(event: KeyboardEvent): void {
    if (event.key === 'ArrowDown' && isSuggestionMenuVisible.value && hasSuggestions.value) {
      event.preventDefault();
      if (!isSuggestionNavigationActive.value) {
        activateSuggestionNavigation(0);
        return;
      }
      moveHighlight(1);
      return;
    }
    if (event.key === 'ArrowUp' && isSuggestionMenuVisible.value && hasSuggestions.value) {
      event.preventDefault();
      if (!isSuggestionNavigationActive.value) {
        activateSuggestionNavigation(suggestions.value.length - 1);
        return;
      }
      moveHighlight(-1);
      return;
    }
    if (event.key === 'Enter') {
      if (isSuggestionMenuVisible.value && isSuggestionNavigationActive.value && highlightedSuggestion.value) {
        event.preventDefault();
        selectCommand(highlightedSuggestion.value.command);
        return;
      }
      event.preventDefault();
      executeCommand();
      return;
    }
    if (event.key === 'Escape' && isSuggestionMenuVisible.value) {
      event.preventDefault();
      const input = inputElement.value;
      const selectionStart = input?.selectionStart ?? command.value.length;
      const selectionEnd = input?.selectionEnd ?? selectionStart;
      closeSuggestionMenu();
      focusInput(selectionStart, selectionEnd);
      return;
    }
    if (event.key === 'ArrowUp') {
      closeSuggestionMenu();
      const previousCommand = browseUp(command.value);
      if (previousCommand !== null) {
        command.value = previousCommand;
      }
      return;
    }
    if (event.key === 'ArrowDown') {
      closeSuggestionMenu();
      const nextCommand = browseDown();
      if (nextCommand !== null) {
        command.value = nextCommand;
      }
      return;
    }
    if (event.key === 'Tab' && !event.shiftKey) {
      if (
        isSuggestionMenuVisible.value &&
        highlightedSuggestion.value &&
        command.value.trim() !== highlightedSuggestion.value.command
      ) {
        event.preventDefault();
        selectCommand(highlightedSuggestion.value.command);
        return;
      }
      const movedToPlaceholder = selectNextPlaceholder(inputElement.value?.selectionEnd ?? 0);
      if (!movedToPlaceholder) {
        return;
      }
      event.preventDefault();
    }
  }

  function onCommandInput(): void {
    suppressSuggestionsUntilInput.value = false;
    isSuggestionMenuVisible.value = hasSuggestions.value;
    isSuggestionNavigationActive.value = false;
    resetHighlight();
  }

  function onCommandInputFocus(): void {
    if (suppressSuggestionMenuOnFocus.value) {
      suppressSuggestionMenuOnFocus.value = false;
      return;
    }
    if (suppressSuggestionsUntilInput.value) {
      return;
    }
    if (hasSuggestions.value) {
      isSuggestionMenuVisible.value = true;
    }
  }

  function onCommandInputBlur(): void {
    window.setTimeout(() => {
      closeSuggestionMenu();
    }, 100);
  }

  function onSuggestionActivate(index: number): void {
    activateSuggestionNavigation(index);
  }

  function restoreCommandFromHistory(value: string): void {
    command.value = value;
    suppressSuggestionsUntilInput.value = true;
    focusSelectedCommand();
  }

  watch(options.commandHistorySize, () => {
    trimToSize();
  });

  watch(hasSuggestions, value => {
    if (!value) {
      closeSuggestionMenu();
    }
  });

  return {
    clearCommandHistory,
    command,
    commandHistoryEntries,
    executeButtonElement,
    executeCommand,
    hasSuggestions,
    highlightedIndex: activeHighlightedIndex,
    highlightedSuggestion,
    inputElement,
    isSuggestionMenuVisible,
    onCommandInput,
    onCommandInputBlur,
    onCommandInputFocus,
    onCommandKeydown,
    onSuggestionActivate,
    restoreCommandFromHistory,
    selectCommand,
    suggestionListId,
    suggestions,
    totalSuggestionCount,
  };
}
