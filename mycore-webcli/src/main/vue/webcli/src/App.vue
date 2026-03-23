<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';

import CommandHistoryDialog from '@/components/CommandHistoryDialog.vue';
import CommandMenu from '@/components/CommandMenu.vue';
import CommandSuggestions from '@/components/CommandSuggestions.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import { useCommandHistory } from '@/composables/useCommandHistory';
import { useCommandSearch } from '@/composables/useCommandSearch';
import { useWebCliTransport } from '@/composables/useWebCliTransport';
import { loadSettings, normalizeSettings, persistSettings } from '@/services/settings';

const settings = ref(loadSettings());
const command = ref('');
const activeTab = ref<'log' | 'queue'>('log');
const isSettingsDialogOpen = ref(false);
const isCommandHistoryOpen = ref(false);
const isSuggestionMenuVisible = ref(false);
const isSuggestionNavigationActive = ref(false);
const suppressSuggestionMenuOnFocus = ref(false);
const isQueueExpanded = ref(false);
const inputElement = ref<HTMLInputElement | null>(null);
const executeButtonElement = ref<HTMLButtonElement | null>(null);
const logElement = ref<HTMLElement | null>(null);
const suggestionListId = 'webcli-command-suggestions';
const queuePreviewSize = 99;

const {
  addEntry,
  browseDown,
  browseUp,
  clear: clearCommandHistory,
  entries: commandHistoryEntries,
  finalizeExecution,
  trimToSize,
} = useCommandHistory(() => Math.max(0, settings.value.comHistorySize));

const {
  clearCommandQueue,
  clearLogs,
  commandGroups,
  currentCommand,
  lastLogAnnouncement,
  liveStatus,
  logs,
  permissionError,
  queue,
  queueLength,
  refreshRunning,
  remoteContinueIfOneFails,
  runCommand,
  setRefresh,
  trimLogs,
  updateContinueIfOneFails,
} = useWebCliTransport(
  () => settings.value.continueIfOneFails,
  () => settings.value.historySize
);

const {
  hasSuggestions,
  highlightedIndex,
  highlightedSuggestion,
  highlightSuggestion,
  moveHighlight,
  resetHighlight,
  totalSuggestionCount,
  suggestions,
} = useCommandSearch(commandGroups, command, computed(() => settings.value.suggestionLimit));

const visibleQueue = computed(() => {
  return isQueueExpanded.value ? queue.value : queue.value.slice(0, queuePreviewSize);
});
const hiddenQueueCount = computed(() => Math.max(queueLength.value - visibleQueue.value.length, 0));
const visibleLogLines = computed(() => {
  return logs.value.slice(-settings.value.historySize).flatMap(entry => {
    const lines = [`${entry.logLevel}: ${entry.message}`];
    if (entry.exception) {
      lines.push(entry.exception);
    }
    return lines;
  });
});

function areSettingsEqual(left: typeof settings.value, right: typeof settings.value): boolean {
  return (
    left.historySize === right.historySize &&
    left.comHistorySize === right.comHistorySize &&
    left.suggestionLimit === right.suggestionLimit &&
    left.autoscroll === right.autoscroll &&
    left.continueIfOneFails === right.continueIfOneFails
  );
}

function closeSuggestionMenu(restoreFocus = false): void {
  isSuggestionMenuVisible.value = false;
  isSuggestionNavigationActive.value = false;
  resetHighlight();
  if (restoreFocus) {
    suppressSuggestionMenuOnFocus.value = true;
    nextTick(() => {
      if (inputElement.value && document.activeElement !== inputElement.value) {
        inputElement.value.focus();
      }
    });
  }
}

function activateSuggestionNavigation(index = 0): void {
  if (!hasSuggestions.value) {
    return;
  }
  isSuggestionMenuVisible.value = true;
  isSuggestionNavigationActive.value = true;
  highlightSuggestion(index);
}

function selectNextPlaceholder(startFrom = 0): boolean {
  const input = inputElement.value;
  if (!input || !command.value) {
    return false;
  }
  const regex = /\{[0-9]+\}/g;
  regex.lastIndex = startFrom;
  const match = regex.exec(command.value);
  if (match) {
    input.focus();
    input.setSelectionRange(match.index, match.index + match[0].length);
    return true;
  }
  return false;
}

