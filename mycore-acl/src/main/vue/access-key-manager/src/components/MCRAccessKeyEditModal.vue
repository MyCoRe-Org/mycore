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
        :variant="alertVariant"
        style="text-align: center;"
      >{{ alertMessage }}</b-alert>
      <b-form-group
        label-cols-lg="2"
        label-for="input-secret"
        :label="$t('mcr.accessKey.label.secret')"
        v-if="isEdit"
      >
        <b-form-input
          id="input-secret"
          v-model="secret"
          readonly
        ></b-form-input>
      </b-form-group>
      <b-form-group
        label-cols-lg="2"
        v-else
      >
        <template #label>
          {{ $t("mcr.accessKey.label.secret") }}*
          <b-link
            id="popover-secret"
          >
            <font-awesome-icon
              icon="info-circle"
              class="text-secondary"
            ></font-awesome-icon>
          </b-link>
          <b-popover
            target="popover-secret"
            :title="$t('mcr.accessKey.title.popover')"
            triggers="hover"
          >
            <span
              v-html="$t('mcr.accessKey.popover.secret')"
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
            id="input-secret"
            v-model="secret"
            :state="inputState"
            aria-describedby="input-secret-feedback"
          ></b-form-input>
          <b-form-invalid-feedback
            id="input-secret-feedback"
          >
            {{ inputValueFeedback }}
          </b-form-invalid-feedback>
        </b-input-group>
      </b-form-group>
      <div
        v-if="isEdit"
      >
        <b-form-group
          label-cols-lg="2"
        >
          <template #label>
            {{ $t("mcr.accessKey.label.state") }}
            <b-link
              id="popover-enabled"
            >
              <font-awesome-icon
                icon="info-circle"
                class="text-secondary"
              ></font-awesome-icon>
            </b-link>
            <b-popover
              target="popover-enabled"
              :title="$t('mcr.accessKey.title.popover')"
              triggers="hover"
            >
              <span
                v-html="$t('mcr.accessKey.popover.enabled')"
              ></span>
            </b-popover>
          </template>
          <b-form-checkbox
            id="enabled-checkbox"
            v-model="isActive"
            switch
            @change="stateChanged"
          >
            {{ (isActive == true) ?
              $t("mcr.accessKey.label.state.enabled") : $t("mcr.accessKey.label.state.disabled") }}
          </b-form-checkbox>
        </b-form-group>
        <hr class="my-3">
      </div>
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
      >
        <template #label>
          {{ $t("mcr.accessKey.label.expiration") }}
          <b-link
            id="popover-expiration"
          >
            <font-awesome-icon
              icon="info-circle"
              class="text-secondary"
            ></font-awesome-icon>
          </b-link>
          <b-popover
            target="popover-expiration"
            :title="$t('mcr.accessKey.title.popover')"
            triggers="hover"
          >
            <span
              v-html="$t('mcr.accessKey.popover.expiration')"
            ></span>
          </b-popover>
        </template>
        <b-form-datepicker
          v-model="expiration"
          :locale="locale"
          :value-as-date=true
          :reset-button=true
          placeholder="-"
        ></b-form-datepicker>
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
        v-if="isEdit"
      >
        <hr class="my-3">
        <b-form-group
          label-cols-lg="2"
          :label="$t('mcr.accessKey.label.createdBy')"
          label-for="input-created-by"
        >
          <b-form-input
            id="input-created-by"
            v-model="createdBy"
            readonly
          ></b-form-input>
        </b-form-group>
        <b-form-group
          label-cols-lg="2"
          label-for="input-created"
          :label="$t('mcr.accessKey.label.created')"
        >
          <b-form-input
            id="input-created"
            v-model="created"
            readonly
          ></b-form-input>
        </b-form-group>
        <b-form-group
          label-cols-lg="2"
          label-for="input-last-modfied-by"
          :label="$t('mcr.accessKey.label.lastModifiedBy')"
        >
          <b-form-input
            id="input-last-modified-by"
            v-model="lastModifiedBy"
            readonly
          ></b-form-input>
        </b-form-group>
        <b-form-group
          label-cols-lg="2"
          label-for="input-last-modified"
          :label="$t('mcr.accessKey.label.lastModified')"
        >
          <b-form-input
            id="input-last-modified"
            v-model="lastModified"
            readonly
          ></b-form-input>
        </b-form-group>
      </div>
      <template #modal-footer>
        <b-button
          v-if="isEdit"
          v-on:click="remove()"
          variant="danger"
        >
          <font-awesome-icon
            icon="trash"
          ></font-awesome-icon>
          {{ $t("mcr.accessKey.button.remove") }}
        </b-button>
        <b-button
          v-if="isEdit"
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
          {{ $t("mcr.accessKey.button.new") }}
        </b-button>
      </template>
    </b-modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Prop } from 'vue-property-decorator';
import MCRAccessKey from '@/common/MCRAccessKey';
import MCRException from '@/common/MCRException';
import { generateRandomString } from '@/common/MCRUtils';

@Component
export default class Modal extends Vue {
  @Prop({ default: 'en' })
  locale!: string;

  private options = [
    { value: 'read', text: this.$t('mcr.accessKey.label.type.read') },
    { value: 'writedb', text: this.$t('mcr.accessKey.label.type.writedb') },
  ];

  private isEdit = false;

  private secret = '';

  private isActive = true;

  private expiration: Date;

  private comment = '';

  private selected = 'read';

  private createdBy = '';

  private created = '';

  private lastModifiedBy = '';

  private lastModified = '';

  private alertMessage = '';

