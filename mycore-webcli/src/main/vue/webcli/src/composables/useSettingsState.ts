import { ref, watch } from 'vue';

import { clearPersistedSettings, getDefaultSettings, loadSettings, normalizeSettings, persistSettings } from '@/services/settings';
import type { Settings } from '@/types';

function areSettingsEqual(left: Settings, right: Settings): boolean {
  return (
    left.historySize === right.historySize &&
    left.comHistorySize === right.comHistorySize &&
    left.suggestionLimit === right.suggestionLimit &&
    left.autoscroll === right.autoscroll &&
    left.continueIfOneFails === right.continueIfOneFails
  );
}

export function useSettingsState() {
  const settings = ref(loadSettings());
  let skipNextPersist = false;

  watch(settings, value => {
    const normalized = normalizeSettings(value);
    if (!areSettingsEqual(value, normalized)) {
      settings.value = normalized;
      return;
    }
    if (skipNextPersist) {
      skipNextPersist = false;
      return;
    }
    persistSettings(value);
  }, { deep: true });

  function resetSettings(): void {
    clearPersistedSettings();
    skipNextPersist = true;
    settings.value = getDefaultSettings();
  }

  return {
    resetSettings,
    settings,
  };
}
