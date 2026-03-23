import { vi } from 'vitest';

const localStorageState = new Map<string, string>();

export function installLocalStorageMock(): void {
  Object.defineProperty(window, 'localStorage', {
    configurable: true,
    value: {
      get length() {
        return localStorageState.size;
      },
      getItem: (key: string) => localStorageState.get(key) ?? null,
      key: (index: number) => Array.from(localStorageState.keys())[index] ?? null,
      setItem: (key: string, value: string) => {
        localStorageState.set(key, String(value));
      },
      removeItem: (key: string) => {
        localStorageState.delete(key);
      },
      clear: () => {
        localStorageState.clear();
      },
    } satisfies Storage,
  });
}

export function installDialogMock(): void {
  if (typeof HTMLDialogElement === 'undefined') {
    return;
  }
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
    configurable: true,
    value() {
      this.setAttribute('open', '');
    },
  });
  Object.defineProperty(HTMLDialogElement.prototype, 'close', {
    configurable: true,
    value() {
      this.removeAttribute('open');
    },
  });
}

export function setupDomTestEnvironment(): void {
  installLocalStorageMock();
  installDialogMock();
  vi.restoreAllMocks();
  vi.clearAllMocks();
  window.localStorage.clear();
  document.body.innerHTML = '';
  vi.spyOn(window, 'fetch').mockResolvedValue({ ok: true } as Response);
  vi.spyOn(window, 'alert').mockImplementation(() => undefined);
}

export function cleanupDomTestEnvironment(): void {
  document.body.innerHTML = '';
}
