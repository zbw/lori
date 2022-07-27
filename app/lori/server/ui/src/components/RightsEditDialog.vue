<template>
  <v-dialog
    v-model="isActivated"
    max-width="1000px"
    v-on:close="emitClosedDialog"
    v-on:click:outside="emitClosedDialog"
    :retain-focus="false"
  >
    <v-card>
      <v-expansion-panels focusable multiple v-model="openPanelsDefault">
        <v-expansion-panel>
          <v-expansion-panel-header
            >Steuerungsrelevante Elemente</v-expansion-panel-header
          >
          <v-expansion-panel-content>
            <v-container fluid>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Right-Id</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-if="isNew"
                    ref="rightId"
                    label="Wird automatisch generiert"
                    :rules="[rules.required]"
                    disabled
                    outlined
                    hint="Rechte Id"
                  ></v-text-field>
                  <v-text-field
                    v-if="!isNew"
                    ref="rightId"
                    v-model="tmpRight.rightId"
                    :rules="[rules.required]"
                    outlined
                    hint="Rechte Id"
                    disabled
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Aktueller Access-Status</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-select
                    ref="accessState"
                    :items="accessStatusSelect"
                    v-model="showAccessState"
                    :rules="[rules.required]"
                    outlined
                  ></v-select>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Gültigkeit Startdatum</v-subheader>
                </v-col>
                <v-col cols="8">
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
                        ref="startDate"
                        label="Start-Datum"
                        prepend-icon="mdi-calendar"
                        readonly
                        outlined
                        v-bind="attrs"
                        v-on="on"
                        :rules="[rules.required]"
                        required
                      ></v-text-field>
                    </template>
                    <v-date-picker v-model="tmpStartDate" no-title scrollable>
                      <v-spacer></v-spacer>
                      <v-btn
                        text
                        color="primary"
                        @click="menuStartDate = false"
                      >
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
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Gültigkeit Enddatum</v-subheader>
                </v-col>
                <v-col cols="8">
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
                        ref="endDate"
                        label="End-Datum"
                        prepend-icon="mdi-calendar"
                        readonly
                        outlined
                        v-bind="attrs"
                        v-on="on"
                        required
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
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Group</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    outlined
                    hint="Einschränkung des Zugriffs auf eine Berechtigungsgruppe"
                    ref="group"
                    :rules="[rules.maxLength256]"
                    counter
                    maxlength="256"
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Bemerkungen</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-textarea
                    v-model="tmpRight.notesGeneral"
                    hint="Allgemeine Bemerkungen"
                    ref="notesGeneral"
                    :rules="[rules.maxLength256]"
                    counter
                    maxlength="256"
                    outlined
                  ></v-textarea>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <v-expansion-panel>
          <v-expansion-panel-header>Formale Regelung</v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-container fluid>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Lizenzvertrag</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.licenceContract"
                    ref="licenceContract"
                    outlined
                    hint="Gibt Auskunft darüber, ob ein Lizenzvertrag für dieses Item als Nutzungsrechtsquelle vorliegt."
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Urheberrechtschrankennutzung</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-switch
                    v-model="tmpRight.authorRightException"
                    ref="authorRightException"
                    color="indigo"
                    label="Ja"
                    hint="Ist für die ZBW die Nutzung der Urheberrechtschranken möglich?"
                    persistent-hint
                  ></v-switch>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>ZBW Nutzungsvereinbarung</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-switch
                    v-model="tmpRight.zbwUserAgreement"
                    color="indigo"
                    label="Ja"
                    hint="Gibt Auskunft darüber, ob eine Nutzungsvereinbarung für dieses Item als Nutzungsrechtsquelle vorliegt."
                    persistent-hint
                  ></v-switch>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Open-Content-Licence</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    outlined
                    hint="Eine per URI eindeutig referenzierte Standard-Open-Content-Lizenz, die für das Item gilt."
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>
                    Nicht-standardisierte Open-Content-Lizenz (URL)
                  </v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.nonStandardOpenContentLicenceURL"
                    outlined
                    hint="Eine per URL eindeutig referenzierbare Nicht-standardisierte Open-Content-Lizenz, die für das Item gilt."
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader
                    >Nicht-standardisierte Open-Content-Lizenz (keine
                    URL)</v-subheader
                  >
                </v-col>
                <v-col cols="8">
                  <v-switch
                    v-model="tmpRight.nonStandardOpenContentLicence"
                    color="indigo"
                    label="Ja"
                    hint="Ohne URL, als Freitext (bzw. derzeit als Screenshot in Clearingstelle)"
                    persistent-hint
                  ></v-switch>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Eingeschränkte Open-Content-Lizenz</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-switch
                    v-model="tmpRight.restrictedOpenContentLicence"
                    color="indigo"
                    label="Ja"
                    hint="Gilt für dieses Item, dem im Element 'Open-Content-Licence' eine standardisierte Open-Content-Lizenz zugeordnet ist, eine Einschränkung?"
                    persistent-hint
                  ></v-switch>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Bemerkungen</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-textarea
                    v-model="tmpRight.notesFormalRules"
                    ref="notesFormalRules"
                    :rules="[rules.maxLength256]"
                    counter
                    maxlength="256"
                    hint="Bemerkungen für formale Regelungen"
                    outlined
                  ></v-textarea>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <v-expansion-panel>
          <v-expansion-panel-header
            >Prozessdokumentierende Elemente</v-expansion-panel-header
          >
          <v-expansion-panel-content>
            <v-container fluid>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Basis der Speicherung</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-select
                    :items="basisStorage"
                    v-model="selectBasisStorage"
                    ref="basisStorage"
                    :rules="[rules.required]"
                    outlined
                  ></v-select>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Basis des Access-Status</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-select
                    :items="basisAccessState"
                    ref="basisAccessState"
                    v-model="selectBasisAccessState"
                    :rules="[rules.required]"
                    outlined
                  ></v-select>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Bemerkungen</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-textarea
                    v-model="tmpRight.notesProcessDocumentation"
                    hint="Bemerkungen für prozessdokumentierende Elemente"
                    :rules="[rules.maxLength256]"
                    counter
                    maxlength="256"
                    ref="notesProcessDocumentation"
                    outlined
                  ></v-textarea>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-content>
        </v-expansion-panel>
        <v-expansion-panel>
          <v-expansion-panel-header>
            Metadaten über den Rechteinformationseintrag
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <v-container fluid>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Zuletzt editiert am</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastUpdatedOn"
                    readonly
                    outlined
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Zuletzt editiert von</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-text-field
                    v-model="tmpRight.lastUpdatedBy"
                    readonly
                    outlined
                  ></v-text-field>
                </v-col>
              </v-row>
              <v-row>
                <v-col cols="4">
                  <v-subheader>Bemerkungen</v-subheader>
                </v-col>
                <v-col cols="8">
                  <v-textarea
                    v-model="tmpRight.notesManagementRelated"
                    hint="Bemerkungen für Metadaten über den Rechteinformationseintrag"
                    ref="notesManagementRelated"
                    :rules="[rules.maxLength256]"
                    counter
                    maxlength="256"
                    outlined
                  ></v-textarea>
                </v-col>
              </v-row>
            </v-container>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-expansion-panels>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text @click="cancel">Abbrechen</v-btn>
        <v-btn
          color="blue darken-1"
          text
          @click="save"
          :disabled="updateInProgress"
          >Speichern
        </v-btn>
      </v-card-actions>
      <v-alert v-model="saveAlertError" dismissible text type="error">
        Speichern war nicht erfolgreich:
        {{ saveAlertErrorMessage }}
      </v-alert>
      <v-dialog v-model="updateConfirmDialog" max-width="500px">
        <v-card>
          <v-card-title class="text-h5"> Achtung</v-card-title>
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
import {
  ItemEntry,
  RightRest,
  RightRestAccessStateEnum,
  RightRestBasisAccessStateEnum,
  RightRestBasisStorageEnum,
} from "../generated-sources/openapi";

