<template>
  <v-dialog v-model="isActivated" max-width="500px" :retain-focus="false">
    <v-card>
      <v-card-title class="text-h5">Löschen bestätigen</v-card-title>
      <v-alert v-model="deleteAlertError" dismissible text type="error">
        Löschen war nicht erfolgreich:
        {{ deleteErrorMessage }}
      </v-alert>
      <v-card-text>
        Möchtest du diese Rechteinformation wirklich löschen?
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn :disabled="deleteInProgress" color="blue darken-1" @click="close"
          >Abbrechen
        </v-btn>
        <v-btn :loading="deleteInProgress" color="error" @click="deleteRight">
          Löschen
        </v-btn>
        <v-spacer></v-spacer>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import api from "@/api/api";
import Component from "vue-class-component";
import Vue from "vue";
import {Prop, Watch} from "vue-property-decorator";
import { RightRest } from "@/generated-sources/openapi";

@Component
export default class RightsDeleteDialog extends Vue {
  @Prop({ required: true })
  activated!: boolean;
  @Prop({ required: true })
  right!: RightRest;
  @Prop({ required: true })
  index!: number;
  @Prop({ required: true })
  metadataId!: string;

  private deleteAlertError = false;
  private deleteInProgress = false;
  private deleteErrorMessage = "";
  private deleteError = false;
  private isActivated = false;

  public close(): void {
    this.isActivated = false;
    this.$emit("deleteDialogClosed");
  }

  public deleteRight() {
    this.deleteInProgress = true;
    this.deleteAlertError = false;
    this.deleteError = false;

    if (this.right.rightId == undefined) {
      this.deleteErrorMessage = "A right-id is missing!";
      this.deleteError = true;
      this.deleteAlertError = true;
    } else {
      api
        .deleteItemRelation(this.metadataId, this.right.rightId)
        .then(() => {
          this.$emit("deleteSuccessful", this.index);
          this.close();
        })
        .catch((e) => {
          this.deleteErrorMessage =
            e.statusText + "(Statuscode: " + e.status + ")";
          this.deleteError = true;
          this.deleteAlertError = true;
        })
        .finally(() => {
          this.deleteInProgress = false;
        });
    }
  }

  @Watch("activated")
  onChangedActivated(other: boolean): void {
    this.isActivated = other;
  }
}
</script>

<style scoped></style>
