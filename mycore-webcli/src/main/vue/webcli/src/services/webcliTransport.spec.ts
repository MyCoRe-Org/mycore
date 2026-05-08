import { describe, expect, it } from 'vitest';

import { buildPingUrl, buildWebSocketUrl, getContextPath, WebCliTransport } from '@/services/webcliTransport';

class FakeSocket {
  readyState = 1;

  sent: string[] = [];

  onmessage: ((event: { data: string }) => void) | null = null;

  onopen: (() => void) | null = null;

  send(data: string): void {
    this.sent.push(data);
  }
}

describe('webcli transport helpers', () => {
  it('derives the application context from the modules/webcli path', () => {
    expect(getContextPath('/myapp/modules/webcli/gui/index.html')).toBe('/myapp');
    expect(getContextPath('/modules/webcli/gui/index.html')).toBe('');
  });

  it('builds websocket and keepalive URLs from the current location', () => {
    const locationLike = {
      host: 'localhost:8080',
      pathname: '/myapp/modules/webcli/gui/index.html',
      protocol: 'https:',
    };

    expect(buildWebSocketUrl(locationLike)).toBe('wss://localhost:8080/myapp/ws/mycore-webcli/socket');
    expect(buildPingUrl(locationLike)).toBe('/myapp/echo/ping');
  });
});

describe('webcli transport protocol mapping', () => {
  it('sends protocol messages and routes incoming frames', () => {
    const socket = new FakeSocket();
    const seen: string[] = [];
    const transport = new WebCliTransport(
      {
        host: 'localhost',
        pathname: '/myapp/modules/webcli/gui/index.html',
        protocol: 'http:',
      },
      () => socket
    );

    transport.subscribe(event => {
      seen.push(event.type);
    });

    transport.connect();
    transport.getKnownCommands();
    transport.run('process resource demo.txt');

    expect(socket.sent).toEqual([
      JSON.stringify({ type: 'getKnownCommands' }),
      JSON.stringify({ type: 'run', command: 'process resource demo.txt' }),
    ]);

    socket.onmessage?.({
      data: JSON.stringify({
        type: 'commandQueue',
        return: ['cmd1'],
        size: 1,
      }),
    });
    socket.onmessage?.({ data: 'noPermission' });

    expect(seen).toEqual(['queue', 'noPermission']);
  });

  it('queues initial messages until the socket opens', () => {
    const socket = new FakeSocket();
    socket.readyState = 0;
    const transport = new WebCliTransport(
      {
        host: 'localhost',
        pathname: '/myapp/modules/webcli/gui/index.html',
        protocol: 'http:',
      },
      () => socket
    );

    transport.connect();
    transport.getKnownCommands();
    transport.startLog();

    expect(socket.sent).toEqual([]);

    socket.readyState = 1;
    socket.onopen?.();

    expect(socket.sent).toEqual([
      JSON.stringify({ type: 'getKnownCommands' }),
      JSON.stringify({ type: 'startLog' }),
    ]);
  });
});
