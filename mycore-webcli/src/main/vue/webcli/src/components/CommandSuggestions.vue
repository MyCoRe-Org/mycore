<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

import type { SearchableCommand } from '@/types';

const props = defineProps<{
  highlightedIndex: number | null;
  id: string;
  suggestions: SearchableCommand[];
  suggestionLimit: number;
  totalCount: number;
}>();

const emit = defineEmits<{
  activate: [index: number];
  highlight: [index: number];
  select: [command: string];
}>();

const listRef = ref<HTMLElement | null>(null);

function selectSuggestion(command: string): void {
  emit('select', command);
}

function activateSuggestion(index: number): void {
  emit('activate', index);
  emit('highlight', index);
}

watch(() => props.highlightedIndex, async highlightedIndex => {
  if (highlightedIndex === null) {
    return;
  }
  await nextTick();
  const activeOption = listRef.value?.querySelector<HTMLElement>(`#${props.id}-option-${highlightedIndex}`);
  if (typeof activeOption?.scrollIntoView === 'function') {
    activeOption.scrollIntoView({ block: 'nearest' });
  }
});
</script>

<template>
  <div class="webcli-suggestions-panel">
    <ul :id="id" ref="listRef" class="list-group webcli-suggestions" role="listbox">
      <li v-for="(suggestion, index) in suggestions" :key="`${suggestion.groupName}-${suggestion.command}`" role="presentation">
        <button
          :id="`${id}-option-${index}`"
          type="button"
          class="list-group-item list-group-item-action webcli-suggestion"
          :class="{ 'is-highlighted': highlightedIndex === index }"
          role="option"
          :aria-selected="highlightedIndex === index"
          @mouseenter="activateSuggestion(index)"
          @pointerdown.prevent="selectSuggestion(suggestion.command)"
        >
          <span class="webcli-suggestion-command">{{ suggestion.command }}</span>
          <span class="webcli-suggestion-help">{{ suggestion.help }}</span>
          <span class="webcli-suggestion-group">{{ suggestion.groupName }}</span>
        </button>
      </li>
    </ul>
    <div v-if="totalCount > suggestionLimit" class="webcli-suggestions-meta">
      Showing top {{ suggestions.length }} of {{ totalCount }} matches.
    </div>
  </div>
</template>
