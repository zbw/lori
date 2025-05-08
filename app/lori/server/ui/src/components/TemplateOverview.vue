<script lang="ts">
import {computed, defineComponent, onMounted, Ref, ref, watch} from "vue";
import date_utils from "@/utils/date_utils";
import error from "@/utils/error";
import info from "@/utils/info";
import templateApi from "@/api/templateApi";
import {
  RightErrorRest,
  RightRest,
  TemplateApplicationRest,
  TemplateApplicationsRest,
} from "@/generated-sources/openapi";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import {useUserStore} from "@/stores/user";

export default defineComponent({
  components: { RightsEditDialog },
  props: {},
  emits: [
      "getItemsByRightId",
      "templateOverviewClosed",
  ],
  setup(props, { emit }) {
    /**
     * Stores:
     */
    const userStore = useUserStore();
    /**
     *  Data-Table related.
     */
    const renderKey = ref(0);
    const headers = [
      {
        title: "Template Name",
        align: "start",
        value: "templateName",
        sortable: true,
      },
      {
        title: "Status",
        value: "status",
        sortable: false,
      },
      {
        title: "Items verknüpfen",
        value: "displayConnectedItems",
        sortable: true,
      },
      {
        title: "Ausnahme",
        align: "start",
        value: "isException",
        sortable: false,
      },
      {
        title: "Template anwenden",
        value: "applyTemplate",
        sortable: true,
      },
      {
        title: "Aktionen",
        value: "actions",
        sortable: false,
        width: "100px",
        minWidth: "100px",
        maxWidth: "100px",
      },
      {
        title: "Gültigkeit Startdatum",
        value: "startDate",
        sortable: false,
      },
      {
        title: "Gültigkeit Enddatum",
        value: "endDate",
        sortable: false,
      },
    ];
    const selectedHeaders = ref(headers.slice(0, 6));
    const headersValueVSelect = ref(selectedHeaders.value);
    const templateItems: Ref<Array<RightRest>> = ref([]);
    const searchTerm = ref("");

    /**
     * Error messages.
     */
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");
    const templateApplyError = ref(false);
    const templateApplyErrorMsg = ref("");
    const templateApplyErrorNumber = ref(-1);
    const templateApplyItemsApplied = ref(-1);

    /**
     * Template properties.
     */
    const isNew = ref(true);
    const reinitCounter = ref(0);
    const currentTemplate = ref({} as RightRest);
    const templateDraft = ref({} as RightRest);
    const getTemplateList = () => {
      templateApi
        .getTemplateList(
            0,
            100,
            undefined,
            undefined,
            undefined,
            undefined,
        )
        .then((r: Array<RightRest>) => {
          templateItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const templateEditDialogActivated = ref(false);
    const activateTemplateEditDialog = () => {
      successMsgIsActive.value = false;
      templateEditDialogActivated.value = true;
    };
    const createNewTemplate = () => {
      isNew.value = true;
      currentTemplate.value = Object.assign({} as RightRest);
      reinitCounter.value = reinitCounter.value + 1;
      activateTemplateEditDialog();
    };

    const closeTemplateEditDialog = () => {
      templateEditDialogActivated.value = false;
      templateDraft.value = Object.assign({} as RightRest);
    };

    const editTemplate = (templateRight: RightRest) => {
      isNew.value = false;
      reinitCounter.value = reinitCounter.value + 1;
      currentTemplate.value = templateRight;
      activateTemplateEditDialog();
    };

    const copyTemplate = (templateRight: RightRest) => {
      isNew.value = true;
      templateDraft.value = Object.assign({}, templateRight);
      templateDraft.value.rightId = undefined;
      templateDraft.value.exceptionOfId = undefined;
      templateDraft.value.createdBy = undefined;
      templateDraft.value.createdOn = undefined;
      templateDraft.value.lastAppliedOn = undefined;
      templateDraft.value.lastUpdatedOn = undefined;
      templateDraft.value.lastUpdatedBy = undefined;
      templateDraft.value.templateName = "KOPIE - " + templateDraft.value.templateName;
      reinitCounter.value = reinitCounter.value + 1;
      activateTemplateEditDialog();
    };

    const applyTemplate = (template: RightRest) => {
      if (template.rightId == undefined) {
        return;
      }
      templateApi
        .applyTemplates(
            [template.rightId],
            false,
            false,
            false,
        )
        .then((r: TemplateApplicationsRest) => {
          const templateApplicationResult: TemplateApplicationRest =
            r.templateApplication[0];
          const infoMsg = info.constructApplicationInfoText(
            templateApplicationResult,
          );
          successMsgIsActive.value = true;
          successMsg.value = infoMsg;
          templateApplyItemsApplied.value = r.templateApplication.length; // TODO: Handle error messages for exceptions

          // Check for errors
          let exceptionErrors: Array<RightErrorRest> = [];
          if (
            templateApplicationResult.exceptionTemplateApplication !==
            undefined
          ) {
            exceptionErrors =
              templateApplicationResult.exceptionTemplateApplication.errors ?? [];
          }
          const errors: Array<RightErrorRest> = r.templateApplication
            .flatMap((t) => (t.errors != undefined ? t.errors : []))
            .concat(exceptionErrors);
          if (errors.length > 0) {
            templateApplyError.value = true;
            templateApplyErrorMsg.value = errors
              .map((err) => err.message)
              .join("\n");
            templateApplyErrorNumber.value = errors.length;
          }
          updateTemplateOverview();
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    const closeApplyErrorMsg = () => {
      templateApplyError.value = false;
    };

    /**
     * Alerts:
     */
    const successMsgIsActive = ref(false);
    const successMsg = ref("");

    /**
     * Child events:
     */
    const lastModifiedTemplateName = ref("");
    const childTemplateAdded = (template: RightRest) => {
      lastModifiedTemplateName.value =
        template.templateName == undefined ? "invalid" : template.templateName;
      successMsgIsActive.value = true;
      successMsg.value =
        "Template " +
        lastModifiedTemplateName.value +
        " erfolgreich hinzugefügt.";
      updateTemplateOverview();
    };

    const childTemplateDeleted = (templateName: string) => {
      templateEditDialogActivated.value = false;
      lastModifiedTemplateName.value = templateName;
      successMsgIsActive.value = true;
      successMsg.value =
        "Template " + lastModifiedTemplateName.value + " erfolgreich gelöscht.";
      updateTemplateOverview();
    };

    const childTemplateUpdated = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      successMsgIsActive.value = true;
      successMsg.value =
        "Template '" + lastModifiedTemplateName.value + "' erfolgreich editiert.";
      updateTemplateOverview();
      closeTemplateEditDialog();
    };

    const updateTemplateOverview = () => {
      getTemplateList();
      renderKey.value += 1;
    };

    const tooltipEditText = computed(() => {
      if(userStore.isLoggedIn){
        return "Bearbeiten";
      } else {
        return "Anzeigen";
      }
    });

    /**
     * Specific column logic
     */
    const datePrettyPrint: (date: (Date | undefined)) => string = (date: Date | undefined) => {
      if(date == undefined){
        return ""
      } else {
        return date_utils.dateToIso8601(date);
      }
    }

    const currentDate = ref<Date>(new Date());
    const isTemplateValid: (right: RightRest) => boolean = (right: RightRest) => {
      let endDate: Date;
      if(right.endDate != undefined){
        endDate = new Date(right.endDate);
        endDate.setDate(endDate.getDate() + 1);
      }
      if(currentDate.value > right.startDate && right.endDate == undefined ){
        return true;
      }
      return currentDate.value > right.startDate && currentDate.value < endDate;
    };

    /**
     * EMITS
     */
    const emitGetItemsByRightId = (rightId?: string, templateName?: string) => {
      if (rightId != undefined && templateName != undefined) {
        emit("getItemsByRightId", rightId, templateName);
      } else {
        console.log("Error: RightId or TemplateName where undefined: RightId: "
            + rightId + "; Template Name: " + templateName);
      }
    };

    /**
     * Closing:
     */
    const close = () => {
      emit("templateOverviewClosed");
    };

    /**
     * Watches:
     */
    watch(headersValueVSelect, (currentValue) => {
      selectedHeaders.value = currentValue;
    });

    /**
     * Mounting:
     */
    onMounted(() => getTemplateList());

    return {
      currentTemplate,
      headers,
      headersValueVSelect,
      lastModifiedTemplateName,
      isNew,
      reinitCounter,
      renderKey,
      searchTerm,
      selectedHeaders,
      successMsgIsActive,
      successMsg,
      templateApplyError,
      templateApplyErrorMsg,
      templateApplyErrorNumber,
      templateApplyItemsApplied,
      templateEditDialogActivated,
      tooltipEditText,
      errorMsgIsActive,
      errorMsg,
      userStore,
      templateDraft,
      templateItems,
      applyTemplate,
      childTemplateAdded,
      childTemplateDeleted,
      childTemplateUpdated,
      close,
      closeApplyErrorMsg,
      closeTemplateEditDialog,
      copyTemplate,
      createNewTemplate,
      datePrettyPrint,
      editTemplate,
      emitGetItemsByRightId,
      isTemplateValid,
      getTemplateList,
      updateTemplateOverview,
    };
  },
});
</script>

<style scoped>
.multi-line {
  white-space: pre-line;
}
.tooltip-btn {
  margin-right: 10px; /* Adds space between the two buttons */
}
.invisible {
  visibility: hidden;
}
</style>
<template>
  <v-card position="relative">
    <v-toolbar>
      <v-spacer></v-spacer>
      <v-btn
          icon="mdi-close"
          @click="close"
      ></v-btn>
    </v-toolbar>
    <v-container>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          v-model="successMsgIsActive"
          color="success"
      >
        {{ successMsg }}
      </v-snackbar>
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
      <v-dialog v-model="templateApplyError" max-width="500px">
        <v-card>
          <v-card-title class="text-h5"
            >Template Anwendung (teilweise) fehlgeschlagen</v-card-title
          >
          <v-card-text>
            Templates angewandt: {{ templateApplyItemsApplied }}<br />
            Anzahl Fehler: {{ templateApplyErrorNumber }}<br />
            Details zu den Fehlern:
            <v-textarea
              :value="templateApplyErrorMsg"
              readonly
              background-color="red lighten-4"
              color="black"
            >
            </v-textarea>
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn small color="primary" dark @click="closeApplyErrorMsg">
              OK
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-dialog>
      <v-card-title>Templates</v-card-title>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
            color="blue darken-1"
            @click="createNewTemplate"
            :disabled="!userStore.isLoggedIn"
          >Neues Template anlegen
        </v-btn>
      </v-card-actions>

      <v-select
          v-model="headersValueVSelect"
          :items="headers"
          label="Spaltenauswahl"
          multiple
          return-object
      >
        <template v-slot:selection="{ item, index }">
          <v-chip v-if="index === 0">
            <span>{{ item.title }}</span>
          </v-chip>
          <span v-if="index === 1" class="grey--text caption"
          >(+{{ headersValueVSelect.length - 1 }} weitere)</span
          >
        </template>
      </v-select>

      <v-text-field
        v-model="searchTerm"
        append-icon="mdi-magnify"
        hide-details
        label="Suche"
        single-line
      ></v-text-field>
      <v-data-table
        :key="renderKey"
        :headers="selectedHeaders"
        :items="templateItems"
        :search="searchTerm"
        item-value="templateName"
        loading-text="Daten werden geladen... Bitte warten."
      >
        <template v-slot:item.displayConnectedItems="{ item }">
          <v-btn
            color="blue darken-1"
            @click="emitGetItemsByRightId(item.rightId, item.templateName)"
            >Alle verknüpften Items anzeigen
          </v-btn>
        </template>
        <template v-slot:item.status="{ item }">
          <v-tooltip
              v-if="isTemplateValid(item)"
              location="bottom"
              text="Aktuell gültiges Template"
          >
            <template v-slot:activator="{ props }">
              <v-icon v-bind="props">
                mdi-star
              </v-icon>
            </template>
          </v-tooltip>
          <v-icon v-else class="invisible">
            mdi-star
          </v-icon>
          <v-tooltip location="bottom" text="Entwurf">
            <template v-slot:activator="{ props }">
              <v-icon v-bind="props" v-if="item.lastAppliedOn == undefined">
                mdi-alpha-e-box-outline
              </v-icon>
            </template>
          </v-tooltip>
        </template>
        <template v-slot:item.applyTemplate="{ item }">
          <v-btn
            v-if="item.exceptionOfId == undefined"
            color="blue darken-1"
            @click="applyTemplate(item)"
            :disabled="!userStore.isLoggedIn"
            >Template anwenden</v-btn
          >
        </template>
        <template v-slot:item.isException="{ item }">
          <v-tooltip location="bottom" text="Ausnahme">
            <template v-slot:activator="{ props }">
              <v-icon v-bind="props" v-if="item.exceptionOfId !== undefined">
                mdi-alpha-a-box-outline
              </v-icon>
            </template>
          </v-tooltip>
        </template>
        <template v-slot:item.actions="{ item }">
          <v-tooltip location="bottom" text="Kopieren">
            <template v-slot:activator="{ props }">
              <v-icon
                  class="tooltip-btn"
                  small
                  v-bind="props"
                  @click="copyTemplate(item)"
                  :disabled="!userStore.isLoggedIn"
              >
                mdi-content-copy
              </v-icon>
            </template>
          </v-tooltip>
          <v-tooltip location="bottom" :text="tooltipEditText">
            <template v-slot:activator="{ props }">
              <v-icon
                  small
                  v-bind="props"
                  @click="editTemplate(item)"
                  class="tooltip-btn"
                  v-if="userStore.isLoggedIn"
              >
                mdi-pencil
              </v-icon>
              <v-icon
                  small
                  v-bind="props"
                  @click="editTemplate(item)"
                  class="tooltip-btn"
                  v-else
              >
                mdi-eye
              </v-icon>
            </template>
          </v-tooltip>
        </template>
        <template v-slot:item.startDate="{ item }">
          {{ datePrettyPrint(item.startDate)}}
        </template>
        <template v-slot:item.endDate="{ item }">
          {{ datePrettyPrint(item.endDate)}}
        </template>
      </v-data-table>
      <v-dialog
        v-model="templateEditDialogActivated"
        :retain-focus="false"
        max-width="1500px"
        max-height="850px"
        v-on:close="closeTemplateEditDialog"
        scrollable
        persistent
      >
        <RightsEditDialog
          :index="-1"
          :isNewRight="false"
          :isNewTemplate="isNew"
          :reinit-counter="reinitCounter"
          :is-exception-template="
            currentTemplate.exceptionOfId !== undefined &&
            currentTemplate.exceptionOfId != ''
          "
          :rightId="currentTemplate.rightId"
          :initialRight="templateDraft"
          v-on:addTemplateSuccessful="childTemplateAdded"
          v-on:deleteTemplateSuccessful="childTemplateDeleted"
          v-on:editRightClosed="closeTemplateEditDialog"
          v-on:updateTemplateSuccessful="childTemplateUpdated"
        ></RightsEditDialog>
      </v-dialog>
    </v-container>
  </v-card>
</template>
