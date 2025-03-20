<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t('component.acl.accesskey.frontend.title.main') }}</h3>
      </div>
    </div>
    <AccessKeyManager
      :config="config"
      :auth-strategy="authStrategy"
      :reference="reference"
      :permissions="permissions"
      :current-page="page"
      :page-size="pageSize"
      router-enabled
      @error="handleError"
    ></AccessKeyManager>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  AccessKeyConfig,
  AccessKeyManager,
} from '@mycore-org/vue-access-key-manager';
import { AuthStrategy } from '@mycore-test/js-common/auth';

interface Props {
  reference?: string;
  permissions?: string[];
  page?: number;
  pageSize?: number;
}
defineProps<Props>();

const { t } = useI18n();
const config = inject('accessKeyConfig') as AccessKeyConfig;
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
