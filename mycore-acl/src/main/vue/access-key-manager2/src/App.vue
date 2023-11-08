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
    <template v-if="isBooted && !(errorCode === 'noPermission' || errorCode === 'noObjectId')">
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
          <access-key-table :items="accessKeys" :fields="fields" @removeItem ="handleRemoveAccessKey"
            @showItem="handleShowAccessKey" />
        </div>
      </div>
      <div class="row">
        <div class="col d-flex justify-content-center">
          <b-pagination v-model="currentPage" :total-rows="totalCount" :per-page="perPage" @change="handlePageChange" />
        </div>
      </div>
    </template>
    <create-access-key-modal @accessKeyCreated="handleAccessKeyCreated" />
    <access-key-modal :accessKey="viewAccessKey" @accessKeyUpdated="handleAccessKeyUpdated" />
  </div>
</template>
<script setup lang="ts">
import {
  Component,
  h,
  inject,
  ref,
  onMounted,
  getCurrentInstance,
  onErrorCaptured,
} from 'vue';
import {
  useAuthStore,
  useConfigStore,
} from '@/stores';
import { useI18n } from 'vue-i18n';
import { BButton, BvModal } from 'bootstrap-vue';
import {
  AccessKey,
  AccessKeyInformation,
  shortReference,
  getI18nKey,
} from '@/utils';
import {
  fetchDerivateAccessKey,
  fetchDerivateAccessKeys,
  fetchObjectAccessKey,
  fetchObjectAccessKeys,
  removeDerivateAccessKey,
  removeObjectAccessKey,
} from '@/api/service';
import { objectIdKey, derivateIdKey } from '@/keys';
import AccessKeyTable from '@/components/VRTable.vue';
import LoadingOverlay from '@/components/LoadingOverlay.vue';
import CreateAccessKeyModal from '@/components/CreateAccessKeyModal.vue';
import AccessKeyModal from '@/components/AccessKeyModal.vue';

const emit = defineEmits(['bv::show::modal']);
const objectId = inject(objectIdKey);
const derivateId = inject(derivateIdKey);
const instance: Component = getCurrentInstance();
const { t } = useI18n();
const tc = (key: string, obj?) => t(getI18nKey(key), obj);
const authStore = useAuthStore();
const configStore = useConfigStore();
const accessKeys = ref([]);
const errorCode = ref();
const isBooted = ref(false);
const loading = ref(true);
const totalCount = ref(0);
const perPage = 8;
const currentPage = ref(1);
const viewAccessKey = ref();
const fields = [
  {
    key: 'secret',
    label: tc('label.reference'),
    thClass: 'col-3 text-center',
    tdClass: 'col-3 align-middle',
  },
  {
    key: 'isActive',
    formatter: (value) => tc(`label.state.${(value) ? 'en' : 'dis'}abled`),
    label: tc('label.state'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'type',
    formatter: (value) => tc(`label.type.${value}`),
    label: tc('label.type'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'expiration',
    formatter: (value) => ((value != null) ? new Date(value).toLocaleDateString() : '-'),
    label: tc('label.expiration'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'comment',
    formatter: (value) => {
      if (value != null && value.length > 50) return `${value.substring(0, 50)}...`;
      if (value != null && value.length > 0) return value;
      return '-';
    },
    label: tc('label.comment'),
    thClass: 'col text-center',
    tdClass: 'col text-center',
  },
  {
    key: 'edit',
    label: tc('label.actions'),
    thClass: 'col-1 text-right',
    tdClass: 'col-1 text-right align-middle',
  },
];
const handleError = (code) => {
  errorCode.value = code;
};
const fetch = async (): Promise<void> => {
  const offset = (currentPage.value - 1) * perPage;
  try {
    const accessKeyInformation: AccessKeyInformation = (!derivateId)
      ? await fetchObjectAccessKeys(objectId, offset, perPage)
      : await fetchDerivateAccessKeys(objectId, derivateId, offset, perPage);
    accessKeys.value = accessKeyInformation.items;
    totalCount.value = accessKeyInformation.totalResults;
  } catch (err) {
    handleError(err.message);
  }
};
const handlePageChange = async (page): Promise<void> => {
  currentPage.value = page;
  loading.value = true;
  await fetch();
  loading.value = false;
};
const handleAccessKeyUpdated = async (accessKey: AccessKey): Promise<void> => {
  const result = (!derivateId) ? await fetchObjectAccessKey(objectId, accessKey.secret)
    : await fetchDerivateAccessKey(objectId, derivateId, accessKey);
  const index = accessKeys.value.findIndex((k) => k.secret === accessKey.secret);
  accessKeys.value[index] = result;
};
const handleAccessKeyCreated = async (secret: string, reference: string): Promise<void> => {
  const offset = (currentPage.value - 1) * perPage;
  if ((totalCount.value % perPage === 0) || ((totalCount.value - perPage) < offset)) {
    const result = (!derivateId) ? await fetchObjectAccessKey(objectId, reference)
      : await fetchDerivateAccessKey(objectId, derivateId, reference);
    if (totalCount.value % perPage === 0) {
      accessKeys.value = [result];
      currentPage.value = (totalCount.value / perPage) + 1;
    } else {
      accessKeys.value.push(result);
    }
    totalCount.value += 1;
  } else {
    currentPage.value = ((totalCount.value - (totalCount.value % perPage)) / perPage) + 1;
    await fetch();
  }
  const url = configStore.webApplicationBaseURL + tc('success.add.url.format2', {
    objectId,
    secret: encodeURIComponent(secret),
  });
  const messageVNode = h('div', [
    h('span', [tc('success.add2', { reference: shortReference(reference), secret })]),
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
const handleShowAccessKey = (accessKey: AccessKey): void => {
  viewAccessKey.value = accessKey;
  emit('bv::show::modal', 'access-key-modal');
};
const handleRemoveAccessKey = async (accessKey: AccessKey): Promise<void> => {
  const bvModal = instance.ctx._bv__modal as BvModal;
  const value = await bvModal.msgBoxConfirm(tc('text.remove2',
    { reference: shortReference(accessKey.secret) }), {
    title: tc('title.remove'),
    okTitle: tc('button.yes'),
    cancelTitle: tc('button.no'),
    noCloseOnBackdrop: true,
    noCloseOnEsc: true,
    hideBackdrop: true,
    okVariant: 'danger',
  });
  if (value) {
    try {
      if (!derivateId) await removeObjectAccessKey(objectId, accessKey.secret);
      else await removeDerivateAccessKey(objectId, derivateId, accessKey.secret);
      bvModal.msgBoxOk(tc('success.remove'), {
        title: tc('title.remove'),
      });
      const offset = (currentPage.value - 1) * perPage;
      if ((offset === 0) && (totalCount.value > perPage)) {
        await fetch();
      } else if ((offset >= perPage) && (totalCount.value % perPage === 1)) {
        currentPage.value -= 1;
        await fetch();
      } else {
        accessKeys.value = accessKeys.value.filter((k) => k.secret !== accessKey.secret);
        totalCount.value -= 1;
      }
    } catch (error) {
      handleError(error.message);
    }
  }
};
onMounted(async () => {
  if (objectId) {
    try {
      await configStore.fetchConfig();
      if (process.env.NODE_ENV === 'production') {
        await authStore.login(objectId, derivateId);
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
