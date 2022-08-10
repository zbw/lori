<script lang="ts">
import Vue from "vue";
import { useHistoryStore } from "@/stores/history";

export default Vue.extend({
  name: "LoriApp",

  setup() {
    const historyStore = useHistoryStore();
    const items = [
      { title: "Click Me" },
      { title: "Click Me" },
      { title: "Click Me" },
      { title: "Click Me 2" },
    ];

    return {
      items,
      historyStore,
    };
  },
});
</script>

<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <div class="d-flex align-center">
        <v-img
          alt="Lori Logo"
          class="shrink mr-2"
          contain
          src="@/assets/LogoLori.png"
          transition="scale-transition"
          width="100"
        />
      </div>

      <v-spacer></v-spacer>
      <v-menu offset-y>
        <template v-slot:activator="{ on, attrs }">
          <v-chip
            class="ma-2"
            color="green"
            text-color="white"
            v-bind="attrs"
            v-on="on"
          >
            <v-avatar left class="green darken-4">
              {{ historyStore.numberEntries }}
            </v-avatar>
            Änderungen
          </v-chip>
        </template>
        <v-list>
          <v-list-item
            v-for="(item, index) in historyStore.history"
            :key="index"
          >
            <v-list-item-action>
              <v-btn fab small depressed color="primary">
                {{ index }}
              </v-btn>
            </v-list-item-action>
            <v-list-item-title
              >{{ item.type.toString() }}: Right-Id
              {{ item.rightId }}</v-list-item-title
            >
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>

    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
