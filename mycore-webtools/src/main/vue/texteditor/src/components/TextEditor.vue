<script setup lang="ts">
import {PrismEditor} from 'vue-prism-editor';
import {highlight, languages} from 'prismjs';

import 'vue-prism-editor/dist/prismeditor.min.css';
import 'prismjs/themes/prism.css';
import '../modules/prismjs-languages';
import {getPrismLanguage, type PrismLanguage} from "@/modules/prismjs-languages";
import {onMounted, ref, type Ref} from "vue";

const model: Ref = defineModel<Content>();
const editor: Ref = ref(null);

const props = defineProps(["loading", "writeAccess"]);

onMounted(() => {
  const pre = editor.value.$refs.pre;
  const textarea = editor.value.$refs.textarea;
  pre.onscroll = () => {
    textarea.scrollTop = pre.scrollTop;
    textarea.scrollLeft = pre.scrollLeft;
  }
  textarea.onscroll = () => {
    pre.scrollTop = textarea.scrollTop;
    pre.scrollLeft = textarea.scrollLeft;
  }
})

function highlighter(text: string) {
  if (!model.value?.type) {
    return highlight(text, languages.xml, 'xml');
  }
  let prismLanguage: PrismLanguage | undefined = getPrismLanguage(model.value.type);
  if (prismLanguage === undefined) {
    return;
  }
  return highlight(text, prismLanguage.grammar, prismLanguage.language);
}

function onScroll(evt: any) {
  console.log(evt)
}
</script>

<template>
  <div class="overlay">
    <prism-editor @scroll="onScroll" ref="editor" class="texteditor" v-model="model.data"
                  :class="{loading: props.loading, writeAccess: props.writeAccess}"
                  :highlight="highlighter" line-numbers :readonly="props.loading || !props.writeAccess">
    </prism-editor>
    <div v-if="props.loading" class="spinner"></div>
  </div>
</template>

<style scoped>
.texteditor {
  font-family: monospace;
}

.texteditor::v-deep(textarea) {
  outline: none;
  white-space: pre;
  overflow: auto;
}

.texteditor::v-deep(pre) {
  white-space: pre;
  overflow: auto;
}

.texteditor::v-deep(.prism-editor__line-numbers) {
  cursor: default;
}

.texteditor.loading {
  filter: opacity(0.5);
}

.texteditor.loading::v-deep(textarea), .texteditor:not(.writeAccess)::v-deep(textarea) {
  cursor: default;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 5px solid #FFF;
  border-bottom-color: #FF3D00;
  border-radius: 50%;
  display: inline-block;
  box-sizing: border-box;
  animation: rotation 1s linear infinite;
  position: absolute;
  top: calc(50% - 48px);
  left: calc(50% - 48px);
}

@keyframes rotation {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

</style>
