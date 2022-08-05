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

    const retrieveAccessInformation = () => {
      api
        .getList(0, 25)
        .then((response) => {
          items.value = response;
          tableContentLoading.value = false;
        })
        .catch((e) => {
          console.log(e);
          tableContentLoading.value = false;
          loadAlertErrorMessage.value =
            e.statusText + " (Statuscode: " + e.status + ")";
          loadAlertError.value = true;
        });
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

    onMounted(() => retrieveAccessInformation());

    watch(headersValueVSelect, (currentValue, oldValue) => {
      selectedHeaders.value = currentValue;
    });

    return {
      currentItem,
      headers,
      headersValueVSelect,
      items,
      loadAlertError,
      loadAlertErrorMessage,
      search,
      selectedHeaders,
      // Methods
      getAlertLoad,
      retrieveAccessInformation,
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
