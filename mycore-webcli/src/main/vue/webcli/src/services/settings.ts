import type { Settings } from '@/types';

const DEFAULT_SETTINGS: Settings = {
  historySize: 500,
  comHistorySize: 10,
  autoscroll: true,
  continueIfOneFails: false,
};

function readNumber(key: string, fallback: number): number {
  const value = window.localStorage.getItem(key);
  if (!value) {
    window.localStorage.setItem(key, String(fallback));
    return fallback;
  }
  const parsed = Number.parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
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
  return {
    historySize: readNumber('historySize', DEFAULT_SETTINGS.historySize),
    comHistorySize: readNumber('comHistorySize', DEFAULT_SETTINGS.comHistorySize),
    autoscroll: readBoolean('autoScroll', DEFAULT_SETTINGS.autoscroll),
    continueIfOneFails: readBoolean('continueIfOneFails', DEFAULT_SETTINGS.continueIfOneFails),
  };
}

export function persistSettings(settings: Settings): void {
  window.localStorage.setItem('historySize', String(settings.historySize));
  window.localStorage.setItem('comHistorySize', String(settings.comHistorySize));
  window.localStorage.setItem('autoScroll', String(settings.autoscroll));
  window.localStorage.setItem('continueIfOneFails', String(settings.continueIfOneFails));
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
