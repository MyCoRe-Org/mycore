<!--
  - This file is part of ***  M y C o R e  ***
  - See http://www.mycore.de/ for details.
  -
  - MyCoRe is free software: you can redistribute it and/or modify
  - it under the terms of the GNU General Public License as published by
  - the Free Software Foundation, either version 3 of the License, or
  - (at your option) any later version.
  -
  - MyCoRe is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU General Public License for more details.
  -
  - You should have received a copy of the GNU General Public License
  - along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
  -->

<template>
  <main class="card">
    <div class="card-header">
      <nav aria-label="breadcrumb">
        <ol class="breadcrumb">
          <li class="breadcrumb-item">
            <router-link :to="{ name: 'JobQueueListView' }">Job Queue</router-link>
          </li>
          <li class="breadcrumb-item">
            <router-link :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'ALL' } }">
              {{ route.params.queueId }}
            </router-link>
          </li>
          <li class="breadcrumb-item">
            <router-link
                :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: route.params.stats } }">
              {{ model.i18n["jobqueue_job_status_" + route.params.stats] }}
            </router-link>
          </li>
        </ol>
      </nav>

      <ul class="nav nav-tabs card-header-tabs">
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'ALL' } }">
            {{ model.i18n.jobqueue_job_status_ALL }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'NEW' } }">
            {{ model.i18n.jobqueue_job_status_NEW }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'PROCESSING' } }">
            {{ model.i18n.jobqueue_job_status_PROCESSING }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'FINISHED' } }">
            {{ model.i18n.jobqueue_job_status_FINISHED }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'ERROR' } }">
            {{ model.i18n.jobqueue_job_status_ERROR }}
          </router-link>
        </li>
        <li class="nav-item">
          <router-link class="nav-link" active-class="active"
                       :to="{ name: 'JobQueueViewStatus', params: { queueId: route.params.queueId, stats: 'MAX_TRIES' } }">
            {{ model.i18n.jobqueue_job_status_MAX_TRIES }}
          </router-link>
        </li>
      </ul>
    </div>
    <div class="card-body p-0">
      <div v-if="model.status === 'loading'" class="text-center">
        <div class="spinner-border" role="status">
          <span class="sr-only">Loading...</span>
        </div>
      </div>
      <div class="jobqueue" v-else-if="model.status === 'loaded'">
        <div class="jobqueue-search-wrapper">
          <JobSearch v-on:search="executeSearch" v-if="parameter != undefined" :parameter="parameter"/>
        </div>

        <div class="jobqueue-pagination-wrapper">
          <Pagination v-if="pageCount>1" :current-page="currentPage" :page-count="pageCount"
                      v-on:pageClicked="navigateToPage"/>
        </div>
        <div class="jobqueue-table-wrapper">
          <table class="table">
            <thead>
            <tr>
              <th scope="col">{{ model.i18n.jobqueue_job_jobId }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_status }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_date_added }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_date_start }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_date_finished }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_exception }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_tries }}</th>
              <th scope="col">{{ model.i18n.jobqueue_job_parameter }}</th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="job in model.queues?.job">
              <th scope="row">
                {{ job.id }}
              </th>
              <td>{{ job.status }}</td>
              <td>{{ getDate(job, 'added') }}</td>
              <td>{{ getDate(job, 'start') }}</td>
              <td>{{ getDate(job, 'finished') }}</td>
              <td>
                <shortened-text v-if="job.exception" :max-length="50" :text="job.exception"></shortened-text>
              </td>
              <td>{{ job.tries }}</td>
              <td>
                <shortened-text v-if="job.parameter" :max-length="50" :text="getParameter(job)"></shortened-text>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
        <div class="jobqueue-pagination-wrapper">
          <Pagination v-if="pageCount>1" :current-page="currentPage" :page-count="pageCount"
                      v-on:pageClicked="navigateToPage"/>
        </div>
      </div>
      <div v-else-if="model.status === 'error'" class="alert alert-danger" role="alert">
        {{ model.errorMessage }}
      </div>
    </div>
  </main>
</template>

<script setup lang="ts">
import {computed, reactive, watch} from "vue";
import type {Job, JobQueueDetailed} from "@/api/JobQueue";
import {useRoute, useRouter} from "vue-router";
import {getAuthorizationHeader} from "@/api/Auth";
import ShortenedText from "@/components/ShortenedText.vue";
import {getBaseURL} from "@/api/BaseURL";
import {i18n} from "@/api/I18N";
import Pagination from "@/components/Pagination.vue";
import JobSearch from "@/components/JobSearch.vue";