@Component
export default class RightsEditDialog extends Vue {
  @Prop({ required: true })
  activated!: boolean;
  @Prop({ required: true })
  right!: RightRest;
  @Prop({ required: true })
  index!: number;
  @Prop({ required: true })
  isNew!: boolean;
  @Prop({ required: true })
  metadataId!: string;

  // Important: Panels that contain required fields need to be expanded by default.
  // Otherwise, the required check will be skipped (see validateInput).
  private openPanelsDefault = [0];
  private accessStatusSelect = ["Open", "Closed", "Restricted"];
  private basisAccessState = [
    "Lizenzvertrag",
    "OA-Rechte aus Lizenzvertrag",
    "Nutzungsvereinbarung",
    "Urheberrechtschranke",
    "ZBW-Policy",
  ];
  private basisStorage = [
    "Lizenzvertrag",
    "Nutzungsvereinbarung",
    "Urheberrechtschranke",
    "Open-Content-Lizenz",
    "ZBW-Policy (Eingeschränkte OCL)",
    "ZBW-Policy (unbeantwortete Rechteanforderung)",
  ];
  private isActivated = false;
  private formHasErrors = false;
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
  private rules = {
    required: (value: string) => {
      return !!value || "Benötigt.";
    },
    maxLength256: (value: string) => {
      if (value == undefined) {
        return true;
      } else {
        return value.length <= 256 || "Max 256 Zeichen";
      }
    },
  };

