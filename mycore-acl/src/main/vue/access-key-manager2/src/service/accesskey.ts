import {
  AccessKeyDto,
  CreateAccessKeyDto,
  PartialUpdateAccessKeyDto,
} from '@/dtos/accesskey';

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

const request = async (
  url: URL,
  method: 'GET' | 'POST' | 'PATCH' | 'DELETE' | 'PUT',
  headers: HeadersInit = {},
  body?: string
): Promise<Response> => {
  const requestHeaders = new Headers(headers);
  const config: RequestInit = {
    method,
    headers: requestHeaders,
  };
  if (body) {
    config.body = body;
  }
  try {
    const response = await fetch(url, config);
    if (!response.ok) {
      throw new Error(`HTTP Error: ${response.status}`);
    }
    return response;
  } catch (error) {
    console.error('Request failed', error);
    throw error;
  }
};

export class MCRHttpClient {
  private baseUrl: string;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.endsWith('/') ? baseUrl.slice(0, -1) : baseUrl;
  }

  public async request(
    url: string,
    method: 'GET' | 'POST' | 'PATCH' | 'DELETE' | 'PUT',
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {},
    body?: string
  ): Promise<Response> {
    const urlObj = new URL(
      `${this.baseUrl}/${url.startsWith('/') ? url.slice(1) : url}`
    );
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        urlObj.searchParams.append(key, String(value));
      });
    }
    return request(urlObj, method, headers, body);
  }
}

export class MCRRestHttpClient extends MCRHttpClient {
  private defaultHeaders: HeadersInit = {};

  constructor(baseUrl: string, authStrategy?: AuthStrategy) {
    super(baseUrl);
    if (authStrategy) {
      this.defaultHeaders = {
        ...authStrategy.getHeaders(),
      };
    }
  }

  public async get(
    url: string,
    params: Record<string, string | number | boolean> = {},
    responseType: 'json',
    headers: HeadersInit = {}
  ): Promise<Response> {
    const requestHeaders = new Headers(
      Object.assign({}, this.defaultHeaders, headers)
    );
    if (responseType === 'json') {
      requestHeaders.set('Content-Accept', 'application/json');
    }
    return await super.request(url, 'GET', params, requestHeaders);
  }

  public async delete(
    url: string,
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {}
  ): Promise<void> {
    const requestHeaders = new Headers(
      Object.assign({}, this.defaultHeaders, headers)
    );
    await super.request(url, 'DELETE', params, requestHeaders);
  }

  public async post(
    url: string,
    body: string,
    bodyType: 'json',
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {}
  ): Promise<Response> {
    return this.write(url, 'POST', params, body, bodyType, headers);
  }

  public async patch(
    url: string,
    body: string,
    bodyType: 'json',
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {}
  ): Promise<Response> {
    return this.write(url, 'PATCH', params, body, bodyType, headers);
  }

  public async put(
    url: string,
    body: string,
    bodyType: 'json',
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {}
  ): Promise<Response> {
    return this.write(url, 'PUT', params, body, bodyType, headers);
  }

  private async write(
    url: string,
    method: 'POST' | 'PUT' | 'PATCH',
    params: Record<string, string | number | boolean> = {},
    body: string,
    bodyType: string,
    headers: HeadersInit = {}
  ): Promise<Response> {
    const requestHeaders = new Headers(
      Object.assign({}, this.defaultHeaders, headers)
    );
    if (bodyType === 'json') {
      requestHeaders.set('Content-Type', 'application/json');
    } else {
      throw new Error(`Body type: ${bodyType} is not allowed`);
    }
    return await super.request(url, method, params, requestHeaders, body);
  }
}

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
