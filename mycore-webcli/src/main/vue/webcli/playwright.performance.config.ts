import { existsSync } from 'node:fs';
import { execSync } from 'node:child_process';
import path from 'node:path';

import { defineConfig } from '@playwright/test';

// see the note on the file extension in playwright.config.ts
const port = 4175;
const appDirectory = __dirname;
const artifactsRoot = path.resolve(appDirectory, '../../../../target/playwright-performance');

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
  testDir: path.join(appDirectory, 'tests/performance'),
  outputDir: path.join(artifactsRoot, 'test-results'),
  timeout: 120_000,
  reporter: [['list']],
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
    command: 'node ./tests/performance/webcli-stub-server.mjs',
    cwd: appDirectory,
    port,
    reuseExistingServer: false,
  },
});
