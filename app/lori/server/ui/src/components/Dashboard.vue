<script lang="ts">


import {defineComponent, onMounted, Ref, ref} from "vue";
import {RightErrorRest, RightRest} from "@/generated-sources/openapi";
import error from "@/utils/error";
import rightErrorApi from "@/api/rightErrorApi";
import searchquerybuilder from "@/utils/searchquerybuilder";

export default defineComponent({
  components: {},
  props: {},
  emits: [],
  setup(props, {emit}) {
    /**
     * Table:
     */
    const renderKey = ref(0);
    const errorItems: Ref<Array<RightErrorRest>> = ref([]);
    const headers = [
      {
        title: "Handle",
        align: "start",
        value: "handleId",
        sortable: true,
      },
      {
        title: "Art",
        align: "start",
        value: "conflictType",
        sortable: true,
      },
      {
        title: "Erzeugt durch",
        value: "conflictByTemplateName",
        sortable: true,
      },
      {
        title: "Anlass",
        value: "conflictingWithRightId",
        sortable: true,
      },
      {
        title: "Erzeugt am",
        value: "createdOn",
        sortable: true
      },
    ];

    const getErrorList = () => {
      rightErrorApi
          .getRightErrorList(0, 100)
          .then((r: Array<RightErrorRest>) => {
            errorItems.value = r;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };
    const searchTerm = ref("");

    const createHandleHref = (handleId : string) => {
      return window.location.origin + window.location.pathname + "?" +
          searchquerybuilder.QUERY_PARAMETER_DASHBOARD_HANDLE_SEARCH + "=hdl:" + handleId;
    };

    const createRightHref = (handleId : string, rightId: string) => {
      const handlePP = createHandleHref(handleId);
      return handlePP + "&" +
          searchquerybuilder.QUERY_PARAMETER_RIGHT_ID + "=" + rightId;
    };

    const createTemplateHref = (rightId : string) => {
      return window.location.origin + window.location.pathname + "?" +
          searchquerybuilder.QUERY_PARAMETER_TEMPLATE_ID + "=" + rightId;
    };
    /**
     * Alerts:
     */
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");
    const successMsgIsActive = ref(false);
    const successMsg = ref("");

    onMounted(() => getErrorList());

    return {
      headers,
      errorItems,
      errorMsg,
      errorMsgIsActive,
      renderKey,
      searchTerm,
      successMsg,
      successMsgIsActive,
      createHandleHref,
      createRightHref,
      createTemplateHref,
      getErrorList,
    };
  },
});
</script><style scoped>

</style>

<template>
  <v-card position="relative">
    <v-container>
      <v-card-title class="text-h5"
      >Konflikte Zeitliche Gültigkeit</v-card-title
      >
      Meldungen: {{ errorItems.length }}
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
      <v-data-table
          :key="renderKey"
          :headers="headers"
          :items="errorItems"
          :search="searchTerm"
          item-value="handleId"
          loading-text="Daten werden geladen... Bitte warten."
      >
      <template v-slot:item.conflictByTemplateName="{ item }">
        <td >
          <a
              v-bind:href="
                  createTemplateHref(item.conflictByRightId)
                  "
              target="_blank"
          > Template '{{item.conflictByTemplateName}}'</a>
        </td>
      </template>
      <template v-slot:item.conflictType="{ item }">
        <td >Widerspruch
        </td>
      </template>
        <template v-slot:item.handleId="{ item }">
          <td>
            <a
                v-bind:href="
                  createHandleHref(item.handleId)
                  "
                target="_blank"
            > {{ item.handleId}}</a>
          </td>
        </template>
        <template v-slot:item.conflictingWithRightId="{ item }">
          <td>
            <a
                v-bind:href="
                  createRightHref(item.handleId, item.conflictingWithRightId)
                  "
                target="_blank"
            > Right-ID: {{ item.conflictingWithRightId }}</a>
          </td>
        </template>
      </v-data-table>
    </v-container>
  </v-card>
</template>

