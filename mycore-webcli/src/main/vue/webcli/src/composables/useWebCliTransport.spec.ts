import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick } from 'vue';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

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

  beforeEach(() => {
    vi.spyOn(window, 'fetch').mockResolvedValue({ ok: true } as Response);
  });

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    vi.restoreAllMocks();
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

  it('updates queue, current command, and permission state from transport events', async () => {
    wrapper = mount(Harness);

    currentTransport.emit({
      type: 'queue',
      value: ['import object 1', 'import object 2'],
      size: 2,
    });
    currentTransport.emit({
      type: 'currentCommand',
      value: 'import object 1',
    });
    currentTransport.emit({
      type: 'noPermission',
    });
    await nextTick();

    const vm = wrapper.vm as {
      currentCommand: string;
      liveStatus: string;
      permissionError: string;
      queue: string[];
      queueLength: number;
    };

    expect(vm.queue).toEqual(['import object 1', 'import object 2']);
    expect(vm.queueLength).toBe(2);
    expect(vm.currentCommand).toBe('import object 1');
    expect(vm.permissionError).toContain("don't have permission");
    expect(vm.liveStatus).toContain("don't have permission");
  });

  it('toggles refresh state and delegates to the transport', async () => {
    wrapper = mount(Harness);

    const vm = wrapper.vm as {
      liveStatus: string;
      refreshRunning: boolean;
      setRefresh: (value: boolean) => void;
    };

    vm.setRefresh(false);
    vm.setRefresh(true);
    await nextTick();

    expect(currentTransport.stopLog).toHaveBeenCalledTimes(1);
    expect(currentTransport.startLog).toHaveBeenCalledTimes(2);
    expect(vm.refreshRunning).toBe(true);
    expect(vm.liveStatus).toBe('Log refresh resumed.');
  });

  it('clears logs and forwards queue clearing and continue-on-fail updates', async () => {
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
    await nextTick();

    const vm = wrapper.vm as {
      clearCommandQueue: () => void;
      clearLogs: () => void;
      lastLogAnnouncement: string;
      logs: { message: string }[];
      remoteContinueIfOneFails: boolean | null;
      updateContinueIfOneFails: (value: boolean) => void;
    };

    currentTransport.emit({
      type: 'continueIfOneFails',
      value: true,
    });
    vm.updateContinueIfOneFails(false);
    vm.clearCommandQueue();
    vm.clearLogs();
    await nextTick();

    expect(currentTransport.setContinueIfOneFails).toHaveBeenLastCalledWith(false);
    expect(currentTransport.clearCommandList).toHaveBeenCalledTimes(1);
    expect(vm.remoteContinueIfOneFails).toBe(true);
    expect(vm.logs).toEqual([]);
    expect(vm.lastLogAnnouncement).toBe('Logs cleared.');
  });
});
