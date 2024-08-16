<template>
  <BaseModal :show="showModal" :title="$t('component.acl.accesskey.frontend.title.add')"
    ok-only scrollable @close="handleClose" :busy="busy">
    <div class="container-fluid">
      <div v-if="errorMessage" class="alert alert-danger text-center" role="alert">
        {{ $t(errorMessage) }}
      </div>
      <div>
        <p>{{ $t('component.acl.accesskey.frontend.description.createAccesskey') }}</p>
      </div>
      <form>
        <div class="form-group required">
          <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
          <label for="inputValue">
            {{ $t('component.acl.accesskey.frontend.label.value') }}
          </label>
          <div class="input-group">
            <div class="input-group-prepend">
              <button class="btn btn-primary" type="button" @click="generateValue">
                <i class="fa fa-shuffle"></i>
              </button>
            </div>
            <input type="text" class="form-control" v-model="value" id="inputValue"
               :class="v.value.$error? 'is-invalid' : ''" placeholder="Value">
          </div>
        </div>
        <div class="form-row">
          <div class="form-group col-md-6 required">
            <!-- eslint-disable-next-line vuejs-accessibility/label-has-for -->
            <label for="inputPermission">
              {{ $t('component.acl.accesskey.frontend.label.permission') }}
            </label>
            <select id="inputPermission" class="form-control" v-model="permission"
              :class="v.permission.$error? 'is-invalid' : ''">
              <option value="" disabled>{{ $t('component.acl.accesskey.frontend.select') }}</option>
              <option v-if="configStore.manageReadAccesskeys" value="read">
                {{ $t('component.acl.accesskey.frontend.label.type.read') }}
              </option>
              <option v-if="configStore.manageWriteAccessKeys" value="writedb">
                {{ $t('component.acl.accesskey.frontend.label.type.writedb') }}
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
            {{ $t('component.acl.accesskey.frontend.label.comment')}}
          </label>
          <textarea class="form-control" id="commentTextarea" rows="3" v-model="comment"></textarea>
        </div>
      </form>
    </div>
    <template v-slot:footer>
      <button type="button" class="btn btn-primary" @click="handleCreateAccessKey" :disabled="busy">
        <span v-if="busy" class="spinner-border spinner-border-sm" role="status"
          aria-hidden="true"></span>
        {{ $t('component.acl.accesskey.frontend.button.createAccessKey') }}
      </button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import {
  computed,
  inject,
  ref,
  onErrorCaptured,
} from 'vue';
import { createAccessKey, getAccessKey } from '@/api/service';
import { generateRandomString } from '@/utils';
import { useConfigStore } from '@/stores';
import AccessKeyDto from '@/dtos/AccessKeyDto';
import objectIdKey from '@/keys';
import useVuelidate from '@vuelidate/core';
import { required } from '@vuelidate/validators';
import BaseModal from '@/components/BaseModal.vue';

defineProps({
  showModal: {
    type: Boolean,
    default: false,
  },
});

const rules = computed(() => ({
  permission: {
    required,
  },
  value: {
    required,
  },
}));

const configStore = useConfigStore();
const objectId: string | undefined = inject(objectIdKey);
const emit = defineEmits(['access-key-created', 'close']);

const errorMessage = ref<string>();

const value = ref<string>();
const permission = ref<string>('');
const comment = ref<string>();
const expiration = ref<Date>();
const busy = ref<boolean>();

const v = useVuelidate(rules, {
  value,
  permission,
});
const handleError = (error: unknown) => {
  if (error instanceof Error) {
    errorMessage.value = error.message;
  } else {
    errorMessage.value = 'component.acl.accesskey.frontend.error.fatal';
  }
};
const resetFrom = () => {
  errorMessage.value = undefined;
  value.value = undefined;
  permission.value = '';
  comment.value = undefined;
  expiration.value = undefined;
  v.value.$reset();
};
const handleClose = () => {
  if (!busy.value) {
    resetFrom();
    emit('close');
  }
};
const handleCreateAccessKey = async () => {
  if (objectId && !busy.value) {
    busy.value = true;
    v.value.$validate();
    if (!v.value.$error) {
      try {
        const accessKeyDto: AccessKeyDto = {
          objectId,
          value: value.value as string,
          permission: permission.value as string,
        };
        if (expiration.value) {
          accessKeyDto.expiration = Math.floor(new Date(expiration.value).getTime());
        }
        if (comment.value) {
          accessKeyDto.comment = comment.value.trim();
        }
        const createdValue = await createAccessKey(accessKeyDto);
        const createdAccessKey = await getAccessKey(objectId, createdValue);
        busy.value = false;
        emit('access-key-created', value.value as string, createdAccessKey);
        handleClose();
      } catch (error) {
        handleError(error);
      } finally {
        busy.value = false;
      }
    } else {
      busy.value = false;
    }
  }
};
const generateValue = () => {
  value.value = generateRandomString(16);
};
onErrorCaptured((err) => {
  handleError(err.message);
  return false;
});
</script>
<style scoped>
.form-group.required label:after {
  content:"*";
  color:black;
}
</style>
