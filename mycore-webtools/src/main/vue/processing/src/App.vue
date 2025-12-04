<script setup lang="ts">
import type {IncomingMessage} from "./model/messages.ts";
import RegistryComponent from "./components/RegistryComponent.vue";
import SettingsModal from "./components/SettingsModal.vue";
import {Registry} from "./model/model.ts";
import {ref, useTemplateRef} from "vue";

const socketURL: string = getSocketURL();
let retryCounter: number = 0;
let socket: WebSocket | undefined = undefined;
let errorCode: number | undefined = undefined;
let errorMessage = ref<string | undefined>(undefined);
let registry = ref(new Registry());
const settingsModal = useTemplateRef("settingsModal");

connect();

function getSocketURL() {
  if (import.meta.env.DEV) {
    // in dev mode connect to a local mir
    return "ws://localhost:8291/mir/ws/mycore-webtools/processing";
  }
  const wsProtocol = location.protocol === "https:" ? "wss://" : "ws://";
  const {pathname} = new URL(mycore.webApplicationBaseURL);
  const basePath = pathname === "/" ? "" : pathname.replace(/\/$/, "");
  return `${wsProtocol}${location.host}${basePath}/ws/mycore-webtools/processing`;
}

function send(message: string) {
  if (message === "") {
    return;
  }
  retryCounter++;
  if (socket === null || socket === undefined || socket.readyState === 3) {
    if (retryCounter < 5) {
      connect();
      send(message);
    }
    return;
  }
  if (socket.readyState === 1) {
    retryCounter = 0;
    socket.send(message);
    return;
  }
  if (socket.readyState === 0 || socket.readyState === 2) {
    if (retryCounter < 5) {
      setTimeout(() => send(message), 500);
    }
    return;
  }
}

function connect() {
  retryCounter = 0;

  socket = new WebSocket(socketURL);

  socket.onmessage = (message: MessageEvent) => {
    handleMessage(JSON.parse(message.data));
  }
  socket.onerror = (event: Event) => {
    errorMessage.value = "A websocket error occurred.";
    console.log(event);
  }
  socket.onclose = (closeEvent: CloseEvent) => {
    setTimeout(() => connect(), 5000);
    console.log(closeEvent);
  }
  socket.onopen = () => {
    send(JSON.stringify({
      type: "connect"
    }));
  };
}

function handleMessage(data: IncomingMessage) {
  switch (data.type) {
    case "ERROR":
      errorCode = parseInt(data.error);
      errorMessage.value = "A server error occurred: " + errorCode;
      break;
    case "REGISTRY":
      errorCode = undefined;
      errorMessage.value = undefined;
      registry.value = new Registry();
      break;
    case "ADD_COLLECTION":
      registry.value.addCollection(data);
      break;
    case "UPDATE_PROCESSABLE":
      registry.value.updateProcessable(data);
      break;
    case "UPDATE_COLLECTION_PROPERTY":
      const collection = registry.value.getCollection(data.id);
      if (collection == null) {
        console.warn("Unable to find collection with id " + data.id);
        return;
      }
      collection.setProperty(data.propertyName, data.propertyValue);
      break;
  }
}

function showSettingsModal() {
  if(settingsModal.value) {
    settingsModal.value.show();
  }
}

</script>

<template>
  <h1>
    MyCoRe Processing
    <a class="ms-1 text-secondary" href="#" @click.prevent="showSettingsModal">
      <i class="fa-solid fa-gear fa-xs"></i>
      <span class="visually-hidden">Settings</span>
    </a>
  </h1>
  <div v-if="errorMessage !== undefined" class="error alert alert-danger">
    {{ errorMessage }}
  </div>
  <RegistryComponent :registry="registry"/>
  <SettingsModal ref="settingsModal" />
</template>

<style scoped>

</style>
