<template>
  <v-container>
    <v-text-field label="Suche nach einem Titel" outlined></v-text-field>
    <v-btn outlined @click="searchTitle">
      <v-icon left>mdi-magnify</v-icon> Suche
    </v-btn>
    <v-row>
      <v-col>
        <v-card class="mx-auto" max-width="300" tile>
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
        <v-card class="mx-auto" max-width="300" tile>
          <v-dialog v-model="dialogDelete" max-width="500px">
            <v-card>
              <v-card-title class="text-h5">
                Are you sure you want to delete this item?</v-card-title
              >
              <v-card-actions>
                <v-spacer></v-spacer>
                <v-btn
                  :disabled="deleteConfirmationLoading"
                  color="blue darken-1"
                  @click="cancelDeleteDialog"
                  >Cancel</v-btn
                >
                <v-btn
                  :loading="deleteConfirmationLoading"
                  color="error"
                  @click="approveDeleteDialog"
                >
                  Delete
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
            Delete operation was successful.
          </v-alert>
          <v-alert v-model="deleteAlertError" dismissible text type="error">
            Delete operation was not successful: {{ deleteErrorMessage }}
          </v-alert>
          <div v-if="currentAccInf.id">
            <h4>Eintrag:</h4>
            <div>
              <label><strong>Titel:</strong></label> {{ currentAccInf.id }}
            </div>
            <div>
              <label><strong>Handle URL:</strong></label>
              12345/67890
            </div>
            <div>
              <label><strong>Status:</strong></label>
              {{ currentAccInf.commercialuse ? "Restricted" : "Open Access" }}
            </div>
            <v-btn
              :href="'/accessinformation/' + currentAccInf.id"
              class="ma-2"
              color="success"
              outlined
              tile
            >
              <v-icon left>mdi-pencil</v-icon> Bearbeiten
            </v-btn>
            <v-btn color="error" @click="openDeleteItemDialog()">
              <v-icon left>mdi-delete</v-icon> Delete
            </v-btn>
          </div>
          <div v-else>
            <br />
            <p>Bitte einen Eintrag auswählen ...</p>
          </div>
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

@Component
export default class AccessInformationList extends Vue {
  private items: Array<AccessInformation> = [];
  private currentAccInf = {} as AccessInformation;
  private currentIndex = -1;
  private dialogDelete = false;
  private deleteConfirmationLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";

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

  public openDeleteItemDialog(): void {
    this.dialogDelete = true;
  }

  public approveDeleteDialog(): void {
    this.deleteConfirmationLoading = true;
    api
      .deleteAccessInformation(this.currentAccInf.id)
      .then(() => {
        this.deleteAlertSuccessful = true;
        this.retrieveAccessInformation();
      })
      .catch((e) => {
        this.deleteAlertError = true;
        this.deleteErrorMessage = e.status + " - " + e.statusText;
      })
      .finally(() => {
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
