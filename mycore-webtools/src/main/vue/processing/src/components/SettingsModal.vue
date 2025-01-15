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

<script setup lang="ts">
import {Settings} from "../common/settings.ts";
import {ref} from "vue";
// Props

const emit = defineEmits(['close']);

// Reactive state for the settings value
const maxNumberFinished = ref(parseInt(Settings.get('maxNumberFinished', 50), 10));

// Watch for changes and update localStorage
function updateSetting() {
  Settings.set('maxNumberFinished', maxNumberFinished.value);
}

</script>

<template>
  <div class="modal-backdrop" @click.self="emit('close')">
    <div class="processing-modal">
      <div class="processing-modal-header">
        <h4 class="modal-title">Settings</h4>
        <button @click="emit('close')">&times;</button>
      </div>
      <div class="processing-modal-body">
        <div>
          <div>maximum number of finished processes to display (-1 for unlimited)</div>
          <input
              type="number"
              v-model="maxNumberFinished"
              @input="updateSetting" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.processing-modal-body div {
  font-size: 1rem;
  font-weight: bold;
  margin-bottom: 8px;
}

.processing-modal-body input {
  width: 100%;
  padding: 10px;
  font-size: 1rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box;
}

.processing-modal-body input:focus {
  border-color: #ff6347;
  outline: none;
  box-shadow: 0 0 5px #e5533d;
}
</style>
