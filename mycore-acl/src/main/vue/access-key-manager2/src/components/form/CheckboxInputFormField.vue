<template>
  <BaseFormField :required="required">
    <div class="form-check">
      <input
        :id="inputId"
        :checked="modelValue"
        type="checkbox"
        class="form-check-input"
        :class="inputClass"
        :disabled="disabled"
        @input="onInput"
      />
      <label class="form-check-label" :for="inputId">{{ label }}</label>
    </div>
  </BaseFormField>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import BaseFormField from './BaseFormField.vue';

withDefaults(
  defineProps<{
    modelValue: boolean;
    label: string;
    inputId: string;
    inputClass?: string;
    disabled?: boolean;
    required?: boolean;
  }>(),
  {
    inputClass: 'form-check-input',
    disabled: false,
    required: false,
  }
);

const emit = defineEmits(['update:modelValue']);

const onInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.checked);
};
</script>
