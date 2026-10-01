<template>
  <span :class="iconClass"> </span>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  direction: "asc" | "desc" | "none";
}>();

const iconClass = computed(() => {
  switch (props.direction) {
    case "asc":
      return "fas fa-sort-up";
    case "desc":
      return "fas fa-sort-down";
    default:
      return "fas fa-sort";
  }
});
</script>
