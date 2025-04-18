<script setup lang="ts">
import type {
  AddCollectionMessage,
  ErrorMessage,
  RegistryMessage,
  UpdateCollectionPropertyMessage,
  UpdateProcessableMessage
} from "./model/messages.ts";
import RegistryComponent from "./components/RegistryComponent.vue";
import SettingsModal from "./components/SettingsModal.vue";
import {Registry} from "./model/model.ts";
import {Util} from "./common/util.ts";
import {ref} from "vue";

const socketURL: string = getSocketURL();
let retryCounter: number = 0;
let socket: WebSocket | undefined = undefined;
let errorCode: number | undefined = undefined;
let errorMessage = ref<string | undefined>(undefined);
let registry = ref(new Registry());

connect();

function getSocketURL() {
  if (import.meta.env.DEV) {
    // in dev mode connect to a local mir
    return "ws://localhost:8291/mir/ws/mycore-webtools/processing";
  } else {
    const protocol = location.protocol === "https:" ? "wss://" : "ws://";
    return protocol + location.host + Util.getBasePath() + "/ws/mycore-webtools/processing";
  }
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

function handleMessage(data: RegistryMessage | AddCollectionMessage | UpdateProcessableMessage | UpdateCollectionPropertyMessage | ErrorMessage) {
  switch (data.type) {
    case "ERROR":
      const serverMessage = <ErrorMessage>data;
      errorCode = parseInt(serverMessage.error);
      errorMessage.value = "A server error occurred: " + errorCode;
      break;
    case "REGISTRY":
      errorCode = undefined;
      errorMessage.value = undefined;
      registry.value = new Registry();
      break;
    case "ADD_COLLECTION":
      registry.value.addCollection(<AddCollectionMessage>data);
      break;
    case "UPDATE_PROCESSABLE":
      registry.value.updateProcessable(<UpdateProcessableMessage>data);
      break;
    case "UPDATE_COLLECTION_PROPERTY":
      let updatePropertyMessage = <UpdateCollectionPropertyMessage>data;
      const collection = registry.value.getCollection(updatePropertyMessage.id);
      if (collection == null) {
        console.warn("Unable to find collection with id " + updatePropertyMessage.id);
        return;
      }
      collection.setProperty(updatePropertyMessage.propertyName, updatePropertyMessage.propertyValue);
      break;
    default:
      console.warn("Unable to handle data type: " + data.type);
  }
}

function toggleSettingsModal() {
  isSettingsModalVisible.value = !isSettingsModalVisible.value;
}

const isSettingsModalVisible = ref(false);

</script>

<template>
  <h1>
    MyCoRe Processing
    <a class="modal-button" href="#" @click.prevent="toggleSettingsModal">(Settings)</a>
  </h1>
  <div v-if="errorMessage !== undefined" class="error">
    {{ errorMessage }}
  </div>
  <RegistryComponent :registry="registry"/>
  <transition name="modal-transition">
    <SettingsModal v-if="isSettingsModalVisible" @close="toggleSettingsModal"/>
  </transition>
</template>

<style scoped>
.error {
  background-color: #ffc1ab;
  padding: 1rem;
  border-radius: 5px;
}
</style>
