import { createReadStream, existsSync } from 'node:fs';
import { stat } from 'node:fs/promises';
import http from 'node:http';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const root = path.resolve(__dirname, '../../../../../../target/classes/META-INF/resources/modules/webcli/gui');
const port = 4174;

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

function resolvePath(urlPath) {
  const sanitizedPath = urlPath.split('?')[0];
  const relativePath = sanitizedPath === '/' ? 'index.html' : sanitizedPath.replace(/^\/+/, '');
  return path.join(root, relativePath);
}

const server = http.createServer(async (request, response) => {
  const requestPath = resolvePath(request.url ?? '/');
  const fallbackPath = path.join(root, 'index.html');
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

server.listen(port, '127.0.0.1');
