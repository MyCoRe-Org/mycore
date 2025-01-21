<template>
  <LoadingOverlay :loading="loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t("component.acl.accesskey.frontend.title.main") }}</h3>
      </div>
    </div>
    <div
      v-if="accessKeyCreatedSecret"
      class="row"
    >
      <div class="col-12">
        <div
          class="alert alert-success text-center"
          role="alert"
        >
          {{
            t("component.acl.accesskey.frontend.success.add", {
              secret: accessKeyCreatedSecret,
            })
          }}
          <template
            v-if="accessKeyCreated && config && config.allowedSessionPermissionTypes.includes(accessKeyCreated.type as string)"
          >
            {{ t("component.acl.accesskey.frontend.success.add.url") }}
            <a
              :href="getActivationLink(accessKeyCreatedSecret)"
              disabled
            >
              {{ getActivationLink(accessKeyCreatedSecret) }}
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
          :access-keys="paginatedAccessKeys"
          @remove-access-key="handleRemoveAccessKey"
          @view-access-key="showAccessKey"
        />
      </div>
    </div>
    <div class="row">
      <div class="col-12 d-flex justify-content-center">
        <Pagination
          :current-page="currentPage"
          :total-rows="totalCount"
          :per-page="pageSize"
          @page-changed="handlePageChange"
        />
      </div>
    </div>
    <CreateAccessKeyModal
      :access-key-service="accessKeyService"
      :reference="reference"
      :available-permissions="availablePermissions"
      :show-modal="showCreateAccessKeyModal"
      @close="handleCloseCreateAccessKeyModal"
      @access-key-created="handleAddAccessKey"
    />
    <AccessKeyModal
      :access-key-service="accessKeyService"
      :available-permissions="availablePermissions"
      :reference="reference"
      :show-modal="showAccessKeyModal"
      :access-key="currentAccessKey"
      @access-key-updated="handleUpdateAccessKey"
      @close="handleAccessKeyModalClose"
    />
    <ConfirmModal ref="confirmModal" />
  </div>
</template>
<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { ref, onMounted, onErrorCaptured, Ref, computed } from "vue";
import { useI18n } from "vue-i18n";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { AccessKeyDto } from "@/dtos/accesskey";
import AccessKeyTable from "@/components/AccessKeyTable.vue";
import CreateAccessKeyModal from "@/components/CreateAccessKeyModal.vue";
import AccessKeyModal from "@/components/AccessKeyModal.vue";
import Pagination from "@/components/SimplePagination.vue";
import ConfirmModal from "@/components/ConfirmModal.vue";
import { urlEncode, BASE_URL, fetchJWT, fetchConfig } from "@/utils";
import { AccessKeyService, AccessTokenAuthStrategy, AuthStrategy } from '@/service/accesskey';
import { Config } from '@/config';

class DevAuthStrategy implements AuthStrategy {
  public getHeaders(): Record<string, string> {
    return {
      'Authorization': `Basic ${process.env.VUE_APP_API_TOKEN}`
    };
  }
}

const router = useRouter();
const route = useRoute();
const { t } = useI18n();

const accessKeyService = ref<AccessKeyService>();
const config = ref<Config>();

const reference = route.query.reference as string | undefined;
const availablePermissionsQuery = route.query.availablePermissions as string | undefined;
const availablePermissions = availablePermissionsQuery ? availablePermissionsQuery.split(',') : [];

const totalCount = ref(0);
const currentPage = ref(Number(route.query.page) || 1);
const pageSize = ref(Number(route.query.pageSize) || 8);
const accessKeys: Ref<AccessKeyDto[]> = ref([]);
const paginatedAccessKeys = computed(() =>  accessKeys.value.slice(0, pageSize.value));

const errorMessage = ref<string>();
const accessKeyCreatedSecret = ref<string>();
const accessKeyCreated = ref<AccessKeyDto>();
const loading = ref<boolean>(true);
const showCreateAccessKeyModal = ref<boolean>(false);
const showAccessKeyModal = ref<boolean>(false);
const currentAccessKey = ref<AccessKeyDto>();
const confirmModal = ref();

