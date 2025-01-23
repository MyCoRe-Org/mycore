<template>
  <LoadingOverlay :loading="state.loading" />
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t(getI18nKey("title.main")) }}</h3>
      </div>
    </div>
    <div
      v-if="state.accessKeyCreatedSecret"
      class="row"
    >
      <div class="col-12">
        <div
          class="alert alert-success text-center"
          role="alert"
        >
          {{
            t(getI18nKey("success.add"), {
              secret: state.accessKeyCreatedSecret,
            })
          }}
          <template
            v-if="state.accessKeyCreated && config && config.allowedSessionPermissionTypes.includes(state.accessKeyCreated.type as string)"
          >
            {{ t(getI18nKey("success.add.url")) }}
            <a
              :href="activationLink"
              disabled
            >
              {{ activationLink }}
            </a>
          </template>
        </div>
      </div>
    </div>
    <div
      v-if="state.errorMessage"
      class="row"
    >
      <div class="col-12">
        <div
          class="alert alert-danger text-center"
          role="alert"
        >
          {{ t(state.errorMessage) }}
        </div>
      </div>
    </div>
    <div class="row pb-2">
      <div class="col-12">
        <div class="text-right">
          <button
            class="btn btn-primary"
            @click="openCreateAccessKeyModal"
          >
            <i class="fa fa-plus" />
            {{ t(getI18nKey("button.showCreateAccessKeyModal")) }}
          </button>
        </div>
      </div>
    </div>
    <!-- TODO bind message -->
    <div class="row">
      <div class="col-12">
        <AccessKeyTable
          :access-keys="paginatedAccessKeys"
          @remove-access-key="deleteAccessKey"
          @view-access-key="openAccessKeyInfoModal"
        />
      </div>
    </div>
    <div class="row">
      <div class="col-12 d-flex justify-content-center">
        <Pagination
          :current-page="state.currentPage"
          :total-rows="state.totalCount"
          :per-page="state.pageSize"
          @change-page="changePage"
        />
      </div>
    </div>
    <CreateAccessKeyModal
      :access-key-service="accessKeyService"
      :reference="reference"
      :available-permissions="availablePermissions"
      :is-visible="state.isCreateAccessKeyModalVisible"
      @close="closeCreateAccessKeyModal"
      @add-access-key="addAccessKey"
    />
    <AccessKeyInfoModal
      :access-key-service="accessKeyService"
      :available-permissions="availablePermissions"
      :reference="reference"
      :is-visible="state.isAccessKeyInfoModalVisible"
      :access-key="state.currentAccessKey"
      @update-access-key="updateAccessKey"
      @close="closeAccessKeyInfoModal"
    />
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router';
import { ref, onMounted, onErrorCaptured, computed, reactive } from "vue";
import { useI18n } from "vue-i18n";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { AccessKeyDto } from "@/dtos/accesskey";
import AccessKeyTable from "@/components/AccessKeyTable.vue";
import CreateAccessKeyModal from "@/components/CreateAccessKeyModal.vue";
import AccessKeyInfoModal from "@/components/AccessKeyInfoModal.vue";
import Pagination from "@/components/SimplePagination.vue";
import { urlEncode, BASE_URL, fetchJWT, fetchConfig, getI18nKey } from "@/utils";
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

const getActivationLink = (secret: string): string =>
  t("component.acl.accesskey.frontend.success.add.url.format", {
    baseUrl: BASE_URL,
    reference,
    secret: urlEncode(secret),
  });

const reference = route.query.reference as string | undefined;
const availablePermissionsQuery = route.query.availablePermissions as string | undefined;
const availablePermissions = availablePermissionsQuery ? availablePermissionsQuery.split(',') : [];

const state = reactive({
  loading: false,
  totalCount: 0,
  currentPage: Number(route.query.page) || 1,
  pageSize: Number(route.query.pageSize) || 8,
  accessKeys: [] as AccessKeyDto[],
  errorMessage: undefined as string | undefined,
  accessKeyCreatedSecret: undefined as string | undefined,
  accessKeyCreated: undefined as AccessKeyDto | undefined,
  isCreateAccessKeyModalVisible: false,
  isAccessKeyInfoModalVisible: false,
  currentAccessKey: undefined as AccessKeyDto | undefined,
})
const accessKeyService = ref<AccessKeyService>();
const config = ref<Config>();

const paginatedAccessKeys = computed(() =>  state.accessKeys.slice(0, state.pageSize));
const activationLink = computed((): string => {
  if (state.accessKeyCreated && state.accessKeyCreatedSecret) {
    return getActivationLink(state.accessKeyCreatedSecret);
  }
  return '';
});
const fetchAccessKeys = async (): Promise<void> => {
  if (accessKeyService.value) {
    const offset = (state.currentPage - 1) * state.pageSize;
    const result = await accessKeyService.value.getAccessKeys(
      availablePermissions,
      reference,
      offset,
      state.pageSize
    );
    state.accessKeys = result.accessKeys;
    state.totalCount = result.totalCount;
  }
};
const openCreateAccessKeyModal = (): void => {
  state.isCreateAccessKeyModalVisible = true;
};
const closeCreateAccessKeyModal = (): void => {
  state.isCreateAccessKeyModalVisible = false;
};
const openAccessKeyInfoModal = (index: number): void => {
  state.currentAccessKey = paginatedAccessKeys.value[index];
  state.isAccessKeyInfoModalVisible = true;
};
const closeAccessKeyInfoModal = (): void => {
  state.isAccessKeyInfoModalVisible = false;
  state.currentAccessKey = undefined;
};
const handleError = (error: unknown): void => {
  state.errorMessage =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetInfos = (): void => {
  state.errorMessage = undefined;
  state.accessKeyCreatedSecret = undefined;
  state.accessKeyCreated = undefined;
};
const changePage = async (page: number): Promise<void> => {
  resetInfos();
  state.loading = true;
  state.currentPage = page;
  await fetchAccessKeys();
  state.loading = false;
  router.push({
    query: {
      ...route.query,
      page: page.toString(),
    },
  });
};
const deleteAccessKey = async (accessKeyId: string): Promise<void> => {
  resetInfos();
  if (accessKeyService.value) {
    state.loading = true;
    try {
      await accessKeyService.value.deleteAccessKey(accessKeyId);
      state.accessKeys = state.accessKeys.filter((a: AccessKeyDto) => a.id !== accessKeyId);
      state.totalCount -= 1;
    } finally {
      state.loading = false;
    }
  }
};
const updateAccessKey = (accessKey: AccessKeyDto): void => {
  const index = state.accessKeys.findIndex((a: AccessKeyDto) => a.id === accessKey.id);
  if (index !== -1) {
    state.accessKeys[index] = accessKey;
  }
};
const addAccessKey = (secret: string, accessKey: AccessKeyDto): void => {
  if (accessKey.secret !== secret) {
    state.accessKeyCreatedSecret = secret;
    state.accessKeyCreated = accessKey;
  }
  state.totalCount += 1;
  state.accessKeys.push(accessKey);
};
onMounted(async (): Promise<void> => {
  try {
    state.loading = true;
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
    state.loading = false;
  }
});
onErrorCaptured((error): boolean => {
  handleError(error);
  return false;
});
</script>
