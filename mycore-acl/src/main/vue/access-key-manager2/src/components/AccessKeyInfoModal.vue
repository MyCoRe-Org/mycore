<template>
  <BaseModal
    v-if="accessKey"
    :is-visible="isVisible"
    :title="$t('component.acl.accesskey.frontend.title.viewAccessKey')"
    ok-only
    scrollable
    :busy="busy"
    @close="handleClose"
  >
    <div
      v-if="errorMessage"
      class="alert alert-danger text-center"
      role="alert"
    >
      {{ $t(errorMessage) }}
    </div>
    <form>
      <div
        class="form-group required"
      >
        <label for="inputReference">
          {{ $t("component.acl.accesskey.frontend.label.reference") }}
        </label>
        <div class="input-group">
          <input
            id="inputReference"
            v-model="form.reference"
            :disabled="reference !== undefined"
            type="text"
            class="form-control"
          >
        </div>
      </div>
      <div class="form-row">
        <div class="form-group col-md-6">
          <label for="inputPermission">
            {{ $t("component.acl.accesskey.frontend.label.permission") }}
          </label>
          <select
            v-if="availablePermissions.length > 0"
            id="inputPermission"
            v-model="form.type"
            class="form-control"
          >
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
        @click="handleUpdateAccessKey"
      >
        <span
          v-if="busy"
          class="spinner-border spinner-border-sm"
          role="status"
          aria-hidden="true"
        />
        {{ $t("component.acl.accesskey.frontend.button.updateAccessKey") }}
      </button>
    </template>
  </BaseModal>
</template>
<!-- TODO fix delete date issue -->
<script setup lang="ts">
import { computed, ref, onErrorCaptured, watch } from "vue";
import { AccessKeyDto, PartialUpdateAccessKeyDto } from "@/dtos/accesskey";
import BaseModal from "@/components/BaseModal.vue";
import { required } from "@vuelidate/validators";
import useVuelidate from "@vuelidate/core";
import { AccessKeyService } from "@/service/accesskey";

const props = defineProps<{
  accessKeyService?: AccessKeyService;
  isVisible: boolean;
  availablePermissions: string[];
  reference?: string;
  accessKey?: AccessKeyDto;
}>();


const emit = defineEmits<{
  (event: "update-access-key", accessKey: AccessKeyDto): void;
  (event: "close"): void;
}>();
const rules = computed(() => ({
  reference: {
    required,
  },
  type: {
    required,
  },
}));
const errorMessage = ref<string | undefined>(undefined);
const busy = ref<boolean>(false);

interface FormData {
  reference: string;
  type: string;
  expiration: string | undefined;
  comment: string | undefined;
  isActive: boolean;
}

const form = ref<FormData>({
  reference: "",
  type: "",
  isActive: false,
  comment: undefined,
  expiration: undefined,
});

const v = useVuelidate(rules, form);
watch(
  () => props.accessKey,
  (newAccessKey: AccessKeyDto | undefined) => {
    if (newAccessKey) {
      form.value.reference = newAccessKey.reference;
      form.value.type = newAccessKey.type;
      form.value.expiration = newAccessKey.expiration
        ? new Date(newAccessKey.expiration).toISOString().slice(0, 10)
        : undefined;
      form.value.comment = newAccessKey.comment || undefined;
      form.value.isActive = newAccessKey.isActive;
    }
  },
  { deep: true }
);
const handleError = (error: unknown) => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const handleClose = (force: boolean) => {
  if (force || !busy.value) {
    emit("close");
  }
};
const handleUpdateAccessKey = async () => {
  if (props.accessKeyService && !busy.value) {
    v.value.$validate();
    if (!v.value.$invalid && props.accessKey && props.accessKey.id) {
      busy.value = true;
      try {
        const accessKey: PartialUpdateAccessKeyDto = {};
        if (form.value.reference !== props.accessKey.reference) {
          accessKey.reference = form.value.reference;
        }
        if (form.value.comment !== props.accessKey.comment) {
          accessKey.comment = form.value.comment;
        }
        if (form.value.type !== props.accessKey.type) {
          accessKey.type = form.value.type;
        }
        if (form.value.expiration !== props.accessKey.expiration) {
          accessKey.expiration = form.value.expiration
            ? Math.floor(new Date(form.value.expiration).getTime())
            : null;
        }
        if (form.value.isActive !== props.accessKey.isActive) {
          accessKey.isActive = form.value.isActive;
        }
        await props.accessKeyService.patchAccessKey(props.accessKey.id, accessKey);
        const updatedAccessKey = await props.accessKeyService.getAccessKey(props.accessKey.id);
        emit('update-access-key', updatedAccessKey);
        handleClose(true);
      } catch (error) {
        handleError(error);
      } finally {
        busy.value = false;
      }
    }
  }
};
onErrorCaptured((err) => {
  handleError(err);
  return false;
});
</script>
