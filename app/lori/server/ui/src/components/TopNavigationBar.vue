<script lang="ts">
import { defineComponent } from "vue";
import { useHistoryStore } from "@/stores/history";
import { useDialogsStore } from "@/stores/dialogs";

export default defineComponent({
  setup() {
    const historyStore = useHistoryStore();
    const menuTopics = [{ title: "IP-Gruppen" }, { title: "Einstellungen" }];
    const dialogStore = useDialogsStore();
    const activateGroupDialog = () => {
      dialogStore.groupOverviewActivated = true;
    };
    return {
      dialogStore,
      historyStore,
      menuTopics,
      activateGroupDialog,
    };
  },
});
</script>

<style scoped></style>
<template>
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

    <v-menu bottom left :offset-y="true">
      <template v-slot:activator="{ on, attrs }">
        <v-btn dark icon v-bind="attrs" v-on="on">
          <v-icon>mdi-view-headline</v-icon>
        </v-btn>
      </template>

      <v-list>
        <v-list-item link>
          <v-list-item-title @click="activateGroupDialog"
            >IP-Gruppen</v-list-item-title
          >
        </v-list-item>
        <v-list-item link>
          <v-list-item-title>Einstellungen</v-list-item-title>
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
        <v-list-item v-for="(item, index) in historyStore.history" :key="index">
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
</template>
