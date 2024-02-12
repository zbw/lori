<script lang="ts">
import { MetadataRest } from "@/generated-sources/openapi";
import { computed, defineComponent, PropType } from "vue";

export default defineComponent({
  props: {
    metadata: {
      type: Object as PropType<MetadataRest>,
      required: true,
    },
  },

  setup(props) {
    const prettyPrint = (value: string) => {
      if (value) {
        return value;
      } else {
        return "Kein Wert vorhanden";
      }
    };
    const parseDateToLocaleString = (d: Date | undefined) => {
      if (d === undefined) {
        return "Please reload";
      } else {
        return d.toLocaleString("de");
      }
    };

    const currentMetadata = computed(() => {
      return props.metadata;
    });

    return {
      currentMetadata,
      prettyPrint,
      parseDateToLocaleString,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card v-if="currentMetadata.metadataId" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold">Metadaten</v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable multiple>
      <v-expansion-panel>
        <v-expansion-panel-title>
          DSpace-Item Metadaten
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-container>
            <v-row>
              <v-col>Id</v-col>
              <v-col>{{ prettyPrint(currentMetadata.metadataId) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.collectionName">
              <v-col>Collectionsname</v-col>
              <v-col>{{ prettyPrint(currentMetadata.collectionName) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.communityName">
              <v-col>Communityname</v-col>
              <v-col>{{ prettyPrint(currentMetadata.communityName) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.storageDate">
              <v-col>Speicherdatum im Digitalen Archiv</v-col>
              <v-col>{{
                parseDateToLocaleString(currentMetadata.storageDate)
              }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.subCommunitiesHandles">
              <v-col>Subcommunity Handle-Ids</v-col>
              <v-col>{{
                prettyPrint(currentMetadata.subCommunitiesHandles)
              }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-title>
          Bibliographische Metadaten
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-container>
            <v-row v-show="currentMetadata.title">
              <v-col>Titel</v-col>
              <v-col>{{ prettyPrint(currentMetadata.title) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.author">
              <v-col>Autor:in</v-col>
              <v-col>{{ prettyPrint(currentMetadata.author) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.band">
              <v-col>Band</v-col>
              <v-col>{{ prettyPrint(currentMetadata.band) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col>Publikationstyp</v-col>
              <v-col>{{ currentMetadata.publicationType }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col>Publikationsjahr</v-col>
              <v-col>{{
                currentMetadata.publicationDate.toLocaleDateString("de")
              }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.doi">
              <v-col>DOI</v-col>
              <v-col>{{ prettyPrint(currentMetadata.doi) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.handle">
              <v-col>Handle</v-col>
              <v-col>
                <td>
                  <a :href="currentMetadata.handle">{{
                    currentMetadata.handle
                  }}</a>
                </td>
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.collectionHandle">
              <v-col>Collection Handle</v-col>
              <v-col>
                <td>
                  <a
                    :href="
                      'http://hdl.handle.net/' +
                      currentMetadata.collectionHandle
                    "
                    >{{ currentMetadata.collectionHandle }}</a
                  >
                </td>
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.communityHandle">
              <v-col>Community Handle</v-col>
              <v-col>
                <td>
                  <a
                    :href="
                      'http://hdl.handle.net/' + currentMetadata.communityHandle
                    "
                    >{{ currentMetadata.communityHandle }}</a
                  >
                </td>
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.isbn">
              <v-col>ISBN</v-col>
              <v-col>{{ prettyPrint(currentMetadata.isbn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.ppn">
              <v-col>PPN</v-col>
              <v-col>{{ prettyPrint(currentMetadata.ppn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.rightsK10plus">
              <v-col>Zugriffsrecht K10Plus</v-col>
              <v-col>{{ prettyPrint(currentMetadata.rightsK10plus) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.serialNumber">
              <v-col>Seriennummer</v-col>
              <v-col>{{ prettyPrint(currentMetadata.serialNumber) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.issn">
              <v-col>Issn</v-col>
              <v-col>{{ prettyPrint(currentMetadata.issn) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.paketSigel">
              <v-col>Paket Sigel</v-col>
              <v-col>{{ prettyPrint(currentMetadata.paketSigel) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.titleJournal">
              <v-col>Titel Zeitschrift</v-col>
              <v-col>{{ prettyPrint(currentMetadata.titleJournal) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.titleSeries">
              <v-col>Titel Serie</v-col>
              <v-col>{{ prettyPrint(currentMetadata.titleSeries) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.zdbId">
              <v-col>ZDB Id</v-col>
              <v-col>{{ prettyPrint(currentMetadata.zdbId) }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
