<template>
  <form class="form">
    <div class="row mt-2 mb-2">
      <div class="col-2 mt-2 text-center">
        <h3>{{ model.i18n.jobqueue_job_search }}</h3>
      </div>
      <div class="col-8">
        <input v-on:submit.prevent="emitSearch" v-model="model.searchText" class="w-100 form-control" type="text"
               id="parameterName" placeholder="name:value name2:value2">
      </div>
      <div class="col-2">
        <button v-on:click.prevent="emitSearch" type="submit" class="w-100 pr-3 btn btn-primary">
          {{ model.i18n.jobqueue_job_search_button }}
        </button>
      </div>
    </div>
  </form>

</template>

<script setup lang="ts">

import {computed, reactive} from "vue";
import {i18n} from "@/api/I18N";

const emit = defineEmits(["search"]);

const props = defineProps<{ parameter: Array<{ name: string, value: string }> }>();

const convertParameterToText = (param: Array<{ name: string, value: string }>|null) =>
    param == null ? "" :
    param.map((p) => `${p.name}:${p.value}`).join(" ");

const convertTextToParameter = (text: string) => text.split(" ").map((p) => {
  const [name, value] = p.split(":", 2);
  return {name, value};
});

const model = reactive({
  searchText: convertParameterToText(props.parameter),
  i18n: {} as any
});

[
  "jobqueue.job.search",
  "jobqueue.job.search.button"

].forEach((key) => {
  let keyInObject = key.split(".").join("_");
  model.i18n[keyInObject] = "%" + keyInObject + "%";
  i18n(key).then((value) => {
    model.i18n[keyInObject] = value;
  });
});


const emitSearch = () => {
  emit("search", {"parameter": convertTextToParameter(model.searchText)});
}

</script>

<style scoped>

</style>