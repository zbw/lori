<script lang="ts">
import api from "@/api/api";
import { defineComponent, onMounted, ref, Ref } from "vue";
import { GroupRest } from "@/generated-sources/openapi";

export default defineComponent({
  setup() {
    const headers = [
      {
        text: "Liste aller Gruppen",
        align: "start",
        value: "name",
      },
    ];
    const items: Ref<Array<GroupRest>> = ref([]);
    const groupLoadErrorMsg = ref("");
    const getGroupList = () => {
      api
        .getGroupList(0, 100)
        .then((r) => {
          items.value = r;
        })
        .catch((e) => {
          groupLoadErrorMsg.value =
            e.statusText + " (Statuscode: " + e.status + ")";
        });
    };

    onMounted(() => getGroupList());
    return {
      headers,
      items,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn color="blue darken-1" text>Neue IP-Gruppe anlegen </v-btn>
    </v-card-actions>
    <v-data-table
      :headers="headers"
      :items="items"
      loading-text="Daten werden geladen... Bitte warten."
      show-select
      item-key="groupName"
    ></v-data-table>
  </v-card>
</template>
