<template>
  <div class="card mb-1">
    <div class="card-header">
      <ul class="nav nav-tabs card-header-tabs">
        <li class="nav-item">
          <router-link to="/" class="nav-link" active-class="active">
            {{ i18n["dedup.gui.tab.duplicates"] }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link to="/no-duplicates" class="nav-link" active-class="active">
            {{ i18n["dedup.gui.tab.noDuplicates"] }}
          </router-link>
        </li>
      </ul>
    </div>
    <div class="card-body">
      <RouterView/>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RouterView } from 'vue-router'
import { onMounted, reactive } from "vue";
import { resolveI18N } from "@/api/I18N";

const i18n = reactive<{ [key: string]: string }>({
  "dedup.gui.tab.duplicates": "dedup.gui.tab.duplicates",
  "dedup.gui.tab.noDuplicates": "dedup.gui.tab.noDuplicates",
});

onMounted(() => {
  resolveI18N(i18n);
});
</script>
