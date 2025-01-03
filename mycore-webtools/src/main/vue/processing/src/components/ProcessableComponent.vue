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
import {ref} from "vue";
import ProcessableModal from "./ProcessableModal.vue";

defineProps<{ model: Processable }>()

function formatDate(timestamp: number | undefined): string {
  if (!timestamp) {
    return "";
  }
  const date = new Date(timestamp);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const seconds = String(date.getSeconds()).padStart(2, '0');
  return `${month}.${day} - ${hours}:${minutes}:${seconds}`;
}

function toggleModal() {
  isModalVisible.value = !isModalVisible.value;
}
const isModalVisible = ref(false);

</script>

<template>
  <!-- row -->
  <div class="table-content">
    <div class="col col-name">
      <a href="#" @click.prevent="toggleModal">(P)</a> {{ model.name }}
    </div>
    <div class="col col-user">{{ model.user }}</div>
    <div class="col col-create">{{ formatDate(model.createTime) }}</div>
    <div class="col col-progress">{{ model.progressText }}</div>
  </div>
  <!-- modal -->
  <transition name="modal-transition">
    <ProcessableModal v-if="isModalVisible" :model="model" @close="toggleModal"/>
  </transition>
</template>

<style scoped>
.table-content {
  display: flex;
}
</style>
