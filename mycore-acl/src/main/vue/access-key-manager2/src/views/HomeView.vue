<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t(getI18nKey('title.main')) }}</h3>
      </div>
    </div>
    <div v-if="state.accessKeyCreatedSecret" class="row">
      <div class="col-12">
        <div class="alert alert-success text-center" role="alert">
          {{
            t(getI18nKey('success.add'), {
              secret: state.accessKeyCreatedSecret,
            })
          }}
          <template
            v-if="
              state.accessKeyCreated &&
              config &&
              config.allowedAccessKeySessionPermissions.includes(
                state.accessKeyCreated.type
              )
            "
          >
            {{ t(getI18nKey('success.add.url')) }}
            <a :href="activationLink" disabled>
              {{ activationLink }}
            </a>
          </template>
        </div>
      </div>
    </div>
    <div v-if="state.errorMessage" class="row">
      <div class="col-12">
        <div class="alert alert-danger text-center" role="alert">
          {{ t(state.errorMessage) }}
        </div>
      </div>
    </div>
    <div class="row pb-2">
      <div class="col-12">
        <div class="text-end">
          <button
            class="btn btn-primary"
            :disabled="state.loading"
            @click="createModalRef?.open"
          >
            <i class="fa fa-plus" />
            {{ t(getI18nKey('button.showCreateAccessKeyModal')) }}
          </button>
        </div>
      </div>
    </div>
    <!-- TODO bind message -->
    <div class="row">
      <div class="col-12">
        <div class="position-relative">
          <AccessKeyTable
            :access-keys="paginatedAccessKeys"
            @remove-access-key="deleteAccessKey"
            @view-access-key="openAccessKeyInfoModal"
          />
          <div
            id="loading-overlay"
            class="loading-overlay"
            :style="{ visibility: state.loading ? 'visible' : 'hidden' }"
          >
            <div class="spinner-border text-primary" role="status"></div>
          </div>
        </div>
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
      ref="createModalRef"
      :access-key-service="accessKeyService"
      :reference="reference"
      :available-permissions="availablePermissions"
      @add-access-key="addAccessKey"
    />
    <AccessKeyInfoModal
      ref="infoModalRef"
      :access-key-service="accessKeyService"
      :available-permissions="availablePermissions"
      :reference="reference"
      @update-access-key="updateAccessKey"
    />
  </div>
  <ConfirmModal ref="confirmModal" />
</template>

<script setup lang="ts">
import {
  ref,
  onMounted,
  onErrorCaptured,
  computed,
  reactive,
  inject,
} from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { urlEncode } from '@/common/utils';
import {
  AccessKey,
  AccessKeyConfig,
  AccessKeyService,
} from '@mycore-test/js-common/acl/accesskey';
import {
  AccessKeyTable,
  CreateAccessKeyModal,
  AccessKeyInfoModal,
  ConfirmModal,
} from 'vue-access-key-manager';
import Pagination from '@/components/SimplePagination.vue';
import { fetchJWT } from '@/common/auth';
import {
  HttpClient,
  ClientAuthStrategy,
  AccessTokenClientAuthStrategy,
} from '@mycore-test/js-common/common/client';
import { getI18nKey } from '@/common/utils';

class DevAuthStrategy implements ClientAuthStrategy {
  public getHeaders(): Record<string, string> {
    return {
      Authorization: `Basic ${import.meta.env.VITE_APP_API_TOKEN}`,
    };
  }
}

const baseUrl = inject('baseUrl') as string;
const config = inject('accessKeyConfig') as AccessKeyConfig;

const infoModalRef = ref<{ open: (accessKey: AccessKey) => void } | null>(null);
const createModalRef = ref<{ open: () => void } | null>(null);
const confirmModal = ref<{
  open: (title: string, message: string, callback?: () => void) => void;
} | null>(null);

const router = useRouter();
const route = useRoute();

const { t } = useI18n();

const getActivationLink = (secret: string): string =>
  t(getI18nKey('success.add.url.format'), {
    baseUrl,
    reference: reference ?? '',
    secret: urlEncode(secret),
  });

