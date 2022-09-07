<script lang="ts">
import { ItemRest, MetadataRest } from "@/generated-sources/openapi";
import api from "@/api/api";
import { DataTableHeader } from "vuetify";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";

export default defineComponent({
  components: { RightsView, MetadataView },

  setup() {
    const items: Ref<Array<ItemRest>> = ref([]);
    const currentItem = ref({} as ItemRest);
    const headersValueVSelect = ref([]);
    const loadAlertError = ref(false);
    const loadAlertErrorMessage = ref("");
    const search = ref("");
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
        value: "constationType",
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
        })
        .catch((e) => {
          tableContentLoading.value = false;
          loadAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          loadAlertError.value = true;
        });
    };

    const handlePageChange = (nextPage: number) => {
      currentPage.value = nextPage;
      retrieveItemInformation();
    };

    const handlePageSizeChange = (size: number) => {
      pageSize.value = size;
      currentPage.value = 1;
      retrieveItemInformation();
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

    return {
      currentItem,
      currentPage,
      headers,
      headersValueVSelect,
      items,
      loadAlertError,
      loadAlertErrorMessage,
      pageSize,
      pageSizes,
      search,
      selectedHeaders,
      tableContentLoading,
      totalPages,
      // Methods
      getAlertLoad,
      handlePageChange,
      handlePageSizeChange,
      retrieveAccessInformation: retrieveItemInformation,
      setActiveItem,
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
            v-model="search"
            append-icon="mdi-magnify"
            label="Suche"
            single-line
            hide-details
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
        <v-data-table
          disable-pagination
          :hide-default-footer="true"
          :headers="selectedHeaders"
          :items="items.map((value) => value.metadata)"
          :search="search"
          @click:row="setActiveItem"
          loading="tableContentLoading"
          loading-text="Daten werden geladen... Bitte warten."
          show-select
          item-key="metadataId"
        >
        </v-data-table>
        <v-col cols="12" sm="12">
          <v-row>
            <v-col cols="4" sm="3">
              <v-select
                v-model="pageSize"
                :items="pageSizes"
                label="Einträge pro Seite"
                @change="handlePageSizeChange"
              ></v-select>
            </v-col>
            <v-col cols="12" sm="9">
              <v-pagination
                v-model="currentPage"
                total-visible="7"
                :length="totalPages"
                next-icon="mdi-menu-right"
                prev-icon="mdi-menu-left"
                @input="handlePageChange"
              ></v-pagination> </v-col
          ></v-row>
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
