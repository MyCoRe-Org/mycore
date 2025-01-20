import { AccessKeyDto } from "@/dtos/accesskey";
import { defineStore } from "pinia";

interface AccessKeyState {
  accessKeys: AccessKeyDto[],
  currentPage: number,
  totalCount: number,
  pageSize: number,
}

export const useAccessKeyStore = defineStore('accessKeys', {
  state: (): AccessKeyState => ({
    accessKeys: [],
    totalCount: 0,
    currentPage: 1,
    pageSize: 8,
  }),
  getters: {
    paginatedAccessKeys: (state) => {
      const start = (state.currentPage - 1) * state.pageSize;
      const end = start + state.pageSize;
      return state.accessKeys.slice(start, end);
    },
  },
  actions: {
    setAccessKeys (accessKeys: AccessKeyDto[]) {
      this.accessKeys = accessKeys;
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
    addAccessKey(accessKey: AccessKeyDto) {
      this.accessKeys.push(accessKey);
      this.totalCount = this.totalCount + 1;
    },
    updateAccessKey(accessKey: AccessKeyDto) {
      const index = this.accessKeys.findIndex((accessKey: AccessKeyDto) => accessKey.id === accessKey.id);
      if (index !== -1) {
        this.accessKeys[index] = accessKey;
      }
    },
    removeAccessKey(accessKeyId: string) {
      this.accessKeys = this.accessKeys.filter((accessKey: AccessKeyDto) => accessKey.id !== accessKeyId);
    },
  },
});