<script lang="ts">
import { ItemRest, MetadataRest } from "@/generated-sources/openapi";
import api from "@/api/api";
import { DataTableHeader } from "vuetify";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import SearchFilter from "@/components/SearchFilter.vue";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";

export default defineComponent({
  components: { RightsView, MetadataView, SearchFilter },

  setup() {
    const items: Ref<Array<ItemRest>> = ref([]);
    const currentItem = ref({} as ItemRest);
    const headersValueVSelect = ref([]);
    const loadAlertError = ref(false);
    const loadAlertErrorMessage = ref("");
    const searchTerm = ref("");
    const tableContentLoading = ref(true);

    const headers = [
      {
        text: "Item-Id",
        align: "start",
        sortable: false,
        value: "metadataId",
      },
      {
        text: "Titel",
        sortable: true,
        value: "title",
        width: "300px",
      },
      {
        text: "Handle",
        sortable: true,
        value: "handle",
      },
      {
        text: "Publikationstyp",
        sortable: true,
        value: "publicationType",
      },
      {
        text: "Publikationsjahr",
        sortable: true,
        value: "publicationDate",
      },
      {
        text: "Band",
        value: "band",
      },
      {
        text: "DOI",
        value: "doi",
      },
      {
        text: "ISBN",
        value: "isbn",
      },
      {
        text: "ISSN",
        value: "issn",
      },
      {
        text: "Paket-Sigel",
        value: "paketSigel",
      },
      {
        text: "PPN",
        value: "ppn",
      },
      {
        text: "Rechte-K10Plus",
        value: "rightsK10plus",
      },
      {
        text: "Titel Journal",
        value: "titleJournal",
      },
      {
        text: "Titel Serie",
        value: "titleSeries",
      },
      {
        text: "ZDB-ID",
        value: "zbdId",
      },
    ] as Array<DataTableHeader>;

    const selectedHeaders = ref(headers.slice(0, 6));

    const currentPage = ref(1);
    const pageSize = ref(25); // Default page size is 25
    const pageSizes = ref<Array<number>>([5, 10, 25, 50]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);

    const retrieveItemInformation = () => {
      api
        .getList(
          (currentPage.value - 1) * pageSize.value,
          pageSize.value,
          pageSize.value
        )
        .then((response) => {
          items.value = response.itemArray;
          tableContentLoading.value = false;
          totalPages.value = response.totalPages;
          numberOfResults.value = response.numberOfResults;
        })
        .catch((e) => {
          tableContentLoading.value = false;
          loadAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          loadAlertError.value = true;
        });
    };

    // Page changes
    const handlePageChange = (nextPage: number) => {
      currentPage.value = nextPage;
      searchQuery();
    };

    const handlePageSizeChange = (size: number) => {
      pageSize.value = size;
      currentPage.value = 1;
      searchQuery();
    };

    const setActiveItem = (metadata: MetadataRest) => {
      let item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === metadata.metadataId
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
    };

    const getAlertLoad = () => {
      return loadAlertError;
    };

    onMounted(() => retrieveItemInformation());

    watch(headersValueVSelect, (currentValue, oldValue) => {
      selectedHeaders.value = currentValue;
    });

    // Search
    const searchStore = useSearchStore();

    const startSearch = () => {
      searchStore.lastSearchTerm = searchTerm.value;
      searchQuery();
    };

    const searchQuery = () => {
      let filterPublicationDate =
        searchStore.publicationDateFrom == "" &&
        searchStore.publicationDateTo == ""
          ? undefined
          : searchStore.publicationDateFrom +
            "-" +
            searchStore.publicationDateTo;
      let filterPublicationType = buildPublicationTypeFilter();
      let filterAccessStates = buildAccessState();
      let filterTempVal = buildTempVal();
      api
        .searchQuery(
          searchTerm.value,
          (currentPage.value - 1) * pageSize.value,
          pageSize.value,
          pageSize.value,
          filterPublicationDate,
          filterPublicationType,
          filterAccessStates,
          filterTempVal,
          buildStartDateAt(),
          buildEndDateAt(),
          buildFormalRule()
        )
        .then((response) => {
          items.value = response.itemArray;
          tableContentLoading.value = false;
          totalPages.value = response.totalPages;
          numberOfResults.value = response.numberOfResults;
        })
        .catch((e) => {
          tableContentLoading.value = false;
          loadAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          loadAlertError.value = true;
        });
    };

    const buildAccessState = () => {
      let accessStates: Array<string> = [];
      if (searchStore.accessStateOpen) {
        accessStates.push("OPEN");
      }
      if (searchStore.accessStateRestricted) {
        accessStates.push("RESTRICTED");
      }
      if (searchStore.accessStateClosed) {
        accessStates.push("CLOSED");
      }
      if (accessStates.length == 0) {
        return undefined;
      } else {
        return accessStates.join(",");
      }
    };

    const buildFormalRule = () => {
      let formalRule: Array<string> = [];
      if (searchStore.formalRuleLicenceContract) {
        formalRule.push("LICENCE_CONTRACT");
      }
      if (searchStore.formalRuleOpenContentLicence) {
        formalRule.push("OPEN_CONTENT_LICENCE");
      }
      if (searchStore.formalRuleUserAgreement) {
        formalRule.push("ZBW_USER_AGREEMENT");
      }
      if (formalRule.length == 0) {
        return undefined;
      } else {
        return formalRule.join(",");
      }
    };

    const buildTempVal = () => {
      let tempVal: Array<string> = [];
      if (searchStore.temporalValidityFilterFuture) {
        tempVal.push("FUTURE");
      }
      if (searchStore.temporalValidityFilterPast) {
        tempVal.push("PAST");
      }
      if (searchStore.temporalValidityFilterPresent) {
        tempVal.push("PRESENT");
      }
      if (tempVal.length == 0) {
        return undefined;
      } else {
        return tempVal.join(",");
      }
    };

    const buildStartDateAt = () => {
      if (
        searchStore.temporalEventStartDateFilter &&
        searchStore.temporalEventInput != ""
      ) {
        return searchStore.temporalEventInput;
      }
    };

    const buildEndDateAt = () => {
      if (
        searchStore.temporalEventEndDateFilter &&
        searchStore.temporalEventInput != ""
      ) {
        return searchStore.temporalEventInput;
      }
    };

    const buildPublicationTypeFilter = () => {
      let types: Array<string> = [];
      if (searchStore.publicationTypeArticle) {
        types.push("ARTICLE");
      }
      if (searchStore.publicationTypeBook) {
        types.push("BOOK");
      }
      if (searchStore.publicationTypeBookPart) {
        types.push("BOOK_PART");
      }
      if (searchStore.publicationTypeConferencePaper) {
        types.push("CONFERENCE_PAPER");
      }
      if (searchStore.publicationTypePeriodicalPart) {
        types.push("PERIODICAL_PART");
      }
      if (searchStore.publicationTypeProceedings) {
        types.push("PROCEEDINGS");
      }
      if (searchStore.publicationTypeResearchReport) {
        types.push("RESEARCH_REPORT");
      }
      if (searchStore.publicationTypeThesis) {
        types.push("THESIS");
      }
      if (searchStore.publicationTypeWorkingPaper) {
        types.push("WORKING_PAPER");
      }
      if (types.length == 0) {
        return undefined;
      } else {
        return types.join(",");
      }
    };

    // parse publication type
    const parsePublicationType = (pubType: string) => {
      switch (pubType) {
        case "article":
          return "Article";
        case "book":
          return "Book";
        case "bookPart":
          return "Book Part";
        case "conferencePaper":
          return "Conference Paper";
        case "periodicalPart":
          return "Periodical Part";
        case "proceedings":
          return "Proceedings";
        case "researchReport":
          return "Research Report";
        case "thesis":
          return "Thesis";
        case "workingPaper":
          return "Working Paper";
        default:
          return "Unknown pub type:" + pubType;
      }
    };

    return {
      currentItem,
      currentPage,
      headers,
      headersValueVSelect,
      items,
      loadAlertError,
      loadAlertErrorMessage,
      numberOfResults,
      pageSize,
      pageSizes,
      searchTerm,
      searchStore,
      selectedHeaders,
      tableContentLoading,
      totalPages,
      // Methods
      getAlertLoad,
      handlePageChange,
      handlePageSizeChange,
      retrieveItemInformation,
      parsePublicationType,
      searchQuery,
      setActiveItem,
      startSearch,
    };
  },
});
</script>

