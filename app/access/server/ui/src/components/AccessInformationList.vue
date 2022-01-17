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
          label="Select Item"
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
          @click:row="setActiveAccessInformation"
        >
          <template v-slot:item.actions="{ item }">
            <v-icon
              small
              class="mr-2"
              @click="setActiveAccessInformation(item, 0)"
            >
              mdi-pencil
            </v-icon>
          </template>
        </v-data-table>
      </v-card>
      <v-col>
        <v-card v-if="currentAccInf.id" class="mx-auto" tile>
          <v-card-title class="subheading font-weight-bold">
            Eintrag
          </v-card-title>
          <v-divider></v-divider>
          <v-list dense>
            <v-list-item>
              <v-list-item-content>Id:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.id }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Titel:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.title) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Band:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.band) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Zugriffstatus:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.accessState) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Publikationstyp:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.publicationType }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Publikationsjahr:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.publicationYear }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>DOI:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.doi) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Handle</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.handle }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>ISBN</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.isbn) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>ISSN</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.issn) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Paket-Sigel</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.paketSigel) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>PPN</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.ppn) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>PPN-Ebook</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.ppnEbook) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Zugriffsrecht K10Plus</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.rightsK10plus) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Seriennummer</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.serialNumber) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Titel Journal</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.titleJournal) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Titel Serie</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.titleSeries) }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>ZBD-ID</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ prettyPrint(currentAccInf.zbdId) }}
              </v-list-item-content>
            </v-list-item>
          </v-list>
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

@Component({
  components: { AccessEdit },
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
  private search = "";
  private selectedHeaders: Array<DataTableHeader> = [];
  private headersValueVSelect = [];

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
      value: "pubtype",
    },
    {
      text: "Actions",
      sortable: true,
      value: "actions",
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
      .then((response) => (this.items = response))
      .catch((e) => {
        console.log(e);
      });
  }

  public setActiveAccessInformation(
    accessInformation: ItemRest,
    itemSlotData: any,
  ): void {
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
