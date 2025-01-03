<script setup lang="ts">
import HelloWorld from './components/HelloWorld.vue'
import {Registry} from "./model/model.ts";
import type {
  AddCollectionMessage,
  ErrorMessage,
  RegistryMessage,
  UpdateCollectionPropertyMessage,
  UpdateProcessableMessage
} from "./model/messages.ts";
import {ref} from "vue";
import RegistryComponent from "./components/RegistryComponent.vue";

/*
const loc = window.location;
let protocol = 'ws://';
if (location.protocol === 'https:') {
  protocol = 'wss://';
}
this.socketURL = protocol + loc.host + Util.getBasePath(loc.pathname) + this.path;
 */
const socketURL: string = "ws://localhost:8291/mir/ws/mycore-webtools/processing";
let retryCounter: number = 0;
let socket: WebSocket | undefined = undefined;
let errorCode: number | undefined = undefined;
let registry = ref(new Registry());

connect();

function send(message: string) {
  if (message === '') {
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
    // TODO
    console.log(event);
  }
  socket.onclose = (closeEvent: CloseEvent) => {
    // TODO
    console.log(closeEvent);
  }
  socket.onopen = () => {
    send(JSON.stringify({
      type: 'connect'
    }));
  };
}

function handleMessage(data: RegistryMessage | AddCollectionMessage | UpdateProcessableMessage | UpdateCollectionPropertyMessage | ErrorMessage) {
  switch (data.type) {
    case 'error':
      const errorMessage = <ErrorMessage>data;
      errorCode = parseInt(errorMessage.error);
      break;
    case 'registry':
      errorCode = undefined;
      registry.value = new Registry();
      break;
    case 'addCollection':
      registry.value.addCollection(<AddCollectionMessage>data);
      break;
    case 'updateProcessable':
      registry.value.updateProcessable(<UpdateProcessableMessage>data);
      // triggerDelayedUpdate();
      break;
    case 'updateCollectionProperty':
      let updatePropertyMessage = <UpdateCollectionPropertyMessage>data;
      const collection = registry.value.getCollection(updatePropertyMessage.id);
      if (collection == null) {
        console.warn('Unable to find collection with id ' + updatePropertyMessage.id);
        return;
      }
      collection.setProperty(updatePropertyMessage.propertyName, updatePropertyMessage.propertyValue);
      // triggerDelayedUpdate();
      break;
    default:
      console.warn('Unable to handle data type: ' + data.type);
  }
}

</script>

<template>
  <h1>MyCoRe Processing</h1>
  <RegistryComponent :registry="registry" />
</template>

<style scoped>
</style>