function executeCommand(): void {
  const value = command.value.trim();
  if (!value) {
    return;
  }
  finalizeExecution();
  runCommand(value);
  addEntry(value);
  command.value = '';
  closeSuggestionMenu();
}

function onCommandKeydown(event: KeyboardEvent): void {
  if (event.key === 'ArrowDown' && isSuggestionMenuVisible.value && hasSuggestions.value) {
    event.preventDefault();
    if (!isSuggestionNavigationActive.value) {
      activateSuggestionNavigation(Math.min(1, suggestions.value.length - 1));
      return;
    }
    moveHighlight(1);
    return;
  }
  if (event.key === 'ArrowUp' && isSuggestionMenuVisible.value && hasSuggestions.value) {
    event.preventDefault();
    if (!isSuggestionNavigationActive.value) {
      activateSuggestionNavigation(suggestions.value.length - 1);
      return;
    }
    moveHighlight(-1);
    return;
  }
  if (event.key === 'Enter') {
    if (isSuggestionMenuVisible.value && isSuggestionNavigationActive.value && highlightedSuggestion.value) {
      event.preventDefault();
      selectCommand(highlightedSuggestion.value.command);
      return;
    }
    event.preventDefault();
    executeCommand();
    return;
  }
  if (event.key === 'Escape' && isSuggestionMenuVisible.value) {
    event.preventDefault();
    closeSuggestionMenu(true);
    return;
  }
  if (event.key === 'ArrowUp') {
    closeSuggestionMenu();
    const previousCommand = browseUp(command.value);
    if (previousCommand !== null) {
      command.value = previousCommand;
    }
    return;
  }
  if (event.key === 'ArrowDown') {
    closeSuggestionMenu();
    const nextCommand = browseDown();
    if (nextCommand !== null) {
      command.value = nextCommand;
    }
    return;
  }
  if (event.key === 'Tab' && !event.shiftKey) {
    if (
      isSuggestionMenuVisible.value &&
      highlightedSuggestion.value &&
      command.value.trim() !== highlightedSuggestion.value.command
    ) {
      event.preventDefault();
      selectCommand(highlightedSuggestion.value.command);
      return;
    }
    const movedToPlaceholder = selectNextPlaceholder(inputElement.value?.selectionEnd ?? 0);
    if (!movedToPlaceholder) {
      return;
    }
    event.preventDefault();
  }
}

function selectCommand(value: string): void {
  command.value = value;
  closeSuggestionMenu();
  nextTick(() => {
    const focusedPlaceholder = selectNextPlaceholder(0);
    if (!focusedPlaceholder) {
      executeButtonElement.value?.focus();
    }
  });
}

function onCommandInput(): void {
  isSuggestionMenuVisible.value = hasSuggestions.value;
  isSuggestionNavigationActive.value = false;
  resetHighlight();
}

function onCommandInputFocus(): void {
  if (suppressSuggestionMenuOnFocus.value) {
    suppressSuggestionMenuOnFocus.value = false;
    return;
  }
  if (hasSuggestions.value) {
    isSuggestionMenuVisible.value = true;
  }
}

function onCommandInputBlur(): void {
  window.setTimeout(() => {
    closeSuggestionMenu();
  }, 100);
}

function openCommandHistory(): void {
  isCommandHistoryOpen.value = true;
}

function openSettingsDialog(): void {
  isSettingsDialogOpen.value = true;
}

function toggleQueueExpansion(): void {
  isQueueExpanded.value = !isQueueExpanded.value;
}

function onSuggestionActivate(index: number): void {
  activateSuggestionNavigation(index);
}

function restoreCommandFromHistory(value: string): void {
  command.value = value;
  isCommandHistoryOpen.value = false;
  nextTick(() => inputElement.value?.focus());
}

watch(settings, value => {
  const normalized = normalizeSettings(value);
  if (!areSettingsEqual(value, normalized)) {
    settings.value = normalized;
    return;
  }
  persistSettings(value);
}, { deep: true });

