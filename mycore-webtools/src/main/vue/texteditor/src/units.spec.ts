import { afterEach, describe, expect, it, vi } from 'vitest';

import { ClassificationsContentHandler } from '@/apis/ClassificationsContentHandler';
import { ContentHandlerSelector } from '@/apis/ContentHandlerSelector';
import {
  isDerivate,
  isObjectContent,
  isObjectContentFile,
  isObjectContents,
  supportsExpandedObjectView,
} from '@/apis/ObjectPath';
import { ObjectsContentHandler } from '@/apis/ObjectsContentHandler';
import { getPrismLanguage } from '@/modules/prismjs-languages';
import { FetchMock, jsonResponse } from '@/test/helpers/fetchMock';
import {
  EXPANDED_VIEW_PREFERENCE_KEY,
  isUpdateButtonDisabled,
  isUpdateButtonVisible,
  readExpandedViewPreference,
  resolveExpandedViewOnNavigation,
  viewModeButtonClass,
  writeExpandedViewPreference,
} from '@/views/TextEditorViewState';

const BASE = 'http://localhost:3000/mir/';
const OBJECT = 'mir_mods_00000001';
const DERIVATE = `${OBJECT}/derivates/mir_derivate_00000001`;

describe('ObjectPath', () => {
  it.each([
    [OBJECT, { content: false, contents: false, file: false, derivate: false, expanded: true }],
    [DERIVATE, { content: false, contents: false, file: false, derivate: true, expanded: false }],
    [`${DERIVATE}/contents`, { content: true, contents: true, file: false, derivate: false, expanded: false }],
    [`${DERIVATE}/contents/a/b.xml`, { content: true, contents: false, file: true, derivate: false, expanded: false }],
  ])('classifies %s', (path, expected) => {
    expect({
      content: isObjectContent(path),
      contents: isObjectContents(path),
      file: isObjectContentFile(path),
      derivate: isDerivate(path),
      expanded: supportsExpandedObjectView(path),
    }).toEqual(expected);
  });
});

describe('TextEditorViewState', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('stores and reads the expanded view preference', () => {
    expect(readExpandedViewPreference(localStorage)).toBe(false);

    writeExpandedViewPreference(localStorage, true);
    expect(localStorage.getItem(EXPANDED_VIEW_PREFERENCE_KEY)).toBe('true');
    expect(readExpandedViewPreference(localStorage)).toBe(true);

    writeExpandedViewPreference(localStorage, false);
    expect(readExpandedViewPreference(localStorage)).toBe(false);
  });

  it('ignores missing and blocked storage', () => {
    const blocked = {
      getItem: () => {
        throw new Error('blocked');
      },
      setItem: () => {
        throw new Error('blocked');
      },
    } as unknown as Storage;

    expect(readExpandedViewPreference(undefined)).toBe(false);
    expect(readExpandedViewPreference(blocked)).toBe(false);
    expect(() => writeExpandedViewPreference(undefined, true)).not.toThrow();
    expect(() => writeExpandedViewPreference(blocked, true)).not.toThrow();
  });

  it('derives the toolbar state', () => {
    expect(isUpdateButtonVisible(true, false)).toBe(true);
    expect(isUpdateButtonVisible(true, true)).toBe(false);
    expect(isUpdateButtonVisible(false, false)).toBe(false);
    expect(isUpdateButtonDisabled(true, false)).toBe(false);
    expect(isUpdateButtonDisabled(true, true)).toBe(true);
    expect(isUpdateButtonDisabled(false, false)).toBe(true);
    expect(viewModeButtonClass(true)).toBe('btn-primary active');
    expect(viewModeButtonClass(false)).toBe('btn-outline-primary');
    expect(resolveExpandedViewOnNavigation(true, true)).toBe(true);
    expect(resolveExpandedViewOnNavigation(true, false)).toBe(false);
    expect(resolveExpandedViewOnNavigation(false, true)).toBe(false);
  });
});

describe('getPrismLanguage', () => {
  it.each(['xml', 'markdown', 'csv', 'yaml'])('knows %s', language => {
    expect(getPrismLanguage(`text/${language}`)?.language).toBe(language);
    expect(getPrismLanguage(`text/${language}`)?.grammar).toBeDefined();
  });

  it('returns nothing for unknown languages and fails for invalid content types', () => {
    expect(getPrismLanguage('application/octet-stream')).toBeUndefined();
    expect(() => getPrismLanguage('xml')).toThrow('Invalid contentType xml');
  });
});

