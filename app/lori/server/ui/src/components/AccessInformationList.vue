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
          :items="items"
          :search="search"
          show-select
          @click:row="setActiveAccessInformation"
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
        <v-card v-if="currentAccInf.id" class="mx-auto" tile>
          <MetadataView
            :displayed-item="Object.assign({}, currentAccInf)"
          ></MetadataView>
          <RightsView
            :actions="Object.assign({}, currentAccInf.actions)"
            :access-state="currentAccInf.accessState"
            :lisence-conditions="currentAccInf.licenseConditions"
            :provenance-license="currentAccInf.provenanceLicense"
          ></RightsView>
          <v-dialog v-model="dialogDelete" max-width="500px">
            <v-card>
              <v-card-title class="text-h5">
                Soll dieser Eintrag gelöscht werden?
              </v-card-title>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                  :disabled="deleteConfirmationLoading"
                  color="blue darken-1"
                  @click="cancelDeleteDialog"
                  >Abbrechen
                </v-btn>
                <v-btn
                  :loading="deleteConfirmationLoading"
                  color="error"
                  @click="approveDeleteDialog"
                >
                  Löschen
                </v-btn>
                <v-spacer></v-spacer>
              </v-card-actions>
            </v-card>
          </v-dialog>
          <v-alert
            v-model="deleteAlertSuccessful"
            dismissible
            text
            type="success"
          >
            Löschen war erfolgreich.
          </v-alert>
          <v-alert v-model="deleteAlertError" dismissible text type="error">
            Löschen war nicht erfolgreich: {{ deleteErrorMessage }}
          </v-alert>
          <v-dialog v-model="dialogEdit" max-width="500px">
            <template v-slot:activator="{ on, attrs }">
              <v-btn
                v-bind="attrs"
                v-on="on"
                class="ma-2"
                color="success"
                outlined
                tile
              >
                <v-icon left>mdi-pencil</v-icon>
                Bearbeiten
              </v-btn>
            </template>
            <AccessEdit
              :edit-item="Object.assign({}, currentAccInf)"
              v-on:closeDialog="closeEditItemDialog"
            />
          </v-dialog>
          <v-btn color="error" @click="openDeleteItemDialog()">
            <v-icon left>mdi-delete</v-icon>
            Löschen
          </v-btn>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import { Vue, Watch } from "vue-property-decorator";
import { ItemRest } from "@/generated-sources/openapi";
import api from "@/api/api";
import { Result } from "neverthrow";
import AccessEdit from "./AccessEdit.vue";
import Component from "vue-class-component";
import { DataTableHeader } from "vuetify";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";

@Component({
  components: { RightsView, MetadataView, AccessEdit },
})
export default class AccessInformationList extends Vue {
  private items: Array<ItemRest> = [];
  private currentAccInf = {} as ItemRest;
  private currentIndex = -1;
  private dialogEdit = false;
  private dialogDelete = false;
  private deleteConfirmationLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";
  private headersValueVSelect = [];
  private loadAlertError = false;
  private loadAlertErrorMessage = "";
  private search = "";
  private selectedHeaders: Array<DataTableHeader> = [];
  private tableContentLoading = true;

  private headers = [
    {
      text: "Id",
      align: "start",
      sortable: false,
      value: "id",
    },
    {
      text: "Title",
      sortable: true,
      value: "title",
    },
    {
      text: "Access State",
      value: "accessState",
    },
    {
      text: "Handle",
      sortable: true,
      value: "handle",
    },
    {
      text: "Publication Type",
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
      text: "Lizensbedingungen",
      value: "licenseConditions",
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
      text: "Provienz Lizensinformationen",
      value: "provenanceLicense",
    },
    {
      text: "Rechte-K10Plus",
      value: "rightsK10plus",
    },
    {
      text: "Serial Number",
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

  public setActiveAccessInformation(accessInformation: ItemRest): void {
    this.currentAccInf = accessInformation;
  }

  public searchTitle(): void {
    this.currentIndex = -1;
  }

  public closeEditItemDialog(): void {
    this.dialogEdit = false;
  }

  public openDeleteItemDialog(): void {
    this.dialogDelete = true;
  }

  public approveDeleteDialog(): void {
    this.deleteConfirmationLoading = true;
    api
      .deleteAccessInformation(this.currentAccInf.id)
      .then((response: Result<void, Error>) => {
        if (response.isOk()) {
          this.deleteAlertSuccessful = true;
          this.retrieveAccessInformation();
        } else {
          this.deleteAlertError = true;
          this.deleteErrorMessage = response.error.message;
        }
        this.dialogDelete = false;
        this.deleteConfirmationLoading = false;
      });
  }

  public cancelDeleteDialog(): void {
    this.dialogDelete = false;
  }

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
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

<style scoped>
.list {
  text-align: left;
  max-width: 750px;
  margin: auto;
}
</style>
