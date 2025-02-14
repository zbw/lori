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
  RightRestBasisStorageEnum, type TemplateApplicationRest, TemplateApplicationsRest,
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
import info from "@/utils/info";
import templateApi from "@/api/templateApi";
import TemplateBookmark from "@/components/TemplateBookmark.vue";
import isEqual from "lodash.isequal";
import { uniqWith } from "lodash";
import date_utils from "@/utils/date_utils";
import Dashboard from "@/components/Dashboard.vue";
import rightErrorApi from "@/api/rightErrorApi";
import rightApi from "@/api/rightApi";

export default defineComponent({
  computed: {
    info() {
      return info
    }
  },
  props: {
    rightId: {
      type: String,
      required: false,
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
    handle: {
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
    initialRight: {
      type: Object as PropType<RightRest>,
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
    Dashboard,
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
      templateDescription: string;
      selectedGroups: Array<GroupRest>;
    };

    const formState = reactive({
      accessState: "",
      basisStorage: "",
      basisAccessState: "",
      startDate: {} as Date | undefined,
      endDate: {} as Date | undefined,
      templateName: "",
      templateDescription: "",
      selectedGroups: [] as Array<GroupRest>,
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

    const newRightHasChanges = computed (() => {
      return isNew.value &&
      (formState.accessState != "" ||
      formState.basisStorage != "" ||
      formState.basisAccessState != ""  ||
      !(formState.startDate == undefined || Object.keys(formState.startDate).length === 0) ||
      !(formState.endDate == undefined || Object.keys(formState.endDate).length === 0) ||
      (isTemplate.value && (
              formState.templateName != "" ||
              formState.templateDescription != "" ||
              bookmarkItems.value.length != 0 ||
              exceptionTemplateItems.value.length != 0
          )
      ))
    });
    const existingRightHasChanges = computed (() => {
      return (JSON.stringify(tmpRight.value) != JSON.stringify(lastSavedRight.value) ||
          ((tmpRight.value.groups != undefined) && (formState.selectedGroups != tmpRight.value.groups)) ||
          formState.accessState != accessStateToString(lastSavedRight.value.accessState) ||
          formState.basisStorage != basisStorageToString(lastSavedRight.value.basisStorage) ||
          formState.basisAccessState != basisAccessStateToString(lastSavedRight.value.basisAccessState)  ||
          formState.startDate != lastSavedRight.value.startDate ||
          formState.endDate != lastSavedRight.value.endDate ||
          (isTemplate.value && (
                  formState.templateName != lastSavedRight.value.templateName ||
                  formState.templateDescription != (lastSavedRight.value.templateDescription == undefined ? "" : lastSavedRight.value.templateDescription) ||
                  JSON.stringify(bookmarkItems.value) != JSON.stringify(lastSavedBookmarkItems.value) ||
                  JSON.stringify(exceptionTemplateItems.value) != JSON.stringify(lastSavedExceptionTemplateItems.value)
                )
          )
      )
    });
    const formWasChanged = computed(() => {
      if (isNew.value){
        return newRightHasChanges.value
      } else {
        return existingRightHasChanges.value;
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

    const groupCheck = (value: Array<GroupRest>, siblings: FormState) => {
      return !(siblings.accessState == 'Restricted' && value.length == 0);
    };
    const rules = {
      accessState: { required },
      startDate: { required },
      endDate: { endDateCheck },
      templateName: { required },
      selectedGroups: { groupCheck },
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
        v$.value.templateName.$invalid &&
        v$.value.templateName.$dirty
      ) {
        errors.push("Es wird ein Template-Name benötigt.");
      }
      return errors;
    });
    const errorIPGroup = computed(() =>{
      const errors: Array<string> = [];
      if (v$.value.selectedGroups.$invalid) {
        errors.push("Bei Auswahl des Access-Status 'Restricted' ist die Angabe einer" +
            " IP-Gruppe, auf die der Zugriff beschränkt werden soll, zwingend erforderlich." +
            " Bitte eine IP-Gruppe auswählen.");
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
    const updateConfirmDialog = ref(false);
    const updateInProgress = ref(false);
    const successMsgIsActive = ref(false);
    const successMsg = ref("");
    const metadataCount = ref(0);
    const tmpRight = ref({} as RightRest);
    const lastSavedRight = ref({} as RightRest);

    const emitClosedDialog = () => {
      emit("editRightClosed");
    };

    const close = () => {
      errorMsgIsActive.value = false;
      errorMsg.value = "";
      updateConfirmDialog.value = false;
      updateInProgress.value = false;
      unsavedChangesDialog.value = false;
      successMsgIsActive.value = false;
      successMsg.value = "";
      formState.templateName = "";
      formState.templateDescription = "";
      v$.value.$reset();
      emitClosedDialog();
    };

    const cancel = () => {
      tmpRight.value = Object.assign({}, lastSavedRight.value);
      close();
    };

    const unsavedChangesDialog = ref(false);
    const checkForChangesAndClose = () => {
      if(formWasChanged.value){
        unsavedChangesDialog.value = true;
      } else {
        cancel();
      }
    };
    const closeUnsavedChangesDialog = () => {
      unsavedChangesDialog.value = false;
    };

    const cancelConfirm = () => {
      updateConfirmDialog.value = false;
    };

    /**
     * Deletion:
     */
    const deleteSuccessful = (index: number) => {
      if (isTemplate.value) {
        emit("deleteTemplateSuccessful", formState.templateName);
      } else {
        emit("deleteSuccessful", index, lastSavedRight.value.rightId);
      }
    };

    const deleteDialogClosed = () => {
      if (isTemplate.value) {
        dialogDeleteTemplate.value = false;
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
                handle: props.handle,
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
                errorMsgIsActive.value = true;
                errorMsg.value = "Speichern war nicht erfolgreich: " +  errMsg;
                updateConfirmDialog.value = false;
              });
            });
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsgIsActive.value = true;
            errorMsg.value = "Speichern war nicht erfolgreich: " +  errMsg;
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
          successMsg.value =
            "Rechteinformation " +
            tmpRight.value.rightId +
            " erfolgreich geupdated";
          successMsgIsActive.value = true;
          reinitializeRight();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsgIsActive.value = true;
            errorMsg.value = "Update war nicht erfolgreich: " +  errMsg;
            updateConfirmDialog.value = false;
          });
        });
    };

    /**
     * Templates
     */
    const createTemplate = () => {
      tmpRight.value.rightId = "unset";
      tmpRight.value.isTemplate = true;
      tmpRight.value.templateName = formState.templateName;
      tmpRight.value.templateDescription = formState.templateDescription;
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
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const updateTemplate = () => {
      tmpRight.value.templateName = formState.templateName;
      tmpRight.value.templateDescription = formState.templateDescription;
      templateApi
        .updateTemplate(tmpRight.value)
        .then(() => {
          if (tmpRight.value.rightId == undefined) {
            errorMsg.value =
              "No RightId found when updating. This should NOT happen.";
            errorMsgIsActive.value = true;
          } else {
            const exceptionIds: string[] = exceptionTemplateItems.value.flatMap(
              (e) => (e.rightId ? [e.rightId] : []),
            );
            addExceptionsToTemplate(
              tmpRight.value.rightId,
              exceptionIds,
              () => {
                updateBookmarks(tmpRight.value.rightId, () => {
                  successMsg.value =
                    "Template " +
                    tmpRight.value.templateName +
                    " erfolgreich geupdated";
                  successMsgIsActive.value = true;
                  emit("updateTemplateSuccessful", formState.templateName);
                  reinitializeRight();
                });
              },
            );
          }
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
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
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    /**
     * Refresh bookmarks.
     */
    const updateBookmarks = (rightId: string | undefined, callback: () => void) => {
      if (rightId == undefined){
        return;
      }
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
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const save: () => Promise<void> = async () => {
      // Vuelidate expects this field to be filled. When editing rights it is not required.
      if (!isTemplate.value) {
        formState.templateName = "foo";
      }
      tmpRight.value.groupIds = formState.selectedGroups.map((g: GroupRest) => g.groupId);
      tmpRight.value.groups = formState.selectedGroups;
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

      if (formState.endDate != undefined) {
        tmpRight.value.endDate = new Date();
        tmpRight.value.endDate.setUTCFullYear(
            formState.endDate.getFullYear()
        );

        tmpRight.value.endDate.setUTCMonth(
            formState.endDate.getMonth()
        );
        tmpRight.value.endDate.setUTCDate(
            formState.endDate.getDate()
        );
      }
      tmpRight.value.startDate = new Date();

      tmpRight.value.startDate.setUTCFullYear(
          formState.startDate.getFullYear()
      );
      tmpRight.value.startDate.setUTCMonth(
          formState.startDate.getMonth()
      );
      tmpRight.value.startDate.setUTCDate(
          formState.startDate.getDate()
      );
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
    });
    const computedRightId = computed(() => {
      // The check for undefined is required here!
        if (props.rightId == undefined) {
          return "";
        } else {
          return props.rightId;
        }
    });
    const computedReinitCounter = computed(() => props.reinitCounter);

    const isNew = computed(() => props.isNewRight || props.isNewTemplate);
    const isEditable = computed(
      () =>
        isNew.value ||
        (lastSavedRight.value != undefined && lastSavedRight.value.lastAppliedOn == undefined),
    );
    const isTemplate = computed(
      () =>
        props.isNewTemplate ||
        (lastSavedRight.value != undefined && lastSavedRight.value.isTemplate),
    );
    const isExistingTemplate = computed(
        () =>
            !props.isNewTemplate &&
            (lastSavedRight.value != undefined && lastSavedRight.value.isTemplate),
    );
    const isTemplateAndException = computed(
      () => isTemplate.value && props.isExceptionTemplate,
    );
    const isTemplateDraft = computed(
        () => isTemplate.value && lastSavedRight.value?.lastAppliedOn == undefined,
    );
    const exceptionsAllowed = computed(
      () =>
        !props.isExceptionTemplate &&
        (props.isNewTemplate ||
          (lastSavedRight.value != undefined && lastSavedRight.value.exceptionFrom == undefined)),
    );

    const cardTitle = computed(() => {
      const mode = isNew.value ? "erstellen" : "bearbeiten";
      if (isTemplate.value) {
        let description: string;
        if (props.isExceptionTemplate && lastSavedRight.value?.lastAppliedOn == undefined){
          description = "(Ausnahme und Entwurf)"
        } else if (props.isExceptionTemplate){
          description = "(Ausnahme)"
        } else if (lastSavedRight.value?.lastAppliedOn == undefined) {
          description = "(Entwurf)"
        } else {
          description = ""
        }
        return "Template " + description + " " + mode;
      } else {
        return "Rechteinformation " + mode;
      }
    });

    watch(computedRightId, () => {
      reinitializeRight();
    });
    watch(computedReinitCounter, () => {
      updateInProgress.value = false;
      if (isNew.value) {
        resetAllValues();
        addInitialBookmark();
      } else {
        getRightsData(() =>{
          setGivenValues();
          loadBookmarks();
        });
      }
    });

    const addInitialBookmark = () => {
      if (props.initialBookmark != undefined) {
        bookmarkItems.value = Array(props.initialBookmark);
      }
    };

    const setGivenValues = () => {
      tmpRight.value = Object.assign({}, lastSavedRight.value);
      if(tmpRight.value.groups != undefined) {
        formState.selectedGroups = tmpRight.value.groups;
      }
      formState.templateName =
        tmpRight.value.templateName == undefined ? "" : tmpRight.value.templateName;
      formState.templateDescription =
          tmpRight.value.templateDescription == undefined ? "" : tmpRight.value.templateDescription;
      formState.accessState = accessStateToString(tmpRight.value.accessState);
      formState.basisStorage = basisStorageToString(tmpRight.value.basisStorage);
      formState.basisAccessState = basisAccessStateToString(
        tmpRight.value.basisAccessState,
      );
      formState.startDate = tmpRight.value.startDate;
      if (tmpRight.value.endDate !== undefined) {
        formState.endDate = tmpRight.value.endDate;
      } else {
        formState.endDate = undefined;
      }
    };

    const resetAllValues = () => {
      tmpRight.value = Object.assign({} as RightRest);
      formState.endDate = undefined;
      formState.startDate = undefined;
      formState.templateName = "";
      formState.templateDescription = "";
      formState.accessState = "";
      bookmarkItems.value = [];
    };

    const getRightsData = (callback: () => void) => {
      if (props.rightId == undefined){
        return;
      }
      rightApi
          .getRightById(props.rightId)
          .then((r: RightRest) => {
            lastSavedRight.value = r;
            callback();
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };

    const reinitializeRight = () => {
      updateInProgress.value = false;
      getGroupList();
      if (!isNew.value) {
        getRightsData(() =>{
          setGivenValues();
          if (isTemplate.value) {
            loadBookmarks();
            loadExceptions();
          }
        });
      } else {
        resetAllValues();
        addInitialBookmark();
        if(props.initialRight != undefined){
          lastSavedRight.value = Object.assign({}, props.initialRight);
          setGivenValues();
        }
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
    const lastSavedBookmarkItems: Ref<Array<BookmarkRest>> = ref([]);
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
    const lastSavedExceptionTemplateItems: Ref<Array<RightRest>> = ref([]);
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
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
      }
    };

    // Load Bookmarks
    const loadBookmarks = () => {
      if (computedRightId.value == undefined) {
        errorMsg.value =
          "Error while loading bookmarks. Invalid Template ID.";
        errorMsgIsActive.value = true;
      } else {
        templateApi
          .getBookmarksByRightId(computedRightId.value)
          .then((bookmarks: Array<BookmarkRest>) => {
            bookmarkItems.value = bookmarks;
            lastSavedBookmarkItems.value = Array.from(bookmarks);
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
      }
    };

    const loadExceptions = () => {
      if (computedRightId.value == undefined) {
        errorMsg.value =
          "Error while loading bookmarks. Invalid Template ID.";
        errorMsgIsActive.value = true;
      } else {
        templateApi
          .getExceptionsById(computedRightId.value)
          .then((exceptions: Array<RightRest>) => {
            exceptionTemplateItems.value = exceptions;
            lastSavedExceptionTemplateItems.value = Array.from(exceptions);
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
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
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");
    const groupItems: Ref<Array<GroupRest>> = ref([]);
    const getGroupList = () => {
      api
        .getGroupList(0, 100)
        .then((r: Array<GroupRest>) => {
          groupItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    // DryRun + Dashboard
    const currentTemplateApplicationResult = ref({} as TemplateApplicationRest)
    const dashboardViewActivated = ref(false);
    const dialogSimulationResults = ref(false);
    const testId: Ref<string | undefined> = ref(undefined);

    const dryRunTemplate = () => {
      if (tmpRight.value.rightId == undefined){
        return;
      }
      templateApi
          .applyTemplates(
              [tmpRight.value.rightId],
              false,
              false,
              true,
          )
          .then((r: TemplateApplicationsRest) => {
            if (r.templateApplication.length == 0){
              return;
            }
            testId.value = r.templateApplication[0].testId;
            currentTemplateApplicationResult.value = r.templateApplication[0];
            dialogSimulationResults.value = true;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };
    const openDashboard = () => {
      dashboardViewActivated.value = true;
    };
    const closeDashboard = () => {
      dashboardViewActivated.value = false;
    };
    const closeDialogSimulationResult = () => {
      dialogSimulationResults.value = false;
    };

    const readOnlyProps = computed(() => {
      if (!isEditable.value) {
        return {
          "readonly": true,
          "bg-color": "grey-lighten-2",
        };
      } else {
        return {
          "bg-color": "white",
          "clearable" : true
        };
      }
    });
    watch(dashboardViewActivated, (currentValue) => {
      if(!currentValue && testId.value != undefined){
        dialogSimulationResults.value = false;
        rightErrorApi.deleteRightErrorsByTestId(testId.value)
      }
    });

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
      currentTemplateApplicationResult,
      dashboardViewActivated,
      dialogCreateException,
      dialogDeleteRight,
      dialogDeleteTemplate,
      dialogSimulationResults,
      endDateFormatted,
      errorAccessState,
      errorEndDate,
      errorTemplateName,
      errorStartDate,
      exceptionsAllowed,
      formWasChanged,
      isEditable,
      isExistingTemplate,
      isTemplateAndException,
      isTemplateDraft,
      isNew,
      isTemplate,
      errorIPGroup,
      errorMsgIsActive,
      errorMsg,
      groupItems,
      historyStore,
      isStartDateMenuOpen,
      isEndDateMenuOpen,
      lastSavedRight,
      menuStartDate,
      menuEndDate,
      metadataCount,
      openPanelsDefault,
      openBookmarkSearch,
      renderBookmarkKey,
      renderTemplateKey,
      startDateFormatted,
      exceptionTemplateItems,
      exceptionTemplateHeaders,
      readOnlyProps,
      unsavedChangesDialog,
      updateConfirmDialog,
      successMsgIsActive,
      successMsg,
      testId,
      tmpRight,
      updateInProgress,
      // methods
      addNewException,
      cancel,
      cancelConfirm,
      checkForChangesAndClose,
      closeCreateExceptionDialog,
      closeDashboard,
      closeDialogSimulationResult,
      closeUnsavedChangesDialog,
      createRight,
      initiateDeleteDialog,
      deleteBookmarkEntry,
      deleteDialogClosed,
      deleteExceptionEntry,
      deleteSuccessful,
      dryRunTemplate,
      openCreateExceptionDialog,
      openDashboard,
      selectBookmark,
      setSelectedBookmarks,
      save,
      templateBookmarkClosed,
      updateRight,
    };
  },
});
</script>

<style >
.v-expansion-panel-text__wrapper {
  max-height: calc(700px - 64px - (4 * 48px));
  overflow: scroll;
}

.my-scroll {
  overflow-y: scroll;
}
</style>

<template>
  <v-card position="relative" class="my-scroll">
   <v-toolbar>
    <v-spacer></v-spacer>
    <v-btn
        icon="mdi-close"
        @click="checkForChangesAndClose"
    ></v-btn>
    </v-toolbar>
    <v-dialog
    max-width="500px"
    :retain-focus="false"
    v-model="dialogSimulationResults"
    >
      <v-card>

        <div class="d-flex align-center justify-space-between">
          <v-card-title>Ergebnisse Simulation
            <v-spacer></v-spacer>
          </v-card-title>
          <v-btn @click="closeDialogSimulationResult" icon="mdi-close"></v-btn>
        </div>
        <v-card-text>
          <v-row>
            <v-col>
              {{info.constructApplicationInfoText(currentTemplateApplicationResult)}}
            </v-col>
          </v-row>
          <v-row>
            <v-col>Anzahl Fehler: {{currentTemplateApplicationResult.numberOfErrors}}</v-col>
          </v-row>
          <v-row v-if="currentTemplateApplicationResult.numberOfErrors != 0">
            <v-col>
            Fehler in Dashboard ansehen:
            <v-btn
                @click="openDashboard"
                color="blue darken-1"
            >
              Hier klicken
            </v-btn>
            </v-col>
          </v-row>
        </v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog
        v-model="dashboardViewActivated"
        :retain-focus="false"
        max-width="1000px"
        v-on:close="closeDashboard"
    >
      <Dashboard
          :test-id="testId"
      ></Dashboard>
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
              @click="cancel">
            Änderungen verwerfen
          </v-btn>
          <v-spacer></v-spacer>
        </v-card-actions>
      </v-card>
    </v-dialog>
    <v-card-title>
      <v-row>
        <v-col>
          {{ cardTitle }}
        </v-col>
        <v-col cols="1" offset="4">
          <v-tooltip location="bottom" text="Ausnahme Template">
            <template v-slot:activator="{ props }">
              <v-icon v-if="isTemplateAndException" v-bind="props">
                mdi-alpha-a-box-outline
              </v-icon>
            </template>
          </v-tooltip>
        </v-col>
        <v-col cols="1">
          <v-tooltip location="bottom" text="Template Entwurf">
            <template v-slot:activator="{ props }">
              <v-icon v-if="isTemplateDraft" v-bind="props">
                mdi-alpha-e-box-outline
              </v-icon>
            </template>
          </v-tooltip>
        </v-col>
      </v-row>
    </v-card-title>
    <v-card-actions>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          v-model="errorMsgIsActive"
          color="error"
      >
        {{ errorMsg }}
      </v-snackbar>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          v-model="successMsgIsActive"
          color="success"
      >
        <span v-html="successMsg"></span>
      </v-snackbar>
      <v-spacer></v-spacer>
      <v-btn
          density="compact"
          icon="mdi-help"
          href="https://zbwintern/wiki/x/8wPUG"
          target="_blank"
      ></v-btn>
      <v-btn v-if="!isTemplate" :readonly="updateInProgress" color="blue darken-1" @click="save"
        >Speichern
      </v-btn>

      <v-tooltip
        location="bottom"
      >
        <template v-slot:activator="{ props }">
          <div v-bind="props" class="d-inline-block">
            <v-btn
              :disabled="isNew || isTemplateAndException"
              @click="initiateDeleteDialog"
            >
              <v-icon>mdi-delete</v-icon>
            </v-btn>
          </div>
        </template>
        <span v-if="isNew || isTemplateAndException">Ausnahme-Templates können nicht gelöscht werden</span>
        <span v-else>Löschen</span>
      </v-tooltip>

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
    <v-card-text style="height:1100px;">
    <v-expansion-panels bg-color="light-blue-lighten-5" v-model="openPanelsDefault" focusable variant="accordion">
      <v-expansion-panel v-if="isTemplate" value="0">
          <v-expansion-panel-title>
            Template Informationen
          </v-expansion-panel-title>
          <v-expansion-panel-text>
            <v-container fluid>
              <v-row>
                <v-col cols="4"> Template Name</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="formState.templateName"
                    :error-messages="errorTemplateName"
                    v-bind="{...$attrs, ...readOnlyProps}"
                    hint="Name des Templates"
                    variant="outlined"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">Beschreibung</v-col>
                <v-col cols="8">
                  <v-textarea
                    v-model="formState.templateDescription"
                    hint="Beschreibung des Templates"
                    variant="outlined"
                    v-bind="{...$attrs, ...readOnlyProps}"
                  ></v-textarea>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4"> Erstellt am</v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.createdOn"
                    variant="outlined"
                    readonly
                    bg-color="grey-lighten-2"
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
                    bg-color="grey-lighten-2"
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
                    bg-color="grey-lighten-2"
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
                    bg-color="grey-lighten-2"
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
                    bg-color="grey-lighten-2"
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
                      <v-tooltip
                          location="bottom"
                          :disabled="lastSavedRight?.lastAppliedOn == undefined"
                      >
                        <template v-slot:activator="{ props }">
                          <div v-bind="props" class="d-inline-block">
                            <v-btn
                                :disabled="!isEditable || !(lastSavedRight?.lastAppliedOn == undefined)"
                                @click="deleteBookmarkEntry(item)"
                                >
                            <v-icon
                                small
                            >
                              mdi-delete
                            </v-icon>
                            </v-btn>
                          </div>
                        </template>
                        <span>
                          Verknüpfte Suche kann nicht gelöscht werden, weil das Template bereits angewendet wurde.
                        </span>
                      </v-tooltip>

                    </template>
                  </v-data-table>
                  <v-btn
                      v-if="isEditable"
                      color="blue darken-1"
                      @click="selectBookmark"
                      :disabled="!isEditable || bookmarkItems.length > 0"
                  >Gespeicherte Suche verknüpfen
                  </v-btn>
                  <v-tooltip location="bottom">
                    <template v-slot:activator="{ props }">
                      <div v-bind="props" class="d-inline-block">
                      <v-btn
                        v-if="!isEditable"
                        :disabled="!isEditable"
                        v-bind="props"
                        >Gespeicherte Suche verknüpfen
                      </v-btn>
                      </div>
                    </template>
                    <span>
                      Änderungen müssen erst abgespeichert werden.
                    </span>
                  </v-tooltip>
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
                    max-width="1500px"
                    max-height="850px"
                    scrollable
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
      <v-expansion-panel value="1">
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
                  readonly
                  hint="Rechte Id"
                  label="Wird automatisch generiert"
                  bg-color="grey-lighten-2"
                  variant="outlined"
                ></v-text-field>
                <v-text-field
                  v-if="!isNew"
                  ref="rightId"
                  v-model="tmpRight.rightId"
                  readonly
                  bg-color="grey-lighten-2"
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
                  v-bind="{...$attrs, ...readOnlyProps}"
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
                  :disabled="!isEditable"
                >
                  <template v-slot:activator="{ props }">
                   <v-text-field
                      :modelValue="startDateFormatted"
                      :error-messages="errorStartDate"
                      label="Start-Datum"
                      variant="outlined"
                      prepend-icon="mdi-calendar"
                      required
                      readonly
                      v-bind="{...$attrs, ...props, ...readOnlyProps}"
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
                      clearable
                      bg-color="white"
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
              <v-col cols="4">IP-Gruppe</v-col>
              <v-col cols="8">
                <v-select
                  v-if="!isEditable || formState.accessState != 'Restricted'"
                  bg-color="grey-lighten-2"
                  readonly
                  v-model="formState.selectedGroups"
                  :items="groupItems"
                  :error-messages="errorIPGroup"
                  @blur="v$.selectedGroups.$touch()"
                  @change="v$.selectedGroups.$touch()"
                  chips
                  multiple
                  counter
                  hint="Einschränkung des Zugriffs auf Berechtigungsgruppen (nur verfügbar für Restricted)"
                  variant="outlined"
                  return-object
                  item-title="title"
                >
                </v-select>
                <v-select
                  v-else
                  v-model="formState.selectedGroups"
                  :items="groupItems"
                  :error-messages="errorIPGroup"
                  @blur="v$.selectedGroups.$touch()"
                  @change="v$.selectedGroups.$touch()"
                  bg-color="white"
                  chips
                  multiple
                  counter
                  hint="Einschränkung des Zugriffs auf Berechtigungsgruppen"
                  variant="outlined"
                  return-object
                  item-title="title"
                >
                </v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-model="tmpRight.notesGeneral"
                  counter
                  v-bind="{...$attrs, ...readOnlyProps}"
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
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.licenceContract"
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
                  :readonly="!isEditable"
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
                  :readonly="!isEditable"
                  color="indigo"
                  hint="Gibt Auskunft darüber, ob eine Nutzungsvereinbarung für dieses Item als Nutzungsrechtsquelle vorliegt."
                  label="Ja"
                  persistent-hint
                ></v-switch>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Uneingeschränkte Open-Content-Lizenz</v-col>
              <v-col cols="8">
                <v-text-field
                  v-bind="{...$attrs, ...readOnlyProps}"
                  hint="Eine per URI eindeutig referenzierte Standard-Open-Content-Lizenz, die für das Item gilt."
                  v-model="tmpRight.openContentLicence"
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
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.nonStandardOpenContentLicenceURL"
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
                  :readonly="!isEditable"
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
                  :readonly="!isEditable"
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
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.notesFormalRules"
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
      <v-expansion-panel value="2">
        <v-expansion-panel-title
          >Prozessdokumentierende Elemente
        </v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row>
              <v-col cols="4"> Basis der Speicherung</v-col>
              <v-col cols="8">
                <v-select
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="formState.basisStorage"
                  :items="basisStorage"
                  variant="outlined"
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Basis des Access-Status</v-col>
              <v-col cols="8">
                <v-select
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="formState.basisAccessState"
                  :items="basisAccessState"
                  variant="outlined"
                ></v-select>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.notesProcessDocumentation"
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
      <v-expansion-panel value="3">
        <v-expansion-panel-title>
          Metadaten über den Rechteinformationseintrag
        </v-expansion-panel-title>
        <v-expansion-panel-text eager>
          <v-container fluid>
            <v-row v-if="!isTemplate">
              <v-col cols="4"> Erstellt am</v-col>
              <v-col cols="8">
                <v-text-field
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.createdOn"
                  variant="outlined"
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
                  bg-color="grey-lighten-2"
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
                  bg-color="grey-lighten-2"
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
                  bg-color="grey-lighten-2"
                ></v-text-field>
              </v-col>
            </v-row>
            <v-row>
              <v-col cols="4"> Bemerkungen</v-col>
              <v-col cols="8">
                <v-textarea
                  v-bind="{...$attrs, ...readOnlyProps}"
                  v-model="tmpRight.notesManagementRelated"
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
    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn :disabled="updateInProgress" color="blue darken-1" @click="save"
        >Speichern
      </v-btn>
      <v-tooltip location="bottom">
        <template v-slot:activator="{ props }">
          <div v-bind="props" class="d-inline-block">
            <v-btn
                v-if="isTemplate"
                color="blue darken-1"
                v-bind="props"
                @click="dryRunTemplate"
                :readonly="isNewTemplate || formWasChanged"
            >Testen</v-btn>
          </div>
        </template>
        <span v-if="formWasChanged">
            Nur auswählbar für gespeicherte Templates.
        </span>
      </v-tooltip>
    </v-card-actions>
    <v-dialog v-model="updateConfirmDialog" max-width="500px">
      <v-card>
        <v-card-title class="text-h5">Achtung</v-card-title>
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
