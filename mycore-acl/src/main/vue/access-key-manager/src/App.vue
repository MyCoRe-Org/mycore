<!--
 This file is part of ***  M y C o R e  ***
 See http://www.mycore.de/ for details.
 
 MyCoRe is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 MyCoRe is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
-->

<template>
  <div id="app">
    <b-container
      fluid
    >
      <b-row
        class="pb-2"
      >
        <b-col
          cols="1"
          class="d-flex justify-content-start"
          style="align-items: center;"
        >
          <b-link
            v-on:click="goBack()"
          >
            <font-awesome-icon 
              icon="angle-left"
              size="2x"
             ></font-awesome-icon>
          </b-link>
        </b-col>
        <b-col
          cols="11"
          class="d-flex justify-content-center"
        >
          <h3
          >
            {{ $t("mcr.accessKey.title.main") }}
          </h3>
        </b-col>
      </b-row>
      <b-overlay
        :show="isProcessing"
        variant="transparent"
        spinner-variant="primary"
        spinner-type="grow"
        rounded="sm"
      >
        <b-alert 
          v-model="showAlert"
          :variant="alertVariant"
          style="text-align: center;"
        >
          <h5
            v-if="alertVariant == 'danger' || alertVariant == 'warning'"
          >       
            {{ $t("mcr.accessKey.title.alert") }}
          </h5>
          <span 
            v-html="alertMessage"
          ></span>
        </b-alert>
        <div
          v-if="isAuthorized"  
        >
          <b-table
            :fields="fields" 
            :items="accessKeys"
            :per-page="perPage"
            :current-page="currentPage"
            sort-icon-left
            responsive
            striped
          >
            <template #head(id)>
              {{ $t("mcr.accessKey.label.id") }}
            </template>
            <template #head(enabled)>
              {{ $t("mcr.accessKey.label.state") }}
            </template>
            <template #head(type)>
              {{ $t("mcr.accessKey.label.type") }}
            </template>
            <template #head(expiration)>
              {{ $t("mcr.accessKey.label.expiration") }}
            </template>
            <template #head(comment)>
              {{ $t("mcr.accessKey.label.comment") }}
            </template>
            <template #table-caption>
              <div 
                class="text-right"
              >
                <b-button
                  v-on:click="openModal()"
                  variant="primary"
                >
                  <font-awesome-icon 
                    icon="plus"
                  ></font-awesome-icon>
                  {{ $t("mcr.accessKey.button.add") }}
                </b-button>
              </div>
            </template>
            <template #cell(id)="data">
              {{ data.item.secret.substring(0,8) }}
            </template>
            <template #cell(state)="data">
              {{ (data.item.isActive == true) ? $t("mcr.accessKey.label.state.enabled") : $t("mcr.accessKey.label.state.disabled") }}
            </template>
            <template #cell(type)="data">
              {{ $t("mcr.accessKey.label.type." + data.item.type) }}
            </template>
            <template #cell(expiration)="data">
              {{ (data.item.expiration != null) ? new Date(data.item.expiration).toLocaleDateString() : "-" }}
            </template>
            <template #cell(comment)="data">
              {{ ((data.item.comment != null) && (data.item.comment.length > 50)) ? data.item.comment.substring(0, 50) + "..." : data.item.comment }}
            </template>
            <template #cell(edit)="data">
              <b-link
                v-on:click="openModal(data.index)"
              >
                <font-awesome-icon 
                  icon="edit"
                  class="text-primary"
                ></font-awesome-icon>
              </b-link>
            </template>
          </b-table>
          <b-pagination
            v-model="currentPage"
            :total-rows="rows"
            :per-page="perPage"
            align="center"
            class="pt-1"
          ></b-pagination>
          <MCRAccessKeyEditModal
            ref="new" 
            v-bind:locale="locale"
            v-on:add="addAccessKey"
            v-on:update="updateAccessKey"
            v-on:remove="removeAccessKey"
            v-on:inconsistency="showFatalError"
          ></MCRAccessKeyEditModal>
        </div>
      </b-overlay>
    </b-container>
  </div>
