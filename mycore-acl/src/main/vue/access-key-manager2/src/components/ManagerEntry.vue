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
import {
  AccessKeyConfig,
  AccessKeyManager,
} from '@mycore-org/vue-access-key-manager';
import { inject } from 'vue';
import { AuthStrategy } from '@mycore-test/js-common/auth';
import { AppConfig } from '@mycore-org/vue-components';

interface Props {
  reference?: string;
  permissions?: string[];
  page?: number;
  pageSize?: number;
}
defineProps<Props>();

const accessKeyConfig = inject('accessKeyConfig') as AccessKeyConfig;
const appConfig = inject('appConfig') as AppConfig;
const authStrategy: AuthStrategy | undefined = import.meta.env.DEV
  ? new (class implements AuthStrategy {
      getHeaders(): Record<string, string> {
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
