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
          <form class="row g-3">
            <div class="col-12">
              <label for="inputReference" class="form-label">
                {{ t(getI18nKey('label.reference')) }}
              </label>
              <input
                id="inputReference"
                v-model="form.reference"
                type="text"
                :disabled="reference !== undefined"
                class="form-control"
              />
            </div>
            <div class="col-md-6">
              <label for="inputPermission" class="form-label">
                {{ t(getI18nKey('label.permission')) }}
              </label>
              <select
                v-if="availablePermissions.length > 0"
                id="inputPermission"
                v-model="form.type"
                class="form-select"
              >
                <option
                  v-for="permissionValue in availablePermissions"
                  :key="permissionValue"
                  :value="permissionValue"
                >
                  {{ t(getI18nKey(`label.permission.${permissionValue}`)) }}
                </option>
              </select>
              <input
                v-else
                id="inputPermission"
                v-model="form.type"
                type="text"
                class="form-control"
              />
            </div>
            <div class="col-md-6">
              <label for="expirationInput" class="form-label">
                {{ t(getI18nKey('label.expiration')) }}
              </label>
              <div class="input-group">
                <input
                  id="expirationInput"
                  v-model="form.expiration"
                  type="date"
                  class="form-control"
                />
              </div>
            </div>
            <div class="col-12">
              <div class="form-check">
                <input
                  id="inputActive"
                  v-model="form.isActive"
                  class="form-check-input"
                  type="checkbox"
                />
                <label class="form-check-label" for="inputActive">
                  {{ t(getI18nKey('label.active')) }}
                </label>
              </div>
            </div>
            <div class="col-12">
              <label for="commentTextarea" class="form-label">
                {{ t(getI18nKey('label.comment')) }}
              </label>
              <textarea
                id="commentTextarea"
                v-model="form.comment"
                class="form-control"
                rows="3"
              />
            </div>
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
import {
  MCRAccessKey,
  MCRPartialUpdateAccessKeyDTO,
  MCRAccessKeyService,
} from '@golsch/test/acl/accesskey';
import {
  getI18nKey,
  convertUnixToIso,
  getUnixTimestampString,
} from '@/common/utils';

interface FormData {
  reference: string;
  type: string;
  expiration: string | null;
  comment: string | undefined;
  isActive: boolean;
}

const { t } = useI18n();

const props = defineProps<{
  accessKeyService?: MCRAccessKeyService;
  availablePermissions: string[];
  reference?: string;
}>();

const emit = defineEmits<{
  (event: 'update-access-key', accessKey: MCRAccessKey): void;
  (event: 'close'): void;
}>();

const infoModalElement = ref<HTMLDivElement | null>(null);
let modalInstance: Modal | null = null;
const accessKey = ref<MCRAccessKey>();

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
const buildAccessKeyPayload = (): MCRPartialUpdateAccessKeyDTO => {
  const updatedAccessKey: MCRPartialUpdateAccessKeyDTO = {};
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
      const partialUpdatedAccessKey = buildAccessKeyPayload();
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
const open = (a: MCRAccessKey): void => {
  accessKey.value = a;
  form.value = {
    reference: a.reference,
    type: a.type,
    expiration: a.expiration ? convertUnixToIso(a.expiration) : null,
    comment: a.comment,
    isActive: a.isActive,
  };
  modalInstance?.show();
};
const close = (force: boolean): void => {
  if (force || !isBusy.value) {
    modalInstance?.hide();
  }
};
onMounted((): void => {
  if (infoModalElement.value) {
    modalInstance = new Modal(infoModalElement.value, { backdrop: true });
  }
});
onErrorCaptured((err): boolean => {
  handleError(err);
  return false;
});
defineExpose({ open, close });
</script>
