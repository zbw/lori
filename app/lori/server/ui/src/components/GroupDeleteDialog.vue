<script lang="ts">
import { defineComponent, ref } from "vue";
import { useDialogsStore } from "@/stores/dialogs";
import api from "@/api/api";
import error from "@/utils/error";

export default defineComponent({
  props: {
    groupId: {
      type: Number,
      required: true,
    },
  },
  // Emits
  emits: ["deleteDialogClosed", "deleteGroupSuccessful"],
  setup(props, { emit }) {
    const dialogStore = useDialogsStore();
    const deleteAlertError = ref(false);
    const deleteAlertErrorMessage = ref("");
    const deleteInProgress = ref(false);
    const close = () => {
      dialogStore.groupDeleteActivated = false;
      deleteAlertError.value = false;
      deleteAlertErrorMessage.value = "";
    };

    const deleteGroup = () => {
      deleteInProgress.value = true;
      api
        .deleteGroup(props.groupId)
        .then(() => {
          emit("deleteGroupSuccessful");
          close();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            deleteAlertErrorMessage.value = errMsg;
            deleteAlertError.value = true;
          });
        })
        .finally(() => {
          deleteInProgress.value = false;
        });
    };

    return {
      deleteAlertError,
      deleteAlertErrorMessage,
      deleteInProgress,
      close,
      deleteGroup,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-card-title class="text-h5">Löschen bestätigen</v-card-title>
    <v-snackbar
        contained
        multi-line
        location="top"
        timer="true"
        timeout="10000"
        v-model="deleteAlertError"
        color="error"
    >
      {{ deleteAlertErrorMessage }}
    </v-snackbar>
    <v-card-text> Soll diese Gruppe wirklich gelöscht werden? </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn :disabled="deleteInProgress" color="blue darken-1" @click="close"
        >Abbrechen
      </v-btn>
      <v-btn :loading="deleteInProgress" color="error" @click="deleteGroup">
        Löschen
      </v-btn>
      <v-spacer></v-spacer>
    </v-card-actions>
  </v-card>
</template>
