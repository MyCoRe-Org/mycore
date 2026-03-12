import { onBeforeUnmount, onMounted, ref } from 'vue';

import { buildPingUrl, WebCliTransport } from '@/services/webcliTransport';
import type { CommandGroup, LogEntry } from '@/types';

export function useWebCliTransport(continueIfOneFails: () => boolean) {
  const transport = new WebCliTransport();
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
    const pingUrl = buildPingUrl(window.location);
    window.fetch(pingUrl, { credentials: 'same-origin' }).catch(() => undefined);
  }

  onMounted(() => {
    unsubscribeTransport = transport.subscribe(event => {
      switch (event.type) {
        case 'commandList':
          commandGroups.value = event.value;
          break;
        case 'log':
          logs.value = [...logs.value, event.value];
          lastLogAnnouncement.value = `${event.value.logLevel}: ${event.value.message}`;
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
    keepAliveHandle.value = window.setInterval(keepAlive, 1740000);
  });

  onBeforeUnmount(() => {
    if (unsubscribeTransport) {
      unsubscribeTransport();
    }
    if (keepAliveHandle.value !== null) {
      window.clearInterval(keepAliveHandle.value);
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
    updateContinueIfOneFails,
  };
}
