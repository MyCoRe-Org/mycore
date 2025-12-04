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
import CollectionModal from "./CollectionModal.vue";
import {computed, ref, useTemplateRef} from "vue";

const props = defineProps<{ collection: Collection }>();

const selectedTab = ref<'processing' | 'waiting' | 'finished'>('processing');

const collectionModal = useTemplateRef("collectionModal");

function showModal() {
  if (collectionModal.value) {
    collectionModal.value.show();
  }
}

const currentTabCollection = computed(() => {
  switch (selectedTab.value) {
    case 'processing':
      return props.collection.processingProcessables;
    case 'waiting':
      return props.collection.createdProcessables;
    case 'finished':
      return props.collection.finishedProcessables;
  }
});

</script>

<template>
  <div class="card mb-4">
    <div class="card-header">

      <h2 class="d-inline h5">{{ collection.name }}</h2>
      <a class="ms-1 text-secondary" href="#" @click.prevent="showModal">
        <i class="fa-solid fa-circle-info fa-lg"></i>
        <span class="visually-hidden">Properties</span>
      </a>
    </div>
    <div class="card-body">
      <!-- TAB -->
      <ul class="nav nav-tabs">
        <li class="nav-item">
          <a
              href="#processing"
              @click="selectedTab = 'processing'"
              :class="{ 'nav-link': true, active: selectedTab === 'processing' }">
            Processing <span class="badge rounded-pill bg-info">{{ collection.processingProcessables.length }}</span>
          </a>
        </li>
        <li class="nav-item">
          <a
              href="#waiting"
              @click="selectedTab = 'waiting'"
              :class="{ 'nav-link': true, active: selectedTab === 'waiting' }">
            Waiting <span class="badge rounded-pill bg-info">{{ collection.createdProcessables.length }}</span>
          </a>
        </li>
        <li class="nav-item">
          <a
              href="#finished"
              @click="selectedTab = 'finished'"
              :class="{ 'nav-link': true, active: selectedTab === 'finished' }">
            Finished <span class="badge rounded-pill bg-info">{{ collection.finishedProcessables.length }}</span>
          </a>
        </li>
      </ul>
      <!-- HEADER -->
      <table class="table">
        <thead>
        <tr>
          <th>Name</th>
          <th>User</th>
          <th>Create Date</th>
          <th>Progress</th>
        </tr>
        </thead>
        <tbody>
        <ProcessableComponent v-for="processable in currentTabCollection"
                              :model="processable"
                              :key="processable.id"/>
        <tr v-if="currentTabCollection.length === 0">
          <td colspan="4">
            no {{ selectedTab }} process
          </td>
        </tr>
        </tbody>
      </table>
      <!-- modal -->
      <CollectionModal ref="collectionModal" :model="collection"/>
    </div>
  </div>

</template>

<style scoped>

</style>
