<template>
  <v-card v-if="displayedItem.metadataId" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold">Metadaten</v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable multiple>
      <v-expansion-panel>
        <v-expansion-panel-header>Ebene 1</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-container>
            <v-row>
              <v-col>Id</v-col>
              <v-col>{{ prettyPrint(displayedItem.metadataId) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.title">
              <v-col>Titel</v-col>
              <v-col>{{ prettyPrint(displayedItem.title) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.band">
              <v-col>Band</v-col>
              <v-col>{{ prettyPrint(displayedItem.band) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col>Publikationstyp</v-col>
              <v-col>{{ displayedItem.publicationType }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col>Publikationsjahr</v-col>
              <v-col>{{ displayedItem.publicationYear }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.doi">
              <v-col>DOI</v-col>
              <v-col>{{ prettyPrint(displayedItem.doi) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.handle">
              <v-col>Handle</v-col>
              <v-col>{{ prettyPrint(displayedItem.handle) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.isbn">
              <v-col>ISBN</v-col>
              <v-col>{{ prettyPrint(displayedItem.isbn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.ppn">
              <v-col>PPN</v-col>
              <v-col>{{ prettyPrint(displayedItem.ppn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.ppnEbook">
              <v-col>PPN-Ebook</v-col>
              <v-col>{{ prettyPrint(displayedItem.ppnEbook) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.rightsK10plus">
              <v-col>Zugriffsrecht K10Plus</v-col>
              <v-col>{{ prettyPrint(displayedItem.rightsK10plus) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.serialNumber">
              <v-col>Seriennummer</v-col>
              <v-col>{{ prettyPrint(displayedItem.serialNumber) }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>Ebene 2</v-expansion-panel-header>
        <v-expansion-panel-content>
          <v-container>
            <v-row v-show="displayedItem.issn">
              <v-col>Issn</v-col>
              <v-col>{{ prettyPrint(displayedItem.issn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.paketSigel">
              <v-col>Paket Sigel</v-col>
              <v-col>{{ prettyPrint(displayedItem.paketSigel) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.titleJournal">
              <v-col>Titel Zeitschrift</v-col>
              <v-col>{{ prettyPrint(displayedItem.titleJournal) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.titleSeries">
              <v-col>Titel Serie</v-col>
              <v-col>{{ prettyPrint(displayedItem.titleSeries) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="displayedItem.zbdId">
              <v-col>ZBD Id</v-col>
              <v-col>{{ prettyPrint(displayedItem.zbdId) }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>

<script lang="ts">
import {Prop, Vue} from "vue-property-decorator";
import Component from "vue-class-component";
import {MetadataRest} from "@/generated-sources/openapi";

@Component
export default class MetadataView extends Vue {
  @Prop({required: true})
  displayedItem!: MetadataRest;

  public prettyPrint(value: string): string {
    if (value) {
      return value;
    } else {
      return "Kein Wert vorhanden";
    }
  }
}
</script>

<style scoped></style>
