<script lang="ts">
import {useDialogsStore} from "@/stores/dialogs";
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
import {GroupRest} from "@/generated-sources/openapi/models/GroupRest";
import {required} from "@vuelidate/validators";
import {useVuelidate} from "@vuelidate/core";
import GroupDeleteDialog from "@/components/GroupDeleteDialog.vue";
import error from "@/utils/error";
import {GroupIdCreated, OldGroupVersionRest} from "@/generated-sources/openapi";
import {unparse} from "papaparse";
import {useUserStore} from "@/stores/user";

export default defineComponent({
  components: {GroupDeleteDialog},
  props: {
    isNew: {
      type: Boolean,
      required: true,
    },
    group: {
      type: Object as PropType<GroupRest>,
      required: true,
    },
  },
  emits: [
    "addGroupSuccessful",
    "deleteGroupSuccessful",
    "updateGroupSuccessful",
    "groupEditClosed",
  ],
  setup(props, {emit}) {
    /**
     * Vuelidate.
     */
    type ValidatingFields = {
      title: string;
      ipAddressesFile: File | undefined;
      ipAddressesText: string | undefined;
    };

    const ipAddressCheck = (value: string, siblings: ValidatingFields) => {
      return !(
          siblings.ipAddressesFile == null && siblings.ipAddressesText == ""
      );
    };

    const rules = {
      title: {required},
      ipAddressesFile: {ipAddressCheck},
      ipAddressesText: {ipAddressCheck},
    };

    const formState = reactive({
      groupId: 0 as number | undefined,
      title: "",
      description: "" as string | undefined,
      ipAddressesFile: undefined as File | undefined,
      ipAddressesText: "" as string | undefined,
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
      if (v$.value.title.$invalid && v$.value.title.$dirty) {
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
      } else if (userStore.isLoggedIn){
        return "IP-Gruppe bearbeiten";
      } else {
        return "IP-Gruppe anzeigen";
      }
    });

    /**
     * Dialog management.
     */
    const userStore = useUserStore();
    const dialogStore = useDialogsStore();
    const close = () => {
      v$.value.$reset();
      saveAlertError.value = false;
      saveAlertErrorMessage.value = "";
      emit("groupEditClosed");
    };

    /**
     * Version history.
     */
    const headersVersion = [
      {
        title: "Version",
        key: "version",
        align: "start",
        sortable: true,
      },
      {
        title: "Erstellt am",
        key: "createdOn",
        align: "start",
        sortable: true,
      },
      {
        title: "Erstellt von",
        key: "createdBy",
        align: "start",
        sortable: true,
      },
      {
        title: "Beschreibung",
        key: "description",
        align: "start",
        sortable: true,
      },
      {
        title: "Ansicht",
        key: "viewVersion",
        align: "start",
        sortable: false,
      },
      {
        title: "Download",
        key: "downloadVersion",
        align: "start",
        sortable: false,
      },
    ];
    const oldVersions = ref([] as Array<OldGroupVersionRest>)
    const showDialogOldVersion = ref(false);
    const oldVersion = ref({} as GroupRest);

    const showVersion = (version: number) => {
      api
          .getGroupById(groupTmp.value.groupId, version)
          .then((receivedGroup: GroupRest) => {
            oldVersion.value = receivedGroup;
            showDialogOldVersion.value = true;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              saveAlertErrorMessage.value = errMsg;
              saveAlertError.value = true;
            });
          });
    };

    const downloadVersion = (version: number) => {
      api
          .getGroupById(groupTmp.value.groupId, version)
          .then((receivedGroup: GroupRest) => {
            oldVersion.value = receivedGroup;

            let text = receivedGroup.allowedAddresses == undefined ? '' : unparse(receivedGroup.allowedAddresses);
            let filename = 'version.csv';
            let element = document.createElement('a');
            element.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(text));
            element.setAttribute('download', filename);


            element.style.display = 'none';
            document.body.appendChild(element);

            element.click();
            document.body.removeChild(element);
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              saveAlertErrorMessage.value = errMsg;
              saveAlertError.value = true;
            });
          });
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
        formState.title = "";
        formState.ipAddressesText = "";
        formState.ipAddressesFile = undefined;
        formState.description = "";
      } else {
        formState.groupId = groupTmp.value.groupId;
        formState.title = groupTmp.value.title;
        formState.ipAddressesText = groupTmp.value.allowedAddressesRaw;
        formState.ipAddressesFile = undefined;
        formState.description = groupTmp.value.description;
        hasNoCSVHeader.value = !groupTmp.value.hasCSVHeader;
        if (groupTmp.value.oldVersions != undefined) {
          oldVersions.value = groupTmp.value.oldVersions;
        }
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
          .then((gIdC: GroupIdCreated) => {
            formState.groupId = gIdC.groupId;
            emit("addGroupSuccessful", gIdC.groupId);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              saveAlertErrorMessage.value = errMsg;
              saveAlertError.value = true;
            });
          });
    };

    const updateGroup = () => {
      api
          .updateGroup(groupTmp.value)
          .then(() => {
            emit("updateGroupSuccessful", groupTmp.value.groupId);
            close();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              saveAlertErrorMessage.value = errMsg;
              saveAlertError.value = true;
            });
          });
    };

    const save = () => {
      return v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        groupTmp.value.hasCSVHeader = !hasNoCSVHeader.value;
        groupTmp.value.title = formState.title;
        groupTmp.value.description = formState.description;
        if (
            formState.ipAddressesText == "" &&
            formState.ipAddressesFile != undefined
        ) {
          formState.ipAddressesFile
              .text()
              .then((r) => {
                groupTmp.value.allowedAddressesRaw = r;
                if (props.isNew) {
                  createGroup();
                } else {
                  updateGroup();
                }
              })
              .catch((e) => {
                error.errorHandling(e, (errMsg: string) => {
                  saveAlertError.value = true;
                  saveAlertErrorMessage.value = errMsg;
                });
              });
          return;
        }
        groupTmp.value.allowedAddressesRaw = formState.ipAddressesText;
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
      groupTmp.value.title = formState.title;
      dialogStore.groupDeleteActivated = true;
    };

    const deleteGroupSuccessful = () => {
      emit("deleteGroupSuccessful");
      close();
    };

    // Changes
    const formWasChanged = computed(() => {
      return formState.title != (groupTmp.value.title == undefined ? "" : groupTmp.value.title) ||
          formState.description != (groupTmp.value.description == undefined ? "" : groupTmp.value.description) ||
          formState.ipAddressesText != (groupTmp.value.allowedAddressesRaw == undefined ? "" : groupTmp.value.allowedAddressesRaw) ||
          formState.ipAddressesFile != undefined
    });

    const unsavedChangesDialog = ref(false);
    const checkForChangesAndClose = () => {
      if(formWasChanged.value){
        unsavedChangesDialog.value = true;
      } else {
        close();
      }
    };

    const closeUnsavedChangesDialog = () => {
      unsavedChangesDialog.value = false;
    };

    const loginStatusProps = computed(() => {
      if (!userStore.isLoggedIn) {
        return {
          "readonly": true,
          "bg-color": "grey-lighten-2",
        };
      } else {
        return {
          "bg-color": "white",
          "clearable" : true,
        };
      }
    });

    return {
      unsavedChangesDialog,
      computedGroup,
      dialogStore,
      dialogTitle,
      errorName,
      errorIpAddresses,
      fileContent,
      formState,
      formWasChanged,
      groupTmp,
      hasNoCSVHeader,
      headersVersion,
      loginStatusProps,
      oldVersion,
      oldVersions,
      saveAlertError,
      saveAlertErrorMessage,
      showDialogOldVersion,
      userStore,
      v$,
      checkForChangesAndClose,
      close,
      closeUnsavedChangesDialog,
      createGroup,
      deleteGroupSuccessful,
      downloadVersion,
      initiateDeleteDialog,
      save,
      showVersion,
      updateGroup,
    };
  },
});
</script>

