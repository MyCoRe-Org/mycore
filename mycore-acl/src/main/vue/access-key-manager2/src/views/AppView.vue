<template>
  <!-- TODO fix loading overlay -->
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t('component.acl.accesskey.frontend.title.main') }}</h3>
      </div>
    </div>
    <AccessKeyManagerRouted
      :config="config"
      :auth-strategy="authStrategy"
    ></AccessKeyManagerRouted>
  </div>
</template>

<script setup lang="ts">
import {
  AccessKeyConfig,
  AccessKeyManagerRouted,
} from 'vue-access-key-manager';
import { useI18n } from 'vue-i18n';
import { inject } from 'vue';
import { AuthStrategy } from '@mycore-test/js-common/auth';

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
</script>
<style></style>
