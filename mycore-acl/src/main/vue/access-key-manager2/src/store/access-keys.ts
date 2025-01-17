import { AccessKeyDto } from "@/dtos/accesskey";
import { defineStore } from "pinia";

interface AccessKeyState {
  data: AccessKeyDto[],
  currentPage: number,
  totalCount: number,
  pageSize: number,
}

export const useAccessKeyStore = defineStore('accessKey', {
  state: (): AccessKeyState => ({
    data: [],
    totalCount: 0,
    currentPage: 1,
    pageSize: 8,
  }),
  getters: {
    paginatedAccessKeys: (state) => {
      const start = (state.currentPage - 1) * state.pageSize;
      const end = start + state.pageSize;
      return state.data.slice(start, end);
    },
  },
  actions: {
    setData (accessKeys: AccessKeyDto[]) {
      this.data = accessKeys;
    },
    setTotalCount (totalCount: number) {
      this.totalCount = totalCount;
    },
    setPage(page: number) {
      this.currentPage = page;
    },
    setPageSize(size: number) {
      this.pageSize = size;
      this.currentPage = 1;
    },
    addItem(accessKey: AccessKeyDto) {
      this.data.push(accessKey);
      this.totalCount = this.totalCount + 1;
    },
    updateItem(accessKey: AccessKeyDto) {
      const index = this.data.findIndex((accessKey: AccessKeyDto) => accessKey.id === accessKey.id);
      if (index !== -1) {
        this.data[index] = accessKey;
      }
    },
    deleteItem(accessKeyId: string) {
      this.data = this.data.filter((accessKey: AccessKeyDto) => accessKey.id !== accessKeyId);
    },
  },
});