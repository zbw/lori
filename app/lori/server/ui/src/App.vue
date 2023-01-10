<script lang="ts">
import Vue from "vue";

import TopNavigationBar from "@/components/TopNavigationBar.vue";

export default Vue.extend({
  name: "LoriApp",
  components: { TopNavigationBar },
});
</script>

<template>
  <v-app>
    <TopNavigationBar></TopNavigationBar>
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
