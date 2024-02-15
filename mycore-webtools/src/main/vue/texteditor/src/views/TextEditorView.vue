<script setup lang="ts">
import {type Ref, ref, watch} from "vue";
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
let success: any = ref();
let error: any = ref();
let updateEnabled = ref(false);
let originalModel: string = "";
let writeAccess = ref(false);

watch(() => model.value.data, async (newModel) => {
  updateEnabled.value = originalModel !== newModel;
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
}

const save = async () => {
  const contentHandler: ContentHandler | undefined = ContentHandlerSelector.get(props.type);
  if (contentHandler === undefined) {
    return;
  }
  updateEnabled.value = false;
  try {
    await contentHandler.save(props.id, model.value);
    originalModel = model.value.data;
    success.value = "Erfolgreich gespeichert!";
  } catch (err: any) {
    console.log(err);
    error.value = err;
    updateEnabled.value = true;
  }
}
load();
hasWriteAccess();
</script>

<template>
  <text-editor v-model="model"></text-editor>
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
