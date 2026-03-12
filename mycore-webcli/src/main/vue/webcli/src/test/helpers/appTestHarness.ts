import { mount, type VueWrapper } from '@vue/test-utils';
import { nextTick } from 'vue';
import { vi } from 'vitest';

import type { TransportEvent } from '@/types';

type Handler = (event: TransportEvent) => void;

export class MockTransport {
  handlers: Handler[] = [];

  getKnownCommands = vi.fn();

  run = vi.fn();

  startLog = vi.fn();

  stopLog = vi.fn();

  clearCommandList = vi.fn();

  setContinueIfOneFails = vi.fn();

  connect = vi.fn();

  subscribe(listener: Handler): () => void {
    this.handlers.push(listener);
    return () => {
      this.handlers = this.handlers.filter(entry => entry !== listener);
    };
  }

  emit(event: TransportEvent): void {
    this.handlers.forEach(handler => handler(event));
  }
}

let currentTransport: MockTransport;

vi.mock('@/services/webcliTransport', async () => {
  const actual = await vi.importActual<typeof import('@/services/webcliTransport')>('@/services/webcliTransport');
  const WebCliTransportMock = vi.fn(function MockedWebCliTransport(this: unknown) {
    currentTransport = new MockTransport();
    return currentTransport;
  });
  return {
    ...actual,
    WebCliTransport: WebCliTransportMock,
  };
});

import App from '@/App.vue';

export async function mountApp(): Promise<VueWrapper> {
  const wrapper = mount(App, {
    attachTo: document.body,
  });
  await nextTick();
  return wrapper;
}

export function getCurrentTransport(): MockTransport {
  return currentTransport;
}
