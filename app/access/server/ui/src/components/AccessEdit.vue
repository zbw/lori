<template>
  <v-card>
    <v-card-title>
      <span class="text-h5">Editiere Eintrag</span>
    </v-card-title>
    <v-card-text>
      <v-container>
        <v-row>
          <v-col cols="12" md="6" sm="6">
            <v-text-field
                v-model="editItem.id"
                disabled
                label="Id"
            ></v-text-field>
          </v-col>
          <v-col cols="12" md="4" sm="6">
            <v-text-field
                v-model="editItem.tenant"
                label="Zustände Einrichtung"
            ></v-text-field>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="12" md="6" sm="6">
            <v-checkbox
                v-model="editItem.commercialuse"
                label="Kommerzielle Nutzung erlaubt"
            ></v-checkbox>
          </v-col>
          <v-col>
            <v-checkbox
                v-model="editItem.copyright"
                label="Urheberrechtsschutz vorhanden"
            ></v-checkbox>
          </v-col>
        </v-row>
        <v-row>
          <v-col cols="12" md="6" sm="6">
            <v-select
                :items="publicationType"
                label="Publikationstyp"
            ></v-select>
          </v-col>
          <v-col cols="12" md="6" sm="6">
            <v-select :items="licenseType" label="Lizenstyp"></v-select>
          </v-col>
        </v-row>
      </v-container>
    </v-card-text>
    <v-card-actions>
      <v-spacer></v-spacer>
      <v-btn color="blue darken-1" text @click="closeDialog"> Abbrechen</v-btn>
      <v-btn color="blue darken-1" text> Speichern</v-btn>
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import {ItemRest} from "@/generated-sources/openapi";
import {Prop, Vue} from "vue-property-decorator";
import Component from "vue-class-component";
import {License, Publication} from "@/types/types";
import {$enum} from "ts-enum-util";

@Component
export default class AccessEdit extends Vue {
  @Prop({required: true})
  editItem!: ItemRest;

  private licenseType = $enum(License).getValues();
  private publicationType = $enum(Publication).getValues();

  public closeDialog(): void {
    this.$emit("closeDialog");
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
