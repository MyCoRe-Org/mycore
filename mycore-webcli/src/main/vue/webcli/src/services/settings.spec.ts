import { beforeEach, describe, expect, it } from 'vitest';

import { loadSettings, normalizeSettings, persistSettings } from '@/services/settings';
import { setupDomTestEnvironment } from '@/test/helpers/domMocks';

describe('settings service', () => {
  beforeEach(() => {
    setupDomTestEnvironment();
  });

  it('clamps persisted command history size to zero', () => {
    window.localStorage.setItem('comHistorySize', '-1');

    const settings = loadSettings();

    expect(settings.comHistorySize).toBe(0);
  });

  it('clamps persisted suggestion limit to at least one', () => {
    window.localStorage.setItem('suggestionLimit', '0');

    const settings = loadSettings();

    expect(settings.suggestionLimit).toBe(1);
  });

  it('clamps persisted log history size to at least one', () => {
    window.localStorage.setItem('historySize', '-1');

    const settings = loadSettings();

    expect(settings.historySize).toBe(1);
  });

  it('normalizes invalid in-memory settings before persisting them', () => {
    persistSettings({
      historySize: Number.NaN,
      comHistorySize: Number.NaN,
      autoscroll: true,
      continueIfOneFails: false,
    });

    expect(window.localStorage.getItem('historySize')).toBe('500');
    expect(window.localStorage.getItem('comHistorySize')).toBe('10');
    expect(window.localStorage.getItem('suggestionLimit')).toBe('10');
  });

  it('normalizes partial settings objects with sane defaults', () => {
    expect(
      normalizeSettings({
        historySize: -2,
        comHistorySize: -1,
      })
    ).toEqual({
      historySize: 1,
      comHistorySize: 0,
      suggestionLimit: 10,
      autoscroll: true,
      continueIfOneFails: false,
    });
  });
});
