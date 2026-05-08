import { onBeforeUnmount, onMounted, ref } from 'vue';

import { buildPingUrl, WebCliTransport, type LocationLike } from '@/services/webcliTransport';
import type { CommandGroup, LogEntry, TransportEvent } from '@/types';

export interface WebCliTransportClient {
  clearCommandList(): void;
  connect(): void;
  getKnownCommands(): void;
  run(command: string): void;
  setContinueIfOneFails(value: boolean): void;
  startLog(): void;
  stopLog(): void;
  subscribe(listener: (event: TransportEvent) => void): () => void;
}

export interface UseWebCliTransportRuntime {
  clearIntervalFn?: (handle: number) => void;
  createTransport?: () => WebCliTransportClient;
  fetchImpl?: (input: string, init?: RequestInit) => Promise<unknown>;
  locationLike?: LocationLike;
  setIntervalFn?: (handler: () => void, timeout: number) => number;
}

export function useWebCliTransport(
  continueIfOneFails: () => boolean,
  logLimit: () => number,
  runtime: UseWebCliTransportRuntime = {}
) {
  const locationLike = runtime.locationLike ?? window.location;
  const fetchImpl = runtime.fetchImpl ?? ((input: string, init?: RequestInit) => window.fetch(input, init));
  const setIntervalFn = runtime.setIntervalFn ?? window.setInterval.bind(window);
  const clearIntervalFn = runtime.clearIntervalFn ?? window.clearInterval.bind(window);
  const transport = runtime.createTransport?.() ?? new WebCliTransport(locationLike);
  const commandGroups = ref<CommandGroup[]>([]);
  const currentCommand = ref('');
  const queue = ref<string[]>([]);
  const queueLength = ref(0);
  const logs = ref<LogEntry[]>([]);
  const refreshRunning = ref(true);
  const permissionError = ref('');
  const lastLogAnnouncement = ref('');
  const liveStatus = ref('WebCLI ready.');
  const remoteContinueIfOneFails = ref<boolean | null>(null);
  const keepAliveHandle = ref<number | null>(null);
  let unsubscribeTransport: (() => void) | null = null;

  function resolveLogLimit(): number {
    const limit = logLimit();
    if (!Number.isFinite(limit)) {
      return 1;
    }
    return Math.max(1, Math.trunc(limit));
  }

  function trimLogs(): void {
    const maxEntries = resolveLogLimit();
    if (logs.value.length > maxEntries) {
      logs.value = logs.value.slice(-maxEntries);
    }
  }

  function appendLog(entry: LogEntry): void {
    logs.value = [...logs.value, entry];
    trimLogs();
    lastLogAnnouncement.value = `${entry.logLevel}: ${entry.message}`;
  }

  function clearLogs(): void {
    logs.value = [];
    lastLogAnnouncement.value = 'Logs cleared.';
  }

  function clearCommandQueue(): void {
    transport.clearCommandList();
  }

  function setRefresh(value: boolean): void {
    refreshRunning.value = value;
    if (value) {
      transport.startLog();
      liveStatus.value = 'Log refresh resumed.';
    } else {
      transport.stopLog();
      liveStatus.value = 'Log refresh paused.';
    }
  }

  function runCommand(command: string): void {
    transport.run(command);
    liveStatus.value = `Executed command ${command}.`;
  }

  function updateContinueIfOneFails(value: boolean): void {
    transport.setContinueIfOneFails(value);
  }

  function keepAlive(): void {
    const pingUrl = buildPingUrl(locationLike);
    fetchImpl(pingUrl, { credentials: 'same-origin' }).catch(() => undefined);
  }

  onMounted(() => {
    unsubscribeTransport = transport.subscribe(event => {
      switch (event.type) {
        case 'commandList':
          commandGroups.value = event.value;
          break;
        case 'log':
          appendLog(event.value);
          break;
        case 'queue':
          queue.value = event.value;
          queueLength.value = event.size;
          break;
        case 'currentCommand':
          currentCommand.value = event.value;
          liveStatus.value = event.value ? `Currently running command ${event.value}.` : 'No command running currently.';
          break;
        case 'continueIfOneFails':
          remoteContinueIfOneFails.value = event.value;
          break;
        case 'noPermission':
          permissionError.value = "You don't have permission to use the MyCoRe WebCLI.";
          liveStatus.value = permissionError.value;
          break;
      }
    });
    transport.connect();
    transport.getKnownCommands();
    transport.startLog();
    transport.setContinueIfOneFails(continueIfOneFails());
    keepAliveHandle.value = setIntervalFn(keepAlive, 1740000);
  });

  onBeforeUnmount(() => {
    if (unsubscribeTransport) {
      unsubscribeTransport();
    }
    if (keepAliveHandle.value !== null) {
      clearIntervalFn(keepAliveHandle.value);
    }
  });

  return {
    clearCommandQueue,
    clearLogs,
    commandGroups,
    currentCommand,
    lastLogAnnouncement,
    liveStatus,
    logs,
    permissionError,
    queue,
    queueLength,
    refreshRunning,
    remoteContinueIfOneFails,
    runCommand,
    setRefresh,
    trimLogs,
    updateContinueIfOneFails,
  };
}
