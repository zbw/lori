<template>
  <v-card v-if="displayedItem.id" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold"> Metadaten </v-card-title>
    <v-divider></v-divider>
    <v-list dense>
      <v-list-item>
        <v-list-item-content>Id:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ displayedItem.id }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Titel:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.title) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Band:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.band) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Zugriffstatus:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.accessState) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Publikationstyp:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ displayedItem.publicationType }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Publikationsjahr:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ displayedItem.publicationYear }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>DOI:</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.doi) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Handle</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ displayedItem.handle }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>ISBN</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.isbn) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>ISSN</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.issn) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Paket-Sigel</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.paketSigel) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>PPN</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.ppn) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>PPN-Ebook</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.ppnEbook) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Zugriffsrecht K10Plus</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.rightsK10plus) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Seriennummer</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.serialNumber) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Titel Journal</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.titleJournal) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>Titel Serie</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.titleSeries) }}
        </v-list-item-content>
      </v-list-item>
      <v-list-item>
        <v-list-item-content>ZBD-ID</v-list-item-content>
        <v-list-item-content class="align-end">
          {{ prettyPrint(displayedItem.zbdId) }}
        </v-list-item-content>
      </v-list-item>
    </v-list>
  </v-card>
</template>

<script lang="ts">
import { Prop, Vue } from "vue-property-decorator";
import Component from "vue-class-component";
import { ItemRest } from "@/generated-sources/openapi";

@Component
export default class MetadataView extends Vue {
  @Prop({ required: true })
  displayedItem!: ItemRest;

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