import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';

import { calculateContrastRatio, installBrowserMocks, openCommandSuggestions, parseRgb } from './helpers/webcliPage';

test.beforeEach(async ({ page }) => {
  await installBrowserMocks(page);
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
  await openCommandSuggestions(page);

  const accessibilityScanResults = await new AxeBuilder({ page })
    .include('.webcli-app')
    .analyze();

  const seriousViolations = accessibilityScanResults.violations.filter(violation =>
    ['serious', 'critical'].includes(violation.impact ?? '')
  );

  expect(seriousViolations).toEqual([]);
});

test('keeps sufficient contrast for the highlighted command suggestion', async ({ page }) => {
  await openCommandSuggestions(page);
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