describe('content handlers', () => {
  const objects = new ObjectsContentHandler(BASE);
  const classifications = new ClassificationsContentHandler(BASE);

  it('prettifies xml', () => {
    expect(objects.prettifyXml('<root><item x="1"><empty/></item>  <text>value</text></root>')).toBe(
      ['<root>', '  <item x="1">', '    <empty/>', '  </item>', '  <text>value</text>', '</root>'].join('\n')
    );
    expect(objects.prettifyXml('<a><b><c/></b></a>')).toBe(['<a>', '  <b>', '    <c/>', '  </b>', '</a>'].join('\n'));
  });

  it('builds errors from the error headers of the response', () => {
    const response = new Response(null, {
      status: 500,
      headers: { 'X-Error-Message': 'Boom', 'X-Error-Detail': 'detail', 'X-Error-Uuid': 'uuid' },
    });

    const message = objects.buildError('Unable to save x.', response).message;

    expect(message).toContain('Unable to save x. Request failed with status code 500. Boom - detail -');
    expect(message).toContain('Error Log Code: uuid');
  });

  it('derives write access from the allowed methods', () => {
    expect(objects.handleWriteAccessResponse(OBJECT, new Response(null, { status: 200 }))).toBe(true);
    expect(() => objects.handleWriteAccessResponse(OBJECT, new Response(null, { status: 403 }))).toThrow(
      `Unable get OPTIONS of ${OBJECT}.`
    );
  });

  it('supports locking and expanded view only for objects', () => {
    expect(objects.supportsLocking(OBJECT)).toBe(true);
    expect(objects.supportsLocking(DERIVATE)).toBe(false);
    expect(objects.supportsExpandedView(OBJECT)).toBe(true);
    expect(objects.supportsExpandedView(DERIVATE)).toBe(false);
    expect(objects.dirtyAfterSave(OBJECT)).toBe(true);
    expect(objects.dirtyAfterSave(`${DERIVATE}/contents/a.xml`)).toBe(false);
    expect(classifications.supportsLocking('mir_institutes')).toBe(false);
    expect(classifications.supportsExpandedView('mir_institutes')).toBe(false);
    expect(classifications.dirtyAfterSave('mir_institutes')).toBe(false);
  });

  it('rejects locking for handlers without lock support', async () => {
    await expect(classifications.lock('x')).rejects.toThrow('Locking is not supported by this content handler.');
    await expect(classifications.unlock('x')).rejects.toThrow('Locking is not supported by this content handler.');
    await expect(classifications.hasWriteAccess('x')).resolves.toBe(true);
  });

  it('checks write access of content files on their derivate', async () => {
    const backend = new FetchMock()
      .on('rsc/jwt', () => jsonResponse({ login_success: true, access_token: 'token', token_type: 'Bearer' }))
      .on(/\/try$/, () => new Response(null, { status: 200 }));

    await objects.hasWriteAccess(`${DERIVATE}/contents/a/b.xml`);
    await objects.hasWriteAccess(OBJECT);

    expect(backend.requests('api/').map(call => call.url)).toEqual([
      `${BASE}api/v2/objects/${DERIVATE}/try`,
      `${BASE}api/v2/objects/${OBJECT}/try`,
    ]);
  });

  it('reports lock owners and failures', async () => {
    const backend = new FetchMock().on(/\/lock$/, () => new Response(null, { status: 409 }));
    vi.spyOn(console, 'log').mockImplementation(() => {});

    await expect(objects.lock(OBJECT)).resolves.toEqual({ status: 'not_owner' });
    await expect(objects.unlock(OBJECT)).resolves.toEqual({ status: 'not_owner' });

    backend.on(/\/lock$/, () => new Response(null, { status: 200 }));
    await expect(objects.lock(OBJECT)).resolves.toEqual({ status: 'locked' });
    await expect(objects.unlock(OBJECT)).resolves.toEqual({ status: 'unlocked' });

    backend.on(/\/lock$/, () => new Response(null, { status: 500 }));
    await expect(objects.unlock(OBJECT)).rejects.toThrow(`Unable to unlock ${OBJECT}.`);
  });

  it('registers handlers by type', () => {
    ContentHandlerSelector.registerContentHandler('test', classifications);

    expect(ContentHandlerSelector.get('test')).toBe(classifications);
    expect(ContentHandlerSelector.get('unknown')).toBeUndefined();
  });
});

