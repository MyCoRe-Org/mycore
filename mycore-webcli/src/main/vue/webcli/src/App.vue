<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';

import CommandHistoryDialog from '@/components/CommandHistoryDialog.vue';
import CommandMenu from '@/components/CommandMenu.vue';
import CommandSuggestions from '@/components/CommandSuggestions.vue';
import { useCommandHistory } from '@/composables/useCommandHistory';
import { useCommandSearch } from '@/composables/useCommandSearch';
import { useWebCliTransport } from '@/composables/useWebCliTransport';
import { loadSettings, persistSettings } from '@/services/settings';

const settings = ref(loadSettings());
const command = ref('');
const activeTab = ref<'log' | 'queue'>('log');
const isSettingsVisible = ref(false);
const isCommandHistoryOpen = ref(false);
const isSuggestionMenuVisible = ref(false);
const inputElement = ref<HTMLInputElement | null>(null);
const executeButtonElement = ref<HTMLButtonElement | null>(null);
const logElement = ref<HTMLElement | null>(null);
const suggestionListId = 'webcli-command-suggestions';

const {
  addEntry,
  browseDown,
  browseUp,
  clear: clearCommandHistory,
  entries: commandHistoryEntries,
  finalizeExecution,
  trimToSize,
} = useCommandHistory(() => settings.value.comHistorySize);

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
  updateContinueIfOneFails,
} = useWebCliTransport(() => settings.value.continueIfOneFails);

const {
  hasSuggestions,
  highlightedIndex,
  highlightedSuggestion,
  highlightSuggestion,
  moveHighlight,
  resetHighlight,
  suggestions,
} = useCommandSearch(commandGroups, command);

const visibleQueue = computed(() => queue.value.slice(0, 99));
const visibleLogLines = computed(() => {
  return logs.value.slice(-settings.value.historySize).flatMap(entry => {
    const lines = [`${entry.logLevel}: ${entry.message}`];
    if (entry.exception) {
      lines.push(entry.exception);
    }
    return lines;
  });
});

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
  isSuggestionMenuVisible.value = false;
  resetHighlight();
}

function onCommandKeydown(event: KeyboardEvent): void {
  if (event.key === 'ArrowDown' && hasSuggestions.value) {
    event.preventDefault();
    isSuggestionMenuVisible.value = true;
    moveHighlight(1);
    return;
  }
  if (event.key === 'ArrowUp' && hasSuggestions.value && isSuggestionMenuVisible.value) {
    event.preventDefault();
    moveHighlight(-1);
    return;
  }
  if (event.key === 'Enter') {
    if (isSuggestionMenuVisible.value && highlightedSuggestion.value) {
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
    isSuggestionMenuVisible.value = false;
    return;
  }
  if (event.key === 'ArrowUp') {
    const previousCommand = browseUp(command.value);
    if (previousCommand !== null) {
      command.value = previousCommand;
    }
    return;
  }
  if (event.key === 'ArrowDown') {
    const nextCommand = browseDown();
    if (nextCommand !== null) {
      command.value = nextCommand;
    }
    return;
  }
  if (event.key === 'Tab' && !event.shiftKey) {
    if (isSuggestionMenuVisible.value && highlightedSuggestion.value) {
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
  isSuggestionMenuVisible.value = false;
  resetHighlight();
  nextTick(() => {
    const focusedPlaceholder = selectNextPlaceholder(0);
    if (!focusedPlaceholder) {
      executeButtonElement.value?.focus();
    }
  });
}

function onCommandInput(): void {
  isSuggestionMenuVisible.value = hasSuggestions.value;
  resetHighlight();
}

function onCommandInputFocus(): void {
  if (hasSuggestions.value) {
    isSuggestionMenuVisible.value = true;
  }
}

function onCommandInputBlur(): void {
  window.setTimeout(() => {
    isSuggestionMenuVisible.value = false;
  }, 100);
}

function openCommandHistory(): void {
  isCommandHistoryOpen.value = true;
}

function restoreCommandFromHistory(value: string): void {
  command.value = value;
  isCommandHistoryOpen.value = false;
  nextTick(() => inputElement.value?.focus());
}

watch(settings, value => {
  persistSettings(value);
}, { deep: true });

watch(() => settings.value.continueIfOneFails, value => {
  updateContinueIfOneFails(value);
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
  }
});

watch(hasSuggestions, value => {
  if (!value) {
    isSuggestionMenuVisible.value = false;
    resetHighlight();
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
      <div id="webcli-settings-panel" class="collapse" :class="{ show: isSettingsVisible }">
        <div class="webcli-settings-panel bg-dark p-3">
          <div class="mb-3 row">
            <label for="webcli-history-size" class="col-sm-3 col-form-label">Log History Size:</label>
            <div class="col-sm-2">
              <input
                id="webcli-history-size"
                v-model.number="settings.historySize"
                type="number"
                min="1"
                inputmode="numeric"
                class="form-control"
              />
            </div>
          </div>
          <div class="mb-3 row">
            <label for="webcli-command-history-size" class="col-sm-3 col-form-label">Command History Size:</label>
            <div class="col-sm-2">
              <input
                id="webcli-command-history-size"
                v-model.number="settings.comHistorySize"
                type="number"
                min="1"
                inputmode="numeric"
                class="form-control"
              />
            </div>
            <div class="col-sm-auto d-flex align-items-center">
              <button
                type="button"
                class="btn btn-link comHistoryDeleteButton"
                aria-label="Delete command history"
                @click="clearCommandHistory"
              >
                <i class="fa fa-eraser" aria-hidden="true"></i>
              </button>
            </div>
          </div>
          <div class="mb-3 row">
            <div class="col-sm-3 col-form-label">AutoScroll Logs:</div>
            <div class="col-sm-2 d-flex align-items-center">
              <div class="form-check mb-0">
                <input id="webcli-autoscroll" v-model="settings.autoscroll" type="checkbox" class="form-check-input" />
                <label for="webcli-autoscroll" class="form-check-label">Enable automatic scrolling</label>
              </div>
            </div>
          </div>
          <div class="row">
            <div class="col-sm-3 col-form-label">Continue if one fails:</div>
            <div class="col-sm-2 d-flex align-items-center">
              <div class="form-check mb-0">
                <input
                  id="webcli-continue-on-fail"
                  v-model="settings.continueIfOneFails"
                  type="checkbox"
                  class="form-check-input"
                />
                <label for="webcli-continue-on-fail" class="form-check-label">Continue queue execution</label>
              </div>
            </div>
          </div>
        </div>
      </div>

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
              aria-controls="webcli-settings-panel"
              :aria-expanded="isSettingsVisible"
              @click="isSettingsVisible = !isSettingsVisible"
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
          @highlight="highlightSuggestion"
          @select="selectCommand"
        />
        <div id="webcli-command-help" class="visually-hidden">
          Enter a command, press Tab to jump between placeholders, and press Enter to execute.
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
          <pre class="web-cli-pre">{{ visibleQueue.join('\n') }}<span v-if="queue.length > 99">
...
</span></pre>
        </div>
      </div>
    </div>

    <CommandHistoryDialog
      v-model="isCommandHistoryOpen"
      :entries="commandHistoryEntries"
      @select-command="restoreCommandFromHistory"
    />
  </div>
</template>
