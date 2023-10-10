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
