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
  <div>
  <b-modal 
    id="modal-center"
    body-class="position-static"
    :no-close-on-backdrop=true
    :no-close-on-esc=true
    :hide-backdrop=true
    size="lg"
    @hide="close()"
  >
    <b-overlay
      :show="isProcessing"
      variant="transparent"
      spinner-variant="primary"
      spinner-type="grow"
      rounded="sm"
      no-wrap
    ></b-overlay>
    <template #modal-header>
      <h5
        class="modal-title"
      >{{ title }}</h5>
      <b-link
        v-on:click="close()"
        style="order: 2;"
      >
        <font-awesome-icon 
          icon="times"
          size="lg"
          class="text-secondary"
        ></font-awesome-icon>
      </b-link>
    </template>
    <b-alert 
      v-model="showAlert"
      variant="danger"
      style="text-align: center;"
    >{{ alertMessage }}</b-alert>
    <b-form-group
      label-cols-lg="2"
      label-for="input-id"
      :label="$t('mcr.accessKey.label.id')"
      v-if="id != ''"
    >
      <b-form-input 
        id="input-id" 
        v-model="id"
        readonly
      ></b-form-input>
    </b-form-group>
    <b-form-group
      label-cols-lg="2"
      v-else
    >
      <template #label>
        {{ $t("mcr.accessKey.label.value") }}*
        <b-link
          id="popover-value"
        >
          <font-awesome-icon 
            icon="info-circle"
            class="text-secondary"
          ></font-awesome-icon>
        </b-link>
        <b-popover
          target="popover-value"
          :title="$t('mcr.accessKey.title.popover')"
          triggers="hover"
        >
          <span 
            v-html="$t('mcr.accessKey.popover.value')"
          ></span>
        </b-popover>
      </template>
      <b-input-group>
        <b-input-group-prepend>
          <b-button 
            v-on:click="generate()"
            variant="primary"
          >
            <font-awesome-icon 
              icon="random"
            ></font-awesome-icon>
          </b-button>
        </b-input-group-prepend>
        <b-form-input 
          id="input-value" 
          v-model="value"
          :state="inputState"
          aria-describedby="input-value-feedback"
        ></b-form-input>
        <b-form-invalid-feedback 
          id="input-value-feedback"
        >
          {{ inputValueFeedback }}
        </b-form-invalid-feedback>
      </b-input-group>
    </b-form-group>
    <b-form-group
      label-cols-lg="2"
    >
      <template #label>
        {{ $t("mcr.accessKey.label.type") }}*
        <b-link
          id="popover-type"
        >
          <font-awesome-icon 
            icon="info-circle"
            class="text-secondary"
          ></font-awesome-icon>
        </b-link>
        <b-popover
          target="popover-type"
          :title="$t('mcr.accessKey.title.popover')"
          triggers="hover"
        >
          <span 
            v-html="$t('mcr.accessKey.popover.type')"
          ></span>
        </b-popover>
      </template>
      <b-form-select 
        id="type-select"
        v-model="selected"
        :options="options"
        plain
      ></b-form-select>
    </b-form-group>
    <b-form-group
      label-cols-lg="2"
      :label="$t('mcr.accessKey.label.comment')"
      label-for="textarea-comment"
    >
      <b-form-textarea
        id="textarea-comment"
        v-model="comment"
        rows="3"
      ></b-form-textarea>
    </b-form-group>
    <div
      v-if="id != ''"
    >
      <b-form-group
        label-cols-lg="2"
        :label="$t('mcr.accessKey.label.creator')"
        label-for="input-creator"
      >
        <b-form-input 
          id="input-creator" 
          v-model="creator"
          readonly
        ></b-form-input>
      </b-form-group>
      <b-form-group
        label-cols-lg="2"
        label-for="input-creation"
        :label="$t('mcr.accessKey.label.creation')"
      >
        <b-form-input 
          id="input-creation" 
          v-model="creation"
          readonly
        ></b-form-input>
      </b-form-group>
      <b-form-group
        label-cols-lg="2"
        label-for="input-last-changer"
        :label="$t('mcr.accessKey.label.lastChanger')"
      >
        <b-form-input 
          id="input-last-changer" 
          v-model="lastChanger"
          readonly
        ></b-form-input>
      </b-form-group>
      <b-form-group
        label-cols-lg="2"
        label-for="input-last-change"
        :label="$t('mcr.accessKey.label.lastChange')"
      >
        <b-form-input 
          id="input-last-change" 
          v-model="lastChange"
          readonly
        ></b-form-input>
      </b-form-group>
    </div>
    <template #modal-footer>
      <b-button
        v-if="id != ''"
        v-on:click="remove()"
        variant="danger"
      >
        <font-awesome-icon 
          icon="trash"
        ></font-awesome-icon>
        {{ $t("mcr.accessKey.button.remove") }}
      </b-button>
      <b-button 
        v-if="id != ''"
        v-on:click="update()"
        variant="primary"
      >
        <font-awesome-icon 
          icon="save"
        ></font-awesome-icon>
        {{ $t("mcr.accessKey.button.update") }}
      </b-button>
      <b-button 
        v-else
        v-on:click="add()"
        variant="primary"
      >
        <font-awesome-icon 
          icon="plus"
        ></font-awesome-icon>
      </b-button>
    </template>
  </b-modal>
