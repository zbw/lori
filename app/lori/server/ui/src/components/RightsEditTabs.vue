<script lang="ts">
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import { RightRest } from "@/generated-sources/openapi";
import {computed, ComputedRef, defineComponent, onMounted, PropType, ref, watch} from "vue";

export default defineComponent({
  props: {
    rights: {
      type: Object as PropType<Array<RightRest>>,
      required: true,
    },
    selectedRight: {
      type: String,
      required: true,
    },
    handle: {
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
    const lastDeletedRight = ref("");
    const lastDeletionSuccessful = ref(false);
    const lastUpdatedRight = ref("");
    const lastUpdateSuccessful = ref(false);

    // Methods
    const deleteSuccessful = (
      index: number,
      rightIdDeleted: string | undefined,
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

    const parseDate = (d: Date | undefined) => {
      if (d === undefined) {
        return "";
      } else {
        return d.toLocaleDateString("de");
      }
    };

    // Computed properties
    const tabNames = computed(() => {
      return props.rights.map((r) => r.rightId);
    });

    const currentRights: ComputedRef<Array<RightRest>> = computed(() => {
      return props.rights;
    });

    const tab = ref(0);
    watch(() => props.selectedRight, (currentValue: string, oldValue: string) => {
      const preselectedIdx = props.rights.findIndex(
          (e) => e.rightId === props.selectedRight,
      );
      if (preselectedIdx == -1){
        tab.value = 0;
      } else {
        tab.value = preselectedIdx;
      }
    });

    onMounted(() => {
      const preselectedIdx = props.rights.findIndex(
         (e) => e.rightId === props.selectedRight,
      );
      if (preselectedIdx == -1){
        tab.value = 0;
      } else {
        tab.value = preselectedIdx;
      }
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
      parseDate,
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
    <v-toolbar :key="renderKey" color="cyan" dark flat>
      <v-toolbar-title> Editiere Rechte für {{ handle }} </v-toolbar-title>
      <v-spacer></v-spacer>
      <template v-slot:extension>
        <v-tabs
          v-model="tab"
          align-with-title
          show-arrows
          slider-color="yellow"
        >
          <v-tab v-for="r in currentRights" :key="r.rightId">
            <v-icon v-if="r.isTemplate">mdi-note-multiple</v-icon>
            <v-icon v-else>mdi-note-outline</v-icon>
            Id:'{{ r.rightId }}'; {{ parseDate(r.startDate) }} -
            {{ parseDate(r.endDate) }}
          </v-tab>
        </v-tabs>
      </template>
    </v-toolbar>

    <v-window v-model="tab">
      <v-alert
        v-model="lastDeletionSuccessful"
        closable
        type="success"
        @close="resetLastDeletionSuccessful"
      >
        Rechteinformation {{ lastDeletedRight }} erfolgreich gelöscht für Item
        {{ handle }}.
      </v-alert>
      <v-window-item v-for="(item, index) in currentRights" :key="item.rightId">
        <RightsEditDialog
          :index="index"
          :isNewRight="false"
          :isNewTemplate="false"
          :handle="handle"
          :right="item"
          v-on:deleteSuccessful="deleteSuccessful"
          v-on:editRightClosed="tabDialogClosed"
          v-on:updateSuccessful="updateSuccessful"
        ></RightsEditDialog>
      </v-window-item>
    </v-window>
  </v-card>
</template>
