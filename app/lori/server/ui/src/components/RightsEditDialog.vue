<script lang="ts">
import api from "@/api/api";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import {
  AccessStateRest,
  BookmarkRest,
  GroupRest,
  ItemEntry,
  RightIdCreated,
  RightRest,
  RightRestBasisAccessStateEnum,
  RightRestBasisStorageEnum,
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
import date_utils from "@/utils/date_utils";

export default defineComponent({
  props: {
    right: {
      type: Object as PropType<RightRest>,
      required: false, // Is not required because this component is used for creating new rights as well
    },
    index: {
      type: Number,
      required: true,
    },
    isNewRight: {
      type: Boolean,
      required: true,
    },
    isNewTemplate: {
      type: Boolean,
      required: true,
    },
    isExceptionTemplate: {
      type: Boolean,
      required: false,
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
      type: Object as PropType<BookmarkRest>,
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
      startDate: Date | undefined;
      endDate: Date | undefined;
      formTemplateName: string;
    };

    const formState = reactive({
      accessState: "",
      basisStorage: "",
      basisAccessState: "",
      startDate: {} as Date | undefined,
      endDate: {} as Date | undefined,
      formTemplateName: "",
    });

    const isStartDateMenuOpen = ref(false);
    const isEndDateMenuOpen = ref(false);

    const startDateFormatted = computed(() => {
      if (
        date_utils.isEmptyObject(formState.startDate) ||
        formState.startDate == undefined
      ) {
        return "";
      } else {
        return date_utils.dateToIso8601(formState.startDate);
      }
    });

    const endDateFormatted = computed(() => {
      if (
        date_utils.isEmptyObject(formState.endDate) ||
        formState.endDate == undefined
      ) {
        return "";
      } else {
        return date_utils.dateToIso8601(formState.endDate);
      }
    });

    watch(startDateFormatted, () => {
      isStartDateMenuOpen.value = false;
    });

    watch(endDateFormatted, () => {
      isEndDateMenuOpen.value = false;
    });

    const endDateCheck = (value: Date | undefined, siblings: FormState) => {
      if (value == undefined || siblings.startDate == undefined) {
        return true;
      } else {
        return siblings.startDate < value;
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
     * Constants:
     */
    const openPanelsDefault: Ref<Array<string>> = ref(["0"]);
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
    const saveAlertErrorMsg = ref("");
    const updateConfirmDialog = ref(false);
    const updateInProgress = ref(false);
    const updateSuccessful = ref(false);
    const updateSuccessfulMsg = ref("");
    const metadataCount = ref(0);
    const tmpRight = ref({} as RightRest);

    const emitClosedDialog = () => {
      emit("editRightClosed");
    };

    const close = () => {
      generalAlertError.value = false;
      generalAlertErrorMsg.value = "";
      saveAlertError.value = false;
      saveAlertErrorMsg.value = "";
      updateConfirmDialog.value = false;
      updateInProgress.value = false;
      updateSuccessful.value = false;
      updateSuccessfulMsg.value = "";
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
              true,
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
                saveAlertErrorMsg.value = errMsg;
                updateConfirmDialog.value = false;
              });
            });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            saveAlertError.value = true;
            saveAlertErrorMsg.value = errMsg;
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
          updateSuccessfulMsg.value =
            "Rechteinformation " +
            tmpRight.value.rightId +
            " erfolgreich geupdated";
          updateSuccessful.value = true;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            saveAlertError.value = true;
            saveAlertErrorMsg.value = errMsg;
            updateConfirmDialog.value = false;
          });
        });
    };

    const createTemplate = () => {
      tmpRight.value.rightId = "unset";
      tmpRight.value.isTemplate = true;
      tmpRight.value.templateName = formState.formTemplateName;
      templateApi
        .addTemplate(tmpRight.value)
        .then((r: RightIdCreated) => {
          const exceptionIds: string[] = exceptionTemplateItems.value.flatMap(
            (e) => (e.rightId ? [e.rightId] : []),
          );
          addExceptionsToTemplate(r.rightId, exceptionIds, () => {
            updateBookmarks(r.rightId, () => {
              tmpRight.value.rightId = r.rightId;
              emit("addTemplateSuccessful", tmpRight.value);
              close();
            });
          });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    const updateTemplate = () => {
      tmpRight.value.templateName = formState.formTemplateName;
      templateApi
        .updateTemplate(tmpRight.value)
        .then(() => {
          if (tmpRight.value.rightId == undefined) {
            generalAlertErrorMsg.value =
              "No RightId found when updating. This should NOT happen.";
            generalAlertError.value = true;
          } else {
            const exceptionIds: string[] = exceptionTemplateItems.value.flatMap(
              (e) => (e.rightId ? [e.rightId] : []),
            );
            addExceptionsToTemplate(
              tmpRight.value.rightId,
              exceptionIds,
              () => {
                updateBookmarks(tmpRight.value.rightId, () => {
                  updateSuccessfulMsg.value =
                    "Template " +
                    tmpRight.value.templateName +
                    " erfolgreich geupdated";
                  updateSuccessful.value = true;
                  emit("updateTemplateSuccessful", formState.formTemplateName);
                });
              },
            );
          }
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    /**
     * Add exceptions.
     */
    const addExceptionsToTemplate = (
      rightId: string,
      exceptionIds: Array<string>,
      callback: () => void,
    ) => {
      templateApi
        .addExceptionToTemplate(rightId, exceptionIds)
        .then(() => {
          callback();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    /**
     * Refresh bookmarks.
     */
    const updateBookmarks = (rightId: string, callback: () => void) => {
      templateApi
        .addBookmarksByRightId(
          rightId,
          bookmarkItems.value
            .map((elem) => elem.bookmarkId)
            .filter((elem): elem is number => !!elem),
          true,
        )
        .then(() => {
          callback();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
            generalAlertError.value = true;
          });
        });
    };

    const save: () => Promise<void> = async () => {
      // Vuelidate expects this field to be filled. When editing rights it is not required.
      if (!isTemplate.value) {
        formState.formTemplateName = "foo";
      }
      const isValid = await v$.value.$validate();
      if (!isValid) {
        return;
      }
      tmpRight.value.accessState = stringToAccessState(formState.accessState);
      tmpRight.value.basisStorage = stringToBasisStorage(
        formState.basisStorage,
      );
      tmpRight.value.basisAccessState = stringToBasisAccessState(
        formState.basisAccessState,
      );
      updateInProgress.value = true;
      if (formState.startDate == undefined) {
        return;
      }
      tmpRight.value.startDate = formState.startDate;
      tmpRight.value.endDate = formState.endDate;
      if (props.isNewTemplate) {
        createTemplate();
      } else if (isTemplate.value) {
        updateTemplate();
      } else if (props.isNewRight) {
        createRight();
      } else {
        updateRight();
      }
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
      basisStorage: RightRestBasisStorageEnum | undefined,
    ) => {
      if (basisStorage == undefined) {
        return "Kein Wert";
      } else {
        switch (basisStorage) {
          case RightRestBasisStorageEnum.Authorrightexception:
            return "Urheberrechtschranke";
          case RightRestBasisStorageEnum.Useragreement:
            return "Nutzungsvereinbarung";
          case RightRestBasisStorageEnum.Opencontentlicence:
            return "Open-Content-Lizenz";
          case RightRestBasisStorageEnum.Zbwpolicyunanswered:
            return "ZBW-Policy (unbeantwortete Rechteanforderung)";
          case RightRestBasisStorageEnum.Zbwpolicyrestricted:
            return "ZBW-Policy (Eingeschränkte OCL)";
          case RightRestBasisStorageEnum.Licencecontract:
            return "Lizenzvertrag";
          default:
            return "Kein Wert";
        }
      }
    };

    const stringToBasisStorage = (value: string | undefined) => {
      if (value == undefined) {
        return undefined;
      } else {
        switch (value) {
          case "Lizenzvertrag":
            return RightRestBasisStorageEnum.Licencecontract;
          case "Nutzungsvereinbarung":
            return RightRestBasisStorageEnum.Useragreement;
          case "Urheberrechtschranke":
            return RightRestBasisStorageEnum.Authorrightexception;
          case "Open-Content-Lizenz":
            return RightRestBasisStorageEnum.Opencontentlicence;
          case "ZBW-Policy (Eingeschränkte OCL)":
            return RightRestBasisStorageEnum.Zbwpolicyrestricted;
          case "ZBW-Policy (unbeantwortete Rechteanforderung)":
            return RightRestBasisStorageEnum.Zbwpolicyunanswered;
          default:
            return undefined;
        }
      }
    };

    const basisAccessStateToString = (
      basisAccessState: RightRestBasisAccessStateEnum | undefined,
    ) => {
      if (basisAccessState == undefined) {
        return "Kein Wert";
      } else {
        switch (basisAccessState) {
          case RightRestBasisAccessStateEnum.Authorrightexception:
            return "Urheberrechtschranke";
          case RightRestBasisAccessStateEnum.Useragreement:
            return "Nutzungsvereinbarung";
          case RightRestBasisAccessStateEnum.Licencecontract:
            return "Lizenzvertrag";
          case RightRestBasisAccessStateEnum.Zbwpolicy:
            return "ZBW-Policy";
          case RightRestBasisAccessStateEnum.Licencecontractoa:
            return "OA-Rechte aus Lizenzvertrag";
          default:
            return "Kein Wert";
        }
      }
    };

    const stringToBasisAccessState = (value: string | undefined) => {
      if (value == undefined) {
        tmpRight.value.basisAccessState = undefined;
      } else {
        switch (value) {
          case "Lizenzvertrag":
            return RightRestBasisAccessStateEnum.Licencecontract;
          case "Nutzungsvereinbarung":
            return RightRestBasisAccessStateEnum.Useragreement;
          case "OA-Rechte aus Lizenzvertrag":
            return RightRestBasisAccessStateEnum.Licencecontractoa;
          case "Urheberrechtschranke":
            return RightRestBasisAccessStateEnum.Authorrightexception;
          case "ZBW-Policy":
            return RightRestBasisAccessStateEnum.Zbwpolicy;
          default:
            return undefined;
        }
      }
    };

    // Computed properties
    onMounted(() => {
      reinitializeRight();
      if (!isNew.value && isTemplate.value) {
        loadBookmarks();
        loadExceptions();
      }
    });
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

    const isNew = computed(() => props.isNewRight || props.isNewTemplate);
    const isEditable = computed(
      () =>
        isNew.value ||
        (props.right != undefined && props.right.lastAppliedOn == undefined),
    );
    const isTemplate = computed(
      () =>
        props.isNewTemplate ||
        (props.right != undefined && props.right.isTemplate),
    );
    const exceptionsAllowed = computed(
      () =>
        !props.isExceptionTemplate &&
        (props.isNewTemplate ||
          (props.right != undefined && props.right.exceptionFrom == undefined)),
    );

    const cardTitle = computed(() => {
      const mode = isNew.value ? "erstellen" : "bearbeiten";
      const exception = props.isExceptionTemplate ? " (Ausnahme)" : "";
      if (isTemplate.value) {
        return "Template" + exception + " " + mode;
      } else {
        return "Rechteinformation " + mode;
      }
    });

    watch(computedRight, () => {
      reinitializeRight();
    });
    watch(computedReinitCounter, () => {
      updateInProgress.value = false;
      if (isNew.value) {
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
        props.right.templateName == undefined ? "" : props.right.templateName;
      formState.accessState = accessStateToString(props.right.accessState);
      formState.basisStorage = basisStorageToString(props.right.basisStorage);
      formState.basisAccessState = basisAccessStateToString(
        props.right.basisAccessState,
      );
      formState.startDate = props.right.startDate;
      if (props.right.endDate !== undefined) {
        formState.endDate = props.right.endDate;
      } else {
        formState.endDate = undefined;
      }
    };

    const resetAllValues = () => {
      tmpRight.value = Object.assign({} as RightRest);
      formState.endDate = undefined;
      formState.startDate = undefined;
      formState.formTemplateName = "";
      formState.accessState = "";
      bookmarkItems.value = [];
    };

    const reinitializeRight = () => {
      updateInProgress.value = false;
      getGroupList();
      if (!isNew.value) {
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
        title: "Id",
        align: "start",
        value: "bookmarkId",
        sortable: true,
      },
      {
        title: "Name",
        align: "start",
        value: "bookmarkName",
        sortable: true,
      },
      {
        title: "Beschreibung",
        align: "start",
        value: "description",
        sortable: true,
      },
      { title: "Actions", value: "actions", sortable: false },
    ];

    // Template Exceptions
    const dialogCreateException = ref(false);
    const renderTemplateKey = ref(0);

    const openCreateExceptionDialog = () => {
      dialogCreateException.value = true;
    };
    const closeCreateExceptionDialog = () => {
      dialogCreateException.value = false;
    };

    const exceptionTemplateItems: Ref<Array<RightRest>> = ref([]);
    const exceptionTemplateHeaders = [
      {
        title: "Id",
        align: "start",
        value: "rightId",
        sortable: true,
      },
      {
        title: "Name",
        align: "start",
        value: "templateName",
        sortable: true,
      },
      {
        title: "Beschreibung",
        align: "start",
        value: "templateDescription",
      },
      { title: "Actions", value: "actions", sortable: false },
    ];

    const addNewException = (excTemplate: RightRest) => {
      exceptionTemplateItems.value =
        exceptionTemplateItems.value.concat(excTemplate);
      renderTemplateKey.value += 1;
    };

    const deleteExceptionEntry = (entry: RightRest) => {
      const editedIndex = exceptionTemplateItems.value.indexOf(entry);
      exceptionTemplateItems.value.splice(editedIndex, 1);
      if (entry.rightId != undefined) {
        templateApi.deleteTemplateById(entry.rightId).catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
            generalAlertError.value = true;
          });
        });
      }
    };

    // Load Bookmarks
    const loadBookmarks = () => {
      if (computedRightId.value == undefined) {
        generalAlertErrorMsg.value =
          "Error while loading bookmarks. Invalid Template ID.";
        generalAlertError.value = true;
      } else {
        templateApi
          .getBookmarksByRightId(computedRightId.value)
          .then((bookmarks: Array<BookmarkRest>) => {
            bookmarkItems.value = bookmarks;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              generalAlertErrorMsg.value = errMsg;
              generalAlertError.value = true;
            });
          });
      }
    };

    const loadExceptions = () => {
      if (computedRightId.value == undefined) {
        generalAlertErrorMsg.value =
          "Error while loading bookmarks. Invalid Template ID.";
        generalAlertError.value = true;
      } else {
        templateApi
          .getExceptionsById(computedRightId.value)
          .then((exceptions: Array<RightRest>) => {
            exceptionTemplateItems.value = exceptions;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              generalAlertErrorMsg.value = errMsg;
              generalAlertError.value = true;
            });
          });
      }
    };

    const setSelectedBookmarks = (bookmarks: Array<BookmarkRest>) => {
      // Unionise
      bookmarkItems.value = bookmarkItems.value.concat(bookmarks);
      bookmarkItems.value = uniqWith(bookmarkItems.value, isEqual).sort(
        (a, b) => (a.bookmarkId < b.bookmarkId ? -1 : 1),
      );
      renderBookmarkKey.value += 1;
    };

    const deleteBookmarkEntry = (bookmark: BookmarkRest) => {
      const editedIndex = bookmarkItems.value.indexOf(bookmark);
      bookmarkItems.value.splice(editedIndex, 1);
    };

    // Groups
    const generalAlertError = ref(false);
    const generalAlertErrorMsg = ref("");
    const groupItems: Ref<Array<string>> = ref([]);
    const getGroupList = () => {
      api
        .getGroupList(0, 100, true)
        .then((r: Array<GroupRest>) => {
          groupItems.value = r.map((value) => value.name);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            generalAlertErrorMsg.value = errMsg;
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
      cardTitle,
      computedRightId,
      dialogCreateException,
      dialogDeleteRight,
      dialogDeleteTemplate,
      endDateFormatted,
      errorAccessState,
      errorEndDate,
      errorTemplateName,
      errorStartDate,
      exceptionsAllowed,
      isEditable,
      isNew,
      isTemplate,
      generalAlertError,
      generalAlertErrorMsg,
      groupItems,
      historyStore,
      isStartDateMenuOpen,
      isEndDateMenuOpen,
      menuStartDate,
      menuEndDate,
      metadataCount,
      openPanelsDefault,
      openBookmarkSearch,
      renderBookmarkKey,
      renderTemplateKey,
      saveAlertError,
      saveAlertErrorMsg,
      startDateFormatted,
      exceptionTemplateItems,
      exceptionTemplateHeaders,
      updateConfirmDialog,
      updateSuccessful,
      updateSuccessfulMsg,
      tmpRight,
      updateInProgress,
      // methods
      addNewException,
      cancel,
      cancelConfirm,
      closeCreateExceptionDialog,
      createRight,
      initiateDeleteDialog,
      deleteBookmarkEntry,
      deleteDialogClosed,
      deleteExceptionEntry,
      deleteSuccessful,
      openCreateExceptionDialog,
      selectBookmark,
      setSelectedBookmarks,
      save,
      templateBookmarkClosed,
      updateRight,
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
  <v-card class="my-scroll" max-height="700px">
    <v-card-title>{{ cardTitle }}</v-card-title>
    <v-card-actions>
      <v-alert v-model="updateSuccessful" closable type="success">
        {{ updateSuccessfulMsg }}
      </v-alert>
      <v-alert v-model="saveAlertError" closable type="error">
        Speichern war nicht erfolgreich: {{ saveAlertErrorMsg }}
      </v-alert>
      <v-alert v-model="generalAlertError" closable type="error">
        {{ generalAlertErrorMsg }}
      </v-alert>
      <v-spacer></v-spacer>
      <v-btn :disabled="updateInProgress" color="blue darken-1" @click="save"
        >Speichern
      </v-btn>
      <v-btn color="blue darken-1" @click="cancel">Zurück</v-btn>
      <v-btn :disabled="isNew" @click="initiateDeleteDialog">
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
          :right-id="computedRightId"
          v-on:deleteDialogClosed="deleteDialogClosed"
          v-on:deleteSuccessful="deleteSuccessful"
        ></RightsDeleteDialog>
      </v-dialog>

      <v-dialog
        v-model="dialogDeleteTemplate"
        :retain-focus="false"
        max-width="500px"
      >
        <RightsDeleteDialog
          :index="index"
          :is-template="isTemplate"
          :right-id="computedRightId"
          v-on:deleteDialogClosed="deleteDialogClosed"
          v-on:templateDeleteSuccessful="deleteSuccessful"
        ></RightsDeleteDialog>
      </v-dialog>
    </v-card-actions>

    <v-expansion-panels v-model="openPanelsDefault" focusable multiple>
      <template v-if="isTemplate">
        <v-expansion-panel value="0">
          <v-expansion-panel-title>
            Template Informationen
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-container fluid>
              <v-row>
                <v-col cols="4"> Template Name</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="formState.formTemplateName"
                    :error-messages="errorTemplateName"
                    :disabled="!isEditable"
                    hint="Name des Templates"
                    variant="outlined"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Beschreibung</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.templateDescription"
                    hint="Beschreibung des Templates"
                    variant="outlined"
                    :disabled="!isEditable"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Erstellt am</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.createdOn"
                    variant="outlined"
                    readonly
                    hint="Erstellungsdatum des Templates"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Erstellt von</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.createdBy"
                    variant="outlined"
                    readonly
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Zuletzt editiert am</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastUpdatedOn"
                    variant="outlined"
                    readonly
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Zuletzt editiert von</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastUpdatedBy"
                    variant="outlined"
                    readonly
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Zuletzt angewendet am</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastAppliedOn"
                    variant="outlined"
                    readonly
                    hint="Datum, wann das letzte Mal das Template angewendet wurde bzw. der automatische Job"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Verknüpfte Suchen</v-col>
                <v-col cols="8">
                  <v-data-table
                    :key="renderBookmarkKey"
                    :headers="bookmarkHeaders"
                    :items="bookmarkItems"
                    item-value="bookmarkId"
                    loading-text="Daten werden geladen... Bitte warten."
                  >
                    <template v-slot:item.actions="{ item }">
                      <v-icon
                        small
                        @click="deleteBookmarkEntry(item)"
                        :disabled="!isEditable"
                      >
                        mdi-delete
                      </v-icon>
                    </template>
                  </v-data-table>
                  <v-btn
                    color="blue darken-1"
                    @click="selectBookmark"
                    :disabled="!isEditable"
                    >Suche Bookmark
                  </v-btn>
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
              <v-row v-if="exceptionsAllowed">
                <v-col cols="4">Ausnahmen</v-col>
                <v-col cols="8">
                  <v-data-table
                    :key="renderTemplateKey"
                    :headers="exceptionTemplateHeaders"
                    :items="exceptionTemplateItems"
                    item-value="rightId"
                    loading-text="Daten werden geladen... Bitte warten."
                  >
                    <template v-slot:item.actions="{ item }">
                      <v-icon
                        small
                        :disabled="!isEditable"
                        @click="deleteExceptionEntry(item)"
                      >
                        mdi-delete
                      </v-icon>
                    </template>
                  </v-data-table>
                  <v-btn
                    color="blue darken-1"
                    :disabled="!isEditable"
                    @click="openCreateExceptionDialog"
                    >Erstelle neue Ausnahme
                  </v-btn>
                  <v-dialog
                    v-model="dialogCreateException"
                    :retain-focus="false"
                    max-width="1000px"
                  >
                    <RightsEditDialog
                      :index="index"
                      :isNewRight="false"
                      :isNewTemplate="true"
                      :isExceptionTemplate="true"
                      v-on:editRightClosed="closeCreateExceptionDialog"
                      v-on:addTemplateSuccessful="addNewException"
                    ></RightsEditDialog>
                  </v-dialog>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-text>
        </v-expansion-panel>
      </template>
      <v-expansion-panel>
        <v-expansion-panel-title
          >Steuerungsrelevante Elemente
        </v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4"> Right-Id</v-col>
              <v-col cols="8">
                <v-text-field
                  v-if="isNew"
                  ref="rightId"
                  disabled
                  hint="Rechte Id"
                  label="Wird automatisch generiert"
                  variant="outlined"
                ></v-text-field>
                <v-text-field
                  v-if="!isNew"
                  ref="rightId"
                  v-model="tmpRight.rightId"
                  disabled
                  hint="Rechte Id"
                  variant="outlined"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Aktueller Access-Status</v-col>
              <v-col cols="8">
                <v-select
                  v-model="formState.accessState"
                  :disabled="!isEditable"
                  :error-messages="errorAccessState"
                  :items="accessStatusSelect"
                  variant="outlined"
                  @blur="v$.accessState.$touch()"
                  @change="v$.accessState.$touch()"
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Gültigkeit Startdatum</v-col>
              <v-col cols="8">
                <v-menu
                  :close-on-content-click="false"
                  :location="'bottom'"
                  v-model="isStartDateMenuOpen"
                >
                  <template v-slot:activator="{ props }">
                    <v-text-field
                      :modelValue="startDateFormatted"
                      :error-messages="errorStartDate"
                      label="Start-Datum"
                      variant="outlined"
                      prepend-icon="mdi-calendar"
                      readonly
                      required
                      v-bind="props"
                      @blur="v$.startDate.$touch()"
                      @change="v$.startDate.$touch()"
                    ></v-text-field>
                  </template>
                  <v-date-picker v-model="formState.startDate" color="primary">
                    <template v-slot:header></template>
                  </v-date-picker>
                </v-menu>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Gültigkeit Enddatum</v-col>
              <v-col cols="8">
                <v-menu
                  :close-on-content-click="false"
                  :location="'bottom'"
                  v-model="isEndDateMenuOpen"
                >
                  <template v-slot:activator="{ props }">
                    <v-text-field
                      :modelValue="endDateFormatted"
                      :error-messages="errorEndDate"
                      label="End-Datum"
                      variant="outlined"
                      prepend-icon="mdi-calendar"
                      readonly
                      required
                      v-bind="props"
                      @blur="v$.endDate.$touch()"
                      @change="v$.endDate.$touch()"
                    ></v-text-field>
                  </template>
                  <v-date-picker v-model="formState.endDate" color="primary">
                    <template v-slot:header></template>
                  </v-date-picker>
                </v-menu>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Gruppen</v-col>
              <v-col cols="8">
                <v-select
                  v-model="tmpRight.groupIds"
                  :items="groupItems"
                  :disabled="!isEditable"
                  chips
                  counter
                  hint="Einschränkung des Zugriffs auf Berechtigungsgruppen"
                  multiple
                  variant="outlined"
                >
                </v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesGeneral"
                  :disabled="!isEditable"
                  counter
                  hint="Allgemeine Bemerkungen"
                  maxlength="256"
                  variant="outlined"
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-title>Formale Regelung</v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4"> Lizenzvertrag</v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.licenceContract"
                  :disabled="!isEditable"
                  hint="Gibt Auskunft darüber, ob ein Lizenzvertrag für dieses Item als Nutzungsrechtsquelle vorliegt."
                  variant="outlined"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Urheberrechtschrankennutzung</v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.authorRightException"
                  :disabled="!isEditable"
                  color="indigo"
                  hint="Ist für die ZBW die Nutzung der Urheberrechtschranken möglich?"
                  label="Ja"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> ZBW Nutzungsvereinbarung</v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.zbwUserAgreement"
                  :disabled="!isEditable"
                  color="indigo"
                  hint="Gibt Auskunft darüber, ob eine Nutzungsvereinbarung für dieses Item als Nutzungsrechtsquelle vorliegt."
                  label="Ja"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Open-Content-Licence</v-col>
              <v-col cols="8">
                <v-text-field
                  hint="Eine per URI eindeutig referenzierte Standard-Open-Content-Lizenz, die für das Item gilt."
                  v-model="tmpRight.openContentLicence"
                  :disabled="!isEditable"
                  variant="outlined"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                Nicht-standardisierte Open-Content-Lizenz (URL)
              </v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.nonStandardOpenContentLicenceURL"
                  :disabled="!isEditable"
                  hint="Eine per URL eindeutig referenzierbare Nicht-standardisierte Open-Content-Lizenz, die für das Item gilt."
                  variant="outlined"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4">
                Nicht-standardisierte Open-Content-Lizenz (keine URL)
              </v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.nonStandardOpenContentLicence"
                  :disabled="!isEditable"
                  color="indigo"
                  hint="Ohne URL, als Freitext (bzw. derzeit als Screenshot in Clearingstelle)"
                  label="Ja"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Eingeschränkte Open-Content-Lizenz</v-col>
              <v-col cols="8">
                <v-switch
                  v-model="tmpRight.restrictedOpenContentLicence"
                  :disabled="!isEditable"
                  color="indigo"
                  hint="Gilt für dieses Item, dem im Element 'Open-Content-Licence' eine standardisierte Open-Content-Lizenz zugeordnet ist, eine Einschränkung?"
                  label="Ja"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesFormalRules"
                  :disabled="!isEditable"
                  counter
                  hint="Bemerkungen für formale Regelungen"
                  maxlength="256"
                  variant="outlined"
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-title
          >Prozessdokumentierende Elemente
        </v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4"> Basis der Speicherung</v-col>
              <v-col cols="8">
                <v-select
                  v-model="formState.basisStorage"
                  :disabled="!isEditable"
                  :items="basisStorage"
                  variant="outlined"
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Basis des Access-Status</v-col>
              <v-col cols="8">
                <v-select
                  v-model="formState.basisAccessState"
                  :disabled="!isEditable"
                  :items="basisAccessState"
                  variant="outlined"
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesProcessDocumentation"
                  :disabled="!isEditable"
                  counter
                  hint="Bemerkungen für prozessdokumentierende Elemente"
                  maxlength="256"
                  variant="outlined"
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-title>
          Metadaten über den Rechteinformationseintrag
        </v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row v-if="!isTemplate">
              <v-col cols="4"> Erstellt am</v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.createdOn"
                  variant="outlined"
                  readonly
                  hint="Erstellungsdatum des Templates"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row v-if="!isTemplate">
              <v-col cols="4"> Erstellt von</v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.createdBy"
                  variant="outlined"
                  readonly
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row v-if="!isTemplate">
              <v-col cols="4"> Zuletzt editiert am</v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedOn"
                  variant="outlined"
                  readonly
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row v-if="!isTemplate">
              <v-col cols="4"> Zuletzt editiert von</v-col>
              <v-col cols="8">
                <v-text-field
                  v-model="tmpRight.lastUpdatedBy"
                  variant="outlined"
                  readonly
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesManagementRelated"
                  :disabled="!isEditable"
                  counter
                  hint="Bemerkungen für Metadaten über den Rechteinformationseintrag"
                  maxlength="256"
                  variant="outlined"
                ></v-textarea>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn color="blue darken-1" @click="cancel">Zurück</v-btn>
      <v-btn :disabled="updateInProgress" color="blue darken-1" @click="save"
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
