<script lang="ts">
import api from "@/api/api";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import {
  ItemEntry,
  RightRest,
  RightRestAccessStateEnum,
  RightRestBasisAccessStateEnum,
  RightRestBasisStorageEnum,
} from "@/generated-sources/openapi";
import {
  computed,
  onMounted,
  ref,
  watch,
  defineComponent,
  PropType,
} from "vue";

export default defineComponent({
  props: {
    right: {
      type: {} as PropType<RightRest>,
      required: true,
    },
    index: {
      type: Number,
      required: true,
    },
    isNew: {
      type: Boolean,
      required: true,
    },
    metadataId: {
      type: String,
      required: true,
    },
  },
  // Emits
  emits: [
    "addSuccessful",
    "deleteSuccessful",
    "editDialogClosed",
    "updateSuccessful",
  ],

  // Components
  components: {
    RightsDeleteDialog,
  },

  setup(props, { emit }) {
    const openPanelsDefault = [0];
    const accessStatusSelect = ["Open", "Closed", "Restricted"];
    const basisAccessState = ref([
      "Lizenzvertrag",
      "OA-Rechte aus Lizenzvertrag",
      "Nutzungsvereinbarung",
      "Urheberrechtschranke",
      "ZBW-Policy",
    ]);
    const basisStorage = ref([
      "Lizenzvertrag",
      "Nutzungsvereinbarung",
      "Urheberrechtschranke",
      "Open-Content-Lizenz",
      "ZBW-Policy (Eingeschränkte OCL)",
      "ZBW-Policy (unbeantwortete Rechteanforderung)",
    ]);
    const deleteDialogActivated = ref(false);
    const formHasErrors = ref(false);
    const menuEndDate = ref(false);
    const menuStartDate = ref(false);
    const tmpStartDate = ref("");
    const tmpEndDate = ref("");
    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");
    const updateConfirmDialog = ref(false);
    const updateInProgress = ref(false);
    const metadataCount = ref(0);
    const tmpRight = ref({} as RightRest);
    const rules = {
      required: (value: string) => {
        return !!value || "Benötigt.";
      },
      maxLength256: (value: string) => {
        if (value == undefined) {
          return true;
        } else {
          return value.length <= 256 || "Max 256 Zeichen";
        }
      },
    };

    const emitClosedDialog = () => {
      emit("editDialogClosed");
    };

    const close = () => {
      updateConfirmDialog.value = false;
      updateInProgress.value = false;
      emitClosedDialog();
    };

    const cancel = () => {
      tmpRight.value = Object.assign({}, props.right);
      close();
    };

    const cancelConfirm = () => {
      updateConfirmDialog.value = false;
    };

    const deleteSuccessful = (index: number) => {
      emit("deleteSuccessful", index, props.right.rightId);
    };

    const deleteDialogClosed = () => {
      deleteDialogActivated.value = false;
    };

    const createRight = () => {
      updateInProgress.value = true;
      tmpRight.value.rightId = "unset";
      tmpRight.value.startDate = new Date(tmpStartDate.value);
      tmpRight.value.endDate =
        tmpEndDate.value == "" ? undefined : new Date(tmpEndDate.value);
      api
        .addRight(tmpRight.value)
        .then((r) => {
          api
            .addItemEntry({
              metadataId: props.metadataId,
              rightId: r.rightId,
            } as ItemEntry)
            .then(() => {
              tmpRight.value.rightId = r.rightId;
              emit("addSuccessful", tmpRight.value);
              close();
            })
            .catch((e) => {
              console.log(e);
              saveAlertError.value = true;
              saveAlertErrorMessage.value =
                e.statusText + " (Statuscode: " + e.status + ")";
              updateConfirmDialog.value = false;
            });
        })
        .catch((e) => {
          console.log(e);
          saveAlertError.value = true;
          saveAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          updateConfirmDialog.value = false;
        });
    };

    const updateRight = () => {
      updateInProgress.value = true;
      tmpRight.value.startDate = new Date(tmpStartDate.value);
      tmpRight.value.endDate =
        tmpEndDate.value == "" ? undefined : new Date(tmpEndDate.value);
      api
        .updateRight(tmpRight.value)
        .then(() => {
          emit("updateSuccessful", tmpRight.value, props.index);
        })
        .catch((e) => {
          console.log(e);
          saveAlertError.value = true;
          saveAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          updateConfirmDialog.value = false;
        });
    };

    const save = () => {
      // TODO(vuelidate)
      if (formHasErrors.value) {
        return;
      }
      if (props.isNew) {
        createRight();
      } else {
        updateRight();
      }
    };

    const initiateDeleteDialog = () => {
      deleteDialogActivated.value = true;
    };

    // Computed properties
    const form = computed(() => {
      return {
        accessState: tmpRight.value.accessState,
        basisAccessState: tmpRight.value.accessState,
        basisStorage: tmpRight.value.basisStorage,
        startDate: tmpRight.value.startDate,
        endDate: tmpRight.value.endDate,
        notesFormalRules: tmpRight.value.notesFormalRules,
        notesGeneral: tmpRight.value.notesGeneral,
        notesProcessDocumentation: tmpRight.value.notesProcessDocumentation,
        notesManagementRelated: tmpRight.value.notesManagementRelated,
        licenceContract: tmpRight.value.licenceContract,
      };
    });

    const showAccessState = computed({
      get: () => {
        let accessState = tmpRight.value.accessState;
        if (accessState == undefined) {
          return "Kein Wert";
        } else {
          switch (accessState) {
            case RightRestAccessStateEnum.Open:
              return "Open";
            case RightRestAccessStateEnum.Closed:
              return "Closed";
            default:
              return "Restricted";
          }
        }
      },
      set: (value: string | undefined) => {
        if (value == undefined || value == "Kein Wert") {
          tmpRight.value.accessState = undefined;
        } else {
          switch (value) {
            case "Open":
              tmpRight.value.accessState = RightRestAccessStateEnum.Open;
              break;
            case "Closed":
              tmpRight.value.accessState = RightRestAccessStateEnum.Closed;
              break;
            default:
              tmpRight.value.accessState = RightRestAccessStateEnum.Restricted;
              break;
          }
        }
      },
    });

    const selectBasisStorage = computed({
      get: () => {
        let basisStorage = tmpRight.value.basisStorage;
        if (basisStorage == undefined) {
          return "Kein Wert";
        } else {
          switch (basisStorage) {
            case RightRestBasisStorageEnum.AuthorRightException:
              return "Urheberrechtschranke";
            case RightRestBasisStorageEnum.UserAgreement:
              return "Nutzungsvereinbarung";
            case RightRestBasisStorageEnum.OpenContentLicence:
              return "Open-Content-Lizenz";
            case RightRestBasisStorageEnum.ZbwPolicyUnanswered:
              return "ZBW-Policy (unbeantwortete Rechteanforderung)";
            case RightRestBasisStorageEnum.ZbwPolicyRestricted:
              return "ZBW-Policy (Eingeschränkte OCL)";
            default:
              return "Lizenzvertrag";
          }
        }
      },
      set: (value: string | undefined) => {
        if (value == undefined) {
          tmpRight.value.basisStorage = undefined;
        } else {
          switch (value) {
            case "Lizenzvertrag":
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.LicenceContract;
              break;
            case "Nutzungsvereinbarung":
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.UserAgreement;
              break;
            case "Urheberrechtschranke":
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.AuthorRightException;
              break;
            case "Open-Content-Lizenz":
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.OpenContentLicence;
              break;
            case "ZBW-Policy (Eingeschränkte OCL)":
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.ZbwPolicyRestricted;
              break;
            default:
              tmpRight.value.basisStorage =
                RightRestBasisStorageEnum.ZbwPolicyUnanswered;
              break;
          }
        }
      },
    });

    const selectBasisAccessState = computed({
      get: () => {
        let basisAccessState = tmpRight.value.basisAccessState;
        if (basisAccessState == undefined) {
          return "Kein Wert";
        } else {
          switch (basisAccessState) {
            case RightRestBasisAccessStateEnum.AuthorRightException:
              return "Urheberrechtschranke";
            case RightRestBasisAccessStateEnum.UserAgreement:
              return "Nutzungsvereinbarung";
            case RightRestBasisAccessStateEnum.LicenceContract:
              return "Lizenzvertrag";
            case RightRestBasisAccessStateEnum.ZbwPolicy:
              return "ZBW-Policy";
            default:
              return "OA-Rechte aus Lizenzvertrag";
          }
        }
      },
      set: (value: string | undefined) => {
        if (value == undefined) {
          tmpRight.value.basisAccessState = undefined;
        } else {
          switch (value) {
            case "Lizenzvertrag":
              tmpRight.value.basisAccessState =
                RightRestBasisAccessStateEnum.LicenceContract;
              break;
            case "Nutzungsvereinbarung":
              tmpRight.value.basisAccessState =
                RightRestBasisAccessStateEnum.UserAgreement;
              break;
            case "OA-Rechte aus Lizenzvertrag":
              tmpRight.value.basisAccessState =
                RightRestBasisAccessStateEnum.LicenceContractOa;
              break;
            case "Urheberrechtschranke":
              tmpRight.value.basisAccessState =
                RightRestBasisAccessStateEnum.AuthorRightException;
              break;
            default:
              tmpRight.value.basisAccessState =
                RightRestBasisAccessStateEnum.ZbwPolicy;
              break;
          }
        }
      },
    });

    const title = computed(() => {
      if (props.isNew) {
        return "Erstelle";
      } else {
        return "Editiere";
      }
    });

    onMounted(() => reinitializeRight(props.right));

    watch(props.right, (currentValue, oldValue) => {
      reinitializeRight(currentValue);
    });

    const reinitializeRight = (newValue: RightRest) => {
      updateInProgress.value = false;
      tmpRight.value = Object.assign({}, newValue);
      if (!props.isNew) {
        tmpStartDate.value = props.right.startDate.toISOString().slice(0, 10);
        if (props.right.endDate !== undefined) {
          tmpEndDate.value = props.right.endDate.toISOString().slice(0, 10);
        } else {
          tmpEndDate.value = "";
        }
      } else {
        tmpEndDate.value = "";
        tmpStartDate.value = "";
      }
    };
    return {
      // variables
      accessStatusSelect,
      basisAccessState,
      basisStorage,
      deleteDialogActivated,
      menuStartDate,
      menuEndDate,
      metadataCount,
      openPanelsDefault,
      saveAlertError,
      saveAlertErrorMessage,
      selectBasisAccessState,
      selectBasisStorage,
      showAccessState,
      updateConfirmDialog,
      title,
      tmpRight,
      tmpEndDate,
      tmpStartDate,
      updateInProgress,
      // methods
      cancel,
      cancelConfirm,
      initiateDeleteDialog,
      deleteDialogClosed,
      deleteSuccessful,
      updateRight,
      save,
    };
  },
});
</script>