  private tmpRight: RightRest = {} as RightRest;

  public emitClosedDialog(): void {
    this.$emit("editDialogClosed");
  }

  public close(): void {
    this.updateConfirmDialog = false;
    this.updateInProgress = false;
    this.emitClosedDialog();
  }

  public cancel(): void {
    this.tmpRight = Object.assign({}, this.right);
    this.close();
  }

  public cancelConfirm(): void {
    this.updateConfirmDialog = false;
  }

  public createRight(): void {
    this.updateInProgress = true;
    this.tmpRight.rightId = "unset";
    this.tmpRight.startDate = new Date(this.tmpStartDate);
    this.tmpRight.endDate =
      this.tmpEndDate == "" ? undefined : new Date(this.tmpEndDate);
    api
      .addRight(this.tmpRight)
      .then((r) => {
        api
          .addItemEntry({
            metadataId: this.metadataId,
            rightId: r.rightId,
          } as ItemEntry)
          .then(() => {
            this.tmpRight.rightId = r.rightId;
            this.$emit("addSuccessful", this.tmpRight);
            this.close();
          })
          .catch((e) => {
            console.log(e);
            this.saveAlertError = true;
            this.saveAlertErrorMessage =
              e.statusText + " (Statuscode: " + e.status + ")";
            this.updateConfirmDialog = false;
          });
      })
      .catch((e) => {
        console.log(e);
        this.saveAlertError = true;
        this.saveAlertErrorMessage =
          e.statusText + " (Statuscode: " + e.status + ")";
        this.updateConfirmDialog = false;
      });
  }

