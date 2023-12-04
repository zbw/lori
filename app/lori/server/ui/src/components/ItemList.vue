<script lang="ts">
import {
  AccessStateWithCountRest,
  BookmarkRest,
  ItemInformation,
  ItemRest,
  MetadataRest,
  PaketSigelWithCountRest,
  PublicationTypeWithCountRest,
  ZdbIdWithCountRest,
} from "@/generated-sources/openapi";
import api from "@/api/api";
import { DataTableHeader } from "vuetify";
import GroupOverview from "@/components/GroupOverview.vue";
import MetadataView from "@/components/MetadataView.vue";
import RightsView from "@/components/RightsView.vue";
import SearchFilter from "@/components/SearchFilter.vue";
import { defineComponent, onMounted, Ref, ref, watch } from "vue";
import { useSearchStore } from "@/stores/search";
import { useDialogsStore } from "@/stores/dialogs";
import searchquerybuilder from "@/utils/searchquerybuilder";
import error from "@/utils/error";
import BookmarkSave from "@/components/BookmarkSave.vue";
import TemplateOverview from "@/components/TemplateOverview.vue";
import BookmarkOverview from "@/components/BookmarkOverview.vue";
import templateApi from "@/api/templateApi";

export default defineComponent({
  components: {
    BookmarkOverview,
    TemplateOverview,
    BookmarkSave,
    GroupOverview,
    RightsView,
    MetadataView,
    SearchFilter,
  },

  setup() {
    const items: Ref<Array<ItemRest>> = ref([]);
    const currentItem = ref({} as ItemRest);
    const headersValueVSelect = ref([]);
    const selectedItems = ref([]);
    const searchTerm = ref("");
    const tableContentLoading = ref(true);
    const hintSearchField = ref(
      "Syntax der Sucheingabe: keyword:'suchtext'; Erlaubte Keywords:" +
        "com(Community), col(Collection), hdl (Handle Metadata), hdlcol (Handle Collection), hdlcom(Handle Community), hdlsubcom (Handle Subcommunity), sig(Paket-Sigel), tit(Titel), zdb(ZDB-Id)." +
        " Negationen(!), Verundungen(&), Veroderungen(|) sowie Klammersetzungen sind zulässig, z.B.: col:'(subject1 | subject2) & !subject3'." +
        "Wichtig: Zeichen die als logische Operatoren dienen, aber teil der Suche sein sollen, müssen escaped werden mit \\ " +
        " (z.B. col:'EU & \\(European\\)')"
    );

    /**
     * Error handling>
     */
    const hasSearchTokenWithNoKeyError = ref(false);
    const hasSearchTokenWithNoKeyErrorMsg = ref("");
    const invalidSearchKeyError = ref(false);
    const invalidSearchKeyErrorMsg = ref("");
    const loadAlertError = ref(false);
    const loadAlertErrorMessage = ref("");

    const headers = [
      {
        text: "Item-Id",
        align: "start",
        sortable: false,
        value: "metadataId",
      },
      {
        text: "Titel",
        sortable: true,
        value: "title",
        width: "300px",
      },
      {
        text: "Handle",
        sortable: true,
        value: "handle",
      },
      {
        text: "Community",
        sortable: true,
        value: "communityName",
      },
      {
        text: "Collection",
        sortable: true,
        value: "collectionName",
      },
      {
        text: "Publikationstyp",
        sortable: true,
        value: "publicationType",
      },
      {
        text: "Publikationsjahr",
        sortable: true,
        value: "publicationDate",
      },
      {
        text: "Band",
        value: "band",
      },
      {
        text: "DOI",
        value: "doi",
      },
      {
        text: "ISBN",
        value: "isbn",
      },
      {
        text: "ISSN",
        value: "issn",
      },
      {
        text: "Paket-Sigel",
        value: "paketSigel",
      },
      {
        text: "PPN",
        value: "ppn",
      },
      {
        text: "Rechte-K10Plus",
        value: "rightsK10plus",
      },
      {
        text: "Titel Journal",
        value: "titleJournal",
      },
      {
        text: "Titel Serie",
        value: "titleSeries",
      },
      {
        text: "ZDB-ID",
        value: "zdbId",
      },
    ] as Array<DataTableHeader>;

    const selectedHeaders = ref(headers.slice(0, 6));

    const currentPage = ref(1);
    const pageSize = ref(25); // Default page size is 25
    const pageSizes = ref<Array<number>>([5, 10, 25, 50]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);

    // Page changes
    const handlePageChange = (nextPage: number) => {
      currentPage.value = nextPage;
      if (templateSearchIsActive.value) {
        executeSearchByTemplateId();
      } else {
        searchQuery();
      }
    };

    const handlePageSizeChange = (size: number) => {
      pageSize.value = size;
      currentPage.value = 1;
      if (templateSearchIsActive.value) {
        executeSearchByTemplateId();
      } else {
        searchQuery();
      }
    };

    /**
     * Row selection management.
     *
     * Single Click
     */
    const addActiveItem = (metadata: MetadataRest, row: any) => {
      row.select(true);
      let item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === metadata.metadataId
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
    };

    /** Double Click **/
    const setActiveItem = (clickevent: any, row: any) => {
      row.select(true);
      let item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === row.item.metadataId
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
      selectedItems.value = selectedItems.value.filter(
        (e: MetadataRest) => e.metadataId == row.item.metadataId
      );
    };

    const getAlertLoad = () => {
      return loadAlertError;
    };

    onMounted(() => startSearch());

    watch(headersValueVSelect, (currentValue) => {
      selectedHeaders.value = currentValue;
    });

    // Search
    const searchStore = useSearchStore();

    const alertIsActive = ref(false);
    const alertMsg = ref("");

    const templateSearchIsActive = ref(false);
    const currentTemplateId = ref(0);
    const initSearchByTemplateId = (templateId: number) => {
      templateSearchIsActive.value = true;
      currentPage.value = 1;
      currentTemplateId.value = templateId;
      closeTemplateOverview();
      alertMsg.value =
        "Alle gespeicherten Suchen für Template-ID " +
        templateId +
        " wurden ausgeführt. Es kann zu Doppelungen in den Suchergebnissen kommen.";
      alertIsActive.value = true;
      executeSearchByTemplateId();
    };

    const executeSearchByTemplateId = () => {
      templateApi
        .getItemsByTemplateId(
          currentTemplateId.value,
          pageSize.value,
          (currentPage.value - 1) * pageSize.value
        )
        .then((response: ItemInformation) => {
          processSearchResult(response);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            loadAlertErrorMessage.value = errMsg;
            loadAlertError.value = true;
          });
        });
    };

    const executeBookmarkSearch = (bookmark: BookmarkRest) => {
      searchquerybuilder.setPublicationDateFilter(searchStore, bookmark);
      searchquerybuilder.setPaketSigelFilter(searchStore, bookmark);
      searchquerybuilder.setPublicationTypeFilter(searchStore, bookmark);
      searchquerybuilder.setZDBFilter(searchStore, bookmark);
      searchquerybuilder.setAccessStateFilter(searchStore, bookmark);
      searchquerybuilder.setFormalRuleFilter(searchStore, bookmark);
      searchquerybuilder.setTempValFilter(searchStore, bookmark);
      searchquerybuilder.setStartDateAtFilter(searchStore, bookmark);
      searchquerybuilder.setEndDateAtFilter(searchStore, bookmark);
      searchquerybuilder.setValidOnFilter(searchStore, bookmark);
      searchquerybuilder.setNoRightInformationFilter(searchStore, bookmark);
      closeBookmarkOverview();
      alertMsg.value =
        "Eine gespeicherte Suche '" +
        bookmark.bookmarkName +
        "' und wurde erfolgreich ausgeführt.";
      alertIsActive.value = true;
      startSearch();
    };

    const startSearch = () => {
      currentPage.value = 1;
      if (searchTerm.value == undefined) {
        searchTerm.value = "";
      }
      searchStore.lastSearchTerm = searchTerm.value;
      invalidSearchKeyError.value = false;
      hasSearchTokenWithNoKeyError.value = false;
      templateSearchIsActive.value = false;
      searchQuery();
    };

    const searchQuery = () => {
      api
        .searchQuery(
          searchTerm.value,
          (currentPage.value - 1) * pageSize.value,
          pageSize.value,
          pageSize.value,
          searchquerybuilder.buildPublicationDateFilter(searchStore),
          searchquerybuilder.buildPublicationTypeFilter(searchStore),
          searchquerybuilder.buildAccessStateFilter(searchStore),
          searchquerybuilder.buildTempValFilter(searchStore),
          searchquerybuilder.buildStartDateAtFilter(searchStore),
          searchquerybuilder.buildEndDateAtFilter(searchStore),
          searchquerybuilder.buildFormalRuleFilter(searchStore),
          searchquerybuilder.buildValidOnFilter(searchStore),
          searchquerybuilder.buildPaketSigelIdFilter(searchStore),
          searchquerybuilder.buildZDBIdFilter(searchStore),
          searchquerybuilder.buildNoRightInformation(searchStore)
        )
        .then((response: ItemInformation) => {
          processSearchResult(response);
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            loadAlertErrorMessage.value = errMsg;
            loadAlertError.value = true;
          });
        });
    };

    const processSearchResult = (response: ItemInformation) => {
      items.value = response.itemArray;
      tableContentLoading.value = false;
      totalPages.value = response.totalPages;
      numberOfResults.value = response.numberOfResults;
      if (response.invalidSearchKey?.length || 0 > 0) {
        invalidSearchKeyErrorMsg.value =
          "Die folgenden Suchkeys sind ungültig: " +
            response.invalidSearchKey?.join(", ") || "";
        invalidSearchKeyError.value = true;
      }

      if (response.hasSearchTokenWithNoKey == true) {
        hasSearchTokenWithNoKeyErrorMsg.value =
          "Mindestens ein Wort enthält keinen Suchkey." +
          " Dieser Teil wird bei der Suche ignoriert.";
        hasSearchTokenWithNoKeyError.value = true;
      }
      // TODO: wird nur in abhängigkeit von ergebnis angezeigt
      if (response.paketSigelWithCount != undefined) {
        searchStore.paketSigelIdReceived = response.paketSigelWithCount;
      }
      if (response.hasLicenceContract != undefined) {
        searchStore.hasLicenceContract = response.hasLicenceContract;
      }
      if (response.hasOpenContentLicence != undefined) {
        searchStore.hasOpenContentLicence = response.hasOpenContentLicence;
      }
      if (response.hasZbwUserAgreement != undefined) {
        searchStore.hasZbwUserAgreement = response.hasZbwUserAgreement;
      }
      if (response.itemArray.length == 0) {
        reduceToSelectedFilters();
      } else {
        resetAllDynamicFilter(response);
      }
    };

    const reduceToSelectedFilters = () => {
      searchStore.accessStateReceived =
        searchStore.accessStateSelectedLastSearch.map((elem: string) => {
          return {
            count: 0,
            accessState: searchquerybuilder.accessStateToType(elem),
          } as AccessStateWithCountRest;
        });
      searchStore.accessStateIdx = reduceIdx(searchStore.accessStateIdx);

      searchStore.paketSigelIdReceived =
        searchStore.paketSigelSelectedLastSearch.map((elem: string) => {
          return {
            count: 0,
            paketSigel: elem,
          } as PaketSigelWithCountRest;
        });
      searchStore.paketSigelIdIdx = reduceIdx(searchStore.paketSigelIdIdx);

      searchStore.publicationTypeReceived =
        searchStore.publicationTypeSelectedLastSearch.map((elem: string) => {
          return {
            count: 0,
            publicationType: searchquerybuilder.publicationTypeToType(elem),
          } as PublicationTypeWithCountRest;
        });
      searchStore.publicationTypeIdx = reduceIdx(
        searchStore.publicationTypeIdx
      );

      searchStore.zdbIdReceived = searchStore.zdbIdSelectedLastSearch.map(
        (elem: string) => {
          return {
            count: 0,
            zdbId: elem,
          } as ZdbIdWithCountRest;
        }
      );
      searchStore.zdbIdIdx = reduceIdx(searchStore.zdbIdIdx);
    };

    const reduceIdx = (idxMap: Array<boolean>): Array<boolean> => {
      return idxMap.filter((elem: boolean): boolean => {
        return elem;
      });
    };

    const resetAllDynamicFilter = (response: ItemInformation) => {
      // Reset AccessState
      searchStore.accessStateReceived =
        response.accessStateWithCount != undefined
          ? response.accessStateWithCount
          : Array(0);
      searchStore.accessStateIdx = Array(
        searchStore.accessStateReceived.length
      ).fill(false);
      resetDynamicFilter(
        searchStore.accessStateReceived.map((e) => e.accessState),
        searchStore.accessStateSelectedLastSearch,
        searchStore.accessStateIdx
      );
      // Reset Paket Sigel
      searchStore.paketSigelIdReceived =
        response.paketSigelWithCount != undefined
          ? response.paketSigelWithCount
          : Array(0);
      searchStore.paketSigelIdIdx = Array(
        searchStore.paketSigelIdReceived.length
      ).fill(false);
      resetDynamicFilter(
        searchStore.paketSigelIdReceived.map((e) => e.paketSigel),
        searchStore.paketSigelSelectedLastSearch,
        searchStore.paketSigelIdIdx
      );
      // Reset Publication Type
      searchStore.publicationTypeReceived =
        response.publicationTypeWithCount != undefined
          ? response.publicationTypeWithCount.sort((a, b) =>
              b.publicationType.localeCompare(a.publicationType)
            )
          : Array(0);
      searchStore.publicationTypeIdx = Array(
        searchStore.publicationTypeReceived.length
      ).fill(false);
      resetDynamicFilter(
        searchStore.publicationTypeReceived.map((e) => e.publicationType),
        searchStore.publicationTypeSelectedLastSearch,
        searchStore.publicationTypeIdx
      );
      // Reset ZDB Id
      searchStore.zdbIdReceived =
        response.zdbIdWithCount != undefined
          ? response.zdbIdWithCount
          : Array(0);
      searchStore.zdbIdIdx = Array(searchStore.zdbIdReceived.length).fill(
        false
      );
      resetDynamicFilter(
        searchStore.zdbIdReceived.map((e) => e.zdbId),
        searchStore.zdbIdSelectedLastSearch,
        searchStore.zdbIdIdx
      );
    };

    const resetDynamicFilter = (
      receivedFilters: Array<string>,
      savedFilters: Array<string>,
      idxMap: Array<boolean>
    ) => {
      receivedFilters.forEach((elem: string, index: number): void => {
        if (savedFilters.includes(elem)) {
          idxMap[index] = true;
        }
      });
    };

    // parse publication type
    const parsePublicationType = (pubType: string) => {
      switch (pubType) {
        case "article":
          return "Article";
        case "book":
          return "Book";
        case "bookPart":
          return "Book Part";
        case "book_part":
          return "Book Part";
        case "conferencePaper":
          return "Conference Paper";
        case "conference_paper":
          return "Conference Paper";
        case "periodicalPart":
          return "Periodical Part";
        case "periodical_part":
          return "Periodical Part";
        case "proceedings":
          return "Proceedings";
        case "researchReport":
          return "Research Report";
        case "research_report":
          return "Research Report";
        case "thesis":
          return "Thesis";
        case "workingPaper":
          return "Working Paper";
        case "working_paper":
          return "Working Paper";
        default:
          return "Unknown pub type:" + pubType;
      }
    };

    /**
     * Manage Dialogs.
     */
    const dialogStore = useDialogsStore();

    const closeBookmarkOverview = () => {
      dialogStore.bookmarkOverviewActivated = false;
    };
    const closeGroupDialog = () => {
      dialogStore.groupOverviewActivated = false;
    };

    const closeTemplateOverview = () => {
      dialogStore.templateOverviewActivated = false;
    };

    const closeBookmarkSaveDialog = () => {
      dialogStore.bookmarkSaveActivated = false;
    };

    const bookmarkSuccessfulMsg = ref(false);
    const newBookmarkId = ref(-1);
    const addBookmarkSuccessful = (bookmarkId: number) => {
      newBookmarkId.value = bookmarkId;
      bookmarkSuccessfulMsg.value = true;
    };

    return {
      alertIsActive,
      alertMsg,
      bookmarkSuccessfulMsg,
      currentItem,
      currentPage,
      dialogStore,
      hasSearchTokenWithNoKeyError,
      hasSearchTokenWithNoKeyErrorMsg,
      headers,
      headersValueVSelect,
      hintSearchField,
      items,
      invalidSearchKeyError,
      invalidSearchKeyErrorMsg,
      loadAlertError,
      loadAlertErrorMessage,
      newBookmarkId,
      numberOfResults,
      pageSize,
      pageSizes,
      searchTerm,
      searchStore,
      selectedHeaders,
      selectedItems,
      tableContentLoading,
      totalPages,
      // Methods
      addActiveItem,
      addBookmarkSuccessful,
      closeBookmarkOverview,
      closeBookmarkSaveDialog,
      closeGroupDialog,
      closeTemplateOverview,
      executeBookmarkSearch,
      initSearchByTemplateId,
      getAlertLoad,
      handlePageChange,
      handlePageSizeChange,
      parsePublicationType,
      searchQuery,
      setActiveItem,
      startSearch,
    };
  },
});
</script>

