<template>
  <v-app>
    <v-toolbar class="cyan darken-4" dark>
      <v-toolbar-title> To Do with Hello</v-toolbar-title>
    </v-toolbar>
    <v-main>
      {{ items }}
    </v-main>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "../../node_modules/vue-class-component/lib"
import api from "../api/api";
import { AccessInformation } from "@/generated-sources/openapi";

@Component
export default class AccessUi extends Vue {
  private items: Array<AccessInformation> = [];

  public fetchListPart(offset: number, limit: number): void {
    api.getList(offset, limit).then((response) => (this.items = response));
  }
  mounted(): void {
    this.fetchListPart(0, 25);
  }
}
</script>
