import { AxiosResponse } from 'axios';
import AccessKeyDto from '@/dtos/AccessKeyDto';
import AccessKeyPermissionsDto from '@/dtos/AccessKeyPermissionsDto';
import { urlEncode } from '@/utils';
import instance from './axios';

const SECRET_ENCODING_TYPE_NAME = 'base64url';

const extractResponse = (response: AxiosResponse) => (
  { items: response.data, totalResults: parseInt(response.headers['x-total-count'], 10) });

export interface AccessKeyInformation {
  items: Array<AccessKeyDto>;
  totalResults: number;
}

export const getAccessKeys = async (objectId: string, offset: number, limit: number):
  Promise<AccessKeyInformation> => extractResponse(await instance
  .get<AccessKeyDto[]>('rsc/access-keys', { params: { object_id: objectId, offset, limit } }));
export const getAccessKey = async (objectId: string, value: string): Promise<AccessKeyDto> => (
  await instance.get<AccessKeyDto>(`rsc/access-keys/${objectId}/${urlEncode(value)}`, {
    params: { value_encoding: SECRET_ENCODING_TYPE_NAME },
  })).data;
export const createAccessKey = async (accessKey: AccessKeyDto): Promise<string> => (await instance
  .post('rsc/access-keys/', accessKey) as AxiosResponse)
  .headers.location.split('/').pop() as string;
export const patchAccessKey = async (objectId: string, value: string, accessKey: AccessKeyDto):
  Promise<void> => {
  await instance.patch(`rsc/access-keys/${objectId}/${urlEncode(value)}`, accessKey, {
    params: { value_encoding: SECRET_ENCODING_TYPE_NAME },
  });
};
export const removeAccessKey = async (objectId: string, value: string): Promise<void> => {
  await instance.delete(`rsc/access-keys/${objectId}/${urlEncode(value)}`, {
    params: { value_encoding: SECRET_ENCODING_TYPE_NAME },
  });
};
export const fetchPermissions = async (objectId: string): Promise<AccessKeyPermissionsDto> => (
  await instance.get<AccessKeyPermissionsDto>(`rsc/access-keys/${objectId}/permissions`)).data;