</template>
<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator';
  import MCRAccessKey from '@/common/MCRAccessKey';
  import { ButtonPlugin, PaginationPlugin, TablePlugin, LayoutPlugin, AlertPlugin, OverlayPlugin, ModalPlugin, FormInputPlugin, FormSelectPlugin, InputGroupPlugin, FormGroupPlugin, LinkPlugin, PopoverPlugin, FormTextareaPlugin, FormPlugin, FormCheckboxPlugin, FormDatepickerPlugin } from 'bootstrap-vue'
  import 'bootstrap-vue/dist/bootstrap-vue.css'
  import MCRAccessKeyServicePlugin, { MCRAccessKeyInformation } from '@/plugins/MCRAccessKeyServicePlugin';
  import MCRLocalePlugin from '@/plugins/MCRLocalePlugin';
  import MCRException from '@/common/MCRException';
  import MCRAccessKeyEditModal from './components/MCRAccessKeyEditModal.vue';
  import { library } from '@fortawesome/fontawesome-svg-core'
  import { faEdit, faTimes, faPlus, faInfoCircle, faTrash, faSave, faRandom, faAngleLeft } from '@fortawesome/free-solid-svg-icons'
  import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
  import { webApplicationBaseURL, objectID, parentID, locale, fetchDict, fetchJWT, isSessionEnabled, urlEncode, isDerivate } from '@/common/MCRUtils';
  import dict from '@/common/i18n/MCRAccessKeyI18n';

  library.add(faEdit, faTimes, faPlus, faInfoCircle, faTrash, faSave, faRandom, faAngleLeft);

  Vue.use(ButtonPlugin)
  Vue.use(PaginationPlugin)
  Vue.use(TablePlugin)
  Vue.use(LayoutPlugin)
  Vue.use(AlertPlugin)
  Vue.use(OverlayPlugin)
  Vue.use(ModalPlugin)
  Vue.use(FormInputPlugin)
  Vue.use(FormSelectPlugin)
  Vue.use(InputGroupPlugin)
  Vue.use(FormGroupPlugin)
  Vue.use(FormCheckboxPlugin)
  Vue.use(FormDatepickerPlugin)
  Vue.use(LinkPlugin)
  Vue.use(FormPlugin)
  Vue.use(PopoverPlugin)
  Vue.use(FormTextareaPlugin)

  Vue.use(MCRLocalePlugin, dict);

  Vue.component('font-awesome-icon', FontAwesomeIcon); //global

  @Component({
    components: {
      MCRAccessKeyEditModal,
    },
  })
  export default class AccessKeyManager extends Vue {
    private locale: string;
    private fields = [
      {
        key: "id",
        thClass: 'col-1 text-center',
        tdClass: 'col-1 text-center',
      },
      {
        key: "state",
        thClass: 'col-1 text-center',
        tdClass: 'col-1 text-center',
        sortable: true
      },
      {
        key: "type",
        thClass: 'col-2 text-center',
        tdClass: 'col-2 text-center',
        sortable: true
      },
      {
        key: "expiration",
        thClass: 'col-2 text-center',
        tdClass: 'col-2 text-center',
        sortable: true
      },
      {
        key: "comment",
        thClass: 'col-5 text-center',
        tdClass: 'col-5 text-center',
      },
      {
        key: "edit",
        label: "",
        thClass: 'col-1 text-right',
        tdClass: 'col-1 text-right',
      },
    ];
    private perPage = 8;
    private currentPage = 1;
    private accessKeys: Array<MCRAccessKey> = [];
    private alertMessage = "";
    private alertVariant = "danger";
    private isProcessing = true;
    private isAuthorized = false;

    private openModal(localIndex: number): void {
      let accessKey: MCRAccessKey = null;
      if (localIndex != null) {
        const index = localIndex + (this.currentPage - 1) * this.perPage;
        accessKey = this.accessKeys[index];
      }
      (this.$refs.new as MCRAccessKeyEditModal).show(accessKey);
    }

    private get rows(): number {
      return this.accessKeys.length;
    }

    private get showAlert(): boolean {
      return this.alertMessage.length > 0;
    }

    private goBack(): void {
      window.history.back();
    }

    private addAccessKey(accessKey: MCRAccessKey, secret: string) {
      this.accessKeys.push(accessKey);
      this.alertVariant = "success";
      this.alertMessage = this.$t("mcr.accessKey.success.add", accessKey.secret, secret);
      if (isSessionEnabled && !isDerivate(objectID)) {
        this.alertMessage += ` ${this.$t("mcr.accessKey.success.add.url")} ` +
          this.$t("mcr.accessKey.success.add.url.format", webApplicationBaseURL, objectID, urlEncode(secret));
      }
    }

    private updateAccessKey(accessKey: MCRAccessKey) {
      const index = this.accessKeys.findIndex(item => item.secret == accessKey.secret);
      if (index >= 0) {
        this.$set(this.accessKeys, index, accessKey);
      } else {
        this.showFatalError();
      }
    }

    private removeAccessKey(secret: string) {
      const index = this.accessKeys.findIndex(item => item.secret == secret);
      if (index >= 0) {
        this.$delete(this.accessKeys, index);
        this.alertVariant = "success";
        this.alertMessage = this.$t("mcr.accessKey.success.delete", secret);
      } else {
        this.showFatalError();
      }
    }

    private showFatalError(): void {
      this.alertVariant = "danger";
      this.alertMessage = this.$t("mcr.accessKey.error.fatal");
    }

    private async updateI18n(): Promise<void> {
     try {
        const result = await fetchDict(webApplicationBaseURL, locale);
        Object.assign(dict, result.data);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error("i18n update failed.");
      }
    }

    private initializeServicePlugin(token: string): void {
      try {
        Vue.use(MCRAccessKeyServicePlugin, {
          baseURL: webApplicationBaseURL,
          objectID: objectID,
          token
        });
      } catch(error) { //should not happen
        this.showFatalError();
      }
    }
    public async created(): Promise<void> {
      this.locale = locale;
      if (webApplicationBaseURL == null) {
        this.showFatalError();
        this.isProcessing = false;
        // eslint-disable-next-line no-console
        console.error("webApplicationBaseURL is not set");
        return;
      }
      await this.updateI18n();
      if (objectID == null) {
        this.showFatalError();
        this.isProcessing = false;
        // eslint-disable-next-line no-console
        console.error("objectID is not set");
        return;
      }
      let token = "";
      try {
        const result = await fetchJWT(webApplicationBaseURL, objectID, parentID, isSessionEnabled);
        token = result.data["access_token"];
      } catch(error) {
        this.alertVariant = "danger";
        this.alertMessage = this.$t("mcr.accessKey.error.noPermission");
        this.isProcessing = false;
        // eslint-disable-next-line no-console
        console.error("couldn't fetch JWT");
        return;
      }
      this.initializeServicePlugin(token);
      try {
        const accessKeyInformation: MCRAccessKeyInformation = await this.$client.getAccessKeys();
        this.accessKeys = accessKeyInformation.items;
        this.isAuthorized = true;
      } catch (error) {
        const exception: MCRException = error as MCRException;
        const errorCode = exception.errorCode;
        if (errorCode != null) {
          if (errorCode == "request") {
            this.alertVariant = "warning";
            this.alertMessage = this.$t("mcr.accessKey.error.request");
          } else if (errorCode == "noPermission") {
            this.alertVariant = "danger";
            this.alertMessage = this.$t("mcr.accessKey.error.noPermission");
          } else {
            // eslint-disable-next-line no-console
            console.error("unknown error code: %s", errorCode);
          }
        } else {
          // eslint-disable-next-line no-console
          console.error(error);
          this.showFatalError();
        }
      }
      this.isProcessing = false;
    }
  }
</script>
