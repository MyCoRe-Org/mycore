import { vi } from 'vitest';

export class WebSocketMock {
  static readonly CONNECTING = 0;

  static readonly OPEN = 1;

  static readonly CLOSING = 2;

  static readonly CLOSED = 3;

  static instances: WebSocketMock[] = [];

  readyState: number = WebSocketMock.CONNECTING;

  onopen: ((event: Event) => void) | null = null;

  onmessage: ((event: MessageEvent) => void) | null = null;

  onerror: ((event: Event) => void) | null = null;

  onclose: ((event: CloseEvent) => void) | null = null;

  readonly send = vi.fn<(data: string) => void>();

  constructor(readonly url: string) {
    WebSocketMock.instances.push(this);
  }

  static latest(): WebSocketMock {
    const socket = WebSocketMock.instances.at(-1);
    if (socket === undefined) {
      throw new Error('no websocket was opened');
    }
    return socket;
  }

  static install(): void {
    WebSocketMock.instances = [];
    vi.stubGlobal('WebSocket', WebSocketMock);
  }

  open(): void {
    this.readyState = WebSocketMock.OPEN;
    this.onopen?.(new Event('open'));
  }

  receive(message: object): void {
    this.onmessage?.(new MessageEvent('message', { data: JSON.stringify(message) }));
  }

  fail(): void {
    this.onerror?.(new Event('error'));
  }

  close(): void {
    this.readyState = WebSocketMock.CLOSED;
    this.onclose?.(new CloseEvent('close'));
  }

  sentMessages(): unknown[] {
    return this.send.mock.calls.map(([data]) => JSON.parse(data));
  }
}