<style scoped>
/deep/ tr.v-data-table__selected {
  background: #7d92f5 !important;
}
</style>
<template>
  <v-container>
    <v-dialog
      v-model="dialogStore.groupOverviewActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeGroupDialog"
    >
      <GroupOverview></GroupOverview>
    </v-dialog>
    <v-dialog
      v-model="dialogStore.bookmarkSaveActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeBookmarkSaveDialog"
    >
      <BookmarkSave
        v-on:addBookmarkSuccessful="addBookmarkSuccessful"
      ></BookmarkSave>
    </v-dialog>
    <v-dialog
      v-model="dialogStore.templateOverviewActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeTemplateOverview"
    >
      <TemplateOverview
        v-on:getItemsByTemplateId="initSearchByTemplateId"
      ></TemplateOverview>
    </v-dialog>
    <v-dialog
      v-model="dialogStore.bookmarkOverviewActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeBookmarkOverview"
    >
      <BookmarkOverview
        v-on:executeBookmarkSearch="executeBookmarkSearch"
      ></BookmarkOverview>
    </v-dialog>
    <v-row>
      <v-col cols="2">
        <SearchFilter></SearchFilter>
      </v-col>
      <v-col cols="6">
        <v-card>
          <v-card-title>
            <v-text-field
              v-model="searchTerm"
              append-icon="mdi-magnify"
              clearable
              :hint="hintSearchField"
              label="Suche"
              outlined
              persistent-hint
              single-line
              @click:append="startSearch"
              @keydown.enter.prevent="startSearch"
            ></v-text-field>
          </v-card-title>
          <v-spacer></v-spacer>
          <v-alert v-model="loadAlertError" dismissible text type="error">
            Laden der bibliographischen Daten war nicht erfolgreich:
            {{ loadAlertErrorMessage }}
          </v-alert>
          <v-alert
            v-model="invalidSearchKeyError"
            dismissible
            text
            type="error"
          >
            {{ invalidSearchKeyErrorMsg }}
          </v-alert>
          <v-alert
            v-model="hasSearchTokenWithNoKeyError"
            dismissible
            text
            type="error"
          >
            {{ hasSearchTokenWithNoKeyErrorMsg }}
          </v-alert>
          <v-alert
            v-model="bookmarkSuccessfulMsg"
            dismissible
            text
            type="success"
          >
            Bookmark erfolgreich hinzugefügt mit Id {{ newBookmarkId }}.
          </v-alert>
          <v-alert v-model="alertIsActive" dismissible text type="success">
            {{ alertMsg }}
          </v-alert>
          <v-select
            v-model="headersValueVSelect"
            :items="headers"
            label="Spaltenauswahl"
            multiple
            return-object
          >
            <template v-slot:selection="{ item, index }">
              <v-chip v-if="index === 0">
                <span>{{ item.text }}</span>
              </v-chip>
              <span v-if="index === 1" class="grey--text caption"
                >(+{{ headersValueVSelect.length - 1 }} others)</span
              >
            </template>
          </v-select>

          <v-col cols="5" sm="5"> Suchergebnisse: {{ numberOfResults }}</v-col>
          <v-data-table
            v-model="selectedItems"
            :headers="selectedHeaders"
            :hide-default-footer="true"
            :items="items.map((value) => value.metadata)"
            disable-pagination
            item-key="metadataId"
            loading="tableContentLoading"
            loading-text="Daten werden geladen... Bitte warten."
            show-select
            @click:row="addActiveItem"
            @dblclick:row="setActiveItem"
          >
            <template v-slot:item.handle="{ item }">
              <td>
                <a :href="item.handle">{{ item.handle }}</a>
              </td>
            </template>
            <template v-slot:item.publicationType="{ item }">
              <td>{{ parsePublicationType(item.publicationType) }}</td>
            </template>
            <template v-slot:item.publicationDate="{ item }">
              <td>{{ item.publicationDate.toLocaleDateString("de") }}</td>
            </template>
          </v-data-table>
          <v-col cols="14" sm="12">
            <v-row>
              <v-col cols="2" sm="2">
                <v-select
                  v-model="pageSize"
                  :items="pageSizes"
                  label="Einträge pro Seite"
                  @change="handlePageSizeChange"
                ></v-select>
              </v-col>
              <v-col cols="10" sm="9">
                <v-pagination
                  v-model="currentPage"
                  :length="totalPages"
                  next-icon="mdi-menu-right"
                  prev-icon="mdi-menu-left"
                  total-visible="7"
                  @input="handlePageChange"
                ></v-pagination>
              </v-col>
            </v-row>
          </v-col>
        </v-card>
      </v-col>
      <v-col cols="4">
        <v-card v-if="currentItem.metadata" class="mx-auto" tile>
          <RightsView
            :handle="currentItem.metadata.handle"
            :metadataId="currentItem.metadata.metadataId"
            :rights="currentItem.rights"
          ></RightsView>
          <MetadataView
            :metadata="Object.assign({}, currentItem.metadata)"
          ></MetadataView>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
