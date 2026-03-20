import { existsSync } from 'node:fs';
import { execSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { defineConfig } from '@playwright/test';

const port = 4174;
const currentDirectory = path.dirname(fileURLToPath(import.meta.url));
const artifactsRoot = path.resolve(currentDirectory, '../../../../target/playwright');

function detectChromiumBinary(): string | undefined {
  if (process.env.CHROME_BIN && existsSync(process.env.CHROME_BIN)) {
    return process.env.CHROME_BIN;
  }
  if (process.env.CHROMIUM_BIN && existsSync(process.env.CHROMIUM_BIN)) {
    return process.env.CHROMIUM_BIN;
  }

  const candidates = [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
    '/snap/bin/chromium',
  ];

  const firstExistingCandidate = candidates.find(candidate => existsSync(candidate));
  if (firstExistingCandidate) {
    return firstExistingCandidate;
  }

  try {
    const detectedBinary = execSync('which google-chrome || which chromium || which chromium-browser || which chrome', {
      stdio: ['ignore', 'pipe', 'ignore'],
      shell: '/bin/sh',
    }).toString().trim();
    return detectedBinary || undefined;
  } catch {
    return undefined;
  }
}

const chromiumExecutablePath = detectChromiumBinary();

export default defineConfig({
  testDir: './tests/a11y',
  outputDir: path.join(artifactsRoot, 'test-results'),
  timeout: 30_000,
  reporter: [['list'], ['html', { open: 'never', outputFolder: path.join(artifactsRoot, 'report') }]],
  use: {
    baseURL: `http://127.0.0.1:${port}`,
    browserName: 'chromium',
    headless: true,
    launchOptions: chromiumExecutablePath ? {
      executablePath: chromiumExecutablePath,
    } : {},
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'node ./tests/a11y/static-server.mjs',
    port,
    reuseExistingServer: !process.env.CI,
  },
});
