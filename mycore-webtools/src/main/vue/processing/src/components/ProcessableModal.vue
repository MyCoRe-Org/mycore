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
import type {Processable} from "../model/model.ts";
import {Util} from "../common/util.ts";
import {onMounted, useTemplateRef} from "vue";
import type {Modal} from "bootstrap";

declare const bootstrap: { Modal: typeof Modal };

defineProps<{
  model: Processable;
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
  <Teleport to="body">
    <div class="modal" ref="modal">
      <div class="modal-dialog modal-lg">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title">Properties</h4>
            <button @click="emit('close')" type="button" class="btn-close" data-bs-dismiss="modal"
                    aria-label="Close"></button>
          </div>

          <div class="modal-body">
            <!-- Basic Properties -->
            <div class="card mb-3">
              <div class="card-header">
                <h3 class="h5">Basic Properties</h3>
              </div>
              <div class="card-body">
                <table class="table table-hover">
                  <thead>
                  <tr>
                    <th>Name</th>
                    <th>Value</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr>
                    <td>Name</td>
                    <td>{{ model.name }}</td>
                  </tr>
                  <tr>
                    <td>Status</td>
                    <td>{{ model.status }}</td>
                  </tr>
                  <tr>
                    <td>Create Time</td>
                    <td>{{ Util.formatDate(model.createTime) }}</td>
                  </tr>
                  <tr>
                    <td>Start Time</td>
                    <td>{{ Util.formatDate(model.startTime) }}</td>
                  </tr>
                  <tr>
                    <td>End Time</td>
                    <td>{{ Util.formatDate(model.endTime) }}</td>
                  </tr>
                  <tr>
                    <td>Took</td>
                    <td>{{ model.took }}ms</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Error Section -->
            <div v-if="model.status === 'FAILED'">
              <div class="property-row">
                <h4 class="h5">Error</h4>
                <pre class="scrollContainer">{{ model.error }}</pre>
              </div>
            </div>

            <!-- External Properties -->
            <div class="card">
              <div class="card-header">
                <h3 class="h5">External Properties</h3>
              </div>
              <div class="card-body">
                <table class="table table-hover">
                  <thead>
                  <tr>
                    <th>Key</th>
                    <th>Value</th>
                  </tr>
                  </thead>
                  <tbody>
                  <tr v-if="model.propertyKeys.length === 0">
                    <td colspan="2">No external properties set</td>
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
      </div>
    </div>
  </Teleport>
</template>

<style scoped>

</style>
