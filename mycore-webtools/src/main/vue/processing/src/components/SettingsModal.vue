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

<script setup lang="ts">
import {Settings} from "../common/settings.ts";
import {onMounted, ref, useTemplateRef} from "vue";
import type {Modal} from "bootstrap";

declare const bootstrap: { Modal: typeof Modal };
// Props

const emit = defineEmits(['close']);

// Reactive state for the settings value
const maxNumberFinished = ref(parseInt(Settings.get('maxNumberFinished', 50), 10));

// Watch for changes and update localStorage
function updateSetting() {
  Settings.set('maxNumberFinished', maxNumberFinished.value);
}

const modal = useTemplateRef('modal');
let bootStrapModel: Modal;

onMounted(() => {
  if (modal.value) {
    bootStrapModel = new bootstrap.Modal(modal.value);
  }
});

function show() {
  bootStrapModel.show();
}

defineExpose({
  show
});

</script>

<template>
  <Teleport to="body">
    <div class="modal" ref="modal">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title">Settings</h4>
            <button @click="emit('close')" type="button" class="btn-close" data-bs-dismiss="modal"
                    aria-label="Close"></button>
          </div>
          <div class="modal-body">
            <p>maximum number of finished processes to display (-1 for unlimited)</p>
            <input
                type="number"
                class="form-control"
                v-model="maxNumberFinished"
                @input="updateSetting"/>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>

</style>