const route = useRoute();
const router = useRouter();

const baseUrl = getBaseURL();

const model = reactive({
  status: 'loading' as 'loading' | 'error' | 'loaded',
  errorMessage: null as string | null,
  queues: null as JobQueueDetailed | null,
  count: null as number | null,
  i18n: {} as any
});

[
  "jobqueue.job.jobId",
  "jobqueue.job.status",
  "jobqueue.job.status.ALL",
  "jobqueue.job.status.NEW",
  "jobqueue.job.status.PROCESSING",
  "jobqueue.job.status.FINISHED",
  "jobqueue.job.status.ERROR",
  "jobqueue.job.status.MAX_TRIES",
  "jobqueue.job.date.added",
  "jobqueue.job.date.start",
  "jobqueue.job.date.finished",
  "jobqueue.job.exception",
  "jobqueue.job.tries",
  "jobqueue.job.parameter"
].forEach((key) => {
  let keyInObject = key.split(".").join("_");
  model.i18n[keyInObject] = "%" + keyInObject + "%";
  i18n(key).then((value) => {
    model.i18n[keyInObject] = value;
  });
});

const navigateToPage = (pageNumber: number) => {
  router.push({
    name: 'JobQueueViewStatus',
    params: {
      queueId: route.params.queueId,
      stats: route.params.stats
    },
    query: {
      offset: calculateOffset(pageNumber),
      parameter: route.query.parameter
    }
  });
};

const convertParameter = (str: string) => {
  const [name, value] = str.split(":", 2);
  return {name, value};
}

const parameter = computed(() => {
  if (route.query.parameter == null) {
    return undefined;
  } else {
    if (typeof route.query.parameter === "string") {
      return [convertParameter(route.query.parameter)]
    } else {
      return (route.query.parameter as Array<string>).map(convertParameter);
    }
  }
});


const executeSearch = (search: { parameter: Array<{ name: string, value: string }> }) => {
  const query = {
    offset: 0
  } as any;

  let parameters = (search.parameter || []).filter((param) => param.name != null && param.name != "")
      .filter((param) => param.value != null && param.value != "")
      .map((param) => param.name + ":" + param.value);

  if (parameters.length > 0) {
    query.parameter = parameters;
  }

  router.push({
    name: 'JobQueueViewStatus',
    params: {
      queueId: route.params.queueId,
      stats: route.params.stats
    },
    query
  });
};

const getDate = (job: Job, type: string) => job.date
    .filter((date) => date.type == type)
    .map((date) => new Date(date.value).toLocaleString())[0];

const getParameter = (job: Job) => job.parameter
    .map((parameter) => parameter.name + ":" + parameter.value)
    .join("\n ");

const pageCount = computed(() => {
  if (model.count == null) {
    return 0;
  } else {
    return Math.ceil(model.count / 50);
  }
});

const currentPage = computed(() => {
  if (route.query.offset == null) {
    console.log("offset is null");
    return 1;
  } else {
    return Math.ceil(parseInt(route.query.offset as string) / 50) + 1;
  }
});

const calculateOffset = (page: number) => {
  return (page - 1) * 50;
};


const statusParam = computed(() => {
  if (route.params.stats == 'ALL') {
    return "";
  } else {
    return `status=${route.params.stats}`;
  }
});

const retrieveJobs = async () => {
  model.status = 'loading';
  let offset = typeof route.query.offset == "string" ? `offset=${route.query.offset}` : "";

  let searchParams = parameter.value == null ? [] as Array<{ name: string, value: string }> : parameter.value
      .filter((parameter) => parameter.name != null && parameter.name.length > 0 && parameter.value != null && parameter.value.length > 0)
      .map((parameter) => `parameter=${parameter.name}:${parameter.value}`)
      .join("&");
  let params = [statusParam.value, "limit=50", searchParams, offset].filter((param) => param.length > 0).join("&");
  try {
    const response = await fetch(`${baseUrl}api/jobqueue/${route.params.queueId}?${params}`, {
      headers: {
        authorization: await getAuthorizationHeader()
      }
    });
    if (response.ok) {
      model.count = parseInt(response.headers.get("X-Total-Count") as string);
      model.queues = await response.json();
      model.status = 'loaded';

    } else {
      model.status = 'error';
      if (response.status === 404) {
        await router.replace({
          name: '404'
        });
      }
    }
  } catch (e) {
    model.errorMessage = (e as any).toString()
    model.status = 'error';
  }
}

retrieveJobs();

watch(() => [route.params.stats, route.query.offset], retrieveJobs);

</script>

<style scoped>


</style>
