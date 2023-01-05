<script lang="ts">
import Vue, { computed, ref, watch } from "vue";
import { useHistoryStore } from "@/stores/history";

export default Vue.extend({
  name: "LoriApp",

  setup() {
    const drawer = ref(false);
    const historyStore = useHistoryStore();
    const menuTopics = [
      { title: "IP-Gruppen" },
      { title: "Einstellungen" },
    ];

    return {
      menuTopics,
      drawer,
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

      <v-menu
          bottom
          left
          :offset-y="true"
      >
        <template v-slot:activator="{ on, attrs }">
          <v-btn dark icon v-bind="attrs" v-on="on">
            <v-icon>mdi-view-headline</v-icon>
          </v-btn>
        </template>

        <v-list>
          <v-list-item v-for="(topic, i) in menuTopics" :key="i" link>
            <v-list-item-title>{{ topic.title }}</v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>

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
              {{ item.rightId }}
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>
    <v-main>
      <router-view />
    </v-main>
  </v-app>
</template>
