<template>
  <div style="text-align: center; margin-top: 50px;">
    <h1>Ein Fehler ist aufgetreten</h1>
    <p>{{ errorMessage }}</p>
    <button
      @click="retry"
    >
      Erneut versuchen
    </button>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router';

const route = useRoute();
const errorMessage = route.query.message || 'Unbekannter Fehler';

const retry = () => {
  location.reload();
};
</script>