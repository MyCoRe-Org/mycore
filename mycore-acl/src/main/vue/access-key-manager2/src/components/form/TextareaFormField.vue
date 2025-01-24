<template>
  <BaseFormField :required="required">
    <label :for="inputId">{{ label }}</label>
    <div class="input-group">
      <textarea
        :id="inputId"
        :value="modelValue"
        class="form-control"
        :rows="rows"
        :class="inputClass"
        :disabled="disabled"
        @input="onInput"
      />
    </div>
  </BaseFormField>
</template>

<script setup lang="ts">
import { defineProps } from 'vue';
import BaseFormField from './BaseFormField.vue';

withDefaults(
  defineProps<{
    modelValue?: string;
    label: string;
    rows: string;
    inputId: string;
    inputClass?: string;
    disabled?: boolean;
    required?: boolean;
  }>(),
  {
    modelValue: '',
    inputClass: 'form-control',
    disabled: false,
    required: false,
  }
);

const emit = defineEmits(['update:modelValue']);

const onInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit('update:modelValue', target.value);
};
</script>
