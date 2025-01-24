<template>
  <div :class="['form-group', required && 'required']">
    <slot></slot>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    required?: boolean;
  }>(),
  {
    required: false,
  }
);
</script>

<style>
.form-group.required label:after {
  content: '*';
  color: red;
  font-weight: bold;
  margin-left: 5px;
}
</style>
