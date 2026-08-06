import { expect, test } from '@playwright/test';

test('stub streams a log workload over the Web CLI WebSocket protocol', async () => {
  const receivedMessages = await new Promise<Record<string, unknown>[]>((resolve, reject) => {
    const messages: Record<string, unknown>[] = [];
    const socket = new WebSocket('ws://127.0.0.1:4175/ws/mycore-webcli/socket');
    const timeout = setTimeout(() => reject(new Error('Timed out waiting for stub workload')), 2_000);

    socket.onerror = () => reject(new Error('Unable to connect to Web CLI stub'));
    socket.onopen = () => socket.send(JSON.stringify({ type: 'run', command: 'stress logs 2' }));
    socket.onmessage = event => {
      const message = JSON.parse(event.data) as Record<string, unknown>;
      messages.push(message);
      if (message.type === 'currentCommand' && message.return === '') {
        clearTimeout(timeout);
        socket.close();
        resolve(messages);
      }
    };
  });

  expect(receivedMessages).toEqual([
    { type: 'currentCommand', return: 'stress logs 2' },
    {
      type: 'log',
      return: {
        exception: null,
        logLevel: 'INFO',
        message: 'repair object 1',
        time: 1,
      },
    },
    {
      type: 'log',
      return: {
        exception: null,
        logLevel: 'INFO',
        message: 'repair object 2',
        time: 2,
      },
    },
    { type: 'currentCommand', return: '' },
  ]);
});
