<template>
  <BaseModal
    v-if="accessKey"
    :is-visible="isVisible"
    :title="t(getI18nKey('title.viewAccessKey'))"
    ok-only
    scrollable
    :busy="isBusy"
    @close="close"
  >
    <div
      v-if="errorMessage"
      class="alert alert-danger text-center"
      role="alert"
      aria-live="assertive"
    >
      {{ $t(errorMessage) }}
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
            :disabled="reference !== undefined"
            type="text"
            class="form-control"
            aria-labelledby="labelReference"
          >
        </div>
      </div>
      <div class="form-row">
        <div class="form-group col-md-6">
          <label
            id="lablePermission"
            for="inputPermission"
          >
            {{ t(getI18nKey("label.permission")) }}
          </label>
          <select
            v-if="availablePermissions.length > 0"
            id="inputPermission"
            v-model="form.type"
            class="form-control"
            aria-labelledby="inputPermission"
          >
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
            class="form-control"
            aria-labelledby="inputPermission"
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
            type="date"
            class="form-control"
            aria-labelledby="labelExpiration"
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
            aria-labelledby="labelActive"
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
          {{ $t(getI18nKey("label.comment")) }}
        </label>
        <textarea
          id="commentTextarea"
          v-model="form.comment"
          class="form-control"
          rows="3"
          aria-labelledby="labelComment"
        />
      </div>
    </form>
    <template #footer>
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
        {{ t(getI18nKey("button.updateAccessKey")) }}
      </button>
    </template>
  </BaseModal>
</template>

<!-- TODO fix delete date issue in endpoint -->
<script setup lang="ts">
import { computed, ref, onErrorCaptured, watch } from "vue";
import { AccessKeyDto, PartialUpdateAccessKeyDto } from "@/dtos/accesskey";
import BaseModal from "@/components/BaseModal.vue";
import { required } from "@vuelidate/validators";
import useVuelidate from "@vuelidate/core";
import { AccessKeyService } from "@/service/accesskey";
import { getI18nKey } from "@/utils";
import { useI18n } from "vue-i18n";

interface FormData {
  reference: string;
  type: string;
  expiration: string | undefined;
  comment: string | undefined;
  isActive: boolean;
}

const { t } = useI18n();

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

const errorMessage = ref<string | undefined>(undefined);
const isBusy = ref<boolean>(false);
const form = ref<FormData>({
  reference: "",
  type: "",
  isActive: false,
  comment: undefined,
  expiration: undefined,
});

const rules = computed(() => ({
  reference: { required },
  type: { required },
}));

const v = useVuelidate(rules, form);
watch(() => props.accessKey, (newAccessKey: AccessKeyDto | undefined) => {
  if (newAccessKey) {
    form.value = {
      reference: newAccessKey.reference,
      type: newAccessKey.type,
      expiration: newAccessKey.expiration
        ? new Date(newAccessKey.expiration).toISOString().slice(0, 10)
        : undefined,
      comment: newAccessKey.comment,
      isActive: newAccessKey.isActive,
    };
  }
}, { deep: true });
const handleError = (error: unknown): void => {
  errorMessage.value =
    error instanceof Error ? error.message : "component.acl.accesskey.frontend.error.fatal";
};
const close = (force?: boolean): void => {
  if (force || !isBusy.value) {
    emit("close");
  }
};
const buildAccessKeyPayload = (): PartialUpdateAccessKeyDto => {
  const accessKey: PartialUpdateAccessKeyDto = {};
  if (form.value.reference !== props.accessKey?.reference) {
    accessKey.reference = form.value.reference;
  } 
  if (form.value.comment !== props.accessKey?.comment) {
    accessKey.comment = form.value.comment;
  }
  if (form.value.type !== props.accessKey?.type) {
    accessKey.type = form.value.type;
  }
  // TODO fix expiration compare
  if (form.value.expiration !== props.accessKey?.expiration) {
    accessKey.expiration = form.value.expiration ? Math.floor(new Date(form.value.expiration).getTime()) : null;
  } 
  if (form.value.isActive !== props.accessKey?.isActive) {
    accessKey.isActive = form.value.isActive;
  }
  return accessKey;
};
const updateAccessKey = async (): Promise<void> => {
  if (props.accessKeyService && !isBusy.value) {
    isBusy.value = true;
    v.value.$validate();
    if (!v.value.$invalid && props.accessKey && props.accessKey.id) {
      const accessKey: PartialUpdateAccessKeyDto = buildAccessKeyPayload();
      try {
        await props.accessKeyService.patchAccessKey(props.accessKey.id, accessKey);
        const updatedAccessKey = await props.accessKeyService.getAccessKey(props.accessKey.id);
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
onErrorCaptured((err): boolean => {
  handleError(err);
  return false;
});
</script>
