<script lang="ts">
import { defineComponent } from "vue";
import TopNavigationBar from "@/components/TopNavigationBar.vue";

export default defineComponent({
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
