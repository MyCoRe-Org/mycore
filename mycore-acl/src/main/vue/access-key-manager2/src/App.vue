<template>
  <LoadingOverlay :loading="loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t('component.acl.accesskey.frontend.title.main') }}</h3>
      </div>
    </div>
    <div v-if="accessKeyCreatedValue" class="row">
      <div class="col-12">
        <div class="alert alert-success text-center" role="alert">
          {{ t('component.acl.accesskey.frontend.success.add', {
            value: accessKeyCreatedValue,
          }) }}
          <template v-if="accessKeyCreated
            && configStore.getAllowedSessionPermissionTypes
            .includes(accessKeyCreated.permission as string)">
            {{ t('component.acl.accesskey.frontend.success.add.url') }}
            <a :href="getActivationLink(accessKeyCreatedValue)" disabled>
              {{ getActivationLink(accessKeyCreatedValue) }}
            </a>
          </template>
        </div>
      </div>
    </div>
    <div v-if="errorMessage" class="row">
      <div class="col-12">
        <div class="alert alert-danger text-center" role="alert">
          {{ t(errorMessage) }}
        </div>
      </div>
    </div>
    <template v-if="isBooted && !(errorMessage?.endsWith('noPermission')
      || errorMessage?.endsWith('noObjectId'))">
      <div class="row pb-2">
        <div class="col-12">
          <div class="text-right">
            <button class="btn btn-primary" @click="handleShowCreateAccessKeyModal">
              <i class="fa fa-plus"></i>
              {{ t('component.acl.accesskey.frontend.button.add') }}
            </button>
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-12">
          <AccessKeyTable :access-keys="accessKeys" @remove-access-key="handleRemoveAccessKey"
            @update-access-key="handleUpdateAccessKey" @view-access-key="showAccessKey">
          </AccessKeyTable>
        </div>
      </div>
      <div class="row">
        <div class="col-12 d-flex justify-content-center">
          <Pagination :current-page="currentPage" :total-rows="totalCount"
            :per-page="perPage" @page-changed="handlePageChange">
          </Pagination>
        </div>
      </div>
    </template>
    <CreateAccessKeyModal :show-modal="showCreateAccessKeyModal"
      @close="handleCloseCreateAccessKeyModal" @access-key-created="handleAddAccessKey">
    </CreateAccessKeyModal>
    <AccessKeyModal :show-modal="showAccessKeyModal" :access-key="currentAccessKey"
      @close="handleAccessKeyModalClose" @access-key-updated="handleUpdateAccessKey">
    </AccessKeyModal>
    <ConfirmModal ref="confirmModal"></ConfirmModal>
  </div>
</template>
<script setup lang="ts">
import {
  inject,
  ref,
  onMounted,
  onErrorCaptured,
} from 'vue';
import { useI18n } from 'vue-i18n';
import {
  getAccessKeys,
  removeAccessKey,
  AccessKeyInformation,
  fetchPermissions,
} from '@/api/service';
import objectIdKey from '@/keys';
import LoadingOverlay from '@/components/LoadingOverlay.vue';
import AccessKeyDto from '@/dtos/AccessKeyDto';
import AccessKeyTable from '@/components/AccessKeyTable.vue';
import CreateAccessKeyModal from '@/components/CreateAccessKeyModal.vue';
import AccessKeyModal from '@/components/AccessKeyModal.vue';
import AccessKeyPermissionsDto from '@/dtos/AccessKeyPermissionsDto';
import Pagination from '@/components/SimplePagination.vue';
import ConfirmModal from '@/components/ConfirmModal.vue';
import { getWebApplicationBaseURL, urlEncode } from '@/utils';
import { useConfigStore, useAuthStore } from '@/stores';

const objectId: string | undefined = inject(objectIdKey);
const { t } = useI18n();
const authStore = useAuthStore();
const configStore = useConfigStore();

const errorMessage = ref<string>();
const accessKeyCreatedValue = ref<string>();
const accessKeyCreated = ref<AccessKeyDto>();
const isBooted = ref<boolean>(false);
const loading = ref<boolean>(true);

const totalCount = ref<number>(0);
const perPage = 8;
const currentPage = ref<number>(1);

