<script lang="ts">
import { MetadataRest } from "@/generated-sources/openapi";
import { computed, defineComponent, PropType } from "vue";
import metadata_utils from "@/utils/metadata_utils";
import { useSearchStore } from "@/stores/search";

export default defineComponent({
  computed: {
    metadata_utils() {
      return metadata_utils;
    },
  },
  props: {
    metadata: {
      type: Object as PropType<MetadataRest>,
      required: true,
    },
  },

  setup(props) {
    const searchStore = useSearchStore();
    const prettyPrint = (value: string | undefined) => {
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

    const parsePublicationType = (pubType: string) => {
      return metadata_utils.prettyPrintPublicationType(pubType);
    };

    return {
      currentMetadata,
      searchStore,
      prettyPrint,
      parseDateToLocaleString,
      parsePublicationType,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card v-if="currentMetadata.handle" class="mx-auto" tile>
    <v-card-title class="subheading font-weight-bold">Metadaten</v-card-title>
    <v-divider></v-divider>
    <v-expansion-panels focusable multiple>
      <v-expansion-panel>
        <v-expansion-panel-title>
          DSpace-Item Metadaten
        </v-expansion-panel-title>
        <v-expansion-panel-text>
          <v-container>
            <v-row v-show="currentMetadata.handle">
              <v-col>Item-Handle</v-col>
              <v-col>
                <td>
                  <a
                    v-bind:href="
                      metadata_utils.hrefHandle(
                        currentMetadata.handle,
                        searchStore.handleURLResolver,
                      )
                    "
                    >{{
                      metadata_utils.shortenHandle(currentMetadata.handle)
                    }}</a
                  >
                </td>
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.collectionName">
              <v-col>Collection</v-col>
              <v-col
                >{{ prettyPrint(currentMetadata.collectionName) }} (<a
                  v-bind:href="
                    metadata_utils.prependHandleUrl(
                      currentMetadata.collectionHandle,
                      searchStore.handleURLResolver,
                    )
                  "
                  >{{ currentMetadata.collectionHandle }}</a
                >)
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.subCommunityHandle">
              <v-col>Subcommunity</v-col>
              <v-col
                >{{ prettyPrint(currentMetadata.subCommunityName) }} (<a
                  v-bind:href="
                    metadata_utils.prependHandleUrl(
                      currentMetadata.subCommunityHandle,
                      searchStore.handleURLResolver,
                    )
                  "
                  >{{ currentMetadata.subCommunityHandle }}</a
                >)
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.communityName">
              <v-col>Community</v-col>
              <v-col
                >{{ prettyPrint(currentMetadata.communityName) }} (<a
                  v-bind:href="
                    metadata_utils.prependHandleUrl(
                      currentMetadata.communityHandle,
                      searchStore.handleURLResolver,
                    )
                  "
                  >{{ currentMetadata.communityHandle }}</a
                >)
              </v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.storageDate">
              <v-col>Speicherdatum im Digitalen Archiv</v-col>
              <v-col>{{
                parseDateToLocaleString(currentMetadata.storageDate)
              }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.deleted">
              <v-col>Item-Status</v-col>
              <v-col>❌gelöscht, zuletzt importiert am {{ currentMetadata.lastUpdatedOn.toLocaleString("de") }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-col></v-col>
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
              <v-col>{{
                parsePublicationType(currentMetadata.publicationType)
              }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row>
              <v-col>Publikationsjahr</v-col>
              <v-col>{{
                currentMetadata.publicationYear
              }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.doi">
              <v-col>DOI</v-col>
              <v-col>{{ prettyPrint(currentMetadata.doi) }}</v-col>
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
            <v-row v-show="currentMetadata.zdbIdJournal">
              <v-col>ZDB-ID (Zeitschrift)</v-col>
              <v-col>{{ prettyPrint(currentMetadata.zdbIdJournal) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.zdbIdSeries">
              <v-col>ZDB-ID (Serie)</v-col>
              <v-col>{{ prettyPrint(currentMetadata.zdbIdSeries) }}</v-col>
              <v-col></v-col>
            </v-row>
            <v-row v-show="currentMetadata.licenceUrl">
              <v-col>OC-/CC-Lizenz-URL</v-col>
              <v-col>{{ prettyPrint(currentMetadata.licenceUrl) }}</v-col>
              <v-col></v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-text>
      </v-expansion-panel>
    </v-expansion-panels>
  </v-card>
</template>
