<script setup lang="ts">
import { nextTick, ref, toRef } from 'vue';

import { useDialogLifecycle } from '@/composables/useDialogLifecycle';

const props = defineProps<{
  entries: string[];
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'select-command': [value: string];
  'update:modelValue': [value: boolean];
}>();

const dialogRef = ref<HTMLDialogElement | null>(null);

function closeDialog(): void {
  emit('update:modelValue', false);
}

function selectCommand(value: string): void {
  emit('select-command', value);
}

useDialogLifecycle(toRef(props, 'modelValue'), dialogRef, () => {
  nextTick(() => {
    dialogRef.value?.querySelector<HTMLButtonElement>('.list-group-item, .btn-close')?.focus();
  });
});
</script>

<template>
  <dialog
    ref="dialogRef"
    class="webcli-dialog webcli-history-dialog p-0"
    aria-labelledby="command-history-title"
    @cancel.prevent="closeDialog"
    @close="emit('update:modelValue', false)"
  >
    <div class="modal-content">
      <div class="modal-header">
        <h5 id="command-history-title" class="modal-title">Command History</h5>
        <button type="button" class="btn-close" aria-label="Close" @click="closeDialog"></button>
      </div>
      <div class="modal-body webcli-history-body">
        <div class="list-group comHistoryList">
          <button
            v-for="(entry, index) in [...entries].reverse()"
            :key="`${index}-${entry}`"
            type="button"
            class="list-group-item list-group-item-action text-start webcli-history-entry"
            :title="entry"
            @click="selectCommand(entry)"
          >
            {{ entry }}
          </button>
        </div>
      </div>
    </div>
  </dialog>
</template>
