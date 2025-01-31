<template>
  <div ref="modalElement" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">{{ title }}</h5>
          <button type="button" class="btn-close" @click="close"></button>
        </div>
        <div class="modal-body">
          <p>{{ message }}</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" @click="close">Abbrechen</button>
          <button class="btn btn-primary" @click="confirm">Bestätigen</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Modal } from 'bootstrap';

const title = ref<string>('');
const message = ref<string>('');
const modalElement = ref<HTMLDivElement | null>(null);
let modalInstance: Modal | null = null;
let confirmCallback: (() => void) | null = null;

onMounted(() => {
  if (modalElement.value) {
    modalInstance = new Modal(modalElement.value, { backdrop: 'static' });
  }
});

const open = (
  modalTitle: string,
  modalMessage: string,
  callback?: () => void
) => {
  title.value = modalTitle;
  message.value = modalMessage;
  confirmCallback = callback || null;
  modalInstance?.show();
};

const close = () => {
  modalInstance?.hide();
};

const confirm = () => {
  modalInstance?.hide();
  if (confirmCallback) confirmCallback();
};

defineExpose({ open });
</script>
