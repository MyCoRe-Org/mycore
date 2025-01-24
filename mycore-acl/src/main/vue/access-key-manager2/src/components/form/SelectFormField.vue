<template>
  <BaseFormField :required="required">
    <label :for="inputId">{{ label }}</label>
    <div class="input-group">
      <select
        :id="inputId"
        :value="modelValue"
        class="form-control"
        @input="onChange"
      >
        <slot></slot>
      </select>
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

const onChange = (event: Event) => {
  const target = event.target as HTMLSelectElement;
  emit('update:modelValue', target.value);
};
</script>