const reference = route.query.reference as string | undefined;
const availablePermissionsQuery = route.query.availablePermissions as
  | string
  | undefined;
const availablePermissions = availablePermissionsQuery
  ? availablePermissionsQuery.split(',')
  : [];

const state = reactive({
  loading: false,
  totalCount: 0,
  currentPage: Number(route.query.page) || 1,
  pageSize: Number(route.query.pageSize) || 8,
  accessKeys: [] as AccessKey[],
  errorMessage: undefined as string | undefined,
  accessKeyCreatedSecret: undefined as string | undefined,
  accessKeyCreated: undefined as AccessKey | undefined,
});
const accessKeyService = ref<AccessKeyService>();

const paginatedAccessKeys = computed(() =>
  state.accessKeys.slice(0, state.pageSize)
);
const activationLink = computed((): string => {
  if (state.accessKeyCreated && state.accessKeyCreatedSecret) {
    return getActivationLink(state.accessKeyCreatedSecret);
  }
  return '';
});
const fetchAccessKeys = async (): Promise<void> => {
  if (accessKeyService.value) {
    const offset = (state.currentPage - 1) * state.pageSize;
    const result = await accessKeyService.value.getAccessKeys({
      permissions: availablePermissions,
      reference,
      offset,
      limit: state.pageSize,
    });
    state.accessKeys = result.accessKeys;
    state.totalCount = result.totalCount;
  }
};
const openAccessKeyInfoModal = (index: number): void => {
  infoModalRef.value?.open(paginatedAccessKeys.value[index]);
};
const handleError = (error: unknown): void => {
  state.errorMessage =
    error instanceof Error ? error.message : t(getI18nKey('error.fatal'));
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
const deleteAccessKey = async (accessKey: AccessKey): Promise<void> => {
  resetInfos();
  const title = t('component.acl.accesskey.frontend.confirmRemove.title');
  const message = t('component.acl.accesskey.frontend.confirmRemove.text', {
    secret:
      accessKey.id.length > 30
        ? `${accessKey.id.slice(0, 27)}...`
        : accessKey.secret,
  });
  confirmModal.value?.open(title, message, async () => {
    state.loading = true;
    try {
      await accessKeyService.value?.deleteAccessKey(accessKey.id);
      state.accessKeys = state.accessKeys.filter(
        (a: AccessKey) => a.id !== accessKey.id
      );
      state.totalCount -= 1;
    } finally {
      state.loading = false;
    }
  });
};
const updateAccessKey = (accessKey: AccessKey): void => {
  const index = state.accessKeys.findIndex(
    (a: AccessKey) => a.id === accessKey.id
  );
  if (index !== -1) {
    state.accessKeys[index] = accessKey;
  }
};
const addAccessKey = (secret: string, accessKey: AccessKey): void => {
  if (accessKey.secret !== secret) {
    state.accessKeyCreatedSecret = secret;
    state.accessKeyCreated = accessKey;
  }
  state.totalCount += 1;
  state.accessKeys.push(accessKey);
};
const initAccessKeyService = async () => {
  if (import.meta.env.PROD) {
    const jwt = await fetchJWT(
      baseUrl,
      reference || undefined,
      config.isAccessKeySessionEnabled
    );
    accessKeyService.value = new AccessKeyService(
      new HttpClient(baseUrl, {
        authStrategy: new AccessTokenClientAuthStrategy(jwt),
        timeout: 5000,
      })
    );
  } else {
    accessKeyService.value = new AccessKeyService(
      new HttpClient(baseUrl, {
        authStrategy: new DevAuthStrategy(),
        timeout: 5000,
      })
    );
  }
};
onMounted(async (): Promise<void> => {
  try {
    state.loading = true;
    await initAccessKeyService();
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

<style scoped>
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  justify-content: center;
  align-items: center;
  visibility: hidden;
}
.table-container {
  position: relative;
  display: inline-block;
}
</style>
