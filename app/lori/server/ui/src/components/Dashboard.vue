<script lang="ts">


import {computed, defineComponent, onMounted, Ref, ref, watch} from "vue";
import {BookmarkRest, RightErrorInformationRest, RightErrorRest, RightRest} from "@/generated-sources/openapi";
import error from "@/utils/error";
import rightErrorApi from "@/api/rightErrorApi";
import searchquerybuilder from "@/utils/searchquerybuilder";
import date_utils from "@/utils/date_utils";

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
        value: "handle",
        sortable: true,
      },
      {
        title: "Art",
        align: "start",
        value: "conflictType",
        sortable: true,
      },
      {
        title: "Kontext",
        value: "conflictByContext",
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
              pageSize.value,
              buildTemplateNameFilter(),
              startDateFormatted.value != "" ? startDateFormatted.value : undefined,
              endDateFormatted.value != "" ? endDateFormatted.value : undefined,
              buildConflictTypeFilter(),
          )
          .then((r: RightErrorInformationRest) => {
            totalPages.value = r.totalPages;
            errorItems.value = r.errors;
            numberOfResults.value = r.numberOfResults;
            receivedContextNames.value = r.contextNames;
            receivedConflictTypes.value = r.conflictTypes;
            isResetting.value = false;
          })
          .catch((e) => {
            isResetting.value = false;
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

    /**
     * Filter:
     */
    const receivedContextNames: Ref<Array<string>> = ref([]);
    const selectedContextNames: Ref<Array<string>> = ref([]);
    const receivedConflictTypes: Ref<Array<string>> = ref([]);
    const selectedConflictTypes: Ref<Array<string>> = ref([]);

    const buildTemplateNameFilter: () => (string | undefined) = () => {
      if (selectedContextNames.value.length == 0){
        return undefined;
      } else {
        return selectedContextNames.value.join(",");
      }
    };

    const buildConflictTypeFilter: () => (string | undefined) = () => {
      if (selectedConflictTypes.value.length == 0){
        return undefined;
      } else {
        return selectedConflictTypes.value.join(",");
      }
    };

    const prettyPrintConflict: (conflictType: any) => string = (conflictType: string) => {
      switch (conflictType){
        case "date_overlap":
          return "Zeitlicher Widerspruch";
        case "gap":
          return "Zeitliche Lücke";
        default:
          return "Unbekannt";
      }
    };

    const startDate = ref(undefined as Date | undefined);
    const isStartDateMenuOpen = ref(false);
    const startDateFormatted = ref("");
    const startDateEntered = () => {
      if (
          date_utils.isEmptyObject(startDate.value) ||
          startDate.value == undefined
      ) {
        return "";
      } else {
        startDateFormatted.value = date_utils.dateToIso8601(startDate.value);
      }
    };

    const endDate = ref(undefined as Date | undefined);
    const isEndDateMenuOpen = ref(false);
    const endDateFormatted = ref("");
    const endDateEntered = () => {
      if (
          date_utils.isEmptyObject(endDate.value) ||
          endDate.value == undefined
      ) {
        return "";
      } else {
        endDateFormatted.value = date_utils.dateToIso8601(endDate.value);
      }
    };

    /**
     * Reset Button:
     */
    const isResetting = ref(false);
    const resetFilter = () => {
      isResetting.value = true;
      startDateFormatted.value = "";
      endDateFormatted.value = "";
      selectedConflictTypes.value = [];
      selectedContextNames.value = [];
      getErrorList();
    };
    const canReset = computed(() => {
      return (
          selectedContextNames.value.length > 0 ||
              selectedConflictTypes.value.length > 0 ||
              startDateFormatted.value != "" ||
              endDateFormatted.value != ""
      );
    });

    /**
     * Pageinator:
     */
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

    const createRightHref = (handleId : string, rightId: string | undefined) => {
      const handlePP = createHandleHref(handleId);
      return handlePP + "&" +
          searchquerybuilder.QUERY_PARAMETER_RIGHT_ID + "=" + rightId;
    };

    const createTemplateHref: (rightId: string | undefined) => string = (rightId : string | undefined) => {
      if(rightId == undefined){
        return "";
      } else {
        return window.location.origin + window.location.pathname + "?" +
            searchquerybuilder.QUERY_PARAMETER_TEMPLATE_ID + "=" + rightId;
      }
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

    watch(selectedContextNames, () => {
      if(isResetting.value){
        return;
      } else {
        getErrorList();
      }
    });

    watch(selectedConflictTypes, () => {
      if(isResetting.value) {
        return;
      } else {
        getErrorList();
      }
    });

    watch(startDateFormatted, () => {
      if(isResetting.value){
        return;
      } else {
        isStartDateMenuOpen.value = false;
        getErrorList();
      }
    });

    watch(endDateFormatted, () => {
      if(isResetting.value){
        return;
      } else {
        isEndDateMenuOpen.value = false;
        getErrorList();
      }
    });

    /**
     * onMounted:
     */
    onMounted(() => getErrorList());

    return {
      canReset,
      currentPage,
      headers,
      endDate,
      endDateFormatted,
      errorItems,
      errorMsg,
      errorMsgIsActive,
      isEndDateMenuOpen,
      isStartDateMenuOpen,
      numberOfResults,
      pageSize,
      pageSizes,
      receivedConflictTypes,
      receivedContextNames,
      renderKey,
      searchTerm,
      selectedConflictTypes,
      selectedContextNames,
      startDate,
      startDateFormatted,
      successMsg,
      successMsgIsActive,
      totalPages,
      createHandleHref,
      createRightHref,
      createTemplateHref,
      endDateEntered,
      getErrorList,
      prettyPrintConflict,
      resetFilter,
      startDateEntered,
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
      <v-row>
        <v-col>
          <b>Art</b>
          <v-select
              multiple
              v-model="selectedConflictTypes"
              :items="receivedConflictTypes"
          >
            <template v-slot:selection="{ item }">
              {{ prettyPrintConflict(item.title) }}
            </template>
            <template v-slot:item="{ item, props }">
              <v-list-item v-bind="props">
                <template v-slot:title>
                  <v-icon
                      v-if="selectedConflictTypes.includes(item.value)"
                      color="primary"
                      class="mr-3">
                    mdi-checkbox-marked
                  </v-icon>
                  <v-icon v-else class="mr-3">
                    mdi-checkbox-blank-outline
                  </v-icon>
                  {{ prettyPrintConflict(item.title) }}
                </template>
              </v-list-item>
            </template>
          </v-select>
          <v-btn
              size="small"
              color="warning"
              :disabled="!canReset"
              @click="resetFilter"
          >
            Filter resetten</v-btn
          >
        </v-col>
        <v-col>
          <b>Kontext</b>
          <v-select
              multiple
              v-model="selectedContextNames"
              :items="receivedContextNames"
          ></v-select>
        </v-col>
        <v-col>
          <b>Erzeugungszeitraum</b>
          <v-menu
              :close-on-content-click="false"
              :location="'bottom'"
              v-model="isStartDateMenuOpen"
          >
            <template v-slot:activator="{ props }">
              <v-text-field
                  v-model="startDateFormatted"
                  label="Von"
                  variant="outlined"
                  prepend-icon="mdi-calendar"
                  readonly
                  clearable
                  v-bind="props"
                  @update:modelValue="getErrorList"
              ></v-text-field>
            </template>
            <v-date-picker
                v-model="startDate"
                color="primary"
                @update:modelValue="startDateEntered"
            >
              <template v-slot:header></template>
            </v-date-picker>
          </v-menu>
        </v-col>
        <v-col class="ma-6">
          <v-menu
              :close-on-content-click="false"
              :location="'bottom'"
              v-model="isEndDateMenuOpen"
          >
            <template v-slot:activator="{ props }">
              <v-text-field
                  v-model="endDateFormatted"
                  label="Bis"
                  variant="outlined"
                  prepend-icon="mdi-calendar"
                  readonly
                  clearable
                  v-bind="props"
                  @update:modelValue="getErrorList"
              ></v-text-field>
            </template>
            <v-date-picker
                v-model="endDate"
                color="primary"
                @update:modelValue="endDateEntered"
            >
              <template v-slot:header></template>
            </v-date-picker>
          </v-menu>
        </v-col>
      </v-row>
      <v-row>
        <v-col>
          Meldungen: {{ numberOfResults }}
        </v-col>
      </v-row>
      <v-data-table
          :key="renderKey"
          :headers="headers"
          :items="errorItems"
          :search="searchTerm"
          :items-per-page="0"
          item-value="handleId"
          loading-text="Daten werden geladen... Bitte warten."
      >
      <template v-slot:item.conflictByContext="{ item }">
        <td v-if="item.conflictType == 'date_overlap'">
          <a
              v-bind:href="
                  createTemplateHref(item.conflictByRightId)
                  "
              target="_blank"
          > Template '{{item.conflictByContext}}'</a>
        </td>
        <td v-else>
          {{ item.conflictByContext }}
        </td>
      </template>
      <template v-slot:item.conflictType="{ item }">
        <td >
          {{prettyPrintConflict(item.conflictType)}}
        </td>
      </template>
        <template v-slot:item.handle="{ item }">
          <td>
            <a
                v-bind:href="
                  createHandleHref(item.handle)
                  "
                target="_blank"
            > {{ item.handle}}</a>
          </td>
        </template>
        <template v-slot:item.conflictingWithRightId="{ item }">
          <td v-if="item.conflictingWithRightId != undefined">
            <a
                v-bind:href="
                  createRightHref(item.handle, item.conflictingWithRightId)
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

