<template>
  <BaseModal
    :show="showModal"
    :title="$t('component.acl.accesskey.frontend.title.createAccessKey')"
    ok-only
    scrollable
    :busy="busy"
    @close="handleClose()"
  >
    <div
      v-if="errorMessage"
      class="alert alert-danger text-center"
      role="alert"
    >
      {{ $t(errorMessage) }}
    </div>
    <div>
      <p>{{ $t("component.acl.accesskey.frontend.description.createAccesskey") }}</p>
    </div>
    <form>
      <div
        v-if="globalReference === undefined"
        class="form-group required"
      >
        <label for="inputReference">
          {{ $t("component.acl.accesskey.frontend.label.reference") }}
        </label>
        <div class="input-group">
          <input
            id="inputReference"
            v-model="form.reference"
            type="text"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-group required">
        <label for="inputValue">
          {{ $t("component.acl.accesskey.frontend.label.value") }}
        </label>
        <div class="input-group">
          <div class="input-group-prepend">
            <button
              class="btn btn-primary"
              type="button"
              @click="generateValue"
            >
              <i class="fa fa-shuffle" />
            </button>
          </div>
          <input
            id="inputValue"
            v-model="form.secret"
            type="text"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-row">
        <div class="form-group col-md-6 required">
          <label for="inputPermission">
            {{ $t("component.acl.accesskey.frontend.label.permission") }}
          </label>
          <select
            v-if="availablePermissions"
            id="inputPermission"
            v-model="form.type"
            class="form-control"
          >
            <option
              value=""
              disabled
            >
              {{ $t("component.acl.accesskey.frontend.select") }}
            </option>
            <template
              v-for="permissionValue in availablePermissions"
              :key="permissionValue"
            >
              <option :value="permissionValue">
                {{ $t(`component.acl.accesskey.frontend.label.permission.${permissionValue}`) }}
              </option>
            </template>
          </select>
          <input
            v-else
            id="inputPermission"
            v-model="form.type"
            class="form-control"
          >
        </div>
        <div class="form-group col-md-6">
          <label for="expirationInput">
            {{ $t("component.acl.accesskey.frontend.label.expiration") }}
          </label>
          <input
            id="expirationInput"
            v-model="form.expiration"
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
            class="form-check-input"
            type="checkbox"
          >
          <label
            class="form-check-label"
            for="inputActive"
          >
            {{ $t("component.acl.accesskey.frontend.label.active") }}
          </label>
        </div>
      </div>
      <div class="form-group">
        <label for="commentTextarea">
          {{ $t("component.acl.accesskey.frontend.label.comment") }}
        </label>
        <textarea
          id="commentTextarea"
          v-model="form.comment"
          class="form-control"
          rows="3"
        />
      </div>
    </form>
    <template #footer>
      <button
        type="button"
        class="btn btn-primary"
        :disabled="busy || v.$invalid"
        @click="handleCreateAccessKey"
      >
        <span
          v-if="busy"
          class="spinner-border spinner-border-sm"
          role="status"
          aria-hidden="true"
        />
        {{ $t("component.acl.accesskey.frontend.button.createAccessKey") }}
      </button>
    </template>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, inject, ref, onErrorCaptured } from "vue";
import { generateRandomString } from "@/utils";
import { AccessKeyDto, CreateAccessKeyDto } from "@/dtos/accesskey";
import { referenceKey, availablePermissionsKey } from "@/keys";
import useVuelidate from "@vuelidate/core";
import { required } from "@vuelidate/validators";
import BaseModal from "@/components/BaseModal.vue";
import { useAccessKeyStore } from "@/store/access-keys";
import { createAccessKey, getAccessKey } from "@/api/service";

defineProps<{
  showModal: boolean;
}>();

const accessKeyStore = useAccessKeyStore();

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
const emit = defineEmits<{
  (event: "access-key-created", value: string, accessKey: AccessKeyDto): void;
  (event: "close"): void;
}>();

const globalReference: string | undefined = inject(referenceKey);
const availablePermissions: string[] | undefined = inject(availablePermissionsKey);
const errorMessage = ref<string | undefined>(undefined);
const busy = ref<boolean>(false);
const defaultForm = {
  reference: globalReference !== undefined ? globalReference : "",
  isActive: true,
  secret: "",
  type: "",
  comment: undefined,
  expiration: undefined,
};
const form = ref<CreateAccessKeyDto>({ ...defaultForm });
const v = useVuelidate(rules, form);
const handleError = (error: unknown) => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const resetForm = () => {
  form.value = { ...defaultForm };
  v.value.$reset();
};
const resetModal = () => {
  errorMessage.value = undefined;
  resetForm();
};
const handleClose = (force?: boolean) => {
  if (force || !busy.value) {
    resetModal();
    emit("close");
  }
};
const handleCreateAccessKey = async () => {
  if (!busy.value) {
    v.value.$validate();
    if (!v.value.$invalid) {
      if (!v.value.$error) {
        busy.value = true;
        try {
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
          const accessKeyId = await createAccessKey(accessKey);
          const createdAccessKey = await getAccessKey(accessKeyId);
          accessKeyStore.addItem(createdAccessKey);
          emit("access-key-created", form.value.secret, createdAccessKey);
          handleClose(true);
        } catch (error) {
          handleError(error);
        } finally {
          busy.value = false;
        }
      } else {
        busy.value = false;
      }
    }
  }
};
const generateValue = (): void => {
  form.value.secret = generateRandomString(16);
};
onErrorCaptured((err) => {
  handleError(err.message);
  return false;
});
</script>
<style scoped>
.form-group.required label:after {
  content: "*";
  color: black;
}
</style>
