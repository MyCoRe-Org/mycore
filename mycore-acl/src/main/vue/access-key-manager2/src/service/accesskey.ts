import {
  AccessKeyDto,
  CreateAccessKeyDto,
  PartialUpdateAccessKeyDto,
} from '@/dtos/accesskey';
import axios, { AxiosInstance, AxiosResponse } from 'axios';

export interface AuthStrategy {
  getHeaders(): Record<string, string>;
}

export class AccessTokenAuthStrategy implements AuthStrategy {
  private accessToken;

  constructor(accessToken: string) {
    this.accessToken = accessToken;
  }

  public getHeaders(): Record<string, string> {
    return {
      Authorization: `Bearer ${this.accessToken}`,
    };
  }
}

export interface AccessKeyInformation {
  accessKeys: AccessKeyDto[];
  totalCount: number;
}

const extractResponse = (response: AxiosResponse): AccessKeyInformation => {
  return {
    accessKeys: response.data,
    totalCount: parseInt(response.headers['x-total-count'], 10),
  };
};

const API_URL = 'api/v2/access-keys/';

// TODO switch to fetch
export class AccessKeyService {
  private axiosInstance: AxiosInstance;

  constructor(baseUrl: string, authStrategy: AuthStrategy) {
    this.axiosInstance = axios.create({
      baseURL: baseUrl,
      timeout: 5000,
    });
    this.axiosInstance.defaults.headers.common = {
      ...authStrategy.getHeaders(),
    };
  }

  public async getAccessKeys(
    permissions: string[],
    reference?: string,
    offset?: number,
    limit?: number
  ): Promise<AccessKeyInformation> {
    return extractResponse(
      await this.axiosInstance.get<AccessKeyDto[]>(API_URL, {
        params: {
          reference,
          permissions:
            permissions.length > 0 ? permissions.join(',') : undefined,
          offset,
          limit,
        },
      })
    );
  }

  public async getAccessKeysByReference(
    reference: string,
    offset: number,
    limit: number
  ): Promise<AccessKeyInformation> {
    return extractResponse(
      await this.axiosInstance.get<AccessKeyDto[]>(API_URL, {
        params: { reference, offset, limit },
      })
    );
  }

  public async getAccessKey(id: string): Promise<AccessKeyDto> {
    return (await this.axiosInstance.get<AccessKeyDto>(`${API_URL}${id}`)).data;
  }

  public async createAccessKey(accessKey: CreateAccessKeyDto): Promise<string> {
    return (
      (await this.axiosInstance.post(API_URL, accessKey, {
        headers: { 'Content-Type': 'application/json' },
      })) as AxiosResponse
    ).headers.location
      .split('/')
      .pop() as string;
  }

  public async patchAccessKey(
    id: string,
    accessKey: PartialUpdateAccessKeyDto
  ): Promise<void> {
    await this.axiosInstance.patch(`${API_URL}${id}`, accessKey);
  }

  public async deleteAccessKey(id: string): Promise<void> {
    await this.axiosInstance.delete(`${API_URL}${id}`);
  }
}
