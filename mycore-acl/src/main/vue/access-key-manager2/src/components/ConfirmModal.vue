<template>
  <Modal
    :show="visible"
    hide-header-close
    :ok-title="$t('component.acl.accesskey.frontend.button.yes')"
    :cancel-title="$t('component.acl.accesskey.frontend.button.no')"
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

const visible = ref(false);
const title = ref<string | null>(null);
const message = ref<string | null>(null);
let resolvePromise: ((value: boolean) => void) | undefined;
interface Ops {
  title: string;
  message: string;
}
const show = (ops: Ops): Promise<boolean> => {
  title.value = ops.title;
  message.value = ops.message;
  visible.value = true;
  return new Promise<boolean>((resolve) => {
    resolvePromise = resolve;
  });
};
const ok = (): void => {
  if (resolvePromise) {
    visible.value = false;
    resolvePromise(true);
    resolvePromise = undefined;
  }
};
const cancel = (): void => {
  if (resolvePromise) {
    visible.value = false;
    resolvePromise(false);
    resolvePromise = undefined;
  }
};
defineExpose({ show });
</script>
