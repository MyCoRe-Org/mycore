<script setup lang="ts">
import {nextTick, type Ref, ref, watch} from "vue";
import {ContentHandlerSelector} from "@/apis/ContentHandlerSelector";
import TextEditor from "@/components/TextEditor.vue";

const props = defineProps<{
  type: string,
  id: string
}>();
let model: Ref<Content> = ref({
  data: "",
  type: ""
});
let loading: any = ref(true);
let success: any = ref();
let error: any = ref();
let updateEnabled = ref(false);
let writeAccess = ref(false);
let undoKeyPressed = false;
let originalModel: string = "";

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
  } catch (err: any) {
    console.log(err);
    error.value = err;
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
  } catch (err: any) {
    console.log(err);
    error.value = err;
  }
  loading.value = false;
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
    success.value = "Erfolgreich gespeichert!";
    if (contentHandler.dirtyAfterSave(props.id)) {
      await load();
    } else {
      originalModel = model.value.data;
    }
  } catch (err: any) {
    console.log(err);
    error.value = err;
    updateEnabled.value = true;
  }
  loading.value = false;
}

const onKeyDown = async (evt: any) => {
  undoKeyPressed = evt.ctrlKey && (evt.key === "z" || evt.key === "Z");
  // fix undo is not triggered on empty textarea
  await nextTick();
  if (undoKeyPressed && model.value.data === "") {
    model.value.data = originalModel;
  }
}

load();
hasWriteAccess();
</script>

<template>
  <text-editor @keydown="onKeyDown" v-model="model" :loading="loading" :writeAccess="writeAccess"></text-editor>
  <footer>
    <div class="info">
      <div v-if="success" class="alert alert-success">
        {{ success }}
      </div>
      <div v-if="error" class="alert alert-danger">
        {{ error }}
      </div>
    </div>
    <button @click="save" v-if="writeAccess" class="btn btn-sm btn-primary" :disabled="!updateEnabled">Update</button>
  </footer>
</template>

<style scoped>
footer {
  margin-top: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
}

button {

}

.info {
  flex-grow: 1;
}
</style>
