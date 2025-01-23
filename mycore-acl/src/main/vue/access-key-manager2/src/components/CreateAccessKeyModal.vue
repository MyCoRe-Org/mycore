<template>
  <BaseModal
    :is-visible="isVisible"
    :title="t(getI18nKey('title.createAccessKey'))"
    ok-only
    scrollable
    :busy="isBusy"
    @close="handleClose"
  >
    <div
      v-if="errorMessage"
      class="alert alert-danger text-center"
      role="alert"
      aria-live="polite"
    >
      {{ t(errorMessage) }}
    </div>
    <div>
      <p>{{ t(getI18nKey("description.createAccesskey")) }}</p>
    </div>
    <form>
      <div
        class="form-group required"
      >
        <label
          id="labelReference"
          for="inputReference"
        >
          {{ t(getI18nKey("label.reference")) }}
        </label>
        <div class="input-group">
          <input
            id="inputReference"
            v-model="form.reference"
            aria-labelledby="labelReference"
            :disabled="reference !== undefined"
            type="text"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-group required">
        <label
          id="labelSecret"
          for="inputSecret"
        >
          {{ t(getI18nKey("label.value")) }}
        </label>
        <div class="input-group">
          <div class="input-group-prepend">
            <button
              class="btn btn-primary"
              type="button"
              aria-label="generate secret"
              @click="generateSecret"
            >
              <i class="fa fa-shuffle" />
            </button>
          </div>
          <input
            id="inputSecret"
            v-model="form.secret"
            aria-labelledby="labelSecret"
            type="text"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-row">
        <div class="form-group col-md-6 required">
          <label
            id="labelPermission"
            for="inputPermission"
          >
            {{ t(getI18nKey("label.permission")) }}
          </label>
          <select
            v-if="availablePermissions.length > 0"
            id="inputPermission"
            v-model="form.type"
            aria-labelledby="labelPermission"
            class="form-control"
          >
            <option
              value=""
              disabled
            >
              {{ t(getI18nKey("select")) }}
            </option>
            <template
              v-for="permissionValue in availablePermissions"
              :key="permissionValue"
            >
              <option :value="permissionValue">
                {{ t(getI18nKey(`label.permission.${permissionValue}`)) }}
              </option>
            </template>
          </select>
          <input
            v-else
            id="inputPermission"
            v-model="form.type"
            aria-labelledby="labelPermission"
            class="form-control"
          >
        </div>
        <div class="form-group col-md-6">
          <label
            id="labelExpiration"
            for="expirationInput"
          >
            {{ t(getI18nKey("label.expiration")) }}
          </label>
          <input
            id="expirationInput"
            v-model="form.expiration"
            aria-labelledby="labelExpiration"
            type="date"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-group">
        <div class="form-check">
          <input
            id="inputActive"
            v-model="form.isActive"
            aria-labelledby="labelActive"
            class="form-check-input"
            type="checkbox"
          >
          <label
            id="labelActive"
            class="form-check-label"
            for="inputActive"
          >
            {{ t(getI18nKey("label.active")) }}
          </label>
        </div>
      </div>
      <div class="form-group">
        <label
          id="labelComment"
          for="commentTextarea"
        >
          {{ t(getI18nKey("label.comment")) }}
        </label>
        <textarea
          id="commentTextarea"
          v-model="form.comment"
          aria-labelledby="labelComment"
          class="form-control"
          rows="3"
        />
      </div>
    </form>
    <template #footer>
      <button
        type="button"
        class="btn btn-primary"
        :disabled="isBusy || v.$invalid"
        :aria-disabled="isBusy || v.$invalid"
        @click="handleCreateAccessKey"
      >
        <span
          v-if="isBusy"
          class="spinner-border spinner-border-sm"
          role="status"
          aria-hidden="true"
        />
        {{ t(getI18nKey("button.createAccessKey")) }}
      </button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, ref, onErrorCaptured } from "vue";
import { generateRandomString } from "@/utils";
import { AccessKeyDto, CreateAccessKeyDto } from "@/dtos/accesskey";
import useVuelidate from "@vuelidate/core";
import { required } from "@vuelidate/validators";
import BaseModal from "@/components/BaseModal.vue";
import { AccessKeyService } from "@/service/accesskey";
import { getI18nKey } from "@/utils";
import { useI18n } from "vue-i18n";

const { t } = useI18n();

const props = defineProps<{
  accessKeyService?: AccessKeyService;
  reference?: string;
  availablePermissions: string[];
  isVisible: boolean;
}>();

const emit = defineEmits<{
  (event: "add-access-key", secret: string, accessKey: AccessKeyDto): void;
  (event: "close"): void;
}>();

const rules = computed(() => ({
  reference: {
    required,
  },
  type: {
    required,
  },
  secret: {
    required,
  },
}));

const errorMessage = ref<string>();
const isBusy = ref<boolean>(false);
const defaultForm = {
  reference: props.reference !== undefined ? props.reference : "",
  isActive: true,
  secret: "",
  type: "",
  comment: undefined,
  expiration: undefined,
};
const form = ref<CreateAccessKeyDto>({ ...defaultForm });
const v = useVuelidate(rules, form);
const handleError = (error: unknown): void => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetForm = (): void => {
  form.value = { ...defaultForm };
  v.value.$reset();
};
const resetModal = (): void => {
  errorMessage.value = undefined;
  resetForm();
};
const handleClose = (force?: boolean): void => {
  if (force || !isBusy.value) {
    resetModal();
    emit("close");
  }
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
    accessKey.expiration = Math.floor(new Date(form.value.expiration).getTime());
  }
  if (form.value.comment) {
    accessKey.comment = form.value.comment;
  }
  return accessKey;
}
const handleCreateAccessKey = async (): Promise<void> => {
  if (props.accessKeyService && !isBusy.value && await validateForm()) {
    isBusy.value = true;
    try {
      const accessKey = getAccessKey();
      const accessKeyId = await props.accessKeyService.createAccessKey(accessKey);
      const createdAccessKey = await props.accessKeyService.getAccessKey(accessKeyId);
      emit("add-access-key", form.value.secret, createdAccessKey);
      handleClose(true);
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
onErrorCaptured((err): boolean => {
  handleError(err.message);
  return false;
});
</script>

<style scoped>
.form-group.required label:after {
  content: "*";
  color: red;
  font-weight: bold;
  margin-left: 5px;
}
</style>
