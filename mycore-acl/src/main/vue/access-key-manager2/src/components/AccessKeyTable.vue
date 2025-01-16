<template>
  <table class="table">
    <colgroup>
      <col style="width: 30%" />
      <col style="width: 35%" />
      <col style="width: 10%" />
      <col style="width: 10%" />
      <col style="width: 10%" />
      <col style="width: 5%" />
    </colgroup>
    <thead>
      <tr>
        <th>{{ $t("component.acl.accesskey.frontend.label.id") }}</th>
        <th>{{ $t("component.acl.accesskey.frontend.label.reference") }}</th>
        <th>{{ $t("component.acl.accesskey.frontend.label.permission") }}</th>
        <th>{{ $t("component.acl.accesskey.frontend.label.active") }}</th>
        <th>{{ $t("component.acl.accesskey.frontend.label.expiration") }}</th>
        <th>{{ $t("component.acl.accesskey.frontend.label.actions") }}</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="(accessKey, index) in accessKeys" :key="index">
        <td>{{ accessKey.id }}</td>
        <td>{{ accessKey.reference }}</td>
        <td>
          {{ $t(`component.acl.accesskey.frontend.label.permission.${accessKey.permission}`) }}
        </td>
        <td>{{ accessKey.isActive }}</td>
        <td>
          {{ accessKey.expiration ? new Date(accessKey.expiration).toLocaleDateString() : "-" }}
        </td>
        <td>
          <div class="btn-group">
            <button class="btn shadow-none" @click="viewAccessKey(index)">
              <i class="fa fa-eye" />
            </button>
            <button class="btn shadow-none" @click="removeAccessKey(index)">
              <i class="fa fa-trash" />
            </button>
          </div>
        </td>
      </tr>
    </tbody>
  </table>
</template>
<script setup lang="ts">
import { AccessKeyDto } from "@/dtos/accesskey";

defineProps<{
  accessKeys: AccessKeyDto[];
}>();
const emit = defineEmits<{
  (event: "remove-access-key", index: number): void;
  (event: "view-access-key", index: number): void;
}>();
const removeAccessKey = (index: number) => {
  emit("remove-access-key", index);
};
const viewAccessKey = (index: number) => {
  emit("view-access-key", index);
};
</script>
