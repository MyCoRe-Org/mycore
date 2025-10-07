<template>
  <div class="container-fluid">
    <div class="row">
      <div class="col-12 d-flex justify-content-center">
        <h3>
          {{ t('component.acl.accesskey.frontend.title.main') }}

          <span v-if="reference" class="fw-bold ps-1">({{ reference }})</span>
        </h3>
      </div>
    </div>
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute } from 'vue-router';

const route = useRoute();
const reference = computed(() => route.params.reference);

const { t } = useI18n();
</script>
