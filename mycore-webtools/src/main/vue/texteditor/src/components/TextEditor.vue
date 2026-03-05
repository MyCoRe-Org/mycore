<script setup lang="ts">
import {PrismEditor} from 'vue-prism-editor';
import {highlight, languages} from 'prismjs';

import 'vue-prism-editor/dist/prismeditor.min.css';
import 'prismjs/themes/prism.css';
import '../modules/prismjs-languages';
import {getPrismLanguage, type PrismLanguage} from "@/modules/prismjs-languages";
import {onMounted, ref} from "vue";
import type {Content} from "@/apis/ContentHandler.ts";
import {useRouter, useRoute} from "vue-router";

const model = defineModel<Content>({required: true});
const editor = ref();
const router = useRouter();
const route = useRoute();

const props = defineProps<{ loading: boolean, writeAccess: boolean }>();

const MCR_ID_PATTERN = /[a-z][a-z0-9]*_[a-z][a-z0-9]*_\d{8}/g;
const TEXT_FILE_PATTERN = /\b[\w-][\w.-]*\.(xml|xsl|xslt|txt|md|markdown|csv|yaml|yml|json)\b/gi;

function wrapMcrLinks(html: string): string {
  return html.replace(MCR_ID_PATTERN, (id) =>
    `<span class="mcr-link" data-mcr-id="${id}" title="Ctrl+Click to open">${id}</span>`
  );
}

function wrapFileLinks(html: string): string {
  const currentId = Array.isArray(route.params.id) ? route.params.id.join('/') : route.params.id as string;
  if (!currentId?.endsWith('/contents')) return html;
  return html.replace(TEXT_FILE_PATTERN, (filename) =>
    `<span class="file-link" data-file-name="${filename}" title="Ctrl+Click to open">${filename}</span>`
  );
}

function navigateToMcrId(id: string) {
  if (id.includes('_class_')) {
    router.push({name: 'TextEditor', params: {type: 'classifications', id}});
  } else if (id.includes('_derivate_')) {
    const currentId = Array.isArray(route.params.id) ? route.params.id.join('/') : (route.params.id ?? '');
    const parentObjectId = currentId.split('/')[0] ?? '';
    router.push({name: 'TextEditor', params: {type: 'objects', id: [parentObjectId, 'derivates', id]}});
  } else {
    router.push({name: 'TextEditor', params: {type: 'objects', id}});
  }
}

function navigateToFile(filename: string) {
  const currentId = Array.isArray(route.params.id) ? route.params.id.join('/') : route.params.id as string;
  router.push({name: 'TextEditor', params: {type: route.params.type, id: [...currentId.split('/'), filename]}});
}

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

  // In readonly mode the textarea has pointer-events: none, so clicks reach the pre directly
  pre.addEventListener('click', (e: MouseEvent) => {
    const target = e.target as HTMLElement;
    if (target.dataset.mcrId) {
      navigateToMcrId(target.dataset.mcrId);
    } else if (target.dataset.fileName) {
      navigateToFile(target.dataset.fileName);
    }
  });

  function getLinkUnder(e: MouseEvent): HTMLElement | null {
    for (const el of pre.querySelectorAll('.mcr-link, .file-link') as NodeListOf<HTMLElement>) {
      const rect = el.getBoundingClientRect();
      if (e.clientX >= rect.left && e.clientX <= rect.right &&
          e.clientY >= rect.top && e.clientY <= rect.bottom) {
        return el;
      }
    }
    return null;
  }

  textarea.addEventListener('mousemove', (e: MouseEvent) => {
    if (!e.ctrlKey) {
      textarea.style.cursor = '';
      return;
    }
    textarea.style.cursor = getLinkUnder(e) ? 'pointer' : '';
  });

  textarea.addEventListener('mouseleave', () => {
    textarea.style.cursor = '';
  });

  textarea.addEventListener('click', (e: MouseEvent) => {
    if (!e.ctrlKey) return;
    const link = getLinkUnder(e);
    if (link?.dataset.mcrId) {
      navigateToMcrId(link.dataset.mcrId);
    } else if (link?.dataset.fileName) {
      navigateToFile(link.dataset.fileName);
    }
  });
})

function highlighter(text: string) {
  let highlighted: string;
  if (!model.value?.type) {
    highlighted = highlight(text, languages.xml!, 'xml');
  } else {
    const prismLanguage: PrismLanguage | undefined = getPrismLanguage(model.value.type);
    if (prismLanguage === undefined) {
      highlighted = highlight(text, languages.xml!, 'xml');
    } else {
      highlighted = highlight(text, prismLanguage.grammar, prismLanguage.language);
    }
  }
  return wrapFileLinks(wrapMcrLinks(highlighted));
}

</script>

<template>
  <div class="overlay">
    <prism-editor ref="editor" class="texteditor" v-model="model.data"
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

.texteditor.loading::v-deep(textarea) {
  cursor: default;
}

.texteditor:not(.writeAccess)::v-deep(textarea) {
  cursor: default;
}

.texteditor::v-deep(.mcr-link),
.texteditor::v-deep(.file-link) {
  color: #0066cc;
  text-decoration: underline;
  cursor: pointer;
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