describe('Auth', () => {
  async function importAuth() {
    vi.resetModules();
    return await import('@/apis/Auth');
  }

  afterEach(() => {
    vi.stubEnv('DEV', false);
  });

  it('requests the jwt once and reuses it', async () => {
    const backend = new FetchMock().on('rsc/jwt', () =>
      jsonResponse({ login_success: true, access_token: 'abc', token_type: 'Bearer' })
    );
    const { getAuthorizationHeader } = await importAuth();

    expect(await getAuthorizationHeader(BASE)).toBe('Bearer abc');
    expect(await getAuthorizationHeader(BASE)).toBe('Bearer abc');
    expect(backend.calls.map(call => call.url)).toEqual([`${BASE}rsc/jwt`]);
  });

  it('fails when the login is not successful and tries again on the next request', async () => {
    const backend = new FetchMock().on('rsc/jwt', () => jsonResponse({ login_success: false }));
    const { getAuthorizationHeader } = await importAuth();

    await expect(getAuthorizationHeader(BASE)).rejects.toThrow('Login failed');

    backend.on('rsc/jwt', () => jsonResponse({ login_success: true, access_token: 'abc', token_type: 'Bearer' }));
    expect(await getAuthorizationHeader(BASE)).toBe('Bearer abc');
  });

  it('requests the jwt only once for concurrent requests', async () => {
    const backend = new FetchMock().on('rsc/jwt', () =>
      jsonResponse({ login_success: true, access_token: 'abc', token_type: 'Bearer' })
    );
    const { getAuthorizationHeader } = await importAuth();

    const headers = await Promise.all([getAuthorizationHeader(BASE), getAuthorizationHeader(BASE)]);

    expect(headers).toEqual(['Bearer abc', 'Bearer abc']);
    expect(backend.calls).toHaveLength(1);
  });

  it('requests a new jwt shortly before the current one expires', async () => {
    const now = Date.UTC(2026, 10, 25, 12, 0, 0);
    vi.spyOn(Date, 'now').mockReturnValue(now);
    const jwt = (exp: number, id: string) =>
      `header.${btoa(JSON.stringify({ exp: exp / 1000, id })).replace(/=+$/, '')}.signature`;
    let issued = 0;
    const backend = new FetchMock().on('rsc/jwt', () =>
      jsonResponse({ login_success: true, access_token: jwt(now + 600_000, `t${++issued}`), token_type: 'Bearer' })
    );
    const { getAuthorizationHeader } = await importAuth();

    const first = await getAuthorizationHeader(BASE);
    vi.spyOn(Date, 'now').mockReturnValue(now + 500_000);
    expect(await getAuthorizationHeader(BASE)).toBe(first);
    expect(backend.calls).toHaveLength(1);

    vi.spyOn(Date, 'now').mockReturnValue(now + 540_000);
    expect(await getAuthorizationHeader(BASE)).not.toBe(first);
    expect(backend.calls).toHaveLength(2);
  });

  it('uses basic authentication in development mode', async () => {
    vi.stubEnv('DEV', true);
    const { getAuthorizationHeader } = await importAuth();

    expect(await getAuthorizationHeader(BASE)).toBe(`Basic ${btoa('administrator:alleswirdgut')}`);
  });
});

describe('router', () => {
  async function importRouter() {
    vi.resetModules();
    return await import('@/router');
  }

  afterEach(() => {
    vi.stubEnv('DEV', false);
    (window as unknown as Record<string, unknown>).mycore = { webApplicationBaseURL: BASE };
  });

  it('derives context and base urls from the page', async () => {
    const { getContext, getMCRApplicationBaseURL, getWebApplicationBaseURL } = await importRouter();

    expect(getContext()).toBe('/mir/modules/webtools/texteditor/');
    expect(getWebApplicationBaseURL()).toBe(BASE);
    expect(getMCRApplicationBaseURL()).toBe(BASE);
  });

  it('fails without web application base url', async () => {
    const { getWebApplicationBaseURL } = await importRouter();
    (window as unknown as Record<string, unknown>).mycore = {};

    expect(() => getWebApplicationBaseURL()).toThrow("'mycore.webApplicationBaseURL' is not set");
  });

  it('uses the development servers in development mode', async () => {
    vi.stubEnv('DEV', true);
    const { getContext, getMCRApplicationBaseURL } = await importRouter();

    expect(getContext()).toBe('/');
    expect(getMCRApplicationBaseURL()).toBe('http://localhost:8080/jportal/');
  });

  it('passes type and joined id to the editor', async () => {
    const { default: router } = await importRouter();

    const route = router.resolve(`/objects/${DERIVATE}/contents`);
    const props = (route.matched[0]!.props as { default: (r: typeof route) => object }).default(route);

    expect(route.name).toBe('TextEditor');
    expect(props).toEqual({ type: 'objects', id: `${DERIVATE}/contents` });
  });
});
