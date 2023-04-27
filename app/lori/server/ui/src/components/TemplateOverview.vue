<script lang="ts">
import { defineComponent, onMounted, Ref, ref } from "vue";
import error from "@/utils/error";
import templateApi from "@/api/templateApi";
import { TemplateRest } from "@/generated-sources/openapi";
import { useDialogsStore } from "@/stores/dialogs";
import RightsEditDialog from "@/components/RightsEditDialog.vue";

export default defineComponent({
  components: { RightsEditDialog },
  setup() {
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
      },
    ];
    const templateItems: Ref<Array<TemplateRest>> = ref([]);
    /**
     * Error messages.
     */
    const templateLoadError = ref(false);
    const templateLoadErrorMsg = ref("");

    /**
     * Template properties.
     */
    const isNew = ref(true);
    const currentTemplate = ref({} as TemplateRest);
    const getTemplateList = () => {
      templateApi
        .getTemplateList(0, 100)
        .then((r: Array<TemplateRest>) => {
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
      alertSuccessfulAdd.value = false;
      alertSuccessfulUpdate.value = false;
      dialogStore.templateEditActivated = true;
    };
    const createNewTemplate = () => {
      isNew.value = true;
      activateTemplateEditDialog();
    };

    const closeTemplateEditDialog = () => {
      dialogStore.templateEditActivated = false;
    };

    const editTemplate = (template: TemplateRest, row: any) => {
      isNew.value = false;
      currentTemplate.value = template;
      activateTemplateEditDialog();
    };

    /**
     * Alerts:
     */
    const alertSuccessfulAdd = ref(false);
    const alertSuccessfulUpdate = ref(false);

    /**
     * Child events:
     */
    const lastModifiedTemplateName = ref("");
    const childTemplateAdded = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      alertSuccessfulAdd.value = true;
      updateTemplateOverview();
    };

    const childTemplateUpdated = (templateName: string) => {
      lastModifiedTemplateName.value = templateName;
      alertSuccessfulUpdate.value = true;
      updateTemplateOverview();
    };

    const updateTemplateOverview = () => {
      getTemplateList();
      renderKey.value += 1;
    };

    onMounted(() => getTemplateList());

    return {
      alertSuccessfulAdd,
      alertSuccessfulUpdate,
      currentTemplate,
      dialogStore,
      headers,
      lastModifiedTemplateName,
      isNew,
      renderKey,
      templateLoadError,
      templateLoadErrorMsg,
      templateItems,
      childTemplateAdded,
      childTemplateUpdated,
      closeTemplateEditDialog,
      createNewTemplate,
      editTemplate,
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
      <v-alert v-model="alertSuccessfulAdd" dismissible text type="success">
        Template {{ lastModifiedTemplateName }} erfolgreich hinzugefügt.
      </v-alert>

      <v-alert v-model="templateLoadError" dismissible text type="error">
        {{ templateLoadErrorMsg }}
      </v-alert>
      <v-alert v-model="alertSuccessfulUpdate" dismissible text type="success">
        Template {{ lastModifiedTemplateName }} erfolgreich geupdated.
      </v-alert>
      <v-card-title>Template Übersicht</v-card-title>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text @click="createNewTemplate"
          >Neues Template anlegen
        </v-btn>
      </v-card-actions>
      <v-data-table
        :key="renderKey"
        :headers="headers"
        :items="templateItems"
        item-key="templateName"
        loading-text="Daten werden geladen... Bitte warten."
        @click:row="editTemplate"
      ></v-data-table>
      <v-dialog
        v-model="dialogStore.templateEditActivated"
        :retain-focus="false"
        max-width="1000px"
        v-on:close="closeTemplateEditDialog"
      >
        <RightsEditDialog
          :index="-1"
          :isNew="isNew"
          :isTemplate="true"
          :right="currentTemplate.right"
          :template-description="currentTemplate.description"
          :template-id="currentTemplate.templateId"
          :template-name="currentTemplate.templateName"
          metadataId=""
          v-on:addTemplateSuccessful="childTemplateAdded"
          v-on:editRightClosed="closeTemplateEditDialog"
          v-on:updateTemplateSuccessful="childTemplateUpdated"
        ></RightsEditDialog>
      </v-dialog>
    </v-container>
  </v-card>
</template>
