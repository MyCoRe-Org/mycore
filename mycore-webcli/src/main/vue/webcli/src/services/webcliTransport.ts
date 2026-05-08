import type { CommandGroup, LogEntry, TransportEvent } from '@/types';

export interface LocationLike {
  host: string;
  pathname: string;
  protocol: string;
}

export interface WebSocketLike {
  readyState: number;
  send(data: string): void;
  onmessage: ((event: MessageEvent<string>) => void) | null;
  onopen: ((event: Event) => void) | null;
}

export type WebSocketFactory = (url: string) => WebSocketLike;

export function getContextPath(pathname: string): string {
  const marker = '/modules/webcli/gui/';
  const index = pathname.indexOf(marker);
  if (index >= 0) {
    return pathname.slice(0, index);
  }
  return '';
}

export function buildWebSocketUrl(locationLike: LocationLike): string {
  const protocol = locationLike.protocol === 'https:' ? 'wss://' : 'ws://';
  const contextPath = getContextPath(locationLike.pathname);
  return `${protocol}${locationLike.host}${contextPath}/ws/mycore-webcli/socket`;
}

export function buildPingUrl(locationLike: LocationLike): string {
  return `${getContextPath(locationLike.pathname)}/echo/ping`;
}

export class WebCliTransport {
  private readonly listeners = new Set<(event: TransportEvent) => void>();

  private readonly factory: WebSocketFactory;

  private readonly locationLike: LocationLike;

  private socket: WebSocketLike | null = null;

  private pendingMessages: string[] = [];

  constructor(
    locationLike: LocationLike = window.location,
    factory: WebSocketFactory = url => new WebSocket(url)
  ) {
    this.locationLike = locationLike;
    this.factory = factory;
  }

  subscribe(listener: (event: TransportEvent) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  connect(): void {
    const socket = this.factory(buildWebSocketUrl(this.locationLike));
    socket.onopen = () => this.flushPendingMessages();
    socket.onmessage = event => this.handleMessage(event.data);
    this.socket = socket;
  }

  getKnownCommands(): void {
    this.send({ type: 'getKnownCommands' });
  }

  run(command: string): void {
    if (command.trim()) {
      this.send({ type: 'run', command });
    }
  }

  startLog(): void {
    this.send({ type: 'startLog' });
  }

  stopLog(): void {
    this.send({ type: 'stopLog' });
  }

  clearCommandList(): void {
    this.send({ type: 'clearCommandList' });
  }

  setContinueIfOneFails(value: boolean): void {
    this.send({ type: 'continueIfOneFails', value });
  }

  private send(message: Record<string, unknown>): void {
    const payload = JSON.stringify(message);
    if (!this.socket) {
      this.connect();
    }
    if (!this.socket) {
      return;
    }
    if (this.socket.readyState === 1) {
      this.socket.send(payload);
      return;
    }
    this.pendingMessages.push(payload);
    if (this.socket.readyState === 3) {
      this.connect();
    }
  }

  private flushPendingMessages(): void {
    if (!this.socket || this.socket.readyState !== 1 || this.pendingMessages.length === 0) {
      return;
    }
    const messages = [...this.pendingMessages];
    this.pendingMessages = [];
    messages.forEach(message => this.socket?.send(message));
  }

  private emit(event: TransportEvent): void {
    this.listeners.forEach(listener => listener(event));
  }

  private handleMessage(data: string): void {
    if (data === 'noPermission') {
      this.emit({ type: 'noPermission' });
      return;
    }
    const message = JSON.parse(data) as Record<string, unknown>;
    switch (message.type) {
      case 'log':
        this.emit({ type: 'log', value: message.return as LogEntry });
        break;
      case 'commandQueue':
        this.emit({
          type: 'queue',
          value: (message.return as string[]) ?? [],
          size: Number(message.size) || 0,
        });
        break;
      case 'currentCommand':
        this.emit({ type: 'currentCommand', value: String(message.return ?? '') });
        break;
      case 'getKnownCommands':
        this.emit({
          type: 'commandList',
          value: ((message.return as { commands?: CommandGroup[] })?.commands ?? []) as CommandGroup[],
        });
        break;
      case 'continueIfOneFails':
        if (typeof message.value === 'boolean') {
          this.emit({ type: 'continueIfOneFails', value: message.value });
        }
        break;
      default:
        break;
    }
  }
}
