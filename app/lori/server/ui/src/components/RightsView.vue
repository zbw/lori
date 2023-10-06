<script lang="ts">
import { RightRest } from "@/generated-sources/openapi";
import { DataTableHeader } from "vuetify";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import RightsEditTabs from "@/components/RightsEditTabs.vue";
import { computed, defineComponent, PropType, ref } from "vue";
import { useDialogsStore } from "@/stores/dialogs";

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
    handle: {
      type: String,
      required: true,
    },
  },
  components: {
    RightsEditDialog,
    RightsEditTabs,
  },

  setup(props) {
    const tabDialogActivated = ref(false);
    const currentRight = ref({} as RightRest);
    const currentIndex = ref(0);
    const headers = [
      {
        text: "AccessState",
        value: "accessState",
      },
      {
        text: "Start-Datum",
        value: "startDate",
      },
      {
        text: "End-Datum",
        value: "endDate",
      },
      {
        text: "Template",
        value: "templateId",
      },
    ] as Array<DataTableHeader>;
    const isNew = ref(false);
    const renderKey = ref(0);
    const selectedHeaders: Array<DataTableHeader> = headers;
    const updateSuccessful = ref(false);
    const addSuccessful = ref(false);

    const activateTabEdit = () => {
      tabDialogActivated.value = true;
    };

    const tabDialogClosed = () => {
      tabDialogActivated.value = false;
    };

    const dialogStore = useDialogsStore();
    const newRight = () => {
      dialogStore.editRightActivated = true;
      currentRight.value = {} as RightRest;
      updateSuccessful.value = false;
      addSuccessful.value = false;
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
      addSuccessful.value = true;
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
      addSuccessful,
      currentRight,
      currentIndex,
      dialogStore,
      isNew,
      renderKey,
      selectedHeaders,
      tabDialogActivated,
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
  <v-card v-if="rights" class="mx-auto" tile>
    <v-alert v-model="addSuccessful" dismissible text type="success">
      Rechteinformation erfolgreich für Item {{ metadataId }} hinzugefügt.
    </v-alert>
    <v-divider></v-divider>
    <v-data-table
      :key="renderKey"
      :headers="selectedHeaders"
      :items="rights"
      @click:row="activateTabEdit"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title
            >Rechteinformationen
            <a :href="handle">{{ handle.substring(22, 35) }}</a>
          </v-toolbar-title>
          <v-divider class="mx-4" inset vertical></v-divider>
          <v-spacer></v-spacer>
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
    </v-data-table>
    <v-dialog
      v-model="dialogStore.editRightActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="editRightClosed"
      v-on:click:outside="editRightClosed"
    >
      <RightsEditDialog
        :index="currentIndex"
        :isNewRight="isNew"
        :isNewTemplate="false"
        :metadataId="metadataId"
        :right="currentRight"
        :isTemplate="currentRight.templateId != undefined"
        :templateId="currentRight.templateId"
        v-on:addSuccessful="addRight"
        v-on:editRightClosed="editRightClosed"
      ></RightsEditDialog>
    </v-dialog>
    <v-dialog
      v-model="tabDialogActivated"
      :retain-focus="false"
      v-on:close="tabDialogClosed"
      v-on:deleteSuccessful="deleteSuccessful"
      v-on:updateSuccessful="updateRight"
      v-on:click:outside="tabDialogClosed"
    >
      <RightsEditTabs
        :metadata-id="metadataId"
        :rights="rights"
        v-on:tabDialogClosed="tabDialogClosed"
        v-on:updateSuccessful="updateRight"
      ></RightsEditTabs>
    </v-dialog>
  </v-card>
</template>
