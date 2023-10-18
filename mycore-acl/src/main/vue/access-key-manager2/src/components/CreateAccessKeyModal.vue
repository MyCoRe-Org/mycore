<template>
  <b-modal id="create-access-key-modal" :title="tc('title.add')" @hidden="reset" @show="reset" hide-footer>
    <div class="container-fluid">
      <div v-if="errorCode" class="alert alert-danger text-center" role="alert">
        {{ tc(`error.${errorCode}`) }}
      </div>
      <b-form @submit="onSubmit">
        <b-form-row>
          <b-col cols="8">
            <b-form-group>
              <template #label>
                {{ tc('label.secret') }}
                <b-link id="popover-secret">
                  <font-awesome-icon icon="info-circle" class="text-secondary" />
                </b-link>
                <b-popover target="popover-secret" :title="tc('title.popover')" triggers="hover">
                  <span v-html="tc('popover.secret')" />
                </b-popover>
              </template>
              <b-input-group>
                <template #prepend>
                  <b-button v-on:click="generateSecret()" variant="primary">
                    <font-awesome-icon icon="random" size="xs" />
                  </b-button>
                </template>
                  <b-form-input v-bind="secret" id="input-secret" :state="errors.secret ? false : null"
                    aria-describedby="input-secret-feedback" />
              </b-input-group>
              <b-form-invalid-feedback id="input-secret-feedback" :state="errors.secret ? false : null">
                {{ errors.secret }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
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
              <b-form-select v-bind="type" id="type-select" :options="options" :state="errors.type ? false : null" />
              <b-form-invalid-feedback id="type-secret-feedback" :state="errors.type ? false : null">
                {{ errors.type }}
              </b-form-invalid-feedback>
            </b-form-group>
          </b-col>
        </b-form-row>
        <hr class="my-3">
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
          <b-form-datepicker v-bind="expiration" :locale="locale" :value-as-date=true :reset-button=true
            placeholder="-" />
        </b-form-group>
        <b-form-group :label="t('label.comment')" label-for="textarea-comment">
          <b-form-textarea v-bind="comment" id="textarea-comment" rows="3" placeholder="-"/>
        </b-form-group>
        <div class="row">
          <div class="col text-right">
            <b-button :disabled="!meta.valid" type="submit" variant="primary">OK</b-button>
          </div>
        </div>
      </b-form>
    </div>
  </b-modal>
</template>

<script setup lang="ts">
import {
  Component,
  ref,
  getCurrentInstance,
  onErrorCaptured,
} from 'vue';
import { useApplicationStore } from '@/stores';
import { useI18n } from 'vue-i18n';
import { useForm } from 'vee-validate';
import {
  BButton,
  BCol,
  BForm,
  BFormDatepicker,
  BFormGroup,
  BFormInput,
  BFormInvalidFeedback,
  BFormRow,
  BFormSelect,
  BFormTextarea,
  BInputGroup,
  BLink,
  BModal,
  BPopover,
  BvModal,
} from 'bootstrap-vue';
import * as yup from 'yup';
import { getI18nKey, generateRandomString } from '@/utils';

const emit = defineEmits(['accessKeyCreated']);
const { t } = useI18n();
const tc = (key: string, obj?) => t(getI18nKey(key), obj);
const store = useApplicationStore();
const errorCode = ref();
const {
  errors,
  meta,
  setFieldValue,
  handleSubmit,
  resetForm,
  defineInputBinds,
} = useForm({
  validationSchema: yup.object({
    secret: yup.string().required(tc('validation.secret')),
    type: yup.string().required(tc('validation.type')),
  }),
});
const options = [
  { value: 'read', text: tc('label.type.read') },
  { value: 'writedb', text: tc('label.type.writedb') },
];
const generateSecret = () => {
  setFieldValue('secret', generateRandomString(16));
};
const reset = () => {
  errorCode.value = null;
  resetForm();
};
const handleError = (code) => {
  errorCode.value = code;
};
const instance: Component = getCurrentInstance();
const onSubmit = handleSubmit(async (values) => {
  const bvModal = instance.ctx._bv__modal as BvModal;
  const reference = await store.createAccessKey(values);
  bvModal.hide('create-access-key-modal');
  emit('accessKeyCreated', values.secret, reference);
});
onErrorCaptured((err) => {
  handleError(err.message);
  return false;
});
const secret = defineInputBinds('secret');
const type = defineInputBinds('type');
const expiration = defineInputBinds('expiration');
const comment = defineInputBinds('comment');
</script>
