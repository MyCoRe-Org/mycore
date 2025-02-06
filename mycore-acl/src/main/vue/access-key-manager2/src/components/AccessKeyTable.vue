<template>
  <table class="table table-striped table-hover">
    <thead>
      <tr>
        <th scope="col" class="col-3">{{ columnLabels.id }}</th>
        <th scope="col" class="col-5">{{ columnLabels.reference }}</th>
        <th scope="col" class="col-1">{{ columnLabels.permission }}</th>
        <th scope="col" class="col-1">{{ columnLabels.active }}</th>
        <th scope="col" class="col-1">{{ columnLabels.expiration }}</th>
        <th scope="col" class="col-1 text-end">{{ columnLabels.actions }}</th>
      </tr>
    </thead>
    <tbody>
      <tr
        v-for="(accessKey, index) in accessKeys"
        :key="accessKey.id"
        class="align-middle"
      >
        <td>{{ accessKey.id }}</td>
        <td>{{ accessKey.reference }}</td>
        <td>
          {{ accessKey.type }}
        </td>
        <td>
          {{ t(getI18nKey(accessKey.isActive ? 'button.yes' : 'button.no')) }}
        </td>
        <td>
          {{
            accessKey.expiration ? convertUnixToIso(accessKey.expiration) : '-'
          }}
        </td>
        <td class="text-end">
          <div class="btn-group">
            <button
              class="btn fa-btn"
              type="button"
              @click="viewAccessKey(index)"
            >
              <i class="fa fa-edit" />
            </button>
            <button
              class="btn fa-btn"
              type="button"
              @click="removeAccessKey(accessKey)"
            >
              <i class="fa fa-trash" />
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { MCRAccessKey } from '@golsch/test/acl/accesskey';
import { convertUnixToIso, getI18nKey } from '@/common/utils';

const { t } = useI18n();

defineProps<{
  accessKeys: MCRAccessKey[];
}>();

const emit = defineEmits<{
  (event: 'remove-access-key', accessKey: MCRAccessKey): void;
  (event: 'view-access-key', index: number): void;
}>();

const columnLabels = computed(
  (): Record<string, string> => ({
    id: t(getI18nKey('label.id')),
    reference: t(getI18nKey('label.reference')),
    permission: t(getI18nKey('label.permission')),
    active: t(getI18nKey('label.active')),
    expiration: t(getI18nKey('label.expiration')),
    actions: t(getI18nKey('label.actions')),
  })
);

const removeAccessKey = async (accessKey: MCRAccessKey): Promise<void> => {
  emit('remove-access-key', accessKey);
};
const viewAccessKey = (index: number): void => {
  emit('view-access-key', index);
};
</script>

<style lang="css">
.fa-btn {
  border: transparent !important;
}
</style>
