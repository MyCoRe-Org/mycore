<script setup lang="ts">
import { ref, toRef } from 'vue';

import { useDialogLifecycle } from '@/composables/useDialogLifecycle';
import type { Settings } from '@/types';

const props = defineProps<{
  hasCommandHistory: boolean;
  modelValue: boolean;
}>();
const settings = defineModel<Settings>('settings', { required: true });

const emit = defineEmits<{
  'clear-command-history': [];
  'update:modelValue': [value: boolean];
}>();

const dialogRef = ref<HTMLDialogElement | null>(null);
const initialFocusRef = ref<HTMLInputElement | null>(null);

function closeDialog(): void {
  emit('update:modelValue', false);
}

useDialogLifecycle(toRef(props, 'modelValue'), dialogRef, () => {
  initialFocusRef.value?.focus();
  initialFocusRef.value?.select();
});
</script>

<template>
  <dialog
    ref="dialogRef"
    class="webcli-dialog webcli-settings-dialog p-0"
    aria-labelledby="webcli-settings-title"
    @cancel.prevent="closeDialog"
    @close="emit('update:modelValue', false)"
  >
    <div class="modal-content">
      <div class="modal-header">
        <h5 id="webcli-settings-title" class="modal-title">Settings</h5>
        <button type="button" class="btn-close" aria-label="Close" @click="closeDialog"></button>
      </div>
      <div class="modal-body">
        <div class="webcli-settings-stack">
          <section class="webcli-settings-section" aria-labelledby="webcli-settings-history">
            <h6 id="webcli-settings-history" class="webcli-settings-section-title">History</h6>
            <div class="webcli-settings-field">
              <label for="webcli-history-size" class="form-label">Log History Size</label>
              <input
                id="webcli-history-size"
                ref="initialFocusRef"
                v-model.number="settings.historySize"
                type="number"
                min="1"
                inputmode="numeric"
                class="form-control"
              />
              <div class="form-text">Maximum log entries kept in the browser session.</div>
            </div>

            <div class="webcli-settings-field">
              <label for="webcli-command-history-size" class="form-label">Command History Size</label>
              <input
                id="webcli-command-history-size"
                v-model.number="settings.comHistorySize"
                type="number"
                min="0"
                inputmode="numeric"
                class="form-control"
              />
              <div class="form-text">Recently executed commands available from the input field.</div>
              <div class="webcli-settings-actions">
                <button
                  type="button"
                  class="btn btn-outline-secondary"
                  :disabled="!hasCommandHistory"
                  @click="emit('clear-command-history')"
                >
                  Clear command history
                </button>
              </div>
            </div>

            <div class="webcli-settings-field">
              <label for="webcli-suggestion-limit" class="form-label">Suggestion Limit</label>
              <input
                id="webcli-suggestion-limit"
                v-model.number="settings.suggestionLimit"
                type="number"
                min="1"
                inputmode="numeric"
                class="form-control"
              />
              <div class="form-text">Maximum number of command suggestions shown below the input.</div>
            </div>
          </section>

          <section class="webcli-settings-section" aria-labelledby="webcli-settings-behavior">
            <h6 id="webcli-settings-behavior" class="webcli-settings-section-title">Behavior</h6>
            <div class="webcli-settings-field">
              <div class="form-check">
                <input id="webcli-autoscroll" v-model="settings.autoscroll" type="checkbox" class="form-check-input" />
                <label for="webcli-autoscroll" class="form-check-label">
                  Automatically scroll log output to the newest entry
                </label>
              </div>
            </div>

            <div class="webcli-settings-field">
              <div class="form-check">
                <input
                  id="webcli-continue-on-fail"
                  v-model="settings.continueIfOneFails"
                  type="checkbox"
                  class="form-check-input"
                />
                <label for="webcli-continue-on-fail" class="form-check-label">
                  Continue queue execution after a command fails
                </label>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </dialog>
</template>
