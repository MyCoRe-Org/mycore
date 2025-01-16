import { AxiosResponse } from "axios";
import { AccessKeyDto, CreateAccessKeyDto, PartialUpdateAccessKeyDto } from "@/dtos/accesskey";
import instance from "./axios";

const extractResponse = (response: AxiosResponse) => {
  return { items: response.data, totalResults: parseInt(response.headers["x-total-count"], 10) };
};

export interface AccessKeyInformation {
  items: Array<AccessKeyDto>;
  totalResults: number;
}

const API_URL = "api/v2/access-keys/";

export const getAccessKeysByReferenceAndPermission = async (
  reference: string,
  permissions: string[],
  offset: number,
  limit: number
): Promise<AccessKeyInformation> =>
  extractResponse(
    await instance.get<AccessKeyDto[]>(API_URL, {
      params: {
        reference,
        permissions: permissions.join(","),
        offset,
        limit,
      },
    })
  );

export const getAccessKeys = async (offset: number, limit: number): Promise<AccessKeyInformation> =>
  extractResponse(
    await instance.get<AccessKeyDto[]>(API_URL, {
      params: {
        offset,
        limit,
      },
    })
  );

export const getAccessKey = async (id: string): Promise<AccessKeyDto> =>
  (await instance.get<AccessKeyDto>(`${API_URL}${id}`)).data;
export const createAccessKey = async (accessKey: CreateAccessKeyDto): Promise<string> =>
  (
    (await instance.post(API_URL, accessKey, {
      headers: {
        "Content-Type": "application/json",
      },
    })) as AxiosResponse
  ).headers.location
    .split("/")
    .pop() as string;
export const patchAccessKey = async (
  id: string,
  accessKey: PartialUpdateAccessKeyDto
): Promise<void> => {
  await instance.patch(`${API_URL}${id}`, accessKey);
};
export const removeAccessKey = async (id: string): Promise<void> => {
  await instance.delete(`${API_URL}${id}`);
};
