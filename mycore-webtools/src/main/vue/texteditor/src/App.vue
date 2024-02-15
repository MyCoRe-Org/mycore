<script setup lang="ts">
import {RouterView} from 'vue-router'

import {ContentHandlerSelector} from "@/apis/ContentHandlerSelector";
import {ObjectsContentHandler} from "@/apis/ObjectsContentHandler";
import {ClassificationsContentHandler} from "@/apis/ClassificationsContentHandler";
import {getMCRApplicationBaseURL} from "@/router";

const mcrApplicationBaseURL = getMCRApplicationBaseURL();

// register content handler
ContentHandlerSelector.registerContentHandler("objects", new ObjectsContentHandler(mcrApplicationBaseURL));
ContentHandlerSelector.registerContentHandler("classifications", new ClassificationsContentHandler(mcrApplicationBaseURL));

</script>

<template>
  <RouterView/>
</template>

<style>
</style>
