<template>
  <v-container>
    <v-layout align-center row>
      <v-text-field label="Suche nach einem Titel"></v-text-field>
      <v-btn outlined x-large @click="searchTitle">
        <v-icon left>mdi-magnify</v-icon> Suche
      </v-btn>
    </v-layout>
    <v-row>
      <v-col>
        <v-card class="mx-auto" tile>
          <v-list rounded>
            <v-subheader>Items</v-subheader>
            <v-list-item-group color="primary">
              <v-list-item
                v-for="(item, index) in items"
                :key="index"
                @click="setActiveAccessInformation(item, index)"
              >
                {{ item.id }}
              </v-list-item>
            </v-list-item-group>
          </v-list>
        </v-card>
      </v-col>
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
                TODO
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>HandleUrl:</v-list-item-content>
              <v-list-item-content class="align-end">
                TODO
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Template:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.template }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Zuständige Einrichtung:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.tenant }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Richtlinien der Lizens:</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.usageGuide }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Access Status:</v-list-item-content>
              <v-list-item-content class="align-end">
                TODO
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Zuständige Einrichtung</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.tenant }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content>Kommerzielle Nutzung</v-list-item-content>
              <v-list-item-content class="align-end">
                {{ currentAccInf.commercialuse }}
              </v-list-item-content>
            </v-list-item>
            <v-list-item>
              <v-list-item-content
                >Urheberrechtsschutz vorhanden</v-list-item-content
              >
              <v-list-item-content class="align-end">
                {{ currentAccInf.copyright }}
              </v-list-item-content>
            </v-list-item>
          </v-list>
          <v-dialog v-model="dialogDelete" max-width="500px">
            <v-card>
              <v-card-title class="text-h5">
                Soll dieser Eintrag gelöscht werden?</v-card-title
              >
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                  :disabled="deleteConfirmationLoading"
                  color="blue darken-1"
                  @click="cancelDeleteDialog"
                  >Abbrechen</v-btn
                >
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
                <v-icon left>mdi-pencil</v-icon> Bearbeiten
              </v-btn>
            </template>
            <v-card>
              <v-card-title>
                <span class="text-h5">Editiere Eintrag</span>
              </v-card-title>
              <v-card-text>
                <v-container>
                  <v-row>
                    <v-col cols="12" md="6" sm="6">
                      <v-text-field
                        v-model="currentAccInf.id"
                        disabled
                        label="Id"
                      ></v-text-field>
                    </v-col>
                    <v-col cols="12" md="4" sm="6">
                      <v-text-field
                        v-model="currentAccInf.tenant"
                        label="Zustände Einrichtung"
                      ></v-text-field>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col cols="12" md="6" sm="6">
                      <v-checkbox
                        v-model="currentAccInf.commercialuse"
                        label="Kommerzielle Nutzung erlaubt"
                      ></v-checkbox>
                    </v-col>
                    <v-col>
                      <v-checkbox
                        v-model="currentAccInf.copyright"
                        label="Urheberrechtsschutz vorhanden"
                      ></v-checkbox>
                    </v-col>
                  </v-row>
                  <v-row>
                    <v-col cols="12" md="6" sm="6">
                      <v-select
                        :items="publicationType"
                        label="Publikationstyp"
                      ></v-select>
                    </v-col>
                  </v-row>
                </v-container>
              </v-card-text>
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn color="blue darken-1" text @click="closeEditItemDialog">
                  Abbrechen
                </v-btn>
                <v-btn color="blue darken-1" text> Speichern </v-btn>
              </v-card-actions>
            </v-card>
          </v-dialog>
          <v-btn color="error" @click="openDeleteItemDialog()">
            <v-icon left>mdi-delete</v-icon> Löschen
          </v-btn>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "../../node_modules/vue-class-component/lib";
import { AccessInformation } from "@/generated-sources/openapi";
import api from "@/api/api";
import { Result } from "neverthrow";

@Component
export default class AccessInformationList extends Vue {
  private items: Array<AccessInformation> = [];
  private currentAccInf = {} as AccessInformation;
  private currentIndex = -1;
  private dialogEdit = false;
  private dialogDelete = false;
  private deleteConfirmationLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";
  private publicationType = [
    "Nationallizens",
    "Konsortiallizens",
    "Publish-Komponente",
    "ZBW-Nutzungsvereinbarungen",
  ];

  public retrieveAccessInformation(): void {
    api
      .getList(0, 25)
      .then((response) => (this.items = response))
      .catch((e) => {
        console.log(e);
      });
  }

  public setActiveAccessInformation(
    accessInformation: AccessInformation,
    index: number
  ): void {
    this.currentAccInf = accessInformation;
    this.currentIndex = index;
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

  mounted(): void {
    this.retrieveAccessInformation();
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