  public updateRight(): void {
    this.updateInProgress = true;
    this.tmpRight.startDate = new Date(this.tmpStartDate);
    this.tmpRight.endDate =
      this.tmpEndDate == "" ? undefined : new Date(this.tmpEndDate);
    api
      .updateRight(this.tmpRight)
      .then(() => {
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
    this.validateInput();
    if (this.formHasErrors) {
      return;
    }
    if (this.isNew) {
      this.createRight();
    } else {
      this.updateRight();
    }
  }

  public isIdDisabled() {
    return !this.isNew;
  }

  public validateInput() {
    this.formHasErrors = false;
    Object.keys(this.form).forEach((f) => {
      if (
        this.$refs[f] != undefined &&
        !(
          this.$refs[f] as Vue & { validate: (v: boolean) => boolean }
        ).validate(true)
      ) {
        this.formHasErrors = true;
      }
    });
  }

  mounted(): void {
    this.showDialog = this.activated;
  }

  // Computed properties
  get form() {
    return {
      accessState: this.tmpRight.accessState,
      basisAccessState: this.tmpRight.accessState,
      basisStorage: this.tmpRight.basisStorage,
      startDate: this.tmpRight.startDate,
      endDate: this.tmpRight.endDate,
      notesFormalRules: this.tmpRight.notesFormalRules,
      notesGeneral: this.tmpRight.notesGeneral,
      notesProcessDocumentation: this.tmpRight.notesProcessDocumentation,
      notesManagementRelated: this.tmpRight.notesManagementRelated,
      licenceContract: this.tmpRight.licenceContract,
    };
  }

  get showAccessState() {
    let accessState = this.tmpRight.accessState;
    if (accessState == undefined) {
      return "Kein Wert";
    } else {
      switch (accessState) {
        case RightRestAccessStateEnum.Open:
          return "Open";
        case RightRestAccessStateEnum.Closed:
          return "Closed";
        default:
          return "Restricted";
      }
    }
  }

  set showAccessState(value: string | undefined) {
    if (value == undefined || value == "Kein Wert") {
      this.tmpRight.accessState = undefined;
    } else {
      switch (value) {
        case "Open":
          this.tmpRight.accessState = RightRestAccessStateEnum.Open;
          break;
        case "Closed":
          this.tmpRight.accessState = RightRestAccessStateEnum.Closed;
          break;
        default:
          this.tmpRight.accessState = RightRestAccessStateEnum.Restricted;
          break;
      }
    }
  }
  get selectBasisStorage() {
    let basisStorage = this.tmpRight.basisStorage;
    if (basisStorage == undefined) {
      return "Kein Wert";
    } else {
      switch (basisStorage) {
        case RightRestBasisStorageEnum.AuthorRightException:
          return "Urheberrechtschranke";
        case RightRestBasisStorageEnum.UserAgreement:
          return "Nutzungsvereinbarung";
        case RightRestBasisStorageEnum.OpenContentLicence:
          return "Open-Content-Lizenz";
        case RightRestBasisStorageEnum.ZbwPolicyUnanswered:
          return "ZBW-Policy (unbeantwortete Rechteanforderung)";
        case RightRestBasisStorageEnum.ZbwPolicyRestricted:
          return "ZBW-Policy (Eingeschränkte OCL)";
        default:
          return "Lizenzvertrag";
      }
    }
  }

  set selectBasisStorage(value: string | undefined) {
    if (value == undefined) {
      this.tmpRight.basisStorage = undefined;
    } else {
      switch (value) {
        case "Lizenzvertrag":
          this.tmpRight.basisStorage =
            RightRestBasisStorageEnum.LicenceContract;
          break;
        case "Nutzungsvereinbarung":
          this.tmpRight.basisStorage = RightRestBasisStorageEnum.UserAgreement;
          break;
        case "Urheberrechtschranke":
          this.tmpRight.basisStorage =
            RightRestBasisStorageEnum.AuthorRightException;
          break;
        case "Open-Content-Lizenz":
          this.tmpRight.basisStorage =
            RightRestBasisStorageEnum.OpenContentLicence;
          break;
        case "ZBW-Policy (Eingeschränkte OCL)":
          this.tmpRight.basisStorage =
            RightRestBasisStorageEnum.ZbwPolicyRestricted;
          break;
        default:
          this.tmpRight.basisStorage =
            RightRestBasisStorageEnum.ZbwPolicyUnanswered;
          break;
      }
    }
  }
  get selectBasisAccessState() {
    let basisAccessState = this.tmpRight.basisAccessState;
    if (basisAccessState == undefined) {
      return "Kein Wert";
    } else {
      switch (basisAccessState) {
        case RightRestBasisAccessStateEnum.AuthorRightException:
          return "Urheberrechtschranke";
        case RightRestBasisAccessStateEnum.UserAgreement:
          return "Nutzungsvereinbarung";
        case RightRestBasisAccessStateEnum.LicenceContract:
          return "Lizenzvertrag";
        case RightRestBasisAccessStateEnum.ZbwPolicy:
          return "ZBW-Policy";
        default:
          return "OA-Rechte aus Lizenzvertrag";
      }
    }
  }

  set selectBasisAccessState(value: string | undefined) {
    if (value == undefined) {
      this.tmpRight.basisAccessState = undefined;
    } else {
      switch (value) {
        case "Lizenzvertrag":
          this.tmpRight.basisAccessState =
            RightRestBasisAccessStateEnum.LicenceContract;
          break;
        case "Nutzungsvereinbarung":
          this.tmpRight.basisAccessState =
            RightRestBasisAccessStateEnum.UserAgreement;
          break;
        case "OA-Rechte aus Lizenzvertrag":
          this.tmpRight.basisAccessState =
            RightRestBasisAccessStateEnum.LicenceContractOa;
          break;
        case "Urheberrechtschranke":
          this.tmpRight.basisAccessState =
            RightRestBasisAccessStateEnum.AuthorRightException;
          break;
        default:
          this.tmpRight.basisAccessState =
            RightRestBasisAccessStateEnum.ZbwPolicy;
          break;
      }
    }
  }

  get title() {
    if (this.isNew) {
      return "Erstelle";
    } else {
      return "Editiere";
    }
  }

  // Watched properties
  @Watch("right")
  onChangedRight(other: RightRest): void {
    this.tmpRight = Object.assign({}, other);
    if (!this.isNew) {
      this.tmpStartDate = this.right.startDate.toISOString().slice(0, 10);
      if (this.right.endDate !== undefined) {
        this.tmpEndDate = this.right.endDate.toISOString().slice(0, 10);
      } else {
        this.tmpEndDate = "";
      }
    } else {
      this.tmpEndDate = "";
      this.tmpStartDate = "";
    }
  }

  @Watch("isNew")
  onChangedIsNew(): void {
    this.isIdDisabled();
  }

  @Watch("activated")
  onChangedActivated(other: boolean): void {
    this.isActivated = other;
  }
}
</script>

<style scoped></style>
