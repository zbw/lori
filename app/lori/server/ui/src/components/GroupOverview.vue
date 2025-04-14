<script lang="ts">
import api from "@/api/api";
import {computed, defineComponent, onMounted, ref, Ref} from "vue";
import { GroupRest } from "@/generated-sources/openapi";
import GroupEdit from "@/components/GroupEdit.vue";
import { useDialogsStore } from "@/stores/dialogs";
import error from "@/utils/error";
import {useUserStore} from "@/stores/user";

export default defineComponent({
  components: { GroupEdit },
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
        .getGroupList(0, 100 )
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
          successMsg.value = "Gruppe " +
              "'" + group.title + " (" + groupId + ")'" +
              " erfolgreich hinzugefügt.";
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
          successMsg.value = "Gruppe " +
              "'" + group.title + " (" + groupId + ")'" +
              " erfolgreich geupdated.";
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
      successMsg.value = "Gruppe " +
          "'" + deletedGroup.title + " (" + deletedGroup.groupId + ")'" +
          " erfolgreich gelöscht.";
      groupItems.value.splice(index.value, 1);
      renderKey.value += 1;
      successMsgIsActive.value = true;
    };

    const tooltipEditText = computed(() => {
      if(userStore.isLoggedIn){
        return "Bearbeiten";
      } else {
        return "Ansehen";
      }
    });

    /**
     * Closing
     */
    const close = () => {
      emit("groupOverviewClosed");
    };

    return {
      currentGroup,
      dialogStore,
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
                variant="text"
                @click="editGroup(item)"
                icon="mdi-pencil"
                v-bind="props"
                class="tooltip-btn"
            >
            </v-btn>
          </template>
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
    </v-container>
  </v-card>
</template>
