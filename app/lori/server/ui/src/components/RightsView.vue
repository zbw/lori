<template>
  <v-card v-if="rights" class="mx-auto" tile>
    <v-divider></v-divider>
    <v-data-table :headers="selectedHeaders" :items="rights">
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
      <template v-slot:item.actions="{ item }">
        <v-icon small class="mr-2"> mdi-pencil </v-icon>
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

@Component
export default class RightsView extends Vue {
  @Prop({ required: true })
  rights!: Array<RightRest>;

  private selectedHeaders: Array<DataTableHeader> = [];
  private headers = [
    {
      text: "Id",
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

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
  }

  created(): void {
    this.selectedHeaders = this.headers;
  }
}
</script>

<style scoped></style>
