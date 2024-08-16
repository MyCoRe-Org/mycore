<template>
  <BaseModal v-if="accessKey" :show="showModal"
    :title="$t('component.acl.accesskey.frontend.title.viewAccessKey')"
    ok-only scrollable @close="handleClose" :busy="busy">
    <div v-if="errorMessage" class="alert alert-danger text-center" role="alert">
      {{ $t(errorMessage) }}
    </div>
    <form>
      <div class="form-group">
        <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
        <label for="inputValue">
          {{ $t('component.acl.accesskey.frontend.label.value') }}
        </label>
        <input type="text" class="form-control" v-bind:value="accessKey?.value" id="inputValue"
          disabled>
      </div>
      <div class="form-row">
        <div class="form-group col-md-6">
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
          <label for="inputPermission">
            {{ $t('component.acl.accesskey.frontend.label.permission') }}
          </label>
          <select id="inputPermission" class="form-control" v-model="permission">
            <option v-if="configStore.manageReadAccesskeys" value="read">
              {{ $t('component.acl.accesskey.frontend.label.permission.read') }}
            </option>
            <option v-if="configStore.manageWriteAccessKeys" value="writedb">
              {{ $t('component.acl.accesskey.frontend.label.permission.writedb') }}
            </option>
          </select>
        </div>
        <div class="form-group col-md-6">
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
          <label for="expirationInput">
            {{ $t('component.acl.accesskey.frontend.label.expiration') }}
          </label>
          <input type="date" class="form-control" id="expirationInput" v-model="expiration">
        </div>
      </div>
      <div class="form-group">
        <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
        <label for="commentTextarea">
          {{ $t('component.acl.accesskey.frontend.label.comment') }}
        </label>
        <textarea class="form-control" id="commentTextarea" rows="3"
          v-model="comment"></textarea>
      </div>
    </form>
    <template v-slot:footer>
      <button type="button" class="btn btn-primary" @click="handleUpdateAccessKey" :disabled="busy">
        <span v-if="busy" class="spinner-border spinner-border-sm" role="status"
          aria-hidden="true"></span>
          {{ $t('component.acl.accesskey.frontend.button.updateAccessKey') }}
      </button>
    </template>
  </BaseModal>
</template>
<script setup lang="ts">
import {
  ref,
  onErrorCaptured,
  PropType,
  inject,
  watch,
} from 'vue';
import AccessKeyDto from '@/dtos/AccessKeyDto';
import BaseModal from '@/components/BaseModal.vue';
import objectIdKey from '@/keys';
import { useConfigStore } from '@/stores';
import { getAccessKey, patchAccessKey } from '@/api/service';

const props = defineProps({
  showModal: {
    type: Boolean,
    default: false,
  },
  accessKey: {
    type: Object as PropType<AccessKeyDto>,
  },
});
const emit = defineEmits(['access-key-updated', 'close']);
const configStore = useConfigStore();
const objectId: string | undefined = inject(objectIdKey);

const errorMessage = ref<string>();
const busy = ref<boolean>(false);

const permission = ref<string>();
const expiration = ref<string>();
const comment = ref<string>();

watch(() => props.accessKey, (newAccessKey) => {
  permission.value = newAccessKey?.permission;
  if (newAccessKey?.expiration) {
    expiration.value = new Date(newAccessKey.expiration).toISOString().slice(0, 10);
  } else {
    expiration.value = undefined;
  }
  comment.value = newAccessKey?.comment;
}, { deep: true });
const handleError = (error: unknown) => {
  if (error instanceof Error) {
    errorMessage.value = error.message;
  } else {
    errorMessage.value = 'component.acl.accesskey.frontend.error.fatal';
  }
};
const handleClose = () => {
  if (!busy.value) {
    emit('close');
  }
};
onErrorCaptured((err) => {
  handleError(err);
  return false;
});
const handleUpdateAccessKey = async () => {
  if (objectId && props.accessKey?.id && !busy.value) {
    busy.value = true;
    try {
      const accessKeyDto: AccessKeyDto = {};
      if (comment.value !== props.accessKey.comment) {
        accessKeyDto.comment = comment.value;
      }
      if (permission.value !== props.accessKey.permission) {
        accessKeyDto.permission = permission.value;
      }
      if (expiration.value !== props.accessKey.expiration) {
        if (expiration.value) {
          accessKeyDto.expiration = Math.floor(new Date(expiration.value).getTime());
        } else {
          // json.stringify removes undefied props, so use null as workaround
          accessKeyDto.expiration = null;
        }
      }
      await patchAccessKey(props.accessKey.id, accessKeyDto);
      const updatedAccessKey = await getAccessKey(props.accessKey.id);
      busy.value = false;
      emit('access-key-updated', props.accessKey.value, updatedAccessKey);
      handleClose();
    } catch (error) {
      handleError(error);
    } finally {
      busy.value = false;
    }
  } else {
    busy.value = false;
  }
};
</script>
