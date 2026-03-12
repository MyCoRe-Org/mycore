import type { Page } from '@playwright/test';

export function installBrowserMocks(page: Page): Promise<void> {
  return page.addInitScript(() => {
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
}

export async function openCommandSuggestions(page: Page, value = 'xslt'): Promise<void> {
  await page.goto('/');
  await page.locator('#webcli-command-input').fill(value);
}

export function parseRgb(value: string): [number, number, number] {
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

export function calculateContrastRatio(foreground: [number, number, number], background: [number, number, number]): number {
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
