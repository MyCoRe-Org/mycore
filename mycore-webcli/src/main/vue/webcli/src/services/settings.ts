import type { Settings } from '@/types';

const DEFAULT_SETTINGS: Settings = {
  historySize: 500,
  comHistorySize: 10,
  suggestionLimit: 10,
  autoscroll: true,
  continueIfOneFails: false,
};

function normalizeNumber(value: number | undefined, fallback: number, min: number): number {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    return fallback;
  }
  return Math.max(min, Math.trunc(value));
}

function readNumber(key: string, fallback: number, min = Number.NEGATIVE_INFINITY): number {
  const value = window.localStorage.getItem(key);
  if (!value) {
    window.localStorage.setItem(key, String(fallback));
    return fallback;
  }
  const parsed = Number.parseInt(value, 10);
  if (Number.isNaN(parsed)) {
    return fallback;
  }
  return Math.max(min, parsed);
}

function readBoolean(key: string, fallback: boolean): boolean {
  const value = window.localStorage.getItem(key);
  if (!value) {
    window.localStorage.setItem(key, String(fallback));
    return fallback;
  }
  return value === 'true';
}

export function loadSettings(): Settings {
  return normalizeSettings({
    historySize: readNumber('historySize', DEFAULT_SETTINGS.historySize, 1),
    comHistorySize: readNumber('comHistorySize', DEFAULT_SETTINGS.comHistorySize, 0),
    suggestionLimit: readNumber('suggestionLimit', DEFAULT_SETTINGS.suggestionLimit, 1),
    autoscroll: readBoolean('autoScroll', DEFAULT_SETTINGS.autoscroll),
    continueIfOneFails: readBoolean('continueIfOneFails', DEFAULT_SETTINGS.continueIfOneFails),
  });
}

export function persistSettings(settings: Settings): void {
  const normalized = normalizeSettings(settings);
  window.localStorage.setItem('historySize', String(normalized.historySize));
  window.localStorage.setItem('comHistorySize', String(normalized.comHistorySize));
  window.localStorage.setItem('suggestionLimit', String(normalized.suggestionLimit));
  window.localStorage.setItem('autoScroll', String(normalized.autoscroll));
  window.localStorage.setItem('continueIfOneFails', String(normalized.continueIfOneFails));
}

export function loadCommandHistory(): string[] {
  const value = window.localStorage.getItem('commandHistory');
  if (!value) {
    return [];
  }
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.filter((entry): entry is string => typeof entry === 'string') : [];
  } catch {
    return [];
  }
}

export function persistCommandHistory(history: string[]): void {
  window.localStorage.setItem('commandHistory', JSON.stringify(history));
}

export function normalizeSettings(settings: Partial<Settings>): Settings {
  return {
    historySize: normalizeNumber(settings.historySize, DEFAULT_SETTINGS.historySize, 1),
    comHistorySize: normalizeNumber(settings.comHistorySize, DEFAULT_SETTINGS.comHistorySize, 0),
    suggestionLimit: normalizeNumber(settings.suggestionLimit, DEFAULT_SETTINGS.suggestionLimit, 1),
    autoscroll: typeof settings.autoscroll === 'boolean' ? settings.autoscroll : DEFAULT_SETTINGS.autoscroll,
    continueIfOneFails:
      typeof settings.continueIfOneFails === 'boolean'
        ? settings.continueIfOneFails
        : DEFAULT_SETTINGS.continueIfOneFails,
  };
}
