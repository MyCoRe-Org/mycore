<template>
  <div
    v-if="loading"
    class="overlay"
    role="alert"
    aria-live="polite"
    aria-busy="true"
  >
    <div class="d-flex justify-content-center">
      <div
        class="spinner-grow text-primary"
        role="status"
        style="width: 3rem; height: 3rem; z-index: 20"
        aria-label="Loading..."
      />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  loading?: boolean;
}>();
</script>

<style scoped>
.overlay {
  position: fixed;
  width: 100%;
  height: 100%;
  z-index: 1000;
  top: 10%;
  left: 0;
  background-color: rgba(255, 255, 255, 0.5);
}
</style>
