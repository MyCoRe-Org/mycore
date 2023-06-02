<template>
  <div class="jumbotron">
    <p v-if="model.message">{{ model.message }}</p>
  </div>
</template>

<script setup lang="ts">

import {i18n} from "../api/I18N";
import {reactive} from "vue";

let model = reactive({
  message: ""
});

i18n("jobqueue.404").then((value) => {
  model.message = value;
});
</script>

<style scoped>

</style>