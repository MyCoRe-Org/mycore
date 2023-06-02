<template>
  <div class="jobqueue-pagination">
    <nav>
      <ul class="pagination justify-content-center flex-wrap">
        <li :class="`page-item${currentPage>1?'':' disabled'}`">
          <a v-on:click.prevent="pageClicked(currentPage-1)" href="#"
             class="page-link">{{ model.i18n.jobqueue_job_pagination_previous }}</a>
        </li>
        <li :class="`flex-grow-1 text-center page-item${currentPage===pageNumber?' active':''}`"
            v-for="pageNumber in pageArray">
          <a v-on:click.prevent="pageClicked(pageNumber)" href="#change"
             class="page-link">{{ pageNumber }}</a>
        </li>
        <li :class="`page-item${currentPage<pageCount?'':' disabled'}`">
          <a v-on:click.prevent="pageClicked(currentPage+1)" href="#"
             :class="`page-link`">{{ model.i18n.jobqueue_job_pagination_next }}</a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script lang="ts" setup>

import {computed, reactive} from "vue";
import {i18n} from "@/api/I18N";

const props = defineProps<{
  currentPage: number,
  pageCount: number
}>()

const model = reactive({
  i18n: {} as any
});

[
  "jobqueue.job.pagination.next",
  "jobqueue.job.pagination.previous"
].forEach((key) => {
  let keyInObject = key.split(".").join("_");
  model.i18n[keyInObject] = "%" + keyInObject + "%";
  i18n(key).then((value) => {
    model.i18n[keyInObject] = value;
  });
});


const emit = defineEmits(['pageClicked'])

const pageClicked = (pageNumber: number) => {
  emit('pageClicked', pageNumber)
}


const pageArray = computed(() => {
  let pages = [];
  const start = Math.max(1, props.currentPage - 5);
  const end = Math.min(props.pageCount, props.currentPage + 5);
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }
  return pages;
});

</script>

<style scoped>

</style>