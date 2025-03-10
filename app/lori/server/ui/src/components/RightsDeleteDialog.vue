<script lang="ts">
import api from "@/api/api";
import { defineComponent, ref } from "vue";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";

export default defineComponent({
  props: {
    rightId: {
      type: String,
      required: false,
    },
    index: {
      type: Number,
      required: true,
    },
    isTemplate: {
      type: Boolean,
      required: false,
    },
  },
  // Emits
  emits: ["deleteDialogClosed", "deleteSuccessful", "templateDeleteSuccessful"],

  setup(props, { emit }) {
    const deleteAlertError = ref(false);
    const deleteInProgress = ref(false);
    const deleteErrorMessage = ref("");
    const deleteError = ref(false);

    const close = () => {
      emit("deleteDialogClosed");
    };

    const deleteRight = () => {
      deleteInProgress.value = true;
      deleteAlertError.value = false;
      deleteError.value = false;

      if (props.rightId == undefined || props.rightId == "") {
        deleteErrorMessage.value = "Es fehlt eine valide Rechte-Id!";
        deleteError.value = true;
        deleteAlertError.value = true;
      } else {
        api
          .deleteRight(props.rightId)
          .then(() => {
            emit("deleteSuccessful", props.index);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              deleteErrorMessage.value = errMsg;
              deleteError.value = true;
              deleteAlertError.value = true;
            });
          })
          .finally(() => {
            deleteInProgress.value = false;
          });
      }
    };

    const deleteTemplate = () => {
      deleteInProgress.value = true;
      deleteAlertError.value = false;
      deleteError.value = false;
      if (props.rightId == undefined || props.rightId == "") {
        deleteErrorMessage.value = "Es fehlt eine valide Rechte-Id!";
        deleteError.value = true;
        deleteAlertError.value = true;
      } else {
        templateApi
          .deleteTemplateById(props.rightId)
          .then(() => {
            emit("templateDeleteSuccessful");
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              deleteErrorMessage.value = errMsg;
              deleteError.value = true;
              deleteAlertError.value = true;
            });
          })
          .finally(() => {
            deleteInProgress.value = false;
          });
      }
    };

    const deleteEntity = () => {
      if (props.isTemplate) {
        deleteTemplate();
      } else {
        deleteRight();
      }
    };

    return {
      // variables
      deleteAlertError,
      deleteErrorMessage,
      deleteInProgress,
      // methods
      deleteEntity,
      deleteRight,
      deleteTemplate,
      close,
    };
  },
});
</script>
<style scoped></style>

<template>
  <v-card>
    <v-card-title class="text-h5">Löschen bestätigen</v-card-title>
    <v-alert v-model="deleteAlertError" closable type="error">
      Löschen war nicht erfolgreich:
      {{ deleteErrorMessage }}
    </v-alert>
    <v-card-text v-if="isTemplate">
      Möchtest du dieses Template wirklich löschen?
    </v-card-text>
    <v-card-text v-else>
      Möchtest du diese Rechteinformation wirklich löschen?
    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn :disabled="deleteInProgress" color="blue darken-1" @click="close"
        >Abbrechen
      </v-btn>
      <v-btn :loading="deleteInProgress" color="error" @click="deleteEntity">
        Löschen
      </v-btn>
      <v-spacer></v-spacer>
    </v-card-actions>
  </v-card>
</template>
