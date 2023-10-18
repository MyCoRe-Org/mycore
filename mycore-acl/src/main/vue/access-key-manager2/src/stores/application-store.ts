import { defineStore } from 'pinia';
import { AccessKey, AccessKeyInformation } from '@/utils';
import {
  addDerivateAccessKey,
  addObjectAccessKey,
  fetchDerivateAccessKeys,
  fetchDerivateAccessKey,
  fetchObjectAccessKeys,
  fetchObjectAccessKey,
  removeDerivateAccessKey,
  removeObjectAccessKey,
  updateDerivateAccessKey,
  updateObjectAccessKey,
} from '@/api/service';

interface State {
  accessKeys: AccessKey[]
  modalData: AccessKey | null
  totalCount: number
  limit: number
  offset: number
  objectId: string | undefined
  derivateId: string | undefined
}

export const useApplicationStore = defineStore('application', {
  state: (): State => ({
    accessKeys: [],
    modalData: null,
    totalCount: 0,
    limit: 8,
    offset: 0,
    objectId: undefined,
    derivateId: undefined,
  }),
  actions: {
    async fetch(): Promise<void> {
      const objectId = this.objectId as string;
      const accessKeyInformation: AccessKeyInformation = (!this.derivateId)
        ? await fetchObjectAccessKeys(objectId, this.offset, this.limit)
        : await fetchDerivateAccessKeys(objectId, this.derivateId, this.offset, this.limit);
      this.accessKeys = accessKeyInformation.items;
      this.totalCount = accessKeyInformation.totalResults;
    },
    async createAccessKey(accessKey: AccessKey): Promise<string> {
      const objectId = this.objectId as string;
      const secret = (!this.derivateId) ? await addObjectAccessKey(objectId, accessKey)
        : await addDerivateAccessKey(objectId, this.derivateId, accessKey);
      if ((this.totalCount % this.limit === 0) || ((this.totalCount - this.limit) < this.offset)) {
        const result = (!this.derivateId) ? await fetchObjectAccessKey(objectId, secret)
          : await fetchDerivateAccessKey(objectId, this.derivateId, secret);
        if (this.totalCount % this.limit === 0) {
          this.accessKeys = [result];
          this.offset = this.totalCount;
        } else {
          this.accessKeys.push(result);
        }
        this.totalCount += 1;
      } else {
        this.offset = this.totalCount - (this.totalCount % this.limit);
        this.fetch();
      }
      return secret;
    },
    async updateAccessKey(accessKey: AccessKey): Promise<void> {
      const objectId = this.objectId as string;
      if (!this.derivateId) {
        await updateObjectAccessKey(objectId, accessKey);
        const result = await fetchObjectAccessKey(objectId, accessKey.secret);
        const index = this.accessKeys.findIndex((k) => k.secret === accessKey.secret);
        this.accessKeys[index] = accessKey;
        this.modalData = result;
      } else {
        await updateDerivateAccessKey(objectId, this.derivateId, accessKey);
        const result = await fetchDerivateAccessKey(objectId, this.derivateId, accessKey.secret);
        const index = this.accessKeys.findIndex((k) => k.secret === accessKey.secret);
        this.accessKeys[index] = accessKey;
        this.modalData = result;
      }
    },
    async removeAccessKey(accessKeySecret: string): Promise<void> {
      const objectId = this.objectId as string;
      if (!this.derivateId) {
        await removeObjectAccessKey(objectId, accessKeySecret);
      } else {
        await removeDerivateAccessKey(objectId, this.derivateId, accessKeySecret);
      }
      if ((this.offset === 0) && (this.totalCount > this.limit)) {
        this.fetch();
      } else if ((this.offset >= this.limit) && (this.totalCount % this.limit === 1)) {
        this.offset -= this.limit;
        this.fetch();
      } else {
        this.accessKeys = this.accessKeys.filter((k) => k.secret !== accessKeySecret);
        this.totalCount -= 1;
      }
    },
  },
});
