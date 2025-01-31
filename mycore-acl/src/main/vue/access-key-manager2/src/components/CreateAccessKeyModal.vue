<template>
  <div ref="createModalElement" class="modal fade" tabindex="-1" role="dialog">
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
        <div
          v-if="errorMessage"
          class="alert alert-danger text-center"
          role="alert"
          aria-live="polite"
        >
          {{ t(errorMessage) }}
        </div>
        <div>
          <p>{{ t(getI18nKey('description.createAccesskey')) }}</p>
        </div>
        <form>
          <TextInputFormField
            v-model="form.reference"
            input-id="inputReference"
            :disabled="reference !== undefined"
            :label="t(getI18nKey('label.reference'))"
            required
          />

          <TextInputFormField
            v-model="form.secret"
            input-id="inputSecret"
            :label="t(getI18nKey('label.value'))"
            required
          >
            <template #prepend>
              <span class="input-group-prepend">
                <button
                  class="btn btn-primary"
                  type="button"
                  aria-label="generate secret"
                  @click="generateSecret"
                >
                  <i class="fa fa-shuffle" />
                </button>
              </span>
            </template>
          </TextInputFormField>
          <div class="form-row">
            <SelectFormField
              v-if="availablePermissions.length > 0"
              v-model="form.type"
              class="col-md-6"
              input-id="inputPermission"
              :label="t(getI18nKey('label.permission'))"
              required
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
              required
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
        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-primary"
            :disabled="isBusy || v.$invalid"
            @click="handleCreateAccessKey"
          >
            <span
              v-if="isBusy"
              class="spinner-border spinner-border-sm"
              role="status"
              aria-hidden="true"
            />
            {{ t(getI18nKey('button.createAccessKey')) }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import useVuelidate from '@vuelidate/core';
import { required } from '@vuelidate/validators';
import {
  generateRandomString,
  getI18nKey,
  getUnixTimestampString,
} from '@/common/utils';
import { AccessKeyDto, CreateAccessKeyDto } from '@/dtos/accesskey';
import { AccessKeyService } from '@/service/accesskey';
import TextInputFormField from './form/TextInputFormField.vue';
import SelectFormField from './form/SelectFormField.vue';
import DateInputFormField from './form/DateInputFormField.vue';
import CheckboxInputFormField from './form/CheckboxInputFormField.vue';
import TextareaFormField from './form/TextareaFormField.vue';
import { Modal } from 'bootstrap';

const { t } = useI18n();

const props = defineProps<{
  accessKeyService?: AccessKeyService;
  reference?: string;
  availablePermissions: string[];
}>();

const emit = defineEmits<{
  (event: 'add-access-key', secret: string, accessKey: AccessKeyDto): void;
}>();

const createModalElement = ref<HTMLDivElement | null>(null);
let modalInstance: Modal | null = null;

const rules = {
  reference: {
    required,
  },
  type: {
    required,
  },
  secret: {
    required,
  },
};

const errorMessage = ref<string>();
const isBusy = ref<boolean>(false);
const defaultForm = {
  reference: props.reference !== undefined ? props.reference : '',
  isActive: true,
  secret: '',
  type: '',
  comment: undefined,
  expiration: null,
};
const form = ref<CreateAccessKeyDto>({ ...defaultForm });
const v = useVuelidate(rules, form);
const handleError = (error: unknown): void => {
  errorMessage.value =
    error instanceof Error ? error.message : t(getI18nKey('error.fatal'));
};
const resetForm = (): void => {
  form.value = { ...defaultForm };
  v.value.$reset();
};
const resetModal = (): void => {
  errorMessage.value = undefined;
  resetForm();
};
const validateForm = async (): Promise<boolean> => {
  v.value.$validate();
  if (v.value.$invalid) {
    return false;
  }
  return true;
};
const getAccessKey = (): CreateAccessKeyDto => {
  const accessKey = {
    reference: form.value.reference,
    secret: form.value.secret,
    isActive: form.value.isActive,
    type: form.value.type,
  } as CreateAccessKeyDto;
  if (form.value.expiration) {
    accessKey.expiration = getUnixTimestampString(form.value.expiration);
  }
  if (form.value.comment) {
    accessKey.comment = form.value.comment;
  }
  return accessKey;
};
const handleCreateAccessKey = async (): Promise<void> => {
  if (props.accessKeyService && !isBusy.value && (await validateForm())) {
    isBusy.value = true;
    try {
      const accessKey = getAccessKey();
      const accessKeyId =
        await props.accessKeyService.createAccessKey(accessKey);
      const createdAccessKey =
        await props.accessKeyService.getAccessKey(accessKeyId);
      emit('add-access-key', form.value.secret, createdAccessKey);
      close(true);
    } catch (error) {
      handleError(error);
    } finally {
      isBusy.value = false;
    }
  }
};
const generateSecret = (): void => {
  form.value.secret = generateRandomString(16);
};
onMounted(() => {
  if (createModalElement.value) {
    modalInstance = new Modal(createModalElement.value, { backdrop: true });
  }
});
onErrorCaptured((err): boolean => {
  handleError(err.message);
  return false;
});
const open = () => {
  modalInstance?.show();
};
const close = (force: boolean) => {
  if (force || !isBusy.value) {
    resetModal();
    modalInstance?.hide();
  }
};
defineExpose({ open, close });
</script>
