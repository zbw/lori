<script lang="ts">
import { RightRest } from "@/generated-sources/openapi";
import { DataTableHeader } from "vuetify";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import RightsEditTabs from "@/components/RightsEditTabs.vue";
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
    const editDialogActivated = ref(false);
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

    const newRight = () => {
      editDialogActivated.value = true;
      currentRight.value = {} as RightRest;
      updateSuccessful.value = false;
      addSuccessful.value = false;
      currentIndex.value = -1;
      isNew.value = true;
    };

    const editRightClosed = () => {
      editDialogActivated.value = false;
    };

    const addRight = (right: RightRest) => {
      currentRights.value.unshift(right);
      renderKey.value += 1;
      editDialogActivated.value = false;
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
      editDialogActivated,
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
      :headers="selectedHeaders"
      :items="rights"
      :key="renderKey"
      @click:row="activateTabEdit"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title>Rechteinformationen
            <a :href="handle">{{ handle.substring(22, 35) }}</a>
          </v-toolbar-title>
          <v-divider class="mx-4" inset vertical></v-divider>
          <v-spacer></v-spacer>
          <v-btn @click="newRight()" color="primary" dark class="mb-2">
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
      v-model="editDialogActivated"
      max-width="1000px"
      v-on:close="editRightClosed"
      v-on:click:outside="editRightClosed"
      :retain-focus="false"
    >
      <RightsEditDialog
        :right="currentRight"
        :index="currentIndex"
        :isNew="isNew"
        :metadataId="metadataId"
        v-on:addSuccessful="addRight"
        v-on:editDialogClosed="editRightClosed"
      ></RightsEditDialog>
    </v-dialog>
    <v-dialog
      v-model="tabDialogActivated"
      v-on:close="tabDialogClosed"
      v-on:click:outside="tabDialogClosed"
      v-on:updateSuccessful="updateRight"
      v-on:deleteSuccessful="deleteSuccessful"
      :retain-focus="false"
    >
      <RightsEditTabs
        :rights="rights"
        :metadata-id="metadataId"
        v-on:tabDialogClosed="tabDialogClosed"
        v-on:updateSuccessful="updateRight"
      ></RightsEditTabs>
    </v-dialog>
  </v-card>
</template>
