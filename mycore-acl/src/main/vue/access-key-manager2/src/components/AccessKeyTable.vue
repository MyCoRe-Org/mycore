<template>
  <table class="table">
    <colgroup>
      <col style="width: 30%" />
      <col style="width: 35%" />
      <col style="width: 10%" />
      <col style="width: 10%" />
      <col style="width: 10%" />
      <col style="width: 5%" />
    </colgroup>
    <thead>
      <tr>
        <th>{{ columnLabels.id }}</th>
        <th>{{ columnLabels.reference }}</th>
        <th>{{ columnLabels.permission }}</th>
        <th>{{ columnLabels.active }}</th>
        <th>{{ columnLabels.expiration }}</th>
        <th>{{ columnLabels.actions }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(accessKey, index) in accessKeys" :key="accessKey.id">
        <td>{{ accessKey.id }}</td>
        <td>{{ accessKey.reference }}</td>
        <td>
          {{ t(getI18nKey(`label.permission.${accessKey.type}`)) }}
        </td>
        <td>{{ accessKey.isActive }}</td>
        <td>
          {{ getExpirationDisplay(accessKey.expiration) }}
        </td>
        <td>
          <div class="btn-group">
            <button class="btn shadow-none" @click="viewAccessKey(index)">
              <i class="fa fa-eye" />
            </button>
            <button class="btn shadow-none" @click="removeAccessKey(accessKey)">
              <i class="fa fa-trash" />
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
  <ConfirmModal ref="confirmModal" />
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { AccessKeyDto } from '@/dtos/accesskey';
import { getI18nKey } from '@/common/utils';
import ConfirmModal from '@/components/ConfirmModal.vue';

const { t } = useI18n();
const confirmModal = ref();

defineProps<{
  accessKeys: AccessKeyDto[];
}>();

const emit = defineEmits<{
  (event: 'remove-access-key', accessKey: string): void;
  (event: 'view-access-key', index: number): void;
}>();

const columnLabels = computed(() => ({
  id: t(getI18nKey('label.id')),
  reference: t(getI18nKey('label.reference')),
  permission: t(getI18nKey('label.permission')),
  active: t(getI18nKey('label.active')),
  expiration: t(getI18nKey('label.expiration')),
  actions: t(getI18nKey('label.actions')),
}));

const getExpirationDisplay = (expiration?: number | null): string => {
  return expiration ? new Date(expiration).toLocaleDateString() : '-';
};
const openDeleteConfirmationModal = async (
  accessKey: AccessKeyDto
): Promise<boolean> => {
  const secretPreview =
    accessKey.id.length > 30
      ? `${accessKey.id.slice(0, 27)}...`
      : accessKey.secret;
  return await confirmModal.value.show({
    title: t('component.acl.accesskey.frontend.confirmRemove.title'),
    message: t('component.acl.accesskey.frontend.confirmRemove.text', {
      secret: secretPreview,
    }),
  });
};
const removeAccessKey = async (accessKey: AccessKeyDto): Promise<void> => {
  const confirmed = await openDeleteConfirmationModal(accessKey);
  if (confirmed) {
    emit('remove-access-key', accessKey.id);
  }
};
const viewAccessKey = (index: number): void => {
  emit('view-access-key', index);
};
</script>
