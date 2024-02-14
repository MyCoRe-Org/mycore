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

function onScroll(evt:any) {
  console.log(evt)
}
</script>

<template>
  <prism-editor @scroll="onScroll" ref="editor" class="texteditor" v-model="model.data" :highlight="highlighter" line-numbers></prism-editor>
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

</style>
