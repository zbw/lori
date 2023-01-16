<script lang="ts">
import api from "@/api/api";
import { defineComponent, onMounted, ref, Ref } from "vue";
import { GroupRest } from "@/generated-sources/openapi";
import GroupEdit from "@/components/GroupEdit.vue";
import { useDialogsStore } from "@/stores/dialogs";

export default defineComponent({
  components: { GroupEdit },
  setup() {
    const renderKey = ref(0);
    const headers = [
      {
        text: "Liste aller Gruppen",
        align: "start",
        value: "name",
      },
    ];
    const groupItems: Ref<Array<GroupRest>> = ref([]);
    const groupLoadErrorMsg = ref("");
    const getGroupList = () => {
      api
        .getGroupList(0, 100)
        .then((r: Array<GroupRest>) => {
          groupItems.value = r;
        })
        .catch((e) => {
          groupLoadErrorMsg.value =
            e.response.statusText + " (Statuscode: " + e.response.status + ")";
        });
    };

    onMounted(() => getGroupList());
    const dialogStore = useDialogsStore();

    /**
     * Edit or create new group.
     */
    const activateGroupEditDialog = () => {
      dialogStore.groupEditActivated = true;
      addSuccessfulNotification.value = false;
      updateSuccessfulNotification.value = false;
    };

    const closeGroupEditDialog = () => {
      dialogStore.groupOverviewActivated = false;
    };

    const isNew = ref(false);
    const index = ref(-1);
    const currentGroup = ref({} as GroupRest);
    const createNewGroup = () => {
      isNew.value = true;
      currentGroup.value = {} as GroupRest;
      activateGroupEditDialog();
    };
    const editGroup = (group: GroupRest, row:any) => {
      isNew.value = false;
      currentGroup.value = group;
      index.value = row.index;
      activateGroupEditDialog();
    };

    /**
     * Update list of groups.
     */
    const addSuccessfulNotification = ref(false);
    const updateSuccessfulNotification = ref(false);
    const lastModifiedGroup = ref({} as GroupRest);
    const addGroupEntry = (group: GroupRest) => {
      groupItems.value.unshift(group);
      renderKey.value += 1;
      addSuccessfulNotification.value = true;
      lastModifiedGroup.value = group;
    };
    const updateGroupEntry = (group: GroupRest) => {
      groupItems.value[index.value] = group;
      renderKey.value += 1;
      updateSuccessfulNotification.value = true;
      lastModifiedGroup.value = group;
    };

    return {
      addSuccessfulNotification,
      currentGroup,
      dialogStore,
      headers,
      isNew,
      items: groupItems,
      lastModifiedGroup,
      renderKey,
      updateSuccessfulNotification,
      activateGroupEditDialog,
      addGroupEntry,
      closeGroupEditDialog,
      createNewGroup,
      editGroup,
      updateGroupEntry,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-alert
      v-model="addSuccessfulNotification"
      dismissible
      text
      type="success"
    >
      Gruppe {{ lastModifiedGroup.name }} erfolgreich hinzugefügt.
    </v-alert>
    <v-alert
      v-model="updateSuccessfulNotification"
      dismissible
      text
      type="success"
    >
      Gruppe {{ lastModifiedGroup.name }} erfolgreich geupdated.
    </v-alert>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn color="blue darken-1" text @click="createNewGroup"
        >Neue IP-Gruppe anlegen
      </v-btn>
    </v-card-actions>
    <v-data-table
      :headers="headers"
      :items="items"
      :key="renderKey"
      @click:row="editGroup"
      loading-text="Daten werden geladen... Bitte warten."
      show-select
      item-key="groupName"
    ></v-data-table>
    <v-dialog
      max-width="1000px"
      v-model="dialogStore.groupEditActivated"
      :retain-focus="false"
      v-on:close="closeGroupEditDialog"
    >
      <GroupEdit
        :isNew="isNew"
        :group="currentGroup"
        v-on:addGroupSuccessful="addGroupEntry"
        v-on:updateGroupSuccessful="updateGroupEntry"
      ></GroupEdit>
    </v-dialog>
  </v-card>
</template>
