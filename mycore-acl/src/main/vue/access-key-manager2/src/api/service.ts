import axios, { AxiosInstance, AxiosResponse } from "axios";
import { AccessKeyDto, CreateAccessKeyDto, PartialUpdateAccessKeyDto } from "@/dtos/accesskey";

const extractResponse = (response: AxiosResponse) => {
  return { items: response.data, totalResults: parseInt(response.headers["x-total-count"], 10) };
};

export interface AccessKeyInformation {
  items: Array<AccessKeyDto>;
  totalResults: number;
}

const API_URL = "api/v2/access-keys/";

export class AccessKeyService {

  private _instance: AxiosInstance;

  constructor(webApplicationBaseUrl: string, authorizationHeader: string) {
    this._instance = axios.create({
      baseURL: webApplicationBaseUrl,
    });
    this._instance.defaults.headers['Authorization'] = authorizationHeader;
  }

  public async getAccessKeysByReferenceAndPermission(
    reference: string,
    permissions: string[],
    offset: number,
    limit: number
  ): Promise<AccessKeyInformation> {
    return extractResponse(
      await this._instance.get<AccessKeyDto[]>(API_URL, {
        params: {
          reference,
          permissions: permissions.join(","),
          offset,
          limit,
        },
      })
    );
  }

  public async getAccessKeys(offset: number, limit: number): Promise<AccessKeyInformation> {
    return extractResponse(
      await this._instance.get<AccessKeyDto[]>(API_URL, {
        params: {
          offset,
          limit,
        },
      })
    );
  }

  public async getAccessKey (id: string): Promise<AccessKeyDto> {
    return (await this._instance.get<AccessKeyDto>(`${API_URL}${id}`)).data;
  }

  public async createAccessKey (accessKey: CreateAccessKeyDto): Promise<string> {
    return (
      (await this._instance.post(API_URL, accessKey, {
        headers: {
          "Content-Type": "application/json",
        },
      })) as AxiosResponse
    ).headers.location
      .split("/")
      .pop() as string;
  }

  public async patchAccessKey (
    id: string,
    accessKey: PartialUpdateAccessKeyDto
  ): Promise<void> {
    await this._instance.patch(`${API_URL}${id}`, accessKey);
  }

  public async removeAccessKey (id: string): Promise<void> {
    await this._instance.delete(`${API_URL}${id}`);
  }
}