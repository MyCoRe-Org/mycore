import { computed, ref, watch, type Ref } from 'vue';

import type { CommandGroup, SearchableCommand } from '@/types';

function flattenCommands(commandGroups: CommandGroup[]): SearchableCommand[] {
  return commandGroups.flatMap(group => {
    return group.commands.map(entry => ({
      command: entry.command,
      groupName: group.name,
      help: entry.help,
    }));
  });
}

function scoreCommand(searchTerm: string, command: SearchableCommand): number {
  const normalizedQuery = searchTerm.toLowerCase();
  const normalizedCommand = command.command.toLowerCase();
  const normalizedHelp = command.help.toLowerCase();

  if (normalizedCommand.startsWith(normalizedQuery)) {
    return 0;
  }
  if (normalizedCommand.includes(normalizedQuery)) {
    return 1;
  }
  if (normalizedHelp.includes(normalizedQuery)) {
    return 2;
  }
  return Number.MAX_SAFE_INTEGER;
}

export function useCommandSearch(
  commandGroups: Ref<CommandGroup[]>,
  commandInput: Ref<string>,
  suggestionLimit: Ref<number>
) {
  const highlightedIndex = ref(0);

  const searchableCommands = computed(() => flattenCommands(commandGroups.value));
  const normalizedQuery = computed(() => commandInput.value.trim().toLowerCase());
  const maxSuggestions = computed(() => Math.max(1, Math.trunc(suggestionLimit.value || 1)));
  const matchingCommands = computed(() => {
    if (!normalizedQuery.value) {
      return [];
    }
    return searchableCommands.value
      .filter(command => {
        return (
          command.command.toLowerCase().includes(normalizedQuery.value) ||
          command.help.toLowerCase().includes(normalizedQuery.value)
        );
      })
      .sort((left, right) => {
        const scoreDifference = scoreCommand(normalizedQuery.value, left) - scoreCommand(normalizedQuery.value, right);
        if (scoreDifference !== 0) {
          return scoreDifference;
        }
        const commandDifference = left.command.localeCompare(right.command);
        if (commandDifference !== 0) {
          return commandDifference;
        }
        return left.groupName.localeCompare(right.groupName);
      });
  });

  const suggestions = computed(() => {
    return matchingCommands.value.slice(0, maxSuggestions.value);
  });

  const hasSuggestions = computed(() => suggestions.value.length > 0);
  const highlightedSuggestion = computed(() => suggestions.value[highlightedIndex.value] ?? null);

  function resetHighlight(): void {
    highlightedIndex.value = 0;
  }

  function moveHighlight(delta: number): void {
    if (!hasSuggestions.value) {
      highlightedIndex.value = 0;
      return;
    }
    highlightedIndex.value =
      (highlightedIndex.value + delta + suggestions.value.length) % suggestions.value.length;
  }

  function highlightSuggestion(index: number): void {
    highlightedIndex.value = index;
  }

  watch(suggestions, nextSuggestions => {
    if (nextSuggestions.length === 0) {
      highlightedIndex.value = 0;
      return;
    }
    if (highlightedIndex.value >= nextSuggestions.length) {
      highlightedIndex.value = 0;
    }
  });

  return {
    hasSuggestions,
    highlightedIndex,
    highlightedSuggestion,
    highlightSuggestion,
    moveHighlight,
    resetHighlight,
    totalSuggestionCount: computed(() => matchingCommands.value.length),
    suggestions,
  };
}
