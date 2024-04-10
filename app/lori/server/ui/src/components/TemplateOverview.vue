<script lang="ts">
import { defineComponent, onMounted, Ref, ref } from "vue";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";
import {
  RightErrorRest,
  RightRest,
  TemplateApplicationsRest,
} from "@/generated-sources/openapi";
import { useDialogsStore } from "@/stores/dialogs";
import RightsEditDialog from "@/components/RightsEditDialog.vue";

export default defineComponent({
  components: { RightsEditDialog },
  props: {},
  emits: ["getItemsByTemplateId"],
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
        text: "Template Name",
        align: "start",
        value: "templateName",
        sortable: true,
      },
      {
        text: "Items verknüpfen",
        value: "displayConnectedItems",
        sortable: true,
      },
      {
        text: "Template anwenden",
        value: "applyTemplate",
        sortable: true,
      },
      { text: "Actions", value: "actions", sortable: false },
    ];
    const templateItems: Ref<Array<RightRest>> = ref([]);

    /**
     * Error messages.
     */
    const templateLoadError = ref(false);
    const templateLoadErrorMsg = ref("");
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
            templateLoadErrorMsg.value = errMsg;
            templateLoadError.value = true;
          });
        });
    };

    const activateTemplateEditDialog = () => {
      alertSuccessful.value = false;
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
      if (template.templateId == undefined) {
        return;
      }
      templateApi
        .applyTemplates([template.templateId])
        .then((r: TemplateApplicationsRest) => {
          alertSuccessful.value = true;
          alertSuccessfulMsg.value =
            "Template '" +
            template.templateName +
            "' wurde für " +
            r.templateApplication[0].numberOfAppliedEntries +
            " Einträge angewandt.";
          templateApplyItemsApplied.value = r.templateApplication.length;

          // Check for errors
          const errors: Array<RightErrorRest> = r.templateApplication.flatMap(
            (t) => (t.errors != undefined ? t.errors : []),
          );
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
            templateLoadErrorMsg.value = errMsg;
            templateLoadError.value = true;
          });
        });
    };

    const closeApplyErrorMsg = () => {
      templateApplyError.value = false;
    };

    /**
     * Alerts:
     */
    const alertSuccessful = ref(false);
    const alertSuccessfulMsg = ref("");

    /**
     * Child events:
     */
    const lastModifiedTemplateName = ref("");
    const childTemplateAdded = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      alertSuccessful.value = true;
      alertSuccessfulMsg.value =
        "Template " +
        lastModifiedTemplateName.value +
        " erfolgreich hinzugefügt.";
      updateTemplateOverview();
    };

    const childTemplateDeleted = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      alertSuccessful.value = true;
      alertSuccessfulMsg.value =
        "Template " + lastModifiedTemplateName.value + " erfolgreich gelöscht.";
      updateTemplateOverview();
    };

    const childTemplateUpdated = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      alertSuccessful.value = true;
      alertSuccessfulMsg.value =
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
    const emitGetItemsByTemplateId = (templateId?: string) => {
      if (templateId != undefined) {
        emit("getItemsByTemplateId", templateId);
      }
    };

    onMounted(() => getTemplateList());

    return {
      alertSuccessful,
      alertSuccessfulMsg,
      currentTemplate,
      dialogStore,
      headers,
      lastModifiedTemplateName,
      isNew,
      reinitCounter,
      renderKey,
      templateApplyError,
      templateApplyErrorMsg,
      templateApplyErrorNumber,
      templateApplyItemsApplied,
      templateLoadError,
      templateLoadErrorMsg,
      templateItems,
      applyTemplate,
      childTemplateAdded,
      childTemplateDeleted,
      childTemplateUpdated,
      closeApplyErrorMsg,
      closeTemplateEditDialog,
      createNewTemplate,
      editTemplate,
      emitGetItemsByTemplateId,
      getTemplateList,
      updateTemplateOverview,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card>
    <v-container>
      <v-alert v-model="alertSuccessful" closable type="success">
        {{ alertSuccessfulMsg }}
      </v-alert>
      <v-alert v-model="templateLoadError" closable type="error">
        {{ templateLoadErrorMsg }}
      </v-alert>
      <v-dialog v-model="templateApplyError" max-width="1000">
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
      <v-data-table
        :key="renderKey"
        :headers="headers"
        :items="templateItems"
        item-value="templateName"
        loading-text="Daten werden geladen... Bitte warten."
      >
        <template v-slot:item.displayConnectedItems="{ item }">
          <v-btn
            color="blue darken-1"
            @click="emitGetItemsByTemplateId(item.rightId)"
            >Alle verknüpften Items anzeigen
          </v-btn>
        </template>
        <template v-slot:item.applyTemplate="{ item }">
          <v-btn color="blue darken-1" @click="applyTemplate(item)"
            >Template anwenden</v-btn
          >
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
