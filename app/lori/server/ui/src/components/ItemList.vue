<script lang="ts">
import {
  AboutRest,
  AccessStateWithCountRest,
  BookmarkRest, IsPartOfSeriesCountRest,
  ItemInformation,
  ItemRest,
  PaketSigelWithCountRest,
  PublicationTypeWithCountRest,
  RightRest,
  ZdbIdWithCountRest,
} from "@/generated-sources/openapi";
import api from "@/api/api";
import rightApi from "@/api/rightApi";
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
import RightsEditDialog from "@/components/RightsEditDialog.vue";
import metadata_utils from "@/utils/metadata_utils";

export default defineComponent({
  computed: {
    metadata_utils() {
      return metadata_utils;
    },
  },
  components: {
    RightsEditDialog,
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
    const selectedItems: Ref<Array<string>> = ref([]);
    const tableContentLoading = ref(true);

    /**
     * Error handling>
     */
    const loadAlertError = ref(false);
    const loadAlertErrorMessage = ref("");

    const headers = [
      {
        title: "Metadata-Id",
        align: "start",
        sortable: true,
        value: "metadataId",
      },
      {
        title: "Titel",
        sortable: true,
        value: "title",
        width: "300px",
      },
      {
        title: "Handle",
        sortable: true,
        value: "handle",
      },
      {
        title: "Community",
        sortable: true,
        value: "communityName",
      },
      {
        title: "Collection",
        sortable: true,
        value: "collectionName",
      },
      {
        title: "Publikationstyp",
        sortable: true,
        value: "publicationType",
      },
      {
        title: "Publikationsjahr",
        sortable: true,
        value: "publicationDate",
      },
      {
        title: "Band",
        value: "band",
      },
      {
        title: "DOI",
        value: "doi",
      },
      {
        title: "ISBN",
        value: "isbn",
      },
      {
        title: "ISSN",
        value: "issn",
      },
      {
        title: "Paket-Sigel",
        value: "paketSigel",
      },
      {
        title: "PPN",
        value: "ppn",
      },
      {
        title: "Rechte-K10Plus",
        value: "rightsK10plus",
      },
      {
        title: "Titel Journal",
        value: "titleJournal",
      },
      {
        title: "Titel Serie",
        value: "titleSeries",
      },
      {
        title: "ZDB-ID",
        value: "zdbId",
      },
    ];

    const selectedHeaders = ref(headers.slice(0, 6));

    const currentPage = ref(1);
    const currentRightId = ref("");
    const pageSize = ref(10); // initial page size
    const pageSizes = ref<Array<number>>([5, 10, 25, 50]);
    const totalPages = ref(0);
    const numberOfResults = ref(0);

    // Page changes
    const handlePageChange = () => {
      if (templateSearchIsActive.value) {
        executeSearchByRightId(currentRightId.value);
      } else {
        searchQuery();
      }
    };

    const handlePageSizeChange = () => {
      currentPage.value = 1;
      if (templateSearchIsActive.value) {
        executeSearchByRightId(currentRightId.value);
      } else {
        searchQuery();
      }
    };

    /**
     * Row selection management.
     *
     * Single Click
     */
    const addActiveItem = (mouseEvent: MouseEvent, row: any) => {
      const item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === row.item.metadataId,
      );
      if (item !== undefined) {
        currentItem.value = item;
        selectedItems.value = [row.item.metadataId];
      }
    };

    /** Double Click **/
    const setActiveItem = (mouseEvent: MouseEvent, row: any) => {
      //row.select(true);
      const item: ItemRest | undefined = items.value.find(
        (e) => e.metadata.metadataId === row.item.metadataId,
      );
      if (item !== undefined) {
        currentItem.value = item;
      }
      selectedItems.value = selectedItems.value.filter(
        (e: string) => e == row.item.metadataId,
      );
    };

    const getAlertLoad = () => {
      return loadAlertError;
    };

    onMounted(() => {
      const hasTemplateParameter = loadTemplateView();
      if (!hasTemplateParameter) {
        loadRightView();
      }
      const hasMetadataParameter = loadMetadataView();
      if (!hasMetadataParameter) {
        startSearch();
      }
      loadBackendParameters();
    });

    watch(headersValueVSelect, (currentValue) => {
      selectedHeaders.value = currentValue;
    });

    watch(currentPage, () => {
      handlePageChange();
    });

    watch(pageSize, () => {
      handlePageSizeChange();
    });

    // Initial Template View
    const templateLoadError = ref(false);
    const templateLoadErrorMsg = ref("");
    const queryParameterRight = ref({} as RightRest);
    const rightEditActivated = ref(false);
    const loadTemplateView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const templateId: string | null = urlParams.get("templateId");
      if (templateId == null || templateId == "") {
        return false;
      }
      templateApi
        .getTemplateById(templateId)
        .then((response: RightRest) => {
          queryParameterRight.value = response;
          rightEditActivated.value = true;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            templateLoadErrorMsg.value = errMsg;
            templateLoadError.value = true;
          });
        });
      return true;
    };

    const loadRightView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const rightId: string | null = urlParams.get("rightId");
      if (rightId == null || rightId == "") {
        return false;
      }
      rightApi
        .getRightById(rightId)
        .then((response: RightRest) => {
          queryParameterRight.value = response;
          rightEditActivated.value = true;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            templateLoadErrorMsg.value = errMsg;
            templateLoadError.value = true;
          });
        });
      return true;
    };

    const loadMetadataView: () => boolean = () => {
      const urlParams = new URLSearchParams(window.location.search);
      const metadataId: string | null = urlParams.get("metadataId");
      if (metadataId == null || metadataId == "") {
        return false;
      }
      executeSearchByMetadataId("metadataId:" + metadataId);
      return true;
    };

    const loadBackendParameters = () => {
      api
        .getAboutInformation()
        .then((response: AboutRest) => {
          searchStore.stage = response.stage;
          searchStore.handleURLResolver = response.handleURL;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            tableContentLoading.value = false;
            loadAlertErrorMessage.value = errMsg;
            loadAlertError.value = true;
          });
        });
    };

    const closeTemplateEditDialog = () => {
      rightEditActivated.value = false;
    };

    // Search
    const searchStore = useSearchStore();

    const alertIsActive = ref(false);
    const alertMsg = ref("");

    const templateSearchIsActive = ref(false);
    const initSearchByRightId = (rightId: string) => {
      templateSearchIsActive.value = true;
      currentPage.value = 1;
      currentRightId.value = rightId;
      closeTemplateOverview();
      alertMsg.value =
        "Alle gespeicherten Suchen für Template-ID " +
        rightId +
        " wurden ausgeführt.";
      alertIsActive.value = true;
      searchStore.isLastSearchForTemplates = true;
      executeSearchByRightId(rightId);
    };

    const executeSearchByRightId = (rightId: string) => {
      api
        .searchQuery(
          "",
          (currentPage.value - 1) * pageSize.value, // offset
          pageSize.value, // limit
          pageSize.value,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
            rightId,
          undefined,
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

    const executeSearchByMetadataId = (searchTerm: string) => {
      searchStore.isLastSearchForTemplates = false;
      api
        .searchQuery(
          searchTerm,
          (currentPage.value - 1) * pageSize.value, // offset
          pageSize.value, // limit
          currentPage.value,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
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
      searchquerybuilder.setRightIdFilter(searchStore, bookmark);
      searchquerybuilder.setSeriesFilter(searchStore, bookmark);
      searchStore.searchTerm =
        bookmark.searchTerm != undefined ? bookmark.searchTerm : "";
      closeBookmarkOverview();
      alertMsg.value =
        "Eine gespeicherte Suche '" +
        bookmark.bookmarkName +
        "' und wurde erfolgreich ausgeführt.";
      alertIsActive.value = true;
      startSearch();
    };

    const startEmptySearch = () => {
      searchStore.searchTerm = "";
      startSearch();
    };

    const startSearch = () => {
      currentPage.value = 1;
      if (searchStore.searchTerm == undefined) {
        searchStore.searchTerm = "";
      }
      searchStore.lastSearchTerm = searchStore.searchTerm;
      searchStore.isLastSearchForTemplates = false;
      templateSearchIsActive.value = false;
      currentRightId.value = "";
      currentItem.value = {} as ItemRest;
      searchQuery();
    };

    const searchQuery = () => {
      api
        .searchQuery(
          searchStore.searchTerm,
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
          searchquerybuilder.buildNoRightInformation(searchStore),
          searchquerybuilder.buildRightIdFilter(searchStore),
          searchquerybuilder.buildSeriesFilter(searchStore),
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
        searchStore.publicationTypeIdx,
      );

      searchStore.zdbIdReceived = searchStore.zdbIdSelectedLastSearch.map(
        (elem: string) => {
          return {
            count: 0,
            zdbId: elem,
          } as ZdbIdWithCountRest;
        },
      );
      searchStore.zdbIdIdx = reduceIdx(searchStore.zdbIdIdx);
      searchStore.seriesReceived = searchStore.seriesSelectedLastSearch.map(
          (elem: string) => {
            return {
              count: 0,
              series: elem,
            } as IsPartOfSeriesCountRest;
          },
      );
      searchStore.seriesIdx = reduceIdx(searchStore.seriesIdx);
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
        searchStore.accessStateReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.accessStateReceived.map((e) => e.accessState),
        searchStore.accessStateSelectedLastSearch,
        searchStore.accessStateIdx,
      );
      // Reset Paket Sigel
      searchStore.paketSigelIdReceived =
        response.paketSigelWithCount != undefined
          ? response.paketSigelWithCount
          : Array(0);
      searchStore.paketSigelIdIdx = Array(
        searchStore.paketSigelIdReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.paketSigelIdReceived.map((e) => e.paketSigel),
        searchStore.paketSigelSelectedLastSearch,
        searchStore.paketSigelIdIdx,
      );
      // Reset Publication Type
      searchStore.publicationTypeReceived =
        response.publicationTypeWithCount != undefined
          ? response.publicationTypeWithCount
          : Array(0);
      searchStore.publicationTypeIdx = Array(
        searchStore.publicationTypeReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.publicationTypeReceived.map((e) => e.publicationType),
        searchStore.publicationTypeSelectedLastSearch,
        searchStore.publicationTypeIdx,
      );
      // Reset ZDB Id
      searchStore.zdbIdReceived =
        response.zdbIdWithCount != undefined
          ? response.zdbIdWithCount
          : Array(0);
      searchStore.zdbIdIdx = Array(searchStore.zdbIdReceived.length).fill(
        false,
      );
      resetDynamicFilter(
          searchStore.zdbIdReceived.map((e) => e.zdbId),
          searchStore.zdbIdSelectedLastSearch,
          searchStore.zdbIdIdx,
      );
      // Reset Series
      searchStore.seriesReceived =
          response.isPartOfSeriesCount != undefined
              ? response.isPartOfSeriesCount
              : Array(0);
      searchStore.seriesIdx = Array(searchStore.seriesReceived.length).fill(
          false,
      );
      resetDynamicFilter(
          searchStore.seriesReceived.map((e) => e.series),
          searchStore.seriesSelectedLastSearch,
          searchStore.seriesIdx,
      );
      // Reset Template Names
      searchStore.templateNameReceived =
        response.templateNameWithCount != undefined
          ? response.templateNameWithCount
          : Array(0);
      searchStore.templateNameIdx = Array(
        searchStore.templateNameReceived.length,
      ).fill(false);
      resetDynamicFilter(
        searchStore.templateNameReceived.map((e) => e.rightId),
        searchStore.templateNameSelectedLastSearch,
        searchStore.templateNameIdx,
      );
    };

    const resetDynamicFilter = (
      receivedFilters: Array<string>,
      savedFilters: Array<string>,
      idxMap: Array<boolean>,
    ) => {
      receivedFilters.forEach((elem: string, index: number): void => {
        if (savedFilters.includes(elem)) {
          idxMap[index] = true;
        }
      });
    };

    // parse publication type
    const parsePublicationType = (pubType: string) => {
      return metadata_utils.prettyPrintPublicationType(pubType);
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

    const searchHelpDialog = ref(false);

    const selectedRowColor = (row: any) => {
      if(selectedItems.value[0] !== undefined && selectedItems.value[0] == row.item.metadataId){
        return { class: "bg-blue-lighten-4"}
      }
    };
    const renderKey = ref(0);

    return {
      alertIsActive,
      alertMsg,
      bookmarkSuccessfulMsg,
      currentItem,
      currentPage,
      queryParameterRight,
      dialogStore,
      headers,
      headersValueVSelect,
      items,
      loadAlertError,
      loadAlertErrorMessage,
      newBookmarkId,
      numberOfResults,
      pageSize,
      pageSizes,
      searchStore,
      selectedHeaders,
      selectedItems,
      searchHelpDialog,
      tableContentLoading,
      rightEditActivated,
      templateLoadError,
      templateLoadErrorMsg,
      totalPages,
      renderKey,
      // Methods
      addActiveItem,
      addBookmarkSuccessful,
      closeBookmarkOverview,
      closeBookmarkSaveDialog,
      closeTemplateEditDialog,
      closeGroupDialog,
      closeTemplateOverview,
      executeBookmarkSearch,
      initSearchByRightId,
      getAlertLoad,
      handlePageChange,
      handlePageSizeChange,
      loadTemplateView,
      parsePublicationType,
      searchQuery,
      selectedRowColor,
      setActiveItem,
      startEmptySearch,
      startSearch,
    };
  },
});
</script>

