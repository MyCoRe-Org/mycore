<template>
  <div class="entry">
    <span v-if="title">{{ title }}</span>
    <span class="text-muted" :title="i18n['dedup.gui.objectId']"> ({{ mcrId }})</span>
    <a :href="getObjectURL(mcrId)" :aria-label="i18n['dedup.gui.action.showMetadata']"
       :title="i18n['dedup.gui.action.showMetadata']">
      <span class="fas fa-file-alt d-inline-block ms-2"> </span>
    </a>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive } from "vue";
import { getObjectURL } from "@/api/Utils";
import { resolveI18N } from "@/api/I18N";

defineProps<{
  mcrId: string;
  title: string | null;
}>();

const i18n = reactive<{ [key: string]: string }>({
  "dedup.gui.action.showMetadata": "dedup.gui.action.showMetadata",
  "dedup.gui.objectId": "dedup.gui.objectId",
});

onMounted(() => {
  resolveI18N(i18n);
});
</script>

<style scoped>
.entry {
  min-height: 21px;
}
</style>
