<template>
  <b-modal id="access-key-modal" @show="show" :title="title" size="lg" ok-only scrollable>
    <div class="container-fluid">
      <div v-if="errorCode" class="alert alert-danger text-center" role="alert">
        {{ tc(`error.${errorCode}`) }}
      </div>
      <div v-if="infoCode" class="alert alert-success text-center" role="alert">
        {{ tc(`success.${infoCode}`) }}
      </div>
      <b-form>
        <b-form-group>
          <b-form-checkbox v-model="isActive.value" id="enabled-checkbox" switch @change="change">
            {{ (isActive.value ) ? tc('label.state.enabled') : tc('label.state.disabled') }}
          </b-form-checkbox>
        </b-form-group>
        <b-form-row>
          <b-col cols="4">
            <b-form-group>
              <template #label>
                {{ tc('label.type') }}
                <b-link id="popover-type">
                  <font-awesome-icon icon="info-circle" class="text-secondary" />
                </b-link>
                <b-popover target="popover-type" :title="tc('title.popover')" triggers="hover">
                  <span v-html="tc('popover.type')" />
                </b-popover>
              </template>
              <b-form-select v-bind="type" id="type-select" :options="options" />
            </b-form-group>
          </b-col>
          <b-col cols="8">
            <b-form-group>
              <template #label>
                {{ tc('label.expiration') }}
                <b-link id="popover-expiration">
                  <font-awesome-icon icon="info-circle" class="text-secondary" />
                </b-link>
                <b-popover target="popover-expiration" :title="tc('title.popover')" triggers="hover">
                  <span v-html="tc('popover.expiration')" />
                </b-popover>
              </template>
              <b-form-datepicker v-bind="expiration" :reset-button=true placeholder="-" />
            </b-form-group>
          </b-col>
        </b-form-row>
        <b-form-group :label="tc('label.comment')"
          label-for="textarea-comment">
          <b-form-textarea v-bind="comment" id="textarea-comment" rows="3" placeholder="-"/>
        </b-form-group>
        <div class="text-right">
          <!-- TODO i18 n -->
          <b-button :disabled="!meta.dirty" v-on:click="updateAccessKey()" variant="primary">
            <font-awesome-icon icon="save" />
              Save
          </b-button>
        </div>
      </b-form>
      <hr class="my-3">
      <b-row>
        <b-col cols="3">
          {{ tc('label.created') }}:
        </b-col>
        <b-col>
          {{ props.accessKey.createdBy }} ({{ new Date(props.accessKey.created).toLocaleString() }})
        </b-col>
      </b-row>
      <b-row>
        <b-col cols="3">
          {{ tc('label.lastModified') }}:
        </b-col>
        <b-col>
          {{ props.accessKey.lastModifiedBy }} ({{ new Date(props.accessKey.lastModified).toLocaleString() }})
        </b-col>
      </b-row>
    </div>
  </b-modal>
</template>
<script setup lang="ts">
import {
  inject,
  ref,
  onErrorCaptured,
  nextTick,
} from 'vue';
import { useI18n } from 'vue-i18n';
import { useForm } from 'vee-validate';
import {
  BButton,
  BCol,
  BForm,
  BFormCheckbox,
  BFormDatepicker,
  BFormGroup,
  BFormRow,
  BFormSelect,
  BFormTextarea,
  BLink,
  BModal,
  BPopover,
  BRow,
} from 'bootstrap-vue';
import { updateDerivateAccessKey, updateObjectAccessKey } from '@/api/service';
import { getI18nKey } from '@/utils';
import { objectIdKey, derivateIdKey } from '@/keys';

const objectId = inject(objectIdKey);
const derivateId = inject(derivateIdKey);
const props = defineProps({
  accessKey: Object,
});
const emit = defineEmits(['accessKeyUpdated']);
const { t } = useI18n();
const tc = (key: string, obj?) => t(getI18nKey(key), obj);
const options = [
  { value: 'read', text: tc('label.type.read') },
  { value: 'writedb', text: tc('label.type.writedb') },
];
const errorCode = ref();
const infoCode = ref();
const title = ref();
const {
  meta,
  setFieldValue,
  handleSubmit,
  resetForm,
  defineInputBinds,
} = useForm();
const show = async () => {
  await nextTick();
  title.value = props.accessKey.secret;
  const expiration = props.accessKey.expiration ? new Date(props.accessKey.expiration) : null;
  errorCode.value = null;
  infoCode.value = null;
  resetForm(
    {
      values: {
        secret: props.accessKey.secret,
        type: props.accessKey.type,
        isActive: props.accessKey.isActive,
        comment: props.accessKey.comment,
        expiration,
      },
    },
  );
};
const handleError = (code) => {
  errorCode.value = code;
};
const handleInfo = (code) => {
  infoCode.value = code;
};
const change = (value) => {
  setFieldValue('isActive', value);
};
const updateAccessKey = handleSubmit(async (values) => {
  const result = values;
  if (values.expiration) {
    result.expiration = new Date(values.expiration).valueOf();
  }
  if (!derivateId) await updateObjectAccessKey(objectId, result);
  else await updateDerivateAccessKey(objectId, derivateId, result);
  handleInfo('update');
  emit('accessKeyUpdated', result);
});
onErrorCaptured((err) => {
  handleError(err.message);
  return false;
});
const isActive = defineInputBinds('isActive');
const type = defineInputBinds('type');
const expiration = defineInputBinds('expiration');
const comment = defineInputBinds('comment');
</script>