<style scoped>
.my-scroll {
  height: calc(100vh - 200px);
  overflow-y: auto;
}
</style>

<template>
  <v-card class="my-scroll" position="relative">
    <v-toolbar>
      <v-spacer></v-spacer>
      <v-btn
          icon="mdi-close"
          @click="checkForChangesAndClose"
      ></v-btn>
    </v-toolbar>
    <v-dialog
        v-model="showDialogOldVersion"
        max-width="1000px"
        max-height="850px"
        :retain-focus="false"
    >
      <v-card>
        <v-card-title>IP Gruppen für Version {{oldVersion.version}}</v-card-title>
        <v-card-text>
          <v-textarea
              label="IP-Adressen"
              v-model="oldVersion.allowedAddressesRaw"
              variant="outlined"
          ></v-textarea>
        </v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog v-model="unsavedChangesDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5 text-center">Hinweis</v-card-title>
        <v-card-text class="text-center">
          Änderungen wurden noch nicht gespeichert!
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
              @click="closeUnsavedChangesDialog"
              color="blue darken-1"
          >Abbrechen
          </v-btn>
          <v-btn
              color="error"
              @click="close">
            Änderungen verwerfen
          </v-btn>
          <v-spacer></v-spacer>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-card-title>{{ dialogTitle }}
    </v-card-title>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn
          density="compact"
          icon="mdi-help"
          href="https://zbwintern/wiki/display/stba/03_IP-Gruppen"
          target="_blank"
      ></v-btn>
    </v-card-actions>
    <v-card-text style="height:1100px;">
      <v-snackbar
          v-model="saveAlertError"
          closable
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          color="error">
        {{ saveAlertErrorMessage }}
      </v-snackbar>
      <v-dialog v-model="dialogStore.groupDeleteActivated" max-width="700px">
        <GroupDeleteDialog
            :group-id="groupTmp.groupId"
            v-on:deleteGroupSuccessful="deleteGroupSuccessful"
        ></GroupDeleteDialog>
      </v-dialog>
      <v-row>
        <v-col cols="4">Name</v-col>
        <v-col cols="8">
          <v-text-field
              v-if="isNew"
              variant="outlined"
              hint="Name der Berechtigungsgruppe"
              v-model="formState.title"
              bg-color="white"
              :error-messages="errorName"
          ></v-text-field>
          <v-text-field
              v-if="!isNew"
              bg-color="grey-lighten-2"
              readonly
              variant="outlined"
              hint="Name der Berechtigungsgruppe"
              v-model="formState.title"
              :error-messages="errorName"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4"> Erstellt am</v-col>
        <v-col cols="8">
          <v-text-field
              v-model="computedGroup.createdOn"
              variant="outlined"
              readonly
              bg-color="grey-lighten-2"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4"> Erstellt von</v-col>
        <v-col cols="8">
          <v-text-field
              v-model="computedGroup.createdBy"
              variant="outlined"
              readonly
              bg-color="grey-lighten-2"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4">Zuletzt editiert am</v-col>
        <v-col cols="8">
          <v-text-field
              v-model="computedGroup.lastUpdatedOn"
              variant="outlined"
              readonly
              bg-color="grey-lighten-2"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row>
        <v-col cols="4">Zuletzt editiert von</v-col>
        <v-col cols="8">
          <v-text-field
              v-model="computedGroup.lastUpdatedBy"
              variant="outlined"
              readonly
              bg-color="grey-lighten-2"
          ></v-text-field>
        </v-col>
      </v-row>

      <v-row>
        Berechtigte IP-Adress-Bereiche
      </v-row>
      <v-card
          class="mb-12 mt-6"
          color="surface-variant"
          variant="tonal"
      >
        <v-card-text class="text-medium-emphasis text-caption">
          Eine Neue Gruppe kann angelegt werden, indem die IP-Bereiche manuell
          hier eingegeben werden oder indem die entsprechenden IP-Bereiche per
          CSV-Datei hochgeladen werden. Beides gleichzeitig ist nicht möglich. Der
          Freitext hat höhere Priorität. Das erwartete CSV Format ist:
          "IPAdresse1,IPAdresse2,...,IPAdresseN;Organisation;"
        </v-card-text>
      </v-card>
      <v-row>
        <v-col cols="5">
          <v-textarea
              label="IP-Adressen"
              v-model="formState.ipAddressesText"
              :error-messages="errorIpAddresses"
              v-bind="{...$attrs, ...loginStatusProps}"
              variant="outlined"
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
              bg-color="white"
              accept=".csv"
              label="CSV-Datei"
              v-model="formState.ipAddressesFile"
              :error-messages="errorIpAddresses"
              v-bind="{...$attrs, ...loginStatusProps}"
              variant="outlined"
          ></v-file-input>
          Hinweis: Es kann nur eine CSV-Datei pro Gruppe hinterlegt werden.
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          <v-textarea
              label="Beschreibung"
              v-model="formState.description"
              variant="outlined"
              bg-color="white"
              v-bind="{...$attrs, ...loginStatusProps}"
          ></v-textarea>
        </v-col>
      </v-row>
      <v-expansion-panels bg-color="light-blue-lighten-5" focusable multiple>
        <v-expansion-panel>
          <v-expansion-panel-title>Historie</v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-container fluid>
              <v-data-table
                  :headers="headersVersion"
                  :items="oldVersions"
                  loading-text="Daten werden geladen... Bitte warten."
              >
                <template v-slot:item.viewVersion="{ item }">
                  <v-btn
                      variant="text"
                      icon="mdi-eye"
                  >
                    <v-icon small @click="showVersion(item.version)">mdi-eye
                    </v-icon>
                  </v-btn>
                </template>
                <template v-slot:item.downloadVersion="{ item }">
                  <v-btn
                      variant="text"
                      icon="mdi-download-outline"
                  >
                    <v-icon small @click="downloadVersion(item.version)">mdi-download-outline
                    </v-icon>
                  </v-btn>
                </template>
              </v-data-table>
            </v-container>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </v-expansion-panels>
    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn
          @click="save"
          color="blue darken-1"
          :disabled="!userStore.isLoggedIn"
      >
        Speichern
      </v-btn>
      <v-btn
          :disabled="!userStore.isLoggedIn"
          v-if="!isNew"
          @click="initiateDeleteDialog"
      >
        <v-icon>mdi-delete</v-icon>
      </v-btn>
      <v-btn v-if="isNew" disabled>
        <v-icon>mdi-delete</v-icon>
      </v-btn>
    </v-card-actions>
  </v-card>
</template>