  private isProcessing = true;

  private inputValueFeedback = '';

  private alertVariant = 'danger';

  private get showAlert(): boolean {
    return this.alertMessage.length > 0;
  }

  private get inputState(): boolean {
    return this.inputValueFeedback == null ? null : false;
  }

  private get title(): string {
    if (this.isEdit) {
      return this.$t('mcr.accessKey.title.edit');
    }
    return this.$t('mcr.accessKey.title.add');
  }

  private async updateAccessKey(secret: string, accessKey: MCRAccessKey): Promise<void> {
    try {
      await this.$client.updateAccessKey(secret, accessKey);
      this.alertVariant = 'success';
      this.alertMessage = this.$t('mcr.accessKey.success.update');
      const result: MCRAccessKey = await this.$client.getAccessKey(secret);
      this.setAccessKey(result);
      this.$emit('update', result);
    } catch (error) {
      this.handleException(error);
    }
  }

  private async stateChanged(): Promise<void> {
    this.isProcessing = true;
    const accessKey: MCRAccessKey = {
      isActive: this.isActive,
    };
    await this.updateAccessKey(this.secret, accessKey);
    this.isProcessing = false;
  }

  private generate(): void {
    this.secret = generateRandomString(32);
  }

  private setAccessKey(accessKey: MCRAccessKey): void {
    this.secret = accessKey.secret;
    this.selected = accessKey.type;
    if (accessKey.expiration != null) {
      this.expiration = new Date(accessKey.expiration);
    }
    this.isActive = accessKey.isActive;
    this.comment = accessKey.comment;
    if (accessKey.createdBy != null) {
      this.createdBy = accessKey.createdBy;
    } else {
      this.createdBy = '-';
    }
    if (accessKey.created != null) {
      this.created = new Date(accessKey.created).toLocaleString();
    } else {
      this.created = '-';
    }
    if (accessKey.lastModifiedBy != null) {
      this.lastModifiedBy = accessKey.lastModifiedBy;
    } else {
      this.lastModifiedBy = '-';
    }
    if (accessKey.lastModified != null) {
      this.lastModified = new Date(accessKey.lastModified).toLocaleString();
    } else {
      this.lastModified = '-';
    }
  }

  public show(accessKey: MCRAccessKey): void {
    this.isProcessing = true;
    if (accessKey == null) {
      this.isEdit = false;
      this.secret = '';
      this.selected = 'read';
      this.expiration = null;
      this.comment = '';
      this.inputValueFeedback = null;
    } else {
      this.isEdit = true;
      this.setAccessKey(accessKey);
    }
    this.alertMessage = '';
    this.$bvModal.show('modal-center');
    this.isProcessing = false;
  }

  public close(): void {
    this.$bvModal.hide('modal-center');
  }

  private async add(): Promise<void> {
    this.isProcessing = true;
    // if (this.secret.length === 0 || this.secret.includes('/') || this.secret.includes('\\')) {
    if (this.secret.length === 0) {
      this.inputValueFeedback = this.$t('mcr.accessKey.error.invalidSecret');
      this.isProcessing = false;
      return;
    }
    this.inputValueFeedback = null;

    const accessKey: MCRAccessKey = {
      secret: this.secret,
      type: this.selected,
    };
    if (this.expiration != null) {
      accessKey.expiration = this.expiration;
    }
    if (this.comment != null) {
      accessKey.comment = this.comment;
    }
    try {
      const secret: string = await this.$client.addAccessKey(accessKey);
      const result: MCRAccessKey = await this.$client.getAccessKey(secret);
      this.$emit('add', result, accessKey.secret);
      this.close();
    } catch (error) {
      this.handleException(error);
    }
  }

  private async update(): Promise<void> {
    this.isProcessing = true;
    const accessKey: MCRAccessKey = {
      type: this.selected,
    };
    if (this.expiration != null) {
      accessKey.expiration = this.expiration;
    }
    if (this.comment != null) {
      accessKey.comment = this.comment;
    }
    await this.updateAccessKey(this.secret, accessKey);
    this.isProcessing = false;
  }

  private async remove(): Promise<void> {
    const sure: boolean = await this.$bvModal.msgBoxConfirm(this.$t('mcr.accessKey.text.remove'), {
      title: this.$t('mcr.accessKey.title.modal'),
      size: 'sm',
      noCloseOnBackdrop: true,
      noCloseOnEsc: true,
      hideBackdrop: true,
      okVariant: 'danger',
      okTitle: this.$t('mcr.accessKey.button.yes'),
      cancelTitle: this.$t('mcr.accessKey.button.no'),
    });
    if (sure) {
      this.isProcessing = true;
      try {
        await this.$client.removeAccessKey(this.secret);
        this.$emit('remove', this.secret);
        this.close();
      } catch (error) {
        this.handleException(error);
      }
    }
  }

  private async handleException(e: MCRException): Promise<void> {
    const { errorCode } = e;
    if (errorCode != null) {
      if (errorCode.endsWith('collision')) {
        this.inputValueFeedback = this.$t('mcr.accessKey.error.collision');
      } else if (errorCode.endsWith('unknownKey') || errorCode.endsWith('unknownObject')) {
        this.$emit('inconsistency');
        this.close();
      } else {
        // eslint-disable-next-line no-console
        console.error('unknown error code: %s', errorCode);
        this.$emit('inconsistency');
        this.close();
      }
    } else {
      // eslint-disable-next-line no-console
      console.error(e);
      this.$emit('inconsistency');
      this.close();
    }
    this.isProcessing = false;
  }
}
</script>
