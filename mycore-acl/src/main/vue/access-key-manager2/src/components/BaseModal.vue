<template>
  <div
    v-if="isVisible"
    class="modal-backdrop"
    :aria-hidden="isVisible ? 'false' : 'true'"
    @click="close"
  >
    <div
      class="modal-dialog modal-dialog-centered"
      :class="style"
      role="document"
      aria-modal="true"
      :aria-labelledby="modalTitleId"
      :aria-describedby="modalDescriptionId"
      @click.stop=""
    >
      <div class="modal-content">
        <div class="modal-header">
          <slot name="title">
            <h5
              :id="modalTitleId" 
              class="modal-title"
            >
              {{ title }}
            </h5>
            <button
              v-if="!hideHeaderClose"
              type="button"
              class="close"
              :aria-disabled="isBusy"
              aria-label="Close"
            >
              <span
                aria-hidden="true"
                @click="close"
              >&times;</span>
            </button>
          </slot>
        </div>
        <div
          :id="modalDescriptionId"
          class="modal-body"
        >
          <slot />
        </div>
        <div class="modal-footer">
          <slot name="footer">
            <button
              v-if="!okOnly"
              type="button"
              class="btn btn-secondary"
              :aria-label="cancelTitle"
              :aria-disabled="isBusy"
              :disabled="isBusy"
              @click="cancel"
            >
              {{ cancelTitle }}
            </button>
            <button
              type="button"
              class="btn btn-primary"
              :aria-label="okTitle"
              :aria-disabled="isBusy"
              :disabled="isBusy"
              @click="ok"
            >
              {{ okTitle }}
            </button>
          </slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
  defineProps<{
    isVisible: boolean;
    title?: string;
    okTitle?: string;
    cancelTitle?: string;
    okOnly?: boolean;
    busy?: boolean;
    size?: string;
    scrollable?: boolean;
    hideHeaderClose?: boolean;
  }>(),
  {
    isVisible: false,
    title: "Title",
    okTitle: "OK",
    cancelTitle: "Cancel",
    okOnly: false,
    busy: false,
    size: "md",
    scrollable: false,
    hideHeaderClose: false,
  }
);

const emit = defineEmits(["close", "ok", "cancel"]);

const modalTitleId = computed(() => `modal-title-${Math.random().toString(36).substring(2, 9)}`);
const modalDescriptionId = computed(() => `modal-description-${Math.random().toString(36).substring(2, 9)}`);
const isBusy = computed(() => props.busy);
const style = computed(() => {
  let result = `modal-${props.size}`;
  if (props.scrollable) {
    result += " modal-dialog-scrollable";
  }
  return result;
});

const close = () => {
  if (!isBusy.value) {
    emit("close");
  }
};
const ok = () => {
  if (!isBusy.value) {
    emit("ok");
  }
};
const cancel = () => {
  if (!isBusy.value) {
    emit("cancel");
  }
};
</script>

<style>
.modal-backdrop {
  background-color: rgba(0, 0, 0, 0.3);
}
</style>
