import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

function parseRgb(value: string): [number, number, number] {
  const match = value.match(/\d+/g);
  if (!match || match.length < 3) {
    throw new Error(`Unable to parse RGB value: ${value}`);
  }
  return [Number(match[0]), Number(match[1]), Number(match[2])];
}

function luminanceChannel(value: number): number {
  const normalized = value / 255;
  return normalized <= 0.03928 ? normalized / 12.92 : ((normalized + 0.055) / 1.055) ** 2.4;
}

function calculateContrastRatio(foreground: [number, number, number], background: [number, number, number]): number {
  const foregroundLuminance =
    0.2126 * luminanceChannel(foreground[0]) +
    0.7152 * luminanceChannel(foreground[1]) +
    0.0722 * luminanceChannel(foreground[2]);
  const backgroundLuminance =
    0.2126 * luminanceChannel(background[0]) +
    0.7152 * luminanceChannel(background[1]) +
    0.0722 * luminanceChannel(background[2]);

  const lighter = Math.max(foregroundLuminance, backgroundLuminance);
  const darker = Math.min(foregroundLuminance, backgroundLuminance);
  return (lighter + 0.05) / (darker + 0.05);
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    class MockWebSocket {
      static OPEN = 1;
      readyState = MockWebSocket.OPEN;
      onmessage = null;
      onopen = null;

      constructor() {
        queueMicrotask(() => {
          this.onopen?.();
        });
      }

      send(message) {
        const payload = JSON.parse(message);
        if (payload.type === 'getKnownCommands') {
          queueMicrotask(() => {
            this.onmessage?.({
              data: JSON.stringify({
                type: 'getKnownCommands',
                return: {
                  commands: [
                    {
                      name: 'Transformations',
                      commands: [
                        {
                          command: 'xslt transform {0}',
                          help: 'Run an XSLT transformation.',
                        },
                        {
                          command: 'import object',
                          help: 'Import a resource with optional xslt mapping.',
                        },
                      ],
                    },
                  ],
                },
              }),
            });
          });
        }
      }
    }

    window.WebSocket = MockWebSocket;
    window.fetch = () => Promise.resolve(new Response('', { status: 200 }));
  });
});

test('has no serious axe violations in the default WebCLI view', async ({ page }) => {
  await page.goto('/');

  const accessibilityScanResults = await new AxeBuilder({ page })
    .include('.webcli-app')
    .analyze();

  const seriousViolations = accessibilityScanResults.violations.filter(violation =>
    ['serious', 'critical'].includes(violation.impact ?? '')
  );

  expect(seriousViolations).toEqual([]);
});

test('has no serious axe violations with the command suggestion popup open', async ({ page }) => {
  await page.goto('/');

  await page.locator('#webcli-command-input').fill('xslt');

  const accessibilityScanResults = await new AxeBuilder({ page })
    .include('.webcli-app')
    .analyze();

  const seriousViolations = accessibilityScanResults.violations.filter(violation =>
    ['serious', 'critical'].includes(violation.impact ?? '')
  );

  expect(seriousViolations).toEqual([]);
});

test('keeps sufficient contrast for the highlighted command suggestion', async ({ page }) => {
  await page.goto('/');

  await page.locator('#webcli-command-input').fill('xslt');
  await page.locator('#webcli-command-input').press('ArrowDown');

  const suggestionColors = await page.locator('.webcli-suggestion.is-highlighted').evaluate(element => {
    const computedStyle = window.getComputedStyle(element);
    return {
      color: computedStyle.color,
      background: computedStyle.backgroundColor,
    };
  });

  const ratio = calculateContrastRatio(parseRgb(suggestionColors.color), parseRgb(suggestionColors.background));
  expect(ratio).toBeGreaterThanOrEqual(4.5);
});

test('keeps sufficient contrast for toolbar controls on the dark navbar', async ({ page }) => {
  await page.goto('/');

  const contrastRatio = await page.locator('.navbar .nav-link').first().evaluate(element => {
    const computedStyle = window.getComputedStyle(element);
    const background = window.getComputedStyle(element.closest('.navbar') ?? document.body).backgroundColor;
    return {
      color: computedStyle.color,
      background,
    };
  });

  const ratio = calculateContrastRatio(parseRgb(contrastRatio.color), parseRgb(contrastRatio.background));
  expect(ratio).toBeGreaterThanOrEqual(4.5);
});
