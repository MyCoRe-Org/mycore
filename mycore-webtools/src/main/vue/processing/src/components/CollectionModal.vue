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

defineProps<{
  model: Collection;
}>();

const emit = defineEmits(['close']);

</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="processing-modal">
      <div class="processing-modal-header">
        <h4 class="modal-title">Properties</h4>
        <button @click="emit('close')">&times;</button>
      </div>
      <div class="processing-modal-body">
        <div class="card">
          <h5>Properties</h5>
          <div class="card-content">
            <div v-if="model.propertyKeys.length === 0">No properties set</div>
            <div v-for="key in model.propertyKeys" :key="key" class="property-row">
              <span class="propertyKey">{{ key }}:</span>
              <span class="propertyValue" v-html="Util.useJsonString(model.properties[key])"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>
