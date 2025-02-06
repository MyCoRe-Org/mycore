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
          <button class="btn btn-secondary" @click="close">
            {{ t(getI18nKey('button.no')) }}
          </button>
          <button class="btn btn-primary" @click="confirm">
            {{ t(getI18nKey('button.yes')) }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Modal } from 'bootstrap';
import { getI18nKey } from '@/common/utils';

const { t } = useI18n();

const title = ref<string>('');
const message = ref<string>('');
const modalElement = ref<HTMLDivElement | null>(null);
let modalInstance: Modal | null = null;
let confirmCallback: (() => void) | null = null;

const open = (
  modalTitle: string,
  modalMessage: string,
  callback?: () => void
): void => {
  title.value = modalTitle;
  message.value = modalMessage;
  confirmCallback = callback || null;
  modalInstance?.show();
};

const close = (): void => {
  modalInstance?.hide();
};

const confirm = (): void => {
  modalInstance?.hide();
  if (confirmCallback) {
    confirmCallback();
  }
};

onMounted((): void => {
  if (modalElement.value) {
    modalInstance = new Modal(modalElement.value, { backdrop: 'static' });
  }
});

defineExpose({ open });
</script>
