<template>
  <nav>
    <ul class="pagination">
      <li class="page-item" :class="currentPage === 1 ? 'disabled' : ''"
          @click="jumpToPage(currentPage - 1)" @keyDown="jumpToPage(currentPage - 1)">
        <a class="page-link" href="#">
          {{ $t('component.acl.accesskey.frontend.button.previous') }}
        </a>
      </li>
      <li v-for="page in pages" :key="page" class="page-item"
          :class="page === currentPage ? 'active' : ''" @click="jumpToPage(page)"
            @keyDown="jumpToPage(page)">
        <a class="page-link" href="#">
          {{ page }}
        </a>
      </li>
      <li class="page-item"
          :class="currentPage === totalPages || totalRows == 0 ? 'disabled' : ''"
          @click="jumpToPage(currentPage + 1)" @keyDown="jumpToPage(currentPage + 1)">
        <a class="page-link" href="#">
          {{ $t('component.acl.accesskey.frontend.button.next') }}
        </a>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps({
  totalRows: {
    type: Number,
    required: true,
  },
  currentPage: {
    type: Number,
    required: true,
  },
  perPage: {
    type: Number,
    required: true,
  },
});
const emit = defineEmits(['page-changed']);
const totalPages = computed(() => Math.ceil(props.totalRows / props.perPage));
const pages = computed(() => {
  if (props.totalRows === 0) {
    return [1];
  }
  const range: number[] = [];
  for (let i = 1; i <= totalPages.value; i += 1) {
    range.push(i);
  }
  return range;
});
const jumpToPage = (page: number) => {
  if (page > 0 && page <= totalPages.value && page !== props.currentPage) {
    emit('page-changed', page);
  }
};
</script>
