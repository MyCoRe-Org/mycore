<template>
  <Modal :show="visible" @ok="ok" @cancel="cancel" hide-header-close :title="title"
    :cancel-title="$t('component.acl.accesskey.frontend.button.no')"
    :ok-title="$t('component.acl.accesskey.frontend.button.yes')">
    {{ message }}
  </Modal>
</template>
<script setup lang="ts">
import { ref } from 'vue';
import Modal from './BaseModal.vue';

const visible = ref(false);
const title = ref<string>();
const message = ref<string>();
let resolvePromise;

interface Ops {
  title: string;
  message: string;
}

const show = (ops: Ops) => {
  title.value = ops.title;
  message.value = ops.message;
  visible.value = true;
  return new Promise<boolean>((resolve) => {
    resolvePromise = resolve;
  });
};
const ok = () => {
  visible.value = false;
  resolvePromise(true);
};
const cancel = () => {
  visible.value = false;
  resolvePromise(false);
};
defineExpose({ show });
</script>
