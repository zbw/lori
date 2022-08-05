<script lang="ts">
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import { RightRest } from "@/generated-sources/openapi";
import { computed, defineComponent, PropType, ref } from "vue";

export default defineComponent({
  props: {
    rights: {
      type: {} as PropType<Array<RightRest>>,
      required: true,
    },
    metadataId: {
      type: String,
      required: true,
    },
  },
  emits: ["deleteSuccessful", "tabDialogClosed", "updateSuccessful"],
  components: {
    RightsEditDialog,
  },

  setup(props, { emit }) {
    const renderKey = ref(0);
    const tab = ref(null);
    const lastDeletedRight = ref("");
    const lastDeletionSuccessful = ref(false);
    const lastUpdatedRight = ref("");
    const lastUpdateSuccessful = ref(false);

    // Methods
    const deleteSuccessful = (
      index: number,
      rightIdDeleted: string | undefined
    ) => {
      currentRights.value.splice(index, 1);
      renderKey.value += 1;
      lastDeletionSuccessful.value = true;
      lastDeletedRight.value =
        rightIdDeleted != undefined ? rightIdDeleted : "";
      emit("deleteSuccessful", index);
    };

    const resetLastDeletionSuccessful = () => {
      lastDeletionSuccessful.value = false;
    };

    const tabDialogClosed = () => {
      emit("tabDialogClosed");
    };

    const updateSuccessful = (right: RightRest, index: number) => {
      lastUpdateSuccessful.value = true;
      lastUpdatedRight.value = right.rightId != undefined ? right.rightId : "";
      emit("updateSuccessful", right, index);
    };

    const resetLastUpdateSuccessful = () => {
      lastUpdateSuccessful.value = false;
    };

    // Computed properties
    const tabNames = computed(() => {
      return props.rights.map((r) => r.rightId);
    });

    const currentRights = computed(() => {
      return props.rights;
    });

    return {
      currentRights,
      lastDeletedRight,
      lastDeletionSuccessful,
      lastUpdatedRight,
      lastUpdateSuccessful,
      renderKey,
      tab,
      tabNames,
      deleteSuccessful,
      resetLastDeletionSuccessful,
      resetLastUpdateSuccessful,
      tabDialogClosed,
      updateSuccessful,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-toolbar color="cyan" dark flat :key="renderKey">
      <v-toolbar-title> Editiere Rechte für {{ metadataId }} </v-toolbar-title>
      <v-spacer></v-spacer>
      <template v-slot:extension>
        <v-tabs v-model="tab" align-with-title show-arrows>
          <v-tabs-slider color="yellow"></v-tabs-slider>

          <v-tab v-for="name in tabNames" :key="name">
            {{ name }}
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-tabs-items v-model="tab">
      <v-alert
        @close="resetLastUpdateSuccessful"
        v-model="lastUpdateSuccessful"
        dismissible
        text
        type="success"
      >
        Rechteinformation {{ lastUpdatedRight }} erfolgreich geupdated für Item
        {{ metadataId }}.
      </v-alert>
      <v-alert
        @close="resetLastDeletionSuccessful"
        v-model="lastDeletionSuccessful"
        dismissible
        text
        type="success"
      >
        Rechteinformation {{ lastDeletedRight }} erfolgreich gelöscht für Item
        {{ metadataId }}.
      </v-alert>
      <v-tab-item v-for="(item, index) in currentRights" :key="item.rightId">
        <RightsEditDialog
          :activated="true"
          :right="item"
          :index="index"
          :isNew="false"
          :metadataId="metadataId"
          v-on:deleteSuccessful="deleteSuccessful"
          v-on:editDialogClosed="tabDialogClosed"
          v-on:updateSuccessful="updateSuccessful"
        ></RightsEditDialog>
      </v-tab-item>
    </v-tabs-items>
  </v-card>
</template>
