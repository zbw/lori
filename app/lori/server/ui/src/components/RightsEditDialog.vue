<script lang="ts">
import api from "@/api/api";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import {
  AccessStateRest,
  BookmarkRest,
  GroupRest,
  ItemEntry,
  RightRest,
  RightRestBasisAccessStateEnum,
  RightRestBasisStorageEnum,
  TemplateIdCreated,
} from "@/generated-sources/openapi";
import {
  computed,
  defineComponent,
  onMounted,
  PropType,
  reactive,
  Ref,
  ref,
  watch,
} from "vue";

import { useVuelidate } from "@vuelidate/core";
import { required } from "@vuelidate/validators";
import { ChangeType, useHistoryStore } from "@/stores/history";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";
import TemplateBookmark from "@/components/TemplateBookmark.vue";
import isEqual from "lodash.isequal";
import { uniqWith } from "lodash";

export default defineComponent({
  props: {
    right: {
      type: {} as PropType<RightRest>,
      required: false, // Is not required because this component is used for creating new rights as well
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
      required: false,
    },
    reinitCounter: {
      type: Number,
      required: false,
    },
    initialBookmark: {
      type: {} as PropType<BookmarkRest>,
      required: false,
    },
  },
  // Emits
  emits: [
    "addSuccessful",
    "addTemplateSuccessful",
    "deleteTemplateSuccessful",
    "updateTemplateSuccessful",
    "deleteSuccessful",
    "editRightClosed",
    "updateSuccessful",
  ],

  // Components
  components: {
    TemplateBookmark,
    RightsDeleteDialog,
  },

  setup(props, { emit }) {
    // Stores
    const historyStore = useHistoryStore();
    /**
     * Vuelidate.
     */
    type FormState = {
      accessState: string;
      startDate: string;
      endDate: string;
      formTemplateName: string;
    };

    const formState = reactive({
      accessState: "",
      basisStorage: "",
      basisAccessState: "",
      startDate: "",
      endDate: "",
      formTemplateName: "",
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
      formTemplateName: { required },
    };

    const v$ = useVuelidate(rules, formState);

    /**
     * Vuelidate Errors:
     */
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
    const errorTemplateName = computed(() => {
      const errors: Array<string> = [];
      if (
        v$.value.formTemplateName.$invalid &&
        v$.value.formTemplateName.$dirty
      ) {
        errors.push("Es wird ein Template-Name benötigt.");
      }
      return errors;
    });

    /**
     * Template related:
     */
    const tmpTemplateId = ref(-1);
    const tmpTemplateDescription = ref("");

    /**
     * Constants:
     */
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
    const dialogDeleteRight = ref(false);
    const dialogDeleteTemplate = ref(false);
    const menuEndDate = ref(false);
    const menuStartDate = ref(false);
    const saveAlertError = ref(false);
    const saveAlertErrorMessage = ref("");
    const updateConfirmDialog = ref(false);
    const updateInProgress = ref(false);
    const metadataCount = ref(0);
    const tmpRight = ref({} as RightRest);

    const emitClosedDialog = () => {
      emit("editRightClosed");
    };

    const close = () => {
      generalAlertError.value = false;
      generalAlertErrorMessage.value = "";
      saveAlertError.value = false;
      saveAlertErrorMessage.value = "";
      updateConfirmDialog.value = false;
      updateInProgress.value = false;
      tmpTemplateDescription.value = "";
      formState.formTemplateName = "";
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

    /**
     * Deletion:
     */
    const deleteSuccessful = (index: number) => {
      if (isTemplate.value) {
        emit("deleteTemplateSuccessful", formState.formTemplateName);
      } else {
        if (props.right != undefined) {
          emit("deleteSuccessful", index, props.right.rightId);
        }
      }
    };

    const deleteDialogClosed = () => {
      if (isTemplate.value) {
        dialogDeleteTemplate.value = false;
        close();
      } else {
        dialogDeleteRight.value = false;
      }
    };

    const initiateDeleteDialog = () => {
      if (isTemplate.value) {
        dialogDeleteTemplate.value = true;
      } else {
        dialogDeleteRight.value = true;
      }
    };

    /**
     * Create/Update Right/Template:
     */
    const createRight = () => {
      tmpRight.value.rightId = "unset";
      api
        .addRight(tmpRight.value)
        .then((r) => {
          api
            .addItemEntry(
              {
                metadataId: props.metadataId,
                rightId: r.rightId,
              } as ItemEntry,
              true
            )
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
              error.errorHandling(e, (errMsg: string) => {
                saveAlertError.value = true;
                saveAlertErrorMessage.value = errMsg;
                updateConfirmDialog.value = false;
              });
            });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            saveAlertError.value = true;
            saveAlertErrorMessage.value = errMsg;
            updateConfirmDialog.value = false;
          });
        });
    };

    const updateRight = () => {
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
          error.errorHandling(e, (errMsg: string) => {
            saveAlertError.value = true;
            saveAlertErrorMessage.value = errMsg;
            updateConfirmDialog.value = false;
          });
        });
    };

    const createTemplate = () => {
      tmpRight.value.rightId = "unset";
      tmpRight.value.templateId = -1;
      tmpRight.value.templateName = formState.formTemplateName;
      tmpRight.value.templateDescription = tmpTemplateDescription.value;
      templateApi
        .addTemplate(tmpRight.value)
        .then((r: TemplateIdCreated) => {
          updateBookmarks(r.templateId, () => {
            emit("addTemplateSuccessful", formState.formTemplateName);
            close();
          });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMessage.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    const updateTemplate = () => {
      tmpRight.value.templateId = tmpTemplateId.value;
      tmpRight.value.templateName = formState.formTemplateName;
      tmpRight.value.templateDescription = tmpTemplateDescription.value;
      templateApi
        .updateTemplate(tmpRight.value)
        .then(() => {
          // TODO: refactor this with callbacks
          updateBookmarks(tmpTemplateId.value, () => {
            emit("updateTemplateSuccessful", formState.formTemplateName);
            close();
          });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMessage.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    /**
     * Delete and Create Bookmarks compared to last save:
     */
    const updateBookmarks = (templateId: number, callback: () => void) => {
      templateApi
        .addBookmarksByTemplateId(
          templateId,
          bookmarkItems.value
            .map((elem) => elem.bookmarkId)
            .filter((elem): elem is number => !!elem),
          true
        )
        .then(() => {
          callback();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMessage.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    const save: () => Promise<void> = () => {
      // Vuelidate expects this field to be filled. When editing rights it is not required.
      if (!isTemplate.value) {
        formState.formTemplateName = "foo";
      }
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
        updateInProgress.value = true;
        tmpRight.value.startDate = new Date(formState.startDate);
        tmpRight.value.endDate =
          formState.endDate == "" ? undefined : new Date(formState.endDate);
        if (props.isNew && isTemplate.value) {
          createTemplate();
        } else if (isTemplate.value) {
          updateTemplate();
        } else if (props.isNew) {
          createRight();
        } else {
          updateRight();
        }
      });
    };

    const accessStateToString = (access: AccessStateRest | undefined) => {
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
    onMounted(() => {
      reinitializeRight();
      if (!props.isNew) {
        loadBookmarks();
      }
    });
    const computedMetadataId = computed(() => props.metadataId != undefined ? props.metadataId : "");
    const computedRight = computed(() => props.right);
    const computedReinitCounter = computed(() => props.reinitCounter);
    const computedRightId = computed(() => {
      // The check for undefined is required here!
      if (props.right == undefined || props.right.rightId == undefined) {
        return "";
      } else {
        return props.right.rightId;
      }
    });
    const computedTemplateId = computed(() =>
      props.right == undefined || props.right.templateId == undefined
        ? -1
        : props.right.templateId
    );

    const isTemplate = computed(
      () => props.right != undefined && props.right.templateId != undefined
    );
    const isEditable = computed(
      () =>
        props.right != undefined &&
        props.right.templateId != undefined &&
        !props.isNew
    );

    watch(computedRight, () => {
      reinitializeRight();
    });
    watch(computedReinitCounter, () => {
      updateInProgress.value = false;
      if (props.isNew) {
        resetAllValues();
        addInitialBookmark();
      } else {
        setGivenValues();
        loadBookmarks();
      }
    });

    const addInitialBookmark = () => {
      if (props.initialBookmark != undefined) {
        bookmarkItems.value = Array(props.initialBookmark);
      }
    };

    const setGivenValues = () => {
      if (props.right == undefined) {
        // This should never happen :'(
        return;
      }
      tmpRight.value = Object.assign({}, props.right);
      formState.formTemplateName =
        props.right == undefined || props.right.templateName == undefined
          ? ""
          : props.right.templateName;
      formState.accessState = accessStateToString(props.right.accessState);
      formState.basisStorage = basisStorageToString(props.right.basisStorage);
      formState.basisAccessState = basisAccessStateToString(
        props.right.basisAccessState
      );
      formState.startDate = props.right.startDate.toISOString().slice(0, 10);
      if (props.right.endDate !== undefined) {
        formState.endDate = props.right.endDate.toISOString().slice(0, 10);
      } else {
        formState.endDate = "";
      }
    };

    const resetAllValues = () => {
      tmpRight.value = Object.assign({} as RightRest);
      formState.endDate = "";
      formState.startDate = "";
      tmpTemplateId.value = -1;
      tmpTemplateDescription.value = "";
      formState.formTemplateName = "";
      formState.accessState = "";
      bookmarkItems.value = [];
    };

    const reinitializeRight = () => {
      updateInProgress.value = false;
      getGroupList();
      if (!props.isNew) {
        setGivenValues();
      } else {
        resetAllValues();
        addInitialBookmark();
      }
    };

    /**
     * Bookmarks related to given Template.
     */
    const bookmarkDialogOn = ref(false);
    const openBookmarkSearch = ref(0);
    const renderBookmarkKey = ref(0);
    const selectBookmark = () => {
      bookmarkDialogOn.value = true;
      openBookmarkSearch.value += 1;
    };
    const templateBookmarkClosed = () => {
      bookmarkDialogOn.value = false;
    };

    const bookmarkItems: Ref<Array<BookmarkRest>> = ref([]);
    const bookmarkHeaders = [
      {
        text: "Id",
        align: "start",
        value: "bookmarkId",
        sortable: true,
      },
      {
        text: "Name",
        align: "start",
        value: "bookmarkName",
        sortable: true,
      },
      {
        text: "Beschreibung",
        align: "start",
        value: "description",
        sortable: true,
      },
      { text: "Actions", value: "actions", sortable: false },
    ];

    // Load Bookmarks
    const loadBookmarks = () => {
      templateApi
        .getBookmarksByTemplateId(computedTemplateId.value)
        .then((bookmarks: Array<BookmarkRest>) => {
          bookmarkItems.value = bookmarks;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMessage.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    const setSelectedBookmarks = (bookmarks: Array<BookmarkRest>) => {
      // Unionise
      bookmarkItems.value = bookmarkItems.value.concat(bookmarks);
      bookmarkItems.value = uniqWith(bookmarkItems.value, isEqual).sort(
        (a, b) => (a.bookmarkId < b.bookmarkId ? -1 : 1)
      );
      renderBookmarkKey.value += 1;
    };

    const deleteBookmarkEntry = (bookmark: BookmarkRest) => {
      const editedIndex = bookmarkItems.value.indexOf(bookmark);
      bookmarkItems.value.splice(editedIndex, 1);
    };

    // Groups
    const generalAlertError = ref(false);
    const generalAlertErrorMessage = ref("");
    const groupItems: Ref<Array<string>> = ref([]);
    const getGroupList = () => {
      api
        .getGroupList(0, 100, true)
        .then((r: Array<GroupRest>) => {
          groupItems.value = r.map((value) => value.name);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMessage.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };
    return {
      formState,
      v$,
      // variables
      accessStatusSelect,
      basisAccessState,
      basisStorage,
      bookmarkDialogOn,
      bookmarkItems,
      bookmarkHeaders,
      computedMetadataId,
      computedRightId,
      computedTemplateId,
      dialogDeleteRight,
      dialogDeleteTemplate,
      errorAccessState,
      errorEndDate,
      errorTemplateName,
      errorStartDate,
      isEditable,
      isTemplate,
      generalAlertError,
      generalAlertErrorMessage,
      groupItems,
      historyStore,
      menuStartDate,
      menuEndDate,
      metadataCount,
      openPanelsDefault,
      openBookmarkSearch,
      renderBookmarkKey,
      saveAlertError,
      saveAlertErrorMessage,
      updateConfirmDialog,
      tmpRight,
      tmpTemplateDescription,
      updateInProgress,
      // methods
      cancel,
      cancelConfirm,
      createRight,
      initiateDeleteDialog,
      deleteBookmarkEntry,
      deleteDialogClosed,
      deleteSuccessful,
      selectBookmark,
      setSelectedBookmarks,
      save,
      templateBookmarkClosed,
      updateRight,
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
        :disabled="updateInProgress"
        color="blue darken-1"
        text
        @click="save"
        >Speichern
      </v-btn>
      <v-btn color="blue darken-1" text @click="cancel">Zurück</v-btn>
      <v-btn :disabled="isNew" icon @click="initiateDeleteDialog">
        <v-icon>mdi-delete</v-icon>
      </v-btn>
      <v-dialog
        v-model="dialogDeleteRight"
        :retain-focus="false"
        max-width="500px"
      >
        <RightsDeleteDialog
          :index="index"
          :is-template="isTemplate"
          :metadataId="computedMetadataId"
          :right-id="computedRightId"
          v-on:deleteDialogClosed="deleteDialogClosed"
          v-on:deleteSuccessful="deleteSuccessful"
        ></RightsDeleteDialog>
      </v-dialog>

      <v-dialog
        v-model="dialogDeleteTemplate"
        :retain-focus="false"
        max-width="500pxi"
      >
        <RightsDeleteDialog
          :index="index"
          :is-template="isTemplate"
          :metadataId="computedMetadataId"
          :right-id="computedTemplateId.toString()"
          v-on:deleteDialogClosed="deleteDialogClosed"
          v-on:templateDeleteSuccessful="deleteSuccessful"
        ></RightsDeleteDialog>
      </v-dialog>
    </v-card-actions>
    <v-alert v-model="saveAlertError" dismissible text type="error">
      Speichern war nicht erfolgreich:
      {{ saveAlertErrorMessage }}
    </v-alert>
    <v-alert v-model="generalAlertError" dismissible text type="error">
      {{ generalAlertErrorMessage }}
    </v-alert>
    <v-expansion-panels v-model="openPanelsDefault" focusable multiple>
      <template v-if="isTemplate">
        <v-expansion-panel>
          <v-expansion-panel-header>
            Template Informationen
          </v-expansion-panel-header>
          <v-expansion-panel-content eager>
            <v-container fluid>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Template-Id</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-if="isNew"
                    ref="templateId"
                    :disabled="isNew"
                    hint="Template Id"
                    label="Wird automatisch generiert"
                    outlined
                  ></v-text-field>
                  <v-text-field
                    v-if="!isNew"
                    ref="templateId"
                    v-model="computedTemplateId"
                    disabled
                    hint="Template Id"
                    outlined
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Template Name</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="formState.formTemplateName"
                    :error-messages="errorTemplateName"
                    :disabled="isEditable"
                    hint="Name des Templates"
                    outlined
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Beschreibung</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.templateDescription"
                    hint="Beschreibung des Templates"
                    outlined
                    :disabled="isEditable"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Erstellt am</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.createdOn"
                    outlined
                    readonly
                    hint="Erstellungsdatum des Templates"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Zuletzt editiert am</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastUpdatedOn"
                    outlined
                    readonly
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
                    outlined
                    readonly
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Zuletzt angewendet am</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastAppliedOn"
                    outlined
                    readonly
                    hint="Datum, wann das letzte Mal das Template angewendet wurde bzw. der automatische Job"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Verknüpfte Suchen</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-data-table
                    :key="renderBookmarkKey"
                    :headers="bookmarkHeaders"
                    :items="bookmarkItems"
                    item-key="bookmarkId"
                    loading-text="Daten werden geladen... Bitte warten."
                  >
                    <template v-slot:item.actions="{ item }">
                      <v-icon
                        small
                        @click="deleteBookmarkEntry(item)"
                        :disabled="isEditable"
                      >
                        mdi-delete
                      </v-icon>
                    </template>
                  </v-data-table>
                  <v-btn
                    color="blue darken-1"
                    text
                    @click="selectBookmark"
                    :disabled="isEditable"
                    >Suche Bookmark</v-btn
                  >
                  <v-dialog
                    v-model="bookmarkDialogOn"
                    :retain-focus="false"
                    max-width="500px"
                  >
                    <TemplateBookmark
                      :reinit-counter="openBookmarkSearch"
                      v-on:bookmarksSelected="setSelectedBookmarks"
                      v-on:templateBookmarkClosed="templateBookmarkClosed"
                    ></TemplateBookmark>
                  </v-dialog>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </template>
      <v-expansion-panel>
        <v-expansion-panel-header
          >Steuerungsrelevante Elemente
        </v-expansion-panel-header>
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
                  disabled
                  hint="Rechte Id"
                  label="Wird automatisch generiert"
                  outlined
                ></v-text-field>
                <v-text-field
                  v-if="!isNew"
                  ref="rightId"
                  v-model="tmpRight.rightId"
                  disabled
                  hint="Rechte Id"
                  outlined
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Aktueller Access-Status</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  v-model="formState.accessState"
                  :disabled="isEditable"
                  :error-messages="errorAccessState"
                  :items="accessStatusSelect"
                  outlined
                  @blur="v$.accessState.$touch()"
                  @change="v$.accessState.$touch()"
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
                  min-width="auto"
                  offset-y
                  transition="scale-transition"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      ref="startDate"
                      v-model="formState.startDate"
                      :error-messages="errorStartDate"
                      label="Start-Datum"
                      outlined
                      prepend-icon="mdi-calendar"
                      readonly
                      required
                      v-bind="attrs"
                      @blur="v$.startDate.$touch()"
                      @change="v$.startDate.$touch()"
                      v-on="on"
                    ></v-text-field>
                  </template>
                  <v-date-picker
                    v-model="formState.startDate"
                    no-title
                    scrollable
                    :disabled="isEditable"
                  >
                    <v-spacer></v-spacer>
                    <v-btn
                      color="primary"
                      text
                      @click="menuStartDate = false"
                      :disabled="isEditable"
                    >
                      Cancel
                    </v-btn>
                    <v-btn
                      color="primary"
                      text
                      @click="$refs.menuStart.save(formState.startDate)"
                      :disabled="isEditable"
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
                  min-width="auto"
                  offset-y
                  transition="scale-transition"
                >
                  <template v-slot:activator="{ on, attrs }">
                    <v-text-field
                      ref="endDate"
                      v-model="formState.endDate"
                      :error-messages="errorEndDate"
                      label="End-Datum"
                      outlined
                      prepend-icon="mdi-calendar"
                      readonly
                      required
                      v-bind="attrs"
                      @blur="v$.endDate.$touch()"
                      @change="v$.endDate.$touch()"
                      v-on="on"
                    ></v-text-field>
                  </template>
                  <v-date-picker
                    v-model="formState.endDate"
                    no-title
                    scrollable
                    :disabled="isEditable"
                  >
                    <v-spacer></v-spacer>
                    <v-btn
                      :disabled="isEditable"
                      color="primary"
                      text
                      @click="menuEndDate = false"
                    >
                      Cancel
                    </v-btn>
                    <v-btn
                      color="primary"
                      text
                      :disabled="isEditable"
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
                <v-subheader>Gruppen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  v-model="tmpRight.groupIds"
                  :items="groupItems"
                  :disabled="isEditable"
                  chips
                  counter
                  hint="Einschränkung des Zugriffs auf Berechtigungsgruppen"
                  multiple
                  outlined
                >
                </v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader>Bemerkungen</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesGeneral"
                  :disabled="isEditable"
                  counter
                  hint="Allgemeine Bemerkungen"
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
                  :disabled="isEditable"
                  hint="Gibt Auskunft darüber, ob ein Lizenzvertrag für dieses Item als Nutzungsrechtsquelle vorliegt."
                  outlined
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
                  :disabled="isEditable"
                  color="indigo"
                  hint="Ist für die ZBW die Nutzung der Urheberrechtschranken möglich?"
                  label="Ja"
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
                  :disabled="isEditable"
                  color="indigo"
                  hint="Gibt Auskunft darüber, ob eine Nutzungsvereinbarung für dieses Item als Nutzungsrechtsquelle vorliegt."
                  label="Ja"
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
                  hint="Eine per URI eindeutig referenzierte Standard-Open-Content-Lizenz, die für das Item gilt."
                  :disabled="isEditable"
                  outlined
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
                  :disabled="isEditable"
                  hint="Eine per URL eindeutig referenzierbare Nicht-standardisierte Open-Content-Lizenz, die für das Item gilt."
                  outlined
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                <v-subheader
                  >Nicht-standardisierte Open-Content-Lizenz (keine URL)
                </v-subheader>
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.nonStandardOpenContentLicence"
                  :disabled="isEditable"
                  color="indigo"
                  hint="Ohne URL, als Freitext (bzw. derzeit als Screenshot in Clearingstelle)"
                  label="Ja"
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
                  :disabled="isEditable"
                  color="indigo"
                  hint="Gilt für dieses Item, dem im Element 'Open-Content-Licence' eine standardisierte Open-Content-Lizenz zugeordnet ist, eine Einschränkung?"
                  label="Ja"
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
                  :disabled="isEditable"
                  counter
                  hint="Bemerkungen für formale Regelungen"
                  maxlength="256"
                  outlined
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header
          >Prozessdokumentierende Elemente
        </v-expansion-panel-header>
        <v-expansion-panel-content eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4">
                <v-subheader>Basis der Speicherung</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-select
                  v-model="formState.basisStorage"
                  :disabled="isEditable"
                  :items="basisStorage"
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
                  v-model="formState.basisAccessState"
                  :disabled="isEditable"
                  :items="basisAccessState"
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
                  :disabled="isEditable"
                  counter
                  hint="Bemerkungen für prozessdokumentierende Elemente"
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
            <v-row v-if="!isTemplate">
              <v-col cols="4">
                <v-subheader>Zuletzt editiert am</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedOn"
                  outlined
                  readonly
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row v-if="!isTemplate">
              <v-col cols="4">
                <v-subheader>Zuletzt editiert von</v-subheader>
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedBy"
                  outlined
                  readonly
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
                  :disabled="isEditable"
                  counter
                  hint="Bemerkungen für Metadaten über den Rechteinformationseintrag"
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
        :disabled="updateInProgress"
        color="blue darken-1"
        text
        @click="save"
        >Speichern
      </v-btn>
    </v-card-actions>
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
