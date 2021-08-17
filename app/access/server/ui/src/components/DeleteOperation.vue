<template>
  <div>
    <v-dialog v-model="dialogDelete" max-width="500px">
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
  </div>
</template>

<script lang="ts">
import Component from "../../node_modules/vue-class-component/lib";
import Vue from "vue";
import {AccessInformation} from "@/generated-sources/openapi";

@Component
export default class DeleteOperation extends Vue {
  @Prop({required})
  private id = "";
  private dialogDelete = false;
  private deleteLoading = false;
  private deleteAlertSuccessful = false;
  private deleteAlertError = false;
  private deleteErrorMessage = "";

  mounted() {
    this.id = this.$route.params.id;
  }
}
</script>

<style scoped></style>
