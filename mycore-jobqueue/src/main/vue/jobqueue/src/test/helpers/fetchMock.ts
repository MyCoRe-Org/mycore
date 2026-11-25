import { vi } from 'vitest';

import { WEB_APPLICATION_BASE_URL } from '@/test/setup';

export interface FetchCall {
  url: string;
  init?: RequestInit;
}

type Responder = (url: string, init?: RequestInit) => Response | Promise<Response>;

interface Route {
  matches: (url: string) => boolean;
  respond: Responder;
}

const TRANSLATE_PATH = 'rsc/locale/translate/';

export const JWT = { login_success: true, access_token: 'test-token', token_type: 'Bearer' };

export function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init.headers ?? {}) },
  });
}

export function translation(key: string): string {
  return `t(${key})`;
}

export class FetchMock {
  readonly calls: FetchCall[] = [];

  private readonly routes: Route[] = [];

  constructor() {
    this.on(`${TRANSLATE_PATH}`, url => new Response(translation(url.split('/').pop() ?? '')));
    this.on('rsc/jwt', () => jsonResponse(JWT));
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = input.toString();
        this.calls.push({ url, init });
        const route = [...this.routes].reverse().find(entry => entry.matches(url));
        if (route === undefined) {
          throw new Error(`unexpected request: ${url}`);
        }
        return route.respond(url, init);
      })
    );
  }

  on(path: string | RegExp, respond: Responder): this {
    const matches =
      typeof path === 'string'
        ? (url: string) => url.startsWith(WEB_APPLICATION_BASE_URL + path)
        : (url: string) => path.test(url);
    this.routes.push({ matches, respond });
    return this;
  }

  requests(path: string): FetchCall[] {
    return this.calls.filter(call => call.url.startsWith(WEB_APPLICATION_BASE_URL + path));
  }

  apiRequests(): FetchCall[] {
    return this.requests('api/');
  }

  translatedKeys(): string[] {
    return this.requests(TRANSLATE_PATH).map(call => call.url.split('/').pop() ?? '');
  }
}
