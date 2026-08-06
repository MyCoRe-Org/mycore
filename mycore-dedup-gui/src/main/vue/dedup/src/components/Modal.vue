<template>
  <div class="modal" tabindex="-1" ref="modal">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <slot name="title"></slot>
          </h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <slot/>
        </div>
        <div class="modal-footer">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, useTemplateRef } from "vue";
import type { Modal } from "bootstrap";

// Bootstrap is provided as a global by the surrounding MyCoRe layout, it is not bundled with this app.
declare const bootstrap: { Modal: typeof Modal };

const modal = useTemplateRef("modal");

let bootstrapModal: Modal | null = null;

const show = () => {
  bootstrapModal?.show();
};

const hide = () => {
  bootstrapModal?.hide();
};

defineExpose({ show, hide });

const emit = defineEmits<{
  show: [],
  hide: []
}>();

onMounted(() => {
  if (modal.value) {
    bootstrapModal = new bootstrap.Modal(modal.value);
    modal.value.addEventListener("hidden.bs.modal", () => emit("hide"));
    modal.value.addEventListener("shown.bs.modal", () => emit("show"));
  }
});
</script>
