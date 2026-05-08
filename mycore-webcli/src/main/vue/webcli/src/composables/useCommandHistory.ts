import { ref } from 'vue';

import { loadCommandHistory, persistCommandHistory } from '@/services/settings';

export function useCommandHistory(commandHistorySize: () => number) {
  const entries = ref(loadCommandHistory());
  const draftIndex = ref(0);

  function addEntry(value: string): void {
    const trimmed = value.trim();
    if (!trimmed) {
      return;
    }
    const nextEntries = [...entries.value];
    if (nextEntries.at(-1) !== trimmed) {
      nextEntries.push(trimmed);
    }
    while (nextEntries.length > commandHistorySize()) {
      nextEntries.shift();
    }
    entries.value = nextEntries;
    persistCommandHistory(entries.value);
  }

  function trimToSize(): void {
    const maxSize = commandHistorySize();
    if (entries.value.length > maxSize) {
      entries.value = entries.value.slice(-maxSize);
      persistCommandHistory(entries.value);
    }
  }

  function prepareBrowseDraft(currentCommand: string): void {
    if (draftIndex.value === 0 && currentCommand) {
      entries.value = [...entries.value, currentCommand];
      draftIndex.value += 1;
    }
  }

  function browseUp(currentCommand: string): string | null {
    prepareBrowseDraft(currentCommand);
    if (entries.value.length > draftIndex.value) {
      draftIndex.value += 1;
      return entries.value[entries.value.length - draftIndex.value] ?? currentCommand;
    }
    return null;
  }

  function browseDown(): string | null {
    if (draftIndex.value > 1) {
      draftIndex.value -= 1;
      return entries.value[entries.value.length - draftIndex.value] ?? null;
    }
    if (draftIndex.value === 1) {
      entries.value = entries.value.slice(0, -1);
      draftIndex.value -= 1;
    }
    return null;
  }

  function finalizeExecution(): void {
    if (draftIndex.value !== 0) {
      entries.value = entries.value.slice(0, -1);
    }
    draftIndex.value = 0;
  }

  function clear(): void {
    entries.value = [];
    draftIndex.value = 0;
    window.localStorage.removeItem('commandHistory');
  }

  return {
    entries,
    addEntry,
    browseDown,
    browseUp,
    clear,
    finalizeExecution,
    trimToSize,
  };
}
