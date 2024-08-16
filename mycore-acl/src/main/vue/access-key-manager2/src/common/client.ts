export interface MCRClientAuthStrategy {
  getHeaders(): Record<string, string>;
}

export class MCRAccessTokenClientAuthStrategy implements MCRClientAuthStrategy {
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
  private authStrategy: MCRClientAuthStrategy | null = null;

  constructor(
    baseUrl: string,
    authStrategy: MCRClientAuthStrategy | null = null
  ) {
    super(baseUrl);
    this.authStrategy = authStrategy;
  }

  private getHeaders(customHeaders: HeadersInit = {}): Headers {
    return new Headers({
      ...(this.authStrategy?.getHeaders() ?? {}),
      ...customHeaders,
    });
  }

  public setAuthStrategy(strategy: MCRClientAuthStrategy | null) {
    this.authStrategy = strategy;
  }

  public async get(
    url: string,
    params: Record<string, string | number | boolean> = {},
    responseType: 'json',
    headers: HeadersInit = {}
  ): Promise<Response> {
    const requestHeaders = this.getHeaders(headers);
    if (responseType === 'json') {
      requestHeaders.set('Accept', 'application/json');
    }
    return await super.request(url, 'GET', params, requestHeaders);
  }

  public async delete(
    url: string,
    params: Record<string, string | number | boolean> = {},
    headers: HeadersInit = {}
  ): Promise<void> {
    const requestHeaders = this.getHeaders(headers);
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
    const requestHeaders = this.getHeaders(headers);
    if (bodyType === 'json') {
      requestHeaders.set('Content-Type', 'application/json');
    } else {
      throw new Error(`Body type: ${bodyType} is not allowed`);
    }
    return await super.request(url, method, params, requestHeaders, body);
  }
}
