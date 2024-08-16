<template>
  <LoadingOverlay :loading="loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t("component.acl.accesskey.frontend.title.main") }}</h3>
      </div>
    </div>
    <div v-if="accessKeyCreatedValue" class="row">
      <div class="col-12">
        <div class="alert alert-success text-center" role="alert">
          {{
            t("component.acl.accesskey.frontend.success.add", {
              value: accessKeyCreatedValue,
            })
          }}
          <template
            v-if="accessKeyCreated && configStore.getAllowedSessionPermissionTypes.includes(accessKeyCreated.permission as string)"
          >
            {{ t("component.acl.accesskey.frontend.success.add.url") }}
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
    <template
      v-if="
        isBooted &&
        !(errorMessage?.endsWith('noPermission') || errorMessage?.endsWith('noObjectId'))
      "
    >
      <div class="row pb-2">
        <div class="col-12">
          <div class="text-right">
            <button class="btn btn-primary" @click="handleShowCreateAccessKeyModal">
              <i class="fa fa-plus" />
              {{ t("component.acl.accesskey.frontend.button.showCreateAccessKeyModal") }}
            </button>
          </div>
        </div>
      </div>
      <div class="row">
        <div class="col-12">
          <AccessKeyTable
            :access-keys="accessKeys"
            @remove-access-key="handleRemoveAccessKey"
            @update-access-key="handleUpdateAccessKey"
            @view-access-key="showAccessKey"
          />
        </div>
      </div>
      <div class="row">
        <div class="col-12 d-flex justify-content-center">
          <Pagination
            :current-page="currentPage"
            :total-rows="totalCount"
            :per-page="perPage"
            @page-changed="handlePageChange"
          />
        </div>
      </div>
    </template>
    <CreateAccessKeyModal
      :show-modal="showCreateAccessKeyModal"
      @close="handleCloseCreateAccessKeyModal"
      @access-key-created="handleAddAccessKey"
    />
    <AccessKeyModal
      :show-modal="showAccessKeyModal"
      :access-key="currentAccessKey"
      @close="handleAccessKeyModalClose"
      @access-key-updated="handleUpdateAccessKey"
    />
    <ConfirmModal ref="confirmModal" />
  </div>
</template>
<script setup lang="ts">
import { inject, ref, onMounted, onErrorCaptured } from "vue";
import { useI18n } from "vue-i18n";
import {
  getAccessKeys,
  getAccessKeysByReferenceAndPermission,
  removeAccessKey,
  AccessKeyInformation,
} from "@/api/service";
import { referenceKey, availablePermissionsKey } from "@/keys";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import AccessKeyDto from "@/dtos/AccessKeyDto";
import AccessKeyTable from "@/components/AccessKeyTable.vue";
import CreateAccessKeyModal from "@/components/CreateAccessKeyModal.vue";
import AccessKeyModal from "@/components/AccessKeyModal.vue";
import Pagination from "@/components/SimplePagination.vue";
import ConfirmModal from "@/components/ConfirmModal.vue";
import { getWebApplicationBaseURL, urlEncode } from "@/utils";
import { useConfigStore, useAuthStore } from "@/stores";

const reference: string | undefined = inject(referenceKey);
const { t } = useI18n();
const authStore = useAuthStore();
const configStore = useConfigStore();
const availablePermissions: string[] | undefined = inject(availablePermissionsKey);

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
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetInfos = () => {
  errorMessage.value = undefined;
  accessKeyCreatedValue.value = undefined;
  accessKeyCreated.value = undefined;
};
const fetchAccessKeys = async (): Promise<void> => {
  resetInfos();
  const offset = (currentPage.value - 1) * perPage;
  const accessKeyInformation: AccessKeyInformation =
    reference && availablePermissions
      ? await getAccessKeysByReferenceAndPermission(
          reference,
          availablePermissions,
          offset,
          perPage
        )
      : await getAccessKeys(offset, perPage);
  accessKeys.value = accessKeyInformation.items;
  totalCount.value = accessKeyInformation.totalResults;
};
// TODO
const getActivationLink = (value: string) =>
  t("component.acl.accesskey.frontend.success.add.url.format", {
    webApplicationBaseUrl: configStore.webApplicationBaseURL,
    objectId: reference,
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
  await fetchAccessKeys();
  loading.value = false;
};
const handleRemoveAccessKey = async (index: number): Promise<void> => {
  resetInfos();
  const accessKey: AccessKeyDto = accessKeys.value[index];
  if (accessKey.id) {
    const fixedValue =
      accessKey.id.length > 30 ? `${accessKey.id.slice(0, 27)}...` : accessKey.value;
    const ok = await confirmModal.value.show({
      title: t("component.acl.accesskey.frontend.confirmRemove.title"),
      message: t("component.acl.accesskey.frontend.confirmRemove.text", {
        value: fixedValue,
      }),
    });
    if (ok) {
      await removeAccessKey(accessKey.id);
      accessKeys.value.splice(index, 1);
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
  try {
    configStore.webApplicationBaseURL = getWebApplicationBaseURL() as string;
    await configStore.fetchConfig();
    if (process.env.NODE_ENV === "production") {
      reference ? await authStore.login(reference) : await authStore.login();
    }
    await fetchAccessKeys();
    isBooted.value = true;
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
