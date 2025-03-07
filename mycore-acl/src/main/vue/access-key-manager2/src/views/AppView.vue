<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col d-flex justify-content-center">
        <h3>{{ t(getI18nKey('title.main')) }}</h3>
      </div>
    </div>
    <AccessKeyManager :config="config"></AccessKeyManager>
  </div>
</template>

<script setup lang="ts">
import { AccessKeyConfig } from '@/common/config';
import { useI18n } from 'vue-i18n';
import AccessKeyManager from '@/components/AccessKeyManager.vue';
import { getI18nKey } from '@/common/utils';
import { inject } from 'vue';

const { t } = useI18n();

const config = inject('accessKeyConfig') as AccessKeyConfig;
</script>
<style></style>
