<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-12 d-flex justify-content-center">
        <h3>{{ t('component.acl.accesskey.frontend.title.main') }}</h3>
      </div>
    </div>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>