watch(() => settings.value.continueIfOneFails, value => {
  updateContinueIfOneFails(value);
});

watch(() => settings.value.historySize, () => {
  trimLogs();
});

watch(() => settings.value.comHistorySize, () => {
  trimToSize();
});

watch(remoteContinueIfOneFails, value => {
  if (typeof value === 'boolean') {
    settings.value.continueIfOneFails = value;
  }
});

watch(visibleLogLines, async () => {
  if (!settings.value.autoscroll) {
    return;
  }
  await nextTick();
  const element = logElement.value;
  if (element) {
    element.scrollTop = element.scrollHeight;
  }
});

watch(queueLength, value => {
  if (value < 1) {
    activeTab.value = 'log';
    isQueueExpanded.value = false;
  }
});

watch(hasSuggestions, value => {
  if (!value) {
    closeSuggestionMenu();
  }
});
</script>

<template>
  <div class="webcli-app">
    <div class="visually-hidden" aria-live="polite" aria-atomic="true">{{ liveStatus }}</div>
    <div class="visually-hidden" aria-live="polite" aria-atomic="true">{{ lastLogAnnouncement }}</div>

    <div v-if="permissionError" class="alert alert-danger mb-0 rounded-0" role="alert">
      {{ permissionError }}
    </div>

    <div id="web-cli-header">
      <nav class="navbar navbar-expand navbar-dark bg-dark">
        <ul class="nav navbar-nav flex-row flex-wrap align-items-center">
          <CommandMenu :groups="commandGroups" @select="selectCommand" />
          <li class="nav-item">
            <button type="button" class="nav-link btn btn-link" @click="clearLogs">
              <i class="fa fa-eraser" aria-hidden="true"></i> Clear Logs
            </button>
          </li>
          <li v-if="!refreshRunning" class="nav-item">
            <button type="button" class="nav-link btn btn-link" @click="setRefresh(true)">
              <i class="fa fa-play" aria-hidden="true"></i> Refresh
            </button>
          </li>
          <li v-else class="nav-item">
            <button type="button" class="nav-link btn btn-link with-spinner" @click="setRefresh(false)">
              <i class="fa fa-pause" aria-hidden="true"></i> Stop Refresh
            </button>
          </li>
          <li class="nav-item">
            <button type="button" class="nav-link btn btn-link" @click="clearCommandQueue">
              <i class="fa fa-stop" aria-hidden="true"></i> Clear Command Queue
            </button>
          </li>
          <li v-if="commandHistoryEntries.length > 0" class="nav-item">
            <button type="button" class="nav-link btn btn-link" @click="openCommandHistory">
              <i class="fa fa-archive" aria-hidden="true"></i> Command History
            </button>
          </li>
          <li class="nav-item">
            <button
              type="button"
              class="nav-link btn btn-link"
              aria-haspopup="dialog"
              :aria-expanded="isSettingsDialogOpen"
              @click="openSettingsDialog"
            >
              <i class="fa fa-cog" aria-hidden="true"></i> Settings
            </button>
          </li>
        </ul>
      </nav>
    </div>

    <div id="command-input" class="row">
      <div class="col-12">
        <div class="input-group webcli-command-combobox">
          <label for="webcli-command-input" class="visually-hidden">Command input</label>
          <input
            id="webcli-command-input"
            ref="inputElement"
            v-model="command"
            type="text"
            class="form-control"
            placeholder="Command..."
            role="combobox"
            autocomplete="off"
            aria-describedby="webcli-command-help"
            :aria-expanded="isSuggestionMenuVisible && hasSuggestions"
            :aria-controls="suggestionListId"
            :aria-activedescendant="isSuggestionMenuVisible && highlightedSuggestion ? `${suggestionListId}-option-${highlightedIndex}` : undefined"
            @keydown="onCommandKeydown"
            @input="onCommandInput"
            @focus="onCommandInputFocus"
            @blur="onCommandInputBlur"
          />
          <button
            ref="executeButtonElement"
            class="btn btn-secondary webcli-execute-button"
            type="button"
            @click="executeCommand"
          >
            Execute
          </button>
        </div>
        <CommandSuggestions
          v-if="isSuggestionMenuVisible && hasSuggestions"
          :id="suggestionListId"
          :highlighted-index="highlightedIndex"
          :suggestions="suggestions"
          :total-count="totalSuggestionCount"
          :suggestion-limit="settings.suggestionLimit"
          @activate="onSuggestionActivate"
          @select="selectCommand"
        />
        <div id="webcli-command-help" class="visually-hidden">
          Enter executes the current command. Press Tab to accept a suggestion or jump between placeholders. When the
          suggestion list is open, use Arrow keys to navigate it and Escape to close it.
        </div>
      </div>
    </div>

    <section id="current-command" class="row" aria-live="polite" aria-atomic="true">
      <div v-if="currentCommand">
        <div class="col-lg-3 col-md-4 col-sm-5 current-command-label">
          Currently running command:
        </div>
        <div class="col-lg-9 col-md-8 col-sm-7" id="current-command-running" :title="currentCommand">
          {{ currentCommand }}
        </div>
        <div class="col-12">
          <div class="loader" aria-hidden="true"></div>
        </div>
      </div>
      <div v-else>
        <div class="col-12 current-command-label">
          No command running currently.
        </div>
        <div class="col-12">
          <div class="loader-no-animation" aria-hidden="true"></div>
        </div>
      </div>
    </section>

    <ul class="nav nav-tabs" role="tablist">
      <li class="nav-item" role="presentation">
        <button
          id="webcli-log-tab"
          type="button"
          class="nav-link logTab"
          :class="{ active: activeTab === 'log' }"
          role="tab"
          :aria-selected="activeTab === 'log'"
          aria-controls="webcli-log-panel"
          @click="activeTab = 'log'"
        >
          Log
        </button>
      </li>
      <li v-if="queueLength > 0" class="nav-item" role="presentation">
        <button
          id="webcli-queue-tab"
          type="button"
          class="nav-link queueTab"
          :class="{ active: activeTab === 'queue' }"
          role="tab"
          :aria-selected="activeTab === 'queue'"
          aria-controls="webcli-queue-panel"
          @click="activeTab = 'queue'"
        >
          Command Queue ({{ queueLength }})
        </button>
      </li>
    </ul>

    <div class="tab-content">
      <div
        v-show="activeTab === 'log'"
        id="webcli-log-panel"
        class="row tab-pane fade show active"
        role="tabpanel"
        aria-labelledby="webcli-log-tab"
      >
        <div ref="logElement" class="col-12 web-cli-log" role="log" aria-live="polite" aria-relevant="additions text">
          <pre v-for="(line, index) in visibleLogLines" :key="`${index}-${line}`">{{ line }}</pre>
        </div>
      </div>
      <div
        v-show="activeTab === 'queue'"
        id="webcli-queue-panel"
        class="row tab-pane fade show active"
        role="tabpanel"
        aria-labelledby="webcli-queue-tab"
      >
        <div class="col-12 web-cli-queue">
          <div v-if="queueLength > queuePreviewSize" class="webcli-queue-toolbar">
            <button type="button" class="btn btn-link px-0" @click="toggleQueueExpansion">
              {{ isQueueExpanded ? `Show first ${queuePreviewSize} commands` : `Show all ${queueLength} commands` }}
            </button>
          </div>
          <pre class="web-cli-pre">{{ visibleQueue.join('\n') }}</pre>
          <div v-if="!isQueueExpanded && hiddenQueueCount > 0" class="small text-secondary">
            {{ hiddenQueueCount }} more commands hidden.
          </div>
        </div>
      </div>
    </div>

    <CommandHistoryDialog
      v-model="isCommandHistoryOpen"
      :entries="commandHistoryEntries"
      @select-command="restoreCommandFromHistory"
    />
    <SettingsDialog
      v-model="isSettingsDialogOpen"
      :settings="settings"
      :has-command-history="commandHistoryEntries.length > 0"
      @clear-command-history="clearCommandHistory"
    />
  </div>
</template>
