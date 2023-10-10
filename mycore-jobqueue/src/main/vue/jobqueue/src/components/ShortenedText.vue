<!--
  - This file is part of ***  M y C o R e  ***
  - See http://www.mycore.de/ for details.
  -
  - MyCoRe is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - MyCoRe is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
  -->

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
