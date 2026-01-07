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
import {Processable} from "../model/model.ts";
import ProcessableModal from "./ProcessableModal.vue";
import {Util} from "../common/util.ts";
import {useTemplateRef} from "vue";

defineProps<{
  model: Processable;
}>();

function getProgress(model: Processable) {
  return `${model.progress || 0}%`;
}

const processableModal = useTemplateRef("processableModal");

function showModal() {
  if (processableModal.value) {
    processableModal.value.show();
  }
}

</script>

<template>
  <!-- row -->
  <tr >
    <td class="col-name">
      <a href="#" @click.prevent="showModal">
        <i class="fa-solid fa-circle-info"></i>
      </a> {{ model.name }}
    </td>
    <td class="col-user">{{ model.user }}</td>
    <td class="col-create">{{ Util.formatDate(model.createTime) }}</td>
    <td class="col-progress">
      <!-- If progress is undefined -->
      <div v-if="model.progress === undefined">{{ model.status }}</div>
      <!-- Default case -->
      <div v-else class="progress">
        <div
            class="progress-bar"
            role="progressbar"
            :style="{width: getProgress(model)}"
            aria-valuemin="0"
            aria-valuemax="100"
        >
        </div>
        <div class="progress-text">{{ model.progressText }}</div>
      </div>
    </td>
    <ProcessableModal ref="processableModal" :model="model" />
  </tr>
</template>

<style scoped>
.table-content {
  display: flex;
}
.progress {
  border-radius: 4px;
  border: 1px solid #ccc;
  height: 20px;
  overflow: hidden;
  margin: 1px;
  background: #f5f5f5;
  position: relative;
}

.progress-bar {
  transition: width 0.3s ease;
  height: 100%;
  background-color: #ffc1ab;
  position: absolute;
  top: 0;
  left:0;
  z-index: 1;
}

.progress-text {
  position: absolute;
  top: 2px;
  z-index: 2;
  width: 100%;
  text-align: center;
}

</style>
