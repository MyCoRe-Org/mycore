<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';

import type { SearchableCommand } from '@/types';

const props = defineProps<{
  highlightedIndex: number;
  id: string;
  suggestions: SearchableCommand[];
}>();

const emit = defineEmits<{
  highlight: [index: number];
  select: [command: string];
}>();

const listRef = ref<HTMLElement | null>(null);

function selectSuggestion(command: string): void {
  emit('select', command);
}

watch(() => props.highlightedIndex, async highlightedIndex => {
  await nextTick();
  const activeOption = listRef.value?.querySelector<HTMLElement>(`#${props.id}-option-${highlightedIndex}`);
  activeOption?.scrollIntoView({ block: 'nearest' });
});
</script>

<template>
  <ul :id="id" ref="listRef" class="list-group webcli-suggestions" role="listbox">
    <li v-for="(suggestion, index) in suggestions" :key="`${suggestion.groupName}-${suggestion.command}`" role="presentation">
      <button
        :id="`${id}-option-${index}`"
        type="button"
        class="list-group-item list-group-item-action webcli-suggestion"
        :class="{ 'is-highlighted': highlightedIndex === index }"
        role="option"
        :aria-selected="highlightedIndex === index"
        @mouseenter="emit('highlight', index)"
        @click="selectSuggestion(suggestion.command)"
      >
        <span class="webcli-suggestion-command">{{ suggestion.command }}</span>
        <span class="webcli-suggestion-help">{{ suggestion.help }}</span>
        <span class="webcli-suggestion-group">{{ suggestion.groupName }}</span>
      </button>
    </li>
  </ul>
</template>
