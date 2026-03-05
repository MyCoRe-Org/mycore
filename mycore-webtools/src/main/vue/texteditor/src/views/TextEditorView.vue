<script setup lang="ts">
import {computed, nextTick, ref, watch, onBeforeUnmount, onMounted} from "vue";
import {ContentHandlerSelector} from "@/apis/ContentHandlerSelector";
import TextEditor from "@/components/TextEditor.vue";
import type {Content, ContentHandler, LockResult} from "@/apis/ContentHandler.ts";
import {getMCRApplicationBaseURL} from "@/router";
import {useRouter} from "vue-router";

const props = defineProps<{
  type: string,
  id: string
}>();
const model = ref<Content>({
  data: "",
  type: ""
});
const loading = ref(true);
const success = ref<string | undefined>();
const error = ref<string | undefined>();
const updateEnabled = ref(false);
const writeAccess = ref(false);
const lockResult = ref<LockResult | undefined>(undefined);
let undoKeyPressed = false;
let originalModel: string = "";
let refreshInterval: number | undefined;

const router = useRouter();

onMounted(() => {
  window.addEventListener("beforeunload", handleBeforeUnload);
});

onBeforeUnmount(async () => {
  unlockIfPossible();
  window.removeEventListener("beforeunload", handleBeforeUnload);
});

const handleBeforeUnload = () => {
  unlockIfPossible();
};

const isContentFile = computed(() => props.type === 'objects' && props.id.includes('/contents/'));
const isContents = computed(() => props.type === 'objects' && props.id.endsWith('/contents'));
const isDerivate = computed(() => props.type === 'objects' && props.id.includes('/derivates/') && !isContents.value && !isContentFile.value);
const isObject = computed(() => props.type === 'objects' && !isDerivate.value && !isContents.value && !isContentFile.value);

interface Breadcrumb {
  label: string;
  path: string[] | null;
}

const breadcrumbs = computed((): Breadcrumb[] => {
  const segments = props.id.split('/');
  const result: Breadcrumb[] = [];
  for (let i = 0; i < segments.length; i++) {
    const segment = segments[i]!;
    if (segment === 'derivates') continue;
    const isCurrent = i === segments.length - 1;
    result.push({
      label: segment,
      path: isCurrent ? null : segments.slice(0, i + 1)
    });
  }
  return result;
});

function navigateToBreadcrumb(path: string[]) {
  router.push({name: 'TextEditor', params: {type: props.type, id: path}});
}

function openViewObject() {
  window.location.href = `${getMCRApplicationBaseURL()}receive/${props.id}`;
}

function openViewContents() {
  router.push({name: 'TextEditor', params: {type: props.type, id: [...props.id.split('/'), 'contents']}});
}

watch(() => model.value.data, async (newModel) => {
  // prevent ctrl+z clearing the textarea
  await nextTick();
  if (undoKeyPressed && newModel === "") {
    model.value.data = originalModel;
    updateEnabled.value = false;
  } else {
    updateEnabled.value = originalModel !== newModel;
  }
});

const hasWriteAccess = async () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (contentHandler === undefined) {
    return;
  }
  try {
    writeAccess.value = await contentHandler.hasWriteAccess(props.id);
  } catch (err) {
    console.error(err);
    error.value = err instanceof Error ? err.message : String(err);
  }
}

const load = async () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (contentHandler === undefined) {
    return;
  }
  try {
    const content: Content = await contentHandler.load(props.id);
    model.value.data = content.data;
    model.value.type = content.type;
    originalModel = content.data;
  } catch (err) {
    console.error(err);
    error.value = err instanceof Error ? err.message : String(err);
  } finally {
    loading.value = false;
  }
}

const save = async () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (contentHandler === undefined) {
    return;
  }
  loading.value = true;
  success.value = undefined;
  error.value = undefined;
  updateEnabled.value = false;
  try {
    await contentHandler.save(props.id, model.value);
    success.value = "Successfully saved!";
    if (contentHandler.dirtyAfterSave(props.id)) {
      await load();
    } else {
      originalModel = model.value.data;
    }
  } catch (err) {
    console.error(err);
    error.value = err instanceof Error ? err.message : String(err);
    updateEnabled.value = true;
  } finally {
    loading.value = false;
  }
}

/**
 * Checks whether locking is possible.
 * <ul>
 *   <li>the content handler supports the locking</li>
 *   <li>we have write access</li>
 *   <li>it's a valid document</li>
 * </ul>
 */
