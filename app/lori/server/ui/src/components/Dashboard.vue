<script lang="ts">


import {defineComponent, onMounted, Ref, ref, watch} from "vue";
import {RightErrorInformationRest, RightErrorRest, RightRest} from "@/generated-sources/openapi";
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
          .getRightErrorList(
              (currentPage.value - 1) * pageSize.value,
              pageSize.value
          )
          .then((r: RightErrorInformationRest) => {
            totalPages.value = r.totalPages;
            errorItems.value = r.errors;
            numberOfResults.value = r.numberOfResults;
          })
          .catch((e) => {
            error.errorHandling(e, (errMsg: string) => {
              errorMsg.value = errMsg;
              errorMsgIsActive.value = true;
            });
          });
    };
    const searchTerm = ref("");
    const currentPage = ref(1);
    const pageSize = ref(10);
    const pageSizes = ref<Array<number>>([5, 10, 25, 50]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);


    const handlePageChange = () => {
      getErrorList();
    };

    const handlePageSizeChange = () => {
      currentPage.value = 1;
      getErrorList();
    };

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

    /**
     * Watches:
     */
    watch(currentPage, () => {
      handlePageChange();
    });

    watch(pageSize, () => {
      handlePageSizeChange();
    });

    onMounted(() => getErrorList());

    return {
      currentPage,
      headers,
      errorItems,
      errorMsg,
      errorMsgIsActive,
      numberOfResults,
      pageSize,
      pageSizes,
      renderKey,
      searchTerm,
      successMsg,
      successMsgIsActive,
      totalPages,
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
      Meldungen: {{ numberOfResults }}
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
          :items-per-page="0"
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
        <template #bottom></template>
      </v-data-table>
    </v-container>
    <v-col cols="14" sm="12">
      <v-row>
        <v-col cols="2" sm="2">
          <v-select
              v-model="pageSize"
              :items="pageSizes"
              label="Einträge pro Seite"
          ></v-select>
        </v-col>
        <v-col cols="10" sm="9">
          <v-pagination
              v-model="currentPage"
              :length="totalPages"
              next-icon="mdi-menu-right"
              prev-icon="mdi-menu-left"
              total-visible="7"
          ></v-pagination>
        </v-col>
      </v-row>
    </v-col>
  </v-card>
</template>

