<script lang="ts">
import Vue from "vue";

import TopNavigationBar from "@/components/TopNavigationBar.vue";
import VueMaterial from "vue-material";
Vue.use(VueMaterial);
import VueResource from "vue-resource";
Vue.use(VueResource);

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
