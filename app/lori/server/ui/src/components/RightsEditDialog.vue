<template>
  <v-dialog
    v-model="activated"
    max-width="500px"
    @close="emitClosedDialog"
    :retain-focus="false"
  >
    <v-card>
      <v-card-title>
        <span class="text-h5">Editiere Eintrag</span>
      </v-card-title>
      <v-card-text>
        <v-container>
          <v-row>
            <v-col cols="12" sm="6" md="4">
              <v-text-field
                disabled="true"
                v-model="right.rightId"
                label="RightId"
              ></v-text-field>
            </v-col>
            <v-col cols="15" sm="6" md="4">
              <v-menu
                ref="menuStart"
                v-model="menuStartDate"
                :close-on-content-click="false"
                :return-value.sync="tmpStartDate"
                transition="scale-transition"
                offset-y
                min-width="auto"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-text-field
                    v-model="tmpStartDate"
                    label="Start-Datum"
                    prepend-icon="mdi-calendar"
                    readonly
                    v-bind="attrs"
                    v-on="on"
                  ></v-text-field>
                </template>
                <v-date-picker v-model="tmpStartDate" no-title scrollable>
                  <v-spacer></v-spacer>
                  <v-btn text color="primary" @click="menuStartDate = false">
                    Cancel
                  </v-btn>
                  <v-btn
                    text
                    color="primary"
                    @click="$refs.menuStart.save(tmpStartDate)"
                  >
                    OK
                  </v-btn>
                </v-date-picker>
              </v-menu>
            </v-col>
            <v-col cols="15" sm="6" md="4">
              <v-menu
                ref="menuEnd"
                v-model="menuEndDate"
                :close-on-content-click="false"
                :return-value.sync="tmpEndDate"
                transition="scale-transition"
                offset-y
                min-width="auto"
              >
                <template v-slot:activator="{ on, attrs }">
                  <v-text-field
                    v-model="tmpEndDate"
                    label="End-Datum"
                    prepend-icon="mdi-calendar"
                    readonly
                    v-bind="attrs"
                    v-on="on"
                  ></v-text-field>
                </template>
                <v-date-picker v-model="tmpEndDate" no-title scrollable>
                  <v-spacer></v-spacer>
                  <v-btn text color="primary" @click="menuEndDate = false">
                    Cancel
                  </v-btn>
                  <v-btn
                    text
                    color="primary"
                    @click="$refs.menuEnd.save(tmpEndDate)"
                  >
                    OK
                  </v-btn>
                </v-date-picker>
              </v-menu>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <v-text-field
                v-model="tmpRight.licenseConditions"
                label="Lizensbedingungen"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6" md="4">
              <v-text-field
                v-model="tmpRight.provenanceLicense"
                label="Provenance"
              ></v-text-field>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>

      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text @click="cancel">Abbrechen</v-btn>
        <v-btn
          color="blue darken-1"
          text
          @click="save"
          :disabled="updateInProgress"
          >Speichern</v-btn
        >
      </v-card-actions>
      <v-alert v-model="saveAlertError" dismissible text type="error">
        Speichern war nicht erfolgreich:
        {{ saveAlertErrorMessage }}
      </v-alert>
      <v-dialog v-model="updateConfirmDialog" max-width="500px">
        <v-card>
          <v-card-title class="text-h5"> Achtung </v-card-title>
          <v-card-text>
            {{ metadataCount - 1 }} andere Items verweisen ebenfalls auf diese
            Rechteinformation. Mit der Bestätigung wird die Rechteinformation an
            all diesen geändert. Bist du dir sicher?
          </v-card-text>
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              :disabled="updateInProgress"
              color="blue darken-1"
              @click="cancelConfirm"
              >Abbrechen
            </v-btn>
            <v-btn
              :loading="updateInProgress"
              color="error"
              @click="updateRight"
            >
              Update
            </v-btn>
            <v-spacer></v-spacer>
          </v-card-actions>
        </v-card>
      </v-dialog>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import api from "@/api/api";
import Component from "vue-class-component";
import { Prop, Vue, Watch } from "vue-property-decorator";
import { RightRest } from "@/generated-sources/openapi";

@Component
export default class RightsEditDialog extends Vue {
  @Prop({ required: true })
  activated!: boolean;
  @Prop({ required: true })
  right!: RightRest;
  @Prop({ required: true })
  index!: number;

  private showDialog = false;
  private menuEndDate = false;
  private menuStartDate = false;
  private tmpStartDate = "";
  private tmpEndDate = "";
  private saveAlertError = false;
  private saveAlertErrorMessage = "";
  private updateConfirmDialog = false;
  private updateInProgress = false;
  private metadataCount = 0;

  private tmpRight: RightRest = {} as RightRest;

  public emitClosedDialog(): void {
    this.$emit("editDialogClosed");
  }

  public close(): void {
    this.updateConfirmDialog = false;
    this.updateInProgress = false;
    this.activated = false;
    this.emitClosedDialog();
  }

  public cancel(): void {
    this.tmpRight = Object.assign({}, this.right);
    this.close();
  }

  public cancelConfirm(): void {
    this.updateConfirmDialog = false;
  }

  public updateRight(): void {
    this.updateInProgress = true;
    this.tmpRight.startDate = new Date(this.tmpStartDate);
    this.tmpRight.endDate = new Date(this.tmpEndDate);
    api
      .updateRight(this.tmpRight)
      .then(() => {
        this.right = Object.assign({}, this.tmpRight);
        this.$emit("updateSuccessful", this.tmpRight, this.index);
        this.close();
      })
      .catch((e) => {
        console.log(e);
        this.saveAlertError = true;
        this.saveAlertErrorMessage =
          e.statusText + " (Statuscode: " + e.status + ")";
        this.updateConfirmDialog = false;
      });
  }

  public save(): void {
    api
      .getItemCountByRightId(this.right.rightId)
      .then((response) => {
        if (response.count == 1) {
          this.updateRight();
        } else {
          this.metadataCount = response.count;
          this.updateConfirmDialog = true;
        }
      })
      .catch((e) => {
        console.log(e);
        this.saveAlertError = true;
        this.saveAlertErrorMessage =
          e.statusText + " (Statuscode: " + e.status + ")";
      });
  }

  mounted(): void {
    this.showDialog = this.activated;
  }

  @Watch("right")
  onChangedRight(other: RightRest): void {
    this.tmpRight = Object.assign({}, other);
    this.tmpEndDate = this.right.endDate.toISOString().slice(0, 10);
    this.tmpStartDate = this.right.startDate.toISOString().slice(0, 10);
  }
}
</script>

<style scoped></style>
