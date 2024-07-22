<script lang="ts">
import { defineComponent } from "vue";
import TopNavigationBar from "@/components/TopNavigationBar.vue";

export default defineComponent({
  components: { TopNavigationBar },
});
</script>

<template>
  <v-app>
    <v-layout class="rounded rounded-md">
      <TopNavigationBar></TopNavigationBar>
      <router-view />
    </v-layout>
  </v-app>
</template>
