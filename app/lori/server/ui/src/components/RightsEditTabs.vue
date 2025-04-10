<script lang="ts">
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import { RightRest } from "@/generated-sources/openapi";
import {computed, ComputedRef, defineComponent, onMounted, PropType, Ref, ref, watch} from "vue";

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
    licenceUrl: {
      type: String,
      required: false,
    }
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
    const formStatus: Ref<Record<string, boolean>> = ref({});
    const unsavedChangesDialog = ref(false);

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

    const closeUnsavedChangesDialog = () => {
      unsavedChangesDialog.value = false;
    };

    const closeTabDisregardChanges = () => {
      formStatus.value = Object.assign({} as Ref<Record<string, boolean>>);
      unsavedChangesDialog.value = false;
      emit("tabDialogClosed");
    };

    const tabDialogClosed = () => {
      if(Object.values(formStatus.value).includes(true)){
        unsavedChangesDialog.value = true;
      } else {
        emit("tabDialogClosed");
      }
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

    const setFormStatus = (b: boolean, rightId: string) => {
      formStatus.value[rightId] = b;
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
      unsavedChangesDialog,
      closeUnsavedChangesDialog,
      closeTabDisregardChanges,
      deleteSuccessful,
      parseDate,
      resetLastDeletionSuccessful,
      resetLastUpdateSuccessful,
      setFormStatus,
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
      <v-btn
          icon="mdi-close"
          @click="tabDialogClosed"
      ></v-btn>
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
          :rightId="item.rightId"
          :isTabEntry="true"
          :licenceUrl="licenceUrl"
          v-on:deleteSuccessful="deleteSuccessful"
          v-on:editRightClosed="tabDialogClosed"
          v-on:hasFormChanged="setFormStatus"
          v-on:updateSuccessful="updateSuccessful"
        ></RightsEditDialog>
        <v-dialog v-model="unsavedChangesDialog" max-width="500px">
          <v-card>
            <v-card-title class="text-h5 text-center">Hinweis</v-card-title>
            <v-card-text class="text-center">
              Änderungen wurden noch nicht gespeichert!
            </v-card-text>
            <v-card-actions>
              <v-spacer></v-spacer>
              <v-btn
                  @click="closeUnsavedChangesDialog"
                  color="blue darken-1"
              >Abbrechen
              </v-btn>
              <v-btn
                  color="error"
                  @click="closeTabDisregardChanges">
                Änderungen verwerfen
              </v-btn>
              <v-spacer></v-spacer>
            </v-card-actions>
          </v-card>
        </v-dialog>
      </v-window-item>
    </v-window>
  </v-card>
</template>
