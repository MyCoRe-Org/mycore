<template>
  <main class="card">
    <div class="card-header">
      <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
          <li class="breadcrumb-item">
            <router-link :to="{ name: 'JobQueueListView' }">Job Queue</router-link>
          </li>
        </ol>
      </nav>
    </div>
    <div class="card-body">
      <div v-if="model.status === 'loading'" class="text-center">
        <div class="spinner-border" role="status">
          <span class="sr-only">Loading...</span>
        </div>
      </div>
      <div class="jobqueue-list-wrapper" v-else-if="model.status === 'loaded'">
        <ol class="jobqueue-list list-group" v-if="model.queues !== null">
          <li class="jobqueue-list-item list-group-item" v-for="queue in model.queues.queue">
            <router-link :to="{ name: 'JobQueueViewStatus', params: { queueId: queue.name, stats: 'ALL' } }">{{ queue.name }}</router-link>
          </li>
        </ol>
      </div>
      <div v-else-if="model.status === 'error'" class="alert alert-danger" role="alert">
        {{ model.errorMessage }}
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import {useRouter} from 'vue-router'
import type {JobQueueList} from "@/api/JobQueue";
import {reactive} from "vue";
import {getAuthorizationHeader} from "@/api/Auth";
import {getBaseURL} from "@/api/BaseURL";

const router = useRouter()

const baseUrl =  getBaseURL();


const model = reactive({
  status: 'loading' as 'loading' | 'error' | 'loaded',
  errorMessage: null as string | null,
  queues: null as JobQueueList | null
});

const retrieveJobs = async () => {
  try{
    const response = await fetch(baseUrl + 'api/jobqueue/', {
      headers: {
        authorization: await getAuthorizationHeader()
      }
    });
    if(response.ok){
      model.queues = await response.json();
      model.status = 'loaded';
    } else {
      model.status = 'error';
      if(response.status === 404){
        await router.replace({
          name: 'JobQueue404'
        });
      }
    }
  } catch (e) {
    model.status = 'error';
  }
}

retrieveJobs();

</script>

<style>

</style>

