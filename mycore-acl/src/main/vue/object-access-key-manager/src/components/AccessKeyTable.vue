<template>
  <table class="table">
    <colgroup>
      <col style="width: 75%">
      <col style="width: 10%">
      <col style="width: 10%">
      <col style="width: 5%">
    </colgroup>
    <thead>
      <tr>
        <th>{{ $t('component.acl.accesskey.frontend.label.value') }}</th>
        <th>{{ $t('component.acl.accesskey.frontend.label.permission') }}</th>
        <th>{{ $t('component.acl.accesskey.frontend.label.expiration') }}</th>
        <th>{{ $t('component.acl.accesskey.frontend.label.actions') }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(accessKey, index) in accessKeys" :key='index'>
        <td>{{ accessKey.value }}</td>
        <td>{{ accessKey.permission }}</td>
        <td>{{ ((accessKey.expiration) ?
          new Date(accessKey.expiration).toLocaleDateString() : '-') }}</td>
        <td>
          <div class="btn-group">
            <button class="btn shadow-none" @click="viewAccessKey(index)">
              <i class="fa fa-eye"></i>
            </button>
            <button class="btn shadow-none" @click="removeAccessKey(index)">
              <i class="fa fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script setup lang="ts">
import { PropType } from 'vue';
import AccessKeyDto from '@/dtos/AccessKeyDto';

defineProps({
  accessKeys: {
    type: Object as PropType<AccessKeyDto[]>,
    required: true,
  },
});
const emit = defineEmits(['remove-access-key', 'update-access-key', 'view-access-key']);
const removeAccessKey = (index: number) => {
  emit('remove-access-key', index);
};
const viewAccessKey = (index: number) => {
  emit('view-access-key', index);
};
</script>
