import { createReadStream, existsSync } from 'node:fs';
import { stat } from 'node:fs/promises';
import http from 'node:http';
import path from 'node:path';
import { setTimeout } from 'node:timers';
import { fileURLToPath, URL } from 'node:url';

import { WebSocketServer } from 'ws';

const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const staticRoot = path.resolve(
  currentDirectory,
  '../../mycore-webcli/target/classes/META-INF/resources/modules/webcli/gui',
);
const port = 4175;
const webSocketPath = '/ws/mycore-webcli/socket';

const contentTypes = new Map([
  ['.css', 'text/css; charset=utf-8'],
  ['.eot', 'application/vnd.ms-fontobject'],
  ['.html', 'text/html; charset=utf-8'],
  ['.js', 'text/javascript; charset=utf-8'],
  ['.svg', 'image/svg+xml'],
  ['.ttf', 'font/ttf'],
  ['.woff', 'font/woff'],
  ['.woff2', 'font/woff2'],
]);

function resolveStaticPath(urlPath) {
  const sanitizedPath = urlPath.split('?')[0];
  const relativePath = sanitizedPath === '/' ? 'index.html' : sanitizedPath.replace(/^\/+/, '');
  return path.join(staticRoot, relativePath);
}

const server = http.createServer(async (request, response) => {
  const requestPath = resolveStaticPath(request.url ?? '/');
  const fallbackPath = path.join(staticRoot, 'index.html');
  const selectedPath = existsSync(requestPath) ? requestPath : fallbackPath;

  try {
    const fileStats = await stat(selectedPath);
    if (!fileStats.isFile()) {
      response.writeHead(404);
      response.end();
      return;
    }
    response.writeHead(200, {
      'Content-Type': contentTypes.get(path.extname(selectedPath)) ?? 'application/octet-stream',
      'Cache-Control': 'no-store',
    });
    createReadStream(selectedPath).pipe(response);
  } catch {
    response.writeHead(500);
    response.end();
  }
});

const webSocketServer = new WebSocketServer({ noServer: true });

server.on('upgrade', (request, socket, head) => {
  if (new URL(request.url ?? '/', 'http://127.0.0.1').pathname !== webSocketPath) {
    socket.destroy();
    return;
  }
  webSocketServer.handleUpgrade(request, socket, head, webSocket => {
    webSocketServer.emit('connection', webSocket, request);
  });
});

function send(webSocket, message) {
  if (webSocket.readyState === webSocket.OPEN) {
    webSocket.send(JSON.stringify(message));
  }
}

function nextTurn() {
  return new Promise(resolve => setTimeout(resolve, 0));
}

async function streamLogs(webSocket, count) {
  for (let index = 1; index <= count; index++) {
    send(webSocket, {
      type: 'log',
      return: {
        exception: null,
        logLevel: 'INFO',
        message: `repair object ${index}`,
        time: index,
      },
    });
    await nextTurn();
  }
}

async function streamQueue(webSocket, count) {
  for (let firstCommand = 1; firstCommand <= count + 1; firstCommand++) {
    const remaining = count - firstCommand + 1;
    send(webSocket, {
      type: 'commandQueue',
      return: Array.from(
        { length: Math.min(Math.max(remaining, 0), 100) },
        (_, offset) => `repair metadata search of ID test_document_${firstCommand + offset}`,
      ),
      size: Math.max(remaining, 0),
    });
    await nextTurn();
  }
}

async function runWorkload(webSocket, command) {
  const match = /^stress (logs|queue) (\d+)$/.exec(command);
  if (!match) {
    return;
  }

  const [, mode, rawCount] = match;
  const count = Number(rawCount);
  send(webSocket, { type: 'currentCommand', return: command });
  if (mode === 'logs') {
    await streamLogs(webSocket, count);
  } else {
    await streamQueue(webSocket, count);
  }
  send(webSocket, { type: 'currentCommand', return: '' });
}

webSocketServer.on('connection', webSocket => {
  webSocket.on('message', data => {
    const message = JSON.parse(data.toString());
    switch (message.type) {
      case 'getKnownCommands':
        send(webSocket, {
          type: 'getKnownCommands',
          return: {
            commands: [{
              name: 'Stress',
              commands: [
                { command: 'stress logs {0}', help: 'Stream log messages.' },
                { command: 'stress queue {0}', help: 'Stream command queue updates.' },
              ],
            }],
          },
        });
        break;
      case 'continueIfOneFails':
        send(webSocket, { type: 'continueIfOneFails', value: Boolean(message.value) });
        break;
      case 'run':
        void runWorkload(webSocket, String(message.command ?? ''));
        break;
      default:
        break;
    }
  });
});

server.listen(port, '127.0.0.1');
