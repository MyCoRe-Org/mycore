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
import {Collection} from "../model/model.ts";
import ProcessableComponent from "./ProcessableComponent.vue";
import {ref} from "vue";

defineProps<{ collection: Collection }>();

const selectedTab = ref<'processing' | 'waiting' | 'finished'>('processing');
</script>

<template>
  <h2>
    {{ collection.name }}
  </h2>
  <!-- TAB -->
  <div style="white-space: nowrap; overflow-x: auto;">
    <button
        @click="selectedTab = 'processing'"
        :class="{ active: selectedTab === 'processing' }">
      Processing <span class="badge">{{ collection.processingProcessables.length }}</span>
    </button>
    <button
        @click="selectedTab = 'waiting'"
        :class="{ active: selectedTab === 'waiting' }">
      Waiting <span class="badge">{{ collection.createdProcessables.length }}</span>
    </button>
    <button
        @click="selectedTab = 'finished'"
        :class="{ active: selectedTab === 'finished' }">
      Finished <span class="badge">{{ collection.finishedProcessables.length }}</span>
    </button>
  </div>
  <!-- HEADER -->
  <div class="table-header">
    <div class="col col-name">Name</div>
    <div class="col col-user">User</div>
    <div class="col col-create">Create Date</div>
    <div class="col col-progress">Progress</div>
  </div>
  <!-- CONTENT -->
  <div v-if="selectedTab === 'processing'">
    <ProcessableComponent v-for="processable in collection.processingProcessables"
                          :model="processable"
                          :key="processable.id"/>
    <div v-if="collection.processingProcessables.length === 0">
      no active process
    </div>
  </div>
  <div v-if="selectedTab === 'waiting'">
    <ProcessableComponent v-for="processable in collection.createdProcessables"
                          :model="processable"
                          :key="processable.id"/>
    <div v-if="collection.createdProcessables.length === 0">
      no waiting process
    </div>
  </div>
  <div v-if="selectedTab === 'finished'">
    <ProcessableComponent v-for="processable in collection.finishedProcessables"
                          :model="processable"
                          :key="processable.id"/>
    <div v-if="collection.finishedProcessables.length === 0">
      no finished process
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 3rem 0 1rem;
}
button {
  margin-right: 8px;
  padding: 8px 16px;
  cursor: pointer;
  border: 1px solid #ccc;
  background: #f5f5f5;
  border-radius: 4px;
  outline: none;
}

button:hover {
  background: #e0e0e0;
}

button.active {
  background: #ffc1ab;
  border-color: #ff6347;
}

.badge {
  background-color: #ff6347;
  color: white;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 12px;
  margin-left: 8px;
}

.table-header {
  display: flex;
  font-weight: bold;
  border-bottom: 1px solid #ccc;
  margin: 10px 0 4px;
}
</style>
