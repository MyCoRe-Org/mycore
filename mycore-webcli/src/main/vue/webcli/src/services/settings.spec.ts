import { beforeEach, describe, expect, it } from 'vitest';

import { loadSettings } from '@/services/settings';
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

  it('clamps persisted log history size to at least one', () => {
    window.localStorage.setItem('historySize', '-1');

    const settings = loadSettings();

    expect(settings.historySize).toBe(1);
  });
});
