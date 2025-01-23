<template>
  <Modal
    :is-visible="isVisible"
    hide-header-close
    :ok-title="t(getI18nKey('button.yes'))"
    :cancel-title="t(getI18nKey('button.no'))"
    :title="title"
    @ok="ok"
    @cancel="cancel"
  >
    {{ message }}
  </Modal>
</template>
<script setup lang="ts">
import { ref } from "vue";
import Modal from "./BaseModal.vue";
import { getI18nKey } from "@/utils";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const isVisible = ref(false);
const title = ref<string>();
const message = ref<string>();
let resolvePromise: ((value: boolean) => void) | undefined;
interface Ops {
  title: string;
  message: string;
}
const show = (ops: Ops): Promise<boolean> => {
  title.value = ops.title;
  message.value = ops.message;
  isVisible.value = true;
  return new Promise<boolean>((resolve) => {
    resolvePromise = resolve;
  });
};
const ok = (): void => {
  if (resolvePromise) {
    isVisible.value = false;
    resolvePromise(true);
    resolvePromise = undefined;
  }
};
const cancel = (): void => {
  if (resolvePromise) {
    isVisible.value = false;
    resolvePromise(false);
    resolvePromise = undefined;
  }
};
defineExpose({ show });
</script>
