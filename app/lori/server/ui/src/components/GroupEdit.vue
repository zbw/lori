<script lang="ts">
import { useDialogsStore } from "@/stores/dialogs";
import {computed, defineComponent, onMounted, PropType, reactive, ref, watch} from "vue";
import api from "@/api/api";
import { GroupRest } from "@/generated-sources/openapi/models/GroupRest";
import { required } from "@vuelidate/validators";
import { useVuelidate } from "@vuelidate/core";

export default defineComponent({
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
  emits: ["addGroupSuccessful", "updateGroupSuccessful"],
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
     * Group object initialization
     */
    const groupTmp = ref({} as GroupRest);
    const computedGroup = computed(() => props.group);
    watch(computedGroup, (currentValue, oldValue) => {
      reinitializeGroup(currentValue);
    });
    onMounted(() => reinitializeGroup(props.group));

    const reinitializeGroup = (newValue: GroupRest) => {
      groupTmp.value = Object.assign({}, newValue);
      if (props.isNew) {
        formState.name = "";
        formState.ipAddressesText = "";
        formState.ipAddressesFile = null;
        formState.description = "";
      } else {
        formState.name = groupTmp.value.name;
        formState.ipAddressesText = groupTmp.value.ipAddresses;
        formState.ipAddressesFile = null;
        formState.description = groupTmp.value.description;
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
          emit("addGroupSuccessful", groupTmp.value);
          close();
        })
        .catch((e) => {
          console.log(e);
          saveAlertError.value = true;
          saveAlertErrorMessage.value =
            "Speichern ist fehlgeschlagen: " +
            e.response.statusText +
            " (Statuscode: " +
            e.response.status +
            ")";
        });
    };

    const updateGroup = () => {
      api
        .updateGroup(groupTmp.value)
        .then((r) => {
          emit("updateGroupSuccessful", groupTmp.value);
          close();
        })
        .catch((e) => {
          console.log(e);
          saveAlertError.value = true;
          saveAlertErrorMessage.value =
            "Speichern ist fehlgeschlagen: " +
            e.response.statusText +
            " (Statuscode: " +
            e.response.status +
            ")";
        });
    };

    const save = () => {
      return v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
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
              createGroup();
            })
            .catch((e) => {
              console.log(e);
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

    return {
      dialogStore,
      dialogTitle,
      errorName,
      errorIpAddresses,
      fileContent,
      formState,
      groupTmp,
      saveAlertError,
      saveAlertErrorMessage,
      v$,
      close,
      createGroup,
      save,
      updateGroup,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-container class="grey lighten-5 mb-6" fluid>
    <v-card>
      <v-card-title>{{ dialogTitle }}</v-card-title>
      <v-alert v-model="saveAlertError" dismissible text type="error">
        Speichern war nicht erfolgreich:
        {{ saveAlertErrorMessage }}
      </v-alert>
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
        Freitext hat höhere Priorität.
      </v-alert>
      <v-card>
        <v-row justify="center">
          <v-card-title>Berechtigte IP-Adress-Bereiche</v-card-title>
        </v-row>
        <v-row>
          <v-col cols="5">
            <v-textarea
              label="IP-Adressen"
              hint="Es wird ein CSV Format erwartet: <Name>,<IP-Adressbereich>"
              v-model="formState.ipAddressesText"
              :error-messages="errorIpAddresses"
              outlined
            ></v-textarea>
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
            ></v-textarea>
          </v-col>
        </v-row>
        <v-row justify="end">
          <v-card tile outlined>
            <v-btn @click="save" color="blue darken-1" text>Speichern</v-btn>
            <v-btn @click="close" color="blue darken-1" text>Zurück</v-btn>
          </v-card>
        </v-row>
      </v-card>
    </v-card>
  </v-container>
</template>
