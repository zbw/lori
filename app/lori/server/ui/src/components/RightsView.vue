<template>
  <v-card v-if="rights" class="mx-auto" tile>
    <v-alert v-model="updateSuccessful" dismissible text type="success">
      Rechteinformation erfolgreich geupdated.
    </v-alert>
    <v-divider></v-divider>
    <v-data-table :headers="selectedHeaders" :items="rights" :key="renderKey">
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title>Rechteinformationen</v-toolbar-title>
          <v-divider class="mx-4" inset vertical></v-divider>
          <v-spacer></v-spacer>
          <v-dialog v-model="dialog" max-width="500px">
            <template v-slot:activator="{ on, attrs }">
              <v-btn color="primary" dark class="mb-2" v-bind="attrs" v-on="on">
                Neu
              </v-btn>
            </template>
          </v-dialog>
        </v-toolbar>
      </template>
      <template v-slot:item.endDate="{ item }">
        <td>{{ item.endDate.toLocaleDateString("de") }}</td>
      </template>
      <template v-slot:item.startDate="{ item }">
        <td>{{ item.startDate.toLocaleDateString("de") }}</td>
      </template>
      <template v-slot:item.lastUpdatedOn="{ item }">
        <td>{{ item.lastUpdatedOn.toLocaleString("de") }}</td>
      </template>
      <template v-slot:item.actions="{ item, index }">
        <RightsEditDialog
          :activated="editDialog"
          :right="currentRight"
          :index="currentIndex"
          v-on:editDialogClosed="editRightClosed"
          v-on:updateSuccessful="updateRight"
        ></RightsEditDialog>
        <v-icon small class="mr-2" @click="editRight(item, index)">
          mdi-pencil
        </v-icon>
        <v-icon small> mdi-delete </v-icon>
      </template>
    </v-data-table>
  </v-card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import { Prop, Vue } from "vue-property-decorator";
import { RightRest } from "@/generated-sources/openapi";
import { DataTableHeader } from "vuetify";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
@Component({
  components: { RightsEditDialog },
})
export default class RightsView extends Vue {
  @Prop({ required: true })
  rights!: Array<RightRest>;

  private editDialog = false;
  private currentRight: RightRest = {} as RightRest;
  private currentIndex = 0;
  private headers = [
    {
      text: "Rechte-Id",
      align: "start",
      value: "rightId",
    },
    {
      text: "AccessState",
      value: "accessState",
    },
    {
      text: "Start-Datum",
      value: "startDate",
    },
    {
      text: "End-Datum",
      value: "endDate",
    },
    {
      text: "Lizenzbedingungen",
      value: "licenseConditions",
    },
    {
      text: "Provenienz-Lizenzinformation",
      value: "provenanceLicense",
    },
    {
      text: "Zuletzt verändert am",
      value: "lastUpdatedOn",
    },
    {
      text: "Zuletzt verändert von",
      value: "lastUpdatedBy",
    },
    { text: "Actions", value: "actions", sortable: false },
  ] as Array<DataTableHeader>;
  private renderKey = 0;
  private selectedHeaders: Array<DataTableHeader> = [];
  private updateSuccessful = false;

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
  }

  public editRight(right: RightRest, index: number): void {
    this.editDialog = true;
    this.currentIndex = index;
    this.currentRight = right;
    this.updateSuccessful = false;
  }

  public editRightClosed(): void {
    this.editDialog = false;
  }

  public updateRight(right: RightRest, index: number): void {
    this.rights[index] = right;
    this.renderKey += 1;
    this.editDialog = false;
    this.updateSuccessful = true;
  }

  created(): void {
    this.selectedHeaders = this.headers;
  }
}
</script>

<style scoped></style>
