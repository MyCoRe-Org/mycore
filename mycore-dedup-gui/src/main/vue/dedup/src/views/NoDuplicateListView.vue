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
      {{ i18n["dedup.gui.noDuplicates.empty"] }}
    </div>
    <table v-else class="table table-hover">
      <thead>
      <tr>
        <th class="pointer nobreak" v-on:click="toggleSort('object1')">
          {{ i18n["dedup.gui.noDuplicates.table.object1"] }}
          <SortArrow v-if="model.sortField == 'object1'" :direction="model.sortDir"/>
        </th>
        <th class="pointer nobreak" v-on:click="toggleSort('object2')">
          {{ i18n["dedup.gui.noDuplicates.table.object2"] }}
          <SortArrow v-if="model.sortField == 'object2'" :direction="model.sortDir"/>
        </th>
        <th class="pointer nobreak" v-on:click="toggleSort('creator')">
          {{ i18n["dedup.gui.noDuplicates.table.creator"] }}
          <SortArrow v-if="model.sortField == 'creator'" :direction="model.sortDir"/>
        </th>
        <th class="pointer nobreak" v-on:click="toggleSort('created')">
          {{ i18n["dedup.gui.noDuplicates.table.date"] }}
          <SortArrow v-if="model.sortField == 'created'" :direction="model.sortDir"/>
        </th>
        <th></th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="entry in sortedList" :key="entry.id">
        <td>
          <PublicationDisplay :mcr-id="entry.objectId1" :title="entry.title1"/>
        </td>
        <td>
          <PublicationDisplay :mcr-id="entry.objectId2" :title="entry.title2"/>
        </td>
        <td>{{ entry.creator }}</td>
        <td class="nobreak">{{ formatDate(entry.created) }}</td>
        <td>
          <button class="btn btn-danger btn-sm" v-on:click="showDeleteModal(entry)">
            {{ i18n["dedup.gui.action.delete"] }}
          </button>
        </td>
      </tr>
      </tbody>
    </table>

    <Modal ref="deleteModalRef">
      <template #title>{{ i18n["dedup.gui.action.delete"] }}</template>
      <template #default>
        <p>{{ i18n["dedup.gui.action.deleteConfirm"] }}</p>
        <p v-if="model.selected" class="mt-1">
          <PublicationDisplay :mcr-id="model.selected.objectId1" :title="model.selected.title1"/>
        </p>
        <p v-if="model.selected" class="mt-1">
          <PublicationDisplay :mcr-id="model.selected.objectId2" :title="model.selected.title2"/>
        </p>
      </template>
      <template #footer>
        <button class="btn btn-danger" v-on:click="confirmDelete">
          {{ i18n["dedup.gui.action.deleteYes"] }}
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
import type { NoDuplicateEntry, NoDuplicateList } from "@/api/Model";
import { getWebApplicationBaseURL } from "@/api/BaseURL";
import { getAuthorizationHeader } from "@/api/Auth";
import { resolveI18N } from "@/api/I18N";
import SortArrow from "@/components/SortArrow.vue";
import PublicationDisplay from "@/components/PublicationDisplay.vue";
import Modal from "@/components/Modal.vue";

type SortField = "object1" | "object2" | "creator" | "created";

const deleteModal = useTemplateRef("deleteModalRef");

const model = reactive({
  loading: true,
  list: [] as NoDuplicateList,
  sortField: "created" as SortField,
  sortDir: "desc" as "asc" | "desc",
  error: null as number | null,
  selected: null as NoDuplicateEntry | null,
});

const i18n = reactive<{ [key: string]: string }>({
  "dedup.gui.noDuplicates.table.object1": "dedup.gui.noDuplicates.table.object1",
  "dedup.gui.noDuplicates.table.object2": "dedup.gui.noDuplicates.table.object2",
  "dedup.gui.noDuplicates.table.creator": "dedup.gui.noDuplicates.table.creator",
  "dedup.gui.noDuplicates.table.date": "dedup.gui.noDuplicates.table.date",
  "dedup.gui.noDuplicates.empty": "dedup.gui.noDuplicates.empty",
  "dedup.gui.action.delete": "dedup.gui.action.delete",
  "dedup.gui.action.deleteConfirm": "dedup.gui.action.deleteConfirm",
  "dedup.gui.action.deleteYes": "dedup.gui.action.deleteYes",
  "dedup.gui.action.cancel": "dedup.gui.action.cancel",
  "dedup.gui.notAllowed": "dedup.gui.notAllowed",
});

const formatDate = (value: string): string => {
  const date = new Date(value);
  return isNaN(date.getTime()) ? value : date.toLocaleString();
};

const sortKey = (entry: NoDuplicateEntry): string => {
  switch (model.sortField) {
    case "object1":
      return entry.objectId1;
    case "object2":
      return entry.objectId2;
    case "creator":
      return entry.creator ?? "";
    default:
      return entry.created ?? "";
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
  const response = await fetch(`${getWebApplicationBaseURL()}api/dedup/no-duplicates`, {
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

const showDeleteModal = (entry: NoDuplicateEntry) => {
  model.selected = entry;
  deleteModal.value?.show();
};

const closeModal = () => {
  deleteModal.value?.hide();
  model.selected = null;
};

const confirmDelete = async () => {
  const entry = model.selected;
  deleteModal.value?.hide();
  model.selected = null;
  if (entry == null) {
    return;
  }
  const response = await fetch(`${getWebApplicationBaseURL()}api/dedup/no-duplicates/${entry.id}`, {
    method: "DELETE",
    cache: "no-store",
    headers: {
      authorization: await getAuthorizationHeader()
    }
  });
  if (response.status != 200 && response.status != 204) {
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
