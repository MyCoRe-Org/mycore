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

defineProps<{
  model: Processable;
}>();

const emit = defineEmits(['close']);

// Format date
function formatDate(timestamp: number | undefined): string {
  if (!timestamp) {
    return "";
  }
  const date = new Date(timestamp);
  return `${date.getMonth() + 1}.${String(date.getDate()).padStart(2, '0')}
          ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:${String(date.getSeconds()).padStart(2, '0')}`;
}
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-header">
        <h4 class="modal-title">Properties</h4>
        <button @click="emit('close')">&times;</button>
      </div>
      <div class="modal-body">
        <!-- Basic Properties -->
        <div class="card">
          <h5>Basic Properties</h5>
          <div class="property-row">
            <span class="propertyKey">Name:</span>
            <span class="propertyValue">{{ model.name }}</span>
          </div>
          <div class="property-row">
            <span class="propertyKey">Status:</span>
            <span class="propertyValue">{{ model.status }}</span>
          </div>
          <div class="property-row">
            <span class="propertyKey">Create Time:</span>
            <span class="propertyValue">{{ formatDate(model.createTime) }}</span>
          </div>
          <div class="property-row">
            <span class="propertyKey">Start Time:</span>
            <span class="propertyValue">{{ formatDate(model.startTime) }}</span>
          </div>
          <div class="property-row">
            <span class="propertyKey">End Time:</span>
            <span class="propertyValue">{{ formatDate(model.endTime) }}</span>
          </div>
          <div class="property-row">
            <span class="propertyKey">Took:</span>
            <span class="propertyValue">{{ model.took }}ms</span>
          </div>
        </div>

        <!-- Error Section -->
        <div v-if="model.status === 'failed'">
          <div class="property-row">
            <span class="propertyKey">Error:</span>
            <pre class="scrollContainer">{{ model.error }}</pre>
          </div>
        </div>

        <!-- External Properties -->
        <div class="card">
          <h5>External Properties</h5>
          <div v-if="model.propertyKeys.length === 0">No properties set</div>
          <div v-for="key in model.propertyKeys" :key="key" class="property-row">
            <span class="propertyKey">{{ key }}:</span>
            <span class="propertyValue" v-html="Util.useJsonString(model.properties[key])"></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