<template>
  <v-container>
    <v-row>
      <v-col cols="3">
        <SearchFilter></SearchFilter>
      </v-col>
      <v-col cols="9">
        <v-card>
          <v-card-title>
            <v-text-field
              v-model="searchTerm"
              append-icon="mdi-magnify"
              label="Suche"
              single-line
              hide-details
              @click:append="startSearch"
              @keydown.enter.prevent="startSearch"
            ></v-text-field>
          </v-card-title>
          <v-select
            v-model="headersValueVSelect"
            :items="headers"
            label="Spaltenauswahl"
            multiple
            return-object
          >
            <template v-slot:selection="{ item, index }">
              <v-chip v-if="index === 0">
                <span>{{ item.text }}</span>
              </v-chip>
              <span v-if="index === 1" class="grey--text caption"
                >(+{{ headersValueVSelect.length - 1 }} others)</span
              >
            </template>
          </v-select>

          <v-col cols="5" sm="5"> Suchergebnisse: {{ numberOfResults }}</v-col>
          <v-data-table
            disable-pagination
            :hide-default-footer="true"
            :headers="selectedHeaders"
            :items="items.map((value) => value.metadata)"
            @click:row="setActiveItem"
            loading="tableContentLoading"
            loading-text="Daten werden geladen... Bitte warten."
            show-select
            item-key="metadataId"
          >
            <template v-slot:item.handle="{ item }">
              <td>
                <a :href="item.handle">{{ item.handle.substring(22, 35) }}</a>
              </td>
            </template>
            <template v-slot:item.publicationType="{ item }">
              <td>{{ parsePublicationType(item.publicationType) }}</td>
            </template>
            <template v-slot:item.publicationDate="{ item }">
              <td>{{ item.publicationDate.toLocaleDateString("de") }}</td>
            </template>
          </v-data-table>
          <v-col cols="14" sm="12">
            <v-row>
              <v-col cols="2" sm="2">
                <v-select
                  v-model="pageSize"
                  :items="pageSizes"
                  label="Einträge pro Seite"
                  @change="handlePageSizeChange"
                ></v-select>
              </v-col>
              <v-col cols="10" sm="9">
                <v-pagination
                  v-model="currentPage"
                  total-visible="7"
                  :length="totalPages"
                  next-icon="mdi-menu-right"
                  prev-icon="mdi-menu-left"
                  @input="handlePageChange"
                ></v-pagination>
              </v-col>
            </v-row>
          </v-col>
          <v-alert v-model="loadAlertError" dismissible text type="error">
            Laden der bibliographischen Daten war nicht erfolgreich:
            {{ loadAlertErrorMessage }}
          </v-alert>
        </v-card>
        <v-col>
          <v-card v-if="currentItem.metadata" class="mx-auto" tile>
            <RightsView
              :rights="currentItem.rights"
              :metadataId="currentItem.metadata.metadataId"
            ></RightsView>
            <MetadataView
              :metadata="Object.assign({}, currentItem.metadata)"
            ></MetadataView>
          </v-card>
        </v-col>
      </v-col>
    </v-row>
  </v-container>
</template>
