<template>
  <div class="row">
    <div class="col-12">
      <AccessKeyManager
        :base-url="appConfig.baseUrl"
        :auth-strategy="authStrategy"
        :reference="reference"
        :permissions="permissions"
        :allowed-session-permissions="
          accessKeyConfig.allowedAccessKeySessionPermissions
        "
        :current-page="page"
        :page-size="pageSize"
        router-enabled
        @error="handleError"
      ></AccessKeyManager>
    </div>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { AccessKeyManager } from '@mycore-org/vue-access-key-manager';
import { AccessKeyConfig, AppConfig } from '@/config/types';
import { AuthStrategy } from '@jsr/mycore__js-common/auth';
import { AppConfigKey, AccessKeyConfigKey } from '@/keys';

interface Props {
  reference?: string;
  permissions?: string[];
  page?: number;
  pageSize?: number;
}
defineProps<Props>();

const appConfig = inject(AppConfigKey) as AppConfig;
const accessKeyConfig = inject(AccessKeyConfigKey) as AccessKeyConfig;
const authStrategy: AuthStrategy | undefined = import.meta.env.DEV
  ? new (class implements AuthStrategy {
      async getHeaders(): Promise<Record<string, string>> {
        return {
          Authorization: `Basic ${import.meta.env.VITE_APP_API_TOKEN}`,
        };
      }
    })()
  : undefined;

const handleError = (error: unknown) => {
  throw error;
};
</script>
<style></style>
