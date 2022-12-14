<script lang="ts">
import api from "@/api/api";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import {
  ItemEntry,
  RightRest,
  AccessStateRest,
  RightRestBasisAccessStateEnum,
  RightRestBasisStorageEnum,
} from "@/generated-sources/openapi";
import {
  computed,
  onMounted,
  reactive,
  ref,
  watch,
  defineComponent,
  PropType,
} from "vue";

import { useVuelidate } from "@vuelidate/core";
import { required } from "@vuelidate/validators";
import {ChangeType, useHistoryStore} from "@/stores/history";

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
    // Stores
    const historyStore = useHistoryStore();
    // Vuelidate
    type FormState = {
      accessState: string;
      startDate: string;
      endDate: string;
    };

    const formState = reactive({
      accessState: "",
      basisStorage: "",
      basisAccessState: "",
      startDate: "",
      endDate: "",
    });

    const endDateCheck = (value: string, siblings: FormState) => {
      if (value == "") {
        return true;
      } else {
        const endDate = new Date(value);
        const startDate = new Date(siblings.startDate);
        return startDate < endDate;
      }
    };

    const rules = {
      accessState: { required },
      startDate: { required },
      endDate: { endDateCheck },
    };

    const v$ = useVuelidate(rules, formState);

    const errorAccessState = computed(() => {
      const errors: Array<string> = [];
      if (
        v$.value.accessState.required.$invalid &&
        v$.value.accessState.$dirty
      ) {
        errors.push("Eintrag wird benötigt");
      }
      return errors;
    });
    const errorStartDate = computed(() => {
      const errors: Array<string> = [];
      if (v$.value.startDate.required.$invalid && v$.value.startDate.$dirty) {
        errors.push("Eintrag wird benötigt");
      }
      return errors;
    });
    const errorEndDate = computed(() => {
      const errors: Array<string> = [];
      if (v$.value.endDate.$invalid) {
        errors.push("Enddatum muss nach dem Startdatum liegen");
      }
      return errors;
    });

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
    const menuEndDate = ref(false);
    const menuStartDate = ref(false);
    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");
    const updateConfirmDialog = ref(false);
    const updateInProgress = ref(false);
    const metadataCount = ref(0);
    const tmpRight = ref({} as RightRest);

    const emitClosedDialog = () => {
      emit("editDialogClosed");
    };

    const close = () => {
      updateConfirmDialog.value = false;
      updateInProgress.value = false;
      v$.value.$reset();
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
      tmpRight.value.startDate = new Date(formState.startDate);
      tmpRight.value.endDate =
        formState.endDate == "" ? undefined : new Date(formState.endDate);
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
              historyStore.addEntry({
                type: ChangeType.CREATED,
                rightId: r.rightId,
              });
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
      tmpRight.value.startDate = new Date(formState.startDate);
      tmpRight.value.endDate =
        formState.endDate == "" ? undefined : new Date(formState.endDate);
      api
        .updateRight(tmpRight.value)
        .then(() => {
          historyStore.addEntry({
            type: ChangeType.UPDATED,
            rightId: tmpRight.value.rightId,
          });
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

    const save: () => Promise<void> = () => {
      return v$.value.$validate().then((isValid) => {
        if (!isValid) {
          return;
        }
        tmpRight.value.accessState = stringToAccessState(formState.accessState);
        tmpRight.value.basisStorage = stringToBasisStorage(
          formState.basisStorage
        );
        tmpRight.value.basisAccessState = stringToBasisAccessState(
          formState.basisAccessState
        );
        if (props.isNew) {
          createRight();
        } else {
          updateRight();
        }
      });
    };

    const initiateDeleteDialog = () => {
      deleteDialogActivated.value = true;
    };

    const accessStateToString = (
      access: AccessStateRest | undefined
    ) => {
      if (access == undefined) {
        return "Kein Wert";
      } else {
        switch (access) {
          case AccessStateRest.Open:
            return "Open";
          case AccessStateRest.Closed:
            return "Closed";
          default:
            return "Restricted";
        }
      }
    };

    const stringToAccessState = (value: string | undefined) => {
      if (value == undefined || value == "Kein Wert") {
        return;
      } else {
        switch (value) {
          case "Open":
            return AccessStateRest.Open;
          case "Closed":
            return AccessStateRest.Closed;
          default:
            return AccessStateRest.Restricted;
        }
      }
    };

    const basisStorageToString = (
      basisStorage: RightRestBasisStorageEnum | undefined
    ) => {
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
    };

    const stringToBasisStorage = (value: string | undefined) => {
      if (value == undefined) {
        return undefined;
      } else {
        switch (value) {
          case "Lizenzvertrag":
            return RightRestBasisStorageEnum.LicenceContract;
          case "Nutzungsvereinbarung":
            return RightRestBasisStorageEnum.UserAgreement;
          case "Urheberrechtschranke":
            return RightRestBasisStorageEnum.AuthorRightException;
          case "Open-Content-Lizenz":
            return RightRestBasisStorageEnum.OpenContentLicence;
          case "ZBW-Policy (Eingeschränkte OCL)":
            return RightRestBasisStorageEnum.ZbwPolicyRestricted;
          default:
            return RightRestBasisStorageEnum.ZbwPolicyUnanswered;
        }
      }
    };

    const basisAccessStateToString = (
      basisAccessState: RightRestBasisAccessStateEnum | undefined
    ) => {
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
    };

    const stringToBasisAccessState = (value: string | undefined) => {
      if (value == undefined) {
        tmpRight.value.basisAccessState = undefined;
      } else {
        switch (value) {
          case "Lizenzvertrag":
            return RightRestBasisAccessStateEnum.LicenceContract;
          case "Nutzungsvereinbarung":
            return RightRestBasisAccessStateEnum.UserAgreement;
          case "OA-Rechte aus Lizenzvertrag":
            return RightRestBasisAccessStateEnum.LicenceContractOa;
          case "Urheberrechtschranke":
            return RightRestBasisAccessStateEnum.AuthorRightException;
          default:
            return RightRestBasisAccessStateEnum.ZbwPolicy;
        }
      }
    };

    // Computed properties
    const title = computed(() => {
      if (props.isNew) {
        return "Erstelle";
      } else {
        return "Editiere";
      }
    });

    onMounted(() => reinitializeRight(props.right));
    const computedRight = computed(() => props.right);

    watch(computedRight, (currentValue, oldValue) => {
      reinitializeRight(currentValue);
    });

    const reinitializeRight = (newValue: RightRest) => {
      updateInProgress.value = false;
      tmpRight.value = Object.assign({}, newValue);
      if (!props.isNew) {
        formState.accessState = accessStateToString(newValue.accessState);
        formState.basisStorage = basisStorageToString(newValue.basisStorage);
        formState.basisAccessState = basisAccessStateToString(
          newValue.basisAccessState
        );
        formState.startDate = newValue.startDate.toISOString().slice(0, 10);
        if (newValue.endDate !== undefined) {
          formState.endDate = newValue.endDate.toISOString().slice(0, 10);
        } else {
          formState.endDate = "";
        }
      } else {
        formState.endDate = "";
        formState.startDate = "";
      }
    };

    return {
      formState,
      v$,
      // variables
      accessStatusSelect,
      basisAccessState,
      basisStorage,
      deleteDialogActivated,
      errorAccessState,
      errorEndDate,
      errorStartDate,
      historyStore,
      menuStartDate,
      menuEndDate,
      metadataCount,
      openPanelsDefault,
      saveAlertError,
      saveAlertErrorMessage,
      updateConfirmDialog,
      title,
      tmpRight,
      updateInProgress,
      // methods
      cancel,
      cancelConfirm,
      createRight,
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

      <v-dialog
        v-model="deleteDialogActivated"
        max-width="500px"
        :retain-focus="false"
      >
        <RightsDeleteDialog
          :right="right"
          :index="index"
          :metadataId="metadataId"
          v-on:deleteSuccessful="deleteSuccessful"
          v-on:deleteDialogClosed="deleteDialogClosed"
        ></RightsDeleteDialog>
      </v-dialog>
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
                  v-model="formState.accessState"
                  outlined
                  @blur="v$.accessState.$touch()"
                  @change="v$.accessState.$touch()"
                  :error-messages="errorAccessState"
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
                  :return-value.sync="formState.startDate"
                  transition="scale-transition"
                  offset-y
                  min-width="auto"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      v-model="formState.startDate"
                      ref="startDate"
                      label="Start-Datum"
                      prepend-icon="mdi-calendar"
                      readonly
                      outlined
                      v-bind="attrs"
                      v-on="on"
                      required
                      @change="v$.startDate.$touch()"
                      @blur="v$.startDate.$touch()"
                      :error-messages="errorStartDate"
                    ></v-text-field>
                  </template>
                  <v-date-picker
                    v-model="formState.startDate"
                    no-title
                    scrollable
                  >
                    <v-spacer></v-spacer>
                    <v-btn text color="primary" @click="menuStartDate = false">
                      Cancel
                    </v-btn>
                    <v-btn
                      text
                      color="primary"
                      @click="$refs.menuStart.save(formState.startDate)"
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
                  :return-value.sync="formState.endDate"
                  transition="scale-transition"
                  offset-y
                  min-width="auto"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      v-model="formState.endDate"
                      ref="endDate"
                      label="End-Datum"
                      prepend-icon="mdi-calendar"
                      readonly
                      outlined
                      v-bind="attrs"
                      v-on="on"
                      required
                      @change="v$.endDate.$touch()"
                      @blur="v$.endDate.$touch()"
                      :error-messages="errorEndDate"
                    ></v-text-field>
                  </template>
                  <v-date-picker
                    v-model="formState.endDate"
                    no-title
                    scrollable
                  >
                    <v-spacer></v-spacer>
                    <v-btn text color="primary" @click="menuEndDate = false">
                      Cancel
                    </v-btn>
                    <v-btn
                      text
                      color="primary"
                      @click="$refs.menuEnd.save(formState.endDate)"
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
                  v-model="formState.basisStorage"
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
                  v-model="formState.basisAccessState"
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
