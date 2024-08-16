<template>
  <ManagerEntry
    :reference="reference"
    :permissions="permissions"
    :page="page"
    :page-size="pageSize"
  />
</template>

<script setup lang="ts">
import ManagerEntry from '@/components/ManagerEntry.vue';

interface Props {
  reference?: string;
  permissions?: string[];
  page?: number;
  pageSize?: number;
}
defineProps<Props>();
</script>