</div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator';
  import MCRAccessKey from '@/common/MCRAccessKey';
  import MCRException from '@/common/MCRException';
  import { generateRandomString } from '@/common/MCRUtils';

  @Component
  export default class Modal extends Vue {
    private options = [
      { value: "read", text: this.$t("mcr.accessKey.label.type.read") },
      { value: "writedb", text: this.$t("mcr.accessKey.label.type.writedb") }
    ];
    private value = "";
    private id = "";
    private comment = "";
    private selected = "read";
    private creator = "";
    private creation = "";
    private lastChanger = "";
    private lastChange = "";
    private alertMessage = "";
    private isProcessing = true;
    private inputValueFeedback = "";

    private get showAlert(): boolean {
      return this.alertMessage.length > 0;
    }

    private get inputState(): boolean {
      return this.inputValueFeedback == null ? null : false;
    }

    private get title(): string {
      if (this.id == "") {
        return this.$t("mcr.accessKey.title.add");
      } else {
        return this.$t("mcr.accessKey.title.edit");
      }
    }

    private generate(): void {
      this.value = generateRandomString(32);
    }

    public show(accessKey: MCRAccessKey): void {
      this.isProcessing = true;
      if (accessKey == null) {
        this.id = "";
        this.value = "";
        this.selected = "read";
        this.comment = "";
        this.inputValueFeedback = null;
      } else {
        this.id = accessKey.value;
        this.selected = accessKey.type;
        this.comment = accessKey.comment;
        if (accessKey.creator != null) {
          this.creator = accessKey.creator;
        } else {
          this.creator = "-";
        }
        if (accessKey.creation != null) {
          this.creation = new Date(accessKey.creation).toLocaleString();
        } else {
          this.creation = "-";
        }
        if (accessKey.lastChanger != null) {
          this.lastChanger = accessKey.lastChanger;
        } else {
          this.lastChanger = "-";
        }
        if (accessKey.lastChange != null) {
          this.lastChange = new Date(accessKey.lastChange).toLocaleString();
        } else {
         this.lastChange = "-";
        }
      }
      this.alertMessage = "";
      this.$bvModal.show("modal-center");
      this.isProcessing = false;
    }

    public close(): void {
      this.$bvModal.hide("modal-center");
    }

    private async add(): Promise<void> {
      this.isProcessing = true;
      if (this.value.length == 0) {
        this.inputValueFeedback = this.$t("mcr.accessKey.error.invalidValue");
        this.isProcessing = false;
        return;
      } else {
        this.inputValueFeedback = null;
      }
      const accessKey: MCRAccessKey = {
        value: this.value,
        type: this.selected,
        comment: this.comment,
      }
      try {
        const id: string= await this.$client.addAccessKey(accessKey);
        const result: MCRAccessKey = await this.$client.getAccessKey(id);
        this.$emit("add", result, accessKey.value);
        this.close();
      } catch (error) {
        this.handleException(error);  
      }
    }
    
    private async update(): Promise<void> {
      this.isProcessing = true;
      const accessKey: MCRAccessKey = {
        value: this.id,
        type: this.selected,
        comment: this.comment,
      }
      try {
        await this.$client.updateAccessKey(accessKey);
        const result: MCRAccessKey = await this.$client.getAccessKey(accessKey.value);
        this.$emit("update", result);
        this.close();
      } catch(error) {
        this.handleException(error);  
      }
    }

    private async remove(): Promise<void> {
      const sure: boolean = await this.$bvModal.msgBoxConfirm(this.$t("mcr.accessKey.text.remove"), {
        title: this.$t("mcr.accessKey.title.modal"),
        size: 'sm',
        noCloseOnBackdrop: true,
        noCloseOnEsc: true,
        hideBackdrop: true,
        okVariant: 'danger',
        okTitle: this.$t("mcr.accessKey.button.yes"),
        cancelTitle: this.$t("mcr.accessKey.button.no"),
      });
      if (sure) { 
        this.isProcessing = true;
        try {
          await this.$client.removeAccessKey(this.id);
          this.$emit("remove", this.id);
          this.close();
        } catch(error) {
          this.handleException(error);  
        }
      }
    }

    private async handleException(e: MCRException): Promise<void> {
      const errorCode: string = e.errorCode;
      if (errorCode != null) {
        if (errorCode.endsWith("collision")) {
          this.inputValueFeedback = this.$t("mcr.accessKey.text.error.collision");
        } else if (errorCode.endsWith("unknownKey") || errorCode.endsWith("unknownObject")){
          this.$emit("inconsistency");
          this.close();
        } else {
          // eslint-disable-next-line no-console
          console.error("unknown error code: %s", errorCode);
          this.$emit("inconsistency");
          this.close();
        }
      } else {
        // eslint-disable-next-line no-console
        console.error(e);
        this.$emit("inconsistency");
        this.close();
      }
      this.isProcessing = false;
    }
  }
</script>
