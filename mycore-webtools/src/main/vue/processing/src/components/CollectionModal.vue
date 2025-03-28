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

// Props
import {Collection} from "../model/model.ts";
import {Util} from "../common/util.ts";
import {onMounted, useTemplateRef} from "vue";
import type {Modal} from "bootstrap";
declare const bootstrap: { Modal: typeof Modal };

defineProps<{
  model: Collection;
}>();

const emit = defineEmits(['close']);

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
  <div class="modal" ref="modal">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="modal-title">Properties</h4>
          <button @click="emit('close')" type="button" class="btn-close" data-bs-dismiss="modal"
                  aria-label="Close"></button>
        </div>

        <div class="modal-body">
          <table class="table">
            <thead>
              <tr>
                <th>Key</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="model.propertyKeys.length === 0">
                <td colspan="2">No properties set</td>
              </tr>
              <tr v-for="key in model.propertyKeys" :key="key" class="property-row">
                <td>{{ key }}</td>
                <td v-html="Util.useJsonString(model.properties[key])"></td>
              </tr>
            </tbody>
          </table>
        </div>

      </div>
  </div>
  </div>
</template>

<style scoped>

</style>