<style scoped>
:deep(tr.v-data-table__selected) {
  background: #7d92f5 !important;
}
table.special, th.special, td.special {
  border:1px solid black;
}
</style>
<template>
  <v-navigation-drawer permanent>
        <SearchFilter
            v-on:startEmptySearch="startEmptySearch"
            v-on:startSearch="startSearch"
        ></SearchFilter>
  </v-navigation-drawer>
  <v-main class="d-flex align-center justify-center">
  <v-card>
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
        v-on:getItemsByRightId="initSearchByRightId"
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
    <v-dialog v-model="templateLoadError" max-width="1000">
      <v-card>
        <v-card-title class="text-h5"
          >Laden von Template fehlgeschlagen</v-card-title
        >
        <v-card-text>
          Informationen zum Fehler: {{ templateLoadErrorMsg }}
        </v-card-text>
      </v-card>
    </v-dialog>
    <v-dialog
      v-model="rightEditActivated"
      :retain-focus="false"
      max-width="1000px"
      v-on:close="closeTemplateEditDialog"
    >
      <RightsEditDialog
        :index="-1"
        :isNewRight="false"
        :isNewTemplate="false"
        :right="queryParameterRight"
        v-on:editRightClosed="closeTemplateEditDialog"
      ></RightsEditDialog>
    </v-dialog>
        <v-card>
          <v-card-title>
            <v-text-field
              v-model="searchStore.searchTerm"
              append-icon="mdi-magnify"
              clearable
              label="Suche"
              variant="outlined"
              persistent-hint
              single-line
              @click:append="startSearch"
              @keydown.enter.prevent="startSearch"
            ></v-text-field>
            <v-dialog v-model="searchHelpDialog" max-width="500px">
              <template v-slot:activator="{ props: activatorProps }">
                <v-btn
                  density="compact"
                  icon="mdi-help"
                  v-bind="activatorProps"
                >
                </v-btn>
              </template>
              <v-card
              >
                <template v-slot:actions>
                  <v-spacer></v-spacer>
                  <v-btn color="blue darken-1" @click="searchHelpDialog = false"> Zurück </v-btn>
                </template>
                <v-card-title class="text-h5">
                  Syntax der Sucheingabe
                </v-card-title>
                <v-card-text>
                  <p class="text-left text-body-1 font-weight-bold">Genereller Aufbau:</p>
                  <p class="text-center text-body-2 bg-grey-lighten-2">
                    suchschluessel1:"wert1" & suchschluessel2:"wert2"
                  </p>
                  <p class="text-left text-body-2 mt-1 mb-1">
                    Das " kann auch durch ' ersetzt werden. Beides kann man
                    weglassen wenn nur ein Wort gesucht wird.
                  </p>
                  <table class="special">
                    <tr class=special>
                      <th class=special>Suche</th>
                      <th class=special>Suchschlüssel</th>
                    </tr>
                    <tr class=special>
                      <td class=special>Titel</td>
                      <td class=special>tit</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Handle des Items</td>
                      <td class=special>hdl</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Community</td>
                      <td class=special>com</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Handle Community</td>
                      <td class=special>hdlcom</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Handle Subommunity</td>
                      <td class=special>hdlsubcom</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Collection</td>
                      <td class=special>col</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Handle Collection</td>
                      <td class=special>hdlcol</td>
                    </tr>
                    <tr class=special>
                      <td class=special>ZDB-Id</td>
                      <td class=special>zdb</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Paket-Sigel</td>
                      <td class=special>sig</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Lizenz URL</td>
                      <td class=special>lur</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Metadata Id Lori</td>
                      <td class=special>metadataid</td>
                    </tr>
                  </table>
                  <p class="text-left text-body-2 mt-1 mb-1">
                  Zeichen die als logische Operatoren dienen, aber Teil der Suche sein sollen,
                  müssen mit dem Zeichen \ vor und hinter dem entsprechenden Teil der Suche
                  versehen werden.
                  </p>
                  <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                    Beispiel: Community Asian Development Bank (ADB), Manila <br></br>
                    com:'Asian Development Bank \(ADB\), Manila'
                  </p>

                  <p class="text-left text-body-1 mt-4 font-weight-bold">
                    Bool'sche Operatoren
                  </p>
                  <table class="special">
                    <tr class=special>
                      <th class=special> </th>
                      <th class=special> </th>
                      <th class=special>Beispiele</th>
                    </tr>
                    <tr class=special>
                      <td class=special>Und</td>
                      <td class=special>&</td>
                      <td class=special>col:'Economics & series'</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Nicht</td>
                      <td class=special>!</td>
                      <td class=special>col:'department' & !tit:'geopolitical'</td>
                    </tr>
                    <tr class=special>
                      <td class=special>Oder</td>
                      <td class=special>|</td>
                      <td class=special>col:'Economics | series'</td>
                    </tr>
                  </table>

                  <p class="text-left text-body-2 mt-1 mb-1">
                    Es ist möglich unter einem Suchschlüssel mehrere Werte gleichzeitig
                    zu suchen mittels der obigen Operatoren und Klammersetzungen.
                  </p>

                  <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                    Beispiel: col:"(subject1 | subject2) & !subject3"
                  </p>

                  <p class="text-left text-body-2 mt-1 mb-1">
                    Gleiches gilt wenn man auf verschiedenen Feldern suchen möchte.
                    Auch diese Suchen können mittels der Operatoren verknüpft werden.
                  </p>

                  <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                    Beispiel: col:"subject1" | (hdl:"handle" & !com:"community")
                  </p>
                  <p class="text-left text-body-1 mt-4">Sonderzeichen:</p>
                  <p class="text-left text-body-2 mt-1 mb-1">
                    Zeichen die als logische Operatoren dienen, aber teil der
                    Suche sein sollen, müssen mit einem Backslash \ beginnen
                  </p>

                  <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                    Beispiel: col:"EU & \(European\)"
                  </p>

                  <p class="text-left text-body-2 mt-1 mb-1">
                    Klammersetzungen sind zulässig, z.B:
                  </p>
                  <p class="text-center text-body-2 bg-grey-lighten-2 mt-1 mb-1">
                    col:'(subject1 | subject2) & !subject3'
                  </p>
                </v-card-text>
              </v-card>
            </v-dialog>
          </v-card-title>
          <v-spacer></v-spacer>
          <v-alert v-model="loadAlertError" closable type="error">
            Laden der bibliographischen Daten war nicht erfolgreich:
            {{ loadAlertErrorMessage }}
          </v-alert>
          <v-alert v-model="bookmarkSuccessfulMsg" closable type="success">
            Bookmark erfolgreich hinzugefügt mit Id
            {{ newBookmarkId.toString() }}.
          </v-alert>
          <v-alert v-model="alertIsActive" closable type="success">
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
            :items="items.map((value) => value.metadata)"
            :items-per-page="0"
            item-value="metadataId"
            :loading="tableContentLoading"
            :key="renderKey"
            :row-props="selectedRowColor"
            loading-text="Daten werden geladen... Bitte warten."
            show-select
            select-strategy="single"
            height="550px"
            @click:row="addActiveItem"
            @dblclick:row="setActiveItem"
          >
            <template v-slot:item.handle="{ item }">
              <td>
                <a
                  v-bind:href="
                    metadata_utils.hrefHandle(
                      item.handle,
                      searchStore.handleURLResolver,
                    )
                  "
                  >{{ metadata_utils.shortenHandle(item.handle) }}</a
                >
              </td>
            </template>
            <template v-slot:item.publicationType="{ item }">
              <td>{{ parsePublicationType(item.publicationType) }}</td>
            </template>
            <template v-slot:item.publicationDate="{ item }">
              <td>{{ item.publicationDate.toLocaleDateString("de") }}</td>
            </template>
            <template #bottom></template>
          </v-data-table>
          <v-col cols="14" sm="12">
            <v-row>
              <v-col cols="2" sm="2">
                <v-select
                  v-model="pageSize"
                  :items="pageSizes"
                  label="Einträge pro Seite"
                ></v-select>
              </v-col>
              <v-col cols="10" sm="9">
                <v-pagination
                  v-model="currentPage"
                  :length="totalPages"
                  next-icon="mdi-menu-right"
                  prev-icon="mdi-menu-left"
                  total-visible="7"
                ></v-pagination>
              </v-col>
            </v-row>
          </v-col>
        </v-card>

  </v-card>
  </v-main>
  <v-navigation-drawer location="right" :width="500" permanent>
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
  </v-navigation-drawer>
</template>
