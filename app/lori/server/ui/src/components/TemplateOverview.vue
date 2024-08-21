<script lang="ts">
import { defineComponent, onMounted, Ref, ref } from "vue";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";
import {
  RightErrorRest,
  RightRest,
  TemplateApplicationRest,
  TemplateApplicationsRest,
} from "@/generated-sources/openapi";
import { useDialogsStore } from "@/stores/dialogs";
import RightsEditDialog from "@/components/RightsEditDialog.vue";

export default defineComponent({
  components: { RightsEditDialog },
  props: {},
  emits: ["getItemsByRightId"],
  setup(props, { emit }) {
    /**
     * Stores.
     */
    const dialogStore = useDialogsStore();
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
        title: "Ausnahme",
        align: "start",
        value: "isException",
        sortable: false,
      },
      {
        title: "Items verknüpfen",
        value: "displayConnectedItems",
        sortable: true,
      },
      {
        title: "Template anwenden",
        value: "applyTemplate",
        sortable: true,
      },
      { title: "Aktionen", value: "actions", sortable: false },
    ];
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
    const getTemplateList = () => {
      templateApi
        .getTemplateList(0, 100)
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

    const activateTemplateEditDialog = () => {
      successMsgIsActive.value = false;
      dialogStore.templateEditActivated = true;
    };
    const createNewTemplate = () => {
      isNew.value = true;
      reinitCounter.value = reinitCounter.value + 1;
      activateTemplateEditDialog();
    };

    const closeTemplateEditDialog = () => {
      dialogStore.templateEditActivated = false;
    };

    const editTemplate = (templateRight: RightRest) => {
      isNew.value = false;
      reinitCounter.value = reinitCounter.value + 1;
      currentTemplate.value = templateRight;
      activateTemplateEditDialog();
    };

    const applyTemplate = (template: RightRest) => {
      if (template.rightId == undefined) {
        return;
      }
      templateApi
        .applyTemplates([template.rightId])
        .then((r: TemplateApplicationsRest) => {
          const templateApplicationResult: TemplateApplicationRest =
            r.templateApplication[0];
          const infoMsg = constructApplicationInfoText(
            templateApplicationResult,
          );
          successMsgIsActive.value = true;
          successMsg.value = infoMsg;
          templateApplyItemsApplied.value = r.templateApplication.length; // TODO: Handle error messages for exceptions

          // Check for errors
          let exceptionErrors: Array<RightErrorRest> = [];
          if (
            templateApplicationResult.exceptionTemplateApplications !==
            undefined
          ) {
            exceptionErrors =
              templateApplicationResult.exceptionTemplateApplications.flatMap(
                (t) => (t.errors != undefined ? t.errors : []),
              );
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

    const constructApplicationInfoText: (
      templateApplication: TemplateApplicationRest,
    ) => string = (templateApplication: TemplateApplicationRest) => {
      const parent: string =
        "Template '" +
        templateApplication.templateName +
        "' wurde für " +
        templateApplication.numberOfAppliedEntries +
        " Einträge angewandt.";
      let exceptions: string = "";
      if (templateApplication.exceptionTemplateApplications !== undefined) {
        exceptions = templateApplication.exceptionTemplateApplications
          .map(
            (tA: TemplateApplicationRest) =>
              "Template (Ausnahme) '" +
              tA.templateName +
              "' wurde für " +
              tA.numberOfAppliedEntries +
              " Einträge angewandt.",
          )
          .join("\n");
      }
      return parent + "\n" + exceptions;
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
        "Template " + lastModifiedTemplateName.value + " erfolgreich editiert.";
      updateTemplateOverview();
      closeTemplateEditDialog();
    };

    const updateTemplateOverview = () => {
      getTemplateList();
      renderKey.value += 1;
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

    onMounted(() => getTemplateList());

    return {
      currentTemplate,
      dialogStore,
      headers,
      lastModifiedTemplateName,
      isNew,
      reinitCounter,
      renderKey,
      searchTerm,
      successMsgIsActive,
      successMsg,
      templateApplyError,
      templateApplyErrorMsg,
      templateApplyErrorNumber,
      templateApplyItemsApplied,
      errorMsgIsActive,
      errorMsg,
      templateItems,
      applyTemplate,
      childTemplateAdded,
      childTemplateDeleted,
      childTemplateUpdated,
      closeApplyErrorMsg,
      closeTemplateEditDialog,
      createNewTemplate,
      editTemplate,
      emitGetItemsByRightId,
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
</style>
<template>
  <v-card position="relative">
    <v-container>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="10000"
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
          timeout="10000"
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
      <v-card-title>Template Übersicht</v-card-title>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" @click="createNewTemplate"
          >Neues Template anlegen
        </v-btn>
      </v-card-actions>
      <v-text-field
        v-model="searchTerm"
        append-icon="mdi-magnify"
        hide-details
        label="Suche"
        single-line
      ></v-text-field>
      <v-data-table
        :key="renderKey"
        :headers="headers"
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
        <template v-slot:item.applyTemplate="{ item }">
          <v-btn
            v-if="item.exceptionFrom == undefined"
            color="blue darken-1"
            @click="applyTemplate(item)"
            >Template anwenden</v-btn
          >
        </template>
        <template v-slot:item.isException="{ item }">
          <v-icon v-if="item.exceptionFrom !== undefined">
            mdi-alpha-a-box-outline
          </v-icon>
        </template>
        <template v-slot:item.actions="{ item }">
          <v-icon small @click="editTemplate(item)">mdi-pencil</v-icon>
        </template>
      </v-data-table>
      <v-dialog
        v-model="dialogStore.templateEditActivated"
        :retain-focus="false"
        max-width="1000px"
        v-on:close="closeTemplateEditDialog"
      >
        <RightsEditDialog
          :index="-1"
          :isNewRight="false"
          :isNewTemplate="isNew"
          :reinit-counter="reinitCounter"
          :is-exception-template="
            currentTemplate.exceptionFrom !== undefined &&
            currentTemplate.exceptionFrom != ''
          "
          :right="currentTemplate"
          v-on:addTemplateSuccessful="childTemplateAdded"
          v-on:deleteTemplateSuccessful="childTemplateDeleted"
          v-on:editRightClosed="closeTemplateEditDialog"
          v-on:updateTemplateSuccessful="childTemplateUpdated"
        ></RightsEditDialog>
      </v-dialog>
    </v-container>
  </v-card>
</template>