const showCreateAccessKeyModal = ref<boolean>(false);
const showAccessKeyModal = ref<boolean>(false);
const currentAccessKey = ref<AccessKeyDto>();
const accessKeys = ref<AccessKeyDto[]>([]);
const confirmModal = ref();

const handleShowCreateAccessKeyModal = () => {
  showCreateAccessKeyModal.value = true;
};
const handleCloseCreateAccessKeyModal = () => {
  showCreateAccessKeyModal.value = false;
};

const showAccessKey = (index: number) => {
  currentAccessKey.value = accessKeys.value[index];
  showAccessKeyModal.value = true;
};

const handleError = (error: unknown) => {
  if (error instanceof Error) {
    errorMessage.value = error.message;
  } else {
    errorMessage.value = 'component.acl.accesskey.frontend.error.fatal';
  }
};
const resetInfos = () => {
  errorMessage.value = undefined;
  accessKeyCreatedValue.value = undefined;
  accessKeyCreated.value = undefined;
};
const fetch = async (): Promise<void> => {
  resetInfos();
  if (objectId) {
    const offset = (currentPage.value - 1) * perPage;
    const accessKeyInformation:
      AccessKeyInformation = await getAccessKeys(objectId, offset, perPage);
    accessKeys.value = accessKeyInformation.items;
    totalCount.value = accessKeyInformation.totalResults;
  }
};

const getActivationLink = (value: string) => t('component.acl.accesskey.frontend.success.add.url.format', {
  webApplicationBaseUrl: configStore.webApplicationBaseURL,
  objectId,
  value: urlEncode(value),
});

const handleAccessKeyModalClose = () => {
  showAccessKeyModal.value = false;
  currentAccessKey.value = undefined;
};

const handleUpdateAccessKey = (value: string, accessKey: AccessKeyDto) => {
  resetInfos();
  const index = accessKeys.value.findIndex((k: AccessKeyDto) => value === k.value);
  Object.assign(accessKeys.value[index], accessKey);
};

const handlePageChange = async (page: number): Promise<void> => {
  resetInfos();
  currentPage.value = page;
  loading.value = true;
  await fetch();
  loading.value = false;
};
const handleRemoveAccessKey = async (index: number): Promise<void> => {
  resetInfos();
  if (objectId) {
    const accessKey: AccessKeyDto = accessKeys.value[index];
    if (accessKey.value) {
      const fixedValue = accessKey.value.length > 30 ? `${accessKey.value.slice(0, 27)}...` : accessKey.value;
      const ok = await confirmModal.value.show({
        title: t('component.acl.accesskey.frontend.confirmRemove.title'),
        message: t('component.acl.accesskey.frontend.confirmRemove.text', {
          value: fixedValue,
        }),
      });
      if (ok) {
        await removeAccessKey(objectId, accessKey.value);
        accessKeys.value.splice(index, 1);
      }
    }
  }
};
const handleAddAccessKey = (value: string, accessKey: AccessKeyDto) => {
  accessKeys.value.push(accessKey);
  if (accessKey.value !== value) {
    accessKeyCreatedValue.value = value;
    accessKeyCreated.value = accessKey;
  }
};

onMounted(async () => {
  if (objectId) {
    try {
      configStore.webApplicationBaseURL = getWebApplicationBaseURL() as string;
      await configStore.fetchConfig();
      if (process.env.NODE_ENV === 'production') {
        await authStore.login(objectId);
      }
      const permissions: AccessKeyPermissionsDto = await fetchPermissions(objectId);
      configStore.manageReadAccesskeys = permissions.manageReadAccessKeys;
      configStore.manageWriteAccessKeys = permissions.manageWriteAccessKeys;
      if (configStore.manageReadAccesskeys || configStore.manageWriteAccessKeys) {
        await fetch();
      } else {
        throw Error('component.acl.accesskey.frontend.error.noPermission');
      }
    } catch (error) {
      handleError(error);
    }
  } else {
    errorMessage.value = 'component.acl.accesskey.frontend.error.noObjectId';
  }
  isBooted.value = true;
  loading.value = false;
});

onErrorCaptured((error) => {
  handleError(error);
  return false;
});

</script>
