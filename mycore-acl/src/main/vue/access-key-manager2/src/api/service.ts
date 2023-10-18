import { AxiosResponse } from 'axios';
import {
  AccessKey,
  AccessKeyInformation,
} from '@/utils';
import instance from './axios';

const SECRET_ENCODING_TYPE_NAME = 'base64url';

const urlEncode = (value: string) => btoa(value).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

const extractResponse = (response: AxiosResponse) => (
  { items: response.data, totalResults: parseInt(response.headers['x-total-count'], 10) });

export const fetchObjectAccessKeys = async (objectId: string, offset: number, limit: number):
  Promise<AccessKeyInformation> => extractResponse(await instance
  .get<AccessKey[]>(`api/v2/objects/${objectId}/accesskeys`, { params: { offset, limit } }));
export const fetchDerivateAccessKeys = async (objectId: string, derivateId: string, offset: number, limit: number):
  Promise<AccessKeyInformation> => extractResponse(await instance
  .get<AccessKeyInformation>(`api/v2/objects/${objectId}/derivates/${derivateId}/accesskeys`,
    { params: { offset, limit } }));
export const fetchObjectAccessKey = async (objectId: string, secret: string): Promise<AccessKey> => (
  await instance.get<AccessKey>(`api/v2/objects/${objectId}/accesskeys/${secret}`)).data;
export const fetchDerivateAccessKey = async (objectId: string, derivateId: string, secret: string):
  Promise<AccessKey> => (
  await instance.get<AccessKey>(`api/v2/objects/${objectId}/derivates/${derivateId}/accesskeys/${secret}`)).data;
export const addDerivateAccessKey = async (objectId: string, derivateId: string, accessKey: AccessKey):
    Promise<string> => (
    await instance.post(`api/v2/objects/${objectId}/derivates/${derivateId}/accesskeys`, accessKey) as AxiosResponse)
      .headers.location.split('/').pop() as string;
export const addObjectAccessKey = async (objectId: string, accessKey: AccessKey): Promise<string> => (await instance
  .post(`api/v2/objects/${objectId}/accesskeys`, accessKey) as AxiosResponse)
  .headers.location.split('/').pop() as string;
export const updateDerivateAccessKey = async (objectId: string, derivateId: string, accessKey: AccessKey):
  Promise<void> => {
  await instance.put(`api/v2/objects/${objectId}/derivates/${derivateId}/accesskeys/${urlEncode(accessKey.secret)}`,
    accessKey, { params: { secret_encoding: SECRET_ENCODING_TYPE_NAME } });
};
export const updateObjectAccessKey = async (objectId: string, accessKey: AccessKey): Promise<void> => {
  await instance.put(`api/v2/objects/${objectId}/accesskeys/${urlEncode(accessKey.secret)}`, accessKey, {
    params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
  });
};
export const removeDerivateAccessKey = async (objectId: string, derivateId: string, secret: string): Promise<void> => {
  await instance.delete(`api/v2/objects/${objectId}/derivates/${derivateId}/accesskeys/${urlEncode(secret)}`, {
    params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
  });
};
export const removeObjectAccessKey = async (objectId: string, secret: string): Promise<void> => {
  await instance.delete(`api/v2/objects/${objectId}/accesskeys/${urlEncode(secret)}`, {
    params: { secret_encoding: SECRET_ENCODING_TYPE_NAME },
  });
};
