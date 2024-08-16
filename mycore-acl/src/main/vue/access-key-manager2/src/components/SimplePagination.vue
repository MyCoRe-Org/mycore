<template>
  <nav>
    <ul class="pagination">
      <li class="page-item" :class="previousButtonDisabled ? 'disabled' : ''">
        <button
          class="page-link"
          :aria-label="translate('button.previous')"
          :disabled="previousButtonDisabled"
          :aria-disabled="previousButtonDisabled"
          @click="jumpToPage(currentPage - 1)"
        >
          {{ translate('button.previous') }}
        </button>
      </li>
      <li
        v-for="page in pages"
        :key="page"
        class="page-item"
        :class="page === currentPage ? 'active' : ''"
      >
        <button
          class="page-link"
          :aria-label="String(page)"
          :aria-current="page === currentPage"
          :aria-disabled="page === currentPage"
          :disabled="page === currentPage"
          @click="jumpToPage(page)"
        >
          {{ page }}
        </button>
      </li>
      <li class="page-item" :class="nextButtonDisabled ? 'disabled' : ''">
        <button
          class="page-link"
          :aria-label="translate('button.next')"
          :disabled="nextButtonDisabled"
          :aria-disabled="nextButtonDisabled"
          @click="jumpToPage(currentPage + 1)"
        >
          {{ translate('button.next') }}
        </button>
      </li>
    </ul>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { getI18nKey } from '@/common/utils';

const { t } = useI18n();
const translate = (key: string) => t(getI18nKey(key));

const props = defineProps<{
  totalRows: number;
  currentPage: number;
  perPage: number;
}>();

const emit = defineEmits<{
  (event: 'change-page', page: number): void;
}>();

const totalPages = computed((): number =>
  Math.ceil(props.totalRows / props.perPage)
);
const nextButtonDisabled = computed(
  (): boolean => props.currentPage === totalPages.value || props.totalRows === 0
);
const previousButtonDisabled = computed((): boolean => props.currentPage === 1);
const pages = computed((): number[] => {
  if (props.totalRows === 0) {
    return [1];
  }
  const range: number[] = [];
  for (let i = 1; i <= totalPages.value; i += 1) {
    range.push(i);
  }
  return range;
});

const jumpToPage = (page: number): void => {
  if (page <= 0 || page > totalPages.value || page === props.currentPage) {
    return;
  }
  emit('change-page', page);
};
</script>
