<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';

const props = defineProps<{
  entries: string[];
  modelValue: boolean;
}>();

const emit = defineEmits<{
  'select-command': [value: string];
  'update:modelValue': [value: boolean];
}>();

const dialogRef = ref<HTMLDialogElement | null>(null);
let previousFocus: HTMLElement | null = null;

function closeDialog(): void {
  emit('update:modelValue', false);
}

function selectCommand(value: string): void {
  emit('select-command', value);
}

watch(() => props.modelValue, async value => {
  const dialog = dialogRef.value;
  if (!dialog) {
    return;
  }
  if (value) {
    previousFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    if (typeof dialog.showModal === 'function' && !dialog.open) {
      dialog.showModal();
    } else {
      dialog.setAttribute('open', '');
    }
    await nextTick();
    dialog.querySelector<HTMLButtonElement>('.list-group-item, .btn-close')?.focus();
    return;
  }
  if (dialog.open && typeof dialog.close === 'function') {
    dialog.close();
  } else {
    dialog.removeAttribute('open');
  }
  previousFocus?.focus();
});

onBeforeUnmount(() => {
  if (dialogRef.value?.open && typeof dialogRef.value.close === 'function') {
    dialogRef.value.close();
  }
});
</script>

<template>
  <dialog
    ref="dialogRef"
    class="webcli-dialog p-0"
    aria-labelledby="command-history-title"
    @cancel.prevent="closeDialog"
    @close="emit('update:modelValue', false)"
  >
    <div class="modal-content">
      <div class="modal-header">
        <h5 id="command-history-title" class="modal-title">Command History</h5>
        <button type="button" class="btn-close" aria-label="Close" @click="closeDialog"></button>
      </div>
      <div class="modal-body">
        <div class="list-group comHistoryList">
          <button
            v-for="entry in [...entries].reverse()"
            :key="entry"
            type="button"
            class="list-group-item list-group-item-action text-start"
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
