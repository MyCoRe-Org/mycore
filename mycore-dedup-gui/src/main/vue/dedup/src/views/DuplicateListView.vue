<template>
  <main>
    <div v-if="model.loading" class="text-center">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>
    <div v-else-if="model.error != null" class="alert alert-danger" role="alert">
      <template v-if="model.error == 403">{{ i18n["dedup.gui.notAllowed"] }}</template>
      <template v-else>{{ model.error }}</template>
    </div>
    <div v-else-if="model.list.length == 0" class="alert alert-info" role="alert">
      {{ i18n["dedup.gui.duplicates.empty"] }}
    </div>
    <table v-else class="table table-hover">
      <thead>
      <tr>
        <th class="pointer nobreak" v-on:click="toggleSort('criterion')">
          {{ i18n["dedup.gui.duplicates.table.criterion"] }}
          <SortArrow v-if="model.sortField == 'criterion'" :direction="model.sortDir"/>
        </th>
        <th class="pointer nobreak" v-on:click="toggleSort('object1')">
          {{ i18n["dedup.gui.duplicates.table.object1"] }}
          <SortArrow v-if="model.sortField == 'object1'" :direction="model.sortDir"/>
        </th>
        <th class="pointer nobreak" v-on:click="toggleSort('object2')">
          {{ i18n["dedup.gui.duplicates.table.object2"] }}
          <SortArrow v-if="model.sortField == 'object2'" :direction="model.sortDir"/>
        </th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="entry in sortedList"
          :key="entry.criterionType + '-' + entry.criterionValue + '-' + entry.objectId1 + '-' + entry.objectId2">
        <td>
          <strong>{{ entry.criterionType }}</strong>: {{ entry.criterionValue }}
        </td>
        <td>
          <PublicationDisplay :mcr-id="entry.objectId1" :title="entry.title1"/>
        </td>
        <td>
          <PublicationDisplay :mcr-id="entry.objectId2" :title="entry.title2"/>
        </td>
        <td>
          <button class="btn btn-outline-secondary btn-sm nobreak" v-on:click="showMarkModal(entry)">
            {{ i18n["dedup.gui.action.markNoDuplicate"] }}
          </button>
        </td>
      </tr>
      </tbody>
    </table>

    <Modal ref="markModalRef">
      <template #title>{{ i18n["dedup.gui.action.markNoDuplicate"] }}</template>
      <template #default>
        <p>{{ i18n["dedup.gui.action.markNoDuplicateConfirm"] }}</p>
        <p v-if="model.selected" class="mt-1">
          <PublicationDisplay :mcr-id="model.selected.objectId1" :title="model.selected.title1"/>
        </p>
        <p v-if="model.selected" class="mt-1">
          <PublicationDisplay :mcr-id="model.selected.objectId2" :title="model.selected.title2"/>
        </p>
      </template>
      <template #footer>
        <button class="btn btn-primary" v-on:click="confirmMark">
          {{ i18n["dedup.gui.action.markNoDuplicateYes"] }}
        </button>
        <button class="btn btn-secondary" v-on:click="closeModal">
          {{ i18n["dedup.gui.action.cancel"] }}
        </button>
      </template>
    </Modal>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, useTemplateRef } from "vue";
import type { DuplicateEntry, DuplicateList } from "@/api/Model";
import { getWebApplicationBaseURL } from "@/api/BaseURL";
import { getAuthorizationHeader } from "@/api/Auth";
import { resolveI18N } from "@/api/I18N";
import SortArrow from "@/components/SortArrow.vue";
import PublicationDisplay from "@/components/PublicationDisplay.vue";
import Modal from "@/components/Modal.vue";

type SortField = "criterion" | "object1" | "object2";

const markModal = useTemplateRef("markModalRef");

const model = reactive({
  loading: true,
  list: [] as DuplicateList,
  sortField: "criterion" as SortField,
  sortDir: "asc" as "asc" | "desc",
  error: null as number | null,
  selected: null as DuplicateEntry | null,
});

const i18n = reactive<{ [key: string]: string }>({
  "dedup.gui.duplicates.table.criterion": "dedup.gui.duplicates.table.criterion",
  "dedup.gui.duplicates.table.object1": "dedup.gui.duplicates.table.object1",
  "dedup.gui.duplicates.table.object2": "dedup.gui.duplicates.table.object2",
  "dedup.gui.duplicates.empty": "dedup.gui.duplicates.empty",
  "dedup.gui.action.markNoDuplicate": "dedup.gui.action.markNoDuplicate",
  "dedup.gui.action.markNoDuplicateConfirm": "dedup.gui.action.markNoDuplicateConfirm",
  "dedup.gui.action.markNoDuplicateYes": "dedup.gui.action.markNoDuplicateYes",
  "dedup.gui.action.cancel": "dedup.gui.action.cancel",
  "dedup.gui.notAllowed": "dedup.gui.notAllowed",
});

const sortKey = (entry: DuplicateEntry): string => {
  switch (model.sortField) {
    case "object1":
      return entry.objectId1;
    case "object2":
      return entry.objectId2;
    default:
      return entry.criterionType + " " + entry.criterionValue;
  }
};

const sortedList = computed(() => {
  const factor = model.sortDir === "desc" ? -1 : 1;
  return [...model.list].sort((a, b) => factor * sortKey(a).localeCompare(sortKey(b)));
});

const toggleSort = (field: SortField) => {
  if (model.sortField === field) {
    model.sortDir = model.sortDir === "asc" ? "desc" : "asc";
  } else {
    model.sortField = field;
    model.sortDir = "asc";
  }
};

const resolveList = async () => {
  model.loading = true;
  model.error = null;
  const response = await fetch(`${getWebApplicationBaseURL()}api/dedup/duplicates`, {
    cache: "no-store",
    headers: {
      authorization: await getAuthorizationHeader()
    }
  });
  if (response.status != 200) {
    model.error = response.status;
    model.loading = false;
    return;
  }
  model.list = await response.json();
  model.loading = false;
};

const showMarkModal = (entry: DuplicateEntry) => {
  model.selected = entry;
  markModal.value?.show();
};

const closeModal = () => {
  markModal.value?.hide();
  model.selected = null;
};

const confirmMark = async () => {
  const entry = model.selected;
  markModal.value?.hide();
  model.selected = null;
  if (entry == null) {
    return;
  }
  const query = `id1=${encodeURIComponent(entry.objectId1)}&id2=${encodeURIComponent(entry.objectId2)}`;
  const response = await fetch(`${getWebApplicationBaseURL()}api/dedup/no-duplicates?${query}`, {
    method: "POST",
    cache: "no-store",
    headers: {
      authorization: await getAuthorizationHeader()
    }
  });
  if (response.status != 201) {
    model.error = response.status;
  } else {
    await resolveList();
  }
};

onMounted(() => {
  resolveList();
  resolveI18N(i18n);
});
</script>

<style scoped>
.pointer {
  cursor: pointer;
}

.nobreak {
  white-space: nowrap;
}
</style>
