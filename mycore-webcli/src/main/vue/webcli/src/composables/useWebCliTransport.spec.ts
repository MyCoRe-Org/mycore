import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';

import type { TransportEvent } from '@/types';

type Handler = (event: TransportEvent) => void;

class MockTransport {
  handlers: Handler[] = [];

  clearCommandList = vi.fn();

  connect = vi.fn();

  getKnownCommands = vi.fn();

  run = vi.fn();

  setContinueIfOneFails = vi.fn();

  startLog = vi.fn();

  stopLog = vi.fn();

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

import { useWebCliTransport } from '@/composables/useWebCliTransport';

const Harness = defineComponent({
  setup() {
    return useWebCliTransport(() => false, () => 2);
  },
  template: '<div />',
});

describe('useWebCliTransport', () => {
  let wrapper: VueWrapper | null = null;

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
  });

  it('keeps only the configured number of log entries in memory', async () => {
    wrapper = mount(Harness);

    currentTransport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'first',
        exception: null,
        time: 1,
      },
    });
    currentTransport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'second',
        exception: null,
        time: 2,
      },
    });
    currentTransport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'third',
        exception: null,
        time: 3,
      },
    });
    await nextTick();

    expect((wrapper.vm as { logs: { message: string }[] }).logs.map(entry => entry.message)).toEqual(['second', 'third']);
  });
});