const isLockingPossible = () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (contentHandler === undefined) {
    return;
  }
  const canWrite = writeAccess.value;
  const supportsLocking = contentHandler.supportsLocking(props.id);
  const isValidDocument = model.value.type !== "";
  return isValidDocument && canWrite && supportsLocking;
}

/**
 * Locks a resource if possible. This only happens if we have write access and the resource can be locked.
 */
const lockIfPossible = async () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (!contentHandler || !isLockingPossible()) {
    return;
  }
  try {
    lockResult.value = await contentHandler.lock(props.id);
  } catch (err) {
    console.error(err);
    error.value = err instanceof Error ? err.message : String(err);
  }
};

/**
 * Unlocks a resource if possible. This only happens if we have write access and the resource can be locked.
 */
const unlockIfPossible = () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (!contentHandler || !isLockingPossible()) {
    return;
  }
  try {
    contentHandler.unlock(props.id);
  } catch (e) {
    console.error("unlock failed", e);
  }
};

const startLockRefresh = () => {
  stopLockRefresh();
  refreshInterval = window.setInterval(async () => {
    await lockIfPossible();
  }, 30000);
}

const stopLockRefresh = () => {
  if (refreshInterval) {
    clearInterval(refreshInterval);
    refreshInterval = undefined;
  }
}

const onKeyDown = async (evt: KeyboardEvent) => {
  undoKeyPressed = evt.ctrlKey && (evt.key === "z" || evt.key === "Z");
  // fix undo is not triggered on empty textarea
  await nextTick();
  if (undoKeyPressed && model.value.data === "") {
    model.value.data = originalModel;
  }
}

watch(() => [props.type, props.id], async () => {
  loading.value = true;
  success.value = undefined;
  error.value = undefined;
  updateEnabled.value = false;
  await Promise.all([load(), hasWriteAccess()]);
  await lockIfPossible();
  if (isLockingPossible()) {
    startLockRefresh();
  }
}, {immediate: true});
</script>

<template>
  <div class="toolbar">
    <div class="toolbar-left">
      <nav class="breadcrumb">
        <template v-for="(crumb, index) in breadcrumbs" :key="index">
          <span v-if="index > 0" class="breadcrumb-sep">›</span>
          <span v-if="crumb.path" class="breadcrumb-link" @click="navigateToBreadcrumb(crumb.path)">{{ crumb.label }}</span>
          <span v-else class="breadcrumb-current">{{ crumb.label }}</span>
        </template>
      </nav>
      <template v-if="isObject || isDerivate">
        <div class="toolbar-divider"></div>
        <button @click="openViewObject" v-if="isObject" class="btn btn-sm btn-outline-secondary">View Object</button>
        <button @click="openViewContents" v-if="isDerivate" class="btn btn-sm btn-outline-secondary">View Contents</button>
      </template>
    </div>
    <div class="toolbar-right">
      <button @click="save" v-if="writeAccess && !isContents" class="btn btn-sm btn-primary" :disabled="!updateEnabled">Update</button>
    </div>
  </div>
  <text-editor @keydown="onKeyDown" v-model="model" :loading="loading" :writeAccess="writeAccess"></text-editor>
  <div class="hint-bar">
    <span class="hint"><kbd>Ctrl</kbd>+Click on links to navigate</span>
  </div>
  <div class="info">
    <div v-if="success" class="alert alert-success">{{ success }}</div>
    <div v-if="error" class="alert alert-danger">{{ error }}</div>
    <div v-if="lockResult && lockResult.status === 'not_owner'" class="alert alert-warning">
      This object is currently being edited by another user!
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.5rem 0;
  margin-bottom: 1rem;
  gap: 1rem;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}

.toolbar-divider {
  width: 1px;
  height: 1.25rem;
  background: #dee2e6;
  flex-shrink: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  font-family: monospace;
  font-size: 0.9rem;
  margin-bottom: 0;
}

.breadcrumb-sep {
  color: #adb5bd;
  user-select: none;
  font-size: 1rem;
}

.breadcrumb-link {
  color: #6c757d;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.15s;
}

.breadcrumb-link:hover {
  color: #0d6efd;
  text-decoration: underline;
}

.breadcrumb-current {
  color: #212529;
  font-weight: 600;
  white-space: nowrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}

.hint-bar {
  text-align: center;
  margin-top: 0.5rem;
}

.hint {
  font-size: 0.75rem;
  color: #adb5bd;
}

.hint kbd {
  background: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 3px;
  padding: 1px 4px;
  font-size: 0.7rem;
  color: #495057;
}

.info {
  margin-top: 0.5rem;
}
</style>
