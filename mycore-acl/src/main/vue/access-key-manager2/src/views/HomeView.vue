<template>
  <LoadingOverlay :loading="loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t("component.acl.accesskey.frontend.title.main") }}</h3>
      </div>
    </div>
    <div
      v-if="accessKeyCreatedValue"
      class="row"
    >
      <div class="col-12">
        <div
          class="alert alert-success text-center"
          role="alert"
        >
          {{
            t("component.acl.accesskey.frontend.success.add", {
              value: accessKeyCreatedValue,
            })
          }}
          <template
            v-if="accessKeyCreated && config?.allowedSessionPermissionTypes?.includes(accessKeyCreated.type as string)"
          >
            {{ t("component.acl.accesskey.frontend.success.add.url") }}
            <a
              :href="getActivationLink(accessKeyCreatedValue)"
              disabled
            >
              {{ getActivationLink(accessKeyCreatedValue) }}
            </a>
          </template>
        </div>
      </div>
    </div>
    <div
      v-if="errorMessage"
      class="row"
    >
      <div class="col-12">
        <div
          class="alert alert-danger text-center"
          role="alert"
        >
          {{ t(errorMessage) }}
        </div>
      </div>
    </div>
    <div class="row pb-2">
      <div class="col-12">
        <div class="text-right">
          <button
            class="btn btn-primary"
            @click="handleShowCreateAccessKeyModal"
          >
            <i class="fa fa-plus" />
            {{ t("component.acl.accesskey.frontend.button.showCreateAccessKeyModal") }}
          </button>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <AccessKeyTable
          @remove-access-key="handleRemoveAccessKey"
          @view-access-key="showAccessKey"
        />
      </div>
    </div>
    <div class="row">
      <div class="col-12 d-flex justify-content-center">
        <Pagination
          :current-page="accessKeyStore.currentPage"
          :total-rows="accessKeyStore.totalCount"
          :per-page="accessKeyStore.pageSize"
          @page-changed="handlePageChange"
        />
      </div>
    </div>
    <CreateAccessKeyModal
      :show-modal="showCreateAccessKeyModal"
      @close="handleCloseCreateAccessKeyModal"
      @access-key-created="handleAddAccessKey"
    />
    <AccessKeyModal
      :show-modal="showAccessKeyModal"
      :access-key="currentAccessKey"
      @close="handleAccessKeyModalClose"
    />
    <ConfirmModal ref="confirmModal" />
  </div>
</template>
<script setup lang="ts">
import { inject, ref, onMounted, onErrorCaptured } from "vue";
import { useI18n } from "vue-i18n";
import { referenceKey, configKey, webApplicationBaseUrlKey, availablePermissionsKey, } from "@/keys";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { AccessKeyDto } from "@/dtos/accesskey";
import AccessKeyTable from "@/components/AccessKeyTable.vue";
import CreateAccessKeyModal from "@/components/CreateAccessKeyModal.vue";
import AccessKeyModal from "@/components/AccessKeyModal.vue";
import Pagination from "@/components/SimplePagination.vue";
import ConfirmModal from "@/components/ConfirmModal.vue";
import { urlEncode } from "@/utils";
import { Config } from "@/config";
import { useAccessKeyStore } from "@/store/access-keys";
import { deleteAccessKey, getAccessKeys, getAccessKeysByReferenceAndPermission } from "@/api/service";


const reference: string | undefined = inject(referenceKey);
const permissions: string[] | undefined = inject(availablePermissionsKey);
const { t } = useI18n();
const config: Config | undefined = inject(configKey);
const webApplicationBaseUrl: string | undefined = inject(webApplicationBaseUrlKey);

const errorMessage = ref<string>();
const accessKeyCreatedValue = ref<string>();
const accessKeyCreated = ref<AccessKeyDto>();
const loading = ref<boolean>(true);
const showCreateAccessKeyModal = ref<boolean>(false);
const showAccessKeyModal = ref<boolean>(false);
const currentAccessKey = ref<AccessKeyDto>();
const confirmModal = ref();

const accessKeyStore = useAccessKeyStore();

const fetchAccessKeys = async () => {
  let result;
  const offset = (accessKeyStore.currentPage - 1) * accessKeyStore.pageSize;
  const limit = accessKeyStore.pageSize;
  if (permissions && reference) {
    result = await getAccessKeysByReferenceAndPermission(
      reference,
      permissions,
      offset,
      limit
    );
  } else {
    result = await getAccessKeys(offset, limit);
  }
  accessKeyStore.setData(result.items);
  accessKeyStore.setTotalCount(result.totalResults);
}

const handleShowCreateAccessKeyModal = () => {
  showCreateAccessKeyModal.value = true;
};
const handleCloseCreateAccessKeyModal = () => {
  showCreateAccessKeyModal.value = false;
};
const showAccessKey = (index: number) => {
  currentAccessKey.value = accessKeyStore.paginatedAccessKeys[index];
  showAccessKeyModal.value = true;
};
const handleError = (error: unknown) => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetInfos = () => {
  errorMessage.value = undefined;
  accessKeyCreatedValue.value = undefined;
  accessKeyCreated.value = undefined;
};
// TODO
const getActivationLink = (value: string) =>
  t("component.acl.accesskey.frontend.success.add.url.format", {
    webApplicationBaseUrl: webApplicationBaseUrl,
    objectId: reference,
    value: urlEncode(value),
  });
const handleAccessKeyModalClose = () => {
  showAccessKeyModal.value = false;
  currentAccessKey.value = undefined;
};
const handlePageChange = async (page: number): Promise<void> => {
  resetInfos();
  loading.value = true;
  accessKeyStore.setPage(page);
  await fetchAccessKeys();
  loading.value = false;
};
const handleRemoveAccessKey = async (index: number): Promise<void> => {
  resetInfos();
  const accessKey: AccessKeyDto = accessKeyStore.paginatedAccessKeys[index];
  if (accessKey.id) {
    const fixedSecret =
      accessKey.id.length > 30 ? `${accessKey.id.slice(0, 27)}...` : accessKey.secret;
    const ok = await confirmModal.value.show({
      title: t("component.acl.accesskey.frontend.confirmRemove.title"),
      message: t("component.acl.accesskey.frontend.confirmRemove.text", {
        value: fixedSecret,
      }),
    });
    if (ok) {
      loading.value = true;
      try {
        await deleteAccessKey(accessKey.id);
        accessKeyStore.deleteItem(accessKey.id);
      } finally {
        loading.value = false;
      }
    }
  }
};
const handleAddAccessKey = (secret: string, accessKey: AccessKeyDto) => {
  if (accessKey.secret !== secret) {
    accessKeyCreatedValue.value = secret;
    accessKeyCreated.value = accessKey;
  }
};
onMounted(async () => {
  try {
    await fetchAccessKeys();
  } catch (error) {
    handleError(error);
  } finally {
    loading.value = false;
  }
});
onErrorCaptured((error) => {
  handleError(error);
  return false;
});
</script>
