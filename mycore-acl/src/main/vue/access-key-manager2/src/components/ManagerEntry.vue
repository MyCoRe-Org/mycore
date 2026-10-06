<template>
  <div class="row">
    <div class="col-12">
      <AccessKeyManager
        :reference="reference"
        :permissions="permissions"
        :current-page="page"
        :page-size="pageSize"
        router-enabled
        @error="handleError"
      ></AccessKeyManager>
    </div>
  </div>
</template>

<script setup lang="ts">
import { AccessKeyManager } from '@mycore-org/vue-access-key-manager';

interface Props {
  reference?: string;
  permissions?: string[];
  page?: number;
  pageSize?: number;
}
defineProps<Props>();

const handleError = (error: unknown) => {
  throw error;
};
</script>
<style></style>
