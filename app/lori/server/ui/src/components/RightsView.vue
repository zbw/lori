<template>
  <v-card v-if="rights" class="mx-auto" tile>
    <v-alert v-model="updateSuccessful" dismissible text type="success">
      Rechteinformation erfolgreich geupdated.
    </v-alert>
    <v-alert v-model="addSuccessful" dismissible text type="success">
      Rechteinformation erfolgreich hinzugefügt.
    </v-alert>
    <v-divider></v-divider>
    <v-data-table
      :headers="selectedHeaders"
      :items="rights"
      :key="renderKey"
      @click:row="editRightSlot"
    >
      <template v-slot:top>
        <v-toolbar flat>
          <v-toolbar-title>Rechteinformationen</v-toolbar-title>
          <v-divider class="mx-4" inset vertical></v-divider>
          <v-spacer></v-spacer>
          <v-btn @click="newRight()" color="primary" dark class="mb-2">
            Neu
          </v-btn>
        </v-toolbar>
      </template>
      <template v-slot:item.endDate="{ item }">
        <td>{{ parseEndDate(item.endDate) }}</td>
      </template>
      <template v-slot:item.startDate="{ item }">
        <td>{{ item.startDate.toLocaleDateString("de") }}</td>
      </template>
    </v-data-table>

    <RightsEditDialog
      :activated="editDialogActivated"
      :right="currentRight"
      :index="currentIndex"
      :isNew="isNew"
      :metadataId="metadataId"
      v-on:deleteSuccessful="deleteSuccessful"
      v-on:editDialogClosed="editRightClosed"
      v-on:updateSuccessful="updateRight"
      v-on:addSuccessful="addRight"
    ></RightsEditDialog>
    <RightsDeleteDialog
      :activated="deleteDialogActivated"
      :right="currentRight"
      :index="currentIndex"
      :metadataId="metadataId"
      v-on:deleteSuccessful="deleteSuccessful"
      v-on:deleteDialogClosed="deleteDialogClosed"
    ></RightsDeleteDialog>
  </v-card>
</template>

<script lang="ts">
import Component from "vue-class-component";
import { Prop, Vue } from "vue-property-decorator";
import { RightRest } from "@/generated-sources/openapi";
import { DataTableHeader } from "vuetify";
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import RightsDeleteDialog from "@/components/RightsDeleteDialog.vue";
import { ItemSlot } from "@/types/types";

@Component({
  components: { RightsDeleteDialog, RightsEditDialog },
})
export default class RightsView extends Vue {
  @Prop({ required: true })
  rights!: Array<RightRest>;
  @Prop({ required: true })
  metadataId!: string;

  private editDialogActivated = false;
  private currentRight: RightRest = {} as RightRest;
  private currentIndex = 0;
  private deleteDialogActivated = false;
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
  ] as Array<DataTableHeader>;
  private isNew = false;
  private renderKey = 0;
  private selectedHeaders: Array<DataTableHeader> = [];
  private updateSuccessful = false;
  private addSuccessful = false;

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
  }

  public editRight(right: RightRest, index: number): void {
    this.editDialogActivated = true;
    this.currentIndex = index;
    this.currentRight = right;
    this.updateSuccessful = false;
    this.addSuccessful = false;
    this.isNew = false;
  }

  public editRightSlot(right: RightRest, slot: ItemSlot): void {
    this.editRight(right, slot.index);
  }

  public newRight(): void {
    this.editDialogActivated = true;
    this.currentRight = {} as RightRest;
    this.updateSuccessful = false;
    this.addSuccessful = false;
    this.currentIndex = -1;
    this.isNew = true;
  }

  public editRightClosed(): void {
    this.editDialogActivated = false;
  }

  public addRight(right: RightRest): void {
    this.rights.unshift(right);
    this.renderKey += 1;
    this.editDialogActivated = false;
    this.addSuccessful = true;
  }

  public updateRight(right: RightRest, index: number): void {
    this.rights[index] = right;
    this.renderKey += 1;
    this.editDialogActivated = false;
    this.updateSuccessful = true;
  }

  public initiateDeleteDialog(right: RightRest, index: number): void {
    this.currentIndex = index;
    this.currentRight = right;
    this.deleteDialogActivated = true;
  }

  public deleteSuccessful(index: number): void {
    this.rights.splice(index, 1);
    this.renderKey += 1;
  }

  public deleteDialogClosed(): void {
    this.deleteDialogActivated = false;
  }

  public parseLastUpdatedOn(d: Date | undefined): string {
    if (d === undefined) {
      return "Please reload";
    } else {
      return d.toLocaleString("de");
    }
  }

  public parseEndDate(d: Date | undefined): string {
    if (d === undefined) {
      return "";
    } else {
      return d.toLocaleDateString("de");
    }
  }

  created(): void {
    this.selectedHeaders = this.headers;
  }
}
</script>

<style scoped></style>
