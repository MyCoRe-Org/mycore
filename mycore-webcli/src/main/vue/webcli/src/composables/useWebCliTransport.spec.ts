import { mount, type VueWrapper } from '@vue/test-utils';
import { defineComponent, nextTick } from 'vue';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { useWebCliTransport, type UseWebCliTransportRuntime, type WebCliTransportClient } from '@/composables/useWebCliTransport';
import type { TransportEvent } from '@/types';

type Handler = (event: TransportEvent) => void;

class MockTransport implements WebCliTransportClient {
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

interface HarnessOptions {
  continueIfOneFails?: () => boolean;
  logLimit?: () => number;
  runtime?: UseWebCliTransportRuntime;
}

function createHarness(options: HarnessOptions = {}) {
  return defineComponent({
    setup() {
      return useWebCliTransport(
        options.continueIfOneFails ?? (() => false),
        options.logLimit ?? (() => 2),
        options.runtime
      );
    },
    template: '<div />',
  });
}

describe('useWebCliTransport', () => {
  let wrapper: VueWrapper | null = null;

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    vi.restoreAllMocks();
  });

  it('connects and initializes the transport on mount', () => {
    const transport = new MockTransport();

    wrapper = mount(createHarness({
      continueIfOneFails: () => true,
      runtime: {
        createTransport: () => transport,
      },
    }));

    expect(transport.connect).toHaveBeenCalledTimes(1);
    expect(transport.getKnownCommands).toHaveBeenCalledTimes(1);
    expect(transport.startLog).toHaveBeenCalledTimes(1);
    expect(transport.setContinueIfOneFails).toHaveBeenCalledWith(true);
  });

  it('keeps only the configured number of log entries in memory', async () => {
    const transport = new MockTransport();
    wrapper = mount(createHarness({
      runtime: {
        createTransport: () => transport,
      },
    }));

    transport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'first',
        exception: null,
        time: 1,
      },
    });
    transport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'second',
        exception: null,
        time: 2,
      },
    });
    transport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'third',
        exception: null,
        time: 3,
      },
    });
    await nextTick();

    expect(((wrapper.vm as unknown) as { logs: { message: string }[] }).logs.map(entry => entry.message)).toEqual(['second', 'third']);
  });

  it('updates queue, current command, and permission state from transport events', async () => {
    const transport = new MockTransport();
    wrapper = mount(createHarness({
      runtime: {
        createTransport: () => transport,
      },
    }));

    transport.emit({
      type: 'queue',
      value: ['import object 1', 'import object 2'],
      size: 2,
    });
    transport.emit({
      type: 'currentCommand',
      value: 'import object 1',
    });
    transport.emit({
      type: 'noPermission',
    });
    await nextTick();

    const vm = (wrapper.vm as unknown) as {
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
    const transport = new MockTransport();
    wrapper = mount(createHarness({
      runtime: {
        createTransport: () => transport,
      },
    }));

    const vm = (wrapper.vm as unknown) as {
      liveStatus: string;
      refreshRunning: boolean;
      setRefresh: (value: boolean) => void;
    };

    vm.setRefresh(false);
    vm.setRefresh(true);
    await nextTick();

    expect(transport.stopLog).toHaveBeenCalledTimes(1);
    expect(transport.startLog).toHaveBeenCalledTimes(2);
    expect(vm.refreshRunning).toBe(true);
    expect(vm.liveStatus).toBe('Log refresh resumed.');
  });

  it('clears logs and forwards queue clearing and continue-on-fail updates', async () => {
    const transport = new MockTransport();
    wrapper = mount(createHarness({
      runtime: {
        createTransport: () => transport,
      },
    }));

    transport.emit({
      type: 'log',
      value: {
        logLevel: 'INFO',
        message: 'first',
        exception: null,
        time: 1,
      },
    });
    await nextTick();

    const vm = (wrapper.vm as unknown) as {
      clearCommandQueue: () => void;
      clearLogs: () => void;
      lastLogAnnouncement: string;
      logs: { message: string }[];
      remoteContinueIfOneFails: boolean | null;
      updateContinueIfOneFails: (value: boolean) => void;
    };

    transport.emit({
      type: 'continueIfOneFails',
      value: true,
    });
    vm.updateContinueIfOneFails(false);
    vm.clearCommandQueue();
    vm.clearLogs();
    await nextTick();

    expect(transport.setContinueIfOneFails).toHaveBeenLastCalledWith(false);
    expect(transport.clearCommandList).toHaveBeenCalledTimes(1);
    expect(vm.remoteContinueIfOneFails).toBe(true);
    expect(vm.logs).toEqual([]);
    expect(vm.lastLogAnnouncement).toBe('Logs cleared.');
  });

  it('uses injected keepalive dependencies and cleans the timer up on unmount', async () => {
    const transport = new MockTransport();
    const fetchImpl = vi.fn().mockResolvedValue({ ok: true });
    const setIntervalFn = vi.fn((_handler: () => void, _timeout: number) => 42);
    const clearIntervalFn = vi.fn();

    wrapper = mount(createHarness({
      runtime: {
        clearIntervalFn,
        createTransport: () => transport,
        fetchImpl,
        locationLike: {
          host: 'localhost:8080',
          pathname: '/myapp/modules/webcli/gui/index.html',
          protocol: 'https:',
        },
        setIntervalFn,
      },
    }));

    expect(setIntervalFn).toHaveBeenCalledTimes(1);

    const keepAlive = setIntervalFn.mock.calls[0]?.[0] as (() => void) | undefined;
    expect(typeof keepAlive).toBe('function');
    await keepAlive?.();

    expect(fetchImpl).toHaveBeenCalledWith('/myapp/echo/ping', { credentials: 'same-origin' });

    wrapper.unmount();
    wrapper = null;

    expect(clearIntervalFn).toHaveBeenCalledWith(42);
  });
});