const fetchAccessKeys = async (): Promise<void> => {
  if (accessKeyService.value) {
    const offset = (currentPage.value - 1) * pageSize.value;
    const limit = pageSize.value;
    const result = await accessKeyService.value.getAccessKeys(
      availablePermissions,
      reference,
      offset,
      limit
    );
    accessKeys.value = result.accessKeys;
    totalCount.value = result.totalCount;
  }
};
const handleShowCreateAccessKeyModal = (): void => {
  showCreateAccessKeyModal.value = true;
};
const handleCloseCreateAccessKeyModal = (): void => {
  showCreateAccessKeyModal.value = false;
};
const showAccessKey = (index: number): void => {
  currentAccessKey.value = paginatedAccessKeys.value[index];
  showAccessKeyModal.value = true;
};
const handleError = (error: unknown): void => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetInfos = (): void => {
  errorMessage.value = undefined;
  accessKeyCreatedSecret.value = undefined;
  accessKeyCreated.value = undefined;
};
const getActivationLink = (secret: string): string =>
  t("component.acl.accesskey.frontend.success.add.url.format", {
    baseUrl: BASE_URL,
    reference,
    secret: urlEncode(secret),
  });
const handleAccessKeyModalClose = (): void => {
  showAccessKeyModal.value = false;
  currentAccessKey.value = undefined;
};
const handlePageChange = async (page: number): Promise<void> => {
  resetInfos();
  loading.value = true;
  currentPage.value = page;
  await fetchAccessKeys();
  loading.value = false;
  router.push({
    query: {
      ...route.query,
      page: page.toString(),
    },
  });
};
const handleRemoveAccessKey = async (index: number): Promise<void> => {
  resetInfos();
  const accessKey: AccessKeyDto = paginatedAccessKeys.value[index];
  if (accessKey.id) {
    const ok = await confirmModal.value.show({
      title: t("component.acl.accesskey.frontend.confirmRemove.title"),
      message: t("component.acl.accesskey.frontend.confirmRemove.text", {
        secret: accessKey.id.length > 30 ? `${accessKey.id.slice(0, 27)}...` : accessKey.secret,
      }),
    });
    if (accessKeyService.value && ok) {
      loading.value = true;
      try {
        await accessKeyService.value.deleteAccessKey(accessKey.id);
        accessKeys.value = accessKeys.value.filter((a: AccessKeyDto) => a.id !== accessKey.id);
        totalCount.value -= 1;
      } finally {
        loading.value = false;
      }
    }
  }
};
const handleUpdateAccessKey = (accessKey: AccessKeyDto): void => {
  const index = accessKeys.value.findIndex((accessKey: AccessKeyDto) => accessKey.id === accessKey.id);
  if (index !== -1) {
    accessKeys.value[index] = accessKey;
  }
};
const handleAddAccessKey = (secret: string, accessKey: AccessKeyDto): void => {
  if (accessKey.secret !== secret) {
    accessKeyCreatedSecret.value = secret;
    accessKeyCreated.value = accessKey;
  }
  totalCount.value += 1;
  accessKeys.value.push(accessKey);
};
onMounted(async (): Promise<void> => {
  try {
    config.value = await fetchConfig(BASE_URL);
    if (process.env.NODE_ENV === "development") {
      accessKeyService.value = new AccessKeyService(BASE_URL, new DevAuthStrategy());
    } else {
      const jwt = await fetchJWT(BASE_URL, reference || undefined, config.value.isSessionEnabled);
      accessKeyService.value = new AccessKeyService(BASE_URL, new AccessTokenAuthStrategy(jwt.access_token));
    }
    await fetchAccessKeys();
  } catch (error) {
    handleError(error);
  } finally {
    loading.value = false;
  }
});
onErrorCaptured((error): boolean => {
  handleError(error);
  return false;
});
</script>
