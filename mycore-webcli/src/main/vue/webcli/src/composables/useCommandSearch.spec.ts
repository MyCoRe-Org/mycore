import { ref } from 'vue';
import { describe, expect, it } from 'vitest';

import { useCommandSearch } from '@/composables/useCommandSearch';
import type { CommandGroup } from '@/types';

function buildGroups(): CommandGroup[] {
  return [
    {
      name: 'Transformations',
      commands: [
        { command: 'xslt transform', help: 'Run an XSLT transformation.' },
        { command: 'import object', help: 'Import a resource with optional xslt mapping.' },
      ],
    },
    {
      name: 'Maintenance',
      commands: [
        { command: 'reindex', help: 'Rebuild the search index.' },
      ],
    },
  ];
}

describe('useCommandSearch', () => {
  it('filters commands by case-insensitive substring and ranks command matches before help matches', () => {
    const commandGroups = ref(buildGroups());
    const commandInput = ref('xslt');

    const { suggestions } = useCommandSearch(commandGroups, commandInput);

    expect(suggestions.value.map(entry => entry.command)).toEqual(['xslt transform', 'import object']);
  });

  it('returns no suggestions for an empty query', () => {
    const commandGroups = ref(buildGroups());
    const commandInput = ref('');

    const { suggestions, hasSuggestions } = useCommandSearch(commandGroups, commandInput);

    expect(suggestions.value).toEqual([]);
    expect(hasSuggestions.value).toBe(false);
  });
});
