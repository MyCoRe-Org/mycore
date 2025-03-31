<template>
  <ManagerEntry :reference="reference" :page="page" :page-size="pageSize" />
</template>

<script setup lang="ts">
import ManagerEntry from '@/components/ManagerEntry.vue';

interface Props {
  reference?: string;
  page?: number;
  pageSize?: number;
}
defineProps<Props>();
</script>
