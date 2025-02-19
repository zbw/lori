<script lang="ts">
import { RightRest } from "@/generated-sources/openapi";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import RightsEditTabs from "@/components/RightsEditTabs.vue";
import { computed, defineComponent, PropType, ref } from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import metadata_utils from "@/utils/metadata_utils";
import { useSearchStore } from "@/stores/search";

export default defineComponent({
  computed: {
    metadata_utils() {
      return metadata_utils;
    },
  },
  props: {
    rights: {
      type: Object as PropType<Array<RightRest>>,
      required: true,
    },
    handle: {
      type: String,
      required: true,
    },
    title: {
      type: String,
      required: true,
    },
  },
  components: {
    RightsEditDialog,
    RightsEditTabs,
  },

  emits: [
    "addRightSuccessful",
  ],

  setup(props, { emit }) {
    const searchStore = useSearchStore();
    const currentRight = ref({} as RightRest);
    const currentIndex = ref(0);
    const headers = [
      {
        title: "AccessState",
        value: "accessState",
      },
      {
        title: "Start-Datum",
        value: "startDate",
      },
      {
        title: "End-Datum",
        value: "endDate",
      },
      {
        title: "Rechte-ID",
        value: "rightId",
      },
      {
        title: "Typ",
        value: "type",
      },
    ];
    const isNew = ref(false);
    const renderKey = ref(0);
    const updateSuccessful = ref(false);

    const activateTabEdit = (mouseEvent: MouseEvent, row: any) => {
      dialogStore.rightsEditTabsSelectedRight = row.item.rightId;
      dialogStore.rightsEditTabsActivated = true;
    };

    const tabDialogClosed = () => {
      dialogStore.rightsEditTabsActivated = false;
    };

    const dialogStore = useDialogsStore();
    const newRight = () => {
      dialogStore.editRightActivated = true;
      currentRight.value = {} as RightRest;
      updateSuccessful.value = false;
      currentIndex.value = -1;
      isNew.value = true;
    };
    const editRightClosed = () => {
      dialogStore.editRightActivated = false;
    };

    const addRight = (right: RightRest) => {
      currentRights.value.unshift(right);
      renderKey.value += 1;
      dialogStore.editRightActivated = false;
      emit("addRightSuccessful", props.title, props.handle);
    };

    const updateRight = (right: RightRest, index: number) => {
      currentRights.value[index] = right;
      renderKey.value += 1;
      updateSuccessful.value = true;
    };

    const deleteSuccessful = (index: number) => {
      currentRights.value.splice(index, 1);
      renderKey.value += 1;
    };

    const parseEndDate = (d: Date | undefined) => {
      if (d === undefined) {
        return "";
      } else {
        return d.toLocaleDateString("de");
      }
    };

    const currentRights = computed(() => {
      return props.rights;
    });

    return {
      // Variables
      currentRight,
      currentIndex,
      dialogStore,
      isNew,
      renderKey,
      headers,
      searchStore,
      // Methods
      activateTabEdit,
      addRight,
      deleteSuccessful,
      editRightClosed,
      newRight,
      parseEndDate,
      tabDialogClosed,
      updateRight,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-sheet v-if="rights" class="mx-auto" tile>
    <v-divider></v-divider>
    <v-data-table
      :key="renderKey"
      :headers="headers"
      :items="rights"
      @click:row="activateTabEdit"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title
            >Rechteinformationen
            <a
              v-bind:href="
                metadata_utils.hrefHandle(handle, searchStore.handleURLResolver)
              "
              >{{ metadata_utils.shortenHandle(handle) }}</a
            >
          </v-toolbar-title>
          <v-divider class="mx-4" inset vertical></v-divider>
          <v-btn class="mb-2" color="primary" dark @click="newRight()">
            Neu
          </v-btn>
        </v-toolbar>
      </template>
      <template v-slot:item.endDate="{ item }">
        <td>{{ parseEndDate(item.endDate) }}</td>
      </template>
      <template v-slot:item.startDate="{ item }">
        <td>{{ item.startDate.toLocaleDateString("de") }}</td>
      </template>
      <template v-slot:item.type="{ item }">
        <v-tooltip location="bottom" text="Template">
          <template v-slot:activator="{ props }">
              <v-icon v-if="item.isTemplate" v-bind="props">
                mdi-note-multiple
            </v-icon>
          </template>
        </v-tooltip>
        <v-tooltip location="bottom" text="Einzelner Rechteeintrag">
          <template v-slot:activator="{ props }">
            <v-icon v-if="!item.isTemplate" v-bind="props">
              mdi-note-outline
            </v-icon>
          </template>
        </v-tooltip>
      </template>
    </v-data-table>
    <v-dialog
      v-model="dialogStore.editRightActivated"
      :retain-focus="false"
      max-height="800px"
      max-width="1600px"
      v-on:close="editRightClosed"
      persistent
    >
      <RightsEditDialog
        :index="currentIndex"
        :isNewRight="isNew"
        :isNewTemplate="false"
        :handle="handle"
        :rightId="currentRight.rightId"
        v-on:addSuccessful="addRight"
        v-on:editRightClosed="editRightClosed"
      ></RightsEditDialog>
    </v-dialog>
    <v-dialog
      v-model="dialogStore.rightsEditTabsActivated"
      max-height="1000px"
      max-width="1600px"
      :retain-focus="false"
      v-on:close="tabDialogClosed"
      v-on:deleteSuccessful="deleteSuccessful"
      v-on:updateSuccessful="updateRight"
      persistent
      scrollable
    >
      <RightsEditTabs
        :handle="handle"
        :rights="rights"
        :selectedRight="dialogStore.rightsEditTabsSelectedRight"
        v-on:tabDialogClosed="tabDialogClosed"
        v-on:updateSuccessful="updateRight"
      ></RightsEditTabs>
    </v-dialog>
  </v-sheet>
</template>
