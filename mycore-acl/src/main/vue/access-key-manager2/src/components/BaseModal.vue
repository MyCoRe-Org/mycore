<template>
  <div
    v-if="show"
    class="modal-backdrop"
    @click="close"
    @keyDown="close"
  >
    <div
      class="modal-dialog modal-dialog-centered"
      :class="style"
      role="document"
      @click.stop=""
    >
      <div class="modal-content">
        <div class="modal-header">
          <slot name="title">
            <h5 class="modal-title">
              {{ title }}
            </h5>
            <button
              v-if="!hideHeaderClose"
              type="button"
              class="close"
              aria-label="Close"
            >
              <span
                aria-hidden="true"
                @click="close"
              >&times;</span>
            </button>
          </slot>
        </div>
        <div class="modal-body">
          <slot />
        </div>
        <div class="modal-footer">
          <slot name="footer">
            <button
              v-if="!okOnly"
              type="button"
              class="btn btn-secondary"
              @click="cancel"
            >
              {{ cancelTitle }}
            </button>
            <button
              type="button"
              class="btn btn-primary"
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
    show: boolean;
    title: string | null;
    okTitle?: string;
    cancelTitle?: string;
    okOnly?: boolean;
    busy?: boolean;
    size?: string;
    scrollable?: boolean;
    hideHeaderClose?: boolean;
  }>(),
  {
    show: false,
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
const close = () => {
  if (!props.busy) {
    emit("close");
  }
};
const ok = () => {
  if (!props.busy) {
    emit("ok");
  }
};
const cancel = () => {
  if (!props.busy) {
    emit("cancel");
  }
};
const style = computed(() => {
  let result = `modal-${props.size}`;
  if (props.scrollable) {
    result += " modal-dialog-scrollable";
  }
  return result;
});
</script>
<style>
.modal-backdrop {
  background-color: rgba(0, 0, 0, 0.3);
}
</style>
