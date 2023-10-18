<template>
  <b-table :fields="fields" :items="accessKeys" sort-icon-left responsive striped>
    <template #table-caption>
    </template>
    <template #cell(edit)="data">
      <div class="btn-group">
        <b-link v-b-modal.access-key-modal v-on:click="viewAccessKey(data.item)" class="pr-1">
          <font-awesome-icon icon="eye" class="text-primary" />
        </b-link>
        <b-link v-on:click="removeAccessKey(data.item.secret)" class="pl-1">
          <font-awesome-icon icon="trash" />
        </b-link>
      </div>
    </template>
  </b-table>
  <access-key-modal />
</template>

<script setup lang="ts">
import {
  Component,
  computed,
  getCurrentInstance,
  onErrorCaptured,
} from 'vue';
import { useI18n } from 'vue-i18n';
import { useApplicationStore } from '@/stores';
import { BLink, BTable, BvModal } from 'bootstrap-vue';
import { AccessKey, getI18nKey, shortReference } from '@/utils';
import AccessKeyModal from '@/components/AccessKeyModal.vue';

const { t } = useI18n();
const tc = (key: string, obj?) => t(getI18nKey(key), obj);
const store = useApplicationStore();
const accessKeys: AccessKey[] = computed(() => store.accessKeys);
const fields = [
  {
    key: 'secret',
    label: tc('label.reference'),
    thClass: 'col-3 text-center',
    tdClass: 'col-3 align-middle',
  },
  {
    key: 'isActive',
    formatter: (value) => tc(`label.state.${(value) ? 'en' : 'dis'}abled`),
    label: tc('label.state'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'type',
    formatter: (value) => tc(`label.type.${value}`),
    label: tc('label.type'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'expiration',
    formatter: (value) => ((value != null) ? new Date(value).toLocaleDateString() : '-'),
    label: tc('label.expiration'),
    thClass: 'col-1 text-center',
    tdClass: 'col-1 text-center align-middle',
  },
  {
    key: 'comment',
    formatter: (value) => {
      if (value != null && value.length > 50) return `${value.substring(0, 50)}...`;
      if (value != null && value.length > 0) return value;
      return '-';
    },
    label: tc('label.comment'),
    thClass: 'col text-center',
    tdClass: 'col text-center',
  },
  {
    key: 'edit',
    label: '',
    thClass: 'col-1 text-right',
    tdClass: 'col-1 text-right align-middle',
  },
];
const instance: Component = getCurrentInstance();
const handleError = (error) => {
  const bvModal = instance.ctx._bv__modal as BvModal;
  bvModal.msgBoxOk(tc(`error.${error}`), {
    title: tc('title.alert'),
    okVariant: 'danger',
  });
};
const viewAccessKey = (accessKey: AccessKey) => {
  store.modalData = accessKey;
};
const removeAccessKey = async (reference: string) => {
  const bvModal = instance.ctx._bv__modal as BvModal;
  const value = await bvModal.msgBoxConfirm(tc('text.remove2',
    { reference: shortReference(reference) }), {
    title: tc('title.remove'),
    okTitle: tc('button.yes'),
    cancelTitle: tc('button.no'),
    noCloseOnBackdrop: true,
    noCloseOnEsc: true,
    hideBackdrop: true,
    okVariant: 'danger',
  });
  if (value) {
    try {
      await store.removeAccessKey(reference);
      bvModal.msgBoxOk(tc('success.remove'), {
        title: tc('title.remove'),
      });
    } catch (error) {
      handleError(error.message);
    }
  }
};
onErrorCaptured((err) => {
  handleError(err);
  return false;
});
</script>
