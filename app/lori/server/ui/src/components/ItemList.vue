<script lang="ts">
import {
  ItemRest,
  MetadataRest,
  MetadataRestPublicationTypeEnum,
} from "@/generated-sources/openapi";
import api from "@/api/api";
import { DataTableHeader } from "vuetify";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";

export default defineComponent({
  components: { RightsView, MetadataView },

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
        text: "PPN-Ebook",
        value: "ppnEbook",
      },
      {
        text: "Rechte-K10Plus",
        value: "rightsK10plus",
      },
      {
        text: "Seriennummer",
        value: "serialNumber",
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
        text: "ZBD-Id",
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
      api
        .searchQuery(
          searchTerm.value,
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

        <v-col cols="5" sm="5"> Suchergebnisse: {{ numberOfResults }} </v-col>
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
    </v-row>
  </v-container>
</template>
