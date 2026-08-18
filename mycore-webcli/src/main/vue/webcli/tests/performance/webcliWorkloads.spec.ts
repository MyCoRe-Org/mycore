import type { Browser, Page } from '@playwright/test';
import { expect, test } from '@playwright/test';

type WorkloadMode = 'logs' | 'queue';

interface DomChurn {
  added: number;
  characterData: number;
  removed: number;
}

interface HeapUsage {
  usedSize: number;
}

interface WorkloadResult {
  count: number;
  domChurn: DomChurn;
  elapsedMs: number;
  heapGrowthBeforeGc: number;
  mode: WorkloadMode;
  peakHeapGrowth: number;
  retainedHeapGrowth: number;
}

async function installDomObservers(page: Page): Promise<void> {
  await page.evaluate(() => {
    const stats = {
      log: { added: 0, characterData: 0, removed: 0 },
      queue: { added: 0, characterData: 0, removed: 0 },
    };
    const observe = (selector: string, target: 'log' | 'queue') => {
      const element = document.querySelector(selector);
      if (!element) {
        throw new Error(`Unable to observe ${selector}`);
      }
      const observer = new MutationObserver(records => {
        for (const record of records) {
          stats[target].added += record.addedNodes.length;
          stats[target].removed += record.removedNodes.length;
          if (record.type === 'characterData') {
            stats[target].characterData++;
          }
        }
      });
      observer.observe(element, { characterData: true, childList: true, subtree: true });
    };

    observe('.web-cli-log', 'log');
    observe('.web-cli-pre', 'queue');
    (window as typeof window & { mcr3794DomStats: typeof stats }).mcr3794DomStats = stats;
  });
}

async function readHeapUsage(page: Page): Promise<number> {
  const session = await page.context().newCDPSession(page);
  const usage = await session.send('Runtime.getHeapUsage') as HeapUsage;
  await session.detach();
  return usage.usedSize;
}

async function collectGarbage(page: Page): Promise<void> {
  const session = await page.context().newCDPSession(page);
  await session.send('HeapProfiler.collectGarbage');
  await session.detach();
}

async function runWorkload(browser: Browser, mode: WorkloadMode, count: number): Promise<WorkloadResult> {
  const context = await browser.newContext();
  const page = await context.newPage();
  await page.goto('http://127.0.0.1:4175/');
  await page.locator('#webcli-command-input').waitFor();
  await installDomObservers(page);
  await collectGarbage(page);

  const heapBefore = await readHeapUsage(page);
  let peakHeap = heapBefore;
  const command = `stress ${mode} ${count}`;
  const started = Date.now();

  await page.locator('#webcli-command-input').fill(command);
  await page.locator('.webcli-execute-button').click();
  await expect(page.locator('#current-command-running')).toHaveText(command);

  while (await page.locator('#current-command-running').count() > 0) {
    peakHeap = Math.max(peakHeap, await readHeapUsage(page));
    await page.waitForTimeout(25);
  }
  await page.waitForTimeout(50);

  const elapsedMs = Date.now() - started;
  const heapAfterWorkload = await readHeapUsage(page);
  const domStats = await page.evaluate(() =>
    (window as typeof window & {
      mcr3794DomStats: Record<WorkloadMode, DomChurn>;
    }).mcr3794DomStats
  );
  await collectGarbage(page);
  const heapAfterGc = await readHeapUsage(page);
  await context.close();

  return {
    count,
    domChurn: domStats[mode === 'logs' ? 'log' : 'queue'],
    elapsedMs,
    heapGrowthBeforeGc: heapAfterWorkload - heapBefore,
    mode,
    peakHeapGrowth: peakHeap - heapBefore,
    retainedHeapGrowth: heapAfterGc - heapBefore,
  };
}

function totalDomChurn(result: WorkloadResult): number {
  return result.domChurn.added + result.domChurn.removed + result.domChurn.characterData;
}

test('updates bounded log history with linear DOM churn', async ({ browser }) => {
  const result = await runWorkload(browser, 'logs', 1_000);

  expect(totalDomChurn(result)).toBeLessThanOrEqual(4_000);
});

test('measures browser cost of log and command-queue workloads', async ({ browser }) => {
  const counts = [500, 1_000, 2_000, 10_000];
  const results: WorkloadResult[] = [];

  for (const count of counts) {
    results.push(await runWorkload(browser, 'queue', count));
    results.push(await runWorkload(browser, 'logs', count));
  }

  console.log(`[MCR-3794] ${JSON.stringify(results)}`);

  const largestQueue = results.find(result => result.mode === 'queue' && result.count === 10_000);
  const largestLog = results.find(result => result.mode === 'logs' && result.count === 10_000);
  expect(largestQueue).toBeDefined();
  expect(largestLog).toBeDefined();

  const queueChurn = totalDomChurn(largestQueue!);
  const logChurn = totalDomChurn(largestLog!);

  expect(queueChurn).toBeLessThanOrEqual(10_000 * 4);
  expect(logChurn).toBeLessThanOrEqual(10_000 * 4);
});
