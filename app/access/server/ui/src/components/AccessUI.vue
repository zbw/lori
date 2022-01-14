<template>
  <v-app>
    <v-toolbar class="cyan darken-4" dark>
      <v-toolbar-title>AccessUI</v-toolbar-title>
    </v-toolbar>
    <v-main>
      <v-data-table
        v-model="selected"
        :headers="headers"
        :items="items"
        class="elevation-1"
        item-key="id"
        show-select
      >
        <template v-slot:top>
          <v-toolbar flat>
            <v-dialog v-model="dialogDelete" max-width="500px">
              <v-card>
                <v-card-title class="text-h5">
                  Are you sure you want to delete this item?
                </v-card-title>
                <v-card-actions>
                  <v-spacer></v-spacer>
                  <v-btn
                    :disabled="deleteLoading"
                    color="blue darken-1"
                    @click="cancelDeleteDialog"
                    >Cancel
                  </v-btn>
                  <v-btn
                    :loading="deleteLoading"
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
          </v-toolbar>
        </template>
        <template v-slot:item.action="{ item }">
          <v-icon class="mr-2" small @click="openDeleteItemDialog(item)">
            mdi-delete
          </v-icon>
        </template>
      </v-data-table>
    </v-main>
  </v-app>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "../../node_modules/vue-class-component/lib";
import api from "@/api/api";
import { Result } from "neverthrow";
import {
  ItemRest,
  ItemRestPublicationTypeEnum,
} from "@/generated-sources/openapi";

@Component
export default class AccessUi extends Vue {
  private dialogDelete = false;
  private headers = [
    {
      text: "Id",
      align: "start",
      value: "id",
    },
    { text: "Tenant", value: "tenant" },
    { text: "Usage guide", value: "usageGuide" },
    { text: "Template", value: "template" },
    { text: "Mention", value: "mention" },
    { text: "Sharealike", value: "sharealike" },
    { text: "Commercial Use", value: "commercial" },
    { text: "Copyright", value: "copyright" },
    { text: "Actions", value: "actions" },
    { text: "Aktion", value: "action", sortable: false },
  ];
  private indexToDelete = -1;
  private items: Array<ItemRest> = [];
  private defaultItem: ItemRest = {
    id: "",
    title: "",
    handle: "",
    publicationYear: 2022,
    publicationType: ItemRestPublicationTypeEnum.Mono,
    actions: [],
  };
  private deleteLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";
  private itemToDelete: ItemRest = this.defaultItem;

  public fetchListPart(offset: number, limit: number): void {
    api.getList(offset, limit).then((response) => (this.items = response));
  }

  public openDeleteItemDialog(item: ItemRest): void {
    this.dialogDelete = true;
    this.itemToDelete = item;
    this.indexToDelete = this.items.indexOf(item);
  }

  public approveDeleteDialog(): void {
    this.deleteLoading = true;
    api
      .deleteAccessInformation(this.itemToDelete.id)
      .then((response: Result<void, Error>) => {
        if (response.isOk()) {
          this.deleteAlertSuccessful = true;
          this.items.splice(this.indexToDelete, 1);
        } else {
          this.deleteAlertError = true;
          this.deleteErrorMessage = response.error.message;
        }
      });
    this.dialogDelete = false;
    this.deleteLoading = false;
  }

  public cancelDeleteDialog(): void {
    this.dialogDelete = false;
    this.$nextTick(() => {
      this.indexToDelete = -1;
      this.itemToDelete = this.defaultItem;
    });
  }

  mounted(): void {
    this.fetchListPart(0, 25);
  }
}
</script>
