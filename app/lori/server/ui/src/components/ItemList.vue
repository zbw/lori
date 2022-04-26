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
          show-select
          @click:row="setActiveItem"
          loading="tableContentLoading"
          loading-text="Daten werden geladen... Bitte warten."
        >
        </v-data-table>
        <v-alert v-model="loadAlertError" dismissible text type="error">
          Laden der bibliographischen Daten war nicht erfolgreich:
          {{ loadAlertErrorMessage }}
        </v-alert>
      </v-card>
      <v-col>
        <v-card v-if="currentItem.metadata" class="mx-auto" tile>
          <RightsView :rights="currentItem.rights"></RightsView>
          <MetadataView
            :displayed-item="Object.assign({}, currentItem.metadata)"
          ></MetadataView>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Vue, Watch } from "vue-property-decorator";
import { ItemRest, MetadataRest } from "@/generated-sources/openapi";
import api from "@/api/api";
import Component from "vue-class-component";
import { DataTableHeader } from "vuetify";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";

@Component({
  components: { RightsView, MetadataView },
})
export default class AccessInformationList extends Vue {
  private items: Array<ItemRest> = [];
  private currentItem = {} as ItemRest;
  private currentIndex = -1;
  private dialogEdit = false;
  private headersValueVSelect = [];
  private loadAlertError = false;
  private loadAlertErrorMessage = "";
  private search = "";
  private selectedHeaders: Array<DataTableHeader> = [];
  private tableContentLoading = true;

  private headers = [
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

  public retrieveAccessInformation(): void {
    api
      .getList(0, 25)
      .then((response) => {
        this.items = response;
        this.tableContentLoading = false;
      })
      .catch((e) => {
        console.log(e);
        this.tableContentLoading = false;
        this.loadAlertErrorMessage =
          e.statusText + " (Statuscode: " + e.status + ")";
        this.loadAlertError = true;
      });
  }

  public setActiveItem(metadata: MetadataRest): void {
    let item: ItemRest | undefined = this.items.find(
      (e) => e.metadata.metadataId === metadata.metadataId
    );
    if (item !== undefined) {
      this.currentItem = item;
    }
  }

  public searchTitle(): void {
    this.currentIndex = -1;
  }

  public closeEditItemDialog(): void {
    this.dialogEdit = false;
  }

  public getAlertLoad(): boolean {
    return this.loadAlertError;
  }

  mounted(): void {
    this.retrieveAccessInformation();
  }

  created(): void {
    this.selectedHeaders = this.headers.slice(0, 6);
  }

  @Watch("headersValueVSelect")
  onValueChanged(val: Array<DataTableHeader>): void {
    this.selectedHeaders = val;
  }
}
</script>
