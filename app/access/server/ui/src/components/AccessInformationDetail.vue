<template>
  <v-form ref="form" v-model="valid" disabled="true" lazy-validation>
    <v-text-field v-model="currentAccInf.id" label="Id" required></v-text-field>
    <v-text-field v-model="currentAccInf.tenant" label="Tenant"></v-text-field>
    <v-dialog v-model="deleteDialogActive" max-width="500px">
      <v-card>
        <v-card-title class="text-h5">
          Are you sure you want to delete item {{ id }}?</v-card-title
        >
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            :disabled="deleteLoading"
            color="blue darken-1"
            @click="cancelDeleteDialog"
            >Cancel</v-btn
          >
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
    <v-alert v-model="deleteAlertSuccessful" dismissible text type="success">
      Delete operation was successful.
    </v-alert>
    <v-alert v-model="deleteAlertError" dismissible text type="error">
      Delete operation was not successful: {{ deleteErrorMessage }}
    </v-alert>
  </v-form>
</template>

<script lang="ts">
import Component from "../../node_modules/vue-class-component/lib";
import Vue from "vue";
import { AccessInformation } from "@/generated-sources/openapi";
import api from "@/api/api";

@Component
export default class AccessInformationDetail extends Vue {
  private valid = true;
  private currentAccInf = {} as AccessInformation;
  private cancelDeleteDialog = false;
  private approveDeleteDialog = false;
  private deleteDialogActive = false;
  private deleteLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";

  public getAccessInformation(id: string): void {
    api
      .getItemById(id)
      .then((response) => {
        this.currentAccInf = response;
      })
      .catch((e) => {
        console.log(e);
      });
  }

  mounted(): void {
    this.getAccessInformation(this.$route.params.id);
  }
}
</script>

<style scoped></style>
