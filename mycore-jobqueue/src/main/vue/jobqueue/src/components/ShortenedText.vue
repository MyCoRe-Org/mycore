<template>
  <span>
      {{ shortenedText }}
      <i v-if="needShorten" v-on:click="showAll()" class="fas fa-info"> </i>
  </span>
  <div  v-if="model.showAll" class="modal d-block" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-xl" role="document">
      <div class="modal-content">
        <div class="modal-body">
          <pre>{{ props.text }}</pre>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" v-on:click="model.showAll=false">Close</button>
        </div>
      </div>
    </div>

  </div>
</template>
<script setup lang="ts">

import {computed, defineProps, reactive} from 'vue'

const props = defineProps({
  text: {
    type: String,
    required: true
  },
  maxLength: {
    type: Number,
    required: true
  }
})

const shortenedText = computed(() => {
  if (props.text.length > props.maxLength) {
    return props.text.substring(0, props.maxLength) + '...'
  } else {
    return props.text
  }
})

const needShorten = computed(() => {
  return props.text.length > props.maxLength
})


const model = reactive({showAll: false});

function showAll() {
  model.showAll = true;
}
</script>