<style scoped></style>

<template>
  <v-card>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn
        color="blue darken-1"
        text
        @click="save"
        :disabled="updateInProgress"
        >Speichern
      </v-btn>
      <v-btn color="blue darken-1" text @click="cancel">Zurück</v-btn>
      <v-btn icon @click="initiateDeleteDialog">
        <v-icon>mdi-delete</v-icon>
      </v-btn>
      <RightsDeleteDialog
        :activated="deleteDialogActivated.value"
        :right="right"
        :index="index"
        :metadataId="metadataId"
        v-on:deleteSuccessful="deleteSuccessful"
        v-on:deleteDialogClosed="deleteDialogClosed"
      ></RightsDeleteDialog>
    </v-card-actions>
    <v-expansion-panels focusable multiple v-model="openPanelsDefault">
      <v-expansion-panel>
        <v-expansion-panel-header
          >Steuerungsrelevante Elemente</v-expansion-panel-header
        >
        <v-expansion-panel-content eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4">
                <v-subheader>Right-Id</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-if="isNew"
                  ref="rightId"
                  label="Wird automatisch generiert"
                  disabled
                  outlined
                  hint="Rechte Id"
                ></v-text-field>
                <v-text-field
                  v-if="!isNew"
                  ref="rightId"
                  v-model="tmpRight.rightId"
                  outlined
                  hint="Rechte Id"
                  disabled
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Aktueller Access-Status</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  :items="accessStatusSelect"
                  v-model="showAccessState"
                  outlined
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Gültigkeit Startdatum</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-menu
                  ref="menuStart"
                  v-model="menuStartDate"
                  :close-on-content-click="false"
                  :return-value.sync="tmpStartDate"
                  transition="scale-transition"
                  offset-y
                  min-width="auto"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      v-model="tmpStartDate"
                      ref="startDate"
                      label="Start-Datum"
                      prepend-icon="mdi-calendar"
                      readonly
                      outlined
                      v-bind="attrs"
                      v-on="on"
                      required
                    ></v-text-field>
                  </template>
                  <v-date-picker v-model="tmpStartDate" no-title scrollable>
                    <v-spacer></v-spacer>
                    <v-btn text color="primary" @click="menuStartDate = false">
                      Cancel
                    </v-btn>
                    <v-btn
                      text
                      color="primary"
                      @click="$refs.menuStart.save(tmpStartDate)"
                    >
                      OK
                    </v-btn>
                  </v-date-picker>
                </v-menu>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Gültigkeit Enddatum</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-menu
                  ref="menuEnd"
                  v-model="menuEndDate"
                  :close-on-content-click="false"
                  :return-value.sync="tmpEndDate"
                  transition="scale-transition"
                  offset-y
                  min-width="auto"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      v-model="tmpEndDate"
                      ref="endDate"
                      label="End-Datum"
                      prepend-icon="mdi-calendar"
                      readonly
                      outlined
                      v-bind="attrs"
                      v-on="on"
                      required
                    ></v-text-field>
                  </template>
                  <v-date-picker v-model="tmpEndDate" no-title scrollable>
                    <v-spacer></v-spacer>
                    <v-btn text color="primary" @click="menuEndDate = false">
                      Cancel
                    </v-btn>
                    <v-btn
                      text
                      color="primary"
                      @click="$refs.menuEnd.save(tmpEndDate)"
                    >
                      OK
                    </v-btn>
                  </v-date-picker>
                </v-menu>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Group</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  outlined
                  hint="Einschränkung des Zugriffs auf eine Berechtigungsgruppe"
                  counter
                  maxlength="256"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Bemerkungen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesGeneral"
                  hint="Allgemeine Bemerkungen"
                  counter
                  maxlength="256"
                  outlined
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>Formale Regelung</v-expansion-panel-header>
        <v-expansion-panel-content eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4">
                <v-subheader>Lizenzvertrag</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.licenceContract"
                  outlined
                  hint="Gibt Auskunft darüber, ob ein Lizenzvertrag für dieses Item als Nutzungsrechtsquelle vorliegt."
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Urheberrechtschrankennutzung</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.authorRightException"
                  color="indigo"
                  label="Ja"
                  hint="Ist für die ZBW die Nutzung der Urheberrechtschranken möglich?"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>ZBW Nutzungsvereinbarung</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.zbwUserAgreement"
                  color="indigo"
                  label="Ja"
                  hint="Gibt Auskunft darüber, ob eine Nutzungsvereinbarung für dieses Item als Nutzungsrechtsquelle vorliegt."
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Open-Content-Licence</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  outlined
                  hint="Eine per URI eindeutig referenzierte Standard-Open-Content-Lizenz, die für das Item gilt."
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>
                  Nicht-standardisierte Open-Content-Lizenz (URL)
                </v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.nonStandardOpenContentLicenceURL"
                  outlined
                  hint="Eine per URL eindeutig referenzierbare Nicht-standardisierte Open-Content-Lizenz, die für das Item gilt."
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader
                  >Nicht-standardisierte Open-Content-Lizenz (keine
                  URL)</v-subheader
                >
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.nonStandardOpenContentLicence"
                  color="indigo"
                  label="Ja"
                  hint="Ohne URL, als Freitext (bzw. derzeit als Screenshot in Clearingstelle)"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Eingeschränkte Open-Content-Lizenz</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.restrictedOpenContentLicence"
                  color="indigo"
                  label="Ja"
                  hint="Gilt für dieses Item, dem im Element 'Open-Content-Licence' eine standardisierte Open-Content-Lizenz zugeordnet ist, eine Einschränkung?"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Bemerkungen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesFormalRules"
                  counter
                  maxlength="256"
                  hint="Bemerkungen für formale Regelungen"
                  outlined
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header
          >Prozessdokumentierende Elemente</v-expansion-panel-header
        >
        <v-expansion-panel-content eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4">
                <v-subheader>Basis der Speicherung</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  :items="basisStorage"
                  v-model="selectBasisStorage"
                  outlined
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Basis des Access-Status</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  :items="basisAccessState"
                  v-model="selectBasisAccessState"
                  outlined
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Bemerkungen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesProcessDocumentation"
                  hint="Bemerkungen für prozessdokumentierende Elemente"
                  counter
                  maxlength="256"
                  outlined
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Metadaten über den Rechteinformationseintrag
        </v-expansion-panel-header>
        <v-expansion-panel-content eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4">
                <v-subheader>Zuletzt editiert am</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedOn"
                  readonly
                  outlined
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Zuletzt editiert von</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedBy"
                  readonly
                  outlined
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Bemerkungen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesManagementRelated"
                  hint="Bemerkungen für Metadaten über den Rechteinformationseintrag"
                  counter
                  maxlength="256"
                  outlined
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn color="blue darken-1" text @click="cancel">Zurück</v-btn>
      <v-btn
        color="blue darken-1"
        text
        @click="save"
        :disabled="updateInProgress"
        >Speichern
      </v-btn>
    </v-card-actions>
    <v-alert v-model="saveAlertError" dismissible text type="error">
      Speichern war nicht erfolgreich:
      {{ saveAlertErrorMessage }}
    </v-alert>
    <v-dialog v-model="updateConfirmDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5"> Achtung</v-card-title>
        <v-card-text>
          {{ metadataCount - 1 }} andere Items verweisen ebenfalls auf diese
          Rechteinformation. Mit der Bestätigung wird die Rechteinformation an
          all diesen geändert. Bist du dir sicher?
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            :disabled="updateInProgress"
            color="blue darken-1"
            @click="cancelConfirm"
            >Abbrechen
          </v-btn>
          <v-btn :loading="updateInProgress" color="error" @click="updateRight">
            Update
          </v-btn>
          <v-spacer></v-spacer>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-card>
</template>
