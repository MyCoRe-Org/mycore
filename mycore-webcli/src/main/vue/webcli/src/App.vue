<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';

import CommandHistoryDialog from '@/components/CommandHistoryDialog.vue';
import CommandMenu from '@/components/CommandMenu.vue';
import CommandSuggestions from '@/components/CommandSuggestions.vue';
import SettingsDialog from '@/components/SettingsDialog.vue';
import { useCommandInputController } from '@/composables/useCommandInputController';
import { useSettingsState } from '@/composables/useSettingsState';
import { useWebCliTransport } from '@/composables/useWebCliTransport';

const activeTab = ref<'log' | 'queue'>('log');
const isSettingsDialogOpen = ref(false);
const isCommandHistoryOpen = ref(false);
const isQueueExpanded = ref(false);
const logElement = ref<HTMLElement | null>(null);
const queuePreviewSize = 99;
const { settings } = useSettingsState();

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
  clearCommandHistory,
  command,
  commandHistoryEntries,
  executeButtonElement,
  executeCommand,
  hasSuggestions,
  highlightedIndex,
  highlightedSuggestion,
  inputElement,
  isSuggestionMenuVisible,
  onCommandInput,
  onCommandInputBlur,
  onCommandInputFocus,
  onCommandKeydown,
  onSuggestionActivate,
  restoreCommandFromHistory,
  selectCommand,
  suggestionListId,
  suggestions,
  totalSuggestionCount,
} = useCommandInputController({
  commandGroups,
  commandHistorySize: computed(() => settings.value.comHistorySize),
  suggestionLimit: computed(() => settings.value.suggestionLimit),
  onExecuteCommand: runCommand,
});

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

function openCommandHistory(): void {
  isCommandHistoryOpen.value = true;
}

function openSettingsDialog(): void {
  isSettingsDialogOpen.value = true;
}

function toggleQueueExpansion(): void {
  isQueueExpanded.value = !isQueueExpanded.value;
}

function onSelectCommandFromHistory(value: string): void {
  restoreCommandFromHistory(value);
  isCommandHistoryOpen.value = false;
}

watch(() => settings.value.continueIfOneFails, value => {
  updateContinueIfOneFails(value);
});

watch(() => settings.value.historySize, () => {
  trimLogs();
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
      @select-command="onSelectCommandFromHistory"
    />
    <SettingsDialog
      v-model="isSettingsDialogOpen"
      v-model:settings="settings"
      :has-command-history="commandHistoryEntries.length > 0"
      @clear-command-history="clearCommandHistory"
    />
  </div>
</template>
