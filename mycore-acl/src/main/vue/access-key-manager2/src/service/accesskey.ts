import { MCRRestHttpClient } from '@/common/client';
import {
  AccessKeyDto,
  CreateAccessKeyDto,
  PartialUpdateAccessKeyDto,
} from '@/dtos/accesskey';

export interface AccessKeyInformation {
  accessKeys: AccessKeyDto[];
  totalCount: number;
}

const extractResponse = async (
  response: Response
): Promise<AccessKeyInformation> => {
  const totalCount = response.headers.get('x-total-count');
  return {
    accessKeys: await response.json(),
    totalCount: totalCount ? parseInt(totalCount, 10) : 0,
  };
};

const API_URL = 'api/v2/access-keys/';

export class AccessKeyService {
  private client: MCRRestHttpClient;

  constructor(client: MCRRestHttpClient) {
    this.client = client;
  }

  public async getAccessKeys(
    permissions: string[],
    reference?: string,
    offset?: number,
    limit?: number
  ): Promise<AccessKeyInformation> {
    const params: Record<string, string | number> = {};
    if (reference) {
      params['reference'] = reference;
    }
    if (permissions.length > 0) {
      params['permissions'] = permissions.join(',');
    }
    if (offset) {
      params['offset'] = offset;
    }
    if (limit) {
      params['limit'] = limit;
    }
    return extractResponse(await this.client.get(API_URL, params, 'json'));
  }

  public async getAccessKeysByReference(
    reference: string,
    offset?: number,
    limit?: number
  ): Promise<AccessKeyInformation> {
    const params: Record<string, string | number> = {};
    if (reference) {
      params['reference'] = reference;
    }
    if (offset) {
      params['offset'] = offset;
    }
    if (limit) {
      params['limit'] = limit;
    }
    return extractResponse(await this.client.get(API_URL, params, 'json'));
  }

  public async getAccessKey(id: string): Promise<AccessKeyDto> {
    return (await this.client.get(`${API_URL}${id}`, {}, 'json')).json();
  }

  public async createAccessKey(accessKey: CreateAccessKeyDto): Promise<string> {
    const response = await this.client.post(
      API_URL,
      JSON.stringify(accessKey),
      'json'
    );
    const locationHeader = response.headers.get('Location');
    return locationHeader?.split('/').pop() as string;
  }

  public async patchAccessKey(
    id: string,
    accessKey: PartialUpdateAccessKeyDto
  ): Promise<void> {
    await this.client.patch(
      `${API_URL}${id}`,
      JSON.stringify(accessKey),
      'json'
    );
  }

  public async deleteAccessKey(id: string): Promise<void> {
    this.client.delete(`${API_URL}${id}`);
  }
}
