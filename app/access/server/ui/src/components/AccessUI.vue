<template>
  <v-app>
    <v-toolbar class="cyan darken-4" dark>
      <v-toolbar-title>AccessUI</v-toolbar-title>
    </v-toolbar>
    <v-main>
      <v-data-table
        v-model="selected"
        :headers="headers"
        :items="items"
        class="elevation-1"
        item-key="id"
        show-select
      >
      </v-data-table>
    </v-main>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "../../node_modules/vue-class-component/lib";
import { AccessInformation } from "@/generated-sources/openapi";
import api from "@/api/api";

@Component
export default class AccessUi extends Vue {
  private headers = [
    {
      text: "Id",
      align: "start",
      value: "id",
    },
    { text: "Tenant", value: "tenant" },
    { text: "Usage guide", value: "usageGuide" },
    { text: "Template", value: "template" },
    { text: "Mention", value: "mention" },
    { text: "Sharealike", value: "sharealike" },
    { text: "Commercial Use", value: "commercial" },
    { text: "Copyright", value: "copyright" },
    { text: "Actions", value: "actions" },
  ];
  private items: Array<AccessInformation> = [];

  public fetchListPart(offset: number, limit: number): void {
    api.getList(offset, limit).then((response) => (this.items = response));
  }
  mounted(): void {
    this.fetchListPart(0, 25);
  }
}
</script>
