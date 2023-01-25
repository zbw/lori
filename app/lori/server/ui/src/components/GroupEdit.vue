<script lang="ts">
import { useDialogsStore } from "@/stores/dialogs";
import {
  computed,
  defineComponent,
  onMounted,
  PropType,
  reactive,
  ref,
  watch,
} from "vue";
import api from "@/api/api";
import { GroupRest } from "@/generated-sources/openapi/models/GroupRest";
import { required } from "@vuelidate/validators";
import { useVuelidate } from "@vuelidate/core";
import GroupDeleteDialog from "@/components/GroupDeleteDialog.vue";

export default defineComponent({
  components: { GroupDeleteDialog },
  props: {
    isNew: {
      type: Boolean,
      required: true,
    },
    group: {
      type: {} as PropType<GroupRest>,
      required: true,
    },
  },
  emits: [
    "addGroupSuccessful",
    "deleteGroupSuccessful",
    "updateGroupSuccessful",
  ],
  setup(props, { emit }) {
    /**
     * Vuelidate.
     */
    type ValidatingFields = {
      name: string;
      ipAddressesFile: File | null;
      ipAddressesText: string;
    };

    const ipAddressCheck = (value: string, siblings: ValidatingFields) => {
      return !(
        siblings.ipAddressesFile == null && siblings.ipAddressesText == ""
      );
    };

    const rules = {
      name: { required },
      ipAddressesFile: { ipAddressCheck },
      ipAddressesText: { ipAddressCheck },
    };

    const formState = reactive({
      name: "",
      description: "" as string | undefined,
      ipAddressesFile: null as File | null,
      ipAddressesText: "",
    });

    const v$ = useVuelidate(rules, formState);

    /**
     * Vuelidate error states
     */
    const errorIpAddresses = computed(() => {
      const errors: Array<string> = [];
      if (
        v$.value.ipAddressesText.$invalid &&
        v$.value.ipAddressesText.$dirty &&
        v$.value.ipAddressesFile.$invalid &&
        v$.value.ipAddressesFile.$dirty
      ) {
        errors.push("Es wird eine Eingabe für die IP-Adressen erwartet.");
      }
      return errors;
    });
    const errorName = computed(() => {
      const errors: Array<string> = [];
      if (v$.value.name.$invalid && v$.value.name.$dirty) {
        errors.push("Es wird ein Name benötigt.");
      }
      return errors;
    });
    /**
     * Dialog title.
     */
    const dialogTitle = computed(() => {
      if (props.isNew) {
        return "Neue IP-Gruppe anlegen";
      } else {
        return "IP-Gruppe editieren";
      }
    });

    /**
     * Dialog management.
     */
    const dialogStore = useDialogsStore();
    const close = () => {
      v$.value.$reset();
      dialogStore.groupEditActivated = false;
      saveAlertError.value = false;
      saveAlertErrorMessage.value = "";
    };

    /**
     * Group object initialization.
     */
    const hasNoCSVHeader = ref(false);
    const groupTmp = ref({} as GroupRest);
    const computedGroup = computed(() => props.group);
    watch(computedGroup, (currentValue, oldValue) => {
      reinitializeGroup(currentValue);
    });
    onMounted(() => reinitializeGroup(props.group));

    const reinitializeGroup = (newValue: GroupRest) => {
      groupTmp.value = Object.assign({}, newValue);
      if (props.isNew) {
        hasNoCSVHeader.value = false;
        formState.name = "";
        formState.ipAddressesText = "";
        formState.ipAddressesFile = null;
        formState.description = "";
      } else {
        formState.name = groupTmp.value.name;
        formState.ipAddressesText = groupTmp.value.ipAddresses;
        formState.ipAddressesFile = null;
        formState.description = groupTmp.value.description;
        hasNoCSVHeader.value = !groupTmp.value.hasCSVHeader;
      }
    };
    /**
     * Save Group.
     */
    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");

    const createGroup = () => {
      api
        .addGroup(groupTmp.value)
        .then((r) => {
          emit("addGroupSuccessful", groupTmp.value.name);
          close();
        })
        .catch((e) => {
          saveAlertErrorMessage.value =
            "Speichern ist fehlgeschlagen: " + createErrorMsg(e);
          saveAlertError.value = true;
        });
    };

    const updateGroup = () => {
      api
        .updateGroup(groupTmp.value)
        .then((r) => {
          emit("updateGroupSuccessful", groupTmp.value.name);
          close();
        })
        .catch((e) => {
          saveAlertErrorMessage.value =
            "Update ist fehlgeschlagen: " + createErrorMsg(e);
          saveAlertError.value = true;
        });
    };

    const createErrorMsg: (e: any) => string = (e: any) => {
      let errorExplain: string;
      if (e.response.status == "409") {
        errorExplain =
          "Eine Gruppe mit diesem Namen existiert bereits (Fehlercode 409)";
      } else if (e.response.status == "400") {
        errorExplain =
          "Das Format der CSV Eingabe ist nicht korrekt (Fehlercode 400)";
      } else {
        errorExplain =
          "Unerwarteter Fehler. Bitte an Admin wenden: " +
          e.response.statusText +
          " (Statuscode: " +
          e.response.status +
          ")";
      }
      return errorExplain;
    };

    const save = () => {
      return v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        groupTmp.value.hasCSVHeader = !hasNoCSVHeader.value;
        groupTmp.value.name = formState.name;
        groupTmp.value.description = formState.description;
        if (
          formState.ipAddressesText == "" &&
          formState.ipAddressesFile != null
        ) {
          formState.ipAddressesFile
            .text()
            .then((r) => {
              groupTmp.value.ipAddresses = r;
              if (props.isNew) {
                createGroup();
              } else {
                updateGroup();
              }
            })
            .catch((e) => {
              saveAlertError.value = true;
              saveAlertErrorMessage.value =
                "Auslesen von Datei ist fehlgeschlagen: " +
                e.response.statusText +
                " (Statuscode: " +
                e.response.status +
                ")";
            });
          return;
        }
        groupTmp.value.ipAddresses = formState.ipAddressesText;
        if (props.isNew) {
          createGroup();
        } else {
          updateGroup();
        }
      });
    };

    // Upload CSV-File
    const fileContent = ref({} as File);

    // Delete Group
    const initiateDeleteDialog = () => {
      groupTmp.value.name = formState.name;
      dialogStore.groupDeleteActivated = true;
    };

    const deleteGroupSuccessful = () => {
      emit("deleteGroupSuccessful");
      close();
    };

    return {
      dialogStore,
      dialogTitle,
      errorName,
      errorIpAddresses,
      fileContent,
      formState,
      groupTmp,
      hasNoCSVHeader,
      saveAlertError,
      saveAlertErrorMessage,
      v$,
      close,
      createGroup,
      deleteGroupSuccessful,
      initiateDeleteDialog,
      save,
      updateGroup,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-card>
    <v-container>
      <v-card-title>{{ dialogTitle }}</v-card-title>
      <v-alert v-model="saveAlertError" dismissible text type="error">
        {{ saveAlertErrorMessage }}
      </v-alert>
      <v-dialog
          v-model="dialogStore.groupDeleteActivated"
          max-width="500px">
        <GroupDeleteDialog
          :group-id="groupTmp.name"
          v-on:deleteGroupSuccessful="deleteGroupSuccessful"
        ></GroupDeleteDialog>
      </v-dialog>
      <v-card>
        <v-row>
          <v-col>
            <v-text-field
              v-if="isNew"
              outlined
              label="Name der Berechtigungsgruppe"
              v-model="formState.name"
              :error-messages="errorName"
            ></v-text-field>
            <v-text-field
              v-if="!isNew"
              outlined
              label="Name der Berechtigungsgruppe"
              v-model="formState.name"
              :error-messages="errorName"
              disabled
            ></v-text-field>
          </v-col>
        </v-row>
      </v-card>
      <v-alert border="top" colored-border type="info" elevation="2">
        Eine Neue Gruppe kann angelegt werden, indem die IP-Bereiche manuell
        hier eingegeben werden oder indem die entsprechenden IP-Bereiche per
        CSV-Datei hochgeladen werden. Beides gleichzeitig ist nicht möglich. Der
        Freitext hat höhere Priorität. Das erwartete CSV Format ist:
        "organisation;ip-adressen"
      </v-alert>
      <v-card>
        <v-row justify="center">
          <v-card-title>Berechtigte IP-Adress-Bereiche</v-card-title>
        </v-row>
        <v-row>
          <v-col cols="5">
            <v-textarea
              label="IP-Adressen"
              v-model="formState.ipAddressesText"
              :error-messages="errorIpAddresses"
              outlined
            ></v-textarea>
            <v-checkbox
              label="CSV Eingabe besitzt KEINEN Header"
              v-model="hasNoCSVHeader"
            ></v-checkbox>
          </v-col>
          <v-col cols="2"> oder</v-col>
          <v-col cols="5">
            <v-file-input
              chips
              accept=".csv"
              label="CSV-Datei"
              v-model="formState.ipAddressesFile"
              :error-messages="errorIpAddresses"
              outlined
            ></v-file-input>
            Hinweis: Es kann nur eine CSV-Datei pro Gruppe hinterlegt werden.
          </v-col>
        </v-row>
      </v-card>
      <v-card>
        <v-row>
          <v-col>
            <v-textarea
              label="Beschreibung"
              v-model="formState.description"
              outlined
            ></v-textarea>
          </v-col>
        </v-row>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="save" color="blue darken-1" text>Speichern</v-btn>
          <v-btn @click="close" color="blue darken-1" text>Zurück</v-btn>
          <v-btn v-if="!isNew" icon @click="initiateDeleteDialog">
            <v-icon>mdi-delete</v-icon>
          </v-btn>
          <v-btn v-if="isNew" icon disabled>
            <v-icon>mdi-delete</v-icon>
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-container>
  </v-card>
</template>
