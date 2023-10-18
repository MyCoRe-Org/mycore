<template>
  <loading-overlay :loading="loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>
          {{ tc('title.main') }}
        </h3>
      </div>
    </div>
    <div v-if="errorCode" class="row">
      <div class="col">
        <div class="alert alert-danger text-center" role="alert">
          {{ tc(`error.${errorCode}`) }}
        </div>
      </div>
    </div>
    <template v-if="isBooted && !(errorCode === 'unauthorizedError' || errorCode === 'noObjectId')">
      <div class="row pb-2">
        <div class="col">
          <div class="text-right">
            <b-button v-b-modal.create-access-key-modal variant="primary">
              <font-awesome-icon icon="plus" />
              {{ tc('button.add') }}
            </b-button>
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col">
          <access-key-table @error="handleError" />
        </div>
      </div>
      <div class="row">
        <div class="col d-flex justify-content-center">
          <b-pagination v-model="currentPage" :total-rows="totalCount" :per-page="limit" @change="handlePageChange" />
        </div>
      </div>
    </template>
    <create-access-key-modal @accessKeyCreated="handleCreated" />
  </div>
</template>
<script setup lang="ts">
import {
  Component,
  h,
  ref,
  computed,
  onMounted,
  getCurrentInstance,
  onErrorCaptured,
} from 'vue';
import { useApplicationStore, useAuthStore, useConfigStore } from '@/stores';
import { useI18n } from 'vue-i18n';
import { BButton, BvModal } from 'bootstrap-vue';
import { shortReference, getI18nKey } from '@/utils';
import AccessKeyTable from '@/components/AccessKeyTable.vue';
import LoadingOverlay from '@/components/LoadingOverlay.vue';
import CreateAccessKeyModal from '@/components/CreateAccessKeyModal.vue';

const instance: Component = getCurrentInstance();
const { t } = useI18n();
const tc = (key: string, obj?) => t(getI18nKey(key), obj);
const store = useApplicationStore();
const authStore = useAuthStore();
const configStore = useConfigStore();
const errorCode = ref();
const isBooted = ref(false);
const loading = ref(true);
const totalCount = computed(() => store.totalCount);
const limit = computed(() => store.limit);
const currentPage = computed(() => Math.floor(store.offset / store.limit + 1));
const handleError = (code) => {
  errorCode.value = code;
};
const fetch = async (): Promise<void> => {
  try {
    await store.fetch();
  } catch (err) {
    handleError(err.message);
  }
};
const handlePageChange = async (page) => {
  loading.value = true;
  store.offset = (page - 1) * limit.value;
  await fetch();
  loading.value = false;
};
const handleCreated = async (secret, reference) => {
  const url = configStore.webApplicationBaseURL + tc('success.add.url.format2', {
    objectId: store.objectId,
    secret: encodeURIComponent(secret),
  });
  const messageVNode = h('div', [
    h('span', [tc('success.add', { reference: shortReference(reference), secret })]),
  ]);
  if ((await configStore.isSessionEnabled)) {
    messageVNode.children.push(h('br'), h('br'));
    messageVNode.children.push(h('span', [tc('success.add.url'), ' ']));
    messageVNode.children.push(h('a', { class: ['disabled'], href: url, target: '_blank' }, [url]));
  }
  const bvModal = instance.ctx._bv__modal as BvModal;
  bvModal.msgBoxOk([messageVNode], {
    title: tc('title.add'),
    noCloseOnBackdrop: true,
  });
};
onMounted(async () => {
  if (store.objectId) {
    try {
      configStore.fetchConfig();
      if (process.env.NODE_ENV === 'production') {
        await authStore.login(store.objectId, store.derivateId);
      }
      await fetch();
    } catch (err) {
      handleError(err.message);
    }
  } else {
    errorCode.value = 'noObjectId';
  }
  isBooted.value = true;
  loading.value = false;
});
onErrorCaptured((err) => {
  handleError(err.message);
  return false;
});
</script>
