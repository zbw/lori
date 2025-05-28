<script lang="ts">
import api from "@/api/api";
import {computed, defineComponent, onMounted, ref, Ref} from "vue";
import {BookmarkRest, GroupRest} from "@/generated-sources/openapi";
import GroupEdit from "@/components/GroupEdit.vue";
import { useDialogsStore } from "@/stores/dialogs";
import error from "@/utils/error";
import {useUserStore} from "@/stores/user";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import GroupDeleteDialog from "@/components/GroupDeleteDialog.vue";

export default defineComponent({
  components: {GroupDeleteDialog, RightsDeleteDialog, GroupEdit },
  emits: [
      "groupOverviewClosed"
  ],
  setup(props, {emit}) {
    const renderKey = ref(0);
    const headers = [
      {
        title: "Name",
        align: "start",
        value: "title",
      },
      {
        title: "Aktionen",
        key: "actions",
        align: "start",
        sortable: false,
      },
    ];
    const groupItems: Ref<Array<GroupRest>> = ref([]);
    const groupLoadError = ref(false);
    const groupLoadErrorMsg = ref("");
    const getGroupList = () => {
      api
        .getGroupList(0, 500 )
        .then((r: Array<GroupRest>) => {
          groupItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            groupLoadErrorMsg.value = errMsg;
            groupLoadError.value = true;
          });
        });
    };

    onMounted(() => getGroupList());

    /**
     * Stores:
     */
    const dialogStore = useDialogsStore();
    const userStore = useUserStore();

    /**
     * Edit or create new group.
     */
    const activateGroupEditDialog = () => {
      dialogStore.groupEditActivated = true;
      successMsgIsActive.value = false;
    };

    const closeGroupEditDialog = () => {
      dialogStore.groupEditActivated = false;
    };

    const isNew = ref(false);
    const index = ref(-1);
    const currentGroup = ref({} as GroupRest);
    const createNewGroup = () => {
      isNew.value = true;
      currentGroup.value = {} as GroupRest;
      activateGroupEditDialog();
    };
    const editGroup = (group: GroupRest) => {
      isNew.value = false;
      currentGroup.value = group;
      index.value = groupItems.value.indexOf(group);
      activateGroupEditDialog();
    };

    /**
     * Update list of groups.
     */
    const successMsgIsActive = ref(false);
    const successMsg = ref("");
    const lastModifiedGroup = ref({} as GroupRest);
    const addGroupEntry = (groupId: number) => {
      api
        .getGroupById(groupId, undefined)
        .then((group) => {
          groupItems.value.unshift(group);
          renderKey.value += 1;
          successMsgIsActive.value = true;
          successMsg.value = "Gruppe \"" + group.title + "\" erfolgreich hinzugefügt.";
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            groupLoadErrorMsg.value = errMsg;
            groupLoadError.value = true;
          });
        });
    };
    const updateGroupEntry = (groupId: number) => {
      api
        .getGroupById(groupId, undefined)
        .then((group) => {
          groupItems.value[index.value] = group;
          renderKey.value += 1;
          successMsgIsActive.value = true;
          lastModifiedGroup.value = group;
          successMsg.value = "Gruppe " + "\"" + group.title + "\" erfolgreich geupdated.";
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            groupLoadErrorMsg.value = errMsg;
            groupLoadError.value = true;
          });
        });
    };

    const deleteGroupEntry = () => {
      const deletedGroup: GroupRest = groupItems.value[index.value];
      successMsg.value = "Gruppe \"" + deletedGroup.title + "\" erfolgreich gelöscht.";
      groupItems.value.splice(index.value, 1);
      renderKey.value += 1;
      successMsgIsActive.value = true;
    };

    const tooltipEditText = computed(() => {
      if(userStore.isLoggedIn){
        return "Bearbeiten";
      } else {
        return "Anzeigen";
      }
    });
    /**
     * Deletion
     */

    const deleteDialogActivated = ref(false);
    const openDeleteDialog = (group: GroupRest) => {
      currentGroup.value = Object.assign({}, group);
      index.value = groupItems.value.indexOf(group);
      deleteDialogActivated.value = true;
    };

    const closeDeleteDialog = () => {
      deleteDialogActivated.value = false;
    };

    const deletionSuccessful = () =>  {
      groupItems.value.splice(index.value, 1);
      successMsgIsActive.value = true;
      successMsg.value = "Gruppe \"" + currentGroup.value.title + "\" erfolgreich gelöscht.";
    }

    /**
     * Closing
     */
    const close = () => {
      emit("groupOverviewClosed");
    };

    return {
      closeDeleteDialog,
      currentGroup,
      dialogStore,
      deleteDialogActivated,
      deletionSuccessful,
      groupLoadError,
      groupLoadErrorMsg,
      headers,
      isNew,
      items: groupItems,
      lastModifiedGroup,
      renderKey,
      successMsgIsActive,
      successMsg,
      tooltipEditText,
      userStore,
      activateGroupEditDialog,
      addGroupEntry,
      close,
      closeGroupEditDialog,
      createNewGroup,
      deleteGroupEntry,
      editGroup,
      openDeleteDialog,
      updateGroupEntry,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card position="relative">
    <v-toolbar>
      <v-spacer></v-spacer>
      <v-btn
          icon="mdi-close"
          @click="close"
      ></v-btn>
    </v-toolbar>
    <v-container>
    <v-snackbar
        contained
        multi-line
        location="top"
        timer="true"
        timeout="5000"
        v-model="successMsgIsActive"
        color="success"
    >
      {{ successMsg }}
    </v-snackbar>
    <v-snackbar
        contained
        multi-line
        location="top"
        timer="true"
        timeout="5000"
        v-model="groupLoadError"
        color="error"
    >
      {{ groupLoadErrorMsg }}
    </v-snackbar>
    <v-card-title>IP-Gruppen</v-card-title>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn
          color="blue darken-1"
          @click="createNewGroup"
          :disabled="!userStore.isLoggedIn"
        >Neue IP-Gruppe anlegen
      </v-btn>
    </v-card-actions>
    <v-data-table
      :headers="headers"
      :items="items"
      :key="renderKey"
      loading-text="Daten werden geladen... Bitte warten."
      item-value="groupName"
    >
      <template v-slot:item.actions="{ item }">
        <v-tooltip location="bottom" :text="tooltipEditText">
          <template v-slot:activator="{ props }">
            <v-btn
                v-if="userStore.isLoggedIn"
                variant="text"
                @click="editGroup(item)"
                icon="mdi-pencil"
                v-bind="props"
                class="tooltip-btn"
            >
            </v-btn>
            <v-btn
                v-else
                variant="text"
                @click="editGroup(item)"
                icon="mdi-eye"
                v-bind="props"
                class="tooltip-btn"
            >
            </v-btn>
          </template>
        </v-tooltip>
        <v-tooltip
            location="bottom"
        >
          <template v-slot:activator="{ props }">
            <div v-bind="props" class="d-inline-block">
              <v-btn
                  variant="text"
                  @click="openDeleteDialog(item)"
                  icon="mdi-delete"
                  :disabled="!userStore.isLoggedIn"
              >
              </v-btn>
            </div>
          </template>
          <span>Löschen</span>
        </v-tooltip>
      </template>

    </v-data-table>
    <v-dialog
      v-model="dialogStore.groupEditActivated"
      :retain-focus="false"
      v-on:close="closeGroupEditDialog"
      max-width="1500px"
      max-height="850px"
      scrollable
      persistent
    >
      <GroupEdit
        :isNew="isNew"
        :group="currentGroup"
        v-on:addGroupSuccessful="addGroupEntry"
        v-on:deleteGroupSuccessful="deleteGroupEntry"
        v-on:updateGroupSuccessful="updateGroupEntry"
        v-on:groupEditClosed="closeGroupEditDialog"
      ></GroupEdit>
    </v-dialog>
    <v-dialog
        v-model="deleteDialogActivated" max-width="700px"
    >
      <GroupDeleteDialog
          :group-id="currentGroup.groupId"
          v-on:deleteDialogClosed="closeDeleteDialog"
          v-on:deleteGroupSuccessful="deletionSuccessful"
      ></GroupDeleteDialog>
    </v-dialog>
    </v-container>
  </v-card>
</template>
