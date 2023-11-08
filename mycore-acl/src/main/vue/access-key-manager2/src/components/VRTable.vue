<template>
  <b-table :fields="fields" :items="items" sort-icon-left responsive striped>
    <template #table-caption>
    </template>
    <template #cell(edit)="data">
      <div class="btn-group">
        <b-link v-on:click="showItem(data.item)" class="pr-1">
          <font-awesome-icon icon="eye" class="text-primary" />
        </b-link>
        <b-link v-on:click="removeItem(data.item)" class="pl-1">
          <font-awesome-icon icon="trash" />
        </b-link>
      </div>
    </template>
  </b-table>
</template>
<script setup lang="ts">
import { BLink, BTable } from 'bootstrap-vue';

defineProps({
  fields: Array,
  items: Array,
});
const emit = defineEmits(['removeItem', 'showItem']);
const showItem = (item) => {
  emit('showItem', item);
};
const removeItem = (item) => {
  emit('removeItem', item);
};
</script>
