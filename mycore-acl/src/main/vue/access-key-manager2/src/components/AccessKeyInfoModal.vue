<template>
  <div ref="infoModalElement" class="modal fade" tabindex="-1" role="dialog">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            {{ t(getI18nKey('title.viewAccessKey')) }}
          </h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
          ></button>
        </div>
        <div class="modal-body">
          <div
            v-if="errorMessage"
            class="alert alert-danger text-center"
            role="alert"
            aria-live="assertive"
          >
            {{ $t(errorMessage) }}
          </div>
          <form>
            <TextInputFormField
              v-model="form.reference"
              input-id="inputReference"
              :disabled="reference !== undefined"
              :label="t(getI18nKey('label.reference'))"
            />

            <div class="form-row">
              <SelectFormField
                v-if="availablePermissions.length > 0"
                v-model="form.type"
                class="col-md-6"
                input-id="inputPermission"
                :label="t(getI18nKey('label.permission'))"
              >
                <option
                  v-for="permissionValue in availablePermissions"
                  :key="permissionValue"
                  :value="permissionValue"
                >
                  {{ t(getI18nKey(`label.permission.${permissionValue}`)) }}
                </option>
              </SelectFormField>
              <TextInputFormField
                v-else
                v-model="form.type"
                class="col-md-6"
                input-id="inputPermission"
                :label="t(getI18nKey('label.permission'))"
              />
              <DateInputFormField
                v-model="form.expiration"
                class="col-md-6"
                input-id="expirationInput"
                :label="t(getI18nKey('label.expiration'))"
              />
            </div>
            <CheckboxInputFormField
              v-model="form.isActive"
              :label="t(getI18nKey('label.active'))"
              input-id="inputActive"
            />
            <TextareaFormField
              v-model="form.comment"
              input-id="commentTextarea"
              rows="3"
              :label="t(getI18nKey('label.comment'))"
            />
          </form>
        </div>
        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-primary"
            :disabled="isBusy || v.$invalid"
            @click="updateAccessKey"
          >
            <span
              v-if="isBusy"
              class="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            {{ t(getI18nKey('button.updateAccessKey')) }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { required } from '@vuelidate/validators';
import useVuelidate from '@vuelidate/core';
import { Modal } from 'bootstrap';
import { AccessKeyDto, PartialUpdateAccessKeyDto } from '@/dtos/accesskey';
import { AccessKeyService } from '@/service/accesskey';
import {
  convertUnixToISO,
  getI18nKey,
  getUnixTimestampString,
} from '@/common/utils';
import TextInputFormField from './form/TextInputFormField.vue';
import DateInputFormField from './form/DateInputFormField.vue';
import CheckboxInputFormField from './form/CheckboxInputFormField.vue';
import TextareaFormField from './form/TextareaFormField.vue';
import SelectFormField from './form/SelectFormField.vue';

interface FormData {
  reference: string;
  type: string;
  expiration: string | null;
  comment: string | undefined;
  isActive: boolean;
}

const { t } = useI18n();

const props = defineProps<{
  accessKeyService?: AccessKeyService;
  availablePermissions: string[];
  reference?: string;
}>();

const emit = defineEmits<{
  (event: 'update-access-key', accessKey: AccessKeyDto): void;
  (event: 'close'): void;
}>();

const infoModalElement = ref<HTMLDivElement | null>(null);
let modalInstance: Modal | null = null;
const accessKey = ref<AccessKeyDto>();

const errorMessage = ref<string | undefined>(undefined);
const isBusy = ref<boolean>(false);
const form = ref<FormData>({
  reference: '',
  type: '',
  isActive: false,
  comment: undefined,
  expiration: null,
});

const rules = {
  reference: { required },
  type: { required },
};

const v = useVuelidate(rules, form);
const handleError = (error: unknown): void => {
  errorMessage.value =
    error instanceof Error ? error.message : t(getI18nKey('error.fatal'));
};
const buildAccessKeyPayload = (): PartialUpdateAccessKeyDto => {
  const updatedAccessKey: PartialUpdateAccessKeyDto = {};
  if (form.value.reference !== accessKey.value?.reference) {
    updatedAccessKey.reference = form.value.reference;
  }
  if (form.value.comment !== accessKey.value?.comment) {
    updatedAccessKey.comment = form.value.comment;
  }
  if (form.value.type !== accessKey.value?.type) {
    updatedAccessKey.type = form.value.type;
  }
  const expiration = form.value.expiration
    ? getUnixTimestampString(form.value.expiration)
    : null;
  if (expiration !== accessKey.value?.expiration) {
    updatedAccessKey.expiration = expiration;
  }
  if (form.value.isActive !== accessKey.value?.isActive) {
    updatedAccessKey.isActive = form.value.isActive;
  }
  return updatedAccessKey;
};
const updateAccessKey = async (): Promise<void> => {
  if (props.accessKeyService && !isBusy.value && accessKey.value) {
    isBusy.value = true;
    v.value.$validate();
    if (!v.value.$invalid && accessKey.value && accessKey.value.id) {
      const partialUpdatedAccessKey: PartialUpdateAccessKeyDto =
        buildAccessKeyPayload();
      try {
        await props.accessKeyService.patchAccessKey(
          accessKey.value.id,
          partialUpdatedAccessKey
        );
        const updatedAccessKey = await props.accessKeyService.getAccessKey(
          accessKey.value.id
        );
        emit('update-access-key', updatedAccessKey);
        close(true);
      } catch (error) {
        handleError(error);
      } finally {
        isBusy.value = false;
      }
    } else {
      isBusy.value = false;
    }
  }
};
onMounted(() => {
  if (infoModalElement.value) {
    modalInstance = new Modal(infoModalElement.value, { backdrop: true });
  }
});
onErrorCaptured((err): boolean => {
  handleError(err);
  return false;
});
const open = (a: AccessKeyDto) => {
  accessKey.value = a;
  form.value = {
    reference: a.reference,
    type: a.type,
    expiration: a.expiration ? convertUnixToISO(a.expiration) : null,
    comment: a.comment,
    isActive: a.isActive,
  };
  modalInstance?.show();
};
const close = (force: boolean) => {
  if (force || !isBusy.value) {
    modalInstance?.hide();
  }
};
defineExpose({ open, close });
</script>